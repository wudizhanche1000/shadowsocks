package com.weijian.shadowsocks;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * Created by weijian on 16-8-4.
 */
public class ShadowsocksBackendHandler extends ChannelInboundHandlerAdapter {
    private Channel inboundChannel;

    public ShadowsocksBackendHandler(Channel inboundChannel) {
        this.inboundChannel = inboundChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        inboundChannel.writeAndFlush(msg);
    }
}
