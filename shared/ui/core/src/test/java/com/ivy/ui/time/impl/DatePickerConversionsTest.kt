package com.ivy.ui.time.impl

import com.ivy.base.time.TimeProvider
import com.ivy.base.time.impl.StandardTimeConverter
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

class DatePickerConversionsTest {

    private val losAngeles = ZoneId.of("America/Los_Angeles")
    private val taipei = ZoneId.of("Asia/Taipei")

    private fun converterFor(zone: ZoneId): StandardTimeConverter {
        val timeProvider = mockk<TimeProvider> {
            every { getZoneId() } returns zone
        }
        return StandardTimeConverter(timeProvider)
    }

    private fun instantAt(zone: ZoneId, date: LocalDate, hour: Int, minute: Int): Instant =
        LocalDateTime.of(date, LocalTime.of(hour, minute)).atZone(zone).toInstant()

    @Test
    fun `Given a date, When converting to picker millis, Then it is UTC midnight of that date`() {
        // Given
        val date = LocalDate.of(2026, 11, 8)

        // When
        val millis = date.toDatePickerMillis()

        // Then
        millis shouldBe date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }

    @Test
    fun `Given picker millis at UTC midnight, When converting to date, Then it is that calendar day`() {
        // Given
        val millis = LocalDate.of(2026, 11, 8).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        // When
        val date = millis.toDatePickerDate()

        // Then
        date shouldBe LocalDate.of(2026, 11, 8)
    }

    @Test
    fun `Given the epoch day, When converting to picker millis, Then it is zero`() {
        LocalDate.EPOCH.toDatePickerMillis() shouldBe 0L
        0L.toDatePickerDate() shouldBe LocalDate.EPOCH
    }

    @Test
    fun `Given a transaction near midnight, When opening the dialog, Then it shows the local calendar day`() {
        // Given
        val localDate = LocalDate.of(2026, 3, 15)
        val cases = listOf(
            losAngeles to (23 to 30),
            losAngeles to (0 to 30),
            taipei to (23 to 30),
            taipei to (0 to 30),
        )

        cases.forEach { (zone, time) ->
            val instant = instantAt(zone, localDate, time.first, time.second)

            // When
            val shownDate = converterFor(zone).toPickerDate(instant)
            val initialMillis = shownDate.toDatePickerMillis()

            // Then
            shownDate shouldBe localDate
            Instant.ofEpochMilli(initialMillis).atZone(ZoneOffset.UTC).toLocalDate() shouldBe localDate
        }
    }

    @Test
    fun `Given a late evening instant, When the device zone differs from UTC, Then the date is not the UTC date`() {
        // Given
        val instant = instantAt(losAngeles, LocalDate.of(2026, 3, 15), 23, 30)

        // When
        val date = converterFor(losAngeles).toPickerDate(instant)

        // Then
        instant.atZone(ZoneOffset.UTC).toLocalDate() shouldBe LocalDate.of(2026, 3, 16)
        date shouldBe LocalDate.of(2026, 3, 15)
    }

    @Test
    fun `Given a picked day, When round-tripping through UTC midnight millis, Then the day is unchanged`() {
        // Given
        val date = LocalDate.of(2026, 3, 15)

        // When
        val roundTripped = date.toDatePickerMillis().toDatePickerDate()

        // Then
        roundTripped shouldBe date
    }

    @Test
    fun `Given the dialog opened on an instant, When the same day is picked, Then the date is unchanged`() {
        // Given
        val localDate = LocalDate.of(2026, 12, 31)

        listOf(losAngeles, taipei).forEach { zone ->
            listOf(23 to 30, 0 to 30).forEach { (hour, minute) ->
                val instant = instantAt(zone, localDate, hour, minute)

                // When
                val pickedDate = converterFor(zone)
                    .toPickerDate(instant)
                    .toDatePickerMillis()
                    .toDatePickerDate()

                // Then
                pickedDate shouldBe localDate
            }
        }
    }
}
