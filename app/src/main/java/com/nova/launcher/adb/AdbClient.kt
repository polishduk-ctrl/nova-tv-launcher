package com.nova.launcher.adb

import android.content.Context
import android.util.Base64
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.DataInputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.math.BigInteger
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

/**
 * Minimal client for the ADB wire protocol, used to talk to the TV's *own* debugging service
 * over the loopback interface (127.0.0.1:5555). This lets NOVA run the same `pm` / `cmd`
 * commands a computer would, so no computer is needed. The TV asks the user to allow the
 * connection the first time (exactly like plugging in a computer).
 */
class AdbClient(private val keys: AdbKeys) : Closeable {

    private val socket = Socket()
    private lateinit var input: DataInputStream
    private lateinit var output: OutputStream
    private var nextLocalId = 1

    private class Message(val command: Int, val arg0: Int, val arg1: Int, val payload: ByteArray)

    class AuthRejected : IOException("The TV did not allow the debugging connection")

    /** Connects and authenticates. Blocks up to [authTimeoutMs] while the TV shows its prompt. */
    fun connect(host: String = "127.0.0.1", port: Int = 5555, authTimeoutMs: Int = 90_000) {
        socket.connect(InetSocketAddress(host, port), 4_000)
        socket.tcpNoDelay = true
        socket.soTimeout = authTimeoutMs
        input = DataInputStream(BufferedInputStream(socket.getInputStream()))
        output = socket.getOutputStream()
        send(CNXN, VERSION, MAX_PAYLOAD, "host::nova\u0000".toByteArray())

        var signed = false
        var offeredKey = false
        while (true) {
            val m = read()
            when (m.command) {
                CNXN -> { socket.soTimeout = 30_000; return }
                AUTH -> {
                    if (m.arg0 != AUTH_TOKEN) throw IOException("Unexpected auth message")
                    when {
                        !signed -> { send(AUTH, AUTH_SIGNATURE, 0, keys.sign(m.payload)); signed = true }
                        !offeredKey -> { send(AUTH, AUTH_RSAPUBLICKEY, 0, keys.adbPublicKey()); offeredKey = true }
                        else -> throw AuthRejected()
                    }
                }
                STLS -> throw IOException("This TV requires Wireless debugging pairing, which NOVA doesn't support yet")
                else -> Unit
            }
        }
    }

    /** Runs a shell command and returns its combined output. */
    fun shell(command: String): String {
        val localId = nextLocalId++
        send(OPEN, localId, 0, "shell:$command\u0000".toByteArray())
        val out = ByteArrayOutputStream()
        while (true) {
            val m = read()
            if (m.arg1 != localId) continue
            when (m.command) {
                WRTE -> {
                    out.write(m.payload)
                    send(OKAY, localId, m.arg0, EMPTY)
                }
                CLSE -> {
                    send(CLSE, localId, m.arg0, EMPTY)
                    return out.toString("UTF-8").trim()
                }
                else -> Unit // OKAY
            }
        }
    }

    private fun send(command: Int, arg0: Int, arg1: Int, payload: ByteArray) {
        val buf = ByteBuffer.allocate(24 + payload.size).order(ByteOrder.LITTLE_ENDIAN)
        var checksum = 0
        for (b in payload) checksum += (b.toInt() and 0xFF)
        buf.putInt(command).putInt(arg0).putInt(arg1).putInt(payload.size)
            .putInt(checksum).putInt(command.inv()).put(payload)
        output.write(buf.array())
        output.flush()
    }

    private fun read(): Message {
        val header = ByteArray(24)
        input.readFully(header)
        val b = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        val command = b.int
        val arg0 = b.int
        val arg1 = b.int
        val length = b.int
        if (length < 0 || length > 1 shl 20) throw IOException("Bad ADB message")
        val payload = ByteArray(length)
        input.readFully(payload)
        return Message(command, arg0, arg1, payload)
    }

    override fun close() {
        runCatching { socket.close() }
    }

    private companion object {
        const val CNXN = 0x4e584e43
        const val AUTH = 0x48545541
        const val OPEN = 0x4e45504f
        const val OKAY = 0x59414b4f
        const val CLSE = 0x45534c43
        const val WRTE = 0x45545257
        const val STLS = 0x534c5453
        const val VERSION = 0x01000000
        const val MAX_PAYLOAD = 256 * 1024
        const val AUTH_TOKEN = 1
        const val AUTH_SIGNATURE = 2
        const val AUTH_RSAPUBLICKEY = 3
        val EMPTY = ByteArray(0)
    }
}

/** NOVA's persistent ADB identity (RSA-2048), stored privately in the app's files. */
class AdbKeys private constructor(private val pair: KeyPair) {

    /** PKCS#1 v1.5 signature of the 20-byte token, as adbd expects (SHA-1 DigestInfo prefix). */
    fun sign(token: ByteArray): ByteArray {
        val k = 256
        val t = SHA1_PREFIX + token
        val block = ByteArray(k)
        block[0] = 0
        block[1] = 1
        for (i in 2 until k - t.size - 1) block[i] = 0xFF.toByte()
        block[k - t.size - 1] = 0
        System.arraycopy(t, 0, block, k - t.size, t.size)
        val cipher = Cipher.getInstance("RSA/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, pair.private)
        return cipher.doFinal(block)
    }

    /** Public key in Android's `adbkey.pub` format: base64(RSAPublicKey struct) + " name\0". */
    fun adbPublicKey(): ByteArray {
        val pub = pair.public as RSAPublicKey
        val n = pub.modulus
        val words = 64
        val r32 = BigInteger.ONE.shiftLeft(32)
        val n0inv = n.mod(r32).modInverse(r32).negate().mod(r32)
        val rr = BigInteger.ONE.shiftLeft(32 * words * 2).mod(n)
        val buf = ByteBuffer.allocate(4 + 4 + words * 4 * 2 + 4).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(words)
        buf.putInt(n0inv.toInt())
        putWords(buf, n, words)
        putWords(buf, rr, words)
        buf.putInt(pub.publicExponent.toInt())
        val b64 = Base64.encodeToString(buf.array(), Base64.NO_WRAP)
        return "$b64 nova@NOVA-Launcher\u0000".toByteArray()
    }

    private fun putWords(buf: ByteBuffer, v: BigInteger, words: Int) {
        val mask = BigInteger.valueOf(0xFFFFFFFFL)
        for (i in 0 until words) buf.putInt(v.shiftRight(32 * i).and(mask).toLong().toInt())
    }

    companion object {
        private val SHA1_PREFIX = byteArrayOf(
            0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x0e, 0x03, 0x02, 0x1a, 0x05, 0x00, 0x04, 0x14,
        )

        fun load(context: Context): AdbKeys {
            val dir = File(context.filesDir, "adb").apply { mkdirs() }
            val priv = File(dir, "adbkey")
            val pub = File(dir, "adbkey.pub.der")
            val kf = KeyFactory.getInstance("RSA")
            if (priv.exists() && pub.exists()) {
                runCatching {
                    return AdbKeys(
                        KeyPair(
                            kf.generatePublic(X509EncodedKeySpec(pub.readBytes())),
                            kf.generatePrivate(PKCS8EncodedKeySpec(priv.readBytes())),
                        )
                    )
                }
            }
            val gen = KeyPairGenerator.getInstance("RSA")
            gen.initialize(2048)
            val pair = gen.generateKeyPair()
            priv.writeBytes(pair.private.encoded)
            pub.writeBytes(pair.public.encoded)
            return AdbKeys(pair)
        }
    }
}
