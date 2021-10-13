package io.github.vlmiroshnikov.aero

import cats.*
import cats.effect.IO
import cats.syntax.all.*
import com.aerospike.client.cdt.{MapOperation, MapPolicy}
import com.aerospike.client.query.Statement
import io.github.vlmiroshnikov.aero.codecs.*
import io.github.vlmiroshnikov.aero.reads.*
import io.github.vlmiroshnikov.aero.writes.*
import io.github.vlmiroshnikov.aero.codecs.{RecordDecoder, RecordEncoder, asValue}
import munit.*

class IntergrationSuite extends CatsEffectSuite {

  val client = ResourceFixture(AeroClient(List("10.232.123.11"), 3000))

  case class Rec(source_sids: List[String]) derives RecordDecoder, RecordEncoder

  given Schema("tss", "report_meta")

  client.test("get".ignore) { (ac: AeroClient[IO]) =>
    given AeroClient[IO] = ac
    val rec              = Rec(List("3024fe7c-e0cf-4d67-9065-5cde44297c1f", "12", "34"))
    for {
      _ <- put("key1", rec)
      _ <- put("key2", rec)
      r <- batch(List("key1", "key2"), as[Rec])
      _ <- IO.println(s"res=$r")
    } yield assertEquals(r, List(rec, rec))
  }
}
