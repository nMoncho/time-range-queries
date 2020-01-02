package net.nmoncho.timequery.cassandra

import java.util

import com.datastax.driver.core.{HostDistance, Session}
import com.datastax.driver.extras.codecs.jdk8.{InstantCodec, LocalDateCodec}
import org.cassandraunit.CQLDataLoader
import org.cassandraunit.dataset.CQLDataSet
import org.cassandraunit.utils.EmbeddedCassandraServerHelper
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

import scala.collection.JavaConverters._
import scala.concurrent.{Await, Awaitable}
import scala.concurrent.duration.DurationInt
import scala.io.Source

/**
  * Use this trait when you want to test your unit tests against an embedded Cassandra.
  * This trait will start an embedded Cassandra and create a single schema for you to run your tests.
  * Before each test all tables in the test keyspace are truncated to facilitate test independence.
  * If you need more schemas for your tests, use `initializeSchema`.
  */
trait CassandraSpec extends BeforeAndAfterAll with BeforeAndAfterEach { this: Suite =>

  /**
    * Filepath of the main schema.
    */
  def schema: String = "schema.cql"

  /**
    * Cassandra connection settings.
    */
  def settings: CassandraSettings

  def startTimeout: Long = EmbeddedCassandraServerHelper.DEFAULT_STARTUP_TIMEOUT * 3

  implicit var session: Session = _

  override def beforeAll(): Unit = {
    super.beforeAll()

    EmbeddedCassandraServerHelper.startEmbeddedCassandra("cassandraunit.yml", startTimeout)
    initializeSchema(
      settings.keyspace,
      schema
    )

    session = EmbeddedCassandraServerHelper.getSession
    session.execute(s"USE ${settings.keyspace}")

    session.getCluster.getConfiguration.getPoolingOptions
      .setMaxRequestsPerConnection(HostDistance.LOCAL, 32768)
      .setMaxRequestsPerConnection(HostDistance.REMOTE, 2000)

    session.getCluster.getConfiguration.getCodecRegistry
      .register(
        InstantCodec.instance,
        LocalDateCodec.instance
      )
  }

  override def afterAll(): Unit = {
    EmbeddedCassandraServerHelper.cleanEmbeddedCassandra()
    super.afterAll()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    truncateKeyspace(settings.keyspace)
  }

  /**
    * Creates a schema into Cassandra from a resource.
    */
  protected def initializeSchema(keyspace: String, cqlResourcePath: String): Unit = {
    val sysSession = EmbeddedCassandraServerHelper.getSession
    val schema = Source.fromInputStream(getClass.getClassLoader.getResourceAsStream("schema.cql"))
    val loader  = new CQLDataLoader(sysSession)
    val dLoader = new StringCQLDataSet(keyspace, schema.mkString)
    loader.load(dLoader)

  }

  private class StringCQLDataSet(keyspace: String, cql: String) extends CQLDataSet {
    override def getCQLStatements: util.List[String] =
      cqlLines(cql).asJava

    override def getKeyspaceName: String     = keyspace
    override def isKeyspaceCreation: Boolean = true
    override def isKeyspaceDeletion: Boolean = false

    private def cqlLines(schema: String): List[String] = {
      val terminator                            = ";"
      val containsText                          = "\\w+".r
      def containsWordChars(s: String): Boolean = containsText.findFirstIn(s).isDefined

      schema.split(terminator).filter(_.nonEmpty).filter(containsWordChars).toList
    }
  }

  protected def truncateKeyspace(keyspace: String): Unit =
    for {
      cluster <- Option(session.getCluster)
      metadata <- Option(cluster.getMetadata)
      ks <- Option(metadata.getKeyspace(keyspace))
    } {
      ks.getTables.asScala.foreach(t => session.execute(s"TRUNCATE TABLE $keyspace.${t.getName}"))
    }

  protected def awaitStreamResult[T](aw: Awaitable[T]): T =
    Await.result(aw, 10.seconds)

}
