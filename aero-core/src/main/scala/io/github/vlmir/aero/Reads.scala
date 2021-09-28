package io.github.vlmir.aero.reads

import cats.*
import cats.syntax.all.*
import com.aerospike.client.*
import com.aerospike.client.async.EventLoop
import com.aerospike.client.listener.RecordListener
import io.github.vlmir.aero.*
import io.github.vlmir.aero.AeroClient.*
import io.github.vlmir.aero.codecs.*
import io.github.vlmir.aero.utils.Listeners

import scala.util.Try

def get[F[_], K](
                  key: K,
                  magnet: DecoderMagnet
                )(using
                  ac: AeroClient[F],
                  keyEncoder: Encoder[K],
                  schema: Schema): F[Option[magnet.Repr]] = {
  ac.run[Option[magnet.Repr]] { ctx =>

    val decoder = magnet.decoder()
    val listener = Listeners.recordOptListener(ctx.callback, decoder.decode(_))
    val policy = ctx.client.readPolicyDefault
    val keyV = new Key(schema.namespace, schema.set, keyEncoder.encode(key))

    Either.catchNonFatal(ctx.client.get(ctx.loop, listener, policy, keyV, decoder.bins *))
      .leftMap(e => ctx.callback(e.asLeft))
  }
}

