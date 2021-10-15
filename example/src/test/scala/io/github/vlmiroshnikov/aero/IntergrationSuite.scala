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


case class Data(v: String)
class IntergrationSuite extends CatsEffectSuite {

  val client = ResourceFixture(AeroClient(List("192.168.1.35"), 3000))

  given NestedEncoder[Data] = (r: Data) => List("prefix", r.v)
  given NestedDecoder[Data] = (lst: NestedValue) =>  lst match {
    case s: String => Data(s).asRight
    case s : List[String] => Data(s.tail.head).asRight
    case _ =>   (new Exception()).asLeft

  }

  case class Rec(data: List[Data]) derives RecordEncoder, RecordDecoder

  given Schema("test", "nested")

  client.test("get".ignore) { (ac: AeroClient[IO]) =>
    given AeroClient[IO] = ac
    val rec              = Rec(List(Data("a")))
    for {
      _ <- put("key", rec)
      r <- get("key", as[Rec])
//      _ <- put("key2", rec)
//      r <- batch(List("key1", "key2"), as[Rec])
      _ <- IO.println(s"res=$r")
    } yield  ()//assertEquals(r, List(rec, rec))
  }
}
