package com.weijian.shadowsocks;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by weijian on 16-8-4.
 */
public class EncryptEncoder extends ChannelOutboundHandlerAdapter {
    private final Configuration configuration;
    private boolean firstResponse = true;
    private final byte[] IV;
    private final byte[] key;
    private Cipher cipher = null;

    public EncryptEncoder(Configuration configuration, byte[] iv, byte[] key) {
        this.configuration = configuration;
        IV = iv;
        this.key = key;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        assert msg instanceof ByteBuf;
        ByteBuf response = (ByteBuf) msg;
        byte[] data = new byte[response.readableBytes()];
        response.readBytes(data);
        System.out.println(new String(data));
        System.out.println("Before:" + data.length);
        response.clear();
        if (firstResponse) {
            cipher = Cipher.getInstance("AES/CFB/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(IV));
            response.writeBytes(IV);
            firstResponse = false;
        }
        response.writeBytes(cipher.doFinal(data));
        System.out.println("After:" + response.readableBytes());
        ctx.writeAndFlush(response, promise);
    }
}
