package ru.sosgps.wayrecall.packreceiver;

import com.google.common.collect.ImmutableMap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;


import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.util.concurrent.TimeUnit;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import kamon.Kamon;
import kamon.metric.instrument.MinMaxCounter;
import static kamon.util.JavaTags.tagsFromMap;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import scala.concurrent.duration.FiniteDuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 14.03.13
 * Time: 19:45
 * To change this template use File | Settings | File Templates.
 */
public abstract class Netty4Server extends ProtocolServer {

    @Autowired
    @Qualifier("bossEventLoopGroup")
    public EventLoopGroup bossPool;

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(
            Netty4Server.class.getName());

    static {
        InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
    }

    @Autowired
    @Qualifier("workerEventLoopGroup")
    public EventLoopGroup workerPool;


//    @Autowired
//    public HashedWheelTimer timer;

    protected ServerBootstrap bootstrap;

    private Channel serverChannel;

    private ChannelGroup allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    @PostConstruct
    public void start() throws InterruptedException {
        if (port > 0) {
            genBootstrap();
            // Bind and start to accept incoming connections.
            serverChannel = bootstrap.bind(new InetSocketAddress(port)).sync().channel();

            logger.info("Listening port: {} - {}", port, this.getClass().getSimpleName());
        }
    }

    protected void genBootstrap() {
        bootstrap = new ServerBootstrap(); // (2)
        bootstrap.group(bossPool, workerPool)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new ReadTimeoutHandler(30, TimeUnit.MINUTES));
                        initPipeLine(pipeline);
                        pipeline.addLast(new AllChannels());
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128);          // (5)

    }

    @Override
    @PreDestroy
    public void stop() throws InterruptedException {
        if (bootstrap != null) {
            logger.info("terminating server on port: {}", port);
            logger.debug("closing server channel");

            logger.debug("allChannels count = " + allChannels.size());
            allChannels.close().awaitUninterruptibly();

            ChannelFuture channelFuture = serverChannel.close();
            if (!channelFuture.awaitUninterruptibly(2000))
                logger.warn("serverChannel was not closed");
            else
                logger.debug("server channel closed, releasing External Resources");

            serverChannel.close().sync();
            //bootstrap.
            logger.info("terminated server on port: {}", port);
        }
    }

    protected abstract void initPipeLine(ChannelPipeline pipeline);

    private class AllChannels extends ChannelInboundHandlerAdapter {



        final MinMaxCounter connectionCounter = Kamon.metrics().minMaxCounter(
                "nettyConnections",
                tagsFromMap(ImmutableMap.of("server", Netty4Server.this.getClass().getSimpleName()))
        );


        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            allChannels.add(ctx.channel());
            connectionCounter.increment();
            super.channelRegistered(ctx);
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            allChannels.remove(ctx.channel());
            connectionCounter.decrement();
            super.channelUnregistered(ctx);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
            // Close the connection when an exception is raised.
            //cause.printStackTrace();
            logger.warn("exception caught leading to closing channel", cause);
            ctx.close();
        }

    }

}
