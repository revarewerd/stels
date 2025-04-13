package ru.sosgps.wayrecall.avlprotocols.common

import java.util.concurrent.ConcurrentHashMap

import io.netty.channel.{ChannelHandlerContext, ChannelOutboundHandlerAdapter}

import scala.collection.JavaConversions

/**
  * Created by nickl-mac on 21.02.16.
  */
class ConnectedDevicesWatcher[T <: AnyRef](imeiProvider: (T) => Option[String]){

  val devices = JavaConversions.mapAsScalaConcurrentMap(new ConcurrentHashMap[String, T]())

  def apply(imei: String) = devices(imei)

  def connectionWatcherFor(c: T) = new ChannelOutboundHandlerAdapter {

    override def flush(ctx: ChannelHandlerContext): Unit = {
      imeiProvider(c).foreach(imei =>
        devices.putIfAbsent(imei, c)
          .foreach(old =>
            if (old ne c)
              devices.put(imei, c)
          )
      )
      super.flush(ctx)
    }

    override def handlerRemoved(ctx: ChannelHandlerContext): Unit = {
      super.handlerRemoved(ctx)
      imeiProvider(c).foreach(imei => devices.remove(imei, c))
    }
  }

}