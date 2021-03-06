package com.weijian.shadowsocks.cipher;

import java.lang.reflect.Constructor;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by weijian on 16-8-9.
 */
public class CipherFactory {
    private static Map<String, CipherInfo> ciphers;

    public static class CipherInfo {
        private int keySize;
        private int ivSize;

        Class<? extends Cipher> getCipherClass() {
            return cipherClass;
        }

        private Class<? extends Cipher> cipherClass;

        CipherInfo(int keySize, int ivSize, Class<? extends Cipher> cipherClass) {
            this.keySize = keySize;
            this.ivSize = ivSize;
            this.cipherClass = cipherClass;
        }

        public int getKeySize() {
            return keySize;
        }

        public int getIvSize() {
            return ivSize;
        }

    }

    static {
        ciphers = new HashMap<>();
        ciphers.put("aes-128-cfb", new CipherInfo(16, 16, OpensslCipher.class));
        ciphers.put("aes-192-cfb", new CipherInfo(24, 16, OpensslCipher.class));
        ciphers.put("aes-256-cfb", new CipherInfo(32, 16, OpensslCipher.class));
        ciphers.put("table", new CipherInfo(0, 0, TableCipher.class));
    }

    public static CipherInfo getCipherInfo(String algorithm) {
        return ciphers.get(algorithm);
    }

    public synchronized static Cipher getCipher(String algorithm) throws Exception {
        CipherInfo info = ciphers.get(algorithm);
        if (info == null) {
            throw new NoSuchAlgorithmException("cipher " + algorithm + " not found");
        }
        Class<? extends Cipher> cipherClass = info.getCipherClass();
        Constructor<? extends Cipher> constructor = cipherClass.getDeclaredConstructor(String.class, int.class, int.class);
        return constructor.newInstance(algorithm, info.getKeySize(), info.getIvSize());
    }
}
