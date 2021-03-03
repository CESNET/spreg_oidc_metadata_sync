package cz.muni.ics.oidc;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

public class Utils {

    public static String decrypt(String strToDecrypt, Cipher cipher, SecretKeySpec secretKeySpec)
            throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException
    {
        if (strToDecrypt == null || strToDecrypt.equalsIgnoreCase("null")) {
            return null;
        }

        Base64.Decoder b64dec = Base64.getUrlDecoder();
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        return new String(cipher.doFinal(b64dec.decode(strToDecrypt)));
    }

    public static String encrypt(String strToEncrypt, Cipher cipher, SecretKeySpec secretKeySpec)
            throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException
    {
        if (strToEncrypt == null || strToEncrypt.equalsIgnoreCase("null")) {
            return null;
        }
        Base64.Encoder b64enc = Base64.getUrlEncoder();
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
        return b64enc.encodeToString(cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8)));
    }

    public static SecretKeySpec generateSecretKeySpec(String secret) throws NoSuchAlgorithmException {
        secret = fixSecret(secret);
        MessageDigest sha;
        byte[] key = secret.getBytes(StandardCharsets.UTF_8);
        sha = MessageDigest.getInstance("SHA-1");
        key = sha.digest(key);
        key = Arrays.copyOf(key, 16);
        return new SecretKeySpec(key, "AES");
    }

    public static String fixSecret(String s) {
        if (s.length() < 32) {
            int missingLength = 32 - s.length();
            StringBuilder sBuilder = new StringBuilder(s);
            for (int i = 0; i < missingLength; i++) {
                sBuilder.append('A');
            }
            s = sBuilder.toString();
        }
        return s.substring(0, 32);
    }

}
