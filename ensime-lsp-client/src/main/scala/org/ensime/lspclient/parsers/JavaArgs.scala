package org.ensime.lspclient.parsers

import fastparse.all._

object JavaArgs {
  private val string =
    P("\"" ~ CharsWhile(_ != '\"', min = 0).! ~ "\"")
  private val whitespace =
    P(CharsWhileIn(" \n\t\r", min = 0))
  private val arg =
    P(CharsWhile(c => !" \"\n\t\r".contains(c)).!)

  val javaArgs: Parser[Seq[String]] =
    P(whitespace ~ ((string | arg) ~ whitespace).rep)
}
