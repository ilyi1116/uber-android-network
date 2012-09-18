package com.uber.utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.util.Log;

import com.uber.network.Request;
import com.uber.network.Response;

public class UBLogs {
	
	public static boolean consoleEnabled = true;
	
	private static final int MAX_LOGS = 128;

	private static final ArrayList<String> sLogs = new ArrayList<String>();
	
	public static synchronized void logRequest(Request request) {
		if (request == null) {
			return;
		}
		
		final String url = (request.getUrlAddress() != null ? request.getUrlAddress().getAddress() : "")  
				+ request.getPath();
		
		String log = "---------- REQUEST ----------\n";
		log += "URL: " + url + "\n";
		log += request.getBodyString() + "\n";

		add(log);
	}
	
	public static void logResponse(Request request, HttpURLConnection connection, Response response, long timeInMs) {
		if (connection == null) {
			return;
		}
		
		String log = "---------- RESPONSE ----------\n";
				
		try {
			
			log += "RESPONSE CODE: " + connection.getResponseCode() + "\n";
			log += "RESPONSE TIME: " + (System.currentTimeMillis() - timeInMs) + "\n";
			
			final Map<String, List<String>>  headers = connection.getHeaderFields();
			final List<String> hashes = headers != null ? headers.get("File-Hashes") : null; 

			if (hashes != null) {
				for (String line : hashes) {
					log += line + "\n";
				}
			}
			
			final int responseType = request.getResponseType();
			
			if (Response.IMAGE_TYPE == responseType || response == null) {				
				return;
			}
			
			String responseString = response.getStringData();
			
			responseString = responseString.replaceAll(" ", "");
			responseString = responseString.replaceAll("\n", "");
			log += responseString + "\n";
		} catch (IOException e) {
			log += "Exception while parsing: " + e.getMessage() + "\n";
		} finally {
			add(log);
		}
	}

	public static synchronized void add(String log) {
		final String logWithDate = UBUtils.getUTCTimestamp("yyyy-MM-dd HH:mm:ss") + ": " + log;
		if (sLogs.size() >= MAX_LOGS) {
			sLogs.remove(0);
		}
		sLogs.add(logWithDate);
		
		if (consoleEnabled) {
			Log.d("uber", logWithDate);
		}
	}
	
	public static synchronized String getLogs() {
		final StringBuffer buffer = new StringBuffer();
		for (int i = sLogs.size() -1; i >= 0; i--) {
			
			try {
				buffer.insert(0, "\n\n");
				buffer.insert(0, sLogs.get(i));
			} catch(OutOfMemoryError error) {
				// attempt to send at least last logs
				return buffer.toString();
			}
		}
		return buffer.toString();
	}
	
	
	public static void setConsoleEnabled(boolean consoleEnabled) {
		UBLogs.consoleEnabled = consoleEnabled;
	}
}