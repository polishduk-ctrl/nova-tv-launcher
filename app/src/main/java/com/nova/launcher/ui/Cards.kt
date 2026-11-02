package com.nova.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.nova.launcher.R
import com.nova.launcher.data.AppEntry
import com.nova.launcher.data.Featured

private fun Featured.art(): Int = when (this) {
    Featured.NUVIO -> R.drawable.art_nuvio
    Featured.YOUTUBE -> R.drawable.art_youtube
    Featured.NETFLIX -> R.drawable.art_netflix
    Featured.PRIME_VIDEO -> R.drawable.art_prime
}

fun Featured.accent(): Color = when (this) {
    Featured.NUVIO -> Nova.Violet
    Featured.YOUTUBE -> Nova.Pink
    Featured.NETFLIX -> Nova.Crimson
    Featured.PRIME_VIDEO -> Nova.Cyan
}

/** Focus + remote-press wiring shared by every card. */
@Composable
private fun rememberCardFocus(): Pair<PressState, androidx.compose.runtime.MutableState<Boolean>> =
    remember { PressState() } to remember { mutableStateOf(false) }

private val TextShade = Brush.verticalGradient(0.35f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.72f))

/** Hero favourite: bespoke artwork, the app's own icon, name and a short line. */
@Composable
fun FeaturedCard(
    featured: Featured,
    app: AppEntry?,
    width: Dp,
    height: Dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    focusRequester: FocusRequester? = null,
) {
    val (press, focusedState) = rememberCardFocus()
    val focused by focusedState
    val installed = app != null
    GlassSurface(
        focused = focused, pressed = press.pressed, accent = featured.accent(), corner = 24.dp,
        modifier = Modifier
            .zIndex(if (focused) 1f else 0f)
            .size(width, height)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focusedState.value = it.isFocused }
            .dpadActions(press, onClick, onLongClick)
            .focusable(),
    ) {
        Image(
            painterResource(featured.art()), null,
            contentScale = ContentScale.Crop,
            alpha = if (installed) 0.95f else 0.4f,
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().drawBehind { drawRect(TextShade) })
        Row(
            Modifier.align(Alignment.BottomStart).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(app?.icon, 30.dp, dim = !installed)
            Spacer(Modifier.width(10.dp))
            Column {
                BasicText(featured.title, style = NovaType.CardTitle, maxLines = 1)
                BasicText(
                    if (installed) featured.tagline else "Not installed — select to get it",
                    style = NovaType.CardSub, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** A favourite the user pinned: its TV banner, or its icon over tinted artwork. */
@Composable
fun PinnedCard(app: AppEntry, width: Dp, height: Dp, onClick: () -> Unit, onLongClick: () -> Unit) {
    val (press, focusedState) = rememberCardFocus()
    val focused by focusedState
    GlassSurface(
        focused = focused, pressed = press.pressed, accent = app.accent, corner = 24.dp,
        modifier = Modifier
            .zIndex(if (focused) 1f else 0f)
            .size(width, height)
            .onFocusChanged { focusedState.value = it.isFocused }
            .dpadActions(press, onClick, onLongClick)
            .focusable(),
    ) {
        val banner = app.banner
        if (banner != null) {
            Image(banner, app.label, contentScale = ContentScale.Crop, filterQuality = FilterQuality.Medium,
                modifier = Modifier.fillMaxSize().padding(4.dp).clip(RoundedCornerShape(20.dp)))
        } else {
            Image(
                painterResource(R.drawable.art_generic), null, contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.tint(app.accent, BlendMode.Modulate), modifier = Modifier.fillMaxSize(),
            )
            Box(Modifier.fillMaxSize().drawBehind { drawRect(TextShade) })
            Row(Modifier.align(Alignment.BottomStart).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                AppIcon(app.icon, 30.dp)
                Spacer(Modifier.width(10.dp))
                BasicText(app.label, style = NovaType.CardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Library tile: the app's banner framed in glass, or its icon on frosted glass. */
@Composable
fun AppTile(
    app: AppEntry,
    width: Dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val (press, focusedState) = rememberCardFocus()
    val focused by focusedState
    Column(
        Modifier.width(width).zIndex(if (focused) 1f else 0f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GlassSurface(
            focused = focused, pressed = press.pressed, accent = app.accent, corner = 16.dp, lift = 1.08f,
            modifier = Modifier
                .fillMaxWidth()
                .height(width * 9f / 16f)
                .onFocusChanged { focusedState.value = it.isFocused }
                .dpadActions(press, onClick, onLongClick)
                .focusable(),
        ) {
            val banner = app.banner
            if (banner != null) {
                Image(
                    banner, app.label, contentScale = ContentScale.Crop, filterQuality = FilterQuality.Medium,
                    modifier = Modifier.fillMaxSize().padding(3.dp).clip(RoundedCornerShape(13.dp)),
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    app.icon?.let { Image(it, app.label, Modifier.size(42.dp), filterQuality = FilterQuality.Medium) }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        BasicText(
            app.label,
            style = NovaType.TileLabel.copy(
                color = if (focused) Nova.TextHigh else Nova.TextLow,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun AppIcon(icon: ImageBitmap?, size: Dp, dim: Boolean = false) {
    if (icon == null) {
        Box(
            Modifier.size(size).clip(RoundedCornerShape(size * 0.28f)).drawBehind {
                drawRect(Color.White.copy(alpha = 0.12f))
            }
        )
    } else {
        Image(
            icon, null, filterQuality = FilterQuality.Medium, alpha = if (dim) 0.5f else 1f,
            modifier = Modifier.size(size),
        )
    }
}

@Composable
fun SectionTitle(title: String, meta: String?, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BasicText(title, style = NovaType.Section)
        if (meta != null) BasicText(meta, style = NovaType.SectionMeta)
    }
}
