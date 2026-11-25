package com.pulse.music.ui.component

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Google Pixel / Material 3 Expressive motion tokens and tactile tap modifiers.
 *
 * All transforms use GPU-backed `graphicsLayer` properties (scale, alpha, translation)
 * so 60/120Hz animations never trigger relayout passes across Android or Desktop.
 */
object PixelMotion {
    // Standard Material 3 Emphasized easings (matching Pixel SystemUI & Launcher curves)
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

    // Pixel Expressive Springs
    val SpringFast = spring<Float>(
        dampingRatio = 0.64f,
        stiffness = 1100f,
    )

    val SpringDefault = spring<Float>(
        dampingRatio = 0.78f,
        stiffness = 440f,
    )

    val SpringBouncy = spring<Float>(
        dampingRatio = 0.56f,
        stiffness = 520f,
    )

    val SpringGentle = spring<Float>(
        dampingRatio = 0.86f,
        stiffness = 280f,
    )

    val SpringEffects = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 1400f,
    )

    val SpringOffsetDefault = spring<IntOffset>(
        dampingRatio = 0.80f,
        stiffness = 440f,
    )

    val SpringOffsetFast = spring<IntOffset>(
        dampingRatio = 0.82f,
        stiffness = 820f,
    )

    val SpringSizeDefault = spring<IntSize>(
        dampingRatio = 0.80f,
        stiffness = 440f,
    )

    val SpringSizeFast = spring<IntSize>(
        dampingRatio = 0.82f,
        stiffness = 820f,
    )

    val SpringDpDefault = spring<Dp>(
        dampingRatio = 0.74f,
        stiffness = 480f,
    )

    val SpringDpBouncy = spring<Dp>(
        dampingRatio = 0.58f,
        stiffness = 500f,
    )

    // -------------------------------------------------------------------------
    // Pixel Navigation Screen Transitions
    // -------------------------------------------------------------------------

    /**
     * Pixel predictive-back / Material Shared Axis X forward navigation enter.
     */
    fun sharedAxisForwardEnter(): EnterTransition =
        slideInHorizontally(
            animationSpec = tween(durationMillis = 360, easing = EmphasizedEasing),
            initialOffsetX = { fullWidth -> (fullWidth * 0.18f).toInt() },
        ) + scaleIn(
            animationSpec = tween(durationMillis = 360, easing = EmphasizedEasing),
            initialScale = 0.94f,
        ) + fadeIn(
            animationSpec = tween(durationMillis = 220, delayMillis = 45, easing = EmphasizedDecelerate),
        )

    /**
     * Pixel predictive-back / Material Shared Axis X forward navigation exit.
     */
    fun sharedAxisForwardExit(): ExitTransition =
        slideOutHorizontally(
            animationSpec = tween(durationMillis = 360, easing = EmphasizedEasing),
            targetOffsetX = { fullWidth -> -(fullWidth * 0.10f).toInt() },
        ) + scaleOut(
            animationSpec = tween(durationMillis = 360, easing = EmphasizedEasing),
            targetScale = 0.96f,
        ) + fadeOut(
            animationSpec = tween(durationMillis = 160, easing = EmphasizedAccelerate),
        )

    /**
     * Pixel predictive-back pop enter (incoming previous screen).
     */
    fun sharedAxisPopEnter(): EnterTransition =
        slideInHorizontally(
            animationSpec = tween(durationMillis = 340, easing = EmphasizedEasing),
            initialOffsetX = { fullWidth -> -(fullWidth * 0.10f).toInt() },
        ) + scaleIn(
            animationSpec = tween(durationMillis = 340, easing = EmphasizedEasing),
            initialScale = 0.96f,
        ) + fadeIn(
            animationSpec = tween(durationMillis = 220, delayMillis = 30, easing = EmphasizedDecelerate),
        )

    /**
     * Pixel predictive-back pop exit (leaving top screen shrinks slightly and slides right).
     */
    fun sharedAxisPopExit(): ExitTransition =
        slideOutHorizontally(
            animationSpec = tween(durationMillis = 320, easing = EmphasizedEasing),
            targetOffsetX = { fullWidth -> (fullWidth * 0.22f).toInt() },
        ) + scaleOut(
            animationSpec = tween(durationMillis = 320, easing = EmphasizedEasing),
            targetScale = 0.92f,
        ) + fadeOut(
            animationSpec = tween(durationMillis = 180, easing = EmphasizedAccelerate),
        )

    /**
     * Pixel Material 3 Fade-Through for top-level bottom navigation & rail tabs.
     */
    fun fadeThroughEnter(): EnterTransition =
        fadeIn(
            animationSpec = tween(durationMillis = 220, delayMillis = 50, easing = EmphasizedDecelerate),
        ) + scaleIn(
            animationSpec = tween(durationMillis = 260, delayMillis = 40, easing = EmphasizedDecelerate),
            initialScale = 0.94f,
        )

    fun fadeThroughExit(): ExitTransition =
        fadeOut(
            animationSpec = tween(durationMillis = 110, easing = EmphasizedAccelerate),
        ) + scaleOut(
            animationSpec = tween(durationMillis = 130, easing = EmphasizedAccelerate),
            targetScale = 0.97f,
        )

    /**
     * Vertical Shared Axis Y expand (for Fullscreen player, Wrapped, modal sheets).
     */
    fun verticalExpandEnter(): EnterTransition =
        slideInVertically(
            animationSpec = tween(durationMillis = 380, easing = EmphasizedEasing),
            initialOffsetY = { fullHeight -> (fullHeight * 0.14f).toInt() },
        ) + scaleIn(
            animationSpec = tween(durationMillis = 380, easing = EmphasizedEasing),
            initialScale = 0.93f,
            transformOrigin = TransformOrigin(0.5f, 0.7f),
        ) + fadeIn(
            animationSpec = tween(durationMillis = 240, easing = EmphasizedDecelerate),
        )

    fun verticalExpandExit(): ExitTransition =
        slideOutVertically(
            animationSpec = tween(durationMillis = 300, easing = EmphasizedEasing),
            targetOffsetY = { fullHeight -> (fullHeight * 0.14f).toInt() },
        ) + scaleOut(
            animationSpec = tween(durationMillis = 300, easing = EmphasizedEasing),
            targetScale = 0.93f,
            transformOrigin = TransformOrigin(0.5f, 0.7f),
        ) + fadeOut(
            animationSpec = tween(durationMillis = 180, easing = EmphasizedAccelerate),
        )

    /**
     * Pixel Expressive icon toggle / morph content transform (Play/Pause, Repeat, Shuffle, Like).
     */
    fun iconMorphTransition(): ContentTransform =
        (
            fadeIn(tween(150, easing = EmphasizedDecelerate)) +
                scaleIn(spring(dampingRatio = 0.62f, stiffness = 950f), initialScale = 0.58f)
        ).togetherWith(
            fadeOut(tween(100, easing = EmphasizedAccelerate)) +
                scaleOut(tween(110, easing = EmphasizedAccelerate), targetScale = 0.68f),
        )

    /**
     * Pixel Shared Axis Y track-info text transition (smoothly slides/fades on song skip).
     */
    fun trackChangeTransition(forward: Boolean = true): ContentTransform {
        val dir = if (forward) 1 else -1
        return (
            slideInVertically(tween(260, easing = EmphasizedDecelerate)) { dir * (it / 2).coerceAtLeast(18) } +
                fadeIn(tween(220, delayMillis = 40, easing = EmphasizedDecelerate))
        ).togetherWith(
            slideOutVertically(tween(180, easing = EmphasizedAccelerate)) { -dir * (it / 2).coerceAtLeast(18) } +
                fadeOut(tween(150, easing = EmphasizedAccelerate)),
        )
    }

    /**
     * Expressive Fade-Through transform for AnimatedContent containers (Search panes, tabs, states).
     */
    fun fadeThroughContentTransform(): ContentTransform =
        fadeThroughEnter().togetherWith(fadeThroughExit())
}

/**
 * Pixel tactile press & hover scale effect.
 *
 * Applies a responsive spring squish when pressed and a gentle lift on pointer hover
 * using GPU `graphicsLayer`, without altering measure/layout bounds.
 */
fun Modifier.pixelTapScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.95f,
    hoveredScale: Float = 1.015f,
    enabled: Boolean = true,
): Modifier =
    composed {
        val isPressed by interactionSource.collectIsPressedAsState()
        val isHovered by interactionSource.collectIsHoveredAsState()
        val targetScale =
            when {
                !enabled -> 1f
                isPressed -> pressedScale
                isHovered -> hoveredScale
                else -> 1f
            }
        val scale by animateFloatAsState(
            targetValue = targetScale,
            animationSpec = PixelMotion.SpringFast,
            label = "pixelTapScale",
        )
        this
            .hoverable(interactionSource = interactionSource, enabled = enabled)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    }

/**
 * Standalone spring squish clickable modifier with Pixel micro-haptics, hover lift,
 * spring scale-down on tap, and Material 3 ripple.
 */
fun Modifier.pixelClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.955f,
    hoveredScale: Float = 1.015f,
    hapticOnTap: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    indication: Indication? = null,
    useDefaultIndication: Boolean = true,
    onClick: () -> Unit,
): Modifier =
    composed {
        val interactionSource = remember { MutableInteractionSource() }
        val haptic = LocalHapticFeedback.current
        val resolvedIndication = if (useDefaultIndication) (indication ?: LocalIndication.current) else indication

        this
            .pixelTapScale(
                interactionSource = interactionSource,
                pressedScale = pressedScale,
                hoveredScale = hoveredScale,
                enabled = enabled,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = resolvedIndication,
                enabled = enabled,
                onClickLabel = onClickLabel,
                role = role,
                onClick = {
                    if (hapticOnTap) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onClick()
                },
            )
    }

/**
 * Combined clickable with Pixel spring squish on press, hover lift, and haptic feedback on click & long-click.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pixelCombinedClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.955f,
    hoveredScale: Float = 1.015f,
    hapticOnTap: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    indication: Indication? = null,
    useDefaultIndication: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier =
    composed {
        val interactionSource = remember { MutableInteractionSource() }
        val haptic = LocalHapticFeedback.current
        val resolvedIndication = if (useDefaultIndication) (indication ?: LocalIndication.current) else indication

        this
            .pixelTapScale(
                interactionSource = interactionSource,
                pressedScale = pressedScale,
                hoveredScale = hoveredScale,
                enabled = enabled,
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = resolvedIndication,
                enabled = enabled,
                onClickLabel = onClickLabel,
                role = role,
                onLongClick =
                    onLongClick?.let { longClick ->
                        {
                            if (hapticOnTap) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            longClick()
                        }
                    },
                onDoubleClick = onDoubleClick,
                onClick = {
                    if (hapticOnTap) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onClick()
                },
            )
    }

/**
 * Pixel Expressive corner-radius morphing clip driven by [interactionSource] press state
 * or [isActive] selection state (e.g., pill <-> soft squircle).
 */
fun Modifier.pixelMorphShape(
    interactionSource: MutableInteractionSource,
    defaultRadius: Dp = 16.dp,
    pressedRadius: Dp = 24.dp,
    activeRadius: Dp = pressedRadius,
    isActive: Boolean = false,
): Modifier =
    composed {
        val isPressed by interactionSource.collectIsPressedAsState()
        val targetRadius =
            when {
                isPressed -> pressedRadius
                isActive -> activeRadius
                else -> defaultRadius
            }
        val radius by animateDpAsState(
            targetValue = targetRadius,
            animationSpec = PixelMotion.SpringDpDefault,
            label = "pixelMorphRadius",
        )
        this.clip(RoundedCornerShape(radius))
    }

/**
 * Pixel M3 Expressive spring pop when a state value toggles or changes.
 * Ideal for Play/Pause, Like, Shuffle, Repeat, and Navigation tab icons.
 */
fun Modifier.pixelPopOnChange(
    triggerKey: Any?,
    peakScale: Float = 1.2f,
    onlyWhen: Boolean = true,
): Modifier =
    composed {
        val scale = remember { Animatable(1f) }
        val firstComposition = remember { booleanArrayOf(true) }
        LaunchedEffect(triggerKey) {
            if (firstComposition[0]) {
                firstComposition[0] = false
                return@LaunchedEffect
            }
            if (onlyWhen) {
                scale.snapTo(1f)
                scale.animateTo(
                    targetValue = peakScale,
                    animationSpec = tween(durationMillis = 85, easing = FastOutSlowInEasing),
                )
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = PixelMotion.SpringBouncy,
                )
            }
        }
        this.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
    }

/**
 * Subtle staggered Pixel entrance animation when items first enter composition.
 * Uses GPU-only alpha, translationY, and micro-scale to stay silky-smooth at 120Hz.
 */
fun Modifier.pixelStaggerEntrance(
    index: Int = 0,
    key: Any? = Unit,
    maxStaggerIndex: Int = 8,
    staggerDelayMs: Int = 24,
    slideOffsetPx: Float = 26f,
    initialScale: Float = 0.96f,
): Modifier =
    composed {
        val alphaAnim = remember(key) { Animatable(0f) }
        val progressAnim = remember(key) { Animatable(0f) }

        LaunchedEffect(key) {
            val clampedIndex = index.coerceIn(0, maxStaggerIndex)
            if (clampedIndex > 0) {
                delay((clampedIndex * staggerDelayMs).toLong())
            }
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 220, easing = PixelMotion.EmphasizedDecelerate),
            )
        }

        LaunchedEffect(key) {
            val clampedIndex = index.coerceIn(0, maxStaggerIndex)
            if (clampedIndex > 0) {
                delay((clampedIndex * staggerDelayMs).toLong())
            }
            progressAnim.animateTo(
                targetValue = 1f,
                animationSpec = PixelMotion.SpringDefault,
            )
        }

        this.graphicsLayer {
            val p = progressAnim.value
            alpha = alphaAnim.value
            translationY = (1f - p) * slideOffsetPx
            val s = initialScale + (1f - initialScale) * p
            scaleX = s
            scaleY = s
        }
    }

/**
 * Google Pixel elastic edge-stretch overscroll physics for vertical scrollable lists and grids.
 * Safely coexists with PullToRefreshBox by observing unconsumed boundary scroll/fling energy
 * without consuming downward pull deltas.
 */
fun Modifier.pixelVerticalOverscroll(
    enabled: Boolean = true,
    maxStretchFraction: Float = 0.042f,
    maxTranslationPx: Float = 26f,
): Modifier =
    composed {
        if (!enabled) return@composed this
        val overscrollAnim = remember { Animatable(0f) }
        val scope = rememberCoroutineScope()
        val connection = remember(scope) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    val cur = overscrollAnim.value
                    if (source == NestedScrollSource.UserInput && cur != 0f) {
                        if ((cur > 0f && available.y < 0f) || (cur < 0f && available.y > 0f)) {
                            val next = cur + available.y * 0.45f
                            val clamped = if ((cur > 0f && next <= 0f) || (cur < 0f && next >= 0f)) 0f else next
                            val consumedY = (clamped - cur) / 0.45f
                            scope.launch { overscrollAnim.snapTo(clamped) }
                            return Offset(0f, consumedY)
                        }
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    if (source == NestedScrollSource.UserInput && available.y != 0f) {
                        val delta = available.y * 0.28f
                        val next = (overscrollAnim.value + delta).coerceIn(-110f, 110f)
                        scope.launch { overscrollAnim.snapTo(next) }
                    }
                    return Offset.Zero
                }

                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                    if (overscrollAnim.value != 0f || abs(available.y) > 220f) {
                        val impulse = (available.y * 0.012f).coerceIn(-52f, 52f)
                        if (overscrollAnim.value == 0f && impulse != 0f) {
                            overscrollAnim.snapTo(impulse)
                        }
                        overscrollAnim.animateTo(0f, PixelMotion.SpringBouncy)
                    }
                    return Velocity.Zero
                }
            }
        }
        this
            .nestedScroll(connection)
            .graphicsLayer {
                val v = overscrollAnim.value
                if (v != 0f) {
                    val norm = (abs(v) / 110f).coerceIn(0f, 1f)
                    transformOrigin = if (v > 0f) TransformOrigin(0.5f, 0f) else TransformOrigin(0.5f, 1f)
                    scaleY = 1f + maxStretchFraction * norm
                    scaleX = 1f - (maxStretchFraction * 0.16f * norm)
                    translationY = (v / 110f) * maxTranslationPx
                }
            }
    }

/**
 * 120Hz zero-recomposition Pixel viewport-edge scroll kinematics for LazyColumn/LazyRow items.
 * Gently scales and softens items only as they cross into the top/bottom edges of the viewport.
 */
fun Modifier.pixelScrollScale(
    listState: LazyListState,
    key: Any,
    minScale: Float = 0.96f,
    minAlpha: Float = 0.88f,
): Modifier =
    this.graphicsLayer {
        val layoutInfo = listState.layoutInfo
        val viewportStart = layoutInfo.viewportStartOffset.toFloat()
        val viewportEnd = layoutInfo.viewportEndOffset.toFloat()
        val viewportSpan = (viewportEnd - viewportStart).coerceAtLeast(1f)
        val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } ?: return@graphicsLayer

        val edgeBand = (viewportSpan * 0.16f).coerceIn(48f, 150f)
        val itemTop = itemInfo.offset.toFloat()
        val itemBottom = (itemInfo.offset + itemInfo.size).toFloat()

        val visibility = when {
            itemBottom <= viewportStart || itemTop >= viewportEnd -> 0f
            itemTop < viewportStart -> ((itemBottom - viewportStart) / edgeBand).coerceIn(0f, 1f)
            itemBottom > viewportEnd -> ((viewportEnd - itemTop) / edgeBand).coerceIn(0f, 1f)
            else -> 1f
        }
        val eased = PixelMotion.EmphasizedDecelerate.transform(visibility)
        val s = minScale + (1f - minScale) * eased
        scaleX = s
        scaleY = s
        alpha = minAlpha + (1f - minAlpha) * eased
    }

/**
 * 120Hz zero-recomposition Pixel viewport-edge scroll kinematics for LazyVerticalGrid tiles.
 */
fun Modifier.pixelGridScrollScale(
    gridState: LazyGridState,
    key: Any,
    minScale: Float = 0.95f,
    minAlpha: Float = 0.88f,
): Modifier =
    this.graphicsLayer {
        val layoutInfo = gridState.layoutInfo
        val viewportStart = layoutInfo.viewportStartOffset.toFloat()
        val viewportEnd = layoutInfo.viewportEndOffset.toFloat()
        val viewportSpan = (viewportEnd - viewportStart).coerceAtLeast(1f)
        val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } ?: return@graphicsLayer

        val edgeBand = (viewportSpan * 0.16f).coerceIn(48f, 140f)
        val itemTop = itemInfo.offset.y.toFloat()
        val itemBottom = (itemInfo.offset.y + itemInfo.size.height).toFloat()

        val visibility = when {
            itemBottom <= viewportStart || itemTop >= viewportEnd -> 0f
            itemTop < viewportStart -> ((itemBottom - viewportStart) / edgeBand).coerceIn(0f, 1f)
            itemBottom > viewportEnd -> ((viewportEnd - itemTop) / edgeBand).coerceIn(0f, 1f)
            else -> 1f
        }
        val eased = PixelMotion.EmphasizedDecelerate.transform(visibility)
        val s = minScale + (1f - minScale) * eased
        scaleX = s
        scaleY = s
        alpha = minAlpha + (1f - minAlpha) * eased
    }
