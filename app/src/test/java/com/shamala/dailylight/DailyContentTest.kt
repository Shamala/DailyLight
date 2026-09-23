package com.shamala.dailylight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * [DailyContent.build] is pure, and everything the card says depends on it.
 * These are the guarantees the design rests on: the words hold still all day,
 * they turn over at midnight, and nothing indexes out of bounds.
 */
class DailyContentTest {

    private fun at(y: Int, m: Int, d: Int, h: Int = 9) = LocalDateTime.of(y, m, d, h, 0)

    @Test
    fun `the same day always gives the same words`() {
        val morning = DailyContent.build(at(2026, 9, 23, 7), Voice.MORNING)
        val noon = DailyContent.build(at(2026, 9, 23, 14), Voice.MORNING)
        assertEquals(morning.affirmation, noon.affirmation)
        assertEquals(morning.thought, noon.thought)
    }

    @Test
    fun `the next day gives different words`() {
        val today = DailyContent.build(at(2026, 9, 23), Voice.MORNING)
        val tomorrow = DailyContent.build(at(2026, 9, 24), Voice.MORNING)
        assertNotEquals(today.affirmation, tomorrow.affirmation)
    }

    @Test
    fun `morning and evening never land on the same line`() {
        val morning = DailyContent.build(at(2026, 9, 23), Voice.MORNING)
        val evening = DailyContent.build(at(2026, 9, 23, 21), Voice.EVENING)
        assertNotEquals(morning.affirmation, evening.affirmation)
    }

    @Test
    fun `every day of a leap year renders without going out of bounds`() {
        var day = at(2028, 1, 1)
        repeat(366) {
            listOf(Voice.MORNING, Voice.EVENING).forEach { voice ->
                YearLineStyle.entries.forEach { style ->
                    val card = DailyContent.build(day, voice, style, offset = it)
                    assertTrue(card.affirmation.isNotEmpty())
                    assertTrue(card.thought.isNotEmpty())
                    assertTrue(card.yearProgress in 0..1000)
                }
            }
            day = day.plusDays(1)
        }
    }

    @Test
    fun `the year bar is full on the last day and nearly empty on the first`() {
        assertEquals(1000, DailyContent.build(at(2026, 12, 31), Voice.MORNING).yearProgress)
        assertTrue(DailyContent.build(at(2026, 1, 1), Voice.MORNING).yearProgress < 5)
    }

    @Test
    fun `leap year is counted as 366 days`() {
        assertEquals(366, DailyContent.build(at(2028, 3, 1), Voice.MORNING).daysInYear)
        assertEquals(365, DailyContent.build(at(2026, 3, 1), Voice.MORNING).daysInYear)
    }

    @Test
    fun `bar only writes no year line`() {
        val card = DailyContent.build(at(2026, 9, 23), Voice.MORNING, YearLineStyle.BAR_ONLY)
        assertEquals("", card.yearLine)
    }

    @Test
    fun `countdown reads naturally on the last two days`() {
        assertEquals(
            "the last day of 2026",
            DailyContent.build(at(2026, 12, 31), Voice.MORNING, YearLineStyle.COUNTDOWN).yearLine
        )
        assertEquals(
            "1 day left in 2026",
            DailyContent.build(at(2026, 12, 30), Voice.MORNING, YearLineStyle.COUNTDOWN).yearLine
        )
    }

    @Test
    fun `days lived is singular on the first of January`() {
        assertEquals(
            "1 day lived this year",
            DailyContent.build(at(2026, 1, 1), Voice.MORNING, YearLineStyle.DAYS_LIVED).yearLine
        )
    }

    @Test
    fun `a custom word can be drawn`() {
        val mine = "A line I wrote myself."
        // Some day in the year has to select the last entry in the pool.
        val drawn = (1..400).any {
            DailyContent.build(
                at(2026, 1, 1).plusDays(it.toLong()), Voice.MORNING,
                custom = listOf(mine)
            ).affirmation == mine
        }
        assertTrue("a custom line should surface within a year", drawn)
    }

    @Test
    fun `the small hours still read as evening`() {
        assertEquals(Voice.EVENING, DailyContent.voiceAt(1, eveningEnabled = true, eveningHour = 18))
        assertEquals(Voice.EVENING, DailyContent.voiceAt(23, eveningEnabled = true, eveningHour = 18))
        assertEquals(Voice.MORNING, DailyContent.voiceAt(7, eveningEnabled = true, eveningHour = 18))
        assertEquals(Voice.MORNING, DailyContent.voiceAt(23, eveningEnabled = false, eveningHour = 18))
    }

    @Test
    fun `every hour maps to a phase`() {
        (0..23).forEach { DailyContent.phaseAt(it) }
        assertEquals(Phase.DAWN, DailyContent.phaseAt(6))
        assertEquals(Phase.DAY, DailyContent.phaseAt(12))
        assertEquals(Phase.DUSK, DailyContent.phaseAt(19))
        assertEquals(Phase.NIGHT, DailyContent.phaseAt(23))
        assertEquals(Phase.NIGHT, DailyContent.phaseAt(3))
    }

    @Test
    fun `text scale lifts every line, not just the affirmation`() {
        val small = TextScale.SMALL
        val large = TextScale.LARGE
        assertTrue(DailyContent.weekdaySizeSp(large) > DailyContent.weekdaySizeSp(small))
        assertTrue(DailyContent.dateSizeSp(large) > DailyContent.dateSizeSp(small))
        assertTrue(DailyContent.yearLineSizeSp(large) > DailyContent.yearLineSizeSp(small))
        assertTrue(
            DailyContent.affirmationSizeSp("short", large) >
                DailyContent.affirmationSizeSp("short", small)
        )
    }

    @Test
    fun `an unknown stored setting falls back instead of throwing`() {
        assertEquals(YearLineStyle.DAY_OF_YEAR, YearLineStyle.from("NOT_A_STYLE"))
        assertEquals(YearLineStyle.DAY_OF_YEAR, YearLineStyle.from(null))
        assertEquals(TextScale.MEDIUM, TextScale.from("NOT_A_SCALE"))
    }
}
