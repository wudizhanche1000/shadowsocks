package com.weijian.shadowsocks;

import com.weijian.shadowsocks.cipher.CipherFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;

import static com.weijian.shadowsocks.Constants.*;

/**
 * Created by weijian on 16-8-15.
 */
public class OneTimeAuthHandler extends ChannelInboundHandlerAdapter {

    public static final String NAME = "OneTimeAuthHandler";
    public static final int HMAC_LENGTH = 10;
    public static final String ONE_AUTH_ALGORITHM = "HmacSHA1";
    public static final CipherFactory.CipherInfo cipherInfo = Context.INSTANCE.getCipherInfo();
    private static final Logger logger = LogManager.getLogger();

    private int counter = 0;
    private int dataLength = 0;
    private final Mac macDigest;
    private final byte[] iv;
    private byte[] macKey;
    private byte[] result = new byte[20];
    private byte[] hash = new byte[HMAC_LENGTH];
    private boolean init = true;
    private byte type = 0;
    private CompositeByteBuf buffer;
    private CompositeByteBuf dataBuffer;
    private final byte[] key;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        assert msg instanceof ByteBuf;
        if (buffer == null) {
            buffer = ctx.alloc().compositeDirectBuffer();
            dataBuffer = ctx.alloc().compositeDirectBuffer();
        }
        ByteBuf request = (ByteBuf) msg;
        if (buffer.isReadable()) {
            buffer.addComponent(true, request);
            request = buffer;
        }

        if (init) {
            if (dataLength == 0) {
                if (type == 0) {
                    request.markReaderIndex();
                    type = request.readByte();
                    if (Context.configuration.getAuth() && (type & 0x10) != 0x10) {
                        logger.error("server insist one time auth");
                        ctx.close();
                        return;
                    }
                    byte[] initKey = new byte[iv.length + key.length];
                    if (initKey.length > 0) {
                        System.arraycopy(iv, 0, initKey, 0, iv.length);
                    }
                    System.arraycopy(key, 0, initKey, iv.length, key.length);
                    SecretKeySpec keySpec;
                    // initKey长度为0时需要使用特殊的构造方式
                    if (cipherInfo.getIvSize() + cipherInfo.getKeySize() == 0) {
                        keySpec = new SecretKeySpec(initKey, initKey.length, 0, ONE_AUTH_ALGORITHM);
                    } else {
                        keySpec = new SecretKeySpec(initKey, ONE_AUTH_ALGORITHM);
                    }
                    macDigest.init(keySpec);
                }
                switch (type & 0x0F) {
                    case ADDRESS_IPV4:
                        dataLength = ADDRESS_IPV4_LEN + 2 + 1;
                        break;
                    case ADDRESS_IPV6:
                        dataLength = ADDRESS_IPV6_LEN + 2 + 1;
                        break;
                    case ADDRESS_HOSTNAME:
                        if (request.isReadable()) {
                            int length = request.readByte();
                            dataLength = 1 + 1 + length + 2;
                        } else {
                            ctx.read();
                            return;
                        }
                }
            }
            request.resetReaderIndex();
            if (request.isReadable(dataLength + HMAC_LENGTH)) {
                byte[] header = new byte[dataLength];
                request.readBytes(header);
                macDigest.update(header);
                macDigest.doFinal(result, 0);
                request.readBytes(hash);
                if (!Utils.verifyHmac(result, 0, hash, 0, HMAC_LENGTH)) {
                    logger.error("one time auth failed");
                    ctx.close();
                    return;
                }
                request.resetReaderIndex();
                dataBuffer.addComponent(true, request.slice(request.readerIndex(), dataLength).retain());
                request.skipBytes(dataLength + HMAC_LENGTH);
                init = false;
                dataLength = 0;
                macDigest.reset();
            } else {
                if (request.isReadable() && request != buffer) {
                    buffer.addComponent(true, request);
                }
                ctx.read();
            }
        }
        while (request.isReadable()) {
            if (dataLength == 0 && request.isReadable(2)) {
                dataLength = request.readShort() & 0xFFFF;
            } else {
                break;
            }
            if (!request.isReadable(dataLength + HMAC_LENGTH)) {
                break;
            }
            request.readBytes(hash);
            byte[] t = new byte[dataLength];
            request.markReaderIndex();
            request.readBytes(t);
            macDigest.reset();
            Utils.writeInt(macKey, iv.length, counter++);
            macDigest.init(new SecretKeySpec(macKey, ONE_AUTH_ALGORITHM));
            macDigest.update(t);
            macDigest.doFinal(result, 0);
            if (!Utils.verifyHmac(hash, 0, result, 0, HMAC_LENGTH)) {
                logger.error("one time auth failed");
                ctx.close();
                return;
            }
            request.resetReaderIndex();
            buffer.addComponent(true, request.slice(request.readerIndex(), dataLength).retain());
            request.skipBytes(dataLength);
            dataLength = 0;

        }
        if (request.isReadable() && request != buffer) {
            buffer.addComponent(true, request);
        }
        ctx.read();
        if (dataBuffer.isReadable())
            ctx.fireChannelRead(dataBuffer.retain());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(cause.getMessage());
        NettyUtils.closeOnFlush(ctx.channel());
    }

    /***
     *  Initialize new OneTimeAuthHandler
     *
     * @param iv iv of this request
     * @param key key, 加密算法可能会对key做处理
     * @throws NoSuchAlgorithmException
     */
    public OneTimeAuthHandler(byte[] iv, byte[] key) throws NoSuchAlgorithmException {
        this.iv = iv;
        this.key = key;
        this.macKey = new byte[iv.length + 4];
        if (iv.length > 0)
            System.arraycopy(iv, 0, macKey, 0, iv.length);
        macDigest = Mac.getInstance(ONE_AUTH_ALGORITHM);
    }


    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (buffer != null) {
            buffer.release();
        }
        if (dataBuffer != null) {
            dataBuffer.release();
        }
    }
}
