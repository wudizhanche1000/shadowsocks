package com.weijian.shadowsocks;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.*;

/**
 * Created by weijian on 16-8-2.
 */
public class ShadowsocksServer {
    private static final Logger logger = LogManager.getLogger("Shadowsocks");

    private static void startServer(Configuration configuration) throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            InetAddress address = InetAddress.getByName(configuration.getServer());
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ShadowsocksInitializer(configuration));
            b.bind(address, configuration.getServerPort())
                    .sync().channel().closeFuture().sync();
        } catch (UnknownHostException e) {
            logger.error("Unknown server address {}", configuration.getServer());
            System.exit(1);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Configuration configuration = null;
        try {
            configuration = ConfigurationParser.parse(args);
        } catch (Exception e) {
            logger.error(e.getMessage());
            System.exit(1);
        }
        if (configuration.getServerPort() == 0 || configuration.getPassword().equals("")) {
            logger.error("Not enough arguments");
            System.exit(1);
        }
        startServer(configuration);
    }
}
