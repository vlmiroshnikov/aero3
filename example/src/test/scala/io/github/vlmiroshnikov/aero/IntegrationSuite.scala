package io.github.vlmiroshnikov.aero

import cats.*
import cats.effect.{Async, IO, IOApp}
import cats.syntax.all.*
import com.aerospike.client.cdt.{ListOperation, ListReturnType, MapOperation, MapPolicy}
import com.aerospike.client.query.Statement
import io.github.vlmiroshnikov.aero.codecs.*
import io.github.vlmiroshnikov.aero.*
import munit.*

class IntegrationSuite extends CatsEffectSuite {

  val client = ResourceFixture(AeroClient(List("10.232.123.11"), 3000))
  case class Rec(data: List[String], watermark: Double) derives RecordEncoder, RecordDecoder
  case class ListData(data: List[String]) derives RecordDecoder

  given Schema("test", "nested")

  client.test("put & get nested list".ignore) { ac =>
    given AeroClient[IO] = ac

    val record = Rec(List("a", "b", "c"), 100.0)
    val op = ListOperation.getByValueRange("data", asValue("b"), asValue("d"), ListReturnType.VALUE)
    for
      _      <- put("key", record, sendKey = true)
      listOp <- operate(List(op), "key", as[ListData])
    yield assertEquals(listOp, ListData(List("b", "c")).some)
  }

  client.test("put and get".ignore) { ac =>
    given AeroClient[IO] = ac

    val record = Rec(List("a", "b", "c"), 100.0)

    for
      _ <- put("key", record)
      r <- get("key", as[Rec])
    yield assertEquals(r, record.some)
  }

  client.test("scan ops".ignore) { ac =>
    given AeroClient[IO] = ac

    scanWithKey(as[ListData]).flatMap(IO.println(_))
  }
}




