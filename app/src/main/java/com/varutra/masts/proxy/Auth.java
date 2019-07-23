/** 
 * @Class Name :  Auth.java
 */

package com.varutra.masts.proxy;

/**
 * @author : Varutra Consulting Pvt. Ltd.
 * @Create On : Apr 9, 2014 12:09:58 PM
 * @License : Copyright © 2014 Varutra Consulting Pvt. Ltd.- All rights
 *          reserved.
 */
public class Auth {

	private static String tokn = null;

	public static String getTokn() {
		return tokn;
	}

	public static void setTokn(String tokn) {
		Auth.tokn = tokn;
	}

}
