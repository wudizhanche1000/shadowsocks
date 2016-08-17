package com.weijian.shadowsocks;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Created by weijian on 16-8-15.
 */
public class OneTimeAuthHandler extends ByteToMessageDecoder {

    private static final Logger logger = LogManager.getLogger();

    private int counter = 0;
    private int dataLength = 0;
    private final Mac macDigest;
    private final byte[] iv;
    private byte[] key;
    private byte[] result = new byte[20];
    private byte[] hash = new byte[10];

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(cause.getMessage());
        NettyUtils.closeOnFlush(ctx.channel());
    }

    /***
     *  Initialize new OneTimeAuthHandler
     *
     * @param macDigest Mac could init multi times, so we don't need a new one.
     * @param iv iv of this request
     * @throws NoSuchAlgorithmException
     */
    public OneTimeAuthHandler(Mac macDigest, byte[] iv) throws NoSuchAlgorithmException {
        this.iv = iv;
        this.key = new byte[iv.length + 4];
        if (iv.length > 0)
            System.arraycopy(iv, 0, key, 0, iv.length);
        macDigest.reset();
        this.macDigest = macDigest;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (dataLength == 0) {
            if (in.readableBytes() < 2) {
                return;
            }
            dataLength = in.readShort() & 0xFFFF;
        }
        if (in.readableBytes() < 10 + dataLength)
            return;
        in.readBytes(hash);
        byte[] buffer = new byte[dataLength];
        in.readBytes(buffer);
        Utils.writeInt(key, iv.length, counter++);
        SecretKeySpec keySpec = new SecretKeySpec(key, InitRequestHandler.ONE_AUTH_ALGORITHM);
        macDigest.reset();
        macDigest.init(keySpec);
        macDigest.update(buffer, 0, dataLength);
        macDigest.doFinal(result, 0);
        if (!Utils.verifyHmac(hash, 0, result, 0, 10)) {
            logger.error("one time auth failed");
            NettyUtils.closeOnFlush(ctx.channel());
            return;
        }
        out.add(Unpooled.wrappedBuffer(buffer));
        dataLength = 0;

    }
}
