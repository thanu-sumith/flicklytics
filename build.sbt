name := """Play_web_Application"""
organization := "Concordia"

version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayJava)

scalaVersion := "2.13.18"

val jacocoVersion = "0.8.14"

libraryDependencies ++= Seq(
  guice,
  javaWs,
  caffeine,
  "org.mockito" % "mockito-core" % "5.11.0" % Test,
  "org.junit.jupiter" % "junit-jupiter" % "5.10.0" % Test,
  "org.apache.pekko" %% "pekko-actor-typed" % "1.0.3",
  "org.apache.pekko" %% "pekko-actor-testkit-typed" % "1.0.3" % Test,
  "org.apache.pekko" %% "pekko-testkit" % "1.0.3" % Test,

  // Needed for offline instrumentation (prevents NoClassDefFoundError: Offline)
  "org.jacoco" % "org.jacoco.agent" % jacocoVersion % Test classifier "runtime"
)

dependencyOverrides ++= Seq(
  "org.jacoco" % "org.jacoco.core" % jacocoVersion,
  "org.jacoco" % "org.jacoco.report" % jacocoVersion,
  "org.jacoco" % "org.jacoco.agent" % jacocoVersion
)

enablePlugins(JacocoPlugin)
Test / fork := true
jacocoIncludes := Seq(
  "controllers.*", 
  "models.*", 
  "Services.*",
  "actors.*"

)

jacocoExcludes := Seq(
  "controllers.Reverse*",        // Excludes ReverseHomeController, etc.
  "controllers.javascript.*",    // Excludes the javascript sub-package
  "controllers.routes*",         // Excludes the 'routes' and 'routes$javascript' classes
  "router.*",
  "Routes*",
  "apidoc.*",
  "views.*"
)

Compile / doc / scalacOptions ++= Seq("-private")
javacOptions in (Compile, doc) ++= Seq("-private")
