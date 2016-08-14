package com.weijian.shadowsocks.cipher;

import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by weijian on 16-8-11.
 */
public class TableCipher implements Cipher {
    private static class ByteWrapper {
        int index;
        byte b;
    }

    private static class TransformTable {
        public TransformTable(byte[] encryptTable, byte[] decryptTable) {
            this.encryptTable = encryptTable;
            this.decryptTable = decryptTable;
        }

        byte[] encryptTable;
        byte[] decryptTable;
    }

    private static Map<byte[], TransformTable> cache_tables = new TreeMap<>((o1, o2) -> {
        if (o1.length > o2.length)
            return 1;
        else if (o1.length < o2.length)
            return -1;
        int count = o1.length;
        for (int i = 0; i < count; i++) {
            if (o1[i] > o2[i])
                return 1;
            else if (o1[i] < o2[i])
                return -1;
        }
        return 0;
    });
    private int mode;
    private String algorithm;
    private byte[] key;
    private byte[] iv;
    private int keySize;
    private int ivSize;
    private byte[] encryptTable;
    private byte[] decryptTable;

    void initCipher(String algorithm, int keySize, int ivSize) {
        this.algorithm = algorithm;
        this.keySize = keySize;
        this.ivSize = ivSize;
    }

    @Override
    public int getMode() {
        return this.mode;
    }

    @Override
    public String getAlgorithm() {
        return this.algorithm;
    }

    @Override
    public byte[] getKey() {
        return Arrays.copyOf(key, key.length);
    }

    @Override
    public byte[] getIv() {
        return Arrays.copyOf(iv, iv.length);
    }

    private static byte[] getTable(byte[] key) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        assert digest != null;
        ByteBuffer byteBuffer = ByteBuffer.wrap(digest.digest(key));
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        final long a = byteBuffer.getLong();
        ByteWrapper[] bb = new ByteWrapper[256];
        for (int i = 0; i < bb.length; i++) {
            bb[i] = new ByteWrapper();
            bb[i].b = (byte) i;
        }
        for (int i = 1; i < 1024; i++) {
            for (ByteWrapper w : bb) {
                w.index = (int) Long.remainderUnsigned(a, (w.b & 0xFF) + i);
            }
            Arrays.sort(bb, (o1, o2) -> o1.index > o2.index ? 1 : o1.index == o2.index ? 0 : -1);
        }
        byte[] table = new byte[bb.length];
        for (int i = 0; i < table.length; i++) {
            table[i] = bb[i].b;
        }
        return table;
    }

    @Override
    public void init(int mode, byte[] key, byte[] iv) throws Exception {
        this.mode = mode;
        this.iv = iv;
        this.key = key;
        TransformTable table = cache_tables.get(key);
        if (table == null) {
            byte[] encryptTable = getTable(key);
            byte[] decryptTable = new byte[encryptTable.length];
            for (int i = 0; i < decryptTable.length; i++) {
                decryptTable[encryptTable[i] & 0xff] = (byte) i;
            }
            table = new TransformTable(encryptTable, decryptTable);
            cache_tables.put(key, table);
        }
        this.encryptTable = table.encryptTable;
        this.decryptTable = table.decryptTable;
    }

    private static byte[] translate(byte[] data, byte[] table, boolean copy) {
        byte[] result;
        if (copy)
            result = Arrays.copyOf(data, data.length);
        else
            result = data;
        for (int i = 0; i < data.length; i++) {
            result[i] = table[result[i] & 0xFF];
        }
        return result;
    }

    @Override
    public byte[] update(byte[] data) {
        return update(data, true);
    }

    private byte[] update(byte[] data, boolean copy) {
        if (mode == Cipher.DECRYPT) {
            return translate(data, decryptTable, copy);
        } else {
            return translate(data, encryptTable, copy);
        }
    }

    @Override
    public void update(ByteBuffer src, ByteBuffer dst) {
        throw new UnsupportedOperationException("not implements");
    }

    @Override
    public void update(ByteBuf src, ByteBuf dst) {
        byte[] b = new byte[src.readableBytes()];
        src.readBytes(b);
        update(b, false);
        dst.writeBytes(b);
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
        throw new UnsupportedOperationException("not implements");
    }
}
