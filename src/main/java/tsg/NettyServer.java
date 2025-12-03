package tsg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;

import tsg.rest.aggregator.AggregateService;
import tsg.rest.aggregator.DashboardHandler;
import tsg.rest.aggregator.restclient.FetcherService;

public class NettyServer {

    private static final Logger log = LoggerFactory.getLogger(NettyServer.class);
    private final int port;
    private final AggregateService aggregateService;

    public NettyServer(int port) {
        FetcherService fetcherService = new FetcherService();
        this.port = port;
        this.aggregateService = new AggregateService(fetcherService);
    }

    void run() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new HttpServerCodec(),
                                    new HttpServerExpectContinueHandler(),
                                    new HttpObjectAggregator(64 * 1024),
                                    new HttpContentCompressor(),
                                    new DashboardHandler(aggregateService));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = bootstrap.bind(port).sync();
            log.debug("Netty server initialised on port {}", port);

            // w8 server for server closeed
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        int port = 8080;
        new NettyServer(port).run();
    }
}