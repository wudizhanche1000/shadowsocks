@file:JvmName("Utils")

package com.weijian.shadowsocks

/**
 * Created by weijian on 16-8-16.
 */

fun verifyHmac(a: ByteArray, offsetA: Int, b: ByteArray, offsetB: Int, length: Int): Boolean {
    for (i in 0 until length) {
        if (a[i + offsetA] != b[i + offsetB])
            return false;
    }
    return true;
}

/***
 * this method write an integer into indicate array in BIG_ENDIAN
 * @param b destination array
 * @param offset offset of array
 * @param i Int to write
 *
 * @throws IndexOutOfBoundsException
 */
@Throws(IndexOutOfBoundsException::class)
fun writeInt(b: ByteArray, offset: Int, i: Int) {
    if (b.size - offset < 4)
        throw IndexOutOfBoundsException();
    b[offset] = (i shr 24).toByte()
    b[offset + 1] = (i shr 16).toByte()
    b[offset + 2] = (i shr 8).toByte()
    b[offset + 3] = (i and 0xFF).toByte()
}