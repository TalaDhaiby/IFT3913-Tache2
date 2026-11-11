
package org.apache.tika.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SemgrepValidation {

    public static byte[] insecureHash(byte[] input)
            throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("MD5").digest(input);
    }
}
