name := "s3"
version := "0.0.1"
scalaVersion := "2.11.7"

val awsVersion = "1.10.76"

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-java-sdk-s3" % awsVersion
)

enablePlugins(DockerPlugin)

val creds = settingKey[String]("creds")
creds := sys.props.getOrElse("creds", "credentials")

dockerfile in docker := {
  val jarFile: File = sbt.Keys.`package`.in(Compile, packageBin).value
  val classpath = (managedClasspath in Compile).value
  val mainclass = mainClass.in(Compile, packageBin).value.getOrElse(sys.error("Expected exactly one main class"))
  val jarTarget = s"/app/${jarFile.getName}"
  // Make a colon separated classpath with the JAR file
  val classpathString = classpath.files.map("/app/" + _.getName)
    .mkString(":") + ":" + jarTarget

  new Dockerfile {
    // Base image
    from("java")
    add(new File(creds.value), "/root/.aws/credentials")
    // Add all files on the classpath
    add(classpath.files, "/app/")
    // Add the JAR file
    add(jarFile, jarTarget)
    // On launch run Java with the classpath and the main class
    entryPoint("java", "-cp", classpathString, mainclass)
  }
}

