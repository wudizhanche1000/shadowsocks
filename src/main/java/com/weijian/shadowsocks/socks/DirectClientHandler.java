package com.weijian.shadowsocks.socks;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.Promise;

/**
 * Created by weijian on 16-8-18.
 */
public class DirectClientHandler extends ChannelInboundHandlerAdapter {
    private final Promise<Channel> promise;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.pipeline().remove(this);
        promise.setSuccess(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        promise.setFailure(cause);
    }

    public DirectClientHandler(Promise<Channel> promise) {
        this.promise = promise;
    }
}
