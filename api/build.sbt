name := """sydtransapi"""

version := "0.0.1-alpha"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.10.4"

resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "org.reactivemongo" %% "play2-reactivemongo" % "0.10.5.akka23-SNAPSHOT",
  "com.bizo" % "mighty-csv_2.10" % "0.2"
)
