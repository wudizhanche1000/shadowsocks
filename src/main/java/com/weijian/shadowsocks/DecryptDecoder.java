package com.weijian.shadowsocks;

import com.weijian.shadowsocks.cipher.Cipher;
import com.weijian.shadowsocks.cipher.CipherFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * Created by weijian on 16-8-3.
 */
public class DecryptDecoder extends ByteToMessageDecoder {
    private final Configuration configuration;
    private final String password;
    private final String method;

    private Cipher cipher;

    public DecryptDecoder(Configuration configuration) throws Exception {
        this.configuration = configuration;
        this.password = configuration.getPassword();
        this.method = configuration.getMethod();
        this.cipher = CipherFactory.getCipher(configuration.getMethod());
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        byte[] IV = new byte[16];
        in.readBytes(IV);
        byte[] payload = new byte[in.readableBytes()];
        in.readBytes(payload);
        byte[] key = EncryptionUtils.evpBytesToKey(configuration.getPassword(), 16);
        ctx.channel().pipeline().addLast(new EncryptEncoder(configuration, IV, key));
        cipher.init(Cipher.DECRYPT, key, IV);
        byte[] result = cipher.update(payload);
        out.add(result);
    }
}
