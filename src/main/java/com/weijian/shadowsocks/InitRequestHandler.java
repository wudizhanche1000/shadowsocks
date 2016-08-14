package com.weijian.shadowsocks;

import com.weijian.shadowsocks.cipher.CipherFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by weijian on 16-8-3.
 */
public class InitRequestHandler extends ChannelInboundHandlerAdapter {
    private static Context context = Context.INSTANCE;
    private static CipherFactory.CipherInfo cipherInfo = context.getCipherInfo();
    private byte[] iv = new byte[cipherInfo.getIvSize()];
    private int ivIndex = 0;
    private static Logger logger = LogManager.getLogger();

    public InitRequestHandler() {
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.read();
        if (context.isDebug())
            logger.debug("new connection from {}", ctx.channel().remoteAddress());
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf request = (ByteBuf) msg;
            int oldIndex = request.readerIndex();
            request.readBytes(iv, ivIndex, iv.length - ivIndex);
            ivIndex += (request.readerIndex() - oldIndex);
            if (ivIndex < iv.length) {
                request.release();
                return;
            }
            ctx.pipeline().remove(this);
            ctx.pipeline().addLast(new RelayHandler(iv));
            ctx.pipeline().fireChannelRead(request);
        }
    }
}
