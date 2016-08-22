package com.weijian.shadowsocks

import com.weijian.shadowsocks.cipher.CipherFactory

/**
 * Created by weijian on 16-8-12.
 */
object Context {
    public lateinit var configuration: Configuration
    public val cipherInfo: CipherFactory.CipherInfo by lazy {
        CipherFactory.getCipherInfo(configuration.method)
    }
    public val isDebug: Boolean by lazy {
        configuration.debug
    }
        get
}
