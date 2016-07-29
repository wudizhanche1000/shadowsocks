import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.util.ByteProcessor
import java.io.ByteArrayOutputStream

/**
 * Created by weijian on 16-7-29.
 */
class SimpleRequestDecoder : ByteToMessageDecoder() {

    override fun decode(ctx: ChannelHandlerContext, inBuf: ByteBuf, out: MutableList<Any>) {
        if (inBuf.readableBytes() >= 4) {
            val index = inBuf.readerIndex() + inBuf.readableBytes() - 1;
            var LF = true
            for (i in index downTo index - 3) {
                if (inBuf.getByte(i).toChar() != if (LF) '\n' else '\r') {
                    return;
                }
                LF = !LF
            }
            val array: ByteArray
            if (inBuf.hasArray()) {
                array = inBuf.array()
            } else {
                array = ByteArray(inBuf.readableBytes() - inBuf.readerIndex())
                inBuf.readBytes(array)
            }
            out.add(String(array))
        }

    }
}
