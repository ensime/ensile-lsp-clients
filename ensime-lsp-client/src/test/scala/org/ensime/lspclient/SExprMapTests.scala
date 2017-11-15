package org.ensime.lspclient

import fastparse.core.Parsed.Success
import org.ensime.lspclient.SExprMapAst._
import utest._

object SExprMapTests extends TestSuite {
  val tests = Tests{
    'parseSExprMap - {
      val Success(m, _) = SExprMapParser.sexprMap.parse(
        """
          |(
          |  :a b
          |  :c "d"
          |  :key1 "string 1"
          |  :key2 ("string 1" "string 2")
          |  :key3 (val1 val2 val3)
          |)
        """.stripMargin)
      assert(m == SExprMap(
        Map(
          "a" -> SExprSymbol("b"),
          "key1" -> SExprString("string 1"),
          "key2" -> SExpr(Seq(
            SExprString("string 1"),
            SExprString("string 2")
          )),
          "c" -> SExprString("d"),
          "key3" -> SExpr(Seq(
            SExprSymbol("val1"),
            SExprSymbol("val2"),
            SExprSymbol("val3")))
        )
      ))
    }
  }
}
