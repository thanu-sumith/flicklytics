// The Play plugin
addSbtPlugin("org.playframework" % "sbt-plugin" % "3.0.10")

// Defines scaffolding (found under .g8 folder)
// http://www.foundweekends.org/giter8/scaffolding.html
// sbt "g8Scaffold form"
addSbtPlugin("org.foundweekends.giter8" % "sbt-giter8-scaffold" % "0.18.0")

// JaCoCo coverage plugin
addSbtPlugin("com.github.sbt" % "sbt-jacoco" % "3.5.0")

// Force JaCoCo libraries to a version that can read newer classfiles
val jacocoVersion = "0.8.14"
dependencyOverrides ++= Seq(
  "org.jacoco" % "org.jacoco.core" % jacocoVersion,
  "org.jacoco" % "org.jacoco.report" % jacocoVersion,
  "org.jacoco" % "org.jacoco.agent" % jacocoVersion
)