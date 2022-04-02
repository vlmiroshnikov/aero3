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

trait RecordEncoder[T]:
  def encode(v: T): List[Bin]

object RecordEncoder:

  inline def derived[A](using m: Mirror.Of[A]): RecordEncoder[A] = {
    inline m match
      case mprod: Mirror.ProductOf[A] =>
        lazy val encoders = summonEncodersRec[mprod.MirroredElemTypes]
        (v: A) => encodeProduct(v.asInstanceOf[Product], encoders)

      case _ => throw new RuntimeException("Not supported Sum type")
  }

  inline final def summonEncodersRec[T <: Tuple]: List[Encoder[_]] =
    inline erasedValue[T] match {
      case _: EmptyTuple => Nil
      case _: (t *: ts)  => summonEncoder[t] :: summonEncodersRec[ts]
    }

  inline def summonEncoder[A]: Encoder[A] = summonFrom { case a @ given Encoder[A] => a }

  private def encodeProduct(p: Product, encoders: List[Encoder[_]]): List[Bin] = {
    p.productIterator
      .zip(p.productElementNames)
      .zip(encoders)
      .map {
        case ((v, name), enc) =>
          val r = (enc).asInstanceOf[Encoder[Any]].encode(v)
          new Bin(name, r)
      }
      .toList
  }
