package com.weijian.shadowsocks;

import com.weijian.shadowsocks.socks.SocksServerHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler;

/**
 * Created by weijian on 16-8-18.
 */
public class ClientInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(
                new SocksPortUnificationServerHandler(),
                SocksServerHandler.INSTANCE
        );
    }
}
