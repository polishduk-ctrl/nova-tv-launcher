package com.nova.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.mutableIntStateOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.nova.launcher.ui.HomeScreen

class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels()

    /** Incremented when Home is pressed while NOVA is already in front. */
    private val homeSignal = mutableIntStateOf(0)

    /** Incremented on every resume (used to restore focus after returning from an app). */
    private val resumeSignal = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContent {
            HomeScreen(
                viewModel = viewModel,
                homeSignal = homeSignal.intValue,
                resumeSignal = resumeSignal.intValue,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) homeSignal.intValue++
    }

    override fun onResume() {
        super.onResume()
        HomeBypass.active = false
        resumeSignal.intValue++
        viewModel.refresh()
    }
}
