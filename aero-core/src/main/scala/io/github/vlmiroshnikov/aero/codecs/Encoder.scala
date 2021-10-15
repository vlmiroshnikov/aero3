package io.github.vlmiroshnikov.aero.codecs

import com.aerospike.client.Value
import scala.jdk.CollectionConverters.*

trait Encoder[T]:
  def encode(t: T): Value

type PlainType   = Int | Long | String | Double
type NestedValue = PlainType | List[_ <: PlainType]

trait NestedEncoder[T]:
  def encode(r: T): NestedValue

object Encoder:

  def instance[T](f: T => Value): Encoder[T] = (t: T) => f(t)

  given [T: NestedEncoder]: Encoder[List[T]] = (lst: List[T]) => {
    val enc = summon[NestedEncoder[T]]
    val r = lst.map { v =>
      enc.encode(v) match {
        case r: List[_] => r.asJava
        case p: PlainType => p
      }
    }.asJava

    Value.get(r)
  }

  given Encoder[String] = Encoder.instance(Value.get)
  given Encoder[Int]    = Encoder.instance(Value.get)

object asValue:
  def apply[V](v: V)(using encoder: Encoder[V]): Value = encoder.encode(v)
