name := "Wikifier"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.github.finagle" %% "finch-core" % "0.11.0-M2",
  "com.github.finagle" %% "finch-circe" % "0.11.0-M2",
  "com.twitter" % "twitter-server_2.11" % "1.21.0",
  "io.circe" %% "circe-generic" % "0.5.0-M2",
  "org.apache.thrift" % "libthrift" % "0.9.3" pomOnly()
)