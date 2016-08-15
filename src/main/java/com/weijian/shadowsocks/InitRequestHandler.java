package com.weijian.shadowsocks;

import com.weijian.shadowsocks.cipher.Cipher;
import com.weijian.shadowsocks.cipher.CipherFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.resolver.DefaultNameResolver;
import io.netty.resolver.InetNameResolver;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;

/**
 * Created by weijian on 16-8-13.
 */
public class InitRequestHandler extends ChannelInboundHandlerAdapter {

    public static final int ADDRESS_IPV4 = 1;
    public static final int ADDRESS_IPV6 = 4;
    public static final int ADDRESS_HOSTNAME = 3;
    public static final int ADDRESS_IPV4_LEN = 4;
    public static final int ADDRESS_IPV6_LEN = 16;
    public static final int ADDRESS_HOSTNAME_OFFSET = 2;


    private static final DefaultEventExecutorGroup executorGroup = new DefaultEventExecutorGroup(4);
    private static final Logger logger = LogManager.getLogger();
    private static final Context context = Context.INSTANCE;
    private static final Configuration configuration = context.getConfiguration();
    private static CipherFactory.CipherInfo cipherInfo = context.getCipherInfo();

    private final Cipher cipher = CipherFactory.getCipher(configuration.getMethod());
    private final byte[] iv = new byte[cipherInfo.getIvSize()];
    private int ivIndex;

    public InitRequestHandler() throws Exception {
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        ctx.read();
        if (context.isDebug())
            logger.debug("new connection from {}", ctx.channel().remoteAddress());
    }

    private void fireNextChannelRead(ChannelHandlerContext ctx, Object msg, int type, InetAddress address, int port) throws Exception {
        Bootstrap b = new Bootstrap();
        b.group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .handler(new BackendHandler(ctx.channel()))
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
        b.connect(address, port).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                if ((type & 0x10) != 0) {
                    ctx.pipeline().addLast(new OneTimeAuthHandler(cipher, iv));
                } else {
                    ctx.pipeline().addLast(new DecryptHandler(cipher));
                }
                ctx.pipeline().addLast(new RelayHandler(type, future.channel()));
                ctx.pipeline().remove(this);
                ctx.fireChannelRead(msg);
            } else {
                logger.error("connection to {}:{} failed", address.getHostAddress(), port);
                if (msg instanceof ByteBuf)
                    ((ByteBuf) msg).release();
                ctx.close();
            }
        });
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf request = (ByteBuf) msg;
            int oldIndex = request.readerIndex();
            request.readBytes(iv, ivIndex, iv.length - ivIndex);
            ivIndex += (request.readerIndex() - oldIndex);
            // if iv wasn't filled up.
            if (ivIndex < iv.length) {
                request.release();
                return;
            }

            byte[] key = context.getConfiguration().getPassword().getBytes();
            cipher.init(Cipher.DECRYPT, key, iv);
            ByteBuf encrypted = ctx.channel().alloc().directBuffer(request.capacity());
            cipher.update(request, encrypted);
            request.release();
            byte type = encrypted.readByte();
            // If server switch OTA on but client didn't, shutdown the connection.
            if ((type & 0x10) == 0 && configuration.getAuth()) {
                NettyUtils.closeOnFlush(ctx.channel());
                encrypted.release();
                return;
            }
            byte[] ip;
            int port;
            final InetAddress[] address = new InetAddress[1];
            switch (type & 0x0F) {
                case ADDRESS_IPV4:
                    ip = new byte[ADDRESS_IPV4_LEN];
                    break;
                case ADDRESS_IPV6:
                    ip = new byte[ADDRESS_IPV6_LEN];
                    break;
                case ADDRESS_HOSTNAME:
                    int length = encrypted.readByte() & 0xFF;
                    ip = new byte[length];
                    break;
                default:
                    // unknown address type
                    NettyUtils.closeOnFlush(ctx.channel());
                    encrypted.release();
                    return;

            }
            encrypted.readBytes(ip);
            port = encrypted.readShort() & 0xFFFF;
            if ((type & 0x0F) == ADDRESS_HOSTNAME) {
                final String hostname = new String(ip);
                InetNameResolver nameResolver = new DefaultNameResolver(executorGroup.next());
                nameResolver.resolve(hostname).addListener(future -> {
                            if (future.isSuccess()) {
                                address[0] = (InetAddress) future.getNow();
                                fireNextChannelRead(ctx, encrypted, type, address[0], port);
                            } else {
                                if (context.isDebug())
                                    logger.error("can't resolve hostname {}", hostname);
                                encrypted.release();
                                NettyUtils.closeOnFlush(ctx.channel());
                            }
                        }
                );
            } else {
                address[0] = InetAddress.getByAddress(ip);
                fireNextChannelRead(ctx, encrypted, type, address[0], port);
            }
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(cause.getMessage());
        NettyUtils.closeOnFlush(ctx.channel());
    }
}
