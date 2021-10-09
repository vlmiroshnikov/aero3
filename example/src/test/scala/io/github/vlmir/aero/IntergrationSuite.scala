package io.github.vlmir.aero

import cats.*
import cats.effect.IO
import cats.syntax.all.*
import com.aerospike.client.cdt.{ MapOperation, MapPolicy }
import com.aerospike.client.query.Statement
import io.github.vlmir.aero.codecs.*
import io.github.vlmir.aero.reads.*
import io.github.vlmir.aero.writes.*
import munit.*

class IntergrationSuite extends CatsEffectSuite {

  val client = ResourceFixture(AeroClient(List("localhost"), 3000))

  case class Rec(intBin: Int, sbin: String) derives RecordDecoder, RecordEncoder

  given Schema("test", "test")

  client.test("get") { (ac: AeroClient[IO]) =>
    given AeroClient[IO] = ac
    val rec              = Rec(1, "3")
    for {
      _ <- put("key", rec)
      r <- get("key", as[Rec])
      _ <- operate(
             MapOperation.put(MapPolicy.Default, "map", asValue("mkey"), asValue("value")) :: Nil,
             "key")
    } yield assertEquals(r, rec.some)
  }
}
