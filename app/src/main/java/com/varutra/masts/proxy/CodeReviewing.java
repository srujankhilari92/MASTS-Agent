/** 
 * @Class Name :  CodeReviewing.java
 */

package com.varutra.masts.proxy;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import android.util.Base64;
import android.util.Log;

/**
 * @author : Varutra Consulting Pvt. Ltd.
 * @Create On : Apr 9, 2014 6:31:36 PM
 * @License : Copyright © 2014 Varutra Consulting Pvt. Ltd.- All rights
 *          reserved.
 */
public class CodeReviewing {

	private static byte[] key;
	public static String key1, key2;
	private static SecretKeySpec secretKey;
	private static String decryptedString;
	private static String encryptedString;
	private static SimpleDateFormat sdf;
	private static String curDate;

	public static String getDecryptedString() {
		return decryptedString;
	}

	public static void setDecryptedString(String decryptedString) {
		CodeReviewing.decryptedString = decryptedString;
	}

	public static String getEncryptedString() {
		return encryptedString;
	}

	public static void setEncryptedString(String encryptedString) {
		CodeReviewing.encryptedString = encryptedString;
	}

	public CodeReviewing() {
		// TODO Auto-generated constructor stub
	}

	public static String GenerateKey(String trim, String trim2) {
		// TODO Auto-generated method stub

		UUID uniqueKey = UUID.randomUUID();
		System.out.println(uniqueKey);

		return uniqueKey.toString();
	}

	public static String decrypt2(String key1, String key2, String encrypted) {
		try {
			IvParameterSpec iv = new IvParameterSpec(key2.getBytes("UTF-8"));

			SecretKeySpec skeySpec = new SecretKeySpec(key1.getBytes("UTF-8"),
					"AES");
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
			byte[] original = cipher.doFinal(android.util.Base64.decode(
					encrypted, android.util.Base64.DEFAULT));

			return new String(original);
		} catch (Exception ex) {

			ex.printStackTrace();
		}
		return null;
	}
	public static String encrypt(String key1, String key2, String value) {
		byte[] encrypted=null; 
		try {
			IvParameterSpec iv = new IvParameterSpec(key2.getBytes("UTF-8"));

			SecretKeySpec skeySpec = new SecretKeySpec(key1.getBytes("UTF-8"),"AES");
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
			encrypted= cipher.doFinal(value.getBytes());
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return Base64.encodeToString(encrypted, Base64.DEFAULT);
	}
}