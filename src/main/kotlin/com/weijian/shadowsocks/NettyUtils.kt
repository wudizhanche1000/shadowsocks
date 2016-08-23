@file:JvmName("NettyUtils")

package com.weijian.shadowsocks

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import org.apache.logging.log4j.LogManager
import java.net.InetAddress
import java.net.UnknownHostException


/**
 * Created by weijian on 16-8-10.
 */

fun closeOnFlush(ch: Channel) {
    if (ch.isActive)
        ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
}

private val logger = LogManager.getLogger()

@Throws(InterruptedException::class)
@JvmOverloads
fun startServer(server: String, port: Int, childInitializer: ChannelInitializer<out Channel>, options: Map<ChannelOption<out Any>, Any>? = null,
                childOptions: Map<ChannelOption<out Any>, Any>? = null) {
    val bossGroup = NioEventLoopGroup(1)
    val workerGroup = NioEventLoopGroup()
    try {
        val address = InetAddress.getByName(server)
        val b = ServerBootstrap()
        b.group(bossGroup, workerGroup).channel(NioServerSocketChannel::class.java)
                .handler(LoggingHandler(LogLevel.WARN))
                .childHandler(childInitializer)
        if (options != null)
            for ((key, value) in options)
                b.option(key as ChannelOption<Any>, value)

        if (childOptions != null)
            for ((key, value) in childOptions)
                b.childOption(key as ChannelOption<Any>, value)
        b.bind(address, port).sync().channel().closeFuture().sync()
    } catch (e: UnknownHostException) {
        logger.error("Unknown server address {}", server)
        System.exit(1)
    } finally {
        bossGroup.shutdownGracefully()
        workerGroup.shutdownGracefully()
    }
}

