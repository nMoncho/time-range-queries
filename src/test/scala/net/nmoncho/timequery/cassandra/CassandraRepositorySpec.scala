package net.nmoncho.timequery.cassandra

import java.time.ZoneOffset

import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.TestKit
import net.nmoncho.timequery.TestData
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{Matchers, WordSpecLike}

import scala.concurrent.Future

class CassandraRepositorySpec extends TestKit(ActorSystem()) with WordSpecLike with Matchers with CassandraSpec with ScalaFutures {

  import system.dispatcher
  implicit val mat: Materializer = ActorMaterializer()

  "A CassandraRepository" should {
    val timeout = Timeout(Span(6, Seconds))

    "insert readings without exception" in {
      val repository = new CassandraRepository()

      whenReady(insertData(repository), timeout) { done =>
        done shouldBe Done
      }
    }

    "select yesterday's readings correctly (several buckets, same day)" in {
      val repository = new CassandraRepository()
      val (start, end) = TestData.startEnd(TestData.yesterday)

      val query = for {
        _ <- insertData(repository)
        query <- repository.queryRange(start, end).runWith(Sink.seq)
      } yield query

      whenReady(query, timeout) { readings =>
        readings should not be empty

        withClue("with right order") {
          readings shouldEqual readings.sortBy(_.ts.getEpochSecond)
        }

        withClue("without spilling data from another day") {
          readings.map(_.ts.atOffset(ZoneOffset.UTC).toLocalDate).toSet should have size 1
        }
      }
    }

    "select today's and yesterday's readings correctly (several buckets, several days)" in {
      val repository = new CassandraRepository()
      val (start, _) = TestData.startEnd(TestData.yesterday)
      val (_, end) = TestData.startEnd(TestData.today)

      val query = for {
        _ <- insertData(repository)
        query <- repository.queryRange(start, end).runWith(Sink.seq)
      } yield query

      whenReady(query, timeout) { readings =>
        readings should not be empty

        withClue("with right order") {
          readings shouldEqual readings.sortBy(_.ts.getEpochSecond)
        }

        withClue("without spilling data from another day") {
          readings.map(_.ts.atOffset(ZoneOffset.UTC).toLocalDate).toSet should have size 2
        }
      }
    }

    def insertData(repository: CassandraRepository): Future[Done] =
      Future.sequence(TestData.data.map(repository.write)).map(_ => Done)
  }

  override def settings: CassandraSettings = CassandraSettings.fromConfig(system.settings.config)
}
