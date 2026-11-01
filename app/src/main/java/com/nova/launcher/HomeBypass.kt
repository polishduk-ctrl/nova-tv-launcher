package com.nova.launcher

/**
 * Shared, in-process flag. While true, the optional Home button service lets the TV's
 * original launcher stay on screen (the user chose "Classic home" in NOVA).
 */
object HomeBypass {
    @Volatile
    var active: Boolean = false
}
