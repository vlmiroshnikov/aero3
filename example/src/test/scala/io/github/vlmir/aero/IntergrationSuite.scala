package io.github.vlmir.aero

import cats.*
import cats.effect.IO
import cats.syntax.all.*
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

    val rec = Rec(1, "1")
    for  {
      _ <- put("key", rec)
      r <- get("key", as[Rec])
    } yield assertEquals(r, rec.some)
  }

}
