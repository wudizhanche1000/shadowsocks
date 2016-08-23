package com.weijian.shadowsocks;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

/**
 * Created by weijian on 16-8-2.
 */
public class ShadowsocksInitializer extends ChannelInitializer<SocketChannel> {

    public ShadowsocksInitializer() {
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addFirst(DecryptHandler.NAME,
                new DecryptHandler()
        );
        ch.pipeline().addLast(new InitRequestHandler());
    }

}
