package com.weijian.shadowsocks;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.List;

/**
 * Created by weijian on 16-8-3.
 */
public class DecryptDecoder extends ByteToMessageDecoder {
    private final Configuration configuration;
    private final String password;
    private final String method;

    public DecryptDecoder(Configuration configuration) {
        this.configuration = configuration;
        this.password = configuration.getPassword();
        this.method = configuration.getMethod();
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        byte[] IV = new byte[16];
        in.readBytes(IV);
        Cipher cipher = Cipher.getInstance("AES/CFB/NoPadding");
        byte[] payload = new byte[in.readableBytes()];
        in.readBytes(payload);
        byte[] key = EncryptionTestKt.evpBytesToKey(configuration.getPassword(), 16);
        for (byte k : key) {
            System.out.print(String.format("%02x",k));
        }
        System.out.println();
        ctx.channel().pipeline().addLast(new EncryptEncoder(configuration, IV, key));
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(IV);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        byte[] result = cipher.doFinal(payload);
        out.add(result);
    }
}
