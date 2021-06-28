/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.utilities;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author tobster
 */
public class Security
{
    public static final int SHA256_SIZE = 32;
    private static final char[] BASE16 = "0123456789ABCDEF".toCharArray();
    private static final byte[] AES_KEY = { (byte)0x0a, (byte)0x68, (byte)0x74, (byte)0x69, (byte)0x98, (byte)0x44, (byte)0x35, (byte)0xa8, (byte)0x8d, (byte)0xbe, (byte)0x6b, (byte)0x7e, (byte)0x9b, (byte)0x1c, (byte)0xef, (byte)0x29 };
    private static final int PASSWORD_HASH_SIZE = 32;
    private static final int PASSWORD_SALT_SIZE = 16;
    private static final int ACTIVATION_CODE_SIZE = 4;
    public static final int ACTIVATION_CODE_LENGTH = ACTIVATION_CODE_SIZE * 2; //Because it's base16 encoded.
    public static final int MIN_PASSWORD_LENGTH = 8;
    
    public static String CreateActivationCode()
    {
        SecureRandom sr = new SecureRandom();

        //Generate a salt.
        byte[] cNewSalt = new byte[ACTIVATION_CODE_SIZE];
        sr.nextBytes(cNewSalt);
        
        return BytesToHexString(cNewSalt);
    }

    public static byte[] CreateRandomHash() throws NoSuchAlgorithmException
    {
        SecureRandom sr = new SecureRandom();
        byte[] cRandomNumber = new byte[SHA256_SIZE];
        sr.nextBytes(cRandomNumber);
        return GetSHA256(cRandomNumber);
    }
    
    public static String CreatePassword(byte[] cEncryptedPassword, String strSalt) throws NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException
    {
        byte[] cSalt = HexStringToBytes(strSalt);
        
        //Decipher the password.
        byte[] cDecryptedPassword = DecryptPassword(cEncryptedPassword);
        byte[] cSaltedPassword = SaltPassword(cDecryptedPassword, cSalt);
        //byte[] cPasswordPlusSalt = DecryptAndSaltPassword(cEncryptedPassword, cSalt);
        
        //Hash the password.
        return BytesToHexString(GetSHA256(cSaltedPassword));
    }
    
    public static String CreateSalt()
    {
        SecureRandom sr = new SecureRandom();
        
        //Generate a salt.
        byte[] cNewSalt = new byte[PASSWORD_SALT_SIZE];
        sr.nextBytes(cNewSalt);
        
        return BytesToHexString(cNewSalt);
    }
    
    public static boolean SlowEquals(byte[] cParam1, byte[] cParam2)
    {
        int lDifference = cParam1.length ^ cParam2.length;
        for(int i = 0; i < cParam1.length && i < cParam2.length; i++)
        {
            lDifference |= cParam1[i] ^ cParam2[i];
        }

        return (cParam1.length == cParam2.length) && (lDifference == 0);
    }
    
    public static boolean ValidatePassword(byte[] cEncryptedPassword, String strSalt, String strHash) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException
    {
        byte[] cPasswordSalt = HexStringToBytes(strSalt);
        byte[] cPasswordHash = HexStringToBytes(strHash);
        
        byte[] cDecryptedPassword = DecryptPassword(cEncryptedPassword);
        byte[] cCheckHash = GetSHA256(SaltPassword(cDecryptedPassword, cPasswordSalt));
        //byte[] cCheckHash = GetSHA256(DecryptAndSaltPassword(cEncryptedPassword, cPasswordSalt));

        //Perform slow equals against stored hash.
        return SlowEquals(cCheckHash, cPasswordHash);
    }
    
    public static String GetEncryptedPassword(String strPassword) throws IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException
    {
        return BytesToHexString(EncryptPassword(strPassword));
    }

    public static String GetDecryptedPassword(String strPassword) throws IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, UnsupportedEncodingException
    {
        return new String(DecryptPassword(HexStringToBytes(strPassword)), "UTF-8");
    }
    
    public static byte[] EncryptPassword(String strPassword) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException
    {
        byte[] cPassword = strPassword.getBytes();
        
        Key key = new SecretKeySpec(AES_KEY, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(cPassword);
    }
    
    public static byte[] DecryptPassword(byte[] cEncryptedPassword) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException
    {
        Key key = new SecretKeySpec(AES_KEY, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(cEncryptedPassword);
    }
    
    private static byte[] SaltPassword(byte[] cDecryptedPassword, byte[] cSalt)
    {
        byte[] cResult = new byte[cDecryptedPassword.length + cSalt.length];
        System.arraycopy(cDecryptedPassword, 0, cResult, 0, cDecryptedPassword.length);
        System.arraycopy(cSalt, 0, cResult, cDecryptedPassword.length, cSalt.length);
        return cResult;
    }
    
    public static byte[] GetSHA256(byte[] cData) throws NoSuchAlgorithmException
    {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(cData);
        return md.digest();
    }
    
    public static String BytesToHexString(byte[] cBytes)
    {
        char[] cHexChars = new char[cBytes.length * 2];
        
        for ( int i = 0; i < cBytes.length; i++ )
        {
            int lV = cBytes[i] & 0xFF;
            cHexChars[i * 2] = BASE16[lV >>> 4];
            cHexChars[i * 2 + 1] = BASE16[lV & 0x0F];
        }
    
        return new String(cHexChars);
    }
    
    public static byte[] HexStringToBytes(String strHex)
    {
        byte[] cBytes = new byte[strHex.length() / 2];
        
        for (int i = 0; i < strHex.length(); i += 2)
        {
            cBytes[i / 2] = (byte)((Character.digit(strHex.charAt(i), 16) << 4) + Character.digit(strHex.charAt(i+1), 16));
        }
        
        return cBytes;
    }
    
    public static boolean ValidatePasswordIntegrity(String strPassword)
    {
        //Verify that a password is of suitable length.
        return strPassword.length() >= MIN_PASSWORD_LENGTH;
    }

    public static boolean ValidateEmailAddress(String strEmailAddress)
    {
        //Must contain an at and a dot.
        return (strEmailAddress.contains("@") && strEmailAddress.contains("."));
    }

    public static boolean ValidateActivationCode(String strActivationCode)
    {
        //Activation code must only contain HEX characters.
        if(strActivationCode.length() == Security.ACTIVATION_CODE_LENGTH)
        {
            for (char c : strActivationCode.toCharArray())
            {
                if (!(c == 'A' ||
                        c == 'B' ||
                        c == 'C' ||
                        c == 'D' ||
                        c == 'E' ||
                        c == 'F' ||
                        c == '0' ||
                        c == '1' ||
                        c == '2' ||
                        c == '3' ||
                        c == '4' ||
                        c == '5' ||
                        c == '6' ||
                        c == '7' ||
                        c == '8' ||
                        c == '9'))
                {
                    return false;
                }
            }

            return true;
        }

        return false;
    }
}
