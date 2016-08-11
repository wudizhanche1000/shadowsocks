@file:JvmName("EncryptionUtils")

package com.weijian.shadowsocks


/**
 * Created by weijian on 16-8-9.
 */

import java.security.MessageDigest

/**
 * Created by weijian on 16-8-3.
 */
fun evpBytesToKey(password: ByteArray, keyLen: Int): ByteArray {
    val md5Len = 16
    val cnt = (keyLen - 1) / md5Len + 1
    val m = ByteArray(cnt * md5Len)
    val digest = MessageDigest.getInstance("MD5")
    System.arraycopy(digest.digest(password), 0, m, 0, md5Len)
    val d = ByteArray(md5Len + password.size)
    var start = 0
    for (i in 1 until cnt) {
        start += md5Len
        System.arraycopy(m, start - md5Len, d, 0, md5Len)
        System.arraycopy(password, 0, d, md5Len, password.size)
        System.arraycopy(digest.digest(d), 0, m, start, md5Len)
    }
    return m.sliceArray(0 until keyLen)
}

