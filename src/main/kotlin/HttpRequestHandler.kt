import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.SimpleChannelInboundHandler
import java.net.HttpURLConnection
import java.net.URL

/**
 * Created by weijian on 16-7-29.
 */
class HttpRequestHandler : SimpleChannelInboundHandler<URL>() {
    override fun channelRead0(ctx: ChannelHandlerContext, url: URL) {

    }
}
