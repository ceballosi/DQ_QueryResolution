name := "DQ_QueryResolution"

version := "0.1"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

resolvers += Resolver.sonatypeRepo("releases")

libraryDependencies ++= Seq(
  ws,
  cache,
  "com.typesafe.play" %% "play-slick" % "2.0.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "2.0.0",
  "com.typesafe.play" %% "play-mailer" % "5.0.0",
  "org.postgresql" % "postgresql" % "9.4-1200-jdbc41",
  "joda-time" % "joda-time" % "2.9.6",
  "com.chuusai" %% "shapeless" % "2.3.2",
  "com.github.melrief" %% "purecsv" % "0.0.9",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
)

