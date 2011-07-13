package com.uber.network;

public class Util {

	public static Integer parseInteger(String integerString) {
		try {
			return Integer.valueOf(integerString);
		} catch (NumberFormatException nfe) {
			return null;
		}
	}

	public static Double parseDouble(String doubleString) {
		try {
			return Double.valueOf(doubleString);
		} catch (NumberFormatException nfe) {
			return null;
		}
	}
}
