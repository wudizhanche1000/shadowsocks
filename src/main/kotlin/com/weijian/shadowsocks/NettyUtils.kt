@file:JvmName("NettyUtils")

package com.weijian.shadowsocks

import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener


/**
 * Created by weijian on 16-8-10.
 */
fun closeOnFlush(ch: Channel) {
    if (ch.isActive)
        ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
}