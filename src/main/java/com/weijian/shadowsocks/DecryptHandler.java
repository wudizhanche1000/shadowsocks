package com.weijian.shadowsocks;

import com.weijian.shadowsocks.cipher.Cipher;
import com.weijian.shadowsocks.cipher.CipherFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by weijian on 16-8-15.
 */
public class DecryptHandler extends ChannelInboundHandlerAdapter {
    public static final String NAME = "DecryptHandler";
    private static final Logger logger = LogManager.getLogger();
    private static final Context context = Context.INSTANCE;
    private static final CipherFactory.CipherInfo cipherInfo = context.getCipherInfo();
    private static Configuration configuration = context.getConfiguration();

    private final Cipher cipher = CipherFactory.getCipher(configuration.getMethod());
    private byte[] iv = null;
    private int ivIndex = 0;
    private boolean init = true;

    public DecryptHandler() throws Exception {
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(cause.getMessage());
        NettyUtils.closeOnFlush(ctx.channel());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.read();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        assert msg instanceof ByteBuf;
        ByteBuf request = (ByteBuf) msg;
        if (ivIndex < cipherInfo.getIvSize()) {
            if (iv == null) {
                iv = new byte[cipherInfo.getIvSize()];
            }
            if (request.readableBytes() >= iv.length - ivIndex) {
                request.readBytes(iv, ivIndex, iv.length - ivIndex);
                ivIndex = iv.length;
                cipher.init(Cipher.DECRYPT, configuration.getKey(), iv);
            } else {
                request.readBytes(iv, ivIndex, request.readableBytes());
                ivIndex += request.readableBytes();
                request.release();
                ctx.read();
                return;
            }
        } else if (cipherInfo.getIvSize() == 0) {
            iv = new byte[0];
            cipher.init(Cipher.DECRYPT, configuration.getKey(), iv);
        }
        if (request.isReadable()) {
            ByteBuf decrypted = ctx.alloc().directBuffer(request.capacity());
            cipher.update(request, decrypted);
            request.release();
            if (context.isServerMode() && init) {
                // 如果服务端开启了OTA或者客户端开启了OTA
                if (configuration.getAuth() || (decrypted.getByte(0) & 0x10) == 0x10)
                    ctx.pipeline().addAfter(NAME, OneTimeAuthHandler.NAME, new OneTimeAuthHandler(iv, cipher.getKey()));
                init = false;
            }
            decrypted.markReaderIndex();
            byte[] temp = new byte[decrypted.readableBytes()];
            decrypted.readBytes(temp);
            decrypted.resetReaderIndex();
            ctx.fireChannelRead(decrypted);
        } else {
            request.release();
        }
    }
}
