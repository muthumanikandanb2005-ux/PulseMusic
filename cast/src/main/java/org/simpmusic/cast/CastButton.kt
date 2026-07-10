package org.simpmusic.cast

import android.view.ContextThemeWrapper
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.R as MediaRouterR
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory

/**
 * Cast icon button. No-op (renders nothing) when Cast isn't available.
 *
 * MediaRouteButton requires an AppCompat-derived theme to inflate correctly;
 * the host Activity/Application theme in this Compose-first app isn't
 * guaranteed to be one, so the factory context is wrapped with mediarouter's
 * own `Theme.MediaRouter` to avoid a "You need to use a Theme.AppCompat theme"
 * crash.
 */
@Composable
fun CastIconButton(modifier: Modifier = Modifier) {
    if (!isCastAvailable()) return

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val themedContext = ContextThemeWrapper(context, MediaRouterR.style.Theme_MediaRouter)
            MediaRouteButton(themedContext).apply {
                CastButtonFactory.setUpMediaRouteButton(context.applicationContext, this)
            }
        },
    )
}
