package org.ensime.lspclient.parsers

object SExprMapAst {

  sealed trait SExprPart

  case class SExprString(s: String) extends SExprPart

  case class SExprSymbol(symbol: String) extends SExprPart

  case class SExpr(parts: Seq[SExprPart]) extends SExprPart

  case class SExprMap(fields: Map[String, SExprPart])

}

object SExprMapParser {

  import SExprMapAst._
  import fastparse.all._

  private val string =
    P("\"" ~ CharsWhile(_ != '\"', min = 0).! ~ "\"").map(SExprString)
  private val whitespace =
    P(CharsWhileIn(" \n\t\r", min = 0))
  private val symbol =
    P(CharsWhile(c => !" \"\n\t\r()".contains(c)).!).map(SExprSymbol)

  private val key = P(":" ~ symbol.!)
  private lazy val value: Parser[SExprPart] = P(string | symbol | sexpr)

  lazy val sexpr: Parser[SExprPart] =
    P("(" ~ whitespace ~ (value ~ whitespace).rep ~ ")").map(SExpr)
  lazy val sexprMap: Parser[SExprMap] = {
    val kv = key ~ whitespace ~ value ~ whitespace
    P(whitespace ~ "(" ~ whitespace ~ kv.rep ~ ")" ~ whitespace)
      .map(kvs => SExprMap(kvs.toMap))
  }
}
