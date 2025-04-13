package ru.sosgps.wayrecall.packreceiver;

import com.google.common.collect.ImmutableMap;
import kamon.Kamon;
import kamon.metric.instrument.MinMaxCounter;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioWorkerPool;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Slf4JLoggerFactory;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.ThreadNameDeterminer;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;

import static kamon.util.JavaTags.tagsFromMap;

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 14.03.13
 * Time: 19:45
 * To change this template use File | Settings | File Templates.
 */
public abstract class Netty3Server extends ProtocolServer{

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(
            Netty3Server.class.getName());

    static {
        InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
    }

    protected int port;

    @Autowired
    @Qualifier("defaultThreadPool")
    public ExecutorService bossPool;

    @Autowired
    @Qualifier("workerThreadPool")
    public ExecutorService workerPool;

    private int workerCount = 1;

    @Autowired
    public HashedWheelTimer timer;

    private boolean daemon;

    protected ServerBootstrap bootstrap;

    private Channel serverChannel;

    private ChannelGroup allChannels = new DefaultChannelGroup();

    @PostConstruct
    public void start() {
        if (port > 0) {
            genBootstrap();
            // Bind and start to accept incoming connections.
            serverChannel = bootstrap.bind(new InetSocketAddress(port));

            logger.info("Listening port: {}", port);
        }
    }

    protected void genBootstrap() {

        NioWorkerPool nioWorkerPool = new NioWorkerPool(workerPool, workerCount, new ThreadNameDeterminer() {
            @Override
            public String determineThreadName(String currentThreadName, String proposedThreadName) throws Exception {
                return proposedThreadName + " p=" + port;
            }
        });

        bootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory(
                        bossPool, 1,
                        nioWorkerPool
                ));

        // Set up the pipeline factory.

        final ChannelPipelineFactory pipelineFactory = getPipelineFactory();

        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            @Override
            public ChannelPipeline getPipeline() throws Exception {
                ChannelPipeline pipeline = pipelineFactory.getPipeline();
                pipeline.addLast("AllChannelsHandler", new AllChannels());
                return pipeline;
            }
        });
    }

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


            bootstrap.shutdown();
            logger.info("terminated server on port: {}", port);
        }
    }

    protected abstract ChannelPipelineFactory getPipelineFactory();

    public void setPort(int port) {
        this.port = port;
    }

    public void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }

    public int getWorkerCount() {
        return workerCount;
    }

    public void setWorkerCount(int workerCount) {
        this.workerCount = workerCount;
    }

    private class AllChannels extends SimpleChannelUpstreamHandler {


        final MinMaxCounter connectionCounter = Kamon.metrics().minMaxCounter(
                "nettyConnections",
                tagsFromMap(ImmutableMap.of("server", Netty3Server.this.getClass().getSimpleName()))
        );

        @Override
        public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            allChannels.add(e.getChannel());
            connectionCounter.increment();
            //logger.debug("adding new channel "+allChannels.size());
            super.channelOpen(ctx, e);
        }

        @Override
        public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            allChannels.remove(e.getChannel());
            connectionCounter.decrement();
            //logger.debug("removeing channel "+allChannels.size());
            super.channelClosed(ctx, e);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
            logger.warn("exception caught leading to closing channel", e.getCause());
            super.exceptionCaught(ctx, e);
        }
    }

}
