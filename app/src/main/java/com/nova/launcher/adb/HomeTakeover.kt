package com.nova.launcher.adb

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.nova.launcher.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.SocketTimeoutException

/**
 * Makes NOVA the TV's real Home app by doing, from the TV itself, what is normally done with a
 * computer and `adb`: set NOVA as the preferred Home activity and disable the stock launcher
 * package(s) (Google TV ignores the Home-app setting otherwise). Fully reversible via [restore].
 */
class HomeTakeover(private val context: Context) {

    data class StockHome(val packageName: String, val activityName: String, val label: String)

    private val pm = context.packageManager
    private val prefs = context.getSharedPreferences("nova_home", Context.MODE_PRIVATE)
    private val novaComponent = ComponentName(context, MainActivity::class.java)

    /** Other Home apps on the TV (found at runtime), never including Settings' fallback. */
    fun stockHomes(): List<StockHome> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val flags = PackageManager.MATCH_DISABLED_COMPONENTS
        val list = runCatching {
            if (Build.VERSION.SDK_INT >= 33) {
                pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, flags)
            }
        }.getOrDefault(emptyList())
        return list.map { it.activityInfo }
            .filter { ai ->
                val p = ai.packageName
                p != context.packageName && p != "android" &&
                    !p.contains("settings", ignoreCase = true) && !ai.name.contains("FallbackHome")
            }
            .distinctBy { it.packageName }
            .map { StockHome(it.packageName, it.name, runCatching { it.loadLabel(pm).toString() }.getOrDefault(it.packageName)) }
    }

    fun isNovaDefaultHome(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val ri = runCatching {
            @Suppress("DEPRECATION")
            pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }.getOrNull()
        return ri?.activityInfo?.packageName == context.packageName
    }

    fun isDisabled(pkg: String): Boolean = runCatching {
        val s = pm.getApplicationEnabledSetting(pkg)
        s == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER || s == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }.getOrDefault(false)

    fun disabledByNova(): List<String> =
        prefs.getString(KEY_DISABLED, null)?.split('\n')?.filter { it.isNotBlank() } ?: emptyList()

    suspend fun makeNovaHome(): Result<String> = withContext(Dispatchers.IO) {
        runAdb { adb ->
            val targets = stockHomes()
            // Remember who was Home so "Restore" can put it back.
            val previous = targets.firstOrNull { !isDisabled(it.packageName) }
            if (previous != null && prefs.getString(KEY_RESTORE, null) == null) {
                prefs.edit().putString(KEY_RESTORE, "${previous.packageName}/${previous.activityName}").apply()
            }
            val me = novaComponent.flattenToString()
            adb.shell("cmd package set-home-activity --user 0 $me")
            if (Build.VERSION.SDK_INT >= 29) {
                adb.shell("cmd role add-role-holder --user 0 android.app.role.HOME ${context.packageName}")
            }
            val disabled = disabledByNova().toMutableSet()
            val failed = mutableListOf<String>()
            for (t in targets) {
                val out = adb.shell("pm disable-user --user 0 ${t.packageName}")
                if (out.contains("disabled", ignoreCase = true) || isDisabled(t.packageName)) disabled += t.packageName
                else failed += t.label
            }
            prefs.edit().putString(KEY_DISABLED, disabled.joinToString("\n")).apply()
            if (failed.isEmpty()) "NOVA is now your Home screen. Press Home on the remote to try it."
            else "NOVA is set as Home, but these could not be turned off: ${failed.joinToString()}."
        }
    }

    suspend fun restore(): Result<String> = withContext(Dispatchers.IO) {
        runAdb { adb ->
            val pkgs = (disabledByNova() + stockHomes().map { it.packageName }.filter(::isDisabled)).toSet()
            for (p in pkgs) adb.shell("pm enable $p")
            prefs.getString(KEY_RESTORE, null)?.let { comp ->
                adb.shell("cmd package set-home-activity --user 0 $comp")
                if (Build.VERSION.SDK_INT >= 29) {
                    val pkg = comp.substringBefore('/')
                    adb.shell("cmd role add-role-holder --user 0 android.app.role.HOME $pkg")
                }
            }
            prefs.edit().remove(KEY_DISABLED).remove(KEY_RESTORE).apply()
            "The original launcher is back. Press Home on the remote to return to it."
        }
    }

    private inline fun runAdb(block: (AdbClient) -> String): Result<String> = try {
        AdbClient(AdbKeys.load(context)).use { adb ->
            adb.connect()
            Result.success(block(adb))
        }
    } catch (e: ConnectException) {
        Result.failure(Exception("Couldn't reach the TV's debugging service. Turn on Developer options › USB debugging, then try again."))
    } catch (e: SocketTimeoutException) {
        Result.failure(Exception("The TV didn't answer in time. Try again and choose Allow (tick “Always allow”) when asked."))
    } catch (e: AdbClient.AuthRejected) {
        Result.failure(Exception("The TV refused the connection. Try again and choose Allow (tick “Always allow”)."))
    } catch (e: Exception) {
        Result.failure(Exception(e.message ?: "Something went wrong talking to the TV."))
    }

    private companion object {
        const val KEY_DISABLED = "disabled_packages"
        const val KEY_RESTORE = "restore_component"
    }
}
