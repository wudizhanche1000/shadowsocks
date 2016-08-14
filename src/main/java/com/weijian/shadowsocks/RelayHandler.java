package com.weijian.shadowsocks;

import com.weijian.shadowsocks.cipher.Cipher;
import com.weijian.shadowsocks.cipher.CipherFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.resolver.DefaultNameResolver;
import io.netty.resolver.InetNameResolver;
import io.netty.util.concurrent.DefaultEventExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.Mac;
import java.net.InetAddress;

/**
 * Created by weijian on 16-8-13.
 */
public class RelayHandler extends ChannelInboundHandlerAdapter {

    public static final int ADDRESS_IPV4 = 1;
    public static final int ADDRESS_IPV6 = 4;
    public static final int ADDRESS_HOSTNAME = 3;
    public static final int ADDRESS_IPV4_LEN = 4;
    public static final int ADDRESS_IPV6_LEN = 16;
    public static final int ADDRESS_HOSTNAME_OFFSET = 2;
    public static final String ONE_AUTH_ALGORITHM = "HmacSHA256";

    private static final InetNameResolver nameResolver;

    private static final Logger logger = LogManager.getLogger();
    private static final Context context = Context.INSTANCE;
    private static final Configuration configuration = context.getConfiguration();

    private Mac macDigest;
    private final Cipher cipher = CipherFactory.getCipher(configuration.getMethod());
    private final byte[] iv;

    static {
        nameResolver = new DefaultNameResolver(new DefaultEventExecutor());
    }

    public RelayHandler(byte[] iv) throws Exception {
        this.iv = iv;
        byte[] key = context.getConfiguration().getPassword().getBytes();
        cipher.init(Cipher.DECRYPT, key, iv);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.read();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf request = (ByteBuf) msg;
            ByteBuf encrypted = ctx.alloc().buffer(request.capacity());
            cipher.update(request, encrypted);
            request.release();

            byte type = encrypted.readByte();
            byte[] ip;
            final InetAddress[] address = new InetAddress[1];
            switch (type & 0x0F) {
                case ADDRESS_IPV4:
                    ip = new byte[ADDRESS_IPV4_LEN];
                    encrypted.readBytes(ip);
                    address[0] = InetAddress.getByAddress(ip);
                case ADDRESS_IPV6:
                    ip = new byte[ADDRESS_IPV6_LEN];
                    encrypted.readBytes(ip);
                    address[0] = InetAddress.getByAddress(ip);
                case ADDRESS_HOSTNAME:
                    int length = encrypted.readByte() & 0xFF;
                    byte[] buffer = new byte[length];
                    encrypted.readBytes(buffer);
                    final String hostname = new String(buffer);
                    nameResolver.resolve(hostname).addListener(future -> {
                                if (future.isSuccess()) {
                                    address[0] = (InetAddress) future.getNow();
                                    System.out.println(address[0].toString());
                                } else {
                                    logger.error("can't resolve hostname {}", hostname);
                                }
                            }
                    );


            }
            if (configuration.getAuth()) {
                macDigest = Mac.getInstance(ONE_AUTH_ALGORITHM);

            } else {

            }
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        logger.warn(cause.getMessage());
    }
}
