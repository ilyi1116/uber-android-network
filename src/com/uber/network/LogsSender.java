/**
    UberAndroidNetwork: A JSON/XML network abstraction layer
    Copyright (c) 2011 by Jordan Bonnet, Uber Technologies

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in
    all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    THE SOFTWARE.
 */

package com.uber.network;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.acra.ACRA;
import org.acra.ErrorReporter;
import org.acra.sender.ReportSenderException;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.uber.utils.UBLogs;

public class LogsSender  {

	private static final int ATTEMPTS_COUNT = 3;
	private static final int DONE = 0;
	private static final int PRE_LOAD = 1;
	private static final int ERROR = 2;

	public final static int DOWNLOADER_NORMAL_PRIORITY = 0;
	public final static int DOWNLOADER_RETRY_LOW_PRIORITY = 1;
	public final static int DOWNLOADER_HIGH_PRIORITY = 2;

	public final static boolean REPORT_NETWORK_PROBLEMS = true;
	
	private HttpURLConnection mConnection = null;
	private Bundle headerParams;
	
	private final Request mRequest;
	
	public LogsSender(Request request) {
		super();
		
		mRequest = request;
	}

	private void trustCertificate() {
		try {
			final SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, new TrustManager[] { new UberTrustManager() }, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
		} catch (Exception e) {
			// Pretty fucked here
		}
	}

	/**
	 * This is a *smart* helper method which, given a request, manages to create
	 * the right kind of http connection.
	 * 
	 * @param request
	 * @return the connection object
	 * @throws IOException
	 */
	private HttpURLConnection connect(Request request) throws IOException {
		if (mConnection != null) {
			return mConnection;
			
		} else {
			
			HttpURLConnection connection = null;
		
			// Get url
			final UrlAddress urlAddress = request.getUrlAddress();
			if (urlAddress != null) {
				
				// Get protocol
				URL url = new URL(urlAddress.getAddress() + request.getPath());
								
				final String protocol = url.getProtocol();
				
				// Handle both protocols
				if (protocol.equals("http")) {
					connection = (HttpURLConnection) url.openConnection();
				} else if (protocol.equals("https")) {
					trustCertificate();
					final HttpsURLConnection sslConnection = (HttpsURLConnection) url.openConnection(Proxy.NO_PROXY);
					sslConnection.setHostnameVerifier(new UberHostnameVerifier());
					connection = sslConnection;
				}
				
				if (connection != null) {
					
					// Add custom headers
					if (headerParams != null) {
						for (String key : headerParams.keySet()) {
							final Object param = headerParams.get(key);
							if (param != null) {
								if (param instanceof String) {
									connection.addRequestProperty(key, (String) param);
								} else {
									connection.addRequestProperty(key, param.toString());
								}
							}
						}
					}
					
					
					// Set method
					final String method = request.getRequestMethod();
					connection.setRequestMethod(method);
					
					// Set content type
					if (request.getContentType() != null) {
						connection.setRequestProperty("Content-Type", request.getContentType());
					}
					
					// Set content length
					int contentLength = request.getBody() == null ? 0 : request.getBody().length;
					connection.setRequestProperty("Content-length", String.valueOf(contentLength));
					
					// Handle if has body or not
					if (!(method.equals("DELETE") || method.equals("GET")) && request.getBody() != null) {
						connection.setDoOutput(true);
						connection.setDoInput(true);
						final DataOutputStream output = new DataOutputStream(connection.getOutputStream());
						output.write(request.getBody());
					} else {
						connection.connect();
					}
				}
			}
			return connection;
		}
	}

	public void setHeaderParams(Bundle headerParams) {
		this.headerParams = headerParams;
	}

	/**
	 * This is the core task of the downloader. 1. Pop the next download item 2.
	 * Create the appropriate connection 3. Handle the response 4. Loop back
	 * until there aren't any items left
	 */
	public void execute() throws ReportSenderException {
		// Algorithm
		while (mRequest.getAttemptCount() >= 0) {
			
			final Request request = mRequest;
			if (request.isFirstAttempt()) {
				request.setAttemptCount(ATTEMPTS_COUNT);
				request.setFirstAttempt(false);
			}
			
			// Logs
			long timeInMs = System.currentTimeMillis();
			
			UBLogs.addLine("MOBILE LOGS SENT\n");
			
			try {
				
				final HttpURLConnection connection = connect(request);
				
				if (connection == null) {
					onNetworkError(request);
					return;
				}
				
				request.setResponseCode(connection.getResponseCode());
				
				if (request.getResponseCode() >= 0) {
					InputStream responseStream;
					if (request.getResponseCode() == 200) {
						responseStream = connection.getInputStream();
					} else {
						if (REPORT_NETWORK_PROBLEMS) {
							UBLogs.addLine("Response code " + request.getResponseCode());
						}
						responseStream = connection.getErrorStream();
					}
					final String contentEnconding = connection.getContentEncoding();
					if (contentEnconding != null && contentEnconding.equalsIgnoreCase("gzip")) {
						responseStream = new GZIPInputStream(responseStream);
					}
					if (onServerResponse(request, responseStream, connection, timeInMs)) {
						return;
					}
				}
				
			} catch (ReportSenderException e) {
				throw e;
			} catch (Exception exception) {
				if (exception instanceof ConnectException) {
					// Server is down.
					rotateAddress(request);
				} else if (exception instanceof UnknownHostException || exception instanceof SocketTimeoutException) {
					// Internet connection is down
					onNetworkError(request, exception);
				} else {
					// Don't know why it's here
					onNetworkError(request, exception);
				}
			}
		}
	}

	private boolean onServerResponse(Request request, InputStream responseStream, HttpURLConnection connection, long timeInMs) throws ResponseException {
		if (request.getUrlAddress().shouldRotateWithCode(request.getResponseCode())) {
			UBLogs.logResponse(request, connection, null, timeInMs);
			
			// This error code means we should try another server.
			rotateAddress(request);
			return false;
		} else {
			// Everything looks good here. The response can still have an error
			// code, but it is handled by the server, so we consider it DONE.
			final Response response = Response.create(request, responseStream, connection);
			UBLogs.logResponse(request, connection, response, timeInMs);
						
			return true;
		}
	}

	private void onNetworkError(Request request, Exception exception) throws ReportSenderException {
		if (REPORT_NETWORK_PROBLEMS && ! (exception instanceof MalformedURLException) && exception != null) {
			UBLogs.addLine("Network exception (" + exception.getClass().getSimpleName() + "): " + exception.getMessage());
		}
		
		if (exception == null) {
			throw new ReportSenderException("Failed to establish connection", new RuntimeException());
		} else
		if (!(exception instanceof MalformedURLException)) {
			throw new ReportSenderException(exception.getMessage(), exception);
		}
		
	}

	private void onNetworkError(Request request) throws ReportSenderException {
		onNetworkError(request, null);
	}

	private void rotateAddress(Request request) {
		request.getUrlAddress().rotateAddress();
		request.setRotationCount(request.getRotationCount() - 1);
		if (request.getRotationCount() == 0) {
			// Actually we've already tried all of our servers,
			// so remove this download item and publish a little error.
			mRequest.setAttemptCount(0);
		} else {
			// We still have some servers to try, so let's use the next
			// one and see if it works any better for our current download item.
			// WTF: We need to send SSH requests twice because of some pipe
			// errors.
			mRequest.setAttemptCount(ATTEMPTS_COUNT);
		}
	}
	
	public static String toQueryString(HashMap<String, Object> params) {
		final Object[] keys = params.keySet().toArray();
		String paramsPath = "?";
		final int size = keys.length;
		for (int i = 0; i < size; ++i) {
			final String key = (String) keys[i];
			Object value = params.get(key);
			if (key instanceof String) {
				paramsPath += key + "=" + value;
			} else {
				paramsPath += key + "=" + value.toString();
			}
			if (i < (size - 1)) {
				paramsPath += "&";
			}
		}
		return paramsPath;
	}
}