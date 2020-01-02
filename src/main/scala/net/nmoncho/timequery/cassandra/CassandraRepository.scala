package net.nmoncho.timequery.cassandra

import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDate, ZoneOffset}
import java.util.UUID

import akka.NotUsed
import akka.stream.alpakka.cassandra.scaladsl.CassandraSource
import akka.stream.scaladsl.Source
import com.datastax.driver.core.{ResultSet, Row, Session}
import com.datastax.driver.extras.codecs.scala.Implicits._
import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture}
import net.nmoncho.timequery.domain.Reading

import scala.concurrent.{Future, Promise}

class CassandraRepository()(implicit session: Session) {

  import CassandraRepository.Table._
  import CassandraRepository._

  /**
    * Query readings by a time range given by start and end (inclusive)
    */
  def queryRange(start: Instant, end: Instant): Source[Reading, NotUsed] = {
    // Queries all buckets for a given day
    def queryBuckets(day: LocalDate) = {
      (0 until BucketAmount)
        .foldLeft(Source.empty[Row]) { (bucketSource, bucket) =>
          val stmt = select.bind()
            .setImplicitly(0, day)
            .setImplicitly(1, bucket)
            .setImplicitly(2, start)
            .setImplicitly(3, end)

          bucketSource
            .mergeSorted(
              CassandraSource(stmt))
        }
    }

    import scala.concurrent.duration.DurationInt

    // Queries several partition dates, if needed
    DayRange(start, end)
      .foldLeft(Source.empty[Row]) { (daysRowSource, day) =>
        daysRowSource.concat(Source.lazily(() => queryBuckets(day)))
      }.map { row =>
        Reading(
          row.getUUID(deviceId),
          row.get(ts, classOf[Instant]),
          row.getDouble(temperature),
          row.getDouble(pressure)
      )
    }
  }

  /**
    * Provides ordering based on each reading's timestamp (ASC)
    */
  implicit val rowOrdering: Ordering[Row] = new Ordering[Row] {
    override def compare(x: Row, y: Row): Int = {
      x.get(ts, classOf[Instant])
        .compareTo(y.get(ts, classOf[Instant]))
    }
  }

  private val select = session.prepare(
    s"SELECT * FROM $tableName where $day = ? AND $bucket = ? AND $ts >= ? AND $ts < ?"
  )

  /**
    * Writes a reading into Cassandra
    */
  def write(reading: Reading): Future[ResultSet] = {
    val statement = insert.bind()
      .setImplicitly(0, toLocalDate(reading.ts))
      .setImplicitly(1, bucketFor(reading.deviceId))
      .setImplicitly(2, reading.ts)
      .setImplicitly(3, reading.deviceId)
      .setImplicitly(4, reading.temperature)
      .setImplicitly(5, reading.pressure)

    session
      .executeAsync(statement)
      .asScala
  }

  private val insert = session.prepare(
    s"""INSERT INTO $tableName($day, $bucket, $ts, $deviceId, $temperature, $pressure)
       | VALUES(?, ?, ?, ?, ?, ?)""".stripMargin
  )
}

object CassandraRepository {
  val BucketAmount: Int = 5

  def toLocalDate(ts: Instant): LocalDate = ts.atOffset(ZoneOffset.UTC).toLocalDate

  def bucketFor(deviceId: UUID): Int =
    if (deviceId.hashCode() == Int.MinValue) 0 else Math.abs(deviceId.hashCode() % BucketAmount)

  /**
    * Provides a sequence of dates between instants, inclusive.
    */
  def DayRange(start: Instant, end: Instant): Seq[LocalDate] = {

    val startDate = toLocalDate(start)
    val endDate = toLocalDate(end)

    (0 to ChronoUnit.DAYS.between(startDate, endDate).toInt).map(
      idx => startDate.plusDays(idx)
    )
  }

  /**
    * Used to convert from Google's Future to Scala's
    */
  implicit class FutureOps[T](listenableFuture: ListenableFuture[T]) {
    def asScala: Future[T] = {
      val p = Promise[T]
      Futures.addCallback(
        listenableFuture,
        new FutureCallback[T] {
          override def onSuccess(a: T): Unit = p.success(a)

          override def onFailure(t: Throwable): Unit = p.failure(t)
        }
      )
      p.future
    }
  }

  object Table {
    final val tableName = "readings"

    final val day = "day"
    final val bucket = "bucket"
    final val ts = "ts"
    final val deviceId = "device_id"
    final val temperature = "temperature"
    final val pressure = "pressure"
  }

}