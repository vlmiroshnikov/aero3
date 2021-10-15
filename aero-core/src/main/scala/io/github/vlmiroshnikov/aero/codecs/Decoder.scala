package io.github.vlmiroshnikov.aero.codecs

import cats.*
import cats.data.*
import cats.syntax.all.*
import com.aerospike.client.{ Bin, Record, Value }
import io.github.vlmiroshnikov.aero.DecoderMagnet

import scala.compiletime.*
import scala.deriving.*
import scala.runtime.BoxedUnit
import scala.util.{ Failure, Success, Try }
import scala.jdk.CollectionConverters.*

type Result[T] = Either[Throwable, T]

trait Decoder[T]:
  def decode(v: Record, name: String): Result[T]

trait NestedDecoder[T]:
  def decode(lst: NestedValue): Result[T]

object Decoder:

  def instance[R](decoderF: (Record, String) => R) = new Decoder[R] {
    override def decode(r: Record, name: String): Result[R] = Try(decoderF(r, name)).toEither
  }

  def fromEither[R](decoderF: (Record, String) => Either[Throwable, R]) = new Decoder[R] {
    override def decode(r: Record, name: String): Result[R] = decoderF(r, name)
  }

  given [T: NestedDecoder]: Decoder[List[T]] = (v: Record, name: String) => {
    val dec = summon[NestedDecoder[T]]
    for {
      bin <- Option(v.getList(name)).toRight(NotFoundBin(name))
      lst <- Try(bin.asInstanceOf[java.util.List[AnyRef]].asScala).toEither
      res <- lst.toList.traverse {
              case nest: java.util.List[PlainType] => dec.decode(nest.asScala.toList)
              case plain: PlainType => dec.decode(plain)
            }
    } yield res
  }

  given Decoder[Int]    = instance((r, n) => r.getInt(n))
  given Decoder[String] = instance((r, n) => r.getString(n))

case class NotFoundBin(bin: String) extends RuntimeException {
  override def getMessage: String = s"Not found bin: ${bin}"
}
