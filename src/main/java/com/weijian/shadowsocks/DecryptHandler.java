package com.weijian.shadowsocks;

import com.weijian.shadowsocks.cipher.Cipher;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by weijian on 16-8-15.
 */
public class DecryptHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LogManager.getLogger();

    private final Cipher cipher;
    private boolean init = true;

    public DecryptHandler(Cipher cipher) {
        this.cipher = cipher;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(cause.getMessage());
        NettyUtils.closeOnFlush(ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf request = (ByteBuf) msg;
            if (!request.isReadable()) {
                request.release();
                return;
            }
            ByteBuf encrypted;
            if (!init) {
                encrypted = ctx.alloc().directBuffer(request.capacity());
                cipher.update(request, encrypted);
                request.release();
            } else {
                encrypted = request;
                init = false;
            }
            ctx.fireChannelRead(encrypted);
        }
    }
}
