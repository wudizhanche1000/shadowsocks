package com.weijian.shadowsocks.encryption;

/**
 * Created by weijian on 16-8-7.
 */
public class OpensslEncryption implements Encryption {
    @Override
    public native byte[] update(byte[] data);
}
