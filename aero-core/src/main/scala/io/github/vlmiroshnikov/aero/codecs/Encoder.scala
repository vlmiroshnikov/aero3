package io.github.vlmiroshnikov.aero.codecs

import com.aerospike.client.Value
import scala.jdk.CollectionConverters.*
import java.util.List as JList
import cats.Contravariant

trait Encoder[T]:
  def encode(t: T): Value

type PlainType  = Int | Long | String | Double | Boolean
type NestedType = PlainType | List[_]

type NestedValue = PlainType | JList[_]

trait NestedEncoder[T]:
  def encode(r: T): NestedValue

object NestedEncoder:

  given [T <: NestedType]: NestedEncoder[T] = (r: T) => {
    r match {
      case v: List[_]   => v.asJava
      case p: PlainType => p
    }
  }

  given Contravariant[NestedEncoder] = new Contravariant[NestedEncoder] {

    override def contramap[A, B](fa: NestedEncoder[A])(f: B => A): NestedEncoder[B] =
      new NestedEncoder[B]:
        override def encode(r: B): NestedValue = fa.encode(f(r))
  }

object Encoder:

  def instance[T](f: T => Value): Encoder[T] = (t: T) => f(t)

  given Encoder[String]  = Encoder.instance(Value.StringValue(_))
  given Encoder[Int]     = Encoder.instance(Value.IntegerValue(_))
  given Encoder[Long]    = Encoder.instance(Value.LongValue(_))
  given Encoder[Double]  = Encoder.instance(Value.DoubleValue(_))
  given Encoder[Boolean] = Encoder.instance(Value.BooleanValue(_))

  given [T: NestedEncoder]: Encoder[List[T]] = (lst: List[T]) => {
    val enc = summon[NestedEncoder[T]]
    val r   = lst.map { v => enc.encode(v) }.asJava
    Value.ListValue(r)
  }

  given [K <: PlainType, V: NestedEncoder]: Encoder[Map[K, V]] = (map: Map[K, V]) => {
    val enc = summon[NestedEncoder[V]]
    Value.MapValue(map.map((k, v) => (k, enc.encode(v))).asJava)
  }

object asValue:
  def apply[V](v: V)(using encoder: Encoder[V]): Value = encoder.encode(v)
