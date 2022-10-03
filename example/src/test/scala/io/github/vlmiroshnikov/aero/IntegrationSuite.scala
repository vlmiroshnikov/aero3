package io.github.vlmiroshnikov.aero

import cats.*
import cats.effect.{ Async, IO, IOApp }
import cats.syntax.all.*
import com.aerospike.client.cdt.{ ListOperation, ListReturnType, MapOperation, MapPolicy }
import com.aerospike.client.query.Statement
import io.github.vlmiroshnikov.aero.codecs.*
import io.github.vlmiroshnikov.aero.*
import munit.*

class IntegrationSuite extends CatsEffectSuite {

  val client = ResourceFixture(AeroClient(List("localhost"), 3000))

  case class Rec(data: List[String], watermark: Double) derives RecordEncoder, RecordDecoder
  case class ListFieldProjection(data: List[String]) derives RecordDecoder

  given Schema("test", "nested")

  val KEY = "key"

  client.test("put & get nested list") { ac =>
    given AeroClient[IO] = ac

    val record = Rec(List("a", "b", "c"), 100.0)
    val op = ListOperation.getByValueRange("data", asValue("b"), asValue("d"), ListReturnType.VALUE)
    for
      _      <- put(KEY, record, sendKey = true)
      listOp <- operate(List(op), KEY, as[ListFieldProjection])
    yield assertEquals(listOp, ListFieldProjection(List("b", "c")).some)
  }

  client.test("put and get") { ac =>
    given AeroClient[IO] = ac

    val record = Rec(List("a", "b", "c"), 100.0)

    for
      _  <- put(KEY, record)
      r  <- get(KEY, as[Rec])
      _  <- delete(KEY)
      r1 <- get(KEY, as[Rec])
    yield {
      assertEquals(r, record.some)
      assertEquals(r1, None)
    }
  }

  client.test("scan ops with key") { ac =>
    given AeroClient[IO] = ac

    val record = Rec(List("a", "b", "c"), 100.0)
    for
      _    <- put(KEY, record, sendKey = true)
      lst  <- scanWithKey[IO, String](as[Rec])
    yield assertEquals(lst.find(_._1 == KEY).map(_._2), Some(record))
  }
}
