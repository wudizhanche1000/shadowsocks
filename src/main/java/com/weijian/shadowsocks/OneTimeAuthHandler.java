package com.weijian.shadowsocks;

import com.weijian.shadowsocks.cipher.Cipher;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * Created by weijian on 16-8-15.
 */
public class OneTimeAuthHandler extends ChannelInboundHandlerAdapter {

    public static final String ONE_AUTH_ALGORITHM = "HmacSHA256";

    private int counter = 0;

    public OneTimeAuthHandler(Cipher cipher, byte[] iv) {

    }
}
