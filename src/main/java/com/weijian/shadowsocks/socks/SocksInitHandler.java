package com.weijian.shadowsocks.socks;

import com.weijian.shadowsocks.NettyUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by weijian on 16-8-19.
 */
@ChannelHandler.Sharable
public class SocksInitHandler extends ChannelInboundHandlerAdapter {
    public static SocksInitHandler INSTANCE = new SocksInitHandler();
    private static Logger logger = LogManager.getLogger();
    private static ByteBuf response = Unpooled.directBuffer().writeBytes(new byte[]{0x05, 0x00});

    private SocksInitHandler() {
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf request = (ByteBuf) msg;
        int type = request.readByte() & 0xFF;
        if (type != 0x05) {
            ctx.close();
            return;
        }
        response.retain();
        ctx.writeAndFlush(response);
        ctx.pipeline().addLast(SocksServerHandler.INSTANCE);
        ctx.pipeline().remove(this);
        request.release();
    }

//    @Override
//    protected void channelRead0(ChannelHandlerContext ctx, SocksMessage socksRequest) throws Exception {
//        switch (socksRequest.version()) {
//            case SOCKS5:
//                if (socksRequest instanceof Socks5InitialRequest) {
//                    ctx.pipeline().addFirst(new Socks5CommandRequestDecoder());
//                    ctx.write(new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH));
//                } else if (socksRequest instanceof Socks5PasswordAuthRequest) {
//                    ctx.pipeline().addFirst(new Socks5CommandRequestDecoder());
//                    ctx.write(new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS));
//                } else if (socksRequest instanceof Socks5CommandRequest) {
//                    Socks5CommandRequest socks5CmdRequest = (Socks5CommandRequest) socksRequest;
//                    if (socks5CmdRequest.type() == Socks5CommandType.CONNECT) {
//                        ctx.pipeline().addLast(new SocksServerConnectHandler());
//                        ctx.pipeline().remove(this);
//                        ctx.fireChannelRead(socksRequest);
//                    } else {
//                        ctx.close();
//                    }
//                } else {
//                    ctx.close();
//                }
//                break;
//            default:
//                ctx.close();
//        }
//    }

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
