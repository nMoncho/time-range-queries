package net.nmoncho.timequery

import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDate, ZoneOffset}
import java.util.UUID

import net.nmoncho.timequery.domain.Reading

object TestData {

  val readingsPerDay: Int = 3600
  val today: LocalDate = Instant.now().atOffset(ZoneOffset.UTC).toLocalDate
  val yesterday: LocalDate = today.minusDays(1)
  val beforeYesterday: LocalDate = yesterday.minusDays(1)

  private val deltaBetweenReadings = ChronoUnit.DAYS.getDuration.getSeconds / readingsPerDay

  val data: Seq[Reading] = Seq(today, yesterday, beforeYesterday).flatMap { day =>
    (0 until readingsPerDay).map { idx =>
      Reading(
        UUID.randomUUID(),
        day.atStartOfDay().plusSeconds(deltaBetweenReadings * idx).toInstant(ZoneOffset.UTC),
        0.0,
        0.0
      )
    }
  }

  def startEnd(day: LocalDate): (Instant, Instant) = {
    val start = day.atStartOfDay().toInstant(ZoneOffset.UTC)
    val end = start.plusSeconds(ChronoUnit.DAYS.getDuration.getSeconds - 1)

    start -> end
  }
}
