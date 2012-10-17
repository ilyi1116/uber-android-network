package com.uber.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamUtils {

	public static String streamToString(InputStream stream) {
		final char[] buffer = new char[0x10000];
		final StringBuilder stringBuilder = new StringBuilder();
		try {
			if (stream != null && stream.markSupported() && stream.available() == 0) {
				stream.reset();
			}
			final InputStreamReader isr = new InputStreamReader(stream, "UTF-8");
			int read;
			while ((read = isr.read(buffer, 0, buffer.length)) != -1) {
				stringBuilder.append(buffer, 0, read);
			}
		} catch (IOException e) {
			UBLogs.addLine(e.getMessage());
		}
		
		return stringBuilder.toString();
	}
}
