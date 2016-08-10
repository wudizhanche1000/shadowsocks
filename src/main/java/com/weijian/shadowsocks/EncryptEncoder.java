package com.weijian.shadowsocks;

import com.weijian.shadowsocks.cipher.Cipher;
import com.weijian.shadowsocks.cipher.CipherFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import java.security.SecureRandom;


/**
 * Created by weijian on 16-8-4.
 */
public class EncryptEncoder extends ChannelOutboundHandlerAdapter {
    private final Configuration configuration;
    private final byte[] key;
    private boolean firstResponse = true;
    private Cipher cipher = null;

    public EncryptEncoder(Configuration configuration, byte[] key) throws Exception {
        this.configuration = configuration;
        this.key = key;
        cipher = CipherFactory.getCipher(configuration.getMethod());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        assert msg instanceof ByteBuf;
        ByteBuf response = (ByteBuf) msg;
        byte[] data = new byte[response.readableBytes()];
        response.readBytes(data);
        System.out.println(data.length);
        System.out.println(new String(data));
        response.clear();
        if (firstResponse) {
            byte[] iv = new byte[16];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            cipher.init(Cipher.ENCRYPT, key, iv);
            response.writeBytes(iv);
            firstResponse = false;
        }
        response.writeBytes(cipher.update(data));
        promise.setSuccess();
        ctx.writeAndFlush(response, promise);
    }
}
