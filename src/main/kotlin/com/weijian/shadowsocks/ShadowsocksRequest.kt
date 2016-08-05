package com.weijian.shadowsocks

/**
 * Created by weijian on 16-8-4.
 */
data class ShadowsocksRequest(val addressType: Byte, val desAddress: Any, val desPort: Int, val data: ByteArray){
    fun a(){
    }
}
