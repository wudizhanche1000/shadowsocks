package com.weijian.shadowsocks

import com.weijian.shadowsocks.cipher.CipherFactory

/**
 * Created by weijian on 16-8-12.
 */
object Context {
    lateinit var configuration: Configuration
        get
    val cipherInfo: CipherFactory.CipherInfo by lazy {
        CipherFactory.getCipherInfo(configuration.method)
    }
    val isDebug: Boolean by lazy {
        configuration.debug
    }
        get
}
