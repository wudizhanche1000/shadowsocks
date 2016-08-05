package com.weijian.shadowsocks

import java.lang.annotation.Native
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Created by weijian on 16-8-3.
 */
fun evpBytesToKey(password: String, keyLen: Int): ByteArray {
    val md5Len = 16
    val cnt = (keyLen - 1) / md5Len + 1
    val passwordBytes = password.toByteArray()
    val m = ByteArray(cnt * md5Len)
    val digest = MessageDigest.getInstance("MD5")
    System.arraycopy(digest.digest(passwordBytes), 0, m, 0, md5Len)
    val d = ByteArray(md5Len + password.length)
    var start = 0
    for (i in 1 until cnt) {
        start += md5Len
        System.arraycopy(m, start - md5Len, d, 0, md5Len)
        System.arraycopy(passwordBytes, 0, d, md5Len, password.length)
        System.arraycopy(digest.digest(d), 0, m, start, md5Len)
    }
    return m.sliceArray(0 until keyLen)
}

fun main(args: Array<String>) {
    val key = evpBytesToKey("barfoo!", 16)
    val cipher = Cipher.getInstance("AES/CFB/NoPadding")
    val keySpec = SecretKeySpec(key, "AES")
    val ivSpec = IvParameterSpec(key)

}
