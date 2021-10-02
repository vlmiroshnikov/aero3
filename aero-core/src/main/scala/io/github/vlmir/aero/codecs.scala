package io.github.vlmir.aero.codecs

import cats.*
import cats.syntax.all.*
import com.aerospike.client.{Bin, Record, Value}
import io.github.vlmir.aero.DecoderMagnet

import scala.compiletime.*
import scala.deriving.*
import scala.runtime.BoxedUnit
import scala.util.{Failure, Success, Try}

type Result[T] = Either[Throwable, T]

trait Encoder[T]:
  def encode(t: T): Value

object Encoder:
  def instance[T](f: T => Value) = new Encoder[T] :
    def encode(t: T): Value = f(t)

  given Encoder[String] = Encoder.instance(Value.get(_))
  given Encoder[Int] = Encoder.instance(Value.get(_))



trait RecordEncoder[T]:
  def encode(v: T): List[Bin]

object RecordEncoder:

  inline def derived[A](using m: Mirror.Of[A]): RecordEncoder[A] = {
    inline m match
      case mprod: Mirror.ProductOf[A] =>
        lazy val encoders = summonEncodersRec[mprod.MirroredElemTypes]
        new RecordEncoder[A] {
          override def encode(v: A): List[Bin] =
            encodeProduct(v.asInstanceOf[Product], encoders)
        }

      case _ => throw new RuntimeException()
  }

  inline final def summonLabelsRec[T <: Tuple]: List[String] = inline erasedValue[T] match {
    case _: EmptyTuple => Nil
    case _: (t *: ts) => constValue[t].asInstanceOf[String] :: summonLabelsRec[ts]
  }

  inline final def summonEncodersRec[T <: Tuple]: List[Encoder[_]] =
    inline erasedValue[T] match {
      case _: EmptyTuple => Nil
      case _: (t *: ts) => summonEncoder[t] :: summonEncodersRec[ts]
    }

  inline def summonEncoder[A]: Encoder[A] = summonFrom {
    case a@given Encoder[A] => a
  }

  private def encodeProduct(p: Product, encoders: List[Encoder[_]]): List[Bin] = {
    p.productIterator
      .zip(p.productElementNames)
        .zip(encoders).map {
          case ((v, name), enc) =>
            val r = (enc).asInstanceOf[Encoder[Any]].encode(v)
            new Bin(name, r)
    }.toList
  }


trait Decoder[T]:
  def decode(v: Record, name: String): Result[T]

object Decoder:
  def instance[R](decoderF: (Record, String) => R) = new Decoder[R] {
    override def decode(r: Record, name: String): Result[R] = Try(decoderF(r, name)).toEither
  }

  given Decoder[Int] = instance((r, n) => r.getInt(n))
  given Decoder[String] = instance((r, n) => r.getString(n))


trait RecordDecoder[T]:
  def bins: List[String]
  def decode(r: Record): Result[T]

object RecordDecoder:

  inline def derived[A](using m: Mirror.Of[A]): RecordDecoder[A] = {
    inline m match
      case mprod: Mirror.ProductOf[A] =>
        val res = decodeElems[m.MirroredElemTypes, m.MirroredElemLabels](0)
        new RecordDecoder[A] {
          override def bins: List[String] = res.bins

          override def decode(r: Record): Result[A] =
            res.decode(r).map(v => mprod.fromProduct(v.asInstanceOf[Product]))
        }

      case _ => throw RuntimeException("Not supported Sum types")
  }

  private val unitDecoder: Decoder[EmptyTuple] = new Decoder[EmptyTuple] {
    override def decode(v: Record, name: String): Result[EmptyTuple] = EmptyTuple.asRight
  }

  inline def summonDecoder[A]: Decoder[A] = summonFrom {
    case a@given Decoder[A] => a
  }

  inline def decodeElems[Elems <: Tuple, Labels <: Tuple](n: Int): RecordDecoder[Elems] = {
    inline erasedValue[Elems] match {
      case _: EmptyTuple => unitDecoder.asInstanceOf[RecordDecoder[Elems]]
      case _: (elem *: elems1) =>
        inline erasedValue[Labels] match {
          case _: EmptyTuple => unitDecoder.asInstanceOf[RecordDecoder[Elems]]
          case _: (label *: labels1) =>
            val fieldName = constValue[label].asInstanceOf[String]
            val decodeHead = summonDecoder[elem]
            val decodeTail = decodeElems[elems1, labels1](n + 1)

            new RecordDecoder[Elems] {
              override def bins: List[String] = fieldName +: decodeTail.bins

              override def decode(r: Record): Result[Elems] = {
                decodeHead.decode(r, fieldName) match {
                  case Right(h) => decodeTail.decode(r).map(t => (h *: t).asInstanceOf[Elems])
                  case l@Left(_) => l.asInstanceOf[Result[Elems]]
                }
              }
            }
        }
    }
  }





