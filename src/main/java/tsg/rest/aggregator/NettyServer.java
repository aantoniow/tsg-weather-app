package tsg.rest.aggregator;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpRequestEncoder;

public class NettyServer {

    private final int port;

    public NettyServer(int port) {
        this.port = port;
    }

    void run() throws Exception {
        // konfiguracja grup wątków: bossGroup do akceptowania połączeń, workerGroup do
        // obsługi
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new HttpRequestDecoder(),
                                    new HttpRequestEncoder(),
                                    new DashboardHandler());
                            // ostatni parametr jeśli dobrze rozumiem, to będzie klasa na "kontroler"
                            // pytanie co jeśli ja chcę mieć 3 kontrolery
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            // bind i start servera
            ChannelFuture f = bootstrap.bind(port).sync();
            System.out.println("Netty server initialised on port " + port);

            // czekaj na zamknięcie serwera
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