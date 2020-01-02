ThisBuild / scalaVersion := "2.11.12"
ThisBuild / version := "1.0.0-SNAPSHOT"
ThisBuild / organization := "net.nmoncho"
ThisBuild / organizationName := "nmoncho"

lazy val root = (project in file("."))
  .settings(
    name := "time-range-queries",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % "10.1.7",
      "com.typesafe.akka" %% "akka-http-testkit" % "10.1.7",
      "com.typesafe.akka" %% "akka-stream" % "2.5.19",
      "com.lightbend.akka" %% "akka-stream-alpakka-cassandra" % "0.20" exclude("com.datastax.cassandra", "cassandra-driver-core"),
      "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.19",
      "com.datastax.dse" % "dse-java-driver-core" % "1.6.9",
      "com.datastax.cassandra" % "cassandra-driver-extras"  % "3.6.0",
      "com.datastax.cassandra" %% "java-driver-scala-extras" % "1.0.2" excludeAll ExclusionRule(
        organization = "ch.qos.logback"
      ),
      "org.cassandraunit" % "cassandra-unit" % "3.11.2.0" excludeAll ExclusionRule(
        organization = "io.netty"
      ),
      "org.scalatest" %% "scalatest" % "3.0.1" % Test
    )
  )

