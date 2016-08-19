package com.weijian.shadowsocks.socks;

import com.weijian.shadowsocks.Configuration;
import com.weijian.shadowsocks.Context;
import com.weijian.shadowsocks.NettyUtils;
import com.weijian.shadowsocks.RelayHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;


/**
 * Created by weijian on 16-8-18.
 */
@ChannelHandler.Sharable
public class SocksServerConnectHandler extends ChannelInboundHandlerAdapter {
    private static Logger logger = LogManager.getLogger();
    private static final ByteBuf successResponse = Unpooled.directBuffer();

    static {
        Configuration configuration = Context.INSTANCE.getConfiguration();
        successResponse.writeBytes(new byte[]{0x05, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00});
        successResponse.writeShort(configuration.getLocalPort());
    }

    private final Bootstrap b = new Bootstrap();
    InetAddress dstAddress;
    int dstPort;

    public SocksServerConnectHandler(InetAddress dstAddress, int dstPort) {
        this.dstAddress = dstAddress;
        this.dstPort = dstPort;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(cause.getMessage());
        NettyUtils.closeOnFlush(ctx.channel());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Bootstrap b = new Bootstrap();
        Promise<Channel> promise = ctx.executor().newPromise();
        promise.addListener(new FutureListener<Channel>() {
            @Override
            public void operationComplete(Future<Channel> future) throws Exception {
                Channel channel = future.getNow();
                if (future.isSuccess()) {
                    successResponse.retain();
                    ctx.writeAndFlush(successResponse).addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            ctx.pipeline().remove(SocksServerConnectHandler.this);
                            ctx.pipeline().addLast(new RelayHandler(channel));
                            channel.pipeline().addLast(new RelayHandler(ctx.channel()));
                        }
                    });
                }

            }
        });
        b.group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .handler(new DirectClientHandler(promise));
        b.connect(dstAddress, dstPort).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if(!future.isSuccess()){
                    NettyUtils.closeOnFlush(ctx.channel());
                }
            }
        });
    }

    //    @Override
//    protected void channelRead0(final ChannelHandlerContext ctx, final SocksMessage message) throws Exception {
//        if (message instanceof Socks5CommandRequest) {
//            final Socks5CommandRequest request = (Socks5CommandRequest) message;
//            Promise<Channel> promise = ctx.executor().newPromise();
//            promise.addListener(
//                    (FutureListener<Channel>) future -> {
//                        final Channel outboundChannel = future.getNow();
//                        if (future.isSuccess()) {
//                            ChannelFuture responseFuture = ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(
//                                    Socks5CommandStatus.SUCCESS,
//                                    request.dstAddrType(),
//                                    request.dstAddr(),
//                                    request.dstPort()
//                            ));
//                            responseFuture.addListener((ChannelFutureListener) channelFuture -> {
//                                ctx.pipeline().remove(SocksServerConnectHandler.this);
//                                outboundChannel.pipeline().addLast(new SocksRelayHandler(ctx.channel()));
//                                ctx.pipeline().addFirst(new SocksRelayHandler(outboundChannel));
//                            });
//                        } else {
//                            ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()));
//                            NettyUtils.closeOnFlush(ctx.channel());
//                        }
//                    }
//            );
//            final Channel inboundChannel = ctx.channel();
//            b.group(inboundChannel.eventLoop())
//                    .channel(NioSocketChannel.class)
//                    .option(ChannelOption.SO_KEEPALIVE, true)
//                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
//                    .handler(new DirectClientHandler(promise));
//            b.connect(request.dstAddr(), request.dstPort()).addListener((ChannelFutureListener) future -> {
//                        if (future.isSuccess()) {
//
//                        } else {
//                            ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()));
//                            NettyUtils.closeOnFlush(ctx.channel());
//                        }
//                    }
//            );
//
//        } else {
//            ctx.close();
//        }
//    }
}
