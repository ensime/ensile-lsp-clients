name := "Ensime lsp clients"

lazy val commonSettings = Seq(
  organization := "org.ensime",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.12.3"
)

val generateVscodePlugin = taskKey[Unit]("Task to generate vscode plugin")
val generateAtomPlugin = taskKey[Unit]("Task to generate atom plugin")

lazy val ensimeLspClient = (project in file("ensime-lsp-client"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    commonSettings,
    scalaJSModuleKind := ModuleKind.CommonJSModule,
    scalacOptions += "-P:scalajs:sjsDefinedByDefault",
    testFrameworks += new TestFramework("utest.runner.Framework"),
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "utest" % "0.6.0" % "test",
      "io.scalajs" %%% "nodejs" % "0.4.2",
      "com.lihaoyi" %%% "fastparse" % "1.0.0"
    ),
    artifactPath in (Compile, fullOptJS) := target.value / "clientutils.js",
    artifactPath in (Compile, fastOptJS) := target.value / "clientutils.js",
    generateVscodePlugin := {
      IO.copyFile(
        target.value/"clientutils.js",
        baseDirectory.value/".."/"ensime-lsp-vscode"/"src"/"clientutils.js")
      (fullOptJS in Compile).value
    },
    generateAtomPlugin := {
      IO.copyFile(
        target.value/"clientutils.js",
        baseDirectory.value/".."/"ensime-lsp-atom"/"lib"/"clientutils.js")
      (fastOptJS in Compile).value
    }
  )
