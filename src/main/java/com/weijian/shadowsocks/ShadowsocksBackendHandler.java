package com.weijian.shadowsocks;

import com.weijian.shadowsocks.cipher.Cipher;
import com.weijian.shadowsocks.cipher.CipherFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.security.SecureRandom;

/**
 * Created by weijian on 16-8-4.
 */
public class ShadowsocksBackendHandler extends ChannelInboundHandlerAdapter {
    private Channel inboundChannel;
    private Cipher cipher;
    private Configuration configuration;

    private boolean init = true;

    public ShadowsocksBackendHandler(Channel inboundChannel, Configuration configuration) throws Exception {
        this.configuration = configuration;
        this.inboundChannel = inboundChannel;
        cipher = CipherFactory.getCipher(configuration.getMethod());
        CipherFactory.CipherInfo info = CipherFactory.getCipherInfo(configuration.getMethod());
        byte[] iv = new byte[info.getIvSize()];
        byte[] key = EncryptionUtils.evpBytesToKey(configuration.getPassword(), info.getKeySize());
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        cipher.init(Cipher.ENCRYPT, key, iv);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf response = (ByteBuf) msg;
        byte[] data = new byte[response.readableBytes()];
        response.readBytes(data);
        byte[] encrypted = cipher.update(data);
        if (init) {
            response.writeBytes(cipher.getIv());
            init = false;
        }
        response.writeBytes(encrypted);
        inboundChannel.writeAndFlush(response);
    }
}
