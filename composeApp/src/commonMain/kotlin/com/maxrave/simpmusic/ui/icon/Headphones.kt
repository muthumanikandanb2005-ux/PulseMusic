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
val SimpIcons.Headphones: ImageVector
    get() {
        if (_Headphones != null) {
            return _Headphones!!
        }
        _Headphones =
            ImageVector.Builder(
                name = "Headphones",
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
                    moveTo(12f, 3f)
                    curveTo(7.03f, 3f, 3f, 7.03f, 3f, 12f)
                    verticalLineToRelative(7f)
                    curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                    horizontalLineToRelative(3f)
                    curveToRelative(0.55f, 0f, 1f, -0.45f, 1f, -1f)
                    verticalLineToRelative(-5f)
                    curveToRelative(0f, -0.55f, -0.45f, -1f, -1f, -1f)
                    horizontalLineTo(5f)
                    verticalLineToRelative(-2f)
                    curveToRelative(0f, -3.87f, 3.13f, -7f, 7f, -7f)
                    reflectiveCurveToRelative(7f, 3.13f, 7f, 7f)
                    verticalLineToRelative(2f)
                    horizontalLineToRelative(-3f)
                    curveToRelative(-0.55f, 0f, -1f, 0.45f, -1f, 1f)
                    verticalLineToRelative(5f)
                    curveToRelative(0f, 0.55f, 0.45f, 1f, 1f, 1f)
                    horizontalLineToRelative(3f)
                    curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                    verticalLineToRelative(-7f)
                    curveToRelative(0f, -4.97f, -4.03f, -9f, -9f, -9f)
                    close()
                }
            }.build()
        return _Headphones!!
    }

private var _Headphones: ImageVector? = null
