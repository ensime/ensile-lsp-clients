package org.ensime.lspclient

import fastparse.core.Parsed.{Failure, Success}
import io.scalajs.nodejs.child_process.ChildProcess
import io.scalajs.nodejs.fs._
import io.scalajs.nodejs.path._
import io.scalajs.nodejs.process
import org.ensime.lspclient.SExprMapAst._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import scala.scalajs.js.JSConverters.JSRichFutureNonThenable
import scala.scalajs.js.annotation._

object LspClientProject {
  val delimiterForPlatform =
    Map("win32" -> ";", "darwin" -> ":", "linux" -> ":")
}

@JSExportTopLevel("LspClientProject")
class LspClientProject(javaPathFromConfig: js.UndefOr[String],
                       rootPath: String,
                       logLevel: String)
    extends js.Object {

  private val delimiter: String =
    LspClientProject.delimiterForPlatform.getOrElse(process.platform, ":")

  private val javaHome: Option[String] =
    sys.env
      .get("JDK_HOME")
      .orElse(sys.env.get("JAVA_HOME"))
      .map(dir => Path.join(dir, "bin", "java"))
      .orElse(javaPathFromConfig.toOption)

  private val javaCommand: String =
    javaHome.getOrElse("java")

  private def checkJavaRuntime(command: String): Future[Unit] = {
    val p = Promise[Unit]()
    ChildProcess
      .spawn(command, js.Array("-help"))
      .on("error", (err: js.Error) => {
        p.failure(js.JavaScriptException(err.message))
      })
      .on("exit", (exitCode: Int) => {
        p.success()
      })

    p.future
  }

  def checkRequirements(): js.Promise[Unit] = {
    if (!LspClientProject.delimiterForPlatform.contains(process.platform)) {
      js.Promise.reject(
        js.JavaScriptException(s"Unsupported platform $process.platform"))
    } else {
      checkJavaRuntime(javaCommand).toJSPromise
    }
  }

  private def ensimeFilePath: Future[String] =
    Future.successful(Path.join(rootPath, ".ensime")) // dumb solution

  def classpathFromEnsimeFile: Future[Either[String, String]] = {
    for {
      filePath <- ensimeFilePath
      data <- Fs.readFileFuture(filePath,
                                new FileInputOptions(encoding = "utf8"))
    } yield {
      SExprMapParser.sexprMap.parse(data.toString) match {
        case f @ Failure(_, _, _) => Left(s"Parser error: ${f.toString}")
        case Success(smap, _) =>
          def joinOnlyStrings(parts: Seq[SExprPart]) =
            Some(
              parts
                .collect { case SExprString(s) => s }
                .mkString(delimiter))

          val ensimeServerJars = smap.fields.get("ensime-server-jars") match {
            case Some(SExpr(parts)) => joinOnlyStrings(parts)
            case _                  => None
          }
          val scalaCompilerJars =
            smap.fields.get("scala-compiler-jars") match {
              case Some(SExpr(parts)) => joinOnlyStrings(parts)
              case _                  => None
            }

          if (ensimeServerJars.isEmpty || ensimeServerJars.isEmpty) {
            Left(
              "ensime file should contain :ensime-server-jars and :scala-compiler-jars keys")
          } else {
            Right(s"${ensimeServerJars.get}$delimiter${scalaCompilerJars.get}")
          }
      }
    }
  }

  def javaArgs: js.Promise[js.Array[String]] =
    classpathFromEnsimeFile
      .flatMap({
        case Left(err) => Future.failed(js.JavaScriptException(err))
        case Right(classpath) =>
          Future.successful(
            js.Array("-classpath",
                     classpath,
                     "-Dlsp.workspace=" + rootPath,
                     "-Dlsp.logLevel=" + logLevel,
                     "org.ensime.server.Server",
                     "--lsp"))
      })
      .toJSPromise

  val debugArgs: js.Array[String] =
    js.Array(
      "-Xdebug",
      "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000,quiet=y")

}
