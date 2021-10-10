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

type Result[T] = Either[Throwable, T]

trait Decoder[T]:
  def decode(v: Record, name: String): Result[T]

object Decoder:

  def instance[R](decoderF: (Record, String) => R) = new Decoder[R] {
    override def decode(r: Record, name: String): Result[R] = Try(decoderF(r, name)).toEither
  }

  given Decoder[Int]    = instance((r, n) => r.getInt(n))
  given Decoder[String] = instance((r, n) => r.getString(n))
