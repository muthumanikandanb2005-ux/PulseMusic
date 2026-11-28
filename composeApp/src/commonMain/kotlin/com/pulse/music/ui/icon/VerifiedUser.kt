package com.pulse.music.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
val PulseIcons.VerifiedUser: ImageVector
    get() {
        if (_VerifiedUser != null) {
            return _VerifiedUser!!
        }
        _VerifiedUser =
            ImageVector.Builder(
                name = "VerifiedUser",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    stroke = null,
                    strokeAlpha = 1f,
                    strokeLineWidth = 1f,
                    strokeLineCap = StrokeCap.Butt,
                    strokeLineJoin = StrokeJoin.Bevel,
                    strokeLineMiter = 1f,
                    pathFillType = PathFillType.NonZero,
                ) {
                    moveTo(12f, 1f)
                    lineTo(3f, 5f)
                    verticalLineToRelative(6f)
                    curveToRelative(0f, 5.55f, 3.84f, 10.74f, 9f, 12f)
                    curveToRelative(5.16f, -1.26f, 9f, -6.45f, 9f, -12f)
                    verticalLineTo(5f)
                    lineTo(12f, 1f)
                    close()
                    moveTo(10f, 17f)
                    lineToRelative(-4f, -4f)
                    lineToRelative(1.41f, -1.41f)
                    lineTo(10f, 14.17f)
                    lineToRelative(6.59f, -6.59f)
                    lineTo(18f, 9f)
                    lineToRelative(-8f, 8f)
                    close()
                }
            }.build()
        return _VerifiedUser!!
    }

private var _VerifiedUser: ImageVector? = null
