package com.weijian.shadowsocks;

import com.weijian.shadowsocks.socks.SocksInitHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

/**
 * Created by weijian on 16-8-18.
 */

public class ClientInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(
                SocksInitHandler.INSTANCE
        );
    }
}
