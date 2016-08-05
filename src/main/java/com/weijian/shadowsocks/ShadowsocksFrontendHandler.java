package com.weijian.shadowsocks;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.StringReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;

/**
 * Created by weijian on 16-8-3.
 */
public class ShadowsocksFrontendHandler extends ChannelInboundHandlerAdapter {
    private boolean firstRequest = true;
    private Logger logger = LogManager.getLogger();
    private Channel outboundChannel;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (firstRequest) {
            Channel inboundChannel = ctx.channel();
            ByteBuf request = Unpooled.wrappedBuffer((byte[]) msg);
            InetSocketAddress address;
            byte type = request.readByte();
            String hostname = "";
            switch (type) {
                case Constants.ADDRESS_IPV4:
                    byte[] b = new byte[Constants.ADDRESS_IPV4_LEN];
                    request.readBytes(b);
                    hostname = InetAddress.getByAddress(b).getHostAddress();
                    break;
                case Constants.ADDRESS_IPV6:
                    break;
                case Constants.ADDRESS_HOSTNAME:
                    int length = request.readByte() & 0XFF;
                    hostname = new String(request.array(), Constants.ADDRESS_HOSTNAME_OFFSET, length);
                    request.skipBytes(length);
                    break;
                default:
                    ctx.close();
                    return;
            }
            int port = request.readShort();
            address = new InetSocketAddress(hostname, port);
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(inboundChannel.eventLoop())
                    .channel(inboundChannel.getClass())
                    .handler(new ShadowsocksBackendHandler(inboundChannel));
            ChannelFuture future = bootstrap.connect(address).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    outboundChannel.writeAndFlush(request);
                }
            });
            outboundChannel = future.channel();
            System.out.println(hostname + ":" + port);
            firstRequest = false;
        } else {
            if (outboundChannel.isActive())
                outboundChannel.writeAndFlush(msg);
        }
    }
}
