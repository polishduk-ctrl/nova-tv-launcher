package com.nova.launcher.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import com.nova.launcher.HomeBypass
import com.nova.launcher.MainActivity

/**
 * Optional helper for Google TV devices, which don't let you choose a different default Home
 * app. When the TV's own launcher comes to the foreground, NOVA is brought back on top.
 * It only reacts to window changes; it never reads screen content or intercepts keys.
 * Disabling it in Accessibility settings restores the original behaviour instantly.
 */
class HomeButtonService : AccessibilityService() {

    private var stockHomes: Set<String> = emptySet()
    private var lastLaunch = 0L

    override fun onServiceConnected() {
        stockHomes = findStockHomes()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        if (pkg !in stockHomes) {
            // The user launched something from the classic launcher: next Home returns to NOVA.
            if (HomeBypass.active && pkg != "com.android.systemui" && !pkg.contains("inputmethod")) {
                HomeBypass.active = false
            }
            return
        }
        if (HomeBypass.active) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastLaunch < 800) return
        lastLaunch = now
        val intent = Intent(this, MainActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        )
        runCatching { startActivity(intent) }
    }

    override fun onInterrupt() = Unit

    /**
     * Home apps other than NOVA, excluding anything the user can also open as a normal app
     * (such as Settings, which registers a "FallbackHome" activity on some builds).
     */
    private fun findStockHomes(): Set<String> {
        val pm = packageManager
        @Suppress("DEPRECATION")
        val homes = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0
        ).map { it.activityInfo.packageName }.toSet()
        @Suppress("DEPRECATION")
        val launchable = (pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0
        ) + pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
        )).map { it.activityInfo.packageName }.toSet()
        return homes.filterTo(HashSet()) {
            it != packageName && it !in launchable &&
                !it.contains("settings", true) && !it.contains("setup", true)
        }
    }

    companion object {
        fun isEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val me = ComponentName(context, HomeButtonService::class.java)
            return enabled.split(':').any {
                ComponentName.unflattenFromString(it) == me
            }
        }
    }
}
