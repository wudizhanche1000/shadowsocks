package com.weijian.shadowsocks

import com.weijian.shadowsocks.cipher.CipherFactory

/**
 * Created by weijian on 16-8-12.
 */
object Context {
    var isServerMode: Boolean = false
        get
    lateinit var configuration: Configuration
    val cipherInfo: CipherFactory.CipherInfo by lazy {
        CipherFactory.getCipherInfo(configuration.method)
    }
        get
    val isDebug: Boolean by lazy {
        configuration.debug
    }
        get
}
