package com.weijian.shadowsocks;

import com.weijian.shadowsocks.cipher.CipherFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;

import static com.weijian.shadowsocks.Constants.*;

/**
 * Created by weijian on 16-8-13.
 */
public class ServerConnectHandler extends ChannelInboundHandlerAdapter {



    private static final Logger logger = LogManager.getLogger();
    private static final Context context = Context.INSTANCE;
    private static final Configuration configuration = context.getConfiguration();
    private CipherFactory.CipherInfo cipherInfo = context.getCipherInfo();

    private byte type = 0;
    private int length = 0;

    public ServerConnectHandler() throws Exception {
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (context.isDebug())
            logger.debug("new connection from {}", ctx.channel().remoteAddress());
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        NettyUtils.closeOnFlush(ctx.channel());
        super.channelInactive(ctx);
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        assert msg instanceof ByteBuf;
        final ByteBuf request = (ByteBuf) msg;
        if (length == 0) {
            if (type == 0) {
                type = request.readByte();
            }
            switch (type & 0x0F) {
                case ADDRESS_IPV4:
                    length = ADDRESS_IPV4_LEN + 2;
                    break;
                case ADDRESS_HOSTNAME:
                    if (request.isReadable()) {
                        int hostLength = request.readByte() & 0xFF;
                        length = hostLength + 2;
                    } else {
                        ctx.read();
                        request.release();
                        return;
                    }
                    break;
                case ADDRESS_IPV6:
                    length = ADDRESS_IPV6_LEN + 2;
                    break;
                default:
                    request.release();
                    ctx.close();
                    return;
            }
        }
        if (request.readableBytes() >= length) {
            InetAddress address;
            byte[] temp = new byte[length - 2];
            request.readBytes(temp);
            if ((type & 0x0F) == ADDRESS_HOSTNAME) {
                address = InetAddress.getByName(new String(temp, CharsetUtil.US_ASCII));
            } else {
                address = InetAddress.getByAddress(temp);
            }
            int port = request.readShort() & 0xFFFF;
            Bootstrap b = new Bootstrap();
            b.group(ctx.channel().eventLoop())
                    .channel(NioSocketChannel.class)
                    .handler(new RelayHandler(ctx.channel()))
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
            if (context.isDebug())
                logger.debug("connecting to {}:{}", address.getHostAddress(), port);
            b.connect(address, port).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    ctx.pipeline().addLast(new EncryptHandler());
                    ctx.pipeline().addLast(new RelayHandler(future.channel()));
                    ctx.pipeline().remove(this);
                    ctx.fireChannelRead(request);
                } else {
                    logger.error("Connect to {}:{} failed", address.getHostAddress(), port);
                    ctx.close();
                    request.release();
                }
            });
        } else {
            ctx.read();
            request.release();
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(cause.getMessage());
        NettyUtils.closeOnFlush(ctx.channel());
    }
}
