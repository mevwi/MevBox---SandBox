package com.sandbox.app

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Нарисованные иконки материалов. Все рисуется в условном квадрате 100x100.
 * ЧТОБЫ НАРИСОВАТЬ СВОЮ ИКОНКУ: добавь ветку в when(id) в icon(). Если ветки нет — берётся общая по типу вещества
 * (куча для порошка, капля для жидкости, облачко для газа, блок для твёрдого).
 */

/** Рисует иконку материала [id] так, чтобы она заполнила весь холст. */
fun DrawScope.drawMat(id: Int) {
    val e = Els.list[id] ?: return
    scale(size.minDimension / 100f, Offset.Zero) { icon(id, e) }
}

/** Иконки вкладок (одноцветные): 0 разное, 1 природа, 2 электрика, 3 элементы, 4 особые, 5 термометр, 6 тепло/холод, 7 провода. */
fun DrawScope.drawCat(i: Int, col: Color) {
    scale(size.minDimension / 100f, Offset.Zero) {
        when (i) {
            0 -> { // колба
                fil(col) { moveTo(40f, 8f); lineTo(60f, 8f); lineTo(60f, 36f); lineTo(90f, 88f); lineTo(10f, 88f); lineTo(40f, 36f); close() }
                ln(col, 34f, 8f, 66f, 8f, 8f)
                cc(Color.Black.copy(alpha = .28f), 42f, 70f, 6f); cc(Color.Black.copy(alpha = .28f), 58f, 62f, 4f)
            }
            1 -> { // горы и солнце
                pg(col, 4f, 90f, 34f, 34f, 52f, 62f, 66f, 44f, 96f, 90f)
                cc(col, 76f, 18f, 10f)
            }
            2 -> bolt(col)
            3 -> { // атом
                cc(col, 50f, 50f, 9f)
                for (a in 0 until 3) rotate(a * 60f, Offset(50f, 50f)) { drawOval(col, Offset(8f, 32f), Size(84f, 36f), style = Stroke(6f)) }
            }
            5 -> { // термометр
                rr(col, 38f, 6f, 24f, 64f, 12f); cc(col, 50f, 76f, 20f)
                ln(Color.Black.copy(alpha = .3f), 50f, 30f, 50f, 76f, 7f); cc(Color.Black.copy(alpha = .3f), 50f, 76f, 10f)
                for (k in 0 until 3) ln(col, 70f, 16f + k * 14f, 84f, 16f + k * 14f, 5f)
            }
            6 -> { // тепло/холод: пламя и снежинка
                scale(.62f, Offset(8f, 96f)) { flamePath(col) }
                flake(col, 70f, 32f, 26f, 7f)
            }
            7 -> { // провода: ломаная линия с контактами
                stk(col, 11f) { moveTo(10f, 68f); lineTo(36f, 68f); lineTo(36f, 32f); lineTo(64f, 32f); lineTo(64f, 68f); lineTo(90f, 68f) }
                cc(col, 10f, 68f, 8f); cc(col, 90f, 68f, 8f)
            }
            else -> star(col, 50f, 52f, 46f, 20f)
        }
    }
}

/** Стрелка «отменить» (redo = false) или «повторить» (redo = true), одноцветная. */
fun DrawScope.drawUndo(col: Color, redo: Boolean) {
    scale(size.minDimension / 100f, Offset.Zero) {
        val body: DrawScope.() -> Unit = {
            pg(col, 6f, 44f, 40f, 14f, 40f, 74f)
            stk(col, 10f) { moveTo(36f, 44f); cubicTo(86f, 28f, 98f, 86f, 52f, 86f) }
        }
        if (redo) scale(-1f, 1f, Offset(50f, 50f)) { body() } else body()
    }
}

private fun DrawScope.bolt(col: Color) = pg(col, 60f, 4f, 24f, 56f, 46f, 56f, 38f, 96f, 78f, 40f, 54f, 40f, 68f, 4f)

private fun DrawScope.star(col: Color, cx: Float, cy: Float, ro: Float, ri: Float) {
    val p = Path()
    for (k in 0 until 10) {
        val r = if (k % 2 == 0) ro else ri
        val a = -PI / 2 + k * PI / 5
        val x = cx + (r * cos(a)).toFloat(); val y = cy + (r * sin(a)).toFloat()
        if (k == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close(); drawPath(p, col)
}

// ---------- примитивы (координаты 0..100) ----------
private fun DrawScope.pg(col: Color, vararg p: Float) {
    val pa = Path(); pa.moveTo(p[0], p[1]); var k = 2
    while (k + 1 < p.size) { pa.lineTo(p[k], p[k + 1]); k += 2 }
    pa.close(); drawPath(pa, col)
}
private fun DrawScope.rr(col: Color, x: Float, y: Float, w: Float, h: Float, r: Float) =
    drawRoundRect(col, Offset(x, y), Size(w, h), CornerRadius(r, r))
private fun DrawScope.cc(col: Color, x: Float, y: Float, r: Float) = drawCircle(col, r, Offset(x, y))
private fun DrawScope.ring(col: Color, x: Float, y: Float, r: Float, w: Float) = drawCircle(col, r, Offset(x, y), style = Stroke(w))
private fun DrawScope.ln(col: Color, x1: Float, y1: Float, x2: Float, y2: Float, w: Float) =
    drawLine(col, Offset(x1, y1), Offset(x2, y2), w, StrokeCap.Round)
private fun DrawScope.fil(col: Color, build: Path.() -> Unit) { val p = Path(); p.build(); drawPath(p, col) }
private fun DrawScope.stk(col: Color, w: Float, build: Path.() -> Unit) {
    val p = Path(); p.build(); drawPath(p, col, style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round))
}
private fun DrawScope.flake(col: Color, cx: Float, cy: Float, r: Float, w: Float) {
    for (a in 0 until 3) rotate(a * 60f, Offset(cx, cy)) { ln(col, cx, cy - r, cx, cy + r, w) }
}

// ---------- шаблоны по типу вещества ----------
private fun DrawScope.block(c: Color, dk: Color, lt: Color) {
    rr(dk, 8f, 8f, 84f, 84f, 16f); rr(c, 12f, 12f, 76f, 76f, 13f); rr(lt.copy(alpha = .55f), 17f, 17f, 46f, 9f, 4f)
}
private fun DrawScope.pile(c: Color, dk: Color, lt: Color) {
    pg(dk, 3f, 92f, 18f, 80f, 30f, 58f, 42f, 38f, 50f, 26f, 58f, 38f, 70f, 58f, 82f, 80f, 97f, 92f)
    pg(c, 10f, 88f, 22f, 77f, 33f, 58f, 43f, 41f, 50f, 33f, 57f, 41f, 67f, 58f, 78f, 77f, 90f, 88f)
    cc(lt, 40f, 66f, 3.2f); cc(dk, 55f, 56f, 2.6f); cc(lt, 62f, 76f, 3.2f); cc(dk, 31f, 78f, 2.6f); cc(lt, 48f, 82f, 3f); cc(dk, 50f, 46f, 2.4f)
}
private fun DrawScope.dropShape(col: Color) = fil(col) {
    moveTo(50f, 6f); cubicTo(50f, 6f, 16f, 44f, 16f, 64f); cubicTo(16f, 82f, 31f, 94f, 50f, 94f)
    cubicTo(69f, 94f, 84f, 82f, 84f, 64f); cubicTo(84f, 44f, 50f, 6f, 50f, 6f); close()
}
private fun DrawScope.drop(c: Color, dk: Color) {
    dropShape(dk); scale(.9f, Offset(50f, 66f)) { dropShape(c) }
    stk(Color.White.copy(alpha = .65f), 5f) { moveTo(30f, 62f); quadraticBezierTo(30f, 76f, 40f, 82f) }
}
private fun DrawScope.cloud(col: Color, edge: Color) {
    for (pass in 0..1) {
        val k = if (pass == 0) edge else col; val g = if (pass == 0) 3f else 0f
        cc(k, 32f, 62f, 18f + g); cc(k, 52f, 44f, 24f + g); cc(k, 72f, 60f, 18f + g)
        rr(k, 24f - g, 60f - g, 56f + 2 * g, 22f + 2 * g, 11f + g)
    }
}
private fun DrawScope.flamePath(col: Color) = fil(col) {
    moveTo(50f, 4f); cubicTo(58f, 24f, 84f, 38f, 82f, 66f); cubicTo(80f, 86f, 64f, 96f, 50f, 96f)
    cubicTo(34f, 96f, 18f, 86f, 18f, 66f); cubicTo(18f, 50f, 30f, 44f, 34f, 30f); cubicTo(40f, 38f, 44f, 40f, 46f, 34f)
    cubicTo(50f, 26f, 44f, 14f, 50f, 4f); close()
}
private fun DrawScope.flame(a: Color, b: Color, c: Color) {
    flamePath(a); scale(.64f, Offset(50f, 94f)) { flamePath(b) }; scale(.32f, Offset(50f, 94f)) { flamePath(c) }
}
private fun DrawScope.flask(c: Color, dk: Color) {
    val glass = Color(0xFFCFD8DC)
    fil(glass.copy(alpha = .55f)) { moveTo(40f, 8f); lineTo(60f, 8f); lineTo(60f, 36f); lineTo(92f, 88f); lineTo(8f, 88f); lineTo(40f, 36f); close() }
    fil(c) { moveTo(36f, 52f); lineTo(64f, 52f); lineTo(86f, 84f); lineTo(14f, 84f); close() }
    stk(dk, 4f) { moveTo(40f, 8f); lineTo(40f, 36f); lineTo(8f, 88f); lineTo(92f, 88f); lineTo(60f, 36f); lineTo(60f, 8f) }
    ln(dk, 34f, 8f, 66f, 8f, 5f)
}
private fun DrawScope.bottle(c: Color, dk: Color, lt: Color) {
    rr(dk, 38f, 6f, 24f, 24f, 4f); rr(dk, 22f, 26f, 56f, 68f, 14f); rr(c, 27f, 42f, 46f, 47f, 10f)
    rr(lt.copy(alpha = .6f), 32f, 46f, 7f, 34f, 3.5f); rr(Color(0xFF90A4AE), 40f, 8f, 20f, 10f, 3f)
}
private fun DrawScope.sparkle(col: Color, x: Float, y: Float, r: Float) =
    pg(col, x, y - r, x + r * .28f, y - r * .28f, x + r, y, x + r * .28f, y + r * .28f, x, y + r, x - r * .28f, y + r * .28f, x - r, y, x - r * .28f, y - r * .28f)
private fun DrawScope.trefoil(x: Float, y: Float, r: Float, bg: Color, fg: Color) {
    cc(bg, x, y, r)
    for (a in intArrayOf(30, 150, 270)) drawArc(fg, a.toFloat(), 60f, true, Offset(x - r * .8f, y - r * .8f), Size(r * 1.6f, r * 1.6f))
    cc(bg, x, y, r * .26f); cc(fg, x, y, r * .18f)
}
private fun DrawScope.gear(c: Color, dk: Color, cx: Float, cy: Float, r: Float) {
    for (k in 0 until 8) rotate(k * 45f, Offset(cx, cy)) { rr(dk, cx - r * .22f, cy - r * 1.12f, r * .44f, r * .5f, 2f) }
    cc(dk, cx, cy, r * 1.02f); cc(c, cx, cy, r * .88f); cc(dk, cx, cy, r * .38f); cc(Color(0xFFECEFF1), cx, cy, r * .2f)
}

private fun DrawScope.logic(c: Color, dk: Color, id: Int) {
    // входы/выходы
    val wire = Color(0xFFB0BEC5)
    if (id != 48) { ln(wire, 4f, 34f, 24f, 34f, 5f); ln(wire, 4f, 66f, 24f, 66f, 5f) } else ln(wire, 4f, 50f, 22f, 50f, 5f)
    val outX = if (id == 48 || id == 52 || id == 53) 84f else 80f
    ln(wire, outX, 50f, 97f, 50f, 5f)
    val body: Path.() -> Unit = when (id) {
        48 -> ({ moveTo(22f, 18f); lineTo(22f, 82f); lineTo(74f, 50f); close() })
        49, 52 -> ({ moveTo(24f, 18f); lineTo(52f, 18f); cubicTo(90f, 18f, 90f, 82f, 52f, 82f); lineTo(24f, 82f); close() })
        else -> ({ moveTo(22f, 18f); quadraticBezierTo(62f, 18f, 88f, 50f); quadraticBezierTo(62f, 82f, 22f, 82f); quadraticBezierTo(42f, 50f, 22f, 18f); close() })
    }
    fil(c, body); stk(dk, 4f, body)
    if (id == 48 || id == 52 || id == 53) { cc(c, 80f, 50f, 7f); ring(dk, 80f, 50f, 7f, 3.5f) }
    if (id == 51) stk(dk, 4f) { moveTo(12f, 18f); quadraticBezierTo(32f, 50f, 12f, 82f) }
}

private fun DrawScope.element(id: Int, c: Color, dk: Color, lt: Color, k: K) {
    rr(dk, 5f, 5f, 90f, 90f, 15f); rr(c, 9f, 9f, 82f, 82f, 12f); rr(lt.copy(alpha = .35f), 13f, 13f, 74f, 12f, 6f)
    when (k) {
        K.GAS -> { cc(Color.White.copy(alpha = .75f), 72f, 82f, 3f); cc(Color.White.copy(alpha = .75f), 80f, 74f, 2.4f); cc(Color.White.copy(alpha = .75f), 82f, 84f, 2f) }
        K.LIQUID -> { scale(.2f, Offset(82f, 82f)) { dropShape(Color.White.copy(alpha = .85f)) } }
        else -> {}
    }
    if (id == 107 || id == 108 || id == 109) trefoil(80f, 20f, 9f, Color(0xFFFFEB3B), Color(0xFF212121))
}

private fun DrawScope.icon(id: Int, e: El) {
    var c = Color(e.color)
    if (c.luminance() < .12f) c = lerp(c, Color.White, .25f)
    val dk = lerp(c, Color.Black, .4f)
    val lt = lerp(c, Color.White, .5f)
    val metal = Color(0xFFB0BEC5); val dm = Color(0xFF455A64)
    when (id) {
        in 59..110 -> element(id, c, dk, lt, e.k)
        1 -> { flask(c, dk); cc(lt, 42f, 70f, 4f); cc(lt, 58f, 62f, 3f); cc(lt, 52f, 76f, 2.6f); ring(c, 56f, 28f, 4f, 2f); ring(c, 46f, 20f, 3f, 2f) }
        2 -> { drop(c, dk); stk(Color(0xFF7E57C2).copy(alpha = .8f), 4f) { moveTo(40f, 66f); quadraticBezierTo(50f, 58f, 62f, 68f) }; stk(Color(0xFF26C6DA).copy(alpha = .7f), 3f) { moveTo(40f, 76f); quadraticBezierTo(50f, 70f, 60f, 78f) } }
        3 -> { rr(dk, 20f, 24f, 60f, 70f, 8f); rr(c, 25f, 29f, 50f, 60f, 5f); rr(dk, 58f, 8f, 22f, 18f, 4f)
            stk(dk, 5f) { moveTo(30f, 26f); lineTo(30f, 12f); lineTo(56f, 12f) }
            scale(.36f, Offset(50f, 62f)) { flame(Color(0xFFE65100), Color(0xFFFF9800), Color(0xFFFFF59D)) } }
        4 -> { bottle(c, dk, lt); ln(Color(0xFF1565C0), 36f, 62f, 64f, 62f, 3f) }
        5 -> { stk(dk, 4f) { moveTo(20f, 34f); quadraticBezierTo(50f, -8f, 80f, 34f) }
            pg(dk, 16f, 32f, 84f, 32f, 76f, 92f, 24f, 92f); rr(c, 18f, 24f, 64f, 14f, 7f); rr(c, 30f, 34f, 9f, 24f, 4.5f); rr(c, 58f, 34f, 9f, 32f, 4.5f)
            rr(lt.copy(alpha = .5f), 28f, 64f, 44f, 8f, 4f) }
        6 -> { flask(c, dk); st6(lt) }
        7 -> { cc(dk, 50f, 44f, 22f); rr(dk, 14f, 60f, 72f, 34f, 17f); cc(dk, 50f, 66f, 36f)
            cc(c, 50f, 46f, 19f); rr(c, 18f, 64f, 64f, 26f, 13f); cc(c, 50f, 66f, 32f)
            cc(Color.White, 39f, 58f, 8f); cc(Color.White, 61f, 58f, 8f); cc(Color(0xFF263238), 41f, 60f, 3.6f); cc(Color(0xFF263238), 59f, 60f, 3.6f)
            stk(dk, 3.5f) { moveTo(40f, 74f); quadraticBezierTo(50f, 82f, 60f, 74f) } }
        8 -> { rr(dk, 28f, 6f, 44f, 62f, 8f); rr(c, 32f, 10f, 36f, 54f, 5f); rr(Color(0xFF42A5F5), 36f, 56f, 28f, 14f, 3f)
            pg(dk, 40f, 68f, 60f, 68f, 54f, 82f, 46f, 82f); scale(.28f, Offset(50f, 92f)) { dropShape(c) }; ln(lt, 38f, 16f, 38f, 44f, 3f) }
        9 -> { drop(c, dk); scale(.5f, Offset(50f, 72f)) { flame(Color(0xFFBF360C), Color(0xFFFFB300), Color(0xFFFFF59D)) } }
        10 -> { drop(c, dk); flake(Color.White, 50f, 62f, 16f, 4f) }
        11 -> { rr(dk, 34f, 6f, 32f, 18f, 4f); rr(dk, 20f, 22f, 60f, 72f, 14f); rr(Color.White, 25f, 40f, 50f, 48f, 8f); rr(c, 25f, 40f, 50f, 48f, 8f)
            rr(Color(0xFF29B6F6), 34f, 6f, 32f, 12f, 3f); scale(.4f, Offset(50f, 66f)) { dropShape(Color(0xFF29B6F6)) } }
        12 -> { pile(c, dk, lt); ln(Color(0xFFBCAAA4), 50f, 30f, 62f, 14f, 3f); sparkle(Color(0xFFFFC107), 64f, 12f, 11f) }
        13 -> { for (k in 0 until 3) { val x = 10f + k * 29f; rr(dk, x, 16f, 26f, 72f, 7f); rr(c, x + 3f, 19f, 20f, 66f, 5f); rr(Color(0xFFFFE0B2), x + 3f, 42f, 20f, 18f, 0f) }
            stk(Color(0xFF6D4C41), 3f) { moveTo(50f, 16f); quadraticBezierTo(50f, 4f, 64f, 6f) }; sparkle(Color(0xFFFFC107), 68f, 8f, 8f) }
        14 -> { rr(dk, 8f, 28f, 84f, 52f, 9f); rr(c, 12f, 32f, 76f, 44f, 7f); rr(Color(0xFF1B1B1B), 22f, 40f, 40f, 24f, 4f)
            for (k in 0 until 3) rr(Color(0xFFFF1744), 27f + k * 11f, 46f, 7f, 12f, 1.5f)
            stk(Color(0xFFD32F2F), 3f) { moveTo(70f, 32f); quadraticBezierTo(74f, 12f, 92f, 14f) }; stk(Color(0xFF1976D2), 3f) { moveTo(78f, 32f); quadraticBezierTo(80f, 20f, 92f, 24f) }
            cc(Color(0xFF1B1B1B), 74f, 52f, 6f) }
        15 -> { pile(c, dk, lt); sparkle(Color(0xFFFFAB00), 36f, 36f, 9f); sparkle(Color(0xFFFF6D00), 66f, 28f, 7f); cc(Color(0xFFFFD54F), 54f, 18f, 3f) }
        16 -> { pile(c, dk, lt); cc(Color(0xFFFFEB3B), 40f, 64f, 4f); cc(Color(0xFF00E5FF), 56f, 54f, 4f); cc(Color(0xFF69F0AE), 62f, 74f, 4f)
            cc(Color.White, 32f, 80f, 3.4f); cc(Color(0xFF7C4DFF), 50f, 80f, 4f); rr(Color(0xFFFFEB3B), 70f, 20f, 10f, 4f, 1f); rr(Color(0xFF00E5FF), 24f, 26f, 10f, 4f, 1f) }
        17 -> { rr(dk, 14f, 48f, 38f, 38f, 5f); rr(c, 17f, 51f, 32f, 32f, 3f); rr(dk, 50f, 58f, 34f, 30f, 5f); rr(c, 53f, 61f, 28f, 24f, 3f)
            cc(Color.White.copy(alpha = .55f), 30f, 36f, 11f); cc(Color.White.copy(alpha = .55f), 50f, 28f, 13f); cc(Color.White.copy(alpha = .55f), 70f, 40f, 10f) }
        18 -> { rr(dk, 8f, 34f, 84f, 56f, 10f); rr(c, 12f, 38f, 76f, 48f, 7f); rr(dk, 18f, 18f, 26f, 20f, 5f); rr(dk, 56f, 18f, 26f, 20f, 5f)
            rr(c, 21f, 21f, 20f, 15f, 3f); rr(c, 59f, 21f, 20f, 15f, 3f); rr(lt.copy(alpha = .55f), 18f, 42f, 40f, 8f, 4f) }
        19 -> { cc(Color(0xFF111111), 50f, 50f, 44f); cc(c, 50f, 50f, 37f); for (k in 0 until 12) rotate(k * 30f, Offset(50f, 50f)) { rr(Color(0xFF1B2327), 47f, 8f, 6f, 12f, 2f) }
            cc(metal, 50f, 50f, 15f); cc(dm, 50f, 50f, 6f) }
        20 -> { pile(c, dk, lt); cc(Color(0xFF4E342E), 44f, 74f, 3f) }
        21 -> { pile(c, dk, lt) }
        22 -> { drop(c, dk); stk(lt, 3f) { moveTo(34f, 68f); quadraticBezierTo(42f, 62f, 50f, 68f); quadraticBezierTo(58f, 74f, 66f, 68f) } }
        23 -> { pg(dk, 8f, 72f, 18f, 32f, 44f, 12f, 76f, 20f, 94f, 50f, 88f, 82f, 50f, 92f); pg(c, 14f, 70f, 23f, 36f, 45f, 18f, 73f, 25f, 88f, 50f, 83f, 77f, 50f, 85f)
            pg(lt.copy(alpha = .35f), 23f, 36f, 45f, 18f, 60f, 22f, 40f, 40f); stk(dk, 3f) { moveTo(56f, 26f); lineTo(52f, 46f); lineTo(64f, 60f) } }
        24 -> { drop(c, dk); scale(.55f, Offset(50f, 68f)) { dropShape(Color(0xFFFFC107)) }; cc(Color(0xFFFFEB3B), 44f, 62f, 4f); cc(Color(0xFFFFEB3B), 58f, 74f, 3f) }
        25 -> { pile(c, Color(0xFF90CAF9), Color(0xFFBBDEFB)); flake(Color(0xFF64B5F6), 50f, 62f, 12f, 3f) }
        26 -> { block(Color(0xFF81D4FA), Color(0xFF0288D1), Color.White); pg(Color.White.copy(alpha = .5f), 20f, 62f, 62f, 20f, 70f, 28f, 28f, 70f)
            pg(Color.White.copy(alpha = .35f), 46f, 78f, 78f, 46f, 80f, 54f, 54f, 80f) }
        27 -> { for (k in 0 until 3) { val x = 24f + k * 26f
            stk(Color(0xFFECEFF1), 7f) { moveTo(x, 92f); cubicTo(x - 14f, 76f, x + 14f, 62f, x, 46f); cubicTo(x - 10f, 36f, x + 6f, 26f, x, 14f) } } }
        28 -> { cc(Color(0xFF5D4037), 50f, 50f, 44f); cc(c, 50f, 50f, 36f); ring(dk, 50f, 50f, 26f, 3f); ring(dk, 50f, 50f, 16f, 3f); cc(dk, 50f, 50f, 5f) }
        29 -> { rr(Color(0xFF5D4037), 8f, 34f, 84f, 58f, 10f); rr(Color(0xFF795548), 12f, 40f, 76f, 48f, 7f); rr(dk, 8f, 30f, 84f, 24f, 9f); rr(c, 10f, 30f, 80f, 20f, 8f)
            for (k in 0 until 4) pg(c, 14f + k * 20f, 34f, 20f + k * 20f, 12f + (k % 2) * 6f, 26f + k * 20f, 34f) }
        30 -> { for ((x, y, a) in listOf(Triple(34f, 78f, -30f), Triple(62f, 80f, 25f), Triple(48f, 62f, 70f))) rotate(a, Offset(x, y)) { drawOval(dk, Offset(x - 13f, y - 8f), Size(26f, 16f)); drawOval(c, Offset(x - 11f, y - 6f), Size(22f, 12f)) }
            stk(Color(0xFF66BB6A), 5f) { moveTo(50f, 54f); quadraticBezierTo(50f, 34f, 68f, 26f) }; pg(Color(0xFF66BB6A), 52f, 40f, 34f, 28f, 36f, 44f); pg(Color(0xFF81C784), 66f, 28f, 84f, 30f, 72f, 44f) }
        31 -> { pg(dk, 6f, 80f, 14f, 50f, 34f, 38f, 48f, 60f, 54f, 90f, 12f, 90f); pg(Color(0xFF616161), 12f, 80f, 18f, 54f, 34f, 45f, 44f, 62f, 48f, 84f)
            pg(dk, 44f, 86f, 52f, 40f, 72f, 26f, 92f, 50f, 88f, 90f); pg(Color(0xFF616161), 52f, 82f, 58f, 46f, 72f, 34f, 84f, 52f, 82f, 84f); ln(Color(0xFFBDBDBD), 22f, 56f, 28f, 50f, 3f) }
        32 -> { pile(c, dk, lt) }
        33 -> { for ((x, y, r) in listOf(Triple(30f, 72f, 16f), Triple(58f, 74f, 17f), Triple(44f, 50f, 14f), Triple(72f, 52f, 12f))) { cc(dk, x, y, r + 2.5f); cc(c, x, y, r); cc(lt.copy(alpha = .5f), x - r * .3f, y - r * .3f, r * .3f) } }
        34 -> { rr(dk, 28f, 28f, 44f, 64f, 10f); rr(c, 32f, 32f, 36f, 56f, 7f); rr(metal, 28f, 6f, 44f, 26f, 9f); rr(dm, 28f, 6f, 44f, 26f, 9f)
            rr(metal, 31f, 9f, 38f, 20f, 7f); for (k in 0 until 3) cc(dm, 38f + k * 12f, 18f, 2.4f); rr(Color(0xFF90CAF9), 32f, 58f, 36f, 8f, 0f) }
        35 -> { drop(c, dk); stk(lt, 3f) { moveTo(32f, 64f); quadraticBezierTo(40f, 58f, 48f, 64f); quadraticBezierTo(56f, 70f, 64f, 64f) }; stk(lt, 3f) { moveTo(34f, 76f); quadraticBezierTo(42f, 70f, 50f, 76f); quadraticBezierTo(58f, 82f, 66f, 76f) } }
        36 -> { cloud(Color(0xFFF5F7F8), Color(0xFF90A4AE)); stk(Color(0xFFB0BEC5), 3f) { moveTo(36f, 68f); quadraticBezierTo(46f, 62f, 56f, 68f) } }
        37 -> flame(Color(0xFFE65100), Color(0xFFFFB300), Color(0xFFFFF59D))
        38 -> { cloud(Color(0xFF78909C), Color(0xFF37474F)); cc(Color(0xFF90A4AE), 40f, 54f, 7f) }
        39 -> { pg(Color(0xFF1A0F52), 50f, 4f, 82f, 34f, 72f, 94f, 28f, 94f, 18f, 34f); pg(c, 50f, 12f, 74f, 36f, 66f, 86f, 34f, 86f, 26f, 36f)
            pg(Color.White.copy(alpha = .45f), 34f, 36f, 50f, 20f, 50f, 40f); ln(Color.White.copy(alpha = .5f), 40f, 62f, 40f, 78f, 3f) }
        40 -> { for ((x, y, r) in listOf(Triple(28f, 68f, 18f), Triple(52f, 60f, 22f), Triple(74f, 70f, 16f), Triple(46f, 40f, 14f), Triple(66f, 44f, 12f))) { cc(dk, x, y, r + 2f); cc(c, x, y, r) }
            for ((x, y) in listOf(36f to 62f, 56f to 54f, 70f to 66f, 46f to 36f)) cc(Color(0xFF66BB6A), x, y, 3f) }
        41, in 128..142 -> { val o = lerp(c, Color.Black, .65f)       // провод любого из 16 цветов
            val path: Path.() -> Unit = { moveTo(8f, 70f); lineTo(36f, 70f); lineTo(36f, 30f); lineTo(64f, 30f); lineTo(64f, 70f); lineTo(92f, 70f) }
            stk(o, 17f, path); stk(c, 9f, path) }
        42 -> { rr(dk, 26f, 20f, 48f, 74f, 8f); rr(c, 30f, 24f, 40f, 66f, 5f); rr(Color(0xFFB0BEC5), 40f, 8f, 20f, 14f, 3f); rr(Color(0xFF263238), 30f, 66f, 40f, 24f, 5f)
            ln(Color.White, 50f, 34f, 50f, 54f, 6f); ln(Color.White, 40f, 44f, 60f, 44f, 6f) }
        43 -> { rr(dk, 8f, 62f, 84f, 28f, 7f); rr(Color(0xFF8BC34A), 12f, 66f, 76f, 20f, 5f); cc(metal, 22f, 62f, 6f); cc(metal, 78f, 62f, 6f)
            ln(Color(0xFFEEEEEE), 22f, 62f, 70f, 24f, 9f); cc(Color(0xFFFF5252), 72f, 22f, 10f) }
        44 -> { rr(dk, 10f, 58f, 80f, 32f, 8f); rr(c, 14f, 62f, 72f, 24f, 5f); cc(Color(0xFF8E2A0D), 50f, 52f, 30f); cc(c, 50f, 48f, 26f); cc(Color.White.copy(alpha = .4f), 41f, 38f, 8f) }
        45 -> { cc(dm, 50f, 52f, 44f); cc(Color.White, 50f, 52f, 36f); for (k in 0 until 12) rotate(k * 30f, Offset(50f, 52f)) { ln(dm, 50f, 20f, 50f, if (k % 3 == 0) 28f else 24f, 2.6f) }
            ln(dk, 50f, 52f, 50f, 28f, 5f); ln(dk, 50f, 52f, 68f, 62f, 5f); cc(c, 50f, 52f, 5f); rr(c, 40f, 4f, 20f, 8f, 3f) }
        46, LED_G, LED_B -> {                                           // светодиод: красный, зелёный, синий
            val p = when (id) {
                46 -> longArrayOf(0xFFD50000, 0xFFFF5252, 0xFFB71C1C, 0xFFFF1744)
                LED_G -> longArrayOf(0xFF00A152, 0xFF69F0AE, 0xFF007E3A, 0xFF00E676)
                else -> longArrayOf(0xFF1565C0, 0xFF82B1FF, 0xFF0D47A1, 0xFF448AFF)
            }
            cc(Color(p[3]).copy(alpha = .22f), 50f, 40f, 40f); rr(Color(p[0]), 24f, 14f, 52f, 56f, 26f); rr(Color(p[1]), 28f, 18f, 44f, 54f, 22f)
            rr(Color(p[2]), 20f, 62f, 60f, 10f, 3f); ln(metal, 38f, 72f, 38f, 94f, 5f); ln(metal, 62f, 72f, 62f, 86f, 5f); rr(Color.White.copy(alpha = .6f), 34f, 24f, 8f, 18f, 4f) }
        47 -> { rr(Color(0xFF111111), 10f, 10f, 80f, 80f, 12f); rr(Color(0xFF2B2B2B), 14f, 14f, 72f, 72f, 9f)
            rr(Color(0xFFFF5252), 22f, 22f, 16f, 56f, 3f); rr(Color(0xFF69F0AE), 42f, 22f, 16f, 56f, 3f); rr(Color(0xFF448AFF), 62f, 22f, 16f, 56f, 3f) }
        48, 49, 50, 51, 52, 53 -> logic(c, dk, id)
        54 -> { rr(Color(0xFFB0BEC5), 4f, 30f, 14f, 5f, 2f); rr(Color(0xFFB0BEC5), 4f, 62f, 14f, 5f, 2f); rr(Color(0xFFB0BEC5), 82f, 46f, 14f, 5f, 2f)
            rr(dk, 16f, 14f, 68f, 72f, 10f); rr(c, 20f, 18f, 60f, 64f, 7f)
            stk(Color.White, 6f) { moveTo(38f, 32f); lineTo(38f, 68f); lineTo(50f, 68f); quadraticBezierTo(68f, 68f, 68f, 50f); quadraticBezierTo(68f, 32f, 50f, 32f); close() } }
        55 -> { val b: Path.() -> Unit = { moveTo(6f, 36f); lineTo(52f, 36f); lineTo(52f, 14f); lineTo(94f, 50f); lineTo(52f, 86f); lineTo(52f, 64f); lineTo(6f, 64f); close() }; fil(c, b); stk(dk, 4f, b) }
        56 -> { val b: Path.() -> Unit = { moveTo(4f, 50f); lineTo(30f, 18f); lineTo(30f, 38f); lineTo(70f, 38f); lineTo(70f, 18f); lineTo(96f, 50f); lineTo(70f, 82f); lineTo(70f, 62f); lineTo(30f, 62f); lineTo(30f, 82f); close() }; fil(c, b); stk(dk, 4f, b) }
        57 -> { pg(dk, 20f, 6f, 80f, 6f, 80f, 16f, 58f, 50f, 80f, 84f, 80f, 94f, 20f, 94f, 20f, 84f, 42f, 50f, 20f, 16f); pg(Color(0xFFB3E5FC).copy(alpha = .6f), 28f, 14f, 72f, 14f, 52f, 46f, 48f, 46f)
            pg(Color(0xFFFFD54F), 36f, 18f, 64f, 18f, 52f, 36f, 48f, 36f); pg(Color(0xFFFFD54F), 50f, 62f, 66f, 86f, 34f, 86f); ln(Color(0xFFFFD54F), 50f, 46f, 50f, 64f, 2.5f); rr(c, 14f, 2f, 72f, 8f, 3f); rr(c, 14f, 90f, 72f, 8f, 3f) }
        58 -> { ln(lt, 50f, 6f, 50f, 94f, 11f); ln(dk, 6f, 50f, 34f, 50f, 11f); ln(dk, 66f, 50f, 94f, 50f, 11f); stk(dk, 11f) { moveTo(30f, 50f); quadraticBezierTo(50f, 14f, 70f, 50f) }; stk(c, 5f) { moveTo(30f, 50f); quadraticBezierTo(50f, 14f, 70f, 50f) } }
        HBAT -> { rr(dm, 6f, 14f, 88f, 72f, 10f); rr(Color(0xFFCFD8DC), 10f, 18f, 80f, 64f, 7f)           // радиатор: рёбра, тёплые полосы, патрубки
            for (k in 0 until 5) { val x = 18f + k * 16f; rr(Color(0xFF90A4AE), x, 22f, 10f, 56f, 5f); rr(Color(0xFFFF7043).copy(alpha = .75f), x + 2f, 30f, 6f, 40f, 3f) }
            ln(dm, 2f, 30f, 8f, 30f, 6f); ln(dm, 92f, 30f, 98f, 30f, 6f); ln(dm, 2f, 70f, 8f, 70f, 6f); ln(dm, 92f, 70f, 98f, 70f, 6f) }
        PUMP -> { rr(dk, 2f, 28f, 96f, 44f, 10f); rr(c, 6f, 32f, 88f, 36f, 7f)
            cc(dk, 50f, 50f, 26f); cc(Color(0xFF263238), 50f, 50f, 22f)
            for (k in 0 until 3) rotate(k * 120f + 20f, Offset(50f, 50f)) { pg(lt, 50f, 50f, 39f, 29f, 61f, 29f) }
            cc(Color.White, 50f, 50f, 5f)
            pg(Color.White, 74f, 40f, 92f, 50f, 74f, 60f) }
        HEAT -> { cc(Color(0xFF8E2A0D), 50f, 50f, 46f); cc(Color(0xFFFF5722), 50f, 50f, 40f)
            scale(.62f, Offset(46f, 70f)) { flame(Color(0xFFFFB300), Color(0xFFFFEB3B), Color(0xFFFFFDE7)) }
            ln(Color.White, 72f, 22f, 90f, 22f, 6f); ln(Color.White, 81f, 13f, 81f, 31f, 6f) }
        COLD -> { cc(Color(0xFF01579B), 50f, 50f, 46f); cc(Color(0xFF29B6F6), 50f, 50f, 40f)
            flake(Color.White, 48f, 56f, 26f, 6f); ln(Color.White, 72f, 22f, 90f, 22f, 6f) }
        111 -> { gear(c, dk, 46f, 58f, 30f); pg(Color(0xFFFFEB3B), 84f, 4f, 66f, 30f, 77f, 30f, 70f, 50f, 94f, 22f, 81f, 22f, 90f, 4f); cc(Color.White.copy(alpha = .7f), 22f, 22f, 8f); cc(Color.White.copy(alpha = .5f), 34f, 12f, 6f) }
        112 -> { rr(dk, 36f, 4f, 28f, 92f, 7f); rr(c, 40f, 8f, 20f, 84f, 5f); for (k in 0 until 4) pg(Color(0xFFFFEB3B), 40f, 18f + k * 20f, 60f, 10f + k * 20f, 60f, 20f + k * 20f, 40f, 28f + k * 20f) }
        113 -> stk(Color(0xFF29B6F6), 11f) { moveTo(50f, 50f); cubicTo(62f, 26f, 94f, 26f, 94f, 50f); cubicTo(94f, 74f, 62f, 74f, 50f, 50f); cubicTo(38f, 26f, 6f, 26f, 6f, 50f); cubicTo(6f, 74f, 38f, 74f, 50f, 50f) }
        114 -> { rr(dk, 10f, 10f, 80f, 80f, 7f); rr(Color(0xFFB2EBF2), 15f, 15f, 70f, 70f, 4f); ln(dk, 50f, 15f, 50f, 85f, 4f); ln(dk, 15f, 50f, 85f, 50f, 4f)
            pg(Color.White.copy(alpha = .65f), 20f, 20f, 40f, 20f, 20f, 40f); pg(Color.White.copy(alpha = .5f), 56f, 20f, 70f, 20f, 56f, 34f) }
        115 -> { pile(c, dk, lt); cc(Color(0xFFFFAB91), 38f, 70f, 3f); cc(Color(0xFFFFAB91), 60f, 60f, 3f) }
        116 -> { rr(dk, 8f, 16f, 84f, 68f, 4f); rr(c, 11f, 19f, 78f, 62f, 3f)
            for (y in floatArrayOf(39f, 60f)) ln(Color(0xFFD7CCC8), 8f, y, 92f, y, 3f)
            for (x in floatArrayOf(38f, 68f)) ln(Color(0xFFD7CCC8), x, 19f, x, 39f, 3f)
            for (x in floatArrayOf(24f, 54f, 82f)) ln(Color(0xFFD7CCC8), x, 39f, x, 60f, 3f)
            for (x in floatArrayOf(38f, 68f)) ln(Color(0xFFD7CCC8), x, 60f, x, 81f, 3f) }
        117 -> { pg(dm, 14f, 34f, 86f, 34f, 74f, 84f, 26f, 84f); rr(Color(0xFFFFB300), 12f, 26f, 76f, 16f, 8f); rr(Color(0xFFFF6D00), 18f, 30f, 64f, 8f, 4f)
            cc(Color(0xFFFFEE58), 40f, 32f, 3f); cc(Color(0xFFFFEE58), 62f, 34f, 3f); scale(.22f, Offset(50f, 96f)) { dropShape(Color(0xFFFF8F00)) } }
        118 -> { pg(dm, 6f, 74f, 24f, 34f, 92f, 34f, 76f, 74f); pg(Color(0xFFCFD8DC), 26f, 38f, 88f, 38f, 74f, 60f, 12f, 60f); rr(Color(0xFF78909C), 6f, 60f, 70f, 22f, 4f)
            pg(Color(0xFF546E7A), 76f, 60f, 92f, 38f, 92f, 56f, 76f, 82f); ln(Color.White.copy(alpha = .6f), 30f, 46f, 70f, 46f, 3f) }
        119 -> { pile(c, dk, lt); cc(Color(0xFFFF7043), 44f, 76f, 2.4f); cc(Color(0xFFFF7043), 58f, 66f, 2f) }
        120 -> { rr(dk, 6f, 52f, 88f, 40f, 20f); cc(dk, 32f, 54f, 24f); cc(dk, 64f, 50f, 20f); rr(c, 10f, 56f, 80f, 32f, 16f); cc(c, 32f, 54f, 20f); cc(c, 64f, 50f, 16f)
            cc(lt, 36f, 48f, 4f); cc(lt, 60f, 44f, 3f); cc(lt, 70f, 66f, 3.4f) }
        121 -> { block(c, dk, lt); for ((x, y) in listOf(30f to 40f, 60f to 30f, 70f to 62f, 36f to 70f, 52f to 52f)) { cc(dk, x, y, 3.2f); cc(lt, x + 9f, y + 7f, 2.4f) } }
        122 -> { rr(dk, 20f, 10f, 60f, 82f, 10f); rr(Color(0xFF9E9D24), 24f, 14f, 52f, 74f, 7f); rr(Color(0xFFC6FF00), 24f, 14f, 52f, 74f, 7f)
            ln(Color(0xFF827717), 24f, 28f, 76f, 28f, 4f); ln(Color(0xFF827717), 24f, 76f, 76f, 76f, 4f); trefoil(50f, 52f, 19f, Color(0xFFFFEB3B), Color(0xFF212121)) }
        123 -> { flask(c, Color(0xFF8D6E63)); rr(Color(0xFFD50000), 47f, 56f, 6f, 16f, 3f); cc(Color(0xFFD50000), 50f, 79f, 3.6f) }
        124 -> { pg(dk, 8f, 36f, 26f, 40f, 22f, 52f, 8f, 66f); pg(dk, 8f, 66f, 22f, 60f, 26f, 60f); rr(dk, 16f, 32f, 78f, 38f, 19f); rr(c, 20f, 36f, 70f, 30f, 15f)
            rr(lt.copy(alpha = .5f), 28f, 38f, 44f, 6f, 3f); trefoil(56f, 51f, 13f, Color(0xFFFFEB3B), Color(0xFF212121)); pg(Color(0xFFB71C1C), 80f, 44f, 98f, 51f, 80f, 58f) }
        else -> when (e.k) {
            K.POWDER -> pile(c, dk, lt)
            K.LIQUID -> drop(c, dk)
            K.GAS -> cloud(c, dk)
            else -> block(c, dk, lt)
        }
    }
}

private fun DrawScope.st6(lt: Color) { // гексагон-молекула для щёлочи
    stk(lt, 3f) { moveTo(50f, 58f); lineTo(58f, 63f); lineTo(58f, 73f); lineTo(50f, 78f); lineTo(42f, 73f); lineTo(42f, 63f); close() }
}
