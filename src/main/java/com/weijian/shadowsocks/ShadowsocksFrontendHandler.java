package com.weijian.shadowsocks;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * Created by weijian on 16-8-3.
 */
public class ShadowsocksFrontendHandler extends ChannelInboundHandlerAdapter {
    private boolean firstRequest = true;
    private Logger logger = LogManager.getLogger();
    private Channel outboundChannel;

    private final Configuration configuration;

    public ShadowsocksFrontendHandler(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (firstRequest) {
            Channel inboundChannel = ctx.channel();
            final ByteBuf request = Unpooled.wrappedBuffer((byte[]) msg);
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
                    .handler(new ShadowsocksBackendHandler(inboundChannel, configuration));
            ChannelFuture future = bootstrap.connect(address);
            future.addListener((ChannelFutureListener) future1 -> {
                outboundChannel.writeAndFlush(request);
                ctx.read();
            });
            outboundChannel = future.channel();
            firstRequest = false;
        } else {
            if (outboundChannel.isActive()) {
                outboundChannel.writeAndFlush(msg);
            }
        }
    }
}
