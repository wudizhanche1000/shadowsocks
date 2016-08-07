package com.weijian.shadowsocks.encryption;

/**
 * Created by weijian on 16-8-3.
 */

public interface Encryption {
    byte[] update(byte[] data);
}
