package com.weijian.shadowsocks

/**
 * Created by weijian on 16-8-5.
 */
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream
import org.apache.commons.crypto.cipher.CryptoCipher
import org.apache.commons.crypto.cipher.CryptoCipherFactory
import org.apache.commons.crypto.utils.Utils
import org.apache.logging.log4j.LogManager
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.*
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class Handler constructor(val conn: Socket) : Thread() {
    private var init = true
    private var cipher: Cipher = Cipher.getInstance("AES/CFB128/NoPadding")

    private var remoteHandler: RemoteHandler? = null

    companion object {
        val keySpec = SecretKeySpec(evpBytesToKey("barfoo!", 16), "AES")
    }

    private lateinit var remoteConn: Socket
    override fun run() {
        val channel = conn.channel
        val buffer = ByteBuffer.allocateDirect(4096)
        val decryptBuffer = ByteBuffer.allocate(4096)
        decryptBuffer.order(ByteOrder.BIG_ENDIAN)
        while (true) {
            if (this.isInterrupted) {
                conn.close()
                return
            }

            try {
                if (channel.read(buffer) == -1) {
                    conn.close()
                    remoteHandler?.interrupt()
                    return
                }
            } catch (e: ClosedByInterruptException) {
                conn.close()
                return
            }
            buffer.flip()
            var temp = ByteArray(buffer.remaining())
            buffer.get(temp)
            buffer.flip()
            buffer.put(temp)
            buffer.flip()
            if (init) {
                val iv = ByteArray(16)
                buffer.get(iv)
                val ivSpec = IvParameterSpec(iv);
                cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            }
            cipher.doFinal(buffer, decryptBuffer)
            decryptBuffer.flip()
            if (init) {
                val type = decryptBuffer.get().toInt() and 0xFF
                val ip = ByteArray(4)
                decryptBuffer.get(ip)
                val host = InetAddress.getByAddress(ip)
                val port = decryptBuffer.short.toInt() and 0xFFFF

                println("$type ${host.hostAddress}:$port")
                val socketChannel = SocketChannel.open(InetSocketAddress(host, port))
                remoteConn = socketChannel.socket()
                val cc = CryptoCipherFactory.getCryptoCipher("AES/CFB/NoPadding")
                val encrypt_cipher = Cipher.getInstance("AES/CFB128/NoPadding")
                val encrypt_iv = ByteArray(16)
                Random().nextBytes(encrypt_iv)
                cc.init(Cipher.ENCRYPT_MODE, keySpec, IvParameterSpec(encrypt_iv))
                encrypt_cipher.init(Cipher.ENCRYPT_MODE, keySpec, IvParameterSpec(encrypt_iv))
                remoteHandler = RemoteHandler(conn, remoteConn, cc, encrypt_iv, this)
                remoteHandler!!.start()
                init = false
            }
            remoteConn.channel.write(decryptBuffer)
            buffer.clear()
            decryptBuffer.clear()
        }
    }
}

class RemoteHandler(val localConn: Socket, val remoteConn: Socket, val cipher: CryptoCipher, val iv: ByteArray, val handler: Handler) : Thread() {
    private var init = true
    override fun run() {
        val buffer = ByteBuffer.allocateDirect(1024)
        val encryptBuffer = ByteBuffer.allocate(2048)
        while (true) {
            if (this.isInterrupted) {
                remoteConn.close()
                return
            }
            try {
                if (remoteConn.channel.read(buffer) == -1) {
                    cipher.doFinal(ByteBuffer.allocate(0),encryptBuffer)
                    encryptBuffer.flip()
//                    val last = cipher.doFinal()
                    println("doFinal ${encryptBuffer.remaining()}")
                    localConn.channel.write(encryptBuffer)
                    remoteConn.close()
                    handler.interrupt()
                    return
                }
            } catch (e: ClosedByInterruptException) {
                remoteConn.close()
                return
            }
            buffer.flip()
//            println("before encrypt ${buffer.remaining()}")
            cipher.update(buffer, encryptBuffer)
//            cipher.doFinal(buffer, encryptBuffer)
            encryptBuffer.flip()
//            println("after encrypt ${encryptBuffer.remaining()}")
            if (init) {
                val firstBuffer = ByteBuffer.allocate(16 + encryptBuffer.remaining())
                firstBuffer.put(iv)
                firstBuffer.put(encryptBuffer)
                firstBuffer.flip()
                localConn.channel.write(firstBuffer)
                init = false
            } else {
                localConn.channel.write(encryptBuffer)
            }
            encryptBuffer.clear()
            buffer.clear()
        }
    }

}

var logger = LogManager.getLogger("test")
fun main(args: Array<String>) {
    val selector = Selector.open()
    val serverSocketChannel = ServerSocketChannel.open()
    val socket = serverSocketChannel.socket()
    socket.bind(InetSocketAddress(8388))
    while (true) {
        val conn = socket.accept()
        logger.warn("new connection established ${conn.remoteSocketAddress}")
        val handler = Handler(conn)
        handler.start()
    }
}
