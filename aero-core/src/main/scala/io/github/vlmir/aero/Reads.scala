package io.github.vlmir.aero.reads

import cats.*
import cats.syntax.all.*
import com.aerospike.client.*
import com.aerospike.client.async.EventLoop
import com.aerospike.client.listener.RecordListener
import com.aerospike.client.query.Statement

import io.github.vlmir.aero.*
import io.github.vlmir.aero.AeroClient.*
import io.github.vlmir.aero.codecs.*
import io.github.vlmir.aero.utils.Listeners

import scala.jdk.CollectionConverters.*

import scala.util.Try

def get[F[_], K](
    key: K,
    magnet: DecoderMagnet
  )(using
    ac: AeroClient[F],
    keyEncoder: Encoder[K],
    schema: Schema): F[Option[magnet.Repr]] = {
  ac.run[Option[magnet.Repr]] { ctx =>

    val decoder  = magnet.decoder()
    val listener = Listeners.recordOptListener(ctx.callback, decoder.decode(_))
    val policy   = ctx.client.readPolicyDefault
    val keyV     = new Key(schema.namespace, schema.set, keyEncoder.encode(key))

    Either
      .catchNonFatal(ctx.client.get(ctx.loop, listener, policy, keyV, decoder.bins*))
      .leftMap(e => ctx.callback(e.asLeft))
  }
}

def query[F[_], R](
    stm: Statement,
    magnet: DecoderMagnet
  )(using
    ac: AeroClient[F],
    schema: Schema): F[List[magnet.Repr]] = {
  ac.run[List[magnet.Repr]] { ctx =>
    val decoder = magnet.decoder()

    stm.setBinNames(decoder.bins*)
    stm.setSetName(schema.set)
    stm.setNamespace(schema.namespace)

    val listener = Listeners.listListener(ctx.callback, decoder.decode(_))
    val policy   = ctx.client.queryPolicyDefault

    Either
      .catchNonFatal(ctx.client.query(ctx.loop, listener, policy, stm))
      .leftMap(e => ctx.callback(e.asLeft))
  }
}
