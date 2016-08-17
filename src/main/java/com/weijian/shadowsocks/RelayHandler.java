package com.weijian.shadowsocks;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by weijian on 16-8-14.
 */
public class RelayHandler extends ChannelInboundHandlerAdapter {
    private static Logger logger = LogManager.getLogger();

    private static final Context context = Context.INSTANCE;
    private static final Configuration configuration = context.getConfiguration();

    private final int type;
    private final Channel outboundChannel;

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(cause.getMessage());
        NettyUtils.closeOnFlush(ctx.channel());
    }

    public RelayHandler(int type, Channel outboundChannel) {
        this.type = type;
        this.outboundChannel = outboundChannel;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if(outboundChannel!=null){
            NettyUtils.closeOnFlush(outboundChannel);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        outboundChannel.writeAndFlush(msg).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                ctx.read();
            } else {
                future.channel().close();
            }
        });
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }
}
