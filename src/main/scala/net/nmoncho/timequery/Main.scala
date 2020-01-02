package net.nmoncho.timequery

import com.datastax.driver.core.{Cluster, Session}
import com.datastax.driver.dse.DseCluster
import com.typesafe.config.ConfigFactory

object Main extends App {

  private val config = ConfigFactory.load()
  private val cluster: Cluster = DseCluster
    .builder()
    .addContactPoint(config.getString("cassandra.host"))
    .withPort(config.getInt("cassandra.port"))
    .build()

  implicit val session: Session = cluster.connect(config.getString("cassandra.keyspace"))

  // TODO add REST Api
}
