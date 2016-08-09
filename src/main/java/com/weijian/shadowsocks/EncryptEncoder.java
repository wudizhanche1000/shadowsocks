package com.weijian.shadowsocks;

import com.weijian.shadowsocks.cipher.Cipher;
import com.weijian.shadowsocks.cipher.CipherFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;


/**
 * Created by weijian on 16-8-4.
 */
public class EncryptEncoder extends ChannelOutboundHandlerAdapter {
    private final Configuration configuration;
    private boolean firstResponse = true;
    private final byte[] IV;
    private final byte[] key;
    private Cipher cipher = null;

    public EncryptEncoder(Configuration configuration, byte[] iv, byte[] key) throws Exception {
        this.configuration = configuration;
        IV = iv;
        this.key = key;
        cipher = CipherFactory.getCipher(configuration.getMethod());
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        assert msg instanceof ByteBuf;
        ByteBuf response = (ByteBuf) msg;
        byte[] data = new byte[response.readableBytes()];
        response.readBytes(data);
        response.clear();
        if (firstResponse) {
            cipher.init(Cipher.ENCRYPT, key, IV);
            response.writeBytes(IV);
            firstResponse = false;
        }
        response.writeBytes(cipher.update(data));
        ctx.writeAndFlush(response, promise);
    }
}
