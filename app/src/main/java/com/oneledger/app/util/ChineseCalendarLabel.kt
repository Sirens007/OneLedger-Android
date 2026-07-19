package com.oneledger.app.util

import com.nlf.calendar.Solar
import java.util.Calendar

data class ChineseCalendarLabel(
    val text: String,
    val isFestival: Boolean,
)

fun Long.chineseCalendarLabel(): ChineseCalendarLabel {
    val calendar = Calendar.getInstance().apply { timeInMillis = this@chineseCalendarLabel }
    return chineseCalendarLabel(
        year = calendar.get(Calendar.YEAR),
        month = calendar.get(Calendar.MONTH) + 1,
        day = calendar.get(Calendar.DAY_OF_MONTH),
    )
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
