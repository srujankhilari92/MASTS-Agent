package com.varutra.masts.proxy;

import java.util.ArrayList;
import java.util.List;

public class AES {
	public String key1 = null;
	public String key2 = null;
	private List<String> list;

	public AES() {

		String a = this.toAscii(69) + this.toAscii(110) + this.toAscii(99)
				+ this.toAscii(114) + this.toAscii(121) + this.toAscii(68)
				+ this.toAscii(50) + this.toAscii(50) + this.toAscii(77)
				+ this.toAscii(48) + this.toAscii(54) + this.toAscii(89)
				+ this.toAscii(50) + this.toAscii(48) + this.toAscii(49)
				+ this.toAscii(53);

		String b = this.toAscii(77) + this.toAscii(79) + this.toAscii(77)
				+ this.toAscii(84) + this.toAscii(48) + this.toAscii(53)
				+ this.toAscii(77) + this.toAscii(52) + this.toAscii(53)
				+ this.toAscii(90) + this.toAscii(80) + this.toAscii(77)
				+ this.toAscii(83) + this.toAscii(48) + this.toAscii(51)
				+ this.toAscii(51);
		list = this.encrypt(a, b);

		setKey1(list.get(0));
		setKey2(list.get(1));
	}

	public String getKey1() {
		return key1;
	}

	public void setKey1(String key1) {
		this.key1 = key1;
	}

	public String getKey2() {
		return key2;
	}

	public void setKey2(String key2) {
		this.key2 = key2;
	}

	public List<String> encrypt(String key1, String key2) {

		List<String> list = new ArrayList<String>();
		String res1 = "", res2 = "";
		int l = key2.length() - 1;
		int l1 = key1.length() - 1;
		for (int i = 0; i < key1.length() / 2; i++) {

			res1 = res1 + key1.charAt(i) + key2.charAt(l);
			l = l - 1;

		}
		for (int i = 0; i < key2.length() / 2; i++) {
			res2 = res2 + key2.charAt(i) + key1.charAt(l1);
			l1 = l1 - 1;
		}
		
		list.add(res1);
		list.add(res2);
		return list;
	}

	public String toAscii(int i) {
		return Character.toString((char) i);
	}

}