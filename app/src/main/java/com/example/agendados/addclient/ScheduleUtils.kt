package com.example.agendados.addclient

import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.MonthDay
import java.time.ZoneId

interface HolidayCalendar {
    fun isHoliday(date: LocalDate): Boolean
}

object PeruHolidayCalendar : HolidayCalendar {
    private val fixedHolidays = setOf(
        MonthDay.of(1, 1),
        MonthDay.of(4, 18),
        MonthDay.of(4, 19),
        MonthDay.of(5, 1),
        MonthDay.of(6, 29),
        MonthDay.of(7, 28),
        MonthDay.of(7, 29),
        MonthDay.of(8, 30),
        MonthDay.of(10, 8),
        MonthDay.of(11, 1),
        MonthDay.of(12, 8),
        MonthDay.of(12, 25)
    )

    private val dynamicHolidays = mutableSetOf<LocalDate>()

    override fun isHoliday(date: LocalDate): Boolean {
        return MonthDay.from(date) in fixedHolidays || date in dynamicHolidays
    }

    fun setDynamicHolidays(vararg dates: LocalDate) {
        dynamicHolidays.clear()
        dynamicHolidays.addAll(dates.toSet())
    }

    fun addHoliday(date: LocalDate) {
        dynamicHolidays.add(date)
    }
}

fun generateDateOptions(clock: Clock, daysAhead: Long = 30): List<LocalDate> {
    val today = LocalDate.now(clock)
    return (0L..daysAhead).map { today.plusDays(it) }
}

fun computeDefaultScheduleDate(clock: Clock, calendar: HolidayCalendar = PeruHolidayCalendar): LocalDate {
    val today = LocalDate.now(clock)
    return nextBusinessDay(today, calendar)
}

fun nextBusinessDay(date: LocalDate, calendar: HolidayCalendar = PeruHolidayCalendar): LocalDate {
    var candidate = date.plusDays(1)
    while (!isBusinessDay(candidate, calendar)) {
        candidate = candidate.plusDays(1)
    }
    return candidate
}

fun isBusinessDay(date: LocalDate, calendar: HolidayCalendar = PeruHolidayCalendar): Boolean {
    return date.dayOfWeek != DayOfWeek.SUNDAY && !calendar.isHoliday(date)
}

val LimaZone: ZoneId = ZoneId.of("America/Lima")
