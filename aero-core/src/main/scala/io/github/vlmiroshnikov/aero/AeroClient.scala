package io.github.vlmiroshnikov.aero

import cats.*
import cats.effect.*
import cats.effect.syntax.all.*
import com.aerospike.client.async.{ EventLoop, EventPolicy, NioEventLoops }
import com.aerospike.client.policy.ClientPolicy
import com.aerospike.client.{ AerospikeClient, Host }

import scala.concurrent.duration.*

trait AeroClient[F[_]]:
  def run[R](func: AeroClient.Context[R] => Unit): F[R]

case class Policy(eventPolicy: EventPolicy)

object Policy:
  val default = Policy(new EventPolicy())

object AeroClient {
  type Callback[-A] = Either[Throwable, A] => Unit

  case class Context[R](client: AerospikeClient, loop: EventLoop, callback: Callback[R])

  def apply[F[_]: Async](
      hosts: List[String],
      port: Int,
      policy: Policy = Policy.default): Resource[F, AeroClient[F]] = {

    val init = for {
      loops <- Resource.fromAutoCloseable(Sync[F].delay(new NioEventLoops(policy.eventPolicy, 2)))
      cp <- Resource.pure {
              new ClientPolicy() {
                timeout = 10.seconds.toSeconds.toInt
                tendInterval = 10.seconds.toSeconds.toInt
                eventLoops = loops
              }
            }
      ac <- Resource.fromAutoCloseable(
              Sync[F].delay(new AerospikeClient(cp, hosts.map(h => new Host(h, port))*)))
    } yield (ac, cp)

    init.map { (ac, cp) =>
      new AeroClient[F] {
        override def run[R](func: Context[R] => Unit): F[R] =
          summon[Async[F]].async_[R](cb => func(Context(ac, cp.eventLoops.next, cb)))
      }
    }
  }
}

case class Schema(namespace: String, set: String)
