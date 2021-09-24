package io.github.vlmir.aero

import cats.*
import cats.effect.IO
import cats.syntax.all.*
import io.github.vlmir.aero.codecs.{ given, *}
import io.github.vlmir.aero.reads.*
import munit.*

class IntergrationSuite extends CatsEffectSuite {

  val client = ResourceFixture(AeroClient(List("localhost"), 3000))

  case class Rec(intBin: Int, sbin: String)

  given Schema("test", "test")

  given Decoder[Rec] = Decoder
    .instance("int_bin" :: "sbin" :: Nil, r => Rec(r.getInt("int_bin"), r.getString("sbin")).asRight)


  client.test("get") { (ac: AeroClient[IO]) =>
    given AeroClient[IO] = ac

    assertEquals(get("key", as[Rec]), IO(None))
  }

}
