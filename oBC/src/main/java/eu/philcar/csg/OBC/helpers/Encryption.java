package eu.philcar.csg.OBC.helpers;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import android.os.Build;
import android.util.Base64;

public class Encryption {
    //XvSMJG6YGV
    private static String _charsetName = "UTF8";
    private static String _algorithm = "DES";
    private static int _base64Mode = Base64.DEFAULT;
    
    private static String _cachedKey ="ew3J8M4aGBZhIMMLkPJgUAdKPduqiYxezZhJymNgNkmS3NlXhzO1FYT0o6ONRL4";
    

    public static String getCharsetName() {
        return _charsetName;
    }

    public static void setCharsetName(String charsetName) {
       _charsetName = charsetName;
    }

    public static String getAlgorithm() {
        return _algorithm;
    }

    public static void setAlgorithm(String algorithm) {
        _algorithm = algorithm;
    }

    public int getBase64Mode() {
        return _base64Mode;
    }

    public static void setCachedKey(String key) {
    	_cachedKey = key;
    }
    
    public static void setBase64Mode(int base64Mode) {
        _base64Mode = base64Mode;
    }

    public static String encrypt( String data) {
    	String key = _cachedKey;
        if (key == null || data == null)
            return null;
        try {
            DESKeySpec desKeySpec = new DESKeySpec(key.getBytes(_charsetName));
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(_algorithm);
            SecretKey secretKey = secretKeyFactory.generateSecret(desKeySpec);
            byte[] dataBytes = data.getBytes(_charsetName);
            Cipher cipher = Cipher.getInstance(_algorithm);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.encodeToString(cipher.doFinal(dataBytes), _base64Mode);
        } catch (Exception e) {
            return null;
        }
    }

    public static String decrypt(String data) {
    	String key = _cachedKey;
        if (key == null || data == null)
            return null;
        try {
            byte[] dataBytes = Base64.decode(data, _base64Mode);
            DESKeySpec desKeySpec = new DESKeySpec(key.getBytes());
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(_algorithm);
            SecretKey secretKey = secretKeyFactory.generateSecret(desKeySpec);
            Cipher cipher = Cipher.getInstance(_algorithm);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] dataBytesDecrypted = (cipher.doFinal(dataBytes));
            return new String(dataBytesDecrypted);
        } catch (Exception e) {
            return null;
        }
    }
    
    
    public static String decryptAes(String data) {
    	try {
	    	byte[] cipherText =  Base64.decode(data, _base64Mode);
	        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS7Padding");
	        MessageDigest md = MessageDigest.getInstance("MD5");
	        byte[] thedigest = md.digest(_cachedKey.getBytes("UTF-8"));
	        SecretKeySpec key = new SecretKeySpec(thedigest, "AES/ECB/PKCS5Padding");
	        cipher.init(Cipher.DECRYPT_MODE, key);
	        return new String(cipher.doFinal(cipherText),"UTF-8");
        } catch (Exception e) {
              return null;
        }
      }
    
    public static final String md5(final String s) {
        final String MD5 = "MD5";
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest
                    .getInstance(MD5);
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
}