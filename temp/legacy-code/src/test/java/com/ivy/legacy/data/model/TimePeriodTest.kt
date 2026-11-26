package com.ivy.legacy.data.model

import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.ui.time.TimeFormatter
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.time.LocalDateTime

class TimePeriodTest {
    private val timeProvider = mockk<TimeProvider> {
        every { localNow() } returns LocalDateTime.of(2026, 11, 27, 12, 0)
    }
    private val converter = mockk<TimeConverter>()
    private val formatter = mockk<TimeFormatter>()

    private fun short(period: TimePeriod) = period.toDisplayShort(
        startDateOfMonth = 1,
        timeConverter = converter,
        timeProvider = timeProvider,
        timeFormatter = formatter,
    )

    private fun long(period: TimePeriod) = period.toDisplayLong(
        startDateOfMonth = 1,
        timeProvider = timeProvider,
        timeConverter = converter,
        timeFormatter = formatter,
    )

    @Test
    fun `current year shows the full month name`() {
        val period = TimePeriod(month = Month(11, "November"), year = 2026)

        short(period) shouldBe "November"
        long(period) shouldBe "November"
    }

    @Test
    fun `other year abbreviates the month name to three characters`() {
        val period = TimePeriod(month = Month(1, "January"), year = 2027)

        short(period) shouldBe "Jan, 2027"
        long(period) shouldBe "Jan, 2027"
    }

    @Test
    fun `other year keeps month names shorter than three characters`() {
        // zh-TW "January" is "一月": substring(0, 3) used to crash with StringIndexOutOfBoundsException
        val period = TimePeriod(month = Month(1, "一月"), year = 2027)

        short(period) shouldBe "一月, 2027"
        long(period) shouldBe "一月, 2027"
    }
}
