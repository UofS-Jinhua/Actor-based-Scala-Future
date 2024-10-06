scalaVersion := "2.13.12"


name := "future-test"
version := "1.0"


val AkkaVersion = "2.8.1"

libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "2.1.1"

libraryDependencies += "org.scala-lang" % "scala-library" % "2.13.12"

// Comment out or remove the original Akka library dependency
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % AkkaVersion

// lazy val root = (project in file(".")).dependsOn(akka)

// lazy val akka = RootProject(file("../akka"))
