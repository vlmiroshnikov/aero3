package io.github.vlmiroshnikov.aero

import cats.*
import cats.syntax.all.*
import com.aerospike.client.*
import com.aerospike.client.async.EventLoop
import com.aerospike.client.listener.{ RecordListener, RecordSequenceListener }
import com.aerospike.client.policy.BatchPolicy
import com.aerospike.client.query.Statement
import io.github.vlmiroshnikov.aero.codecs.*
import io.github.vlmiroshnikov.aero.{ AeroClient, DecoderMagnet, Schema }
import io.github.vlmiroshnikov.aero.codecs.{ Encoder, Listeners }

import scala.jdk.CollectionConverters.*
import scala.util.Try

def get[F[_], K](
    key: K,
    magnet: DecoderMagnet
  )(using
    ac: AeroClient[F],
    keyEncoder: Encoder[K],
    schema: Schema): F[Option[magnet.Repr]] =
  ac.run[Option[magnet.Repr]] { ctx =>
    val decoder  = magnet.decoder()
    val listener = Listeners.recordOptListener(ctx.callback, decoder.decode)
    val policy   = ctx.client.readPolicyDefault
    val keyV     = new Key(schema.namespace, schema.set, keyEncoder.encode(key))

    Either
      .catchNonFatal(ctx.client.get(ctx.loop, listener, policy, keyV, decoder.bins*))
      .leftMap(e => ctx.callback(e.asLeft))
  }

def batch[F[_], K](
    keys: List[K],
    magnet: DecoderMagnet
  )(using
    ac: AeroClient[F],
    keyEncoder: Encoder[K],
    schema: Schema): F[List[magnet.Repr]] =
  ac.run[List[magnet.Repr]] { ctx =>

    val decoder  = magnet.decoder()
    val listener = Listeners.listListener(ctx.callback, decoder.decode)
    val policy   = ctx.client.batchPolicyDefault

    def mkKey(key: K) = new Key(schema.namespace, schema.set, keyEncoder.encode(key))

    val rawKeys: Array[Key] = keys.map(mkKey).toArray[Key]
    Either
      .catchNonFatal(ctx.client.get(ctx.loop, listener, policy, rawKeys, decoder.bins*))
      .leftMap(e => ctx.callback(e.asLeft))
  }

def exists[F[_], K](
    key: K
  )(using
    ac: AeroClient[F],
    keyEncoder: Encoder[K],
    schema: Schema): F[Boolean] =
  ac.run[Boolean] { ctx =>

    val listener = Listeners.existsListener(ctx.callback)
    val policy   = ctx.client.readPolicyDefault
    val keyV     = new Key(schema.namespace, schema.set, keyEncoder.encode(key))

    Either
      .catchNonFatal(ctx.client.exists(ctx.loop, listener, policy, keyV))
      .leftMap(e => ctx.callback(e.asLeft))
  }

def query[F[_]](
    stm: Statement,
    magnet: DecoderMagnet
  )(using
    ac: AeroClient[F],
    schema: Schema): F[List[magnet.Repr]] =
  ac.run[List[magnet.Repr]] { ctx =>
    val decoder = magnet.decoder()

    stm.setBinNames(decoder.bins*)
    stm.setSetName(schema.set)
    stm.setNamespace(schema.namespace)

    val listener = Listeners.listListener(ctx.callback, decoder.decode)
    val policy   = ctx.client.queryPolicyDefault

    Either
      .catchNonFatal(ctx.client.query(ctx.loop, listener, policy, stm))
      .leftMap(e => ctx.callback(e.asLeft))
  }

def scan[F[_]](
    magnet: DecoderMagnet
  )(using
    ac: AeroClient[F],
    schema: Schema): F[List[magnet.Repr]] = {
  ac.run[List[magnet.Repr]] { ctx =>
    val decoder = magnet.decoder()

    val listener = Listeners.listListener(ctx.callback, decoder.decode)
    val policy   = ctx.client.scanPolicyDefault

    Either
      .catchNonFatal(
        ctx.client.scanAll(ctx.loop, listener, policy, schema.namespace, schema.set, decoder.bins*))
      .leftMap(e => ctx.callback(e.asLeft))
  }
}

def scanWithKey[F[_]](
    magnet: DecoderMagnet
  )(using
    ac: AeroClient[F],
    schema: Schema): F[List[(Key, magnet.Repr)]] = {
  ac.run[List[(Key, magnet.Repr)]] { ctx =>
    val decoder = magnet.decoder()

    val listener = Listeners.scanListener(ctx.callback, decoder.decode)
    val policy   = ctx.client.scanPolicyDefault

    Either
      .catchNonFatal(
        ctx.client.scanAll(ctx.loop, listener, policy, schema.namespace, schema.set, decoder.bins*))
      .leftMap(e => ctx.callback(e.asLeft))
  }
}
