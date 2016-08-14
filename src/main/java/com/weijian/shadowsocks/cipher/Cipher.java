package com.weijian.shadowsocks.cipher;


import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;

/**
 * Created by weijian on 16-8-3.
 */

public interface Cipher {

    int DECRYPT = 0;
    int ENCRYPT = 1;

    int getMode();

    String getAlgorithm();

    byte[] getKey();

    byte[] getIv();

    void init(int mode, byte[] key, byte[] iv) throws Exception;

    byte[] update(byte[] data);

    void update(ByteBuffer src, ByteBuffer dst);

    void update(ByteBuf src, ByteBuf dst);

    byte[] doFinal(byte[] data);

    void doFinal(ByteBuffer src, ByteBuffer dst);

    void doFinal(ByteBuf src, ByteBuf dst);

}
