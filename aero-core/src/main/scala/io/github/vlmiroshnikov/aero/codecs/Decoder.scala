package io.github.vlmiroshnikov.aero.codecs

import java.util.List as JList
import cats.*
import cats.data.*
import cats.syntax.all.*
import com.aerospike.client.{ Bin, Key, Record, Value }
import io.github.vlmiroshnikov.aero.DecoderMagnet

import scala.compiletime.*
import scala.deriving.*
import scala.runtime.BoxedUnit
import scala.util.{ Failure, Success, Try }
import scala.jdk.CollectionConverters.*

type Result[T] = Either[Throwable, T]

trait KeyDecoder[V]:
  def decode(k: Key): Result[V]

object KeyDecoder:
  def from[V](f: Key => V): KeyDecoder[V] = (k: Key) => Try(f(k)).toEither

  given Identity: KeyDecoder[Key] = from(identity)
  given KeyDecoder[Int]           = from(k => k.userKey.toInteger)
  given KeyDecoder[String]        = from(k => k.userKey.toString)

trait Decoder[T]:
  def decode(v: Record, name: String): Result[T]

trait NestedDecoder[T]:
  def decode(lst: NestedValue): Result[T]

object NestedDecoder:

  given NestedDecoder[String] = {
    case v: String => v.asRight
    case v         => TypeMismatchError(v.toString).asLeft
  }

  given NestedDecoder[Int] = {
    case v: Int => v.asRight
    case v      => TypeMismatchError(v.toString).asLeft
  }

  given NestedDecoder[Long] = {
    case v: Long => v.asRight
    case v       => TypeMismatchError(v.toString).asLeft
  }

  given NestedDecoder[Double] = {
    case v: Double => v.asRight
    case v         => TypeMismatchError(v.toString).asLeft
  }

  given [T <: PlainType]: NestedDecoder[List[T]] = {
    case v: JList[_] => Right(v.asInstanceOf[JList[T]].asScala.toList)
    case lst         => TypeMismatchError(lst.toString).asLeft[List[T]]
  }

object Decoder:

  def instance[R](decoderF: (Record, String) => R): Decoder[R] =
    (r: Record, name: String) =>
      if r.bins.containsKey(name) then Try(decoderF(r, name)).toEither
      else Left(NotFoundBin(name))

  def fromEither[R](decoderF: (Record, String) => Either[Throwable, R]) = new Decoder[R] {
    override def decode(r: Record, name: String): Result[R] = decoderF(r, name)
  }

  given Functor[NestedDecoder] = new Functor[NestedDecoder] {

    override def map[A, B](fa: NestedDecoder[A])(f: A => B): NestedDecoder[B] =
      new NestedDecoder[B]:
        override def decode(lst: NestedValue): Result[B] = fa.decode(lst).map(f)
  }

  given [K <: PlainType, V: NestedDecoder]: Decoder[Map[K, V]] = (v: Record, name: String) => {
    val dec = summon[NestedDecoder[V]]
    for {
      bin <- Option(v.getMap(name)).toRight(NotFoundBin(name))
      lst <- Try(bin.asInstanceOf[java.util.Map[K, AnyRef]].asScala).toEither
      res <- lst.toList.traverse {
               case (key, nest: java.util.List[NestedValue]) => dec.decode(nest).map(v => key -> v)
               case (key, plain: PlainType)                  => dec.decode(plain).map(v => key -> v)
             }
    } yield res.toMap[K, V]
  }

  given [T: NestedDecoder]: Decoder[List[T]] = (v: Record, name: String) => {
    val dec = summon[NestedDecoder[T]]
    for {
      bin <- Option(v.getList(name)).toRight(NotFoundBin(name))
      lst <- Try(bin.asInstanceOf[java.util.List[AnyRef]].asScala).toEither
      res <- lst.toList.traverse {
               case nest: java.util.List[PlainType] => dec.decode(nest)
               case plain: PlainType                => dec.decode(plain)
             }
    } yield res
  }

  given Decoder[Int]     = instance((r, n) => r.getInt(n))
  given Decoder[String]  = instance((r, n) => r.getString(n))
  given Decoder[Long]    = instance((r, n) => r.getLong(n))
  given Decoder[Double]  = instance((r, n) => r.getDouble(n))
  given Decoder[Boolean] = instance((r, n) => r.getBoolean(n))

case class NotFoundBin(bin: String) extends RuntimeException {
  override def getMessage: String = s"Not found bin: ${bin}"
}

case class TypeMismatchError(v: String) extends RuntimeException {
  override def getMessage: String = s"Unexpected value: ${v}"
}
