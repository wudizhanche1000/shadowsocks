package com.weijian.shadowsocks.cipher;


import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;

/**
 * Created by weijian on 16-8-3.
 */

/**
 * 加密类的接口
 * 子类需要实现如下的构造函数
 * <p>
 * Cipher(String algorithm, int keySize, int ivSize)
 */
public interface Cipher {

    int DECRYPT = 0;
    int ENCRYPT = 1;

    /**
     * Get encrypt or decrypt mode of this cipher
     *
     * @return return an integer represents cipher mode
     */
    int getMode();

    String getAlgorithm();

    byte[] getKey();

    byte[] getIv();

    void init(int mode, byte[] key, byte[] iv) throws Exception;

    byte[] update(byte[] data);

    byte[] update(byte[] data, int offset, int length);

    void update(ByteBuffer src, ByteBuffer dst);

    void update(ByteBuf src, ByteBuf dst);

    byte[] doFinal(byte[] data);

    void doFinal(ByteBuffer src, ByteBuffer dst);

    void doFinal(ByteBuf src, ByteBuf dst);

}
