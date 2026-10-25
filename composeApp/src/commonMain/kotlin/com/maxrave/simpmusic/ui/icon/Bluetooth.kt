package com.maxrave.simpmusic.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
val SimpIcons.Bluetooth: ImageVector
    get() {
        if (_Bluetooth != null) {
            return _Bluetooth!!
        }
        _Bluetooth =
            ImageVector.Builder(
                name = "Bluetooth",
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
                    pathFillType = PathFillType.Companion.NonZero,
                ) {
                    moveTo(14.88f, 16.29f)
                    lineTo(13f, 14.41f)
                    verticalLineToRelative(4.76f)
                    lineToRelative(1.88f, -2.88f)
                    close()
                    moveTo(13f, 4.83f)
                    lineToRelative(1.88f, 2.88f)
                    lineTo(13f, 9.59f)
                    verticalLineTo(4.83f)
                    close()
                    moveTo(17.71f, 7.71f)
                    lineTo(12.71f, 2.71f)
                    curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.41f, 0f)
                    curveToRelative(-0.2f, 0.2f, -0.3f, 0.45f, -0.3f, 0.71f)
                    verticalLineToRelative(6.59f)
                    lineTo(7.41f, 6.41f)
                    curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.41f, 0f)
                    curveToRelative(-0.39f, 0.39f, -0.39f, 1.02f, 0f, 1.41f)
                    lineTo(10.59f, 12f)
                    lineToRelative(-4.59f, 4.59f)
                    curveToRelative(-0.39f, 0.39f, -0.39f, 1.02f, 0f, 1.41f)
                    curveToRelative(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0f)
                    lineTo(11f, 14f)
                    verticalLineToRelative(6.59f)
                    curveToRelative(0f, 0.55f, 0.45f, 1f, 1f, 1f)
                    curveToRelative(0.26f, 0f, 0.51f, -0.1f, 0.71f, -0.29f)
                    lineToRelative(5f, -5f)
                    curveToRelative(0.39f, -0.39f, 0.39f, -1.02f, 0f, -1.41f)
                    lineTo(13.41f, 12f)
                    lineToRelative(4.29f, -4.29f)
                    curveToRelative(0.39f, -0.39f, 0.39f, -1.02f, 0.01f, -1.41f)
                    close()
                }
            }.build()
        return _Bluetooth!!
    }

private var _Bluetooth: ImageVector? = null
