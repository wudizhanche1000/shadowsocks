@file:JvmName("Utils")

package com.weijian.shadowsocks

import java.util.*

/**
 * Created by weijian on 16-8-16.
 */
class ByteComparableMap<T> : TreeMap<ByteArray, T>(object : Comparator<ByteArray> {
    override fun compare(o1: ByteArray, o2: ByteArray): Int {
        if (o1.size > o2.size)
            return 1;
        else if (o1.size < o2.size)
            return -1;
        for (i in 0 until o1.size) {
            if (o1[i] < o2[i])
                return -1;
            else if (o1[i] > o2[i])
                return 1;
        }
        return 0;
    }

}) {
    override fun get(key: ByteArray): T? {
        return super.get(key)
    }
}

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