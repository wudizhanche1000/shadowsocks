package com.weijian.shadowsocks.cipher;

import java.nio.ByteBuffer;

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

    void initCipher(String algorithm, int keySize, int ivSize) {
        this.keySize = keySize;
        this.iVSize = ivSize;
        this.algorithm = algorithm;
        this.pCipher = getCipher(algorithm);
    }

    private native byte[] update(long p, byte[] data);

    @Override
    public void init(int mode, byte[] key, byte[] iv) throws Exception {
        if (keySize != key.length) {
            throw new Exception("Key size should be " + keySize);
        }
        if (iVSize != iv.length) {
            throw new Exception("Iv size should be " + iVSize);
        }
        if (this.pCipher == 0) {
            throw new Exception("Cipher invalid");
        }
        pContext = init(mode, pCipher, key, iv);
        if (pContext == 0) {
            throw new Exception("Cipher initialize failed");
        }
    }

    @Override
    public byte[] update(byte[] data) {
        return update(pContext, data);
    }

    @Override
    public void update(ByteBuffer src, ByteBuffer dst) {
        throw new UnsupportedOperationException("Not Implement");
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
    public byte[] getKey() {
        return key;
    }

    @Override
    public byte[] getIv() {
        return iv;
    }


    @Override
    public String getAlgorithm() {
        return algorithm;
    }


}
