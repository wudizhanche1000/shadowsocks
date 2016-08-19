package com.weijian.shadowsocks.socks;

import com.weijian.shadowsocks.NettyUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import io.netty.util.NetUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;

/**
 * Created by weijian on 16-8-18.
 */

@ChannelHandler.Sharable
public class SocksServerHandler extends ChannelInboundHandlerAdapter {
    public static final int IPV6_LEN = 16;
    public static SocksServerHandler INSTANCE = new SocksServerHandler();
    private static Logger logger = LogManager.getLogger();

    private SocksServerHandler() {
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf request = (ByteBuf) msg;
        int version = request.readByte() & 0xFF;
        int command = request.readByte() & 0xFF;
        request.skipBytes(1);
        int addressType = request.readByte() & 0xFF;
        if (version != 0x05 || command != 0x01) {
            ctx.close();
            return;
        }
        InetAddress address;
        switch (addressType) {
            case 0x01:
                int ip = request.readInt();
                address = InetAddress.getByName(NetUtil.intToIpAddress(ip));
                break;
            case 0x03:
                int length = request.readByte() & 0xFF;
                byte[] host = new byte[length];
                address = InetAddress.getByName(new String(host, CharsetUtil.US_ASCII));
                break;
            case 0x04:
                String ipv6;
                if (request.hasArray()) {
                    int index = request.readerIndex();
                    request.readerIndex(index + IPV6_LEN);
                    ipv6 = NetUtil.bytesToIpAddress(request.array(), request.arrayOffset() + index, IPV6_LEN);
                } else {
                    byte[] b = new byte[16];
                    request.readBytes(b);
                    ipv6 = NetUtil.bytesToIpAddress(b, 0, IPV6_LEN);
                }
                address = InetAddress.getByName(ipv6);
                break;
            default:
                ctx.close();
                return;
        }
        int port = request.readShort() & 0xFFFF;
        ctx.pipeline().addLast(new SocksServerConnectHandler(address,port));
        ctx.pipeline().remove(this);
        ctx.fireChannelActive();
        request.release();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(cause.getMessage());
        NettyUtils.closeOnFlush(ctx.channel());
    }
}
