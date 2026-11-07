package com.sandbox.app

enum class K { SOLID, POWDER, LIQUID, GAS, ELEC }
class El(val id: Int, val name: String, val color: Int, val k: K, val dens: Int)

val GX = intArrayOf(0, 1, 0, -1)    // вектор гравитации по направлению gdir
val GY = intArrayOf(1, 0, -1, 0)
val DX = intArrayOf(0, 1, 0, -1)   // 0=вверх 1=вправо 2=вниз 3=влево
val DY = intArrayOf(-1, 0, 1, 0)
/** Размер пространства id (раньше было 128 — на 16 цветов проводов и новые светодиоды не хватало). */
const val NID = 256

// ---- провода: 16 цветов. Цвет 0 — прежний id 41 (старые сохранения остаются рабочими), цвета 1..15 — id 128..142 ----
const val LED_G = 143    // светодиод зелёный (красный — прежний id 46)
const val LED_B = 144    // светодиод синий
const val WIRE_COLORS = 16
const val HBAT = 145     // тепловая батарея (радиатор): электричество не даёт, зато отлично проводит тепло
fun isWire(id: Int) = id == 41 || id in 128..142
fun wireIdx(id: Int) = if (id == 41) 0 else id - 127            // id провода -> номер цвета 0..15
fun wireId(idx: Int) = if (idx == 0) 41 else 127 + idx          // номер цвета -> id провода
fun isLed(id: Int) = id == 46 || id == LED_G || id == LED_B
val WIRE_NAMES = arrayOf("жёлтый", "красный", "оранжевый", "лаймовый", "зелёный", "бирюзовый", "голубой", "синий",
    "индиго", "фиолетовый", "пурпурный", "розовый", "белый", "серый", "коричневый", "бордовый")
val WIRE_HEX = intArrayOf(0xFFEB3B, 0xF44336, 0xFF9800, 0xCDDC39, 0x4CAF50, 0x1DE9B6, 0x00E5FF, 0x2196F3,
    0x3F51B5, 0xAB47BC, 0xE040FB, 0xFF4081, 0xFAFAFA, 0x9E9E9E, 0x8D6E63, 0x880E4F)

fun isE(t: Int) = t in 41..58 || t == 111 || t in 128..144
const val PUMP = 125    // насос (раздел «Электрика», но электричество ему не нужно)
const val HEAT = 126     // инструмент: палец греет
const val COLD = 127     // инструмент: палец охлаждает
fun isTool(id: Int) = id == HEAT || id == COLD
/** Можно ли этот предмет превращать в «источник» (∞). */
fun srcable(id: Int) = !(isE(id) || id == PUMP || isTool(id) || id == 0)

/**
 * Таблица всех предметов.
 * ЧТОБЫ ДОБАВИТЬ ПРЕДМЕТ: добавь строку a(...) здесь, правила реакций — в World.initRules(),
 * иконку — в Icons.kt (если не нарисовать, возьмётся общая по типу вещества).
 */
object Els {
    val list = arrayOfNulls<El>(NID)
    val sym = Array(NID) { "" }        // химический символ (только у элементов)
    val num = IntArray(NID)            // атомный номер
    val heavyGas = BooleanArray(NID)   // тяжёлые газы оседают вниз
    private fun a(id: Int, n: String, hex: Int, k: K, d: Int = 0) {
        list[id] = El(id, n, (0xFF shl 24) or hex, k, if (k == K.POWDER) 10 else d)
    }
    init {
        // 1-19 разное
        a(1,"Кислота",0x7CFC00,K.LIQUID,8); a(2,"Нефть",0x2B2B2B,K.LIQUID,5); a(3,"Бензин",0xE0B030,K.LIQUID,4)
        a(4,"Спирт",0xB3E5FC,K.LIQUID,3); a(5,"Краска",0xE91E63,K.LIQUID,6); a(6,"Щёлочь",0xBA68C8,K.LIQUID,7)
        a(7,"Слизь",0x9CCC65,K.LIQUID,7); a(8,"Клей",0xFFF8E1,K.LIQUID,9); a(9,"Напалм",0xFF7043,K.LIQUID,9)
        a(10,"Жидкий азот",0x80DEEA,K.LIQUID,6); a(11,"Отбеливатель",0xE1F5FE,K.LIQUID,6)
        a(12,"Порох",0x424242,K.POWDER); a(13,"Тротил",0xB71C1C,K.SOLID); a(14,"C4",0xFFF59D,K.SOLID)
        a(15,"Термит",0xA1887F,K.POWDER); a(16,"Конфетти",0xFF4081,K.POWDER); a(17,"Сухой лёд",0xECEFF1,K.POWDER)
        a(18,"Пластик",0x90CAF9,K.SOLID); a(19,"Резина",0x37474F,K.SOLID)
        // 20-40 природа
        a(20,"Земля",0x795548,K.POWDER); a(21,"Песок",0xE6C27A,K.POWDER); a(22,"Вода",0x2979FF,K.LIQUID,6)
        a(23,"Камень",0x757575,K.SOLID); a(24,"Лава",0xFF5722,K.LIQUID,9); a(25,"Снег",0xFFFFFF,K.POWDER)
        a(26,"Лёд",0xB3E5FC,K.SOLID); a(27,"Пар",0xCFD8DC,K.GAS); a(28,"Дерево",0x8D6E63,K.SOLID)
        a(29,"Трава",0x43A047,K.SOLID); a(30,"Семена",0xC0CA33,K.POWDER); a(31,"Уголь",0x212121,K.POWDER)
        a(32,"Глина",0xBCAAA4,K.POWDER); a(33,"Гравий",0x9E9E9E,K.POWDER); a(34,"Соль",0xFAFAFA,K.POWDER)
        a(35,"Морская вода",0x1565C0,K.LIQUID,7); a(36,"Облако",0xECEFF1,K.GAS); a(37,"Огонь",0xFF9800,K.GAS)
        a(38,"Дым",0x616161,K.GAS); a(39,"Обсидиан",0x311B92,K.SOLID); a(40,"Мох",0x2E7D32,K.SOLID)
        // 41-58 электрика
        val el = listOf("Провод жёлтый" to 0xFFEB3B,"Батарея" to 0x00C853,"Рубильник" to 0x33691E,"Кнопка" to 0xD84315,
            "Часы" to 0x00B8D4,"Светодиод красный" to 0x4A1515,"Пиксель" to 0x212121,"НЕ" to 0x7E57C2,"И" to 0x5C6BC0,
            "ИЛИ" to 0x42A5F5,"ИСКЛ ИЛИ" to 0x26A69A,"И-НЕ" to 0x8E24AA,"ИЛИ-НЕ" to 0x3949AB,"Ячейка памяти" to 0xEC407A,
            "Стрелка →" to 0xFFA726,"Стрелка ↔" to 0xFF7043,"Задержка" to 0x9E9D24,"Мост" to 0x78909C)
        el.forEachIndexed { i, p -> a(41 + i, p.first, p.second, K.ELEC) }
        for (k in 1 until WIRE_COLORS) a(wireId(k), "Провод " + WIRE_NAMES[k], WIRE_HEX[k], K.ELEC)
        a(LED_G, "Светодиод зелёный", 0x123D1E, K.ELEC); a(LED_B, "Светодиод синий", 0x132A55, K.ELEC)
        a(HBAT, "Тепловая батарея", 0xB0BEC5, K.SOLID)
        // 59-110 химические элементы (цвета близки к настоящим)
        val names = ("Водород Гелий Литий Бериллий Бор Углерод Азот Кислород Фтор Неон Натрий Магний Алюминий Кремний Фосфор Сера Хлор " +
            "Аргон Калий Кальций Титан Хром Марганец Железо Кобальт Никель Медь Цинк Галлий Германий Мышьяк Селен Бром Криптон Рубидий " +
            "Стронций Серебро Кадмий Олово Йод Ксенон Цезий Барий Вольфрам Платина Золото Свинец Висмут Уран Радий Плутоний Ртуть").split(" ")
        val syms = ("H He Li Be B C N O F Ne Na Mg Al Si P S Cl Ar K Ca Ti Cr Mn Fe Co Ni Cu Zn Ga Ge As Se Br Kr Rb Sr Ag Cd Sn I Xe Cs Ba " +
            "W Pt Au Pb Bi U Ra Pu Hg").split(" ")
        val nums = intArrayOf(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,22,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,
            47,48,50,53,54,55,56,74,78,79,82,83,92,88,94,80)
        val cols = intArrayOf(0xE1F5FE,0xFFF59D,0xCFD8DC,0xA5B8A0,0x6D4C41,0x2F3B42,0x9FA8DA,0xA7FFEB,0xE6EE9C,0xFF8A65,
            0xFFE0B2,0xF5F5F5,0xB0BEC5,0x78909C,0xEF5350,0xFFEB3B,0xAEEA00,0xB39DDB,0xD1C4E9,0xEEEEEE,
            0x90A4AE,0xB0C4CE,0xAB7FC4,0x8D8F94,0x3F51B5,0xA9B7BE,0xE0763A,0x9DB4B9,0x80CBC4,0x9E9D7B,
            0x7E8A6A,0xC2185B,0x8B2E16,0xE1BEE7,0xF8BBD0,0xEF9A9A,0xECEFF1,0xC5CAE9,0xDCE3E6,0x4A148C,
            0x7986CB,0xFFECB3,0x81C784,0x455A64,0xDADDE0,0xFFD700,0x5C6B73,0xCE93A8,0x7CB342,0x00E5FF,
            0xE040FB,0xE0E6EA)
        val gas = setOf(0,1,6,7,8,9,16,17,33,40); val liq = setOf(32,51); val pow = setOf(4,5,13,14,15,39)
        val heavy = setOf(8,16,17,33,40)
        names.forEachIndexed { i, n ->
            val k = if (i in gas) K.GAS else if (i in liq) K.LIQUID else if (i in pow) K.POWDER else K.SOLID
            val dn = if (k == K.GAS) (if (i in heavy) 3 else 0) else 10 + i
            a(59 + i, n, cols[i], k, dn)
            sym[59 + i] = syms[i]; num[59 + i] = nums[i]; if (i in heavy) heavyGas[59 + i] = true
        }
        // 111+ особые
        a(111,"Паровой двигатель",0x546E7A,K.ELEC); a(112,"Стержень управления",0x4E5B63,K.SOLID); a(113,"Источник",0xFFFFFF,K.SOLID)
        a(114,"Стекло",0xB2EBF2,K.SOLID); a(115,"Ржавчина",0x8D3A1A,K.POWDER); a(116,"Кирпич",0xB5532D,K.SOLID)
        a(117,"Расплав металла",0xFF8F00,K.LIQUID,9); a(118,"Сталь",0x9FB0BA,K.SOLID); a(119,"Пепел",0x8A8A8A,K.POWDER)
        a(120,"Грязь",0x5D4037,K.LIQUID,8); a(121,"Бетон",0xBDBDBD,K.SOLID); a(122,"Радиоактивные отходы",0xC6FF00,K.POWDER)
        a(123,"Нитроглицерин",0xFFF176,K.LIQUID,6); a(124,"Ядерный заряд",0x827717,K.SOLID)
        a(PUMP,"Насос",0x4DB6AC,K.SOLID); a(HEAT,"Нагрев",0xFF5722,K.SOLID); a(COLD,"Охлаждение",0x40C4FF,K.SOLID)
    }
}

private object I {
    const val H = 59; const val He = 60; const val Li = 61; const val Be = 62; const val B = 63; const val C = 64; const val N = 65
    const val O = 66; const val F = 67; const val Ne = 68; const val Na = 69; const val Mg = 70; const val Al = 71; const val Si = 72
    const val P = 73; const val S = 74; const val Cl = 75; const val Ar = 76; const val K = 77; const val Ca = 78; const val Ti = 79
    const val Cr = 80; const val Mn = 81; const val Fe = 82; const val Co = 83; const val Ni = 84; const val Cu = 85; const val Zn = 86
    const val Ga = 87; const val Ge = 88; const val As = 89; const val Se = 90; const val Br = 91; const val Kr = 92; const val Rb = 93
    const val Sr = 94; const val Ag = 95; const val Cd = 96; const val Sn = 97; const val Io = 98; const val Xe = 99; const val Cs = 100
    const val Ba = 101; const val W = 102; const val Pt = 103; const val Au = 104; const val Pb = 105; const val Bi = 106
    const val U = 107; const val Ra = 108; const val Pu = 109; const val Hg = 110
}

class World(val w: Int, val h: Int) {
    /** Температура всего поля (°C): к ней стремится всё вокруг. Много — лёд тает, мало — вода замерзает. */
    var amb = 15f
    /** Сколько градусов за кадр добавляет/забирает палец (инструменты «Нагрев» / «Охлаждение»). */
    var fingerPower = 40f
    /** Куда падает всё: 0 вниз, 1 вправо, 2 вверх, 3 влево (экрана). Задаётся наклоном телефона. */
    @Volatile var gdir = 0
    val t = IntArray(w * h); val s = IntArray(w * h); val s2 = IntArray(w * h); val d = IntArray(w * h)
    val px = IntArray(w * h)
    val tp = FloatArray(w * h) { amb }          // температура клеток (°C)
    /** Температура огня (°C): влияет на нагрев вокруг и на шанс поджечь соседей. */
    var fireTemp = 600f
    var fisHeat = 6.0f; var selfHeat = 0.42f; var meltAt = 700f      // параметры реактора
    private val tn = FloatArray(w * h)
    private val bo = FloatArray(w * h)             // накопленная энергия кипения / таяния
    private val cap = FloatArray(NID) { 1f }       // теплоёмкость: чем больше, тем медленнее греется
    private val kc = FloatArray(NID) { 0.06f }     // теплопроводность вещества
    private val hk = FloatArray(NID * NID)         // проводимость пары соседей (среднее гармоническое — как у последовательных сопротивлений)
    private val ignT = IntArray(NID)           // температура самовоспламенения / детонации
    private val ns = IntArray(w * h); private val ns2 = IntArray(w * h); private val mv = BooleanArray(w * h)
    private val rnd = java.util.Random(); private var tick = 0
    private val kind = Array<K?>(NID) { Els.list[it]?.k }
    private val dens = IntArray(NID) { Els.list[it]?.dens ?: 0 }
    private val rules = arrayOfNulls<ArrayList<IntArray>>(NID * NID)
    val expl = IntArray(NID)                 // предмет -> радиус взрыва (0 = не взрывается)
    private val hard = IntArray(NID)         // стойкость к взрыву 0..10
    private val pend = ArrayList<IntArray>()
    private val seen = IntArray(w * h); private var stamp = 0           // для поиска связных объёмов жидкости
    private val bfs = IntArray(w * h); private val topL = IntArray(w * h); private val lowL = IntArray(w * h)
    private val disp = IntArray(NID) { 4 }                              // как далеко растекается жидкость за кадр (вязкость) // отложенные взрывы (цепная реакция)
    private val plants = intArrayOf(29, 40, 30)

    init { initTables(); initRules() }

    /** Сколько пар веществ имеют хотя бы одну реакцию (для отладки). */
    fun ruleCount(): Int = rules.count { it != null }

    /**
     * Правило: a соседствует с b -> a становится ra, b становится rb, шанс p% за тик.
     * Коды: >=0 новый предмет, -1 без изменений, -2 ВЗРЫВ радиуса rb (центр — взрывчатка, если она в паре),
     * -3 расплавить (металл -> жидкий расплав, помнит металл), -4 застыть (расплав -> исходный металл).
     * rb2/p2: с шансом p2% вместо rb получится rb2 (например пепел вместо огня).
     * Для одной пары можно добавить несколько правил — берётся первое сработавшее.
     */
    private fun r(a: Int, b: Int, ra: Int, rb: Int, p: Int, rb2: Int = -1, p2: Int = 0) {
        val idx = a * NID + b
        val l = rules[idx] ?: ArrayList<IntArray>().also { rules[idx] = it }
        l.add(intArrayOf(ra, rb, p, rb2, p2))
    }

    private fun initTables() {
        for (id in 1 until NID) {
            val k = Els.list[id]?.k ?: continue
            hard[id] = when (k) { K.SOLID -> if (id in 59..110) 5 else 3; K.ELEC -> 1; else -> 0 }
        }
        for ((id, v) in mapOf(23 to 5, 39 to 9, 116 to 6, 121 to 8, 118 to 8, 114 to 1, 26 to 2, 28 to 2, 29 to 1, 40 to 1, 13 to 1,
            14 to 1, 18 to 2, 19 to 2, 112 to 4, 113 to 7, 124 to 6, 79 to 8, 80 to 7, 102 to 10, 103 to 7, 82 to 6, 83 to 6, 84 to 6,
            105 to 4, 104 to 4, 95 to 4, 87 to 2, 97 to 3, 86 to 4, 96 to 3, 106 to 3, 88 to 3)) hard[id] = v
        for ((id, v) in mapOf(28 to 300, 29 to 250, 30 to 250, 31 to 420, 40 to 250, 2 to 260, 3 to 200, 4 to 250, 9 to 200, 5 to 360,
            18 to 380, 19 to 380, 16 to 250, 8 to 450, I.P to 60, I.S to 250, I.Mg to 500, I.Li to 180, I.Na to 130, I.K to 70, I.Rb to 50,
            I.Cs to 40, I.H to 520, I.C to 600, 12 to 300, 13 to 420, 123 to 150, 124 to 1600, 14 to 900)) ignT[id] = v
        for ((id, v) in mapOf(24 to 1, 8 to 1, 7 to 1, 120 to 1, 9 to 1, 117 to 2, 123 to 3, 5 to 3, 2 to 3, 22 to 6, 35 to 6, 4 to 5, 10 to 5)) disp[id] = v
        for ((id, v) in mapOf(22 to 10f, 35 to 10f, 107 to 25f, 109 to 25f, 26 to 4f, 25 to 3f, 2 to 3f, 3 to 2f, 4 to 3f, 120 to 3f)) cap[id] = v
        expl[12] = 4; expl[13] = 9; expl[14] = 14; expl[123] = 7; expl[124] = 30
        hard[PUMP] = 5
        // ---- теплопроводность: металлы проводят лучше всего, дерево/пластик/порошки почти не проводят ----
        for (id in 0 until NID) kc[id] = when (kind[id]) {
            K.LIQUID -> 0.10f; K.POWDER -> 0.03f; K.ELEC -> 0.12f; else -> 0.06f
        }
        for (m in intArrayOf(I.Li, I.Be, I.Na, I.Mg, I.Al, I.K, I.Ca, I.Ti, I.Cr, I.Mn, I.Fe, I.Co, I.Ni, I.Cu, I.Zn, I.Ga, I.Rb, I.Sr,
            I.Ag, I.Cd, I.Sn, I.Cs, I.Ba, I.W, I.Pt, I.Au, I.Pb, I.Bi, I.U, I.Ra, I.Pu, I.Hg, 118, 117, PUMP)) { kc[m] = 0.20f; cap[m] = 2f }
        for (k in 0 until WIRE_COLORS) { kc[wireId(k)] = 0.20f; cap[wireId(k)] = 2f }     // провода любого цвета — медные
        kc[HBAT] = 0.25f; cap[HBAT] = 1.2f        // тепловая батарея: проводит тепло лучше меди и быстро прогревается целиком
        for (m in intArrayOf(I.Cu, I.Ag, I.Au, I.Al)) kc[m] = 0.24f
        for ((id, v) in mapOf(24 to 0.15f, 26 to 0.08f, 25 to 0.03f, 28 to 0.02f, 18 to 0.02f, 19 to 0.02f, 114 to 0.05f, 116 to 0.05f,
            121 to 0.07f, 23 to 0.06f, 39 to 0.05f, 27 to 0.05f, 37 to 0.08f)) kc[id] = v
        for (id in intArrayOf(23, 39, 116, 121, 114)) cap[id] = 2f
        for (a in 0 until NID) for (b in 0 until NID) hk[a * NID + b] = 2f * kc[a] * kc[b] / (kc[a] + kc[b])
        hk[HBAT * NID] = 0.012f; hk[HBAT] = 0.012f      // тепловая батарея как «тепловая труба»: с воздухом почти не обменивается, поэтому тепло бежит вдоль неё и не теряется по бокам
    }

    private fun initRules() {
        val heat = intArrayOf(37, 24, 117)   // огонь, лава, расплав — источники жара
        val alkali = intArrayOf(I.Li, I.Na, I.K, I.Rb, I.Cs)
        val halogen = intArrayOf(I.F, I.Cl, I.Br, I.Io)

        // ---- ВЗРЫВЧАТКА ----
        for (e in intArrayOf(12, 13, 123)) for (hh in heat) {
            val pr = if (e == 13) 40 else 100                 // тротил не всегда с первого касания
            r(hh, e, -2, expl[e], pr); r(e, hh, -2, expl[e], pr)
        }
        for (hh in heat) { r(hh, 14, -1, 37, 3); r(14, hh, 37, -1, 3) } // C4 от огня только горит, нужен детонатор (ток)
        for (hh in heat) { r(hh, I.H, -2, 3, 100); r(I.H, hh, -2, 3, 100) }   // водород
        r(37, 3, -2, 2, 4); r(24, 3, -2, 2, 6)               // пары бензина хлопают
        r(37, 16, -2, 3, 40)                                  // фейерверк
        r(I.H, I.F, -2, 5, 50); r(I.F, I.H, -2, 5, 50); r(I.F, 22, -2, 5, 30)
        r(I.Cl, I.H, -2, 4, 8)

        // ---- ГОРЕНИЕ ----
        fun fuel(id: Int, p: Int, alt: Int = -1, pa: Int = 0) {
            r(37, id, 37, 37, p, alt, pa)
            for (hh in intArrayOf(24, 117)) r(hh, id, hh, 37, minOf(100, p * 2), alt, pa)
        }
        fuel(28, 3, 31, 12); fuel(29, 5, 119, 40); fuel(30, 5, 119, 40); fuel(31, 1, 119, 25); fuel(40, 3, 119, 30)
        fuel(2, 8, 38, 40); fuel(3, 25, 38, 20); fuel(4, 15); fuel(9, 20); fuel(5, 6, 38, 30); fuel(8, 2, 38, 50); fuel(7, 1, 119, 30)
        fuel(18, 2, 38, 70); fuel(19, 2, 38, 70); fuel(16, 10); fuel(I.C, 2, 119, 30); fuel(I.P, 30); fuel(I.S, 8, 38, 60); fuel(I.Mg, 14, 119, 50)
        for (m in alkali) fuel(m, 15, 119, 30)
        r(37, I.Io, 37, 38, 30)
        for (g in intArrayOf(I.He, I.Ne, I.Ar, I.Kr, I.Xe, I.N)) r(37, g, 0, -1, 6)       // инертные газы гасят огонь
        r(37, I.O, 37, 37, 15); r(I.P, I.O, 37, -1, 5); r(I.O, I.Fe, -1, 115, 1)

        // ---- ВОДА, ЛЁД, ПАР ----
        r(37, 22, 0, 27, 40); r(37, 26, 0, 22, 20); r(37, 25, 0, 22, 30)
        r(24, 22, 39, 27, 30); r(24, 35, 39, 27, 30); r(24, 25, 24, 27, 40); r(24, 26, 24, 22, 20)
        r(35, 37, 34, 0, 20)                                  // выпаривание соли
        r(10, 22, -1, 26, 40); r(10, 35, -1, 26, 30); r(10, 24, -1, 39, 50); r(10, 27, -1, 22, 50); r(10, 37, -1, 0, 60)
        r(10, 114, -1, 21, 15)                                // термоудар: стекло рассыпается в песок
        r(17, 22, 36, -1, 20); r(25, 22, 22, -1, 8)
        r(34, 26, -1, 22, 8); r(34, 25, -1, 22, 12); r(34, 22, 0, 35, 4)
        r(30, 22, 29, 0, 5); r(40, 22, 40, 40, 2)
        r(12, 22, 20, -1, 10); r(16, 22, -1, 5, 3); r(4, 22, 22, -1, 3)

        // ---- ЩЕЛОЧНЫЕ МЕТАЛЛЫ: вода = взрыв ----
        val arad = intArrayOf(3, 4, 5, 6, 8)
        for ((k, m) in alkali.withIndex()) {
            val rad = arad[k]
            for (wt in intArrayOf(22, 35)) r(m, wt, -2, rad, 40 + k * 10)
            r(m, 25, -2, rad - 1, 25); r(m, 26, -2, rad - 1, 8); r(m, 1, -2, rad, 50); r(m, 4, -1, I.H, 20)
            for (hl in halogen) { r(m, hl, 34, 34, 50); r(hl, m, 34, 34, 50) }
        }
        for (m in intArrayOf(I.Ca, I.Sr, I.Ba, I.Ra)) { r(m, 22, 23, I.H, 6); r(m, 35, 23, I.H, 3) }
        r(I.Mg, 22, -1, I.H, 2)

        // ---- КИСЛОТЫ И ЩЁЛОЧИ ----
        for ((m, p) in listOf(I.Mg to 15, I.Al to 12, I.Ca to 20, I.Cr to 3, I.Mn to 12, I.Fe to 12, I.Co to 8, I.Ni to 8, I.Cu to 5,
            I.Zn to 15, I.Ga to 10, I.Sr to 20, I.Cd to 8, I.Sn to 6, I.Ba to 20, I.Pb to 4, I.Bi to 3, 118 to 6, I.Ag to 2, I.Ti to 1))
            r(1, m, 0, I.H, p)                                // кислота + металл = водород
        for ((m, p) in listOf(20 to 10, 23 to 8, 28 to 10, 121 to 5, 116 to 4, 115 to 15, 29 to 15, 40 to 15, 30 to 15, 16 to 15, 7 to 20,
            18 to 3, 19 to 3)) r(1, m, 0, 0, p)
        r(6, 1, 22, 34, 50); r(1, 6, 34, 22, 50)              // нейтрализация -> вода + соль
        r(11, 1, I.Cl, 0, 30); r(1, 11, 0, I.Cl, 30)          // отбеливатель + кислота = хлор
        r(6, I.Al, 0, I.H, 12); r(6, I.Zn, 0, I.H, 8); r(6, I.Sn, 0, I.H, 4)
        r(6, I.Cl, 11, 0, 20); r(11, 5, -1, 22, 30)
        r(6, 29, -1, 0, 6); r(6, 28, -1, 0, 2); r(11, 7, -1, 0, 15)
        r(2, 1, 18, 0, 3)                                     // нефть + кислота = пластик

        // ---- ГАЛОГЕНЫ, ЯДЫ ----
        for (m in intArrayOf(28, 29, 30, 31, 40, 18, 19, 16, I.C, I.P, I.S)) r(I.F, m, 37, 37, 15)
        for (m in intArrayOf(I.Mg, I.Al, I.Fe, I.Cu, I.Zn, I.Sn, I.Ni, I.Co, I.Mn)) { r(I.F, m, 37, 37, 6); r(I.Cl, m, -1, 34, 5) }
        r(I.Br, I.Al, 37, 37, 10); r(I.Cl, 22, 0, 11, 6)
        r(I.H, I.Cl, 1, 1, 10); r(I.Cl, I.H, 1, 1, 10)
        r(I.H, I.O, 22, 22, 5); r(I.O, I.H, 22, 22, 5)
        r(I.H, 115, 22, I.Fe, 5)                              // водород восстанавливает ржавчину в железо
        for ((tx, p) in listOf(I.F to 30, I.Cl to 25, I.Br to 20, I.As to 10, I.Se to 6, I.Cd to 6, I.Pb to 3, I.Hg to 5, 122 to 20, I.Io to 3, 11 to 10, 34 to 2))
            for (pl in plants) r(tx, pl, -1, 0, p)
        for (pl in plants) r(7, pl, -1, 7, 3)                 // слизь заражает растения
        r(34, 7, -1, 0, 10)

        // ---- МЕТАЛЛЫ: плавка и литьё ----
        fun melt(id: Int, pf: Int, pl: Int) {
            if (pf > 0) r(37, id, 37, -3, pf)
            r(24, id, 24, -3, pl); r(117, id, 117, -3, maxOf(1, pl / 2))
        }
        melt(I.Ga, 10, 30); melt(I.Sn, 6, 20); melt(I.Bi, 6, 20); melt(I.Pb, 5, 20); melt(I.Cd, 4, 15); melt(I.Zn, 3, 12); melt(I.Al, 1, 8)
        melt(I.Ag, 0, 6); melt(I.Au, 0, 5); melt(I.Cu, 0, 5); melt(I.Fe, 0, 4); melt(I.Co, 0, 4); melt(I.Ni, 0, 4); melt(I.Mn, 0, 3)
        melt(I.Cr, 0, 3); melt(I.Ti, 0, 3); melt(I.Be, 0, 3); melt(I.Ge, 0, 5); melt(I.Si, 0, 2); melt(118, 0, 3); melt(I.Pt, 0, 1)
        r(37, 15, 37, -3, 60, 37, 30); r(24, 15, 24, -3, 80); r(117, 15, 117, -3, 80)       // термит -> расплавленная сталь
        r(117, 22, -4, 27, 40); r(117, 35, -4, 27, 40); r(117, 25, -4, 27, 50); r(117, 26, -4, 22, 40); r(117, 10, -4, -1, 60)
        r(117, 31, 118, 0, 8); r(117, I.C, 118, 0, 8)                                      // расплав + уголь = сталь
        r(I.Hg, I.Al, -1, 115, 15); r(I.Hg, I.Au, -1, I.Hg, 6); r(I.Hg, I.Ag, -1, I.Hg, 6); r(37, I.Hg, 37, 38, 4)
        r(82, 22, 115, -1, 1); r(82, 35, 115, -1, 3)                                       // ржавчина
        r(115, 37, 82, 37, 3); r(115, 24, 82, 24, 6); r(115, 117, 82, 117, 10)             // переплавка ржавчины
        r(31, I.S, 12, 12, 3)                                                              // уголь + сера = порох

        // ---- ПЕРЕРАБОТКА ----
        r(28, 24, 31, 24, 6)                                                               // дерево -> уголь
        r(21, 24, 114, 24, 6); r(21, 117, 114, 117, 6); r(32, 37, 116, 37, 5); r(32, 24, 116, 24, 8)
        r(23, 24, 24, 24, 1); r(24, 20, 24, 23, 4); r(24, 33, 24, 23, 4)
        r(2, 37, 3, 37, 3); r(2, 24, 3, 24, 4); r(18, 37, 2, 37, 2); r(2, I.S, 19, 19, 3)   // нефть <-> бензин, пластик, резина
        r(8, 21, 23, 0, 5); r(8, 33, 23, 23, 5)                                            // клей + песок/гравий = камень
        r(20, 22, 120, 0, 5); r(120, 37, 20, -1, 12); r(24, 120, 24, 20, 10); r(30, 120, 29, -1, 8)  // земля <-> грязь
        r(120, 33, 121, 121, 5)                                                            // грязь + гравий = бетон
        r(119, 22, 20, -1, 6)                                                              // пепел + вода = земля
        r(1, 122, 0, I.U, 4)                                                               // кислота + отходы = уран (переработка)
        r(122, 22, -1, 27, 1)
        r(37, 36, -1, 27, 10); r(10, 39, -1, 33, 5)                                        // огонь превращает облако в пар; термоудар по обсидиану
    }

    private fun setT(i: Int, v: Int) { t[i] = v; s[i] = 0; s2[i] = 0 }
    fun clear() { pushUndo(); t.fill(0); s.fill(0); s2.fill(0); d.fill(0); tp.fill(amb); bo.fill(0f); pend.clear() }

    // ---------- отмена / повтор ----------
    private class Snap(val t: ByteArray, val d: ByteArray, val s: ShortArray, val s2: IntArray, val tp: FloatArray)
    private val undoSt = java.util.ArrayDeque<Snap>(); private val redoSt = java.util.ArrayDeque<Snap>()
    val undoSize: Int get() = undoSt.size
    val redoSize: Int get() = redoSt.size
    private fun snap() = Snap(ByteArray(t.size) { t[it].toByte() }, ByteArray(d.size) { d[it].toByte() },
        ShortArray(s.size) { s[it].toShort() }, s2.copyOf(), tp.copyOf())
    private fun restore(n: Snap) {
        for (k in t.indices) { t[k] = n.t[k].toInt() and 255; d[k] = n.d[k].toInt() and 255; s[k] = n.s[k].toInt() }   // id провода > 127 — читаем байт без знака
        System.arraycopy(n.s2, 0, s2, 0, s2.size); System.arraycopy(n.tp, 0, tp, 0, tp.size)
        bo.fill(0f); pend.clear()
    }
    /** Запомнить состояние перед правкой (вызывать один раз на мазок / действие). */
    fun pushUndo() {
        undoSt.addLast(snap()); redoSt.clear()
        while (undoSt.size > 30) undoSt.removeFirst()
    }
    fun undo(): Boolean {
        if (undoSt.isEmpty()) return false
        redoSt.addLast(snap()); restore(undoSt.removeLast()); return true
    }
    fun redo(): Boolean {
        if (redoSt.isEmpty()) return false
        undoSt.addLast(snap()); restore(redoSt.removeLast()); return true
    }
    fun tempAt(x: Int, y: Int): Int = if (x in 0 until w && y in 0 until h) tp[y * w + x].toInt() else amb.toInt()

    /** Применить результат реакции к клетке. */
    private fun put(j: Int, code: Int) {
        when {
            code >= 0 -> setT(j, code)
            code == -3 -> { val old = t[j]; setT(j, 117); d[j] = old }
            code == -4 -> { val old = d[j]; setT(j, if (old in 59..110 || old == 118) old else 118); d[j] = 0 }
        }
    }

    // ---------- ввод ----------
    /**
     * Нажатие на рубильник (43) или кнопку (44). Переключается ВСЯ связная группа таких клеток разом: раньше
     * нарисованный кистью рубильник состоял из десятков клеток, и каждое касание переключало только одну —
     * поэтому «с первого раза» включалось не всегда.
     */
    fun tap(x: Int, y: Int): Boolean {
        if (x !in 0 until w || y !in 0 until h) return false
        val i = y * w + x; val ty = t[i]
        if (ty != 43 && ty != 44) return false
        pushUndo()
        val on = s[i] == 0                                      // рубильник: новое состояние для всей группы
        stamp++
        var qh = 0; var qt = 0
        bfs[qt++] = i; seen[i] = stamp
        while (qh < qt) {
            val c = bfs[qh++]
            if (ty == 43) { s[c] = if (on) 15 else 0 } else { s[c] = 15; s2[c] = 8 }
            val cx = c % w; val cy = c / w
            for (q in 0..3) {
                val nx = cx + DX[q]; val ny = cy + DY[q]
                if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue
                val j = ny * w + nx
                if (t[j] == ty && seen[j] != stamp) { seen[j] = stamp; bfs[qt++] = j }
            }
        }
        return true
    }
    /** Ближайший к (x, y) рубильник или кнопка в радиусе r клеток (0 — только сама клетка). Возвращает номер клетки или -1.
     *  Палец крупнее клетки, поэтому попадать точно в один пиксель не нужно. */
    fun findSwitch(x: Int, y: Int, r: Int): Int {
        var best = -1; var bd = Int.MAX_VALUE
        for (yy in y - r..y + r) for (xx in x - r..x + r) {
            if (xx < 0 || yy < 0 || xx >= w || yy >= h) continue
            val ty = t[yy * w + xx]; if (ty != 43 && ty != 44) continue
            val dd = (xx - x) * (xx - x) + (yy - y) * (yy - y)
            if (dd <= r * r + r && dd < bd) { bd = dd; best = yy * w + xx }
        }
        return best
    }
    /** Палец греет (delta > 0) или охлаждает (delta < 0) круг радиуса r; к краю действие слабеет. Работает и по воздуху, и по твёрдому. */
    private fun heatBrush(cx: Int, cy: Int, r: Int, delta: Float) {
        val rad = maxOf(r, 2)
        for (yy in cy - rad..cy + rad) for (xx in cx - rad..cx + rad) {
            if (xx < 0 || yy < 0 || xx >= w || yy >= h) continue
            val dd = (xx - cx) * (xx - cx) + (yy - cy) * (yy - cy)
            if (dd > rad * rad) continue
            val fall = 1f - 0.6f * Math.sqrt(dd.toDouble()).toFloat() / rad
            val i = yy * w + xx
            tp[i] = (tp[i] + delta * fall).coerceIn(-250f, 3000f)
        }
    }
    fun paint(cx: Int, cy: Int, r: Int, id: Int, dir: Int, src: Boolean = false, gain: Float = 1f) {
        if (isTool(id)) { heatBrush(cx, cy, r, (if (id == HEAT) fingerPower else -fingerPower) * gain); return }
        val k = kind[id]
        val asSrc = src && srcable(id)
        val wire = isWire(id)
        val rad = if (wire) 0 else if (asSrc) minOf(r, 2) else r       // провод всегда в одну клетку: иначе пересечения превращаются в кашу
        for (yy in cy - rad..cy + rad) for (xx in cx - rad..cx + rad) {
            if (xx < 0 || yy < 0 || xx >= w || yy >= h) continue
            if ((xx - cx) * (xx - cx) + (yy - cy) * (yy - cy) > rad * rad + rad / 2) continue
            val i = yy * w + xx
            if (id == 0) { setT(i, 0); d[i] = 0; tp[i] = amb; continue }
            if (wire) {
                val cur = t[i]
                if (isWire(cur)) {                                 // поверх провода: тот же цвет — ничего, другой — пересечение
                    if (cur == id || (isWire(d[i]) && d[i] == id)) continue
                    d[i] = id; continue                            // клетка держит два провода: основной в t, второй в d
                }
                if (cur != 0 && !isE(cur)) continue                // поверх воды/камня провод не кладём
                setT(i, id); d[i] = 0; tp[i] = amb; bo[i] = 0f
                continue
            }
            val replace = k == K.ELEC && isE(t[i])
            if (t[i] != 0 && !replace) continue
            if (replace && t[i] == id && d[i] == dir) continue     // уже стоит такой же — не сбрасываем сигнал
            if (rad > 0 && !asSrc && k != K.SOLID && k != K.ELEC && rnd.nextInt(3) != 0) continue
            if (asSrc) { setT(i, 113); d[i] = id } else { setT(i, id); d[i] = dir }
            tp[i] = amb; bo[i] = 0f
            if (id == 42) s[i] = 15
        }
    }

    // ---------- физика ----------
    private fun can(a: Int, x: Int, y: Int): Boolean {
        if (x < 0 || y < 0 || x >= w || y >= h) return false
        val j = y * w + x; if (mv[j]) return false
        val b = t[j]
        return b == 0 || ((kind[b] == K.LIQUID || kind[b] == K.GAS) && dens[b] < dens[a])
    }
    private fun canUp(a: Int, x: Int, y: Int): Boolean {           // газ поднимается; сквозь жидкость — пузырями
        if (x < 0 || y < 0 || x >= w || y >= h) return false
        val j = y * w + x; if (mv[j]) return false
        val b = t[j]
        return b == 0 || (a != 37 && kind[b] == K.LIQUID && rnd.nextInt(10) < 3)
    }
    private fun canSink(x: Int, y: Int): Boolean {                 // тяжёлый газ оседает сквозь лёгкие газы
        if (x < 0 || y < 0 || x >= w || y >= h) return false
        val j = y * w + x; if (mv[j]) return false
        val b = t[j]
        return b == 0 || (kind[b] == K.GAS && !Els.heavyGas[b])
    }
    private fun swap(i: Int, j: Int) {
        val q = t[i]; t[i] = t[j]; t[j] = q
        val tq = tp[i]; tp[i] = tp[j]; tp[j] = tq
        val bq = bo[i]; bo[i] = bo[j]; bo[j] = bq
        val e = d[i]; d[i] = d[j]; d[j] = e                        // d (память расплава) едет вместе с веществом
        mv[i] = true; mv[j] = true
    }

    fun step() {
        tick++; mv.fill(false)
        runPending()
        val gx = GX[gdir]; val gy = GY[gdir]; val px = gy; val py = gx       // g — «вниз», p — поперёк
        val vert = gx == 0
        val outerN = if (vert) h else w; val innerN = if (vert) w else h
        val desc = if (vert) gy > 0 else gx > 0                              // обходим от «пола» к «потолку»
        for (oo in 0 until outerN) {
            val o = if (desc) outerN - 1 - oo else oo
            val lr = rnd.nextBoolean()
            for (k in 0 until innerN) {
                val nn = if (lr) k else innerN - 1 - k
                val x = if (vert) nn else o; val y = if (vert) o else nn
                val i = y * w + x; val ty = t[i]
                if (ty == 0 || mv[i]) continue
                val side = if (rnd.nextBoolean()) 1 else -1
                val bx = x + gx; val by = y + gy                               // клетка «под» нами
                val sx = px * side; val sy = py * side
                when (kind[ty]) {
                    K.POWDER, K.LIQUID -> {
                        if (can(ty, bx, by)) swap(i, by * w + bx)
                        else if (can(ty, bx + sx, by + sy)) swap(i, (by + sy) * w + bx + sx)
                        else if (can(ty, bx - sx, by - sy)) swap(i, (by - sy) * w + bx - sx)
                        else if (kind[ty] == K.LIQUID) {               // растекание: скользим по поверхности на несколько клеток
                            var tx = -1; var tyy = -1; var kk = 1
                            while (kk <= disp[ty]) {
                                val nx = x + sx * kk; val ny = y + sy * kk
                                if (nx < 0 || ny < 0 || nx >= w || ny >= h || t[ny * w + nx] != 0 || mv[ny * w + nx]) break
                                tx = nx; tyy = ny; kk++
                            }
                            if (tx >= 0) swap(i, tyy * w + tx)
                        }
                    }
                    K.GAS -> if (Els.heavyGas[ty]) {
                        if (rnd.nextInt(10) < 5 && canSink(bx, by)) swap(i, by * w + bx)
                        else if (canSink(bx + sx, by + sy)) swap(i, (by + sy) * w + bx + sx)
                        else if (canSink(x + sx, y + sy)) swap(i, (y + sy) * w + x + sx)
                    } else {
                        val ux = x - gx; val uy = y - gy                       // «вверх» — против гравитации
                        if (rnd.nextInt(10) < 6 && canUp(ty, ux, uy)) swap(i, uy * w + ux)
                        else if (canUp(ty, ux + sx, uy + sy)) swap(i, (uy + sy) * w + ux + sx)
                        else { val nx = x + sx; val ny = y + sy
                            if (nx in 0 until w && ny in 0 until h && t[ny * w + nx] == 0 && !mv[ny * w + nx]) swap(i, ny * w + nx) }
                    }
                    else -> {}
                }
                react(x, y)
            }
        }
        if (tick % 2 == 0) pumps()
        if (tick % 3 == 0) elec()
        if (tick % 2 == 0) level()
        heat()
    }

    // ---------- насос ----------
    // Насос — звено трубы: у него есть направление (кнопка ↑). Он забирает жидкость/газ из соседних клеток (кроме той, куда смотрит),
    // держит внутри одну порцию и выталкивает вперёд: в пустую клетку, в следующий насос или (если там жидкость) вытесняя её в соседнюю
    // свободную клетку. Цепочка насосов = труба, по которой вода идёт вверх, вниз и вбок. Электричество не нужно.
    private val pm = BooleanArray(w * h)
    private fun moveCell(from: Int, to: Int) {
        t[to] = t[from]; d[to] = d[from]; tp[to] = tp[from]; bo[to] = 0f; s[to] = 0; s2[to] = 0
        t[from] = 0; d[from] = 0
    }
    private fun pumpable(b: Int) = b != 0 && b != 37 && (kind[b] == K.LIQUID || kind[b] == K.GAS)
    private fun pumps() {
        pm.fill(false)
        for (y in 0 until h) for (x in 0 until w) {
            val i = y * w + x
            if (t[i] != PUMP || pm[i]) continue
            val dd = d[i] and 3
            if (s[i] == 0) {                                               // пусто: засасываем сзади, потом сбоку
                val back = (dd + 2) % 4; val o = rnd.nextInt(2)
                val order = intArrayOf(back, (dd + 1 + o * 2) % 4, (dd + 3 - o * 2) % 4)
                for (q in order) {
                    val nx = x + DX[q]; val ny = y + DY[q]
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue
                    val j = ny * w + nx; val b = t[j]
                    if (!pumpable(b)) continue
                    val tc = tp[j].toInt().coerceIn(-399, 4999)
                    s[i] = b; s2[i] = ((tc + 400) shl 7) or (d[j] and 127); pm[i] = true
                    t[j] = 0; d[j] = 0; bo[j] = 0f; tp[j] = amb
                    break
                }
                continue
            }
            val fx = x + DX[dd]; val fy = y + DY[dd]
            if (fx < 0 || fy < 0 || fx >= w || fy >= h) continue
            val j = fy * w + fx; val b = t[j]; val cargo = s[i]
            if (b == PUMP) {                                               // передача в следующий насос
                if (s[j] == 0 && !pm[j] && (d[j] and 3) != (dd + 2) % 4) { s[j] = cargo; s2[j] = s2[i]; pm[j] = true; s[i] = 0; s2[i] = 0 }
                continue
            }
            if (b != 0) {                                                  // впереди жидкость/газ — проталкиваем её дальше по ходу насоса
                if (!pumpable(b)) continue
                // ищем свободное место: вдоль направления сквозь столб жидкости или сбоку от него (до 80 клеток)
                var m = 0; var cx = fx; var cy = fy; var sink = -1; var sideCell = -1
                while (m < 80 && cx in 0 until w && cy in 0 until h) {
                    val c = cy * w + cx; val tc = t[c]
                    if (tc == 0) { sink = m; break }
                    if (!pumpable(tc)) break
                    for (q in 0..3) {
                        if (q == dd || q == (dd + 2) % 4) continue
                        val nx = cx + DX[q]; val ny = cy + DY[q]
                        if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue
                        if (t[ny * w + nx] == 0) { sideCell = ny * w + nx; break }
                    }
                    if (sideCell >= 0) { sink = m; break }
                    cx += DX[dd]; cy += DY[dd]; m++
                }
                if (sink < 0) continue                                     // некуда — застой
                if (sideCell >= 0) moveCell((fy + DY[dd] * sink) * w + fx + DX[dd] * sink, sideCell)
                for (mm in sink downTo 1) moveCell((fy + DY[dd] * (mm - 1)) * w + fx + DX[dd] * (mm - 1), (fy + DY[dd] * mm) * w + fx + DX[dd] * mm)
            }
            val c2 = s2[i]
            t[j] = cargo; d[j] = c2 and 127; tp[j] = ((c2 shr 7) - 400).toFloat(); bo[j] = 0f; s[j] = 0; s2[j] = 0
            s[i] = 0; s2[i] = 0
        }
    }

    /** Теплопроводность: тепло течёт между соседями через любые вещества (в том числе сквозь сталь и камень) с учётом
     *  проводимости пары и теплоёмкости клетки. Воздух быстро выравнивается с температурой поля, у огня/лавы/азота температура закреплена. */
    private fun heat() {
        for (y in 0 until h) for (x in 0 until w) {
            val i = y * w + x; val c = tp[i]; val ty = t[i]; val hb = ty * NID
            var fl = 0f
            if (y > 0) fl += hk[hb + t[i - w]] * (tp[i - w] - c)
            if (y < h - 1) fl += hk[hb + t[i + w]] * (tp[i + w] - c)
            if (x > 0) fl += hk[hb + t[i - 1]] * (tp[i - 1] - c)
            if (x < w - 1) fl += hk[hb + t[i + 1]] * (tp[i + 1] - c)
            var v = c + fl / cap[ty]
            val k = kind[ty]
            v -= (v - amb) * (if (k == null || k == K.GAS) 0.07f else 0.0012f)
            when (ty) {
                37 -> if (v < fireTemp) v = fireTemp
                24 -> if (v < 1100f) v = 1100f
                117 -> if (v < 700f) v = 700f
                10 -> v = -190f
                17 -> if (v > -78f) v = -78f
            }
            tn[i] = v.coerceIn(-273f, 3000f)
        }
        System.arraycopy(tn, 0, tp, 0, tp.size)
    }

    private fun ignite(i: Int, a: Int) {
        t[i] = when {
            a == 28 && rnd.nextInt(100) < 12 -> 31
            (a == 29 || a == 30 || a == 40 || a == 31) && rnd.nextInt(100) < 35 -> 119
            kind[a] == K.LIQUID && rnd.nextInt(100) < 40 -> 38
            else -> 37
        }
        s[i] = 0; s2[i] = 0
    }

    /** Сообщающиеся сосуды: в каждом связном объёме жидкости самая высокая поверхность отдаёт клетки
     *  в самое низкое место, куда жидкость может дотечь (по трубкам, под перегородками), пока уровни не сравняются. */
    private fun level() {
        val gx = GX[gdir]; val gy = GY[gdir]
        stamp++
        for (start in 0 until w * h) {
            val v = t[start]
            if (v == 0 || kind[v] != K.LIQUID || seen[start] == stamp) continue
            var qh = 0; var qt = 0; var nt = 0; var nl = 0
            bfs[qt++] = start; seen[start] = stamp
            while (qh < qt) {
                val i = bfs[qh++]; val x = i % w; val y = i / w
                val ux = x - gx; val uy = y - gy; val dx = x + gx; val dy = y + gy
                if (ux in 0 until w && uy in 0 until h && dx in 0 until w && dy in 0 until h &&
                    t[uy * w + ux] == 0 && t[dy * w + dx] != 0 && nt < topL.size) topL[nt++] = i          // поверхность, лежащая на опоре
                for (dq in 0..3) {
                    val nx = x + DX[dq]; val ny = y + DY[dq]
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue
                    val j = ny * w + nx; val tj = t[j]
                    if (tj == v) { if (seen[j] != stamp) { seen[j] = stamp; bfs[qt++] = j } }
                    else if (tj == 0 && nx + gx in 0 until w && ny + gy in 0 until h && t[(ny + gy) * w + nx + gx] != 0 && nl < lowL.size) lowL[nl++] = j  // куда можно дотечь
                }
            }
            if (qt < 4 || nt == 0 || nl == 0) continue
            for (rep in 0 until 3) {
                var bt = -1; var bty = Int.MAX_VALUE; var bl = -1; var bly = Int.MIN_VALUE
                for (k in 0 until nt) { val c = topL[k]; if (c < 0 || t[c] != v) continue; val cy = (c % w) * gx + (c / w) * gy; if (cy < bty) { bty = cy; bt = k } }
                for (k in 0 until nl) { val c = lowL[k]; if (c < 0 || t[c] != 0) continue; val cy = (c % w) * gx + (c / w) * gy; if (cy > bly) { bly = cy; bl = k } }
                if (bt < 0 || bl < 0 || bly <= bty + 1) break
                val from = topL[bt]; val to = lowL[bl]
                t[to] = v; d[to] = d[from]; tp[to] = tp[from]; s[to] = 0; s2[to] = 0
                t[from] = 0; d[from] = 0; topL[bt] = -1; lowL[bl] = -1
            }
        }
    }

    private fun runPending() {
        if (pend.isEmpty()) return
        val go = ArrayList<IntArray>()
        val it = pend.iterator()
        while (it.hasNext()) { val e = it.next(); e[3]--; if (e[3] <= 0) { go.add(e); it.remove() } }
        for (e in go) boom(e[0], e[1], e[2])
    }

    private fun powered(x: Int, y: Int): Boolean {
        for (q in 0..3) { val nx = x + DX[q]; val ny = y + DY[q]
            if (nx in 0 until w && ny in 0 until h) { val j = ny * w + nx
                if (isE(t[j]) && (s[j] > 0 || (isWire(t[j]) && s2[j] > 0))) return true } }
        return false
    }

    /** Взрыв: центр уничтожает всё, к краю выживает тем больше, чем прочнее материал. Соседняя взрывчатка детонирует цепочкой. */
    private fun boom(cx: Int, cy: Int, r: Int) {
        val r2 = r * r
        for (yy in cy - r..cy + r) for (xx in cx - r..cx + r) {
            if (xx < 0 || yy < 0 || xx >= w || yy >= h) continue
            val dd = (xx - cx) * (xx - cx) + (yy - cy) * (yy - cy)
            if (dd > r2) continue
            val j = yy * w + xx; val tt = t[j]
            val q = Math.sqrt(dd.toDouble()) / r
            tp[j] = maxOf(tp[j], 150f + ((1.0 - q) * 1100).toFloat())
            if (tt == 0) { if (rnd.nextInt(100) < 55) setT(j, if (rnd.nextInt(10) < 6) 37 else 38); continue }
            if (expl[tt] > 0 && dd > 0) { pend.add(intArrayOf(xx, yy, expl[tt], 1 + (q * 4).toInt())); setT(j, 37); continue }
            if (hard[tt] > 0 && rnd.nextInt(100) < (hard[tt] * 9 * q).toInt()) continue
            val r1 = rnd.nextInt(100); val k = kind[tt]
            setT(j, when {
                k == K.LIQUID && tt != 24 && tt != 117 && r1 < 55 -> 27
                q > .55 && k == K.SOLID && r1 < 12 -> 33
                r1 < 45 -> 37
                r1 < 65 -> 38
                else -> 0
            })
        }
    }

    /** Ядерный реактор: плотная масса урана/плутония нагревается сама (остаточное тепло растёт с числом соседей),
     *  деление дополнительно греет клетку. Вода рядом кипит от тепла -> пар для двигателя. Стержни (управления, бор, кадмий)
     *  гасят деление. Если температура топлива уходит за 700° — расплавление и взрыв, тем сильнее, чем больше масса. */
    private fun reactor(x: Int, y: Int, a: Int) {
        var n = 0; var wat = 0; var ab = 0
        for (yy in y - 2..y + 2) for (xx in x - 2..x + 2) {
            if (xx < 0 || yy < 0 || xx >= w || yy >= h || (xx == x && yy == y)) continue
            when (t[yy * w + xx]) { 107, 109 -> n++; 22, 35 -> wat++; 112, 63, 96 -> ab++ }
        }
        val i = y * w + x
        if (n >= 3 && s[i] < 6000) s[i]++                                              // масса «созревает»: чем дольше лежит, тем горячее
        if (rnd.nextInt(100) < n * n / 3 + s[i] / 70) tp[i] += if (a == 109) selfHeat * 1.4f else selfHeat      // самонагрев массы
        if (wat > 0 && tp[i] > amb) tp[i] -= (tp[i] - amb) * 0.003f * minOf(wat, 8)     // вода рядом охлаждает топливо
        var p = (if (a == 109) 3 else 2) * n
        if (wat > 0) p += p / 2
        p -= ab * 5
        if (p > 0 && rnd.nextInt(100) < p) {
            tp[i] += fisHeat                                                                // деление
            if (rnd.nextInt(2000) < 1) { setT(i, 122); return }                         // топливо выгорает -> отходы
        }
        if (tp[i] > meltAt && n >= 3) { boom(x, y, 6 + n / 2 + (if (n >= 14) 6 else 0)); setT(i, 24); tp[i] = 1100f }
    }

    private fun react(x: Int, y: Int) {
        val i = y * w + x; val a = t[i]; if (a == 0) return
        when (a) {
            37 -> if (rnd.nextInt(100) < 6) { t[i] = if (rnd.nextInt(10) < 4) 38 else 0; return }
            38 -> if (rnd.nextInt(100) < 3) { t[i] = 0; return }
            27 -> if (amb < 100f && rnd.nextInt(1000) < 4) { t[i] = 22; tp[i] = 60f; return }
            36 -> if (rnd.nextInt(3000) < 2) { t[i] = 22; return }              // дождь
            10 -> if (rnd.nextInt(1000) < 3) { t[i] = 65; return }              // азот испаряется
            17 -> if (rnd.nextInt(1000) < 3) { t[i] = 36; return }
            117 -> if (rnd.nextInt(1000) < 6) { put(i, -4); return }            // расплав остывает
            108 -> if (rnd.nextInt(10000) < 3) { setT(i, 105); return }         // радий распадается
            22, 35 -> {
                if (tp[i] >= 100f) {                                                     // кипение: нужно накопить энергию
                    bo[i] += 1f + (tp[i] - 100f) * cap[a] * 0.3f; tp[i] = 100f
                    if (bo[i] >= 900f) { t[i] = 27; bo[i] = 0f; return }
                } else if (bo[i] > 0f) bo[i] = maxOf(0f, bo[i] - 2f)
                if (a == 22 && tp[i] <= 0.5f) { t[i] = 26; return }                      // замерзание
                if (rnd.nextInt(100) < 2 && powered(x, y)) { t[i] = if (rnd.nextBoolean()) 59 else 66; return } // электролиз
            }
            25, 26 -> if (tp[i] >= 20f) {                                                // таяние тоже не мгновенное
                bo[i] += 1f + (tp[i] - 20f) * cap[a] * 0.3f; tp[i] = 20f
                if (bo[i] >= 300f) { t[i] = 22; tp[i] = amb; bo[i] = 0f; return }
            } else if (bo[i] > 0f) bo[i] = maxOf(0f, bo[i] - 2f)
            122 -> if (rnd.nextInt(100) < 3) tp[i] += 3f                                // распадное тепло
        }
        val ig = ignT[a]
        if (ig > 0 && tp[i] >= ig) { if (expl[a] > 0) boom(x, y, expl[a]) else ignite(i, a); return }
        if (expl[a] > 0 && powered(x, y)) { boom(x, y, expl[a]); return }
        if ((a == 59 || a == 3 || a == 4) && rnd.nextInt(100) < 8 && powered(x, y)) {     // искра поджигает газ/бензин/спирт
            if (a == 59) boom(x, y, 3) else t[i] = 37; return
        }
        if (a == 107 || a == 109) { reactor(x, y, a); return }
        val o = rnd.nextInt(4)
        for (m in 0..3) {
            val q = (o + m) % 4; val nx = x + DX[q]; val ny = y + DY[q]
            if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue
            val j = ny * w + nx; val b = t[j]
            if (a == 113) { if (b == 0) { t[j] = d[i]; s[j] = 0; s2[j] = 0; d[j] = 0; return }; continue }
            if (b == 0) continue
            if (a == 111) { if (b == 38 || b == 27) { t[j] = 0; s2[i] = 8; return }; continue }   // двигатель: дым/пар -> ток
            val lst = rules[a * NID + b] ?: continue
            for (rule in lst) {
                val pr = if (a == 37 && rule[0] == 37) (rule[2] * fireTemp / 600f).toInt().coerceIn(0, 100) else rule[2]   // горячий огонь поджигает чаще
                if (rnd.nextInt(100) >= pr) continue
                if (rule[0] == -2) { boom(if (expl[b] > 0) nx else x, if (expl[b] > 0) ny else y, rule[1]); return }
                val rb = if (rule[4] > 0 && rnd.nextInt(100) < rule[4]) rule[3] else rule[1]
                put(i, rule[0]); put(j, rb)
                return
            }
        }
    }

    // ---------- электрика (сигнал 0..15, как в редстоуне; тик = 3 кадра) ----------
    /**
     * Что «видит» соседний элемент в клетке (nx, ny), глядя на неё с направления od.
     * Провод хранит два канала: s — горизонтальный (лево/право), s2 — вертикальный (верх/низ).
     * У обычного провода они равны, у пересечения разных цветов — независимы.
     */
    private fun em(nx: Int, ny: Int, od: Int): Int {
        if (nx < 0 || ny < 0 || nx >= w || ny >= h) return 0
        val i = ny * w + nx; val ty = t[i]; val dd = d[i]
        if (isWire(ty)) return if (od % 2 == 1) s[i] else s2[i]
        return when (ty) {
            42, 43, 44, 45, 46, 47, 111, LED_G, LED_B -> s[i]
            48, 49, 50, 51, 52, 53, 54, 55, 57 -> if (od == dd) s[i] else 0
            56 -> if (od == dd || od == (dd + 2) % 4) s[i] else 0
            58 -> if (od % 2 == 1) s[i] else s2[i]
            else -> 0
        }
    }
    private fun inp(x: Int, y: Int, dir: Int) = em(x + DX[dir], y + DY[dir], (dir + 2) % 4)

    // Узел сети = (клетка, канал). У обычной клетки один узел (номер клетки*2), у пересечения два: *2 — горизонталь, *2+1 — вертикаль.
    private val pw = BooleanArray(w * h * 2); private val eq = IntArray(w * h * 2)
    private val chH = IntArray(w * h); private val chV = IntArray(w * h)      // цвет канала: 0..15 провод, -1 любой (светодиод/пиксель), -2 не проводник
    /** Для рисования: цвета горизонтального и вертикального провода в клетке-пересечении (-1 — клетка не пересечение). */
    val crossH = IntArray(w * h) { -1 }; val crossV = IntArray(w * h) { -1 }
    private fun cond(ty: Int) = isWire(ty) || ty == 46 || ty == 47 || ty == LED_G || ty == LED_B
    /** Второй провод в клетке (пересечение) или -1. */
    private fun secOf(i: Int): Int = if (isWire(t[i]) && isWire(d[i]) && d[i] != t[i]) wireIdx(d[i]) else -1
    /** Есть ли в клетке (x, y) проводник цвета c (светодиоды и пиксели подходят любому цвету). */
    private fun hasCol(x: Int, y: Int, c: Int): Boolean {
        if (x < 0 || y < 0 || x >= w || y >= h) return false
        val j = y * w + x; val ty = t[j]
        if (!cond(ty)) return false
        if (!isWire(ty)) return true
        return wireIdx(ty) == c || secOf(j) == c
    }

    /**
     * Проводящая сеть. Правила:
     *  - соседние клетки соединяются, только если цвет провода одинаковый (светодиод/пиксель соединяются с любым);
     *  - одинаковый цвет, провода пересеклись — это обычный узел, сигнал идёт во все 4 стороны;
     *  - разные цвета в одной клетке — «мост»: один провод идёт по горизонтали, другой по вертикали, друг на друга не влияют.
     * Запитан каждый узел, до которого можно дойти от источника (батарея, рубильник, выход логики…), стоящего рядом с проводом.
     */
    private fun wireNet() {
        pw.fill(false); var qt = 0
        for (i in 0 until w * h) {                                // 1) цвета каналов
            val ty = t[i]
            if (!cond(ty)) { chH[i] = -2; chV[i] = -2; crossH[i] = -1; crossV[i] = -1; continue }
            if (!isWire(ty)) { chH[i] = -1; chV[i] = -1; crossH[i] = -1; crossV[i] = -1; continue }
            val a = wireIdx(ty); val b = secOf(i)
            if (b < 0) { chH[i] = a; chV[i] = a; crossH[i] = -1; crossV[i] = -1; continue }
            val x = i % w; val y = i / w                          // пересечение: кто идёт по горизонтали — смотрим по соседям
            fun n(v: Boolean) = if (v) 1 else 0
            val sAB = n(hasCol(x - 1, y, a)) + n(hasCol(x + 1, y, a)) + n(hasCol(x, y - 1, b)) + n(hasCol(x, y + 1, b))
            val sBA = n(hasCol(x, y - 1, a)) + n(hasCol(x, y + 1, a)) + n(hasCol(x - 1, y, b)) + n(hasCol(x + 1, y, b))
            val hc = if (sBA > sAB) b else a; val vc = if (sBA > sAB) a else b
            chH[i] = hc; chV[i] = vc; crossH[i] = hc; crossV[i] = vc
        }
        for (i in 0 until w * h) {                                // 2) источники рядом с проводом
            if (chH[i] == -2) continue
            val x = i % w; val y = i / w; val cross = crossH[i] >= 0
            for (q in 0..3) {
                val nx = x + DX[q]; val ny = y + DY[q]
                if (nx < 0 || ny < 0 || nx >= w || ny >= h || cond(t[ny * w + nx])) continue
                if (em(nx, ny, (q + 2) % 4) <= 0) continue
                val node = if (cross && q % 2 == 0) i * 2 + 1 else i * 2      // сверху/снизу питают вертикальный провод, слева/справа — горизонтальный
                if (!pw[node]) { pw[node] = true; eq[qt++] = node }
            }
        }
        var qh = 0                                                // 3) растекаемся по соседям с тем же цветом
        while (qh < qt) {
            val nd = eq[qh++]; val i = nd shr 1; val ch = nd and 1
            val x = i % w; val y = i / w; val cross = crossH[i] >= 0
            val my = if (ch == 0) chH[i] else chV[i]
            for (q in 0..3) {
                val horiz = q % 2 == 1
                if (cross && horiz != (ch == 0)) continue         // узел пересечения ходит только вдоль своей оси
                val nx = x + DX[q]; val ny = y + DY[q]
                if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue
                val j = ny * w + nx
                if (chH[j] == -2) continue
                val jn = if (crossH[j] >= 0 && !horiz) j * 2 + 1 else j * 2
                if (pw[jn]) continue
                val jc = if (horiz) chH[j] else chV[j]
                if (my >= 0 && jc >= 0 && my != jc) continue      // разные цвета не соединяются
                pw[jn] = true; eq[qt++] = jn
            }
        }
    }

    private fun elec() {
        wireNet()
        for (y in 0 until h) for (x in 0 until w) {
            val i = y * w + x; val ty = t[i]
            if (ty == PUMP) { ns[i] = s[i]; ns2[i] = s2[i]; continue }     // у насоса в s/s2 лежит то, что он везёт
            if (!isE(ty)) { ns[i] = 0; ns2[i] = 0; continue }
            if (cond(ty)) {                                                // провод, светодиоды, пиксель: без потерь и на любое расстояние
                val a = if (pw[i * 2]) 15 else 0
                ns[i] = a; ns2[i] = if (crossH[i] >= 0) (if (pw[i * 2 + 1]) 15 else 0) else a
                continue
            }
            val dd = d[i]; val b = (dd + 2) % 4; val l = (dd + 3) % 4; val r = (dd + 1) % 4
            var v = s[i]; var v2 = s2[i]
            val iL = inp(x, y, l) > 0; val iR = inp(x, y, r) > 0; val iB = inp(x, y, b) > 0
            when (ty) {
                42 -> v = 15
                111 -> { v = if (v2 > 0) 15 else 0; if (v2 > 0) v2-- }
                44 -> if (v2 > 0) v2-- else v = 0
                45 -> { v2 = (v2 + 1) % 8; v = if (v2 < 4) 15 else 0 }
                48 -> v = if (iB) 0 else 15
                49 -> v = if (iL && iR) 15 else 0
                50 -> v = if (iL || iR) 15 else 0
                51 -> v = if (iL != iR) 15 else 0
                52 -> v = if (iL && iR) 0 else 15
                53 -> v = if (iL || iR) 0 else 15
                54 -> if (iR) v = if (iL) 15 else 0
                55 -> v = inp(x, y, b)
                56 -> v = maxOf(inp(x, y, b), inp(x, y, dd))
                57 -> { v = v2; v2 = if (iB) 15 else 0 }
                58 -> { v = maxOf(inp(x, y, 1), inp(x, y, 3)); v2 = maxOf(inp(x, y, 0), inp(x, y, 2)) }
            }
            ns[i] = v; ns2[i] = v2
        }
        System.arraycopy(ns, 0, s, 0, s.size); System.arraycopy(ns2, 0, s2, 0, s2.size)
    }

    // ---------- сохранение ----------
    private val MAGIC = 0x53425832
    fun save(f: java.io.File) {
        val tmp = java.io.File(f.path + ".tmp")
        java.io.DataOutputStream(java.util.zip.GZIPOutputStream(tmp.outputStream().buffered())).use { o ->
            o.writeInt(MAGIC); o.writeInt(w); o.writeInt(h)
            for (arr in arrayOf(t, s, s2, d)) for (v in arr) o.writeInt(v)
        }
        if (!tmp.renameTo(f)) { f.delete(); tmp.renameTo(f) }
    }
    fun load(f: java.io.File, undoable: Boolean = true): Boolean = try {
        val nt = IntArray(w * h); val n1 = IntArray(w * h); val n2 = IntArray(w * h); val n3 = IntArray(w * h)
        java.io.DataInputStream(java.util.zip.GZIPInputStream(f.inputStream().buffered())).use { i ->
            var first = i.readInt()
            if (first == MAGIC) first = i.readInt()           // старые сохранения начинались сразу с ширины
            if (first != w || i.readInt() != h) return false
            for (arr in arrayOf(nt, n1, n2, n3)) for (k in arr.indices) arr[k] = i.readInt()
        }
        for (k in nt.indices) if (nt[k] !in 0 until NID || (nt[k] != 0 && Els.list[nt[k]] == null)) nt[k] = 0
        if (undoable) pushUndo()
        System.arraycopy(nt, 0, t, 0, t.size); System.arraycopy(n1, 0, s, 0, s.size)
        System.arraycopy(n2, 0, s2, 0, s2.size); System.arraycopy(n3, 0, d, 0, d.size); tp.fill(amb); pend.clear(); true
    } catch (e: Exception) { false }

    // ---------- рендер ----------
    private fun mix(a: Int, b: Int, f: Float): Int {
        var o = 0xFF shl 24
        for (sh in intArrayOf(16, 8, 0)) { val c1 = (a shr sh) and 255; val c2 = (b shr sh) and 255; o = o or ((c1 + (c2 - c1) * f).toInt() shl sh) }
        return o
    }
    private val WHITE = 0xFFFFFFFF.toInt()
    private val WON = IntArray(WIRE_COLORS) { mix(0xFF000000.toInt() or WIRE_HEX[it], WHITE, .15f) }          // провод под током — ярче
    private val WOFF = IntArray(WIRE_COLORS) { mix(0xFF000000.toInt() or WIRE_HEX[it], 0xFF0E0E0E.toInt(), .68f) } // без тока — тёмный
    /** Цвет провода номер idx (0..15) в включённом или выключенном состоянии. */
    fun wireColor(idx: Int, on: Boolean): Int = if (on) WON[idx] else WOFF[idx]
    private fun wirePixel(i: Int): Int {
        val a = wireIdx(t[i])
        if (crossH[i] >= 0) return mix(wireColor(crossH[i], s[i] > 0), wireColor(crossV[i], s2[i] > 0), .5f)   // пересечение: поверх рисуются два штриха
        val b = secOf(i)
        if (b >= 0) return mix(wireColor(a, false), wireColor(b, false), .5f)
        return wireColor(a, s[i] > 0)
    }
    private val hs = floatArrayOf(-200f, -60f, -5f, 15f, 70f, 130f, 250f, 450f, 700f, 1000f)
    private val hc = intArrayOf(0xFF1A237E.toInt(), 0xFF1E88E5.toInt(), 0xFF00838F.toInt(), 0xFF140F1F.toInt(), 0xFF6A1B9A.toInt(),
        0xFFC2185B.toInt(), 0xFFE53935.toInt(), 0xFFFF9800.toInt(), 0xFFFFEB3B.toInt(), 0xFFFFFFFF.toInt())
    private fun heatColor(v: Float): Int {
        if (v <= hs[0]) return hc[0]
        for (k in 1 until hs.size) if (v <= hs[k]) return mix(hc[k - 1], hc[k], (v - hs[k - 1]) / (hs[k] - hs[k - 1]))
        return hc[hc.size - 1]
    }
    fun render(thermal: Boolean = false) {
        if (thermal) {      // тепловизор: цвет по температуре, форма предметов слегка просвечивает
            for (i in px.indices) {
                val hcol = heatColor(tp[i]); val ty = t[i]
                px[i] = if (ty == 0) hcol else mix(hcol, Els.list[ty]?.color ?: hcol, .16f)
            }
            return
        }
        for (i in px.indices) {
            val ty = t[i]
            px[i] = when {
                ty == 0 -> 0xFF121212.toInt()
                ty == 37 -> if (rnd.nextBoolean()) 0xFFFF9800.toInt() else 0xFFFFEB3B.toInt()
                ty == 24 -> if (rnd.nextInt(10) == 0) 0xFFFFAB40.toInt() else Els.list[24]!!.color
                ty == 117 -> mix(Els.list[d[i].coerceIn(1, 127)]?.color ?: 0xFFB0BEC5.toInt(), 0xFFFF8F00.toInt(), .7f)
                ty == 107 || ty == 108 || ty == 109 || ty == 122 -> if (rnd.nextInt(70) == 0) WHITE else Els.list[ty]!!.color
                isWire(ty) -> wirePixel(i)
                ty == 46 -> mix(0xFF4A1515.toInt(), 0xFFFF1744.toInt(), s[i] / 15f)          // красный светодиод
                ty == LED_G -> mix(0xFF123D1E.toInt(), 0xFF00E676.toInt(), s[i] / 15f)       // зелёный
                ty == LED_B -> mix(0xFF132A55.toInt(), 0xFF448AFF.toInt(), s[i] / 15f)       // синий
                ty == 47 -> mix(0xFF212121.toInt(), WHITE, if (s[i] > 0) 1f else 0f)
                ty == 43 || ty == 44 -> mix(Els.list[ty]!!.color, WHITE, if (s[i] > 0) .6f else 0f)
                ty == 113 -> { val c = Els.list[d[i].coerceIn(1, 127)]?.color ?: WHITE
                    if (((i % w) + (i / w)) and 1 == 0) c else mix(c, WHITE, .55f) }      // источник — «шахматка»
                ty == 111 -> mix(Els.list[111]!!.color, WHITE, if (s[i] > 0) .7f else 0f)
                ty == PUMP -> if (s[i] != 0) mix(Els.list[PUMP]!!.color, Els.list[s[i].coerceIn(1, 127)]?.color ?: WHITE, .7f) else Els.list[PUMP]!!.color
                ty in 48..58 -> mix(Els.list[ty]!!.color, WHITE, if (s[i] > 0 || s2[i] > 0) .45f else 0f)
                else -> Els.list[ty]?.color ?: 0xFFFF00FF.toInt()
            }
        }
    }
}
