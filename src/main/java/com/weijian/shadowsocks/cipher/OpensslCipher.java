package com.weijian.shadowsocks.cipher;

import com.weijian.shadowsocks.EncryptionUtils;
import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by weijian on 16-8-7.
 */
public class OpensslCipher implements Cipher {
    static {
        System.loadLibrary("openssl-crypto");
    }


    private byte[] key;
    private byte[] iv;
    private int keySize;
    private int iVSize;
    private String algorithm;
    private int mode;
    private long pContext = 0;
    private long pCipher = 0;

    public OpensslCipher() {
    }

    private static native long getCipher(String algorithm);

    @Override
    public int getMode() {
        return mode;
    }

    private native long init(int mode, long pCipher, byte[] key, byte[] iv);

    private native void destroy(long p);

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        this.destroy(pContext);
    }

    OpensslCipher(String algorithm, int keySize, int ivSize) {
        this.keySize = keySize;
        this.iVSize = ivSize;
        this.algorithm = algorithm;
        this.pCipher = getCipher(algorithm);
    }

    private native byte[] update(long p, byte[] data);

    @Override
    public void init(int mode, byte[] key, byte[] iv) throws Exception {
        if (iVSize != iv.length) {
            throw new Exception("Iv size should be " + iVSize);
        }
        if (this.pCipher == 0) {
            throw new Exception("Cipher invalid");
        }
        this.key = EncryptionUtils.evpBytesToKey(key, keySize);
        this.iv = iv;
        pContext = init(mode, pCipher, this.key, iv);
        if (pContext == 0) {
            throw new Exception("Cipher initialize failed");
        }
    }

    @Override
    public byte[] update(byte[] data) {
        return update(pContext, data);
    }

    @Override
    public byte[] update(byte[] data, int offset, int length) {
        throw new UnsupportedOperationException("Not Implement");
    }

    @Override
    public void update(ByteBuffer src, ByteBuffer dst) {
        throw new UnsupportedOperationException("Not Implement");
    }

    @Override
    public void update(ByteBuf src, ByteBuf dst) {
        byte[] data = new byte[src.readableBytes()];
        src.readBytes(data);
        dst.writeBytes(update(pContext, data));
    }

    @Override
    public byte[] doFinal(byte[] data) {
        throw new UnsupportedOperationException("Not Implement");
    }

    @Override
    public void doFinal(ByteBuffer src, ByteBuffer dst) {
        throw new UnsupportedOperationException("Not Implement");
    }

    @Override
    public void doFinal(ByteBuf src, ByteBuf dst) {
        throw new UnsupportedOperationException("not implement");
    }

    @Override
    public byte[] getKey() {
        return Arrays.copyOf(key, key.length);
    }

    @Override
    public byte[] getIv() {
        return Arrays.copyOf(iv, iv.length);
    }


    @Override
    public String getAlgorithm() {
        return algorithm;
    }


}
