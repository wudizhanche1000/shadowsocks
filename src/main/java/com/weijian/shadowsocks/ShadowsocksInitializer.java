package com.weijian.shadowsocks;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * Created by weijian on 16-8-2.
 */
public class ShadowsocksInitializer extends ChannelInitializer<SocketChannel> {

    private final Configuration configuration;

    public ShadowsocksInitializer(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(
//                new LoggingHandler(LogLevel.INFO),
//                new DecryptDecoder(configuration),
                new ShadowsocksFrontendHandler(configuration)
        );
    }

}
