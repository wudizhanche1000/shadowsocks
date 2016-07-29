package com.weijian.shadowsocks;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socks.SocksMessage;
import io.netty.handler.codec.socksx.v4.DefaultSocks4CommandResponse;
import io.netty.handler.codec.socksx.v4.Socks4CommandRequest;
import io.netty.handler.codec.socksx.v4.Socks4CommandStatus;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;

/**
 * Created by weijian on 16-7-29.
 */
@ChannelHandler.Sharable
public class SocksServerConnectHandler extends SimpleChannelInboundHandler<SocksMessage> {
    private final Bootstrap b = new Bootstrap();

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        SocksServerUtils.closeOnFlush(ctx.channel());
    }

    protected void channelRead0(final ChannelHandlerContext ctx, final SocksMessage message) throws Exception {
        if (message instanceof Socks4CommandRequest) {
            final Socks4CommandRequest request = (Socks4CommandRequest) message;
            Promise<Channel> promise = ctx.executor().newPromise();
            promise.addListener((FutureListener<Channel>) future -> {
                        final Channel outboundChannel = future.getNow();
                        if (future.isSuccess()) {
                            ChannelFuture responseFuture = ctx.channel().writeAndFlush(
                                    new DefaultSocks4CommandResponse(Socks4CommandStatus.SUCCESS)
                            );
                            responseFuture.addListener((ChannelFutureListener) channelFuture -> {
                                ctx.pipeline().remove(SocksServerConnectHandler.this);
                                outboundChannel.pipeline().addLast(new RelayHandler(ctx.channel()));
                                ctx.pipeline().addLast(new RelayHandler(outboundChannel));
                            });
                        } else {
                            ctx.channel().writeAndFlush(
                                    new DefaultSocks4CommandResponse(Socks4CommandStatus.REJECTED_OR_FAILED)
                            );
                            SocksServerUtils.closeOnFlush(ctx.channel());
                        }
                    }
            );
            final Channel inboundChannel = ctx.channel();
            b.group(inboundChannel.eventLoop()).channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new DirectClientHandler(promise));
            b.connect(request.dstAddr(), request.dstPort()).addListener((future) -> {
                if (future.isSuccess()) {
                } else {
                    ctx.channel().writeAndFlush(new DefaultSocks4CommandResponse(Socks4CommandStatus.REJECTED_OR_FAILED));
                    SocksServerUtils.closeOnFlush(ctx.channel());
                }
            });
        } else if (message instanceof Socks5CommandRequest) {
            final Socks5CommandRequest request = (Socks5CommandRequest) message;
            Promise<Channel> promise = ctx.executor().newPromise();
            promise.addListener((FutureListener<Channel>) future -> {
                final Channel outboundChannel = future.getNow();
                if (future.isSuccess()) {
                    ChannelFuture responseFuture = ctx.channel().writeAndFlush(
                            new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, request.dstAddrType()));
                    responseFuture.addListener((channelFuture) -> {
                        ctx.pipeline().remove(SocksServerConnectHandler.this);
                        outboundChannel.pipeline().addLast(new RelayHandler((ctx.channel())));
                    });
                } else {
                    ctx.channel().writeAndFlush(
                            new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType())
                    );
                    SocksServerUtils.closeOnFlush(ctx.channel());
                }
            });
            final Channel inboundChannel = ctx.channel();
            b.group(inboundChannel.eventLoop())
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new DirectClientHandler(promise));
            b.connect(request.dstAddr(), request.dstPort()).addListener(future -> {
                if (future.isSuccess()) {

                } else {
                    ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()));
                }
            });
        } else {
            ctx.close();
        }
    }
}
