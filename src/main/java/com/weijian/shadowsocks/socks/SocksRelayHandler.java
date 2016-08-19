package com.weijian.shadowsocks.socks;

import com.weijian.shadowsocks.NettyUtils;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by weijian on 16-8-18.
 */
public class SocksRelayHandler extends ChannelInboundHandlerAdapter {
    private final Channel relayChannel;
    private static final Logger logger = LogManager.getLogger();

    public SocksRelayHandler(Channel relayChannel) {
        this.relayChannel = relayChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (relayChannel.isActive()) {
            relayChannel.write(msg);
        } else {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().flush();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (relayChannel.isActive())
            NettyUtils.closeOnFlush(relayChannel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(cause.getMessage());
        ctx.close();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
    }
}
