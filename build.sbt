name := """tubelytics"""
organization := "TubeLytics"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.13.15"

libraryDependencies += guice

libraryDependencies += "com.google.apis" % "google-api-services-youtube" % "v3-rev222-1.25.0"
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.9.2"

libraryDependencies += caffeine
libraryDependencies += "org.mockito" % "mockito-core" % "3.6.0"
libraryDependencies += "org.powermock" % "powermock-api-mockito2" % "2.0.9" % Test
libraryDependencies += "org.powermock" % "powermock-module-junit4" % "2.0.9" % Test

libraryDependencies += "org.junit.jupiter" % "junit-jupiter-api" % "5.7.1"
libraryDependencies += "org.junit.jupiter" % "junit-jupiter-engine" % "5.7.1"
libraryDependencies += "org.junit.jupiter" % "junit-jupiter-params" % "5.7.1"
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.6.20" % Test
val AkkaVersion = "2.6.5"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % Test


