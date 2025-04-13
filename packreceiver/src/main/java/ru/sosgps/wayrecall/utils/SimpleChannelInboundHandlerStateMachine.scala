package ru.sosgps.wayrecall.utils

import _root_.io.netty.buffer.ByteBuf
import _root_.io.netty.channel.{ChannelPipeline, ChannelHandlerContext, SimpleChannelInboundHandler}

/**
  * Created by nickl on 08.03.17.
  */


class SimpleChannelInboundHandlerStateMachine[T] extends SimpleChannelInboundHandler[T] {

  private var currentState: State = new State {
    override def channelRead0(ctx: ChannelHandlerContext, msg: T) = throw new IllegalStateException("no initialState")
  }

  abstract class State extends SimpleChannelInboundHandler[T] {

    def channelRead0(ctx: ChannelHandlerContext, msg: T): Unit

    protected def become(other: State): Unit =
      SimpleChannelInboundHandlerStateMachine.this.become(other)
  }

  def become(state: State): Unit = {
    currentState = state
  }

  def become(handler: (T) => Unit): Unit = become(new State{
    override def channelRead0(ctx: ChannelHandlerContext, msg: T): Unit = handler(msg)
  })

  var context: ChannelHandlerContext = null

  override def channelRead0(ctx: ChannelHandlerContext, msg: T): Unit = {
    context = ctx
    currentState.channelRead0(ctx, msg)
  }
}
