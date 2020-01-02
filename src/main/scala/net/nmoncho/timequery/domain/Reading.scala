package net.nmoncho.timequery.domain

import java.time.Instant
import java.util.UUID

case class Reading(deviceId: UUID,
                   ts: Instant,
                   temperature: Double,
                   pressure: Double)
