name := "DQ_QueryResolution"

version := "0.1"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

resolvers += Resolver.sonatypeRepo("releases")
resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  ws,
  cache,
  //groupID % artifactID % revision
//  groupID %% artifactID gets correct scala version % revision
  "com.typesafe.play" %% "play-slick" % "2.0.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "2.0.0",
  "com.typesafe.play" %% "play-mailer" % "5.0.0",
  "org.postgresql" % "postgresql" % "9.4-1200-jdbc41",
  "joda-time" % "joda-time" % "2.9.6",
  "com.chuusai" %% "shapeless" % "2.3.2",
  "com.github.melrief" %% "purecsv" % "0.0.9",
  "com.wix" %% "accord-core" % "0.6.1",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "org.pac4j" % "play-pac4j" % "3.0.0-RC2-SNAPSHOT",
  "org.pac4j" % "pac4j-ldap" % "2.0.0-RC2-SNAPSHOT",
  "org.pac4j" % "pac4j-http" % "2.0.0-RC2-SNAPSHOT"
)

