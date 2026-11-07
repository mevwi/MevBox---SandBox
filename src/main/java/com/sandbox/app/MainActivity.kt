package com.sandbox.app

import android.content.Context
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    var onSave: (() -> Unit)? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppTheme { SandboxScreen() } }
    }
    override fun onStop() { onSave?.invoke(); super.onStop() }   // автосохранение мира при сворачивании
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val ctx = LocalContext.current; val dark = isSystemInDarkTheme()
    val cs = if (Build.VERSION.SDK_INT >= 31) { if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx) }
    else if (dark) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = cs, content = content)
}

val CATS = listOf("Разное" to ((1..19).toList() + listOf(123, 124)), "Природа" to (20..40).toList(),
    "Электрика" to ((42..46).toList() + listOf(LED_G, LED_B) + (47..58).toList() + listOf(PUMP, HBAT)),
    "Провода" to (0 until WIRE_COLORS).map { wireId(it) }, "Элементы" to (59..110).toList(),
    "Особые" to listOf(111, 112, 114, 115, 116, 117, 118, 119, 120, 121, 122), "Тепло" to listOf(HEAT, COLD))
/** Какой значок рисовать на вкладке CATS[i] (см. drawCat). */
val CAT_ICON = listOf(0, 1, 2, 7, 3, 4, 6)

class Touch { var last: IntOffset? = null; var cell: IntOffset? = null; var since = 0L; var zooming = false; var painted = false; var snapped = false }

/** Где в канвасе лежит левый верхний угол поля при масштабе z (поле по центру, не вылезает за края). */
fun clampOrg(o: Offset, z: Float, sz: IntSize, w: Int, h: Int): Offset {
    val f = minOf(sz.width / w.toFloat(), sz.height / h.toFloat()) * z
    val cw = w * f; val ch = h * f
    val x = if (cw <= sz.width) (sz.width - cw) / 2f else o.x.coerceIn(sz.width - cw, 0f)
    val y = if (ch <= sz.height) (sz.height - ch) / 2f else o.y.coerceIn(sz.height - ch, 0f)
    return Offset(x, y)
}
fun fitScale(sz: IntSize, w: Int, h: Int) = minOf(sz.width / w.toFloat(), sz.height / h.toFloat())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SandboxScreen() {
    val ctx = LocalContext.current
    val auto = remember { File(ctx.filesDir, "autosave.sbx") }
    val world = remember { World(160, 240).also { if (auto.exists()) it.load(auto, false) } }
    val bmp = remember { Bitmap.createBitmap(world.w, world.h, Bitmap.Config.ARGB_8888) }
    val img = remember { bmp.asImageBitmap() }
    val touch = remember { Touch() }
    var run by remember { mutableStateOf(true) }
    var frame by remember { mutableIntStateOf(0) }
    var sel by remember { mutableIntStateOf(22) }
    var dir by remember { mutableIntStateOf(1) }
    var brush by remember { mutableFloatStateOf(3f) }
    var speed by remember { mutableIntStateOf(1) }
    var fireT by remember { mutableIntStateOf(600) }
    var cat by remember { mutableIntStateOf(1) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var org by remember { mutableStateOf(Offset.Zero) }
    var src by remember { mutableStateOf(false) }
    var showSet by remember { mutableStateOf(false) }
    var saveTick by remember { mutableIntStateOf(0) }
    var thermal by remember { mutableStateOf(false) }
    var probe by remember { mutableIntStateOf(15) }
    var undoN by remember { mutableIntStateOf(0) }
    var redoN by remember { mutableIntStateOf(0) }
    var ambT by remember { mutableIntStateOf(world.amb.roundToInt()) }
    var fingerP by remember { mutableIntStateOf(world.fingerPower.roundToInt()) }
    val heatTool = isTool(sel)
    /** Перед первой правкой в жесте запоминаем состояние — для отмены. */
    fun beginEdit() { if (!touch.snapped) { touch.snapped = true; world.pushUndo() } }
    fun say(@Suppress("UNUSED_PARAMETER") m: String) {}      // сообщения внизу убраны: выбранное показывается в заголовке

    var tilt by remember { mutableStateOf(true) }
    // Интерфейс всегда стоит прямо (экран заблокирован в портрете), а наклон телефона меняет только направление гравитации в мире.
    DisposableEffect(tilt) {
        if (!tilt) { world.gdir = 0; onDispose { } }
        else {
            val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            val sen = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            var fx = 0f; var fy = 9.8f
            val l = object : SensorEventListener {
                override fun onSensorChanged(e: SensorEvent) {
                    fx += (-e.values[0] - fx) * 0.15f; fy += (e.values[1] - fy) * 0.15f     // сглаживание
                    val ax = abs(fx); val ay = abs(fy)
                    if (max(ax, ay) < 3f) return                                             // телефон лежит плашмя — не трогаем
                    val vertNow = world.gdir == 0 || world.gdir == 2
                    val horiz = if (vertNow) ax > ay * 1.3f else !(ay > ax * 1.3f)           // гистерезис, чтобы не дёргалось на границе
                    world.gdir = if (horiz) (if (fx > 0) 1 else 3) else (if (fy > 0) 0 else 2)
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            if (sen != null) sm.registerListener(l, sen, SensorManager.SENSOR_DELAY_GAME)
            onDispose { sm?.unregisterListener(l) }
        }
    }

    DisposableEffect(Unit) {
        val act = ctx as? MainActivity
        act?.onSave = { runCatching { world.save(auto) } }
        onDispose { act?.onSave = null }
    }

    LaunchedEffect(Unit) {
        while (true) withFrameNanos {
            if (run) repeat(speed) { world.step() }
            val c = touch.cell      // палец удерживается — непрерывно сыплем материал
            if (c != null && !touch.zooming && SystemClock.uptimeMillis() - touch.since >= 60) {
                beginEdit(); touch.painted = true; world.paint(c.x, c.y, brush.toInt(), sel, dir, src)
            }
            if (undoN != world.undoSize) undoN = world.undoSize
            if (redoN != world.redoSize) redoN = world.redoSize
            val showT = thermal || isTool(sel)
            if (showT) touch.last?.let { probe = world.tempAt(it.x, it.y) }
            world.render(showT); bmp.setPixels(world.px, 0, world.w, 0, 0, world.w, world.h); frame++
        }
    }

    val bar = MaterialTheme.colorScheme.surfaceContainerHigh
    val accent = MaterialTheme.colorScheme.primaryContainer

    // что выбрано: «Категория → Материал»
    val selName = if (sel == 0) "Ластик" else (CATS.firstOrNull { sel in it.second }?.first ?: "") + " → " + (Els.list[sel]?.name ?: "")

    Scaffold { pad ->
        Column(Modifier.padding(pad).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (thermal || heatTool) (if (heatTool) selName else "Тепловизор") + " · ${probe}°C" else selName + if (src) "  ∞" else "",
                    Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                val onV = MaterialTheme.colorScheme.onSurfaceVariant
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(bar)
                    .clickable(enabled = undoN > 0) { world.undo(); undoN = world.undoSize; redoN = world.redoSize }, contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(20.dp)) { drawUndo(if (undoN > 0) onV else onV.copy(alpha = .3f), false) }
                }
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(bar)
                    .clickable(enabled = redoN > 0) { world.redo(); undoN = world.undoSize; redoN = world.redoSize }, contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(20.dp)) { drawUndo(if (redoN > 0) onV else onV.copy(alpha = .3f), true) }
                }
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(if (thermal) accent else bar)
                    .clickable { thermal = !thermal }, contentAlignment = Alignment.Center) {
                    val col = if (thermal) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    Canvas(Modifier.size(22.dp)) { drawCat(5, col) }
                }
                PanelBtn(Modifier.size(38.dp), bar, false, { showSet = true }) { Text("⚙", fontSize = 18.sp) }
            }
            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 4.dp)) {
                Canvas(
                    Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                fun org0() = clampOrg(org, zoom, size, world.w, world.h)
                                fun cell(o: Offset): IntOffset {
                                    val f = fitScale(size, world.w, world.h) * zoom; val g = org0()
                                    return IntOffset(floor((o.x - g.x) / f).toInt(), floor((o.y - g.y) / f).toInt())
                                }
                                fun stroke(a: IntOffset, b: IntOffset, gain: Float = 0.25f) {
                                    beginEdit()
                                    if (isWire(sel)) {          // провод: линия без диагональных «дыр» — каждая клетка касается предыдущей стороной
                                        val nx = abs(b.x - a.x); val ny = abs(b.y - a.y)
                                        val sx = if (b.x >= a.x) 1 else -1; val sy = if (b.y >= a.y) 1 else -1
                                        var x = a.x; var y = a.y; var ix = 0; var iy = 0
                                        world.paint(x, y, 0, sel, dir, src, gain)
                                        while (ix < nx || iy < ny) {
                                            if ((1 + 2 * ix) * ny < (1 + 2 * iy) * nx) { x += sx; ix++ } else { y += sy; iy++ }
                                            world.paint(x, y, 0, sel, dir, src, gain)
                                        }
                                        return
                                    }
                                    val n = max(abs(b.x - a.x), abs(b.y - a.y)) + 1
                                    for (k in 0..n) world.paint(a.x + (b.x - a.x) * k / n, a.y + (b.y - a.y) * k / n, brush.toInt(), sel, dir, src, gain)
                                }
                                val dn = awaitFirstDown()
                                val start = cell(dn.position); var last = start; var cur = start
                                // Рубильник/кнопку переключаем при отпускании пальца, если это было касание, а не мазок. Ищем их с запасом
                                // (палец крупнее клетки), и пока палец стоит на них — ничего не рисуем, иначе кисть затирала рычаг.
                                // С ластиком, инструментами тепла и самими рубильниками/кнопками в руке запас отключён.
                                val reach = if (sel == 0 || isTool(sel)) -1
                                    else if (sel == 43 || sel == 44) 0
                                    else ((10.dp.toPx() / (fitScale(size, world.w, world.h) * zoom)).toInt() + 1).coerceIn(1, 3)
                                var swIdx = if (reach >= 0) world.findSwitch(start.x, start.y, reach) else -1
                                val slop = 12.dp.toPx()
                                touch.last = start; touch.cell = if (swIdx >= 0) null else start
                                touch.since = SystemClock.uptimeMillis(); touch.zooming = false; touch.painted = false; touch.snapped = false
                                do {
                                    val ev = awaitPointerEvent()
                                    val down = ev.changes.filter { it.pressed }
                                    if (down.size >= 2) {                   // два пальца: приближение и перемещение поля
                                        touch.zooming = true; touch.cell = null; swIdx = -1
                                        val nz = (zoom * ev.calculateZoom()).coerceIn(1f, 10f); val c = ev.calculateCentroid()
                                        val np = c - (c - org0()) * (nz / zoom) + ev.calculatePan()
                                        org = clampOrg(np, nz, size, world.w, world.h); zoom = nz
                                        ev.changes.forEach { it.consume() }
                                    } else if (down.size == 1 && !touch.zooming) {
                                        cur = cell(down[0].position); touch.last = cur
                                        if (swIdx >= 0 && (down[0].position - dn.position).getDistance() > slop) swIdx = -1   // палец поехал — это мазок
                                        if (swIdx < 0) {
                                            touch.cell = cur
                                            if (SystemClock.uptimeMillis() - touch.since >= 60) { touch.painted = true; stroke(last, cur); last = cur }
                                        }
                                        down[0].consume()
                                    }
                                } while (ev.changes.any { it.pressed })
                                val wasZoom = touch.zooming
                                touch.cell = null; touch.zooming = false
                                if (!wasZoom && swIdx >= 0) world.tap(swIdx % world.w, swIdx / world.w)                    // нажали на рычаг/кнопку
                                else if (!wasZoom && !touch.painted) { if (!world.tap(cur.x, cur.y)) stroke(start, cur, 4f) }   // быстрое касание
                            }
                        }
                ) {
                    frame.let { }
                    val sz = IntSize(size.width.toInt(), size.height.toInt())
                    val f = fitScale(sz, world.w, world.h) * zoom
                    val o = clampOrg(org, zoom, sz, world.w, world.h)
                    drawImage(img, dstOffset = IntOffset(o.x.toInt(), o.y.toInt()),
                        dstSize = IntSize((world.w * f).toInt(), (world.h * f).toInt()), filterQuality = FilterQuality.None)
                    for (i in world.t.indices) {          // стрелки направления у логических элементов и «мосты» проводов
                        val ty = world.t[i]; val cross = world.crossH[i] >= 0 && isWire(ty)
                        if (!cross && ty !in 48..57 && ty != PUMP) continue
                        val x = o.x + (i % world.w + .5f) * f; val y = o.y + (i / world.w + .5f) * f
                        if (x < -f || y < -f || x > size.width + f || y > size.height + f) continue
                        if (cross) {                       // разные цвета в одной клетке: один провод идёт поперёк другого
                            drawLine(Color(world.wireColor(world.crossH[i], world.s[i] > 0)), Offset(x - f * .5f, y), Offset(x + f * .5f, y), f * .36f)
                            drawLine(Color(0xFF0E0E0E), Offset(x, y - f * .5f), Offset(x, y + f * .5f), f * .62f)
                            drawLine(Color(world.wireColor(world.crossV[i], world.s2[i] > 0)), Offset(x, y - f * .5f), Offset(x, y + f * .5f), f * .36f)
                            continue
                        }
                        val dd = world.d[i] and 3
                        drawLine(Color.White, Offset(x, y), Offset(x + DX[dd] * f * .9f, y + DY[dd] * f * .9f), f * .35f)
                        if (ty == 56) drawLine(Color.White, Offset(x, y), Offset(x - DX[dd] * f * .9f, y - DY[dd] * f * .9f), f * .35f)
                    }
                }
            }

            // ---- одна компактная панель: вкладки + действия ----
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (i in CATS.indices) {
                    val on = cat == i
                    Box(Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(12.dp)).background(if (on) accent else Color.Transparent)
                        .clickable { cat = i; say(CATS[i].first) }, contentAlignment = Alignment.Center) {
                        val col = if (on) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        Canvas(Modifier.size(21.dp)) { drawCat(CAT_ICON[i], col) }
                    }
                }
                Spacer(Modifier.width(5.dp))
                PanelBtn(Modifier.weight(1f), bar, run, { run = !run }) { Text(if (run) "⏸" else "▶", fontSize = 17.sp) }
                PanelBtn(Modifier.weight(1f), bar, sel == 0, { sel = 0; say("Ластик") }) { Text("⌫", fontSize = 17.sp) }
                val rot by animateFloatAsState(dir * 90f, spring(Spring.DampingRatioMediumBouncy), label = "rot")
                PanelBtn(Modifier.weight(1f), bar, false, { dir = (dir + 1) % 4 }) { Text("↑", Modifier.rotate(rot), fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                PanelBtn(Modifier.weight(1f), if (src) accent else bar, src, {
                    src = !src; say(if (src) "Источник: вкл — материал будет литься бесконечно" else "Источник: выкл")
                }) { Canvas(Modifier.size(22.dp)) { drawMat(113) } }
            }
            AnimatedContent(cat, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(100)) }, label = "cat") { c ->
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)) {
                    items(CATS[c].second) { id ->
                        Tile(id, sel == id, src && srcable(id)) {
                            sel = id; say(Els.list[id]!!.name + if (src && srcable(id)) " (источник)" else "")
                        }
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
        }
    }

    if (showSet) ModalBottomSheet(onDismissRequest = { showSet = false }) {
        saveTick.let { }
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Размер кисти: ${brush.toInt()}", style = MaterialTheme.typography.titleSmall)
            Slider(brush, { brush = it }, valueRange = 0f..14f)
            Text("Скорость симуляции: ×$speed", style = MaterialTheme.typography.titleSmall)
            Slider(speed.toFloat(), { speed = it.roundToInt() }, valueRange = 1f..4f, steps = 2)
            Text("Температура огня: ${fireT}°C", style = MaterialTheme.typography.titleSmall)
            Slider(fireT.toFloat(), { fireT = (it / 50f).roundToInt() * 50; world.fireTemp = fireT.toFloat() }, valueRange = 100f..1500f)
            Text("Горячее — сильнее греет и чаще поджигает соседей; холодное — слабо и редко.", fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Гравитация по наклону телефона", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                Switch(tilt, { tilt = it })
            }
            Text("Интерфейс не поворачивается: при наклоне телефона всё в мире падает в сторону низа телефона.", fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Температура всего поля: ${ambT}°C", style = MaterialTheme.typography.titleSmall)
            Slider(ambT.toFloat(), { ambT = (it / 5f).roundToInt() * 5; world.amb = ambT.toFloat() }, valueRange = -200f..1500f)
            Text("Высокая — лёд и снег тают сами, вода кипит, дерево загорается; низкая — вода сама замерзает. Всё меняется постепенно.", fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = { ambT = 15; world.amb = 15f }) { Text("Комнатная (15°C)") }
            Text("Сила «Нагрева» и «Охлаждения» пальцем: $fingerP°C за кадр", style = MaterialTheme.typography.titleSmall)
            Slider(fingerP.toFloat(), { fingerP = it.roundToInt(); world.fingerPower = fingerP.toFloat() }, valueRange = 5f..150f)
            Text("Сохранения (мир ещё сохраняется сам при сворачивании)", style = MaterialTheme.typography.titleSmall)
            for (n in 1..3) {
                val f = File(ctx.filesDir, "world_$n.sbx")
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Слот $n")
                        Text(if (f.exists()) SimpleDateFormat("dd.MM HH:mm").format(Date(f.lastModified())) else "пусто", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalButton(onClick = {
                        runCatching { world.save(f) }.onSuccess { say("Сохранено в слот $n") }.onFailure { say("Ошибка сохранения") }; saveTick++
                    }) { Text("Сохранить") }
                    OutlinedButton(enabled = f.exists(), onClick = {
                        say(if (world.load(f)) "Загружен слот $n" else "Не удалось загрузить"); showSet = false
                    }) { Text("Загрузить") }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { world.clear(); showSet = false }) { Text("Очистить поле") }
                OutlinedButton(onClick = { zoom = 1f; org = Offset.Zero; showSet = false }) { Text("Сбросить масштаб") }
            }
        }
    }
}

@Composable
fun PanelBtn(m: Modifier, bg: Color, active: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    val c = if (active) MaterialTheme.colorScheme.primaryContainer else bg
    Box(m.height(38.dp).clip(RoundedCornerShape(12.dp)).background(c).clickable(onClick = onClick), contentAlignment = Alignment.Center) { content() }
}

@Composable
fun Tile(id: Int, selected: Boolean, isSrc: Boolean, onClick: () -> Unit) {
    val sc by animateFloatAsState(if (selected) 1.1f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "sc")
    val bg by animateColorAsState(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh, label = "bg")
    val shape = RoundedCornerShape(14.dp)
    Box(Modifier.size(48.dp).scale(sc).clip(shape).background(bg)
        .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
        .clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        MatIcon(id, 38.dp)
        if (isSrc) Text("∞", Modifier.align(Alignment.TopEnd).padding(end = 3.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
    }
}

/** Нарисованная иконка материала; у химических элементов поверх — символ и атомный номер. */
@Composable
fun MatIcon(id: Int, size: Dp) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) { drawMat(id) }
        if (id in 59..110) {
            val bg = Color(Els.list[id]!!.color)
            val fg = if (bg.luminance() > .5f) Color(0xFF212121) else Color.White
            Text(Els.sym[id], color = fg, fontSize = (size.value * .36f).sp, fontWeight = FontWeight.Bold)
            Text(Els.num[id].toString(), color = fg.copy(alpha = .8f), fontSize = (size.value * .17f).sp,
                modifier = Modifier.align(Alignment.TopStart).padding(start = size * .13f, top = size * .1f))
        }
    }
}
