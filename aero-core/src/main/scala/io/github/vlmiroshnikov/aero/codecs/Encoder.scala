package io.github.vlmiroshnikov.aero.codecs

import com.aerospike.client.Value
import scala.jdk.CollectionConverters.*

trait Encoder[T]:
  def encode(t: T): Value

object Encoder:

  def instance[T](f: T => Value) = new Encoder[T]:
    def encode(t: T): Value = f(t)

  def list[T](using enc: Encoder[T]) = new Encoder[List[T]] {
    override def encode(lst: List[T]): Value =
      Value.get(lst.map(v=> enc.encode(v)).asJava)
  }

  given Encoder[String] = Encoder.instance(Value.get(_))
  given Encoder[Int]    = Encoder.instance(Value.get(_))

object asValue:
  def apply[V](v: V)(using encoder: Encoder[V]): Value = encoder.encode(v)
