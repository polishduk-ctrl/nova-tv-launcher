package com.nova.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nova.launcher.HomeViewModel
import com.nova.launcher.R
import com.nova.launcher.data.AppEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val COLUMNS = 5
private val SIDE = 56.dp
private val HEADER_HEIGHT = 112.dp

/** TV-style scrolling: the focused row glides to a fixed line instead of hugging the screen edge. */
@OptIn(ExperimentalFoundationApi::class)
private object PivotScroll : BringIntoViewSpec {
    override val scrollAnimationSpec: AnimationSpec<Float> = tween(320, easing = FastOutSlowInEasing)
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float =
        offset - containerSize * 0.32f
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel, homeSignal: Int, resumeSignal: Int) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val setup by viewModel.setup.collectAsStateWithLifecycle()
    val space = ImageBitmap.imageResource(R.drawable.nova_space)
    val frost = ImageBitmap.imageResource(R.drawable.nova_frost)
    val glow = ImageBitmap.imageResource(R.drawable.nova_glow)
    val images = remember(space, frost, glow) { NovaImages(space, frost, glow) }
    val listState = rememberLazyListState()
    val firstFocus = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    var menu by remember { mutableStateOf<MenuTarget?>(null) }
    var showSetup by remember { mutableStateOf(false) }
    var hasFocus by remember { mutableStateOf(false) }
    val intro = remember { Animatable(0f) }

    LaunchedEffect(Unit) { intro.animateTo(1f, tween(450, easing = FastOutSlowInEasing)) }
    LaunchedEffect(state.loading) {
        if (!state.loading) {
            delay(50)
            runCatching { firstFocus.requestFocus() }
        }
    }
    // Coming back from an app: if focus was lost, put it back on the first favourite.
    LaunchedEffect(resumeSignal) {
        delay(120)
        if (!hasFocus && !state.loading) runCatching { firstFocus.requestFocus() }
    }

    suspend fun goHome() {
        menu = null
        showSetup = false
        listState.animateScrollToItem(0)
        delay(30)
        runCatching { firstFocus.requestFocus() }
    }
    LaunchedEffect(homeSignal) { if (homeSignal > 0) goHome() }
    // A launcher must never close on Back: go back to the top instead.
    BackHandler { scope.launch { goHome() } }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Nova.Void)
            .onFocusChanged { hasFocus = it.hasFocus }
    ) {
        val density = LocalDensity.current
        val rootSize = with(density) { Size(maxWidth.toPx(), maxHeight.toPx()) }
        val content = maxWidth - SIDE * 2
        val favGap = 22.dp
        val favW = (content - favGap * 3) / 4
        val favH = favW * 0.64f
        val gap = 20.dp
        val tileW = (content - gap * (COLUMNS - 1)) / COLUMNS
        val defaultScroll = LocalBringIntoViewSpec.current

        CompositionLocalProvider(LocalNovaImages provides images, LocalRootSize provides rootSize) {
            Image(images.space, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds, filterQuality = FilterQuality.Low)

            CompositionLocalProvider(LocalBringIntoViewSpec provides PivotScroll) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().graphicsLayer { alpha = intro.value },
                    contentPadding = PaddingValues(top = HEADER_HEIGHT, bottom = 120.dp),
                ) {
                    item(key = "fav-title") { SectionTitle("Favourites", null, Modifier.padding(start = SIDE, top = 4.dp)) }
                    item(key = "favourites") {
                        CompositionLocalProvider(LocalBringIntoViewSpec provides defaultScroll) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = SIDE, vertical = 30.dp),
                                horizontalArrangement = Arrangement.spacedBy(favGap),
                            ) {
                                itemsIndexed(state.featured, key = { _, s -> "hero:" + s.featured.name }) { i, slot ->
                                    FeaturedCard(
                                        featured = slot.featured, app = slot.app, width = favW, height = favH,
                                        focusRequester = if (i == 0) firstFocus else null,
                                        onClick = {
                                            val app = slot.app
                                            if (app != null) viewModel.launch(app) else viewModel.findApp(slot.featured)
                                        },
                                        onLongClick = { slot.app?.let { menu = MenuTarget(it, isHero = true, isPinned = true) } },
                                    )
                                }
                                items(state.pinned, key = { "pin:" + it.packageName }) { app ->
                                    PinnedCard(
                                        app = app, width = favW, height = favH,
                                        onClick = { viewModel.launch(app) },
                                        onLongClick = { menu = MenuTarget(app, isHero = false, isPinned = true) },
                                    )
                                }
                            }
                        }
                    }
                    appGrid("apps", "Apps", state.tvApps, tileW, gap, state.loading) { app ->
                        AppTile(
                            app = app, width = tileW,
                            onClick = { viewModel.launch(app) },
                            onLongClick = { menu = MenuTarget(app, false, app.packageName in state.pinnedPackages) },
                        )
                    }
                    if (state.otherApps.isNotEmpty()) {
                        appGrid("other", "Phone apps", state.otherApps, tileW, gap, false) { app ->
                            AppTile(
                                app = app, width = tileW,
                                onClick = { viewModel.launch(app) },
                                onLongClick = { menu = MenuTarget(app, false, app.packageName in state.pinnedPackages) },
                            )
                        }
                    }
                    item(key = "hint") {
                        BasicText(
                            "Hold OK on any app for options",
                            style = NovaType.Hint.copy(textAlign = TextAlign.Center),
                            modifier = Modifier.fillMaxWidth().padding(top = 36.dp),
                        )
                    }
                }
            }

            // Header floats above the list; its backdrop is the same sky, fading out at the bottom,
            // so rows slide softly underneath instead of being cut off.
            HeaderBackdrop(images.space, rootSize, HEADER_HEIGHT + 26.dp)
            NovaHeader(
                onSettings = viewModel::openSettings,
                onSetup = { viewModel.clearSetupMessage(); showSetup = true },
                setupHighlighted = !setup.novaIsHome,
                modifier = Modifier.padding(horizontal = SIDE).padding(top = 26.dp).height(64.dp).graphicsLayer { alpha = intro.value },
            )
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
        if (showSetup) {
            SetupPanel(
                setup = setup,
                onDismiss = { showSetup = false },
                onMakeHome = viewModel::makeNovaHome,
                onRestore = viewModel::restoreOriginalHome,
                onDeveloperOptions = viewModel::openDeveloperOptions,
                onClassicHome = { showSetup = false; viewModel.openClassicHome() },
            )
        }
    }
}

private fun LazyListScope.appGrid(
    key: String,
    title: String,
    apps: List<AppEntry>,
    tileW: Dp,
    gap: Dp,
    loading: Boolean,
    tile: @Composable (AppEntry) -> Unit,
) {
    item(key = "$key-title") {
        SectionTitle(title, if (loading) null else "${apps.size}", Modifier.padding(start = SIDE, top = 6.dp, bottom = 10.dp))
    }
    val rows = apps.chunked(COLUMNS)
    items(rows, key = { row -> "$key-row:" + row.first().key }) { row ->
        Row(
            Modifier.padding(horizontal = SIDE, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            row.forEach { tile(it) }
        }
    }
}

@Composable
private fun HeaderBackdrop(space: ImageBitmap, root: Size, height: Dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                val srcH = (space.height * size.height / root.height).roundToInt().coerceIn(1, space.height)
                drawImage(
                    space, srcOffset = IntOffset.Zero, srcSize = IntSize(space.width, srcH),
                    dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                    filterQuality = FilterQuality.Low,
                )
                drawRect(
                    Brush.verticalGradient(0f to Color.Black, 0.62f to Color.Black, 1f to Color.Transparent),
                    blendMode = BlendMode.DstIn,
                )
            }
    )
}
