package net.nmoncho.timequery.cassandra

import com.typesafe.config.Config

case class CassandraSettings(host: String,
                             port: Int,
                             keyspace: String)

object CassandraSettings {

  def fromConfig(config: Config): CassandraSettings =
    CassandraSettings(
      config.getString("cassandra.host"),
      config.getInt("cassandra.port"),
      config.getString("cassandra.keyspace")
    )

}