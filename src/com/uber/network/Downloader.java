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

import android.os.AsyncTask;
import android.os.Bundle;

import com.uber.utils.StreamUtils;
import com.uber.utils.UBLogs;

public class Downloader extends AsyncTask<Object, Object, Object> {

	private static final int DONE = 0;
	private static final int PRE_LOAD = 1;
	private static final int ERROR = 2;

	public final static int DOWNLOADER_NORMAL_PRIORITY = 0;
	public final static int DOWNLOADER_RETRY_LOW_PRIORITY = 1;
	public final static int DOWNLOADER_HIGH_PRIORITY = 2;

	public final static boolean REPORT_NETWORK_PROBLEMS = true;
	
	private boolean mIsConnected = true;
	private boolean mIsProgressUpdated = false;
	private OnDownloadListener mDownloadListener = null;
	private final Vector<Request> mRequestQueue = new Vector<Request>();
	private HttpURLConnection mConnection = null;
	private Bundle headerParams;

	public Downloader() {
		super();
	}

	public Downloader(HttpURLConnection connection) {
		super();
		mConnection = connection;
	}

	public void setOnDownloadListener(OnDownloadListener listener) {
		mDownloadListener = listener;
	}

	public void addDownload(UrlAddress urlAddress, int type, int responseType, int priority) {
		addDownload(urlAddress, type, responseType, priority, null);
	}

	public void addDownload(UrlAddress urlAddress, int type, int responseType, int priority, Object tag) {
		final Request request = new Request(urlAddress, "", "GET", null, null, responseType, type, tag, priority);
		addDownload(request);
	}

	public void addGet(UrlAddress urlAddress, HashMap<String, Object> params, int type, int responseType, int priority) {
		addGet(urlAddress, toQueryString(params), type, responseType, priority);
	}

	public void addGet(UrlAddress urlAddress, String path, int type, int responseType, int priority) {
		final Request request = new Request(urlAddress, path, "GET", null, null, responseType, type, null, priority);
		addDownload(request);
	}

	public void addHead(UrlAddress urlAddress, int type, int responseType, int priority) {
		final Request request = new Request(urlAddress, "", "HEAD", null, null, responseType, type, null, priority);
		addDownload(request);
	}

	public void addPost(UrlAddress urlAddress, String path, String postRequest, String contentType, int type, int responseType, Object tag, int priority) {
		addRequest(urlAddress, path, postRequest, contentType, type, responseType, tag, priority, "POST");
	}

	public void addRequest(UrlAddress urlAddress, String path, String postRequest, String contentType, int type, int responseType, Object tag, int priority, String requestMethod) {
		final Request request = new Request(urlAddress, path, requestMethod, postRequest, contentType, responseType, type, tag, priority);
		addDownload(request);
	}

	public void addRequest(Request request) {
		if (request != null) {
			addDownload(request);
		}
	}

	private void addDownload(Request request) {
		if (mIsConnected) {
			mRequestQueue.add(request);
			if (getStatus() == AsyncTask.Status.PENDING) {
				mIsProgressUpdated = false;
				execute();
			}
		}
	}

	private void trustCertificate() {
		try {
			final SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, new TrustManager[] { new UberTrustManager() }, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
		} catch (Exception e) {
			// Pretty fucked here
			ACRA.getErrorReporter().handleSilentException(e);
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
	@Override
	protected Object doInBackground(Object... params) {
		// Algorithm
		while (!isCancelled()) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// Nothing to do
			}
			if (mRequestQueue.isEmpty()) {
				if (!mIsProgressUpdated) {
					// Don't kill the task until the UI has been updated.
					continue;
				} else {
					// Exit
					break;
				}
			} else {
				final Request request = mRequestQueue.get(0);
				if (request.isFirstAttempt()) {
					resetRequestAttemptCount(request);
					publishProgress(PRE_LOAD, request);
					request.setFirstAttempt(false);
				}
				
				// Logs
				long timeInMs = System.currentTimeMillis();
				
				if (!request.getPath().equals("/mobile_logs")) {
					UBLogs.logRequest(request);
				} else {
					UBLogs.addLine("MOBILE LOGS \n");
				}
				
				HttpURLConnection connection = null;
				try {	
					
					mIsProgressUpdated = false;

					connection = connect(request);
					
					if (connection == null) {
						onNetworkError(request);
						return null;
					}
					
					request.setResponseCode(connection.getResponseCode());
					
					InputStream responseStream;
					
					final int responseCode = request.getResponseCode();
					if (responseCode < 0) {
						// DON'T ASK! Legacy...
						continue;
					} else
					if (responseCode == 200) {
						responseStream = connection.getInputStream();
					} else
					if (responseCode > 500) {
						UBLogs.addLine("Response code: " + responseCode);
						publishProgress(ERROR, request, null);
						continue;
					} else {
						responseStream = connection.getErrorStream();
					}
					
					final String contentEnconding = connection.getContentEncoding();
					if (contentEnconding != null && contentEnconding.equalsIgnoreCase("gzip")) {
						responseStream = new GZIPInputStream(responseStream);
					}
					
					if (REPORT_NETWORK_PROBLEMS && responseStream == null) {
						ACRA.getErrorReporter().handleException(new RuntimeException("Empty response stream. Code: " + responseCode));
					}					
					onServerResponse(request, responseStream, connection, timeInMs);
					

				} catch (Exception exception) {
					final InputStream errorStream = connection != null ? connection.getErrorStream() : null;
					if (exception instanceof ConnectException) {
						// Server is down.
						rotateAddress(request);
					} else if (exception instanceof UnknownHostException || exception instanceof SocketTimeoutException) {
						// Internet connection is down
						onNetworkError(request, exception, errorStream);
					} else {
						// Don't know why it's here
						onNetworkError(request, exception, errorStream);
					}
				}
			}
		}
		return null;
	}

	private void resetRequestAttemptCount(Request request) {
		if (request.getPriority() == DOWNLOADER_RETRY_LOW_PRIORITY) {
			// Setting limit to low priority retry.
			request.setAttemptCount(20);
		} else {
			if (request.getProtocol().equals("https")) {
				request.setAttemptCount(2);
			} else if (request.getProtocol().equals("http")) {
				request.setAttemptCount(1);
			}

			if (request.getPriority() == DOWNLOADER_HIGH_PRIORITY) {
				request.setAttemptCount(request.getAttemptCount() * 3);
			}
		}
	}

	private void onServerResponse(Request request, InputStream responseStream, HttpURLConnection connection, long timeInMs) throws ResponseException {
		mIsConnected = true;
		if (request.getUrlAddress().shouldRotateWithCode(request.getResponseCode())) {
			UBLogs.logResponse(request, connection, null, timeInMs);
			
			// This error code means we should try another server.	
			rotateAddress(request);
		} else {
			// Everything looks good here. The response can still have an error
			// code,
			// but it is handled by the server, so we consider it DONE.
			final Response response = Response.create(request, null, connection);
			UBLogs.logResponse(request, connection, response, timeInMs);
			mRequestQueue.remove(0);
			publishProgress(DONE, request, response);
		}
	}

	private void onNetworkError(Request request, Exception exception, InputStream stream) {
		request.setAttemptCount(request.getAttemptCount() - 1);
		
		if (stream != null) {
			UBLogs.addLine("Error stream: " + StreamUtils.streamToString(stream));
		}
		
		if (REPORT_NETWORK_PROBLEMS) {
			if (!(exception instanceof MalformedURLException ||
				  exception instanceof UnknownHostException  ||
				  exception instanceof SocketTimeoutException)) {
				ACRA.getErrorReporter().handleException(exception);
			} else 
			if (!(exception instanceof MalformedURLException)) {
				UBLogs.addLine("Network Error: " + exception);
			}
		}
				
		if (request.getAttemptCount() <= 0) {
			mIsConnected = false;
			mRequestQueue.remove(0);
			publishProgress(ERROR, request, exception);
		}
	}

	private void onNetworkError(Request request) {
		onNetworkError(request, null, null);
	}

	private void rotateAddress(Request request) {
		request.getUrlAddress().rotateAddress();
		request.setRotationCount(request.getRotationCount() - 1);
		if (request.getRotationCount() == 0) {
			// Actually we've already tried all of our servers,
			// so remove this download item and publish a little error.
			publishProgress(ERROR, request, new Exception("Rotate the server as much as we could"));
			mRequestQueue.remove(0);
		} else {
			// We still have some servers to try, so let's use the next
			// one and see if it works any better for our current download item.
			// WTF: We need to send SSH requests twice because of some pipe
			// errors.
			resetRequestAttemptCount(request);
		}
	}
	
	private OnDownloadListener getListenerFromRequest(Request request) {
		OnDownloadListener listener = request.getListener();
		if (listener == null) {
			listener = this.mDownloadListener;
		}
		return listener;
	}

	@Override
	protected void onProgressUpdate(Object... params) {
		if (params.length >= 2) {
			final int retCode = ((Integer) params[0]).intValue();
			if (retCode == PRE_LOAD) {
				
				// Handle pre load
				final Request request = (Request) params[1];
				final OnDownloadListener listener = getListenerFromRequest(request);
				if (listener != null) {
					listener.onPreLoad(request.getType());
				}
				
			} else if (retCode == ERROR) {
				
				// Handle error
				final Request request = (Request) params[1];
				final OnDownloadListener listener = getListenerFromRequest(request);
				if (listener != null) {
					if (request != null) {
						final Exception exception = (Exception) params[2];
						listener.onError(request.getType(), request, exception);
					} else {
						listener.onError(-1, null, null);
					}
				}
				
			} else if (retCode == DONE) {
				
				// Handle success
				final Request request = (Request) params[1];
				final OnDownloadListener listener = getListenerFromRequest(request);
				if (listener != null) {
					final Response response = (Response) params[2];
					if (response != null) {
						listener.onLoad(response);
					} else {
						listener.onError(request.getType(), request, null);
					}
				}
			}

			if (retCode != PRE_LOAD) {
				mIsProgressUpdated = true;
			}
		}
	}

	@Override
	protected void onCancelled() {
		if (mDownloadListener != null) {
			mDownloadListener.onCancel();
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