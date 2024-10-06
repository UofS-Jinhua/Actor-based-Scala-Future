val scala3Version = "3.3.1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "futureactor-lib-remote",
    version := "0.1.0",
    organization := "MyCode",

    scalaVersion := scala3Version,

    libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test,
    libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "2.3.0",
    libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.8.1",

    resolvers += "Akka library repository".at("https://repo.akka.io/maven"),

    libraryDependencies += "com.typesafe.akka" %% "akka-remote" % "2.9.1"
  )
