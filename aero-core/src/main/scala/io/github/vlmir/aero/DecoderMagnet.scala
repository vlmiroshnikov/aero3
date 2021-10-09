package io.github.vlmir.aero

import io.github.vlmir.aero.codecs.RecordDecoder

trait DecoderMagnet:
  type Repr

  def decoder(): RecordDecoder[Repr]

object DecoderMagnet:

  val unit = new DecoderMagnet {
    override type Repr = EmptyTuple
    override def decoder(): RecordDecoder[Repr] = RecordDecoder.unit
  }

object as:

  def apply[T](using recordDecoder: RecordDecoder[T]) = new DecoderMagnet {
    override type Repr = T

    override def decoder(): RecordDecoder[Repr] = recordDecoder
  }
