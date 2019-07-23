package com.varutra.masts.proxy;

import java.util.*;

class SizeComparator implements Comparator {
	public int compare(Object arg0, Object arg1) {
		// TODO Auto-generated method stub
		Sortbean s1 = (Sortbean) arg0;
		Sortbean s2 = (Sortbean) arg1;

		if (s1.size == s2.size)
			return 0;
		else if (s1.size > s2.size)
			return 1;
		else
			return -1;
	}
}