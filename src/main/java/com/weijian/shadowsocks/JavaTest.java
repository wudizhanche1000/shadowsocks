package com.weijian.shadowsocks;

import com.weijian.shadowsocks.cipher.Cipher;
import com.weijian.shadowsocks.cipher.CipherFactory;

import javax.crypto.spec.SecretKeySpec;

/**
 * Created by weijian on 16-8-4.
 */
public class JavaTest {
    public static void main(String[] args) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec("1234567890123456".getBytes(), "AES");
        byte[] iv = "1234567890123456".getBytes();

//        for (byte b : data) {
//////            byte[] output = decryptCipher.doFinal(new byte[]{b});
////            System.out.println(new String(output));
//        }

//        e = null;
//        System.gc();
//        while (true) {
//
//        }
        while (true) {
            Cipher e = CipherFactory.getCipher("aes-128-cfb");
            e.init(Cipher.DECRYPT, keySpec.getEncoded(), iv);
            byte[] b = e.update("123456789".getBytes());
//            for (byte i : b) {
//                System.out.printf("%02x ", i);
//            }
//            System.out.println();
        }
//        e.update(null);
    }
}
