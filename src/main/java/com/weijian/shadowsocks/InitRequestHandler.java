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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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

    public static final String ONE_AUTH_ALGORITHM = "HmacSHA1";


    private static final DefaultEventExecutorGroup executorGroup = new DefaultEventExecutorGroup(4);
    private static final Logger logger = LogManager.getLogger();
    private static final Context context = Context.INSTANCE;
    private static final Configuration configuration = context.getConfiguration();
    private static CipherFactory.CipherInfo cipherInfo = context.getCipherInfo();

    private Cipher cipher = CipherFactory.getCipher(configuration.getMethod());
    private Mac macDigest = null;
    private byte[] iv = new byte[cipherInfo.getIvSize()];
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
                .option(ChannelOption.AUTO_READ, false)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
        if (context.isDebug())
            logger.debug("connect to {}:{}", address.getHostAddress(), port);
        b.connect(address, port).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                if (!ctx.isRemoved()) {
                    ctx.pipeline().addLast(new DecryptHandler(cipher));
                    if ((type & 0x10) != 0) {
                        ctx.pipeline().addLast(new OneTimeAuthHandler(macDigest, iv));
                    }
                    ctx.pipeline().addLast(new RelayHandler(future.channel()));
                    ctx.pipeline().remove(this);
                    ctx.fireChannelRead(msg);
                } else {
                    future.channel().close();
                }
            } else {
                logger.error("connect to {}:{} failed", address.getHostAddress(), port);
                if (msg instanceof ByteBuf)
                    ((ByteBuf) msg).release();
                if (ctx.channel().isActive())
                    NettyUtils.closeOnFlush(ctx.channel());
                future.channel().close();
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
            ByteBuf decrypted = ctx.channel().alloc().directBuffer(request.capacity());
            cipher.update(request, decrypted);
            request.release();

            byte type = decrypted.readByte();
            // If server switch OTA on but client didn't, close the connection.
            if ((type & 0x10) == 0 && configuration.getAuth()) {
                NettyUtils.closeOnFlush(ctx.channel());
                decrypted.release();
                return;
            }
            byte[] buffer;
            int port;
            final InetAddress[] address = new InetAddress[1];
            switch (type & 0x0F) {
                case ADDRESS_IPV4:
                    buffer = new byte[ADDRESS_IPV4_LEN];
                    break;
                case ADDRESS_IPV6:
                    buffer = new byte[ADDRESS_IPV6_LEN];
                    break;
                case ADDRESS_HOSTNAME:
                    int length = decrypted.readByte() & 0xFF;
                    buffer = new byte[length];
                    break;
                default:
                    // unknown address type
                    NettyUtils.closeOnFlush(ctx.channel());
                    decrypted.release();
                    return;

            }
            decrypted.readBytes(buffer);
            port = decrypted.readShort() & 0xFFFF;
            if ((type & 0x10) != 0) {
                byte[] header;
                macDigest = Mac.getInstance(ONE_AUTH_ALGORITHM);
                // type + dst address length + port length + 1 if address type is hostname
                int headerLength = 1 + buffer.length + 2 + ((type & 0x0F) == ADDRESS_HOSTNAME ? 1 : 0);
                header = new byte[headerLength];
                header[0] = type;
                int index = 1;
                if ((type & 0x0F) == ADDRESS_HOSTNAME) {
                    header[1] = (byte) buffer.length;
                    index += 1;
                }
                System.arraycopy(buffer, 0, header, index, buffer.length);
                index += buffer.length;
                header[index] = (byte) ((port & 0xFF00) >> 8);
                header[index + 1] = (byte) (port & 0x00FF);

                // verify hmac of header
                byte[] headerKey = new byte[iv.length + cipher.getKey().length];
                if (iv.length > 0)
                    System.arraycopy(iv, 0, headerKey, 0, iv.length);
                System.arraycopy(cipher.getKey(), 0, headerKey, iv.length, cipher.getKey().length);
                SecretKeySpec keySpec;
                if (cipherInfo.getIvSize() + cipherInfo.getKeySize() != 0)
                    keySpec = new SecretKeySpec(headerKey, 0, headerKey.length, ONE_AUTH_ALGORITHM);
                else
                    keySpec = new SecretKeySpec(headerKey, headerKey.length, 0, ONE_AUTH_ALGORITHM);
                macDigest.init(keySpec);
                macDigest.update(header);
                byte[] result = macDigest.doFinal();
                byte[] hash = new byte[10];
                decrypted.readBytes(hash);
                if (!Utils.verifyHmac(hash, 0, result, 0, 10)) {
                    logger.error("one time auth verify failed");
                    NettyUtils.closeOnFlush(ctx.channel());
                    decrypted.release();
                    return;
                }
            }
            // if type is hostname, use a new executor
            if ((type & 0x0F) == ADDRESS_HOSTNAME) {
                final String hostname = new String(buffer);
                InetNameResolver nameResolver = new DefaultNameResolver(executorGroup.next());
                nameResolver.resolve(hostname).addListener(future -> {
                            if (future.isSuccess()) {
                                address[0] = (InetAddress) future.getNow();
                                fireNextChannelRead(ctx, decrypted, type, address[0], port);
                            } else {
                                if (context.isDebug())
                                    logger.debug("can't resolve hostname {}", hostname);
                                decrypted.release();
                            }
                        }
                );
            } else {
                address[0] = InetAddress.getByAddress(buffer);
                fireNextChannelRead(ctx, decrypted, type, address[0], port);
            }
        }

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        NettyUtils.closeOnFlush(ctx.channel());
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(cause.getMessage());
        NettyUtils.closeOnFlush(ctx.channel());
    }
}
