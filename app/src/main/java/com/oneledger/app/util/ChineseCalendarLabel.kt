package com.oneledger.app.util

import com.nlf.calendar.Solar
import java.util.Calendar
import java.util.Collections
import java.util.LinkedHashMap

data class ChineseCalendarLabel(
    val text: String,
    val isFestival: Boolean,
)

private const val CalendarLabelCacheSize = 504

private val calendarLabelCache = Collections.synchronizedMap(
    object : LinkedHashMap<Long, ChineseCalendarLabel>(CalendarLabelCacheSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, ChineseCalendarLabel>?): Boolean =
            size > CalendarLabelCacheSize
    },
)

fun Long.chineseCalendarLabel(): ChineseCalendarLabel {
    calendarLabelCache[this]?.let { return it }
    val calendar = Calendar.getInstance().apply { timeInMillis = this@chineseCalendarLabel }
    val label = chineseCalendarLabel(
        year = calendar.get(Calendar.YEAR),
        month = calendar.get(Calendar.MONTH) + 1,
        day = calendar.get(Calendar.DAY_OF_MONTH),
    )
    calendarLabelCache[this] = label
    return label
}

fun chineseCalendarLabel(year: Int, month: Int, day: Int): ChineseCalendarLabel {
    val solar = Solar.fromYmd(year, month, day)
    val lunar = solar.lunar
    val festival = solar.festivals.firstOrNull()
        ?: lunar.festivals.firstOrNull()
        ?: lunar.jieQi.takeIf { it == "清明" }

    if (!festival.isNullOrBlank()) {
        return ChineseCalendarLabel(text = festival, isFestival = true)
    }

    val lunarDay = if (lunar.day == 1) "${lunar.monthInChinese}月" else lunar.dayInChinese
    return ChineseCalendarLabel(text = lunarDay, isFestival = false)
}
