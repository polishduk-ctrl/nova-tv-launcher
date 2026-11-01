package com.nova.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nova.launcher.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

private const val COLUMNS = 6

@Composable
fun HomeScreen(viewModel: HomeViewModel, homeSignal: Int) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var ambientTarget by remember { mutableStateOf(Nova.Violet) }
    val ambient = animateColorAsState(ambientTarget, tween(1200), label = "ambient")
    val listState = rememberLazyListState()
    val firstFocus = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    var menu by remember { mutableStateOf<MenuTarget?>(null) }
    val intro = remember { Animatable(0f) }
    var initialFocusDone by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { intro.animateTo(1f, tween(1500, easing = FastOutSlowInEasing)) }
    LaunchedEffect(state.loading) {
        if (!state.loading && !initialFocusDone) {
            delay(80)
            initialFocusDone = runCatching { firstFocus.requestFocus() }.isSuccess
        }
    }

    suspend fun goHome() {
        menu = null
        listState.animateScrollToItem(0)
        delay(30)
        runCatching { firstFocus.requestFocus() }
    }
    LaunchedEffect(homeSignal) { if (homeSignal > 0) goHome() }
    // A launcher must never close on Back: return to the top instead.
    BackHandler { scope.launch { goHome() } }

    Box(Modifier.fillMaxSize()) {
        CosmicBackground(ambient)

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val side = 48.dp
            val content = maxWidth - side * 2
            val favGap = 20.dp
            val favW = (content - favGap * 3) / 4
            val favH = favW * 0.64f
            val gap = 18.dp
            val tileW = (content - gap * (COLUMNS - 1)) / COLUMNS
            val onFocused: (androidx.compose.ui.graphics.Color) -> Unit = { ambientTarget = it }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 30.dp, bottom = 60.dp),
            ) {
                item(key = "header") {
                    NovaHeader(
                        showHomeSetup = state.showHomeSetup,
                        hasClassicHome = state.hasClassicHome,
                        onSettings = viewModel::openSettings,
                        onClassicHome = viewModel::openClassicHome,
                        onHomeSetup = viewModel::openHomeSetup,
                        modifier = Modifier.padding(horizontal = side).intro(intro, 0),
                    )
                }
                item(key = "fav-title") {
                    SectionTitle("FAVOURITES", null, Modifier.padding(start = side, top = 22.dp).intro(intro, 1))
                }
                item(key = "favourites") {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().intro(intro, 1),
                        contentPadding = PaddingValues(horizontal = side, vertical = 26.dp),
                        horizontalArrangement = Arrangement.spacedBy(favGap),
                    ) {
                        itemsIndexed(state.featured, key = { _, s -> "hero:" + s.featured.name }) { i, slot ->
                            FeaturedCard(
                                featured = slot.featured,
                                app = slot.app,
                                width = favW, height = favH,
                                focusRequester = if (i == 0) firstFocus else null,
                                onFocused = onFocused,
                                onClick = {
                                    val app = slot.app
                                    if (app != null) viewModel.launch(app) else viewModel.findApp(slot.featured)
                                },
                                onLongClick = { slot.app?.let { menu = MenuTarget(it, isHero = true, isPinned = true) } },
                            )
                        }
                        items(state.pinned, key = { "pin:" + it.packageName }) { app ->
                            PinnedCard(
                                app = app, width = favW, height = favH, onFocused = onFocused,
                                onClick = { viewModel.launch(app) },
                                onLongClick = { menu = MenuTarget(app, isHero = false, isPinned = true) },
                            )
                        }
                    }
                }
                item(key = "lib-title") {
                    SectionTitle(
                        "LIBRARY",
                        if (state.loading) null else "${state.apps.size} APPS",
                        Modifier.padding(start = side, top = 4.dp, bottom = 8.dp).intro(intro, 2),
                    )
                }
                val rows = state.apps.chunked(COLUMNS)
                itemsIndexed(rows, key = { _, row -> "row:" + row.first().key }) { i, row ->
                    Row(
                        Modifier.padding(horizontal = side, vertical = 11.dp).intro(intro, 2 + min(i, 3)),
                        horizontalArrangement = Arrangement.spacedBy(gap),
                    ) {
                        row.forEach { app ->
                            AppTile(
                                app = app, width = tileW, onFocused = onFocused,
                                onClick = { viewModel.launch(app) },
                                onLongClick = {
                                    menu = MenuTarget(app, isHero = false, isPinned = app.packageName in state.pinnedPackages)
                                },
                            )
                        }
                    }
                }
                if (!state.loading && state.apps.isEmpty()) {
                    item(key = "empty") {
                        BasicText(
                            "No launchable apps were found.",
                            style = NovaType.Greeting.copy(textAlign = TextAlign.Center),
                            modifier = Modifier.fillMaxWidth().padding(40.dp),
                        )
                    }
                }
                item(key = "hint") {
                    BasicText(
                        "Hold OK on any app for options  ·  Back returns to the top",
                        style = NovaType.SectionCount.copy(textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxWidth().padding(top = 28.dp).intro(intro, 5),
                    )
                }
            }
        }

        menu?.let { target ->
            AppMenu(
                target = target,
                onDismiss = { menu = null },
                onOpen = { menu = null; viewModel.launch(target.app) },
                onTogglePin = { menu = null; viewModel.togglePinned(target.app) },
                onInfo = { menu = null; viewModel.openAppInfo(target.app) },
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String, count: String?, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(6.dp).drawBehind {
                drawCircle(Brush.linearGradient(listOf(Nova.Cyan, Nova.Violet), Offset.Zero, Offset(size.width, size.height)))
            }
        )
        Spacer(Modifier.width(10.dp))
        BasicText(title, style = NovaType.Section)
        if (count != null) {
            Spacer(Modifier.width(12.dp))
            BasicText(count, style = NovaType.SectionCount)
        }
    }
}

/** Staggered entrance: each section rises and fades in slightly after the previous one. */
private fun Modifier.intro(progress: Animatable<Float, AnimationVector1D>, index: Int): Modifier = graphicsLayer {
    val local = ((progress.value - index * 0.12f) / 0.55f).coerceIn(0f, 1f)
    alpha = local
    translationY = (1f - local) * 28.dp.toPx()
}
