package com.weijian.shadowsocks.cipher;


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

    byte[] doFinal(byte[] data);

    void doFinal(ByteBuffer src, ByteBuffer dst);

}
