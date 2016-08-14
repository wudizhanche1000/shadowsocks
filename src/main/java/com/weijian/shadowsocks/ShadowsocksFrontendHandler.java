package com.weijian.shadowsocks;

import com.weijian.shadowsocks.cipher.Cipher;
import com.weijian.shadowsocks.cipher.CipherFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * Created by weijian on 16-8-3.
 */
public class ShadowsocksFrontendHandler extends ChannelInboundHandlerAdapter {
    private boolean firstRequest = true;
    private final Logger logger = LogManager.getLogger();
    private volatile Channel outboundChannel;
    private final Cipher cipher;
    private final Configuration configuration;
    private final byte[] key;
    private final CipherFactory.CipherInfo cipherInfo;

    public ShadowsocksFrontendHandler(Configuration configuration) throws Exception {
        this.configuration = configuration;
        String algorithm = configuration.getMethod();
        this.cipher = CipherFactory.getCipher(algorithm);
        this.cipherInfo = CipherFactory.getCipherInfo(algorithm);
        this.key = configuration.getPassword().getBytes();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().read();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (outboundChannel != null) {
            NettyUtils.closeOnFlush(outboundChannel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        NettyUtils.closeOnFlush(ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        ByteBuf request = (ByteBuf) msg;
        if (firstRequest) {
            byte[] iv = new byte[cipherInfo.getIvSize()];
            request.readBytes(iv);
            cipher.init(Cipher.DECRYPT, key, iv);
            //TODO 使用ByteBuf进行加密
            byte[] t = new byte[request.readableBytes()];
            request.readBytes(t);
            byte[] decrypted = cipher.update(t);
            request.clear();
            request.writeBytes(decrypted);

            final Channel inboundChannel = ctx.channel();
            InetSocketAddress address;
            byte type = request.readByte();
            String hostname = "";
            switch (type) {
                case Constants.ADDRESS_IPV4:
                    byte[] b = new byte[Constants.ADDRESS_IPV4_LEN];
                    request.readBytes(b);
                    hostname = InetAddress.getByAddress(b).getHostAddress();
                    break;
                case Constants.ADDRESS_IPV6:
                    break;
                case Constants.ADDRESS_HOSTNAME:
                    int length = request.readByte() & 0XFF;
                    hostname = new String(request.array(), Constants.ADDRESS_HOSTNAME_OFFSET, length);
                    request.skipBytes(length);
                    break;
                default:
                    ctx.close();
                    return;
            }
            int port = request.readShort();
            address = new InetSocketAddress(hostname, port);
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(inboundChannel.eventLoop())
                    .channel(NioSocketChannel.class)
                    .handler(new ShadowsocksBackendHandler(inboundChannel, configuration))
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                    .option(ChannelOption.AUTO_READ, false);
            ChannelFuture future = bootstrap.connect(address);
            future.addListener((ChannelFutureListener) channelFuture -> {
                outboundChannel.writeAndFlush(request).addListener((ChannelFutureListener) f -> {
                            if (future.isSuccess()) {
                                ctx.channel().read();
                            } else {
                                logger.error("first Connection break;");
                                future.channel().close();
                            }
                        }

                );
            });
            outboundChannel = future.channel();
            firstRequest = false;
        } else {
            byte[] t = new byte[request.readableBytes()];
            request.readBytes(t);
            byte[] decrypted = cipher.update(t);
            request.clear();
            request.writeBytes(decrypted);
            outboundChannel.writeAndFlush(request).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess())
                    ctx.channel().read();
                else {
                    logger.error("Connection break;");
                    future.channel().close();
                }
            });
        }
    }
}
