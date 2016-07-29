import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler

/**
 * Created by weijian on 16-7-29.
 */
class SimpleTransformServer : ChannelInboundHandlerAdapter() {

}

fun main(args: Array<String>) {
    val b = ServerBootstrap()
    val bossGroup = NioEventLoopGroup(1)
    val workerGroup = NioEventLoopGroup()
    b.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .handler(LoggingHandler(LogLevel.INFO))
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().addLast(LoggingHandler(LogLevel.INFO), SimpleRequestDecoder(), SimpleServerHandler())
                }
            })
    b.bind(1234).sync().channel().closeFuture().sync()
}
