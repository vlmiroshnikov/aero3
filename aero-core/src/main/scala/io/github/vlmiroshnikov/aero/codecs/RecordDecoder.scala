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

trait RecordDecoder[T]:
  def bins: List[String]
  def decode(r: Record): Result[T]

object RecordDecoder:

  val unit: RecordDecoder[EmptyTuple] = new RecordDecoder[EmptyTuple] {
    override def bins: List[String]                    = Nil
    override def decode(r: Record): Result[EmptyTuple] = Right(EmptyTuple)
  }

  inline final def summonLabelsRec[T <: Tuple]: List[String] = inline erasedValue[T] match {
    case _: EmptyTuple => Nil
    case _: (t *: ts)  => constValue[t].asInstanceOf[String] :: summonLabelsRec[ts]
  }

  inline final def summonDecodersRec[T <: Tuple]: List[Decoder[_]] =
    inline erasedValue[T] match {
      case _: EmptyTuple => Nil
      case _: (t *: ts)  => summonDecoder[t] :: summonDecodersRec[ts]
    }

  inline def summonDecoder[A]: Decoder[A] = summonFrom { case a @ given Decoder[A] => a }

  inline def derived[A](using m: Mirror.Of[A]): RecordDecoder[A] = {
    inline m match
      case mprod: Mirror.ProductOf[A] =>
        lazy val decoders = summonDecodersRec[mprod.MirroredElemTypes]
        lazy val names    = summonLabelsRec[mprod.MirroredElemLabels]

        new RecordDecoder[A] {
          override def bins: List[String] = names
          override def decode(r: Record): Result[A] = {
            names
              .zip(decoders)
              .foldRight[Result[Tuple]](EmptyTuple.asRight) {
                case ((name, dec), Right(tail)) =>
                  dec.decode(r, name).map(h => h *: tail)
                case (_, left @ Left(_)) => left
              }
              .map(p => mprod.fromProduct(p))
          }
        }

      case _ => throw RuntimeException("Not supported Sum types")
  }
