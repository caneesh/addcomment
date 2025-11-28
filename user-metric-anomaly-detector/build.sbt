name := "user-metric-anomaly-detector"

version := "1.0.0"

scalaVersion := "2.12.15"

organization := "com.company"

// Spark dependencies (provided scope - will be available in cluster)
libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "3.3.0" % "provided",
  "org.apache.spark" %% "spark-sql" % "3.3.0" % "provided",
  "org.apache.spark" %% "spark-hive" % "3.3.0" % "provided"
)

// Application dependencies (compile scope - will be included in fat JAR)
libraryDependencies ++= Seq(
  // Configuration management
  "com.typesafe" % "config" % "1.4.2",

  // Email functionality
  "javax.mail" % "mail" % "1.4.7",

  // Logging
  "org.slf4j" % "slf4j-api" % "1.7.36",
  "org.slf4j" % "slf4j-log4j12" % "1.7.36" % "provided"
)

// Test dependencies
libraryDependencies ++= Seq(
  // ScalaTest for unit testing
  "org.scalatest" %% "scalatest" % "3.2.15" % Test,

  // Spark testing utilities
  "org.apache.spark" %% "spark-core" % "3.3.0" % Test,
  "org.apache.spark" %% "spark-sql" % "3.3.0" % Test,
  "org.apache.spark" %% "spark-hive" % "3.3.0" % Test,

  // Mockito for mocking
  "org.mockito" %% "mockito-scala" % "1.17.12" % Test,
  "org.mockito" %% "mockito-scala-scalatest" % "1.17.12" % Test,

  // Test email server
  "com.icegreen" % "greenmail" % "1.6.9" % Test
)

// Assembly settings for creating fat JAR
assembly / assemblyJarName := s"${name.value}-${version.value}.jar"

// Merge strategy for assembly
assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => xs match {
    case "MANIFEST.MF" :: Nil => MergeStrategy.discard
    case "services" :: _ => MergeStrategy.concat
    case _ => MergeStrategy.discard
  }
  case "reference.conf" => MergeStrategy.concat
  case "application.conf" => MergeStrategy.concat
  case x if x.endsWith(".proto") => MergeStrategy.rename
  case x if x.contains("hadoop") => MergeStrategy.first
  case _ => MergeStrategy.first
}

// Exclude Spark and Hadoop from assembly (provided by cluster)
assembly / assemblyExcludedJars := {
  val cp = (assembly / fullClasspath).value
  cp filter { f =>
    f.data.getName.contains("spark") ||
    f.data.getName.contains("hadoop") ||
    f.data.getName.contains("scala-library")
  }
}

// Compiler options
scalacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-feature",
  "-deprecation",
  "-unchecked",
  "-language:postfixOps",
  "-language:implicitConversions"
)

// Parallel execution settings
Test / parallelExecution := false
