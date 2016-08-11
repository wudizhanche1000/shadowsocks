/**
 * Created by weijian on 16-8-11.
 */
@file:JvmName("CipherTestUtils")

import com.weijian.shadowsocks.cipher.Cipher
import java.util.*

fun runCipher(encryptCipher: Cipher, decryptCipher: Cipher) {
    val BLOCK_SIZE = 16384
    val rounds = 1 * 1024
    val random = Random()
    val plain = ByteArray(BLOCK_SIZE * rounds)
    random.nextBytes(plain)
    println("Start cipher test...")
    val startTime = System.currentTimeMillis();
    var pos = 0
    val c = encryptCipher.update(plain);
    val r = decryptCipher.update(c);

    while (pos < plain.size) {
        var l = 100 + random.nextInt(32769 - 100)
        if (pos + l >= plain.size) {
            l = plain.size - pos;
        }
        val t = Arrays.copyOfRange(plain, pos, pos + l);
        val c = encryptCipher.update(t)
        val r = decryptCipher.update(c);
        for (i in 0 until r.size) {
            assert(t[i] == r[i])
        }
        pos += l
    }
    val endTime = System.currentTimeMillis();
    println("speed: ${BLOCK_SIZE * rounds / ((endTime - startTime) / 1000.0)}")
    for (i in 0 until r.size) {
        assert(r[i] == plain[i])
    }
}