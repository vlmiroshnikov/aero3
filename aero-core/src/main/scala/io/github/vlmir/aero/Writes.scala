package io.github.vlmir.aero.writes

import cats.*
import cats.syntax.all.*
import com.aerospike.client.*
import com.aerospike.client.async.EventLoop
import com.aerospike.client.listener.RecordListener
import io.github.vlmir.aero.*
import io.github.vlmir.aero.AeroClient.*
import io.github.vlmir.aero.codecs.*
import io.github.vlmir.aero.utils.Listeners

def put[F[_], K, V](
                  key: K,
                  value: V
                )(using
                  ac: AeroClient[F],
                  keyEncoder: Encoder[K],
                  recordEncoder: RecordEncoder[V],
                  schema: Schema): F[Unit] = {
  ac.run[Unit] { ctx =>
    val policy = ctx.client.writePolicyDefault
    val keyV = new Key(schema.namespace, schema.set, keyEncoder.encode(key))
    Either.catchNonFatal(ctx.client.put(ctx.loop, Listeners.writeListener(ctx.callback),  policy, keyV, recordEncoder.encode(value) *))
      .leftMap(e => ctx.callback(e.asLeft))
  }
}
