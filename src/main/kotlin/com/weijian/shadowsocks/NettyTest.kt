package com.weijian.shadowsocks

import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.socksx.SocksMessage
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler
import io.netty.handler.codec.socksx.SocksVersion
import io.netty.handler.codec.socksx.v4.DefaultSocks4CommandResponse
import io.netty.handler.codec.socksx.v4.Socks4CommandRequest
import io.netty.handler.codec.socksx.v4.Socks4CommandStatus
import io.netty.handler.codec.socksx.v4.Socks4CommandType
import io.netty.handler.codec.socksx.v5.*
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.netty.util.ReferenceCountUtil
import io.netty.util.concurrent.Future
import io.netty.util.concurrent.FutureListener
import io.netty.util.concurrent.Promise

/**
 * Created by weijian on 16-8-10.
 */


fun main(args: Array<String>) {
    val bossGroup = NioEventLoopGroup(1)
    val workerGroup = NioEventLoopGroup()
    try {
        val b = ServerBootstrap()
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .handler(LoggingHandler(LogLevel.INFO))
                .childHandler(SocksServerInitializer());
        b.bind(8888).sync().channel().closeFuture().sync();
    } finally {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully()
    }
}

class SocksServerInitializer : ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel) {
        ch.pipeline().addLast(
                LoggingHandler(LogLevel.INFO),
                SocksPortUnificationServerHandler(),
                SocksServerHandler
        )
    }

}

@ChannelHandler.Sharable
object SocksServerHandler : SimpleChannelInboundHandler<SocksMessage>() {

    @Throws(Exception::class)
    override fun channelRead0(ctx: ChannelHandlerContext, socksRequest: SocksMessage) {
        when (socksRequest.version()) {
            SocksVersion.SOCKS4a -> {
                val socksV4CmdRequest = socksRequest as Socks4CommandRequest
                if (socksV4CmdRequest.type() == Socks4CommandType.CONNECT) {
                    ctx.pipeline().addLast(SocksServerConnectHandler())
                    ctx.pipeline().remove(this)
                    ctx.fireChannelRead(socksRequest);
                } else {
                    ctx.close()
                }
            }
            SocksVersion.SOCKS5 -> {
                if (socksRequest is Socks5InitialRequest) {
                    ctx.pipeline().addFirst(Socks5CommandRequestDecoder())
                    ctx.write(DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH))
                } else if (socksRequest is Socks5PasswordAuthRequest) {
                    ctx.pipeline().addFirst(Socks5CommandRequestDecoder())
                    ctx.write(DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS));
                } else if (socksRequest is Socks5CommandRequest) {
                    if (socksRequest.type() == Socks5CommandType.CONNECT) {
                        ctx.pipeline().addLast(SocksServerConnectHandler())
                        ctx.pipeline().remove(this)
                        ctx.fireChannelRead(socksRequest)
                    } else {
                        ctx.close()
                    }
                } else {
                    ctx.close()
                }
            }
            SocksVersion.UNKNOWN ->
                ctx.close();
        }
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        ctx.flush()
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        SocksServerUtils.closeOnFlush(ctx.channel())
    }
}

object SocksServerUtils {
    fun closeOnFlush(ch: Channel) {
        if (ch.isActive)
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
    }
}

@ChannelHandler.Sharable
class SocksServerConnectHandler : SimpleChannelInboundHandler<SocksMessage>() {
    private val b = Bootstrap()
    @Throws(Exception::class)
    override fun channelRead0(ctx: ChannelHandlerContext, msg: SocksMessage) {
        if (msg is Socks4CommandRequest) {
            val promise = ctx.executor().newPromise<Channel>()
            promise.addListener(object : FutureListener<Channel> {
                override fun operationComplete(future: Future<Channel>) {
                    val outboundChannel = future.now
                    if (future.isSuccess) {
                        val responseFuture = ctx.channel().writeAndFlush(DefaultSocks4CommandResponse(Socks4CommandStatus.SUCCESS))
                        responseFuture.addListener(object : ChannelFutureListener {
                            override fun operationComplete(future: ChannelFuture) {
                                ctx.pipeline().remove(this@SocksServerConnectHandler)
                                outboundChannel.pipeline().addLast(RelayHandler(ctx.channel()))
                            }

                        })
                    } else {
                        ctx.channel().writeAndFlush(DefaultSocks4CommandResponse(Socks4CommandStatus.REJECTED_OR_FAILED))
                        SocksServerUtils.closeOnFlush(ctx.channel())
                    }
                }
            })
            val inboundChannel = ctx.channel()
            b.group(inboundChannel.eventLoop())
                    .channel(NioSocketChannel::class.java)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(DirectClientHandler(promise))
            b.connect(msg.dstAddr(), msg.dstPort()).addListener(object : ChannelFutureListener {
                override fun operationComplete(future: ChannelFuture) {
                    if (future.isSuccess) {

                    } else {
                        ctx.channel().writeAndFlush(DefaultSocks4CommandResponse(Socks4CommandStatus.REJECTED_OR_FAILED))
                        SocksServerUtils.closeOnFlush(ctx.channel())
                    }
                }
            })

        } else if (msg is Socks5CommandRequest) {
            val promise = ctx.executor().newPromise<Channel>()
            promise.addListener(object : FutureListener<Channel> {
                override fun operationComplete(future: Future<Channel>) {
                    val outboundChannel = future.now
                    if (future.isSuccess) {
                        val responseFuture = ctx.channel().writeAndFlush(DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, msg.dstAddrType()))
                        responseFuture.addListener {
                            channelFuture ->
                            ctx.pipeline().remove(this@SocksServerConnectHandler)
                            outboundChannel.pipeline().addLast(RelayHandler(ctx.channel()))
                            ctx.pipeline().addLast(RelayHandler(outboundChannel))
                        }
                    } else {
                        ctx.channel().writeAndFlush(DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, msg.dstAddrType()))
                        SocksServerUtils.closeOnFlush(ctx.channel());
                    }

                }
            })
            val inboundChannel = ctx.channel()
            b.group(inboundChannel.eventLoop())
                    .channel(NioSocketChannel::class.java)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                    .handler(DirectClientHandler(promise))
            b.connect(msg.dstAddr(), msg.dstPort()).addListener { future ->
                if (!future.isSuccess) {
                    ctx.channel().writeAndFlush(DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, msg.dstAddrType()))
                    SocksServerUtils.closeOnFlush(ctx.channel())
                }
            }
        } else {
            ctx.close()
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        SocksServerUtils.closeOnFlush(ctx.channel())
    }
}

class DirectClientHandler(val promise: Promise<Channel>) : ChannelInboundHandlerAdapter() {
    override fun channelActive(ctx: ChannelHandlerContext) {
        ctx.pipeline().remove(this)
        promise.setSuccess(ctx.channel())
    }

    override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) {
        super.channelRead(ctx, msg)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        promise.setFailure(cause)
    }
}

class RelayHandler(val channel: Channel) : ChannelInboundHandlerAdapter() {
    override fun channelActive(ctx: ChannelHandlerContext) {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER)
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (channel.isActive)
            channel.writeAndFlush(msg)
        else
            ReferenceCountUtil.release(msg)
    }

    override fun channelInactive(ctx: ChannelHandlerContext?) {
        if (channel.isActive)
            SocksServerUtils.closeOnFlush(channel)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }
}
