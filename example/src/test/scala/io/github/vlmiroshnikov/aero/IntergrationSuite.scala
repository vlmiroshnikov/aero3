package io.github.vlmiroshnikov.aero

import cats.*
import cats.effect.IO
import cats.syntax.all.*
import com.aerospike.client.cdt.{ListOperation, ListReturnType, MapOperation, MapPolicy}
import com.aerospike.client.query.Statement
import io.github.vlmiroshnikov.aero.codecs.*
import io.github.vlmiroshnikov.aero.reads.*
import io.github.vlmiroshnikov.aero.writes.*
import munit.*


case class Data(v: String)

class IntegrationSuite extends CatsEffectSuite {

  val client = ResourceFixture(AeroClient(List("10.232.123.11"), 3000))

  //given NestedEncoder[String] = (r: String) => r
//  given NestedDecoder[Data] = (lst: NestedValue) =>  lst match {
//    case s: String => Data(s).asRight
//    case s : List[String] => Data(s.tail.head).asRight
//    case _ =>   (new Exception()).asLeft
//
//  }

  case class Rec(data: List[String]) derives RecordEncoder, RecordDecoder

  given Schema("test", "nested")

  client.test("get".ignore) { ac =>
    given AeroClient[IO] = ac
    val rec              = Rec(List("a", "b", "c"))
    val op = ListOperation.getByValueRange("data", asValue("b"), asValue("d"), ListReturnType.VALUE)
    for {
      _ <- put("key", rec)
      //r <- get("key", as[Rec])
      r <-  operate(List(op), "key", None, as[Rec])
      _ <- IO.println(s"res=$r")
    } yield assertEquals(r, Rec(List("b", "c")).some)
    }
}
