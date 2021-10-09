package io.github.vlmir.aero.utils

import cats.syntax.either.*
import com.aerospike.client.*
import com.aerospike.client.async.EventLoop
import com.aerospike.client.listener.*
import io.github.vlmir.aero.*
import io.github.vlmir.aero.AeroClient.*
import io.github.vlmir.aero.codecs.*

import scala.jdk.CollectionConverters.*

import java.util.concurrent.LinkedBlockingQueue

object Listeners {

  def listListener[V](
      callback: Callback[List[V]],
      encoder: Record => Either[Throwable, V]): RecordSequenceListener =
    new RecordSequenceListener {

      val buffer = new LinkedBlockingQueue[V]()

      override def onRecord(key: Key, record: Record): Unit =
        encoder(record).foreach(v => buffer.put(v))
      override def onSuccess(): Unit = callback(Right(buffer.asScala.toList))
      override def onFailure(exception: AerospikeException): Unit = callback(Left(exception))
    }

  def recordOptListener[V](
      callback: Callback[Option[V]],
      encoder: Record => Either[Throwable, V]): RecordListener =
    new RecordListener {

      override def onSuccess(key: Key, record: Record): Unit =
        Option(record)
          .map(v => encoder(v))
          .fold(callback(Right(Option.empty)))(v => callback(v.map(Option(_))))

      override def onFailure(exception: AerospikeException): Unit =
        callback(Left(exception))
    }

  def writeListener(callback: Callback[Unit]): WriteListener = new WriteListener {
    override def onSuccess(key: Key): Unit = callback(().asRight)

    override def onFailure(exception: AerospikeException): Unit =
      callback(Left(exception))
  }
}
