package io.github.vlmiroshnikov.aero.codecs

import com.aerospike.client.Value

trait Encoder[T]:
  def encode(t: T): Value

object Encoder:

  def instance[T](f: T => Value) = new Encoder[T]:
    def encode(t: T): Value = f(t)

  given Encoder[String] = Encoder.instance(Value.get(_))
  given Encoder[Int]    = Encoder.instance(Value.get(_))

object asValue:
  def apply[V](v: V)(using encoder: Encoder[V]): Value = encoder.encode(v)
