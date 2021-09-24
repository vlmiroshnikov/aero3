package io.github.vlmir.aero.codecs

import com.aerospike.client.{Record, Value}
import io.github.vlmir.aero.codecs.Decoder.Result

trait Encoder[T]:
  def encode(t: T): Value

object Encoder:
  def instance[T](f: T => Value) = new Encoder[T] :
    def encode(t: T): Value = f(t)


given Encoder[String] = Encoder.instance(Value.get(_))

trait Decoder[T]:
  def decode(v: Record): Result[T]

  def bins: List[String]

object Decoder:
  type Result[T] = Either[Throwable, T]

  def instance[R](binNames: List[String], decoderF: Record => Decoder.Result[R]) = new Decoder[R] {
    override def decode(v: Record): Result[R] = decoderF(v)

    override def bins: List[String] = binNames
  }

trait DecoderMagnet:
  type Repr

  def decoder(): Decoder[Repr]


object as:
  def apply[T](using recordDecoder: Decoder[T]) = new DecoderMagnet {
    override type Repr = T

    override def decoder(): Decoder[Repr] = recordDecoder
  }

trait Codec[T] extends Encoder[T] with Decoder[T]
