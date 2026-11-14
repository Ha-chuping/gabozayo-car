package com.gabozayo.car.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** 휴대폰 앱과 같은 선 아이콘 (같은 경로 데이터, 같은 굵기) */
fun ico(d: String): ImageVector = ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
    .addPath(PathParser().parsePathString(d).toNodes(), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round).build()

val IcHome = ico("M3 11l9-8 9 8v10H3z")
val IcHeart = ico("M12 21C5 15.5 3 12 3 8.8C3 6.2 5 4.3 7.5 4.3C9.5 4.3 11.2 5.4 12 7C12.8 5.4 14.5 4.3 16.5 4.3C19 4.3 21 6.2 21 8.8C21 12 19 15.5 12 21Z")
val IcUser = ico("M12 4C14.2 4 16 5.8 16 8C16 10.2 14.2 12 12 12C9.8 12 8 10.2 8 8C8 5.8 9.8 4 12 4ZM4 21C4 17 7.6 14.5 12 14.5C16.4 14.5 20 17 20 21")
val IcCar = ico("M5 15V11l1.8-4.5h10.4L19 11v4M5 15h14M5 15v3h3v-3M16 15v3h3v-3M7.5 11h9")
// 폰 앱에 없는 '옵션'은 같은 선 스타일(둥근 끝, 굵기 2)로 맞춘 슬라이더 모양
val IcOptions = ico("M4 7h9M17 7h3M4 17h3M11 17h9M15 5v4M9 15v4")
val IcPin = ico("M12 21C7 15.5 5 12.5 5 9.5A7 7 0 0 1 19 9.5C19 12.5 17 15.5 12 21ZM12 7.5a2 2 0 1 0 0.01 0z")
val IcClock = ico("M12 3a9 9 0 100 18a9 9 0 000-18zM12 7v5l3 2")
val IcFace = ico("M12 3a9 9 0 100 18a9 9 0 000-18zM8.5 10h.01M15.5 10h.01M8.5 14.5c1 1.2 2.2 1.8 3.5 1.8s2.5-.6 3.5-1.8")
val IcPhone = ico("M8 3h8a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2zM11 18h2")
/** 찜한 곳 표시용 채워진 하트 */
val IcHeartFill: ImageVector = ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
    .addPath(PathParser().parsePathString("M12 21C5 15.5 3 12 3 8.8C3 6.2 5 4.3 7.5 4.3C9.5 4.3 11.2 5.4 12 7C12.8 5.4 14.5 4.3 16.5 4.3C19 4.3 21 6.2 21 8.8C21 12 19 15.5 12 21Z").toNodes(), fill = SolidColor(Color.Black)).build()
val IcSpeaker = ico("M4 9v6h4l5 4V5L8 9zM16.5 8.5a5 5 0 0 1 0 7M19 6a8.5 8.5 0 0 1 0 12")

val IcBack = ico("M15 5l-7 7l7 7")
