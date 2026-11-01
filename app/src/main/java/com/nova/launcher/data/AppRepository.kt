package com.nova.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

/** Discovers launchable apps dynamically through the PackageManager. No package IDs are assumed. */
class AppRepository(private val context: Context) {

    private val pm: PackageManager = context.packageManager

    /** Cheap fingerprint of the installed launchable set, used to skip needless reloads. */
    fun fingerprint(): Set<String> =
        resolveAll().map { (ri, _) -> ri.activityInfo.packageName + "/" + ri.activityInfo.name }.toSet()

    suspend fun loadApps(): List<AppEntry> = withContext(Dispatchers.IO) {
        resolveAll()
            .mapNotNull { (ri, tv) -> runCatching { toEntry(ri, tv) }.getOrNull() }
            .sortedBy { it.label.lowercase() }
    }

    /** One entry per package; a TV (leanback) activity wins over a phone-style one. */
    private fun resolveAll(): List<Pair<ResolveInfo, Boolean>> {
        val byPackage = LinkedHashMap<String, Pair<ResolveInfo, Boolean>>()
        query(Intent.CATEGORY_LEANBACK_LAUNCHER).forEach {
            byPackage.putIfAbsent(it.activityInfo.packageName, it to true)
        }
        query(Intent.CATEGORY_LAUNCHER).forEach {
            byPackage.putIfAbsent(it.activityInfo.packageName, it to false)
        }
        byPackage.remove(context.packageName)
        return byPackage.values.toList()
    }

    private fun query(category: String): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
        return runCatching {
            if (Build.VERSION.SDK_INT >= 33) {
                pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, 0)
            }
        }.getOrDefault(emptyList())
    }

    private fun toEntry(ri: ResolveInfo, tv: Boolean): AppEntry {
        val ai = ri.activityInfo
        val label = ri.loadLabel(pm)?.toString()?.trim().orEmpty().ifEmpty { ai.packageName }
        val iconDrawable: Drawable? = runCatching { ai.loadIcon(pm) }.getOrNull()
        val bannerDrawable: Drawable? = runCatching {
            ai.loadBanner(pm) ?: ai.applicationInfo?.loadBanner(pm)
        }.getOrNull()

        val iconBitmap = iconDrawable?.let { runCatching { it.toBitmap(192, 192) }.getOrNull() }
        val bannerBitmap = bannerDrawable?.let { d ->
            runCatching {
                val w = 480
                val ratio = if (d.intrinsicWidth > 0 && d.intrinsicHeight > 0)
                    d.intrinsicHeight.toFloat() / d.intrinsicWidth else 9f / 16f
                d.toBitmap(w, max(1, (w * ratio).roundToInt()))
            }.getOrNull()
        }
        return AppEntry(
            packageName = ai.packageName,
            activityName = ai.name,
            label = label,
            icon = iconBitmap?.asImageBitmap(),
            banner = bannerBitmap?.asImageBitmap(),
            accent = iconBitmap?.let(::dominantColor) ?: DEFAULT_ACCENT,
            isTvApp = tv,
        )
    }

    fun launch(app: AppEntry): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            component = ComponentName(app.packageName, app.activityName)
            addCategory(if (app.isTvApp) Intent.CATEGORY_LEANBACK_LAUNCHER else Intent.CATEGORY_LAUNCHER)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }
        if (start(intent)) return true
        val fallback = pm.getLeanbackLaunchIntentForPackage(app.packageName)
            ?: pm.getLaunchIntentForPackage(app.packageName)
        return fallback?.let { start(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } ?: false
    }

    fun start(intent: Intent): Boolean = try {
        if (intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK == 0) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (t: Throwable) {
        false
    }

    /** Other installed Home apps (e.g. the TV's original launcher), for the fallback button. */
    fun otherHomeApps(): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val all = runCatching {
            if (Build.VERSION.SDK_INT >= 33) {
                pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, 0)
            }
        }.getOrDefault(emptyList())
        return all.filter {
            val n = it.activityInfo.name
            it.activityInfo.packageName != context.packageName &&
                !n.contains("FallbackHome") && !it.activityInfo.packageName.contains("setup", true)
        }.sortedByDescending { it.priority }
    }

    companion object {
        val DEFAULT_ACCENT = Color(0xFF8B5CFF)

        /** Saturation-weighted average colour of an icon, brightened for use as light. */
        fun dominantColor(src: Bitmap): Color {
            val small = Bitmap.createScaledBitmap(src, 24, 24, true)
            var r = 0.0; var g = 0.0; var b = 0.0; var wSum = 0.0
            val hsv = FloatArray(3)
            for (y in 0 until small.height) for (x in 0 until small.width) {
                val c = small.getPixel(x, y)
                val a = (c ushr 24) / 255.0
                if (a < 0.5) continue
                android.graphics.Color.colorToHSV(c, hsv)
                val w = a * (0.05 + hsv[1] * hsv[2])
                r += ((c shr 16) and 0xFF) * w; g += ((c shr 8) and 0xFF) * w; b += (c and 0xFF) * w
                wSum += w
            }
            if (small != src) small.recycle()
            if (wSum <= 0.0) return DEFAULT_ACCENT
            val avg = android.graphics.Color.rgb((r / wSum).toInt(), (g / wSum).toInt(), (b / wSum).toInt())
            android.graphics.Color.colorToHSV(avg, hsv)
            if (hsv[1] < 0.18f) return DEFAULT_ACCENT // greyscale icons get NOVA violet
            hsv[1] = hsv[1].coerceAtLeast(0.55f)
            hsv[2] = 1f
            return Color(android.graphics.Color.HSVToColor(hsv))
        }
    }
}
