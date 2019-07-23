package com.varutra.masts.proxy;

import android.annotation.SuppressLint;

@SuppressLint("DefaultLocale")
public class Validation {

	private boolean b;

	public boolean IP_Validation(String emailid) {
		try {
			String emailreg = "^[0-9]{1,3}+\\.[0-9]{1,3}+\\.[0-9]{1,3}+\\.[0-9]{1,3}$";

			b = emailid.matches(emailreg);

			if (b == false) {
				System.out.println("IP is Invalid");
			} else if (b == true) {
				System.out.println("IP is Valid");
			}

		} catch (Exception e) {

			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		return b;
	}

	public boolean PORT_Validation(String emailid) {
		try {
			String emailreg = "^[0-9]{4,5}$";

			b = emailid.matches(emailreg);

			if (b == false) {
				System.out.println("IP is Invalid");
			} else if (b == true) {
				System.out.println("IP is Valid");
			}

		} catch (Exception e) {

			e.printStackTrace();
			
		}
		return b;
	}

	public boolean emailValidation(String emailid) {
		try {
			String emailreg = "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";

			b = emailid.matches(emailreg);

			if (b == false) {
				System.out.println("Address is Invalid");
			} else if (b == true) {
				System.out.println("Address is Valid");
			}

		} catch (Exception e) {

			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		return b;
	}

	public boolean validateVersion(String osversion) {
		try {
			String emailreg = "^[0-9]{1,}[\\.]{1,1}[0-9]{1,}(\\.[0-9]{1,})*$";

			b = osversion.matches(emailreg);

			if (b == false) {
				System.out.println("Version is Invalid");
			} else if (b == true) {
				System.out.println("Version is Valid");
			}
		} catch (Exception e) {

			e.printStackTrace();
			System.out.println(e.getMessage());
		}

		return b;

	}

	public boolean validateName(String name) {
		try {

			String emailreg = "^[_A-Za-z0-9\\@\\*\\$\\!]{8,15}$";// "^[A-Za-z]{1}+(\\s[A-Za-z]{1}+)*(\\s[A-Za-z]{1}+)*$";

			b = name.matches(emailreg);

			if (b == false) {
				System.out.println("Username & Password is Invalid");
			} else if (b == true) {
				System.out.println("Username & Password is Valid");
			}
		} catch (Exception e) {

			e.printStackTrace();
			System.out.println(e.getMessage());
		}

		return b;

	}

	public boolean validateCity(String city) {
		try {
			String emailreg = "^[A-Za-z]{1,}[\\s]{0,1}[\\.]{0,1}[A-Za-z]{0,}([\\s]{0,1}[\\.]{0,1}[A-Za-z]{0,})*$";

			b = city.matches(emailreg);

			if (b == false) {
				System.out.println("city is Invalid");
			} else if (b == true) {
				System.out.println("city is Valid");
			}

		} catch (Exception e) {

			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		return b;
	}

	public boolean validateKVID(String kvid) {
		try {
			String emailreg = "^[A-Za-z]{3,3}[0-9]{2,}$";

			b = kvid.matches(emailreg);

			if (b == false) {
				System.out.println("kvid is Invalid");
			} else if (b == true) {
				System.out.println("kvid is Valid");
			}

		} catch (Exception e) {

			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		return b;
	}

	public boolean validateCVE(String cve) {
		try {
			String emailreg = "^([A-Za-z]{3,3}[\\-]{1,1}[0-9]{4,4}[\\-]{1,1}[0-9]{4,7}|[0-9]{4,4}[\\-][0-9]{4,7}|[0-9]{4,7})$";

			b = cve.matches(emailreg);

			if (b == false) {
				System.out.println("cve is Invalid");
			} else if (b == true) {
				System.out.println("cve is Valid");
			}

		} catch (Exception e) {

			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		return b;
	}

	@SuppressLint("DefaultLocale")
	public String nameFormat(String name) {

		String returnStr = null;

		if (name.contains(".")) {
			String[] s1 = name.split("\\.");
			System.out.println("String Length : " + s1.length);
			s1[0] = s1[0].toLowerCase();
			s1[1] = s1[1].toLowerCase();
			char first = Character.toUpperCase(s1[0].charAt(0));
			char second = Character.toUpperCase(s1[1].charAt(0));
			if (s1.length > 2) {
				s1[2] = s1[2].toLowerCase();
				char third = Character.toUpperCase(s1[2].charAt(0));
				returnStr = first + s1[0].substring(1) + "." + second
						+ s1[1].substring(1) + "." + third + s1[2].substring(1);
			} else {
				returnStr = first + s1[0].substring(1) + "." + second
						+ s1[1].substring(1);
			}

		} else if (name.contains("_")) {
			String[] s2 = name.split("\\_");
			System.out.println("String Length : " + s2.length);
			s2[0] = s2[0].toLowerCase();
			s2[1] = s2[1].toLowerCase();
			char third = Character.toUpperCase(s2[0].charAt(0));
			char fourth = Character.toUpperCase(s2[1].charAt(0));
			if (s2.length > 2) {
				s2[2] = s2[2].toLowerCase();
				char fifth = Character.toUpperCase(s2[2].charAt(0));
				returnStr = third + s2[0].substring(1) + "_" + fourth
						+ s2[1].substring(1) + "_" + fifth + s2[2].substring(1);
			} else {
				returnStr = third + s2[0].substring(1) + "_" + fourth
						+ s2[1].substring(1);
				
			}
		} else {
			name = name.toLowerCase();
			char first = Character.toUpperCase(name.charAt(0));
			returnStr = first + name.substring(1);
		}
		return returnStr;
	}
}