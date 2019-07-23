package com.varutra.masts.proxy;

import java.util.Comparator;

public class Datecomparator implements Comparator {

	public int compare(Object arg0, Object arg1) {
		// TODO Auto-generated method stub

		Sortbean s1 = (Sortbean) arg0;
		Sortbean s2 = (Sortbean) arg1;

		return s1.date.compareToIgnoreCase(s2.date);
	}

}
