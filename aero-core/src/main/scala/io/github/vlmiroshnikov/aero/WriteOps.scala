package io.github.vlmiroshnikov.aero

import scala.concurrent.duration.*
import cats.*
import cats.syntax.all.*
import com.aerospike.client.*
import com.aerospike.client.async.EventLoop
import com.aerospike.client.cdt.MapOperation
import com.aerospike.client.listener.RecordListener
import com.aerospike.client.policy.WritePolicy
import io.github.vlmiroshnikov.aero.codecs.*
import io.github.vlmiroshnikov.aero.{ AeroClient, DecoderMagnet, Schema }
import io.github.vlmiroshnikov.aero.codecs.{ Encoder, Listeners, RecordEncoder }

def put[F[_], K, V](
    key: K,
    value: V,
    ttl: Option[FiniteDuration] = None,
    sendKey: Boolean = false
  )(using
    ac: AeroClient[F],
    keyEncoder: Encoder[K],
    recordEncoder: RecordEncoder[V],
    schema: Schema): F[Unit] = {
  ac.run[Unit] { ctx =>
    val policy = ttl.fold(ctx.client.writePolicyDefault) { fd =>
      val updated = new WritePolicy(ctx.client.writePolicyDefault)
      updated.expiration = fd.toSeconds.toInt
      updated
    }

    policy.sendKey = sendKey

    val keyV = new Key(schema.namespace, schema.set, keyEncoder.encode(key))
    Either
      .catchNonFatal(
        ctx
          .client
          .put(ctx.loop,
               Listeners.writeListener(ctx.callback),
               policy,
               keyV,
               recordEncoder.encode(value)*))
      .leftMap(e => ctx.callback(e.asLeft))
  }
}

def delete[F[_], K](
    key: K
  )(using
    ac: AeroClient[F],
    keyEncoder: Encoder[K],
    schema: Schema): F[Boolean] =
  ac.run[Boolean] { ctx =>
    val policy = ctx.client.writePolicyDefault

    val keyV     = new Key(schema.namespace, schema.set, keyEncoder.encode(key))
    val listener = Listeners.deleteListener(ctx.callback)

    Either
      .catchNonFatal(ctx.client.delete(ctx.loop, listener, policy, keyV))
      .leftMap(e => ctx.callback(e.asLeft))
  }

def operate[F[_], K](
    ops: List[Operation],
    key: K,
    magnet: DecoderMagnet = DecoderMagnet.unit,
    ttl: Option[FiniteDuration] = None,
    sendKey: Boolean = false
  )(using
    ac: AeroClient[F],
    keyEncoder: Encoder[K],
    schema: Schema): F[Option[magnet.Repr]] =
  ac.run[Option[magnet.Repr]] { ctx =>
    val decoder = magnet.decoder()
    val policy =
      ttl.fold(ctx.client.writePolicyDefault) { fd =>
        val updated = new WritePolicy(ctx.client.writePolicyDefault)
        updated.expiration = fd.toSeconds.toInt
        updated
      }
    policy.sendKey = sendKey

    val keyV     = new Key(schema.namespace, schema.set, keyEncoder.encode(key))
    val listener = Listeners.recordOptListener(ctx.callback, decoder.decode)

    Either
      .catchNonFatal(ctx.client.operate(ctx.loop, listener, policy, keyV, ops*))
      .leftMap(e => ctx.callback(e.asLeft))
  }
