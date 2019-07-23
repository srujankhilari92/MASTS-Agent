
/** 
 * @Class Name :  BeforDestroy.java
 */

package com.varutra.masts.proxy;


/**
 * @author     :  Varutra Consulting Pvt. Ltd.
 * @Create On  :  Aug 11, 2014 7:43:06 PM
 * @License    :  Copyright © 2014 Varutra Consulting Pvt. Ltd.- All rights reserved.
 */
public class BeforDestroy {

	/**
	 * 
	 */
	public BeforDestroy() {
		// TODO Auto-generated constructor stub
		Chk_TcpFlag.setLogcatflag(false);
		Chk_TcpFlag.setMonitorflag(false);
		Chk_TcpFlag.setTcpflag(false);
	}
	
	
}
