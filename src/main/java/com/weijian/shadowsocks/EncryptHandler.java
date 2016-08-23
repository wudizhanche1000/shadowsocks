package com.weijian.shadowsocks;

import com.weijian.shadowsocks.cipher.Cipher;
import com.weijian.shadowsocks.cipher.CipherFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.SecureRandom;

/**
 * Created by weijian on 16-8-22.
 */
public class EncryptHandler extends ChannelOutboundHandlerAdapter {
    private static final Logger logger = LogManager.getLogger();
    private static final SecureRandom random = new SecureRandom();
    private static final Context context = Context.INSTANCE;
    private static final Configuration configuration = context.getConfiguration();
    private static final CipherFactory.CipherInfo cipherInfo = context.getCipherInfo();

    private boolean init = true;
    private Cipher cipher;

    public EncryptHandler() {
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        assert msg instanceof ByteBuf;
        ByteBuf response = (ByteBuf) msg;
        ByteBuf encryptedBuffer;
        if (init) {
            byte[] iv = new byte[cipherInfo.getIvSize()];
            random.nextBytes(iv);
            cipher = CipherFactory.getCipher(configuration.getMethod());
            cipher.init(Cipher.ENCRYPT, configuration.getPassword().getBytes(), iv);
            encryptedBuffer = ctx.alloc().compositeBuffer();
            ((CompositeByteBuf) encryptedBuffer).addComponents(true, Unpooled.wrappedBuffer(iv));
            cipher.update(response, encryptedBuffer);
            init = false;
        } else {
            encryptedBuffer = ctx.alloc().directBuffer(response.capacity());
            cipher.update(response, encryptedBuffer);
        }
        response.release();
        super.write(ctx, encryptedBuffer, promise);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(cause.getMessage());
    }
}
