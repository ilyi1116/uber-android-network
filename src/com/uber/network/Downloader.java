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
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Vector;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import android.os.AsyncTask;

public class Downloader extends AsyncTask<Object, Object, Object> {

	private static final int DONE = 0;
	private static final int PRE_LOAD = 1;
	private static final int ERROR = 2;

	private final int TIMEOUT_CONNECTION = 5000;

	private boolean mIsConnected = true;
	private boolean mIsProgressUpdated = false;
	private OnDownloadListener mDownloadListener = null;
	private final Vector<Request> mRequestQueue = new Vector<Request>();

	public void setOnDownloadListener(OnDownloadListener listener) {
		mDownloadListener = listener;
	}

	public void addDownload(UrlAddress urlAddress, int type, int responseType) {
		final Request request = new Request(urlAddress, "", "GET", null, null, responseType, type, null);
		addDownload(request);
	}

	public void addGet(UrlAddress urlAddress, HashMap<String, String> params, int type, int responseType) {
		addGet(urlAddress, toQueryString(params), type, responseType);
	}
	
	public void addGet(UrlAddress urlAddress, String path, int type, int responseType) {
		final Request request = new Request(urlAddress, path, "GET", null, null, responseType, type, null);
		addDownload(request);
	}

	public void addHead(UrlAddress urlAddress, int type, int responseType) {
		final Request request = new Request(urlAddress, "", "HEAD", null, null, responseType, type, null);
		addDownload(request);
	}

	public void addPost(UrlAddress urlAddress, String path, String postRequest, String contentType, int type, int responseType, Object tag) {
		final Request request = new Request(urlAddress, path, "POST", postRequest, contentType, responseType, type, tag);
		addDownload(request);
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
		}
	}
	
	/**
	 * This is a *smart* helper method which, given a request,
	 * manages to create the right kind of http connection.
	 * @param request
	 * @return the connection object
	 */
	private HttpURLConnection connect(Request request) {
		HttpURLConnection connection = null;
		try {
			final URL url = new URL(request.urlAddress.getAddress() + request.path);
			final String protocol = url.getProtocol();
			if (protocol.equals("http")) {
				connection = (HttpURLConnection) url.openConnection();
			} else if (protocol.equals("https")) {
				trustCertificate();
				final HttpsURLConnection sslConnection = (HttpsURLConnection) url.openConnection();
				sslConnection.setHostnameVerifier(new UberHostnameVerifier());
				connection = sslConnection;
				// WTF: a bug in android forces us to try connecting twice in a row
				// for Https connections.
				request.attemptCount = 2;
			}
			if (connection != null) {
				connection.setRequestMethod(request.requestMethod);
				connection.setDoOutput(true);
				connection.setConnectTimeout(TIMEOUT_CONNECTION);
				if (request.contentType != null) {
					connection.setRequestProperty("Content-Type", request.contentType);
				}
				if (request.body != null) {
					connection.setDoInput(true);
					connection.setRequestProperty("Content-length", String.valueOf(request.body.length()));
					final DataOutputStream output = new DataOutputStream(connection.getOutputStream());
					output.writeBytes(request.body);
				} else {
					connection.connect();
				}
			}
		} catch (Exception e) {
			connection = null;
		}
		return connection;
	}

	/**
	 * This is the core task of the downloader. 
	 * 1. Pop the next download item
	 * 2. Create the appropriate connection
	 * 3. Handle the response
	 * 4. Loop back until there aren't any items left
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
				if (request.isFirstAttempt) {
					publishProgress(PRE_LOAD, request.type);
					request.isFirstAttempt = false;
				}
				try {
					mIsProgressUpdated = false;
					final HttpURLConnection connection = connect(request);
					if (connection != null) {
						request.responseCode = connection.getResponseCode();
						if (request.responseCode >= 0) {
							final InputStream responseStream;
							if (request.responseCode == 200) {
								responseStream = connection.getInputStream();
							} else {
								responseStream = connection.getErrorStream();
							}
							onServerResponse(request, responseStream, connection.getLastModified());
							continue;
						}
					} else {
						onNetworkError(request);
					}
				} catch (Exception e) {
					onNetworkError(request);
				}
			}
		}
		return null;
	}
	
	private void onServerResponse(Request request, InputStream responseStream, long lastModified) throws ResponseException {
		mIsConnected = true;
		if (request.urlAddress.shouldRotateWithCode(request.responseCode)) {
			// This error code means we should try another server.
			if (--request.rotationCount == 0) {
				// Actually we've already tried all of our servers,
				// so remove this download item and publish a little error.
				publishProgress(ERROR, request.type);
				mRequestQueue.remove(0);
			} else {
				// We still have some servers to try, so let's use the next
				// one and see if it works any better for our current download
				// item.
				request.urlAddress.rotateAddress();
				// WTF: We need to send SSH requests twice because of some pipe errors.
				try {
					final URL url = new URL(request.urlAddress.getAddress());
					final String protocol = url.getProtocol();
					if (protocol.equals("http")) {
						request.attemptCount = 1;
					} else if (protocol.equals("https")) {
						request.attemptCount = 2;
					}
				} catch (MalformedURLException e) {
					request.attemptCount = 1;
				}
			}
		} else {
			// Everything looks good here. The response can still have an error code
			// but it is handled by the server, so we consider it DONE.
			final Response response = Response.create(request.type, responseStream, lastModified, request.responseType, request.tag);
			response.setResponseCode(request.responseCode);
			mRequestQueue.remove(0);
			publishProgress(DONE, response);
		}
	}
	
	private void onNetworkError(Request request) {
		if (--request.attemptCount == 0) {
			mIsConnected = false;
			mRequestQueue.remove(0);
			publishProgress(ERROR, request.type);
		}
	}

	@Override
	protected void onProgressUpdate(Object... params) {
		if (params.length >= 2) {
			final int retCode = ((Integer) params[0]).intValue();
			if (mDownloadListener != null) {
				if (retCode == PRE_LOAD) {
					mDownloadListener.onPreLoad(((Integer) params[1]).intValue());
				} else if (retCode == ERROR) {
					mDownloadListener.onError(((Integer) params[1]).intValue());
				} else if (retCode == DONE) {
					final Response response = (Response) params[1];
					if (response != null) {
						mDownloadListener.onLoad(response);
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
	
	public static String toQueryString(HashMap<String, String> params) {
		final Object[] keys = params.keySet().toArray();
		String paramsPath = "";
		final int size = keys.length;
		for (int i = 0; i < size; ++i) {
			final String key = (String)keys[i];
			paramsPath += key + "=" + params.get(key);
			if (i < (size - 1)) {
				paramsPath += "&";
			}
			i++;
		}
		return paramsPath;
	}

	/**
	 * Wrapper for the request object
	 * @author Jordan Bonnet
	 *
	 */
	private static class Request {
		
		UrlAddress urlAddress;
		String path;
		String requestMethod;
		String body;
		String contentType;
		int responseType;
		int type;
		int responseCode;
		int rotationCount;
		int attemptCount;
		boolean isFirstAttempt;
		Object tag;
		
		public Request(UrlAddress urlAddress, String path, String requestMethod, String body, String contentType, int responseType, int type, Object tag) {
			this.urlAddress = urlAddress;
			this.path = path;
			this.requestMethod = requestMethod;
			this.body = body;
			this.contentType = contentType;
			this.responseType = responseType;
			this.type = type;
			this.tag = tag;
			init();
		}
		
		public void init() {
			this.rotationCount = this.urlAddress.size();
			this.attemptCount = 1;
			this.responseCode = -1;
			this.isFirstAttempt = true;
		}
	}
}
