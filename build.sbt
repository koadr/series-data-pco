name := "series-poc"

version := "1.0"

scalaVersion := "2.10.5"

libraryDependencies ++=
  Seq(
    "com.typesafe.slick" % "slick_2.10" % "3.0.0" force(),
    "com.github.tminglei" %% "slick-pg" % "0.10.0",
    "org.postgresql" % "postgresql" % "9.4-1202-jdbc42",
    "com.zaxxer" % "HikariCP" % "2.3.3",
    "joda-time" % "joda-time" % "2.9.1",
    "org.scalatest" % "scalatest_2.10" % "2.2.4" % "test"

  )

resolvers += "Sonatype OSS Snapshots" at
  "https://oss.sonatype.org/content/repositories/releases"

libraryDependencies += "com.storm-enroute" %% "scalameter" % "0.7"

testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework")

parallelExecution in Test := false

ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = false) }
