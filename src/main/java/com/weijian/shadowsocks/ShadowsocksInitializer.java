package com.weijian.shadowsocks;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

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
                new DecryptDecoder(configuration),
                new ShadowsocksFrontendHandler(configuration)
        );
    }

}
