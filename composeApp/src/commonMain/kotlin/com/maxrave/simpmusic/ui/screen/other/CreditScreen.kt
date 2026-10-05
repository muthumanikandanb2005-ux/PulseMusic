package com.maxrave.simpmusic.ui.screen.other

import androidx.compose.foundation.Image
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.maxrave.simpmusic.expect.openUrl
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.utils.VersionManager
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun CreditScreen(
    paddingValues: PaddingValues,
    navController: NavController,
) {
    val hazeState = rememberHazeState()
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(top = 64.dp)
                .verticalScroll(rememberScrollState())
                .hazeSource(state = hazeState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(30.dp))

        // App icon
        Image(
            painter = painterResource(Res.drawable.app_icon),
            contentDescription = "App Icon",
            modifier =
                Modifier
                    .size(130.dp)
                    .clip(CircleShape),
        )

        Spacer(modifier = Modifier.height(20.dp))

        // App name
        Text(
            text = stringResource(Res.string.app_name),
            style = typo().titleLarge.copy(
                fontWeight = FontWeight.Bold,
            ),
            fontSize = 26.sp,
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Version
        Text(
            text = stringResource(Res.string.version_format, VersionManager.getVersionName()),
            style = typo().bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Developer badge
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Text(
                text = "✨ Owned & Developed by Pulse Music Studio",
                style = typo().bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Secured by Pulse Music Studio badge
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF10B981).copy(alpha = 0.15f),
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Text(
                text = "🛡️ Secured by Pulse Music Studio • 100% Anti-Decompilation & Integrity Protected",
                style = typo().bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                fontSize = 13.sp,
                color = Color(0xFF10B981),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // App description card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
        ) {
            Text(
                text = stringResource(Res.string.credit_app),
                style = typo().bodyMedium,
                modifier = Modifier.padding(20.dp),
                textAlign = TextAlign.Start,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Copyright text
        Text(
            text = stringResource(Res.string.copyright),
            style = typo().bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 25.dp, vertical = 8.dp),
            textAlign = TextAlign.Center,
        )

        // Bottom spacing
        Spacer(modifier = Modifier.height(120.dp))
    }
    TopAppBar(
        modifier =
            Modifier
                .hazeEffect(state = hazeState, style = HazeMaterials.ultraThin()) {
                    blurEnabled = true
                },
        title = {
            Text(
                text = stringResource(Res.string.app_name),
                style = typo().titleMedium,
                maxLines = 1,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(
                            align = Alignment.CenterVertically,
                        ).basicMarquee(
                            iterations = Int.MAX_VALUE,
                            animationMode = MarqueeAnimationMode.Immediately,
                        ).focusable(),
            )
        },
        navigationIcon = {
            Box(Modifier.padding(horizontal = 5.dp)) {
                RippleIconButton(
                    SimpIcons.ArrowBackIosNew,
                    Modifier
                        .size(32.dp),
                    true,
                    tint = MaterialTheme.colorScheme.onSurface,
                ) {
                    navController.navigateUp()
                }
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                Color.Transparent,
                Color.Unspecified,
                Color.Unspecified,
                Color.Unspecified,
                Color.Unspecified,
            ),
    )
}