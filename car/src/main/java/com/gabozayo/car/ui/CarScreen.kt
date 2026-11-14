package com.gabozayo.car.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.gabozayo.car.CarViewModel
import com.gabozayo.car.LatLng
import com.gabozayo.car.SavedPlace
import com.gabozayo.car.Stage
import com.gabozayo.car.TripTarget
import com.gabozayo.car.formatTrip
import com.gabozayo.car.Tab
import com.gabozayo.car.distanceKm
import com.gabozayo.core.SavedItem
import java.util.Locale
import kotlin.math.roundToInt

// ---- 휴대폰 앱과 같은 색 규칙 (차량이 어두운 화면 모드면 자동으로 어둡게) ----
object CarPalette { var dark by mutableStateOf(false) }
private val dk get() = CarPalette.dark
val CarBlue: Color get() = Color(0xFFE3B32B)                                         // 아이콘 노랑 #E3B32B 그대로
val CarBlueTx: Color get() = if (dk) Color(0xFFE3B32B) else Color(0xFFA86B00)       // 노랑 글자·아이콘 (밝은 배경에서 읽히게)
val CarOn = Color(0xFF2B1D00)                                                     // 노랑 위의 글자
val CarOk = Color(0xFF16A37F)
val CarWarn = Color(0xFFE79A1F)
val CarErr = Color(0xFFE5484D)
private val Hy: Color get() = if (dk) Color(0xFF6B4A12) else Color(0xFF3D2A00)
private val LBg: Color get() = if (dk) Color(0xFF14130F) else Color(0xFFF7F5EE)
private val LCard: Color get() = if (dk) Color(0xFF1F1D17) else Color.White
private val LText: Color get() = if (dk) Color(0xFFF5F2E8) else Color(0xFF211B0D)
private val LSub: Color get() = if (dk) Color(0xFFA49F90) else Color(0xFF746E5E)
private val LChip: Color get() = if (dk) Color(0xFF3A2E10) else Color(0xFFFFF1CC)
private val LChipText: Color get() = if (dk) Color(0xFFFFD66B) else Color(0xFF7A4A00)
private val LImg: Color get() = if (dk) Color(0xFF2A271F) else Color(0xFFEBE6D8)
private val BlueGrad get() = Brush.linearGradient(listOf(CarBlue, Color(0xFFF0C95A)))
// 안내 단계 패널은 내비처럼 어두운 면으로 위에 띄워요
private val PanelBg = Color(0xFF0D1016)
private val PanelSub = Color(0xFFA49F90)
private val BarLight = Color(0xFFEDEEF0)

@Composable
fun CarTheme(content: @Composable () -> Unit) {
    // 한글이 단어(띄어쓰기) 단위로 줄바꿈 되도록 해요
    val base = TextStyle(
        letterSpacing = (-0.2).sp,
        lineBreak = LineBreak(LineBreak.Strategy.HighQuality, LineBreak.Strictness.Normal, LineBreak.WordBreak.Phrase),
    )
    // 차량 시스템의 화면 모드(주간/야간)를 그대로 따라가요
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    CarPalette.dark = dark
    val scheme = if (dark) darkColorScheme(primary = CarBlue, background = LBg, surface = LCard, onSurface = LText, onBackground = LText, onPrimary = CarOn)
    else lightColorScheme(primary = CarBlue, background = LBg, surface = LCard, onSurface = LText, onBackground = LText, onPrimary = CarOn)
    MaterialTheme(colorScheme = scheme) { ProvideTextStyle(base, content) }
}

/** 휴대폰 앱의 카드: 테두리 없이 은은한 그림자와 큰 둥근 모서리 */
private fun Modifier.card(r: Dp = 24.dp): Modifier {
    val shape = RoundedCornerShape(r)
    return this.shadow(8.dp, shape, ambientColor = Color.Black.copy(alpha = 0.05f), spotColor = Color.Black.copy(alpha = 0.10f)).clip(shape).background(LCard)
}

/** 누르면 살짝 작아졌다 돌아오는 반응 (휴대폰 앱과 같아요) */
private fun Modifier.press(onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (pressed) 0.97f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), label = "press")
    this.graphicsLayer { scaleX = sc; scaleY = sc }.clickable(interactionSource = src, indication = null, onClick = onClick)
}

private fun kmText(d: Double?): String = when {
    d == null -> ""
    d < 1 -> "${(d * 1000).roundToInt()}m"
    else -> String.format(Locale.US, "%.1fkm", d)
}

private fun CarViewModel.Ui.distTo(lat: Double, lng: Double): Double? = pos?.let { distanceKm(it, LatLng(lat, lng)) }

/**
 * 시안은 가로 1000 x 세로 600 기준(dp)으로 그렸어요. 차량 화면은 해상도가 제각각이라,
 * 화면에 맞춰 전체를 같은 비율로 키워서 어디서나 같은 모양이 되게 해요.
 */
@Composable
fun CarScreen(vm: CarViewModel = viewModel(), onAskLocation: () -> Unit = {}, onAskNotif: () -> Unit = {}, onAskDrive: () -> Unit = {}) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val d = androidx.compose.ui.platform.LocalDensity.current
        val scale = minOf(maxWidth.value / 1000f, maxHeight.value / 600f).coerceIn(0.8f, 4f)
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(d.density * scale, d.fontScale),
        ) { CarContent(vm, onAskLocation, onAskNotif, onAskDrive) }
    }
}

@Composable
private fun CarContent(vm: CarViewModel, onAskLocation: () -> Unit, onAskNotif: () -> Unit, onAskDrive: () -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize().background(LBg)) {
        if (ui.linkedCode == null) {
            Box(Modifier.fillMaxSize().padding(28.dp), Alignment.Center) {
                Column(Modifier.widthIn(max = 520.dp).verticalScroll(rememberScrollState())) { KeypadCard(ui, vm, "휴대폰과 연결") }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    Crossfade(if (ui.detail != null) "detail" else ui.tab.name, label = "tab") { key ->
                        when (key) {
                            "detail" -> ui.detail?.let { DetailScreen(it, ui, vm) }
                            Tab.HOME.name -> HomeScreen(ui, vm)
                            Tab.SAVED.name -> SavedScreen(ui, vm)
                            else -> OptionsScreen(ui, vm, onAskLocation, onAskNotif, onAskDrive)
                        }
                    }
                    ui.suggestion?.let { SuggestionCard(it, ui, vm) }
                    if (ui.message.isNotBlank() && ui.stage == Stage.IDLE) {
                        Text(ui.message, Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp).clip(RoundedCornerShape(20.dp)).background(if (dk) Color(0xFF3A362B) else Color(0xFF211B0D)).padding(horizontal = 20.dp, vertical = 10.dp), fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                NavBar(ui, vm)
            }
        }
        val d = ui.dest
        if (d != null && ui.stage != Stage.IDLE) {
            // 운전자도 조수석 동승자도 손이 닿기 쉽게 화면 아래 가운데에 작게 띄워요 (앞 시야를 가리지 않아요)
            Box(Modifier.fillMaxSize().background(Color(0x66000000)), Alignment.BottomCenter) {
                Column(Modifier.widthIn(max = 540.dp).fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) { StagePanel(ui.stage, ui, vm) }
            }
        }
    }
}

// ---------------------------------------------------------------- 하단 메뉴 (휴대폰 하단 메뉴와 같은 알약 모양)
@Composable
private fun NavBar(ui: CarViewModel.Ui, vm: CarViewModel) {
    Box(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp), Alignment.Center) {
        Row(Modifier.widthIn(max = 620.dp).fillMaxWidth(0.72f).height(84.dp).card(42.dp).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            NavItem(IcHome, "홈", ui.tab == Tab.HOME && ui.detail == null) { vm.openTab(Tab.HOME) }
            NavItem(IcHeart, "찜", ui.tab == Tab.SAVED && ui.detail == null) { vm.openTab(Tab.SAVED) }
            NavItem(IcOptions, "옵션", ui.tab == Tab.OPTIONS && ui.detail == null) { vm.openTab(Tab.OPTIONS) }
        }
    }
}

@Composable
private fun RowScope.NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val tint by animateColorAsState(if (selected) CarOn else (if (dk) Color(0xFFB4BCCD) else Color(0xFF4B5565)), label = "tint")
    val bg by animateColorAsState(if (selected) CarBlue else Color.Transparent, label = "bg")
    val w by animateFloatAsState(if (selected) 2.1f else 1f, spring(dampingRatio = 0.8f, stiffness = 380f), label = "w")
    Row(
        Modifier.weight(w).fillMaxHeight().press(onClick).clip(RoundedCornerShape(34.dp)).background(bg),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, label, Modifier.size(32.dp), tint = tint)
        if (selected) Text(label, Modifier.padding(start = 10.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CarOn, maxLines = 1, softWrap = false)
    }
}

// ---------------------------------------------------------------- 공통 조각
private val TagColors: List<Pair<Color, Color>> get() = if (dk) listOf(
    Color(0xFF4A2A22) to Color(0xFFFFB4A2), Color(0xFF173A2E) to Color(0xFF7FE3B8),
    Color(0xFF4A3B10) to Color(0xFFFFD66B), Color(0xFF22343B) to Color(0xFFA9C8D4),
) else listOf(
    Color(0xFFFFE9E4) to Color(0xFFD9482B), Color(0xFFE2F6EA) to Color(0xFF1B9A5A),
    Color(0xFFFFF1D6) to Color(0xFFB87400), Color(0xFFE1EBEE) to Color(0xFF2F4B57),
)

@Composable
private fun PhotoBox(img: String, name: String, modifier: Modifier) {
    if (img.isNotBlank()) AsyncImage(model = img, contentDescription = name, modifier = modifier.background(LImg), contentScale = ContentScale.Crop)
    else Box(modifier.background(Brush.linearGradient(listOf(LImg, LChip))), Alignment.Center) { Icon(IcPin, null, Modifier.size(32.dp), tint = LSub.copy(alpha = 0.55f)) }
}

/** 사진이 꽉 찬 카드. 글자는 사진 아래쪽 어두운 그라데이션 위에 얹어요 */
@Composable
private fun PhotoCard(img: String, title: String, tag: String, sub: String, modifier: Modifier, corner: @Composable BoxScope.() -> Unit = {}, onClick: () -> Unit) {
    Box(modifier.press(onClick).card(24.dp)) {
        PhotoBox(img, title, Modifier.fillMaxSize())
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(130.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xD9000000)))))
        Column(Modifier.align(Alignment.BottomStart).padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (tag.isNotBlank()) Text(tag, Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0x40FFFFFF)).padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, lineHeight = 23.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            // 첫 줄은 지역(핀), 시간·거리는 한 줄 아래에 따로 보여줘요
            val place = sub.substringBefore("\n"); val trip = sub.substringAfter("\n", "")
            if (place.isNotBlank()) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Icon(IcPin, null, Modifier.size(13.dp), tint = Color(0xE6FFFFFF))
                Text(place, fontSize = 13.sp, color = Color(0xE6FFFFFF), lineHeight = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (trip.isNotBlank()) Text(trip, Modifier.padding(top = 2.dp), fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        corner()
    }
}

@Composable
private fun SelChip(label: String, selected: Boolean, onClick: () -> Unit) =
    Text(label, Modifier.press(onClick).then(if (selected) Modifier.clip(RoundedCornerShape(20.dp)).background(CarBlue) else Modifier.card(20.dp)).padding(horizontal = 18.dp, vertical = 9.dp), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (selected) CarOn else LText, maxLines = 1)

@Composable
private fun CtaButton(text: String, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.fillMaxWidth().height(60.dp).press { if (enabled) onClick() }.clip(RoundedCornerShape(20.dp)).background(if (enabled) BlueGrad else Brush.linearGradient(listOf(Color(0xFFC9D0DE), Color(0xFFC9D0DE)))), Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(IcCar, null, Modifier.size(22.dp), tint = CarOn)
            Text(text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = CarOn)
        }
    }
}

@Composable
private fun EmptyCard(t: String) = Text(t, Modifier.fillMaxWidth().card(20.dp).padding(22.dp), fontSize = 16.sp, color = LSub, lineHeight = 24.sp)

@Composable
private fun InfoRow(label: String, text: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, Modifier.width(58.dp).padding(top = 1.dp), fontSize = 13.sp, color = Color(0xB32B1D00), fontWeight = FontWeight.Medium, maxLines = 1)
        Text(text, Modifier.weight(1f), fontSize = 15.sp, color = CarOn, fontWeight = FontWeight.Bold, lineHeight = 20.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

// ---------------------------------------------------------------- 홈
/** "11:00 / 14:00※ 매주 월요일 휴무" 같은 글을 운영시간 / 휴무 안내로 나눠요 */
private fun splitHours(raw: String): Pair<String, String> {
    val parts = raw.split("※").map { it.trim() }.filter { it.isNotBlank() }
    return (parts.firstOrNull() ?: "") to parts.drop(1).joinToString(" · ")
}

/** 홈 왼쪽 파란 카드를 홈 왼쪽 노란 카드 */
private class Feat(val name: String, val addr: String, val lat: Double, val lng: Double, val sent: com.gabozayo.car.Destination?, val saved: SavedPlace?) {
    val key: String get() = saved?.let { "p${it.id}" } ?: sent?.let { "s${it.id}" } ?: name
}

@Composable
private fun HomeScreen(ui: CarViewModel.Ui, vm: CarViewModel) {
    val date = remember { java.text.SimpleDateFormat("M월 d일 EEEE", Locale.KOREA).format(java.util.Date()) }
    val sel = ui.kidFilter
    // 아이를 누르면 그 아이 취향 추천만, "전체"면 아이들 추천을 번갈아 섞어서 3곳
    val recs = if (sel != null) ui.recs.filter { it.why.startsWith("$sel ") }.take(3)
    else ui.recs.groupBy { it.why.substringBefore(" ") }.values.let { groups ->
        val out = mutableListOf<SavedItem>(); var i = 0
        while (out.size < 3 && groups.any { i < it.size }) { groups.forEach { g -> g.getOrNull(i)?.let { if (out.none { o -> o.id == it.id }) out += it } }; i++ }
        out.take(3)
    }
    val tags = ui.kids.firstOrNull { it.name == sel }?.likes ?: ui.interests.take(4).map { it.label }
    val who = if (ui.kids.isEmpty()) "아이" else ui.kids.joinToString(" · ") { it.name }
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(date, fontSize = 15.sp, color = LSub)
                Text("${sel ?: "우리 아이"}랑 어디로 갈까요?", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = LText, maxLines = 1)
            }
            Row(Modifier.card(20.dp).padding(horizontal = 16.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(if (ui.linkOk) CarOk else Color(0xFFC9C3B2)))
                Text(if (ui.linkOk) "폰 연결됨" else "연결 대기", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LText, maxLines = 1)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (ui.kids.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelChip("전체", sel == null) { vm.selectKid(null) }
                ui.kids.forEach { k ->
                    val em = ui.interests.firstOrNull { it.label == k.likes.firstOrNull() }?.emoji.orEmpty()
                    SelChip(if (em.isNotBlank()) "$em ${k.name}" else k.name, sel == k.name) { vm.selectKid(k.name) }
                }
            }
            Spacer(Modifier.width(6.dp))
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                tags.forEachIndexed { i, t ->
                    val (bg, fg) = TagColors[i % TagColors.size]
                    Text("#$t", Modifier.clip(RoundedCornerShape(10.dp)).background(bg).padding(horizontal = 12.dp, vertical = 5.dp), fontSize = 13.sp, color = fg, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }

        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // 왼쪽: 폰에서 보낸 장소 + 찜한 곳들. 옆으로 밀어서 넘겨 볼 수 있고, 아래 동그라미가 몇 개 중 몇 번째인지 알려줘요
            val sent = ui.lastSent
            val feats = buildList {
                if (sent != null) add(Feat(sent.name, sent.address, sent.lat, sent.lng, sent, null))
                ui.saved.sortedBy { ui.distTo(it.lat, it.lng) ?: Double.MAX_VALUE }
                    .filter { sent == null || it.name != sent.name }
                    .take(8).forEach { add(Feat(it.name, it.address, it.lat, it.lng, null, it)) }
            }
            val pager = androidx.compose.foundation.pager.rememberPagerState(pageCount = { maxOf(feats.size, 1) })
            // 보이는 카드(현재 칸과 다음 칸, 추천 3곳, 축제 3곳)의 실제 시간·거리를 내비에 물어봐요
            LaunchedEffect(pager.currentPage, feats.map { it.key }, recs.map { it.id }, ui.events.take(3).map { it.id }) {
              // 내비가 바빠서 못 받은 곳은 20초마다 다시 물어봐요
              while (true) {
                vm.wantTrips(
                    feats.drop(pager.currentPage).take(2).map { TripTarget(it.key, it.name, it.addr, it.lat, it.lng) } +
                        recs.map { TripTarget("p${it.id}", it.name, it.address, it.lat, it.lng) } +
                        ui.events.take(3).map { TripTarget("p${it.id}", it.name, it.address, it.lat, it.lng) },
                )
                kotlinx.coroutines.delay(20_000)
              }
            }
            Column(Modifier.weight(0.85f).fillMaxHeight().shadow(10.dp, RoundedCornerShape(28.dp), ambientColor = CarBlue.copy(alpha = 0.2f), spotColor = CarBlue.copy(alpha = 0.35f)).clip(RoundedCornerShape(28.dp)).background(BlueGrad).padding(20.dp)) {
                androidx.compose.foundation.pager.HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth(), pageSpacing = 16.dp) { page ->
                    val f = feats.getOrNull(page)
                    Column(Modifier.fillMaxSize()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(IcPhone, null, Modifier.size(14.dp), tint = Color(0xCC2B1D00))
                            Text(if (f?.sent != null) "폰에서 보낸 장소" else "폰에서 찜한 장소", fontSize = 14.sp, color = Color(0xCC2B1D00))
                        }
                        if (f != null) {
                            Text(f.name, Modifier.padding(top = 8.dp), fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = CarOn, lineHeight = 31.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                            // 정리된 순서: 이름 → 주소 → 도착 정보(알약) → 운영시간 → 휴무 → 편의시설
                            if (f.addr.isNotBlank()) Text(f.addr, Modifier.padding(top = 6.dp), fontSize = 14.sp, color = Color(0xCC2B1D00), lineHeight = 19.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            ui.tripText(f.key).takeIf { it.isNotBlank() }?.let { Text(it, Modifier.padding(top = 10.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x1F000000)).padding(horizontal = 12.dp, vertical = 5.dp), fontSize = 15.sp, color = CarOn, fontWeight = FontWeight.ExtraBold, maxLines = 1) }
                            // 폰이 보내 준 상세가 있는 장소(추천·축제)일 때만 운영시간·편의시설을 보여줘요
                            val info = (ui.recs + ui.events).firstOrNull { it.name == f.name }
                            if (info != null) {
                                val (open, note) = splitHours(info.hours)
                                val rows = listOfNotNull(
                                    open.takeIf { it.isNotBlank() }?.let { "운영시간" to it },
                                    note.takeIf { it.isNotBlank() }?.let { "휴무" to it },
                                    info.age.takeIf { it.isNotBlank() }?.let { "이용연령" to it },
                                    info.facilities.take(3).joinToString(" · ").takeIf { it.isNotBlank() }?.let { "편의시설" to it },
                                )
                                if (rows.isNotEmpty()) Column(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0x1A000000)).padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    rows.forEachIndexed { i, r ->
                                        if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x1A000000)))
                                        InfoRow(r.first, r.second)
                                    }
                                }
                            }
                        } else Text("폰에서 장소를 보내거나 찜하면 여기에 나타나요", Modifier.padding(top = 8.dp), fontSize = 19.sp, fontWeight = FontWeight.Bold, color = CarOn, lineHeight = 27.sp)
                    }
                }
                if (feats.size > 1) Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                    feats.indices.forEach { i ->
                        val on = pager.currentPage == i
                        // 지금 보는 칸은 속이 찬 흰 동그라미, 나머지는 속이 빈 흰 동그라미
                        Box(Modifier.size(if (on) 10.dp else 8.dp).clip(CircleShape).background(if (on) CarOn else Color.Transparent).border(1.5.dp, CarOn.copy(alpha = if (on) 1f else 0.75f), CircleShape))
                    }
                }
                val cur = feats.getOrNull(pager.currentPage)
                Box(Modifier.fillMaxWidth().height(58.dp).press { cur?.let { c -> if (c.sent != null) vm.goToDest(c.sent) else c.saved?.let(vm::goToSaved) } }.clip(RoundedCornerShape(20.dp)).background(if (cur != null) Color.White else Color(0x1F000000)), Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(IcCar, null, Modifier.size(22.dp), tint = if (cur != null) CarBlueTx else Color(0x992B1D00))
                        Text("가보자요!", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = if (cur != null) CarBlueTx else Color(0x99FFFFFF))
                    }
                }
            }
            // 오른쪽: 추천 + 축제
            Column(Modifier.weight(2f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column {
                    Text("우리 아이 관심사 추천", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = LText)
                    Text("${sel ?: who}가 좋아할 만한 곳이에요", fontSize = 14.sp, color = LSub, maxLines = 1)
                }
                if (recs.isEmpty()) EmptyCard(if (sel != null) "${sel} 취향에 맞는 장소를 찾고 있어요" else "휴대폰 앱에서 아이 관심사를 고르면 여기에 추천 장소가 나타나요")
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    recs.forEach { r -> PhotoCard(r.img, r.name, r.tag, listOf(r.region, ui.tripText("p${r.id}")).filter { it.isNotBlank() }.joinToString("\n"), Modifier.weight(1f).fillMaxHeight()) { vm.openDetail(r) } }
                    repeat(3 - recs.size) { Spacer(Modifier.weight(1f)) }
                }
                if (ui.events.isNotEmpty()) Column(Modifier.fillMaxWidth().card(20.dp).padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🎪 오늘 열리는 축제", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = LText, maxLines = 1)
                    // 이름과 시간·거리가 잘리지 않게 3칸으로 나눠서, 이름은 2줄까지 보여줘요
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ui.events.take(3).forEach { e ->
                            Column(Modifier.weight(1f).press { vm.openDetail(e) }.clip(RoundedCornerShape(12.dp)).background(LBg).padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(e.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LText, lineHeight = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                ui.tripText("p${e.id}").takeIf { it.isNotBlank() }?.let { Text(it, fontSize = 13.sp, color = CarBlueTx, fontWeight = FontWeight.Bold, maxLines = 1) }
                            }
                        }
                        repeat(3 - ui.events.take(3).size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- 상세
@Composable
private fun InfoTile(icon: ImageVector, tint: Color, bg: Color, label: String, value: String, modifier: Modifier) {
    Row(modifier.card(18.dp).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(bg), Alignment.Center) { Icon(icon, null, Modifier.size(18.dp), tint = tint) }
        Column {
            Text(label, fontSize = 12.sp, color = LSub, maxLines = 1)
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = LText, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 19.sp)
        }
    }
}

@Composable
private fun DetailScreen(p: SavedItem, ui: CarViewModel.Ui, vm: CarViewModel) {
    val km = ui.tripText("p${p.id}")
    LaunchedEffect(p.id) { vm.wantTrips(listOf(TripTarget("p${p.id}", p.name, p.address, p.lat, p.lng))) }
    Row(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        // 큰 사진 + 사진 위에 얹은 제목
        Box(Modifier.weight(1.05f).fillMaxHeight().card(30.dp)) {
            // 사진이 여러 장이면 좌우로 넘겨 볼 수 있어요
            val photos = p.images.ifEmpty { listOf(p.img) }
            val photoPager = androidx.compose.foundation.pager.rememberPagerState(pageCount = { photos.size })
            androidx.compose.foundation.pager.HorizontalPager(photoPager, Modifier.fillMaxSize()) { i -> PhotoBox(photos[i], p.name, Modifier.fillMaxSize()) }
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(220.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6000000)))))
            Box(Modifier.padding(16.dp).size(44.dp).press { vm.openDetail(null) }.clip(CircleShape).background(Color(0x66000000)), Alignment.Center) { Icon(IcBack, "뒤로", Modifier.size(24.dp), tint = Color.White) }
            if (photos.size > 1) Text("${photoPager.currentPage + 1} / ${photos.size}", Modifier.align(Alignment.TopEnd).padding(16.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x66000000)).padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
            Column(Modifier.align(Alignment.BottomStart).padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (p.tag.isNotBlank()) Text(p.tag, Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0x40FFFFFF)).padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    if (p.why.isNotBlank()) Text(p.why, Modifier.clip(RoundedCornerShape(10.dp)).background(CarBlue).padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 12.sp, color = CarOn, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                Text(p.name, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, lineHeight = 37.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                if (p.address.isNotBlank()) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(IcPin, null, Modifier.size(15.dp), tint = Color(0xE6FFFFFF)); Text(p.address, fontSize = 14.sp, color = Color(0xE6FFFFFF), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        // 오른쪽: 한눈에 보기 → 편의시설 → 소개 → 출발 버튼
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("한눈에 보기", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = LText)
                val tiles = listOfNotNull(
                    splitHours(p.hours).first.takeIf { it.isNotBlank() }?.let { Triple(IcClock, "이용 시간", it) },
                    splitHours(p.hours).second.takeIf { it.isNotBlank() }?.let { Triple(IcClock, "휴무", it) },
                    p.age.takeIf { it.isNotBlank() }?.let { Triple(IcFace, "추천 연령", it) },
                    km.takeIf { it.isNotBlank() }?.let { Triple(IcPin, "도착까지", it) },
                )
                val tints = listOf(Color(0xFFA86B00), Color(0xFFE0642B), Color(0xFF16A37F)).map { it to it.copy(alpha = 0.14f) }
                tiles.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { t -> val k = tiles.indexOf(t); InfoTile(t.first, tints[k % 3].first, tints[k % 3].second, t.second, t.third, Modifier.weight(1f)) }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                if (p.facilities.isNotEmpty()) {
                    Text("편의시설", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = LText)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        p.facilities.forEach { Row(Modifier.card(14.dp).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(7.dp).clip(CircleShape).background(CarOk)); Text(it, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LText, maxLines = 1)
                        } }
                    }
                }
                if (p.overview.isNotBlank()) Column(Modifier.fillMaxWidth().card(20.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("이런 곳이에요", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = LText)
                    Text(p.overview, fontSize = 14.sp, color = LSub, lineHeight = 21.sp, maxLines = 5, overflow = TextOverflow.Ellipsis)
                }
            }
            CtaButton(if (km.isNotBlank()) "가보자요!  ·  $km" else "가보자요!") { vm.goTo(p) }
        }
    }
}

// ---------------------------------------------------------------- 찜
@Composable
private fun SavedScreen(ui: CarViewModel.Ui, vm: CarViewModel) {
    val shownSaved = ui.saved.sortedBy { ui.distTo(it.lat, it.lng) ?: Double.MAX_VALUE }.take(12)
    LaunchedEffect(shownSaved.map { it.id }) { vm.wantTrips(shownSaved.map { TripTarget("p${it.id}", it.name, it.address, it.lat, it.lng) }) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column {
            Text("찜한 곳", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = LText)
            Text("휴대폰에서 찜한 ${ui.saved.size}곳 · 눌러서 바로 출발해요", fontSize = 14.sp, color = LSub)
        }
        if (ui.saved.isEmpty()) EmptyCard("휴대폰 가보자요 앱에서 하트를 눌러 찜하면 여기에서 골라 바로 출발할 수 있어요")
        else shownSaved.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { p ->
                    PhotoCard(p.img, p.name, p.tag, listOf(p.region, ui.tripText("p${p.id}")).filter { it.isNotBlank() }.joinToString("\n"), Modifier.weight(1f).height(200.dp), corner = {
                        Icon(IcHeartFill, null, Modifier.align(Alignment.TopEnd).padding(12.dp).size(22.dp), tint = Color(0xFFFF6B6B))
                        Row(Modifier.align(Alignment.BottomEnd).padding(12.dp).clip(RoundedCornerShape(14.dp)).background(Color.White).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(IcCar, null, Modifier.size(16.dp), tint = CarBlueTx); Text("가보자요!", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = CarBlueTx)
                        }
                    }) { vm.goToSaved(p) }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// ---------------------------------------------------------------- 옵션
@Composable
private fun OptionsScreen(ui: CarViewModel.Ui, vm: CarViewModel, onAskLocation: () -> Unit, onAskNotif: () -> Unit, onAskDrive: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column {
            Text("설정", fontSize = 14.sp, color = LSub)
            Text("옵션", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = LText)
        }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth().card(22.dp).padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBox(IcPhone)
                    Column(Modifier.weight(1f)) {
                        Text(if (ui.linkOk) "폰 연결됨" else "연결 대기", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = LText)
                        Text(ui.linkedCode?.chunked(3)?.joinToString(" ") ?: "", fontSize = 14.sp, color = LSub)
                    }
                    Text("연결 해제", Modifier.press { vm.unlink() }.clip(RoundedCornerShape(12.dp)).background(LBg).padding(horizontal = 14.dp, vertical = 9.dp), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LText, maxLines = 1)
                }
                Column(Modifier.fillMaxWidth().card(22.dp).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconBox(IcCar)
                        Column {
                            Text("경로 선호", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = LText)
                            Text("차량에서 바로 출발할 때 쓰는 경로예요", fontSize = 13.sp, color = LSub)
                        }
                    }
                    Segments(com.gabozayo.car.RouteType.entries.map { it.label }, com.gabozayo.car.RouteType.entries.indexOf(ui.routeType)) { vm.setRoute(com.gabozayo.car.RouteType.entries[it]) }
                }
                Row(Modifier.fillMaxWidth().card(22.dp).padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBox(IcSpeaker)
                    Column(Modifier.weight(1f)) {
                        Text("음성 안내", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = LText)
                        Text("목적지를 받거나 안내를 시작할 때 말해 줘요", fontSize = 13.sp, color = LSub)
                    }
                    Switch(checked = ui.voiceOn, onCheckedChange = vm::setVoice)
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.fillMaxWidth().card(22.dp).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconBox(IcPin)
                        Column(Modifier.weight(1f)) {
                            Text("주행 중 맞춤 제안", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = LText)
                            Text("근처에 아이 취향 장소가 있으면 알려줘요", fontSize = 13.sp, color = LSub)
                        }
                        Switch(checked = ui.proactive, onCheckedChange = vm::setProactive)
                    }
                    if (ui.proactive) {
                        Text("알려줄 거리", fontSize = 13.sp, color = LSub)
                        val opts = listOf(1, 3, 5)
                        Segments(opts.map { "${it}km" }, opts.indexOf(ui.radiusKm).coerceAtLeast(0)) { vm.setRadius(opts[it]) }
                    }
                }
                Column(Modifier.fillMaxWidth().card(22.dp).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("권한", fontSize = 16.sp, color = LText, fontWeight = FontWeight.ExtraBold)
                    PermRow("위치", ui.locationOk, onAskLocation)
                    PermRow("알림", ui.notifOk, onAskNotif)
                    PermRow("차량 데이터", ui.driveOk, onAskDrive)
                }
            }
        }
    }
}

/** 알약 모양 선택 버튼 줄 */
@Composable
private fun Segments(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(LBg).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        labels.forEachIndexed { i, l ->
            Box(Modifier.weight(1f).press { onSelect(i) }.clip(RoundedCornerShape(12.dp)).background(if (i == selected) CarBlue else Color.Transparent).padding(vertical = 11.dp), Alignment.Center) {
                Text(l, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (i == selected) CarOn else LSub, maxLines = 1)
            }
        }
    }
}

@Composable
private fun IconBox(icon: ImageVector) =
    Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(LChip), Alignment.Center) { Icon(icon, null, Modifier.size(21.dp), tint = CarBlueTx) }

@Composable
private fun PermRow(label: String, ok: Boolean, onAsk: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontSize = 16.sp, color = LText)
        if (ok) Text("허용됨", Modifier.clip(RoundedCornerShape(10.dp)).background(CarOk.copy(alpha = 0.16f)).padding(horizontal = 12.dp, vertical = 5.dp), fontSize = 13.sp, color = CarOk, fontWeight = FontWeight.Bold)
        else Text("허용하기", Modifier.press(onAsk).clip(RoundedCornerShape(10.dp)).background(CarBlue).padding(horizontal = 14.dp, vertical = 7.dp), fontSize = 13.sp, color = CarOn, fontWeight = FontWeight.Bold)
    }
}

/** 휴대폰 가보자요 앱의 6자리 코드를 입력하는 숫자판 */
@Composable
private fun KeypadCard(ui: CarViewModel.Ui, vm: CarViewModel, title: String) {
    var code by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth().shadow(10.dp, RoundedCornerShape(28.dp), ambientColor = CarBlue.copy(alpha = 0.2f), spotColor = CarBlue.copy(alpha = 0.35f)).clip(RoundedCornerShape(28.dp)).background(BlueGrad).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xE62B1D00))
        Text("폰 앱 [내 정보 → 차량 연결]의 6자리 코드를 입력하세요", fontSize = 13.sp, color = Color(0xCC2B1D00), lineHeight = 18.sp)
        Row(Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(6) { i ->
                Box(Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(14.dp)).background(if (i < code.length) Color(0x26000000) else Color(0x14000000)), Alignment.Center) {
                    Text(code.getOrNull(i)?.toString() ?: "", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = CarOn)
                }
            }
        }
        if (ui.linkError.isNotBlank()) Text(ui.linkError, fontSize = 14.sp, color = Color(0xFFFFD0D0), fontWeight = FontWeight.Bold)
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "←", "0", "✓").chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { k ->
                    val primary = k == "✓"
                    Box(
                        Modifier.weight(1f).height(54.dp).press {
                            if (!ui.driving) when (k) {
                                "←" -> code = code.dropLast(1)
                                "✓" -> vm.connectCode(code)
                                else -> if (code.length < 6) code += k
                            }
                        }.clip(RoundedCornerShape(18.dp)).background(if (primary) Color.White else Color(0x1F000000)),
                        Alignment.Center,
                    ) { Text(k, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (primary) CarBlueTx else CarOn) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- 주행 중 제안 카드 (오른쪽 위)
@Composable
private fun BoxScope.SuggestionCard(p: SavedItem, ui: CarViewModel.Ui, vm: CarViewModel) {
    Column(Modifier.align(Alignment.BottomEnd).padding(bottom = 24.dp, end = 16.dp).width(320.dp).card(24.dp)) {
        Box {
            PhotoBox(p.img, p.name, Modifier.fillMaxWidth().height(120.dp))
            Text(ui.tripText("p${p.id}").let { if (it.isBlank()) "근처" else "근처 · $it" }, Modifier.padding(10.dp).clip(RoundedCornerShape(8.dp)).background(CarBlue).padding(horizontal = 10.dp, vertical = 3.dp), fontSize = 13.sp, color = CarOn, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (p.why.isNotBlank()) Text(p.why, Modifier.clip(RoundedCornerShape(8.dp)).background(LChip).padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 12.sp, color = LChipText, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(p.name, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = LText, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (p.hours.isNotBlank()) Text(p.hours, fontSize = 13.sp, color = LSub, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("닫기", Modifier.press { vm.dismissSuggestion() }.clip(RoundedCornerShape(14.dp)).background(LBg).padding(horizontal = 18.dp, vertical = 13.dp), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = LSub)
                Box(Modifier.weight(1f).press { vm.goTo(p, fromSuggestion = true) }.clip(RoundedCornerShape(14.dp)).background(BlueGrad).padding(vertical = 13.dp), Alignment.Center) {
                    Text("가보자요!", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = CarOn)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- 안내 단계 패널 (어두운 면)
@Composable
private fun Panel(
    title: String, accent: Color, onClose: (() -> Unit)?, barText: String?, barEnabled: Boolean = true, onBar: () -> Unit = {},
    body: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(PanelBg).border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(26.dp))) {
        Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 8.dp).height(60.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (onClose != null) Box(Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onClose), Alignment.Center) { Text("✕", fontSize = 20.sp, color = Color.White) }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x22FFFFFF)))
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(Modifier.width(6.dp).fillMaxHeight().background(accent))
            Column(Modifier.weight(1f).padding(horizontal = 22.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = body)
        }
        if (barText != null) Box(
            Modifier.fillMaxWidth().height(64.dp).background(if (barEnabled) BarLight else Color(0xFF232733)).clickable(enabled = barEnabled, onClick = onBar),
            Alignment.Center,
        ) { Text(barText, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (barEnabled) Color(0xFF0B0F17) else PanelSub, maxLines = 1) }
    }
}

/** 내비가 알려준 "도착까지 약 12분 · 4.2km" (아직 못 받았으면 null) */
private fun tripLine(ui: CarViewModel.Ui): String? {
    val sec = ui.etaSec ?: return null
    return "도착까지 ${formatTrip(sec, ui.tripDistM ?: 0)}"
}

@Composable
private fun StagePanel(st: Stage, ui: CarViewModel.Ui, vm: CarViewModel) {
    val d = ui.dest ?: return
    when (st) {
        Stage.RECEIVED -> Panel(d.name, CarBlue, vm::dismiss, "안내 시작", !ui.driving, vm::start) {
            Text(d.route.label, fontSize = 16.sp, color = Color.White)
            // 시간·거리는 내비가 계산해 준 값만 보여줘요 (아직 못 받았으면 보여주지 않아요)
            tripLine(ui)?.let { Text(it, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White) }
            if (d.address.isNotBlank()) Text(d.address, Modifier.padding(top = 8.dp), fontSize = 15.sp, color = PanelSub, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (ui.message.isNotBlank()) Text(ui.message, Modifier.padding(top = 10.dp), fontSize = 15.sp, color = Color(0xFFFF8A8A))
        }
        Stage.REQUESTING -> Panel(d.name, CarBlue, vm::cancelRequest, "취소", true, vm::cancelRequest) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                CircularProgressIndicator(Modifier.size(34.dp), strokeWidth = 4.dp, color = Color.White)
                Text("경로를 찾는 중이에요", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
            if (ui.message.isNotBlank()) Text(ui.message, Modifier.padding(top = 8.dp), fontSize = 16.sp, color = PanelSub)
        }
        Stage.READY -> Panel(d.name, CarOk, vm::dismiss, "확인", true, vm::dismiss) {
            Text("경로가 준비됐어요", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            tripLine(ui)?.let { Text(it, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White) }
            Text(ui.message.ifBlank { "내비 화면에서 [안내 시작]을 눌러 주세요" }, fontSize = 16.sp, color = PanelSub, lineHeight = 30.sp)
        }
        Stage.GUIDING -> Panel(d.name, CarOk, vm::dismiss, "확인", true, vm::dismiss) {
            Text("안내 중이에요", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            tripLine(ui)?.let { Text(it, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White) }
            Text("즐거운 나들이 되세요", fontSize = 16.sp, color = PanelSub)
        }
        else -> Panel(d.name, CarErr, vm::dismiss, "확인", true, vm::dismiss) {
            Text("경로를 만들지 못했어요", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text(ui.message, fontSize = 16.sp, color = Color(0xFFFF8A8A), lineHeight = 30.sp)
            if (!ui.driving) TextButton(vm::start) { Text("다시 시도", fontSize = 16.sp, color = Color(0xFF8FB0FF)) }
        }
    }
}
