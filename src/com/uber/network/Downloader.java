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
import java.net.URL;
import java.util.Vector;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import android.os.AsyncTask;

public class Downloader extends AsyncTask<Object, Object, Object> {

	private static final int DONE = 0;
	private static final int PRE_LOAD = 1;
	private static final int ERROR = 2;

	private static final int CONNECTION_ATTEMPTS = 3;

	private final int TIMEOUT_CONNECTION = 5000;
	
	private boolean mIsConnected = true;
	private boolean mIsProgressUpdated = false;
	private OnDownloadListener mDownloadListener = null;
	private final Vector<Request> mRequestQueue = new Vector<Request>();

	public void setOnDownloadListener(OnDownloadListener listener) {
		mDownloadListener = listener;
	}

	public void addDownload(String url, int type, int responseType) {
		final Request request = new Request();
		request.urlAddress = new UrlAddress(url);
		request.path = "";
		request.requestMethod = "GET";
		request.type = type;
		request.reponseType = responseType;
		addDownload(request);
	}
	
	public void addHead(String url, int type, int responseType) {
		final Request request = new Request();
		request.urlAddress = new UrlAddress(url);
		request.path = "";
		request.requestMethod = "HEAD";
		request.type = type;
		request.reponseType = responseType;
		addDownload(request);
	}

	public void addPost(UrlAddress urlAddress, String path, String postRequest, String contentType, int type, int responseType) {
		final Request request = new Request();
		request.urlAddress = urlAddress;
		request.path = path;
		request.requestMethod = "POST";
		request.body = postRequest;
		request.contentType = contentType;
		request.type = type;
		request.reponseType = responseType;
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

	@Override
	protected Object doInBackground(Object... params) {
		// Start conditions
		boolean shouldRemove = false;
		boolean firstAttempt = true;
		int attempt = CONNECTION_ATTEMPTS;
		Request request = null;
		if (!mRequestQueue.isEmpty()) {
			request = mRequestQueue.get(0);
			attempt = Math.max(request.urlAddress.size(), CONNECTION_ATTEMPTS);
		}
		// Algorithm
		while (!isCancelled()) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// Nothing to do
			}
			if (mRequestQueue.isEmpty()) {
				if (!mIsProgressUpdated) {
					continue;
				} else {
					// Exit
					break;
				}
			} else {
				request = mRequestQueue.get(0);
				if (shouldRemove || attempt == 0) {
					mRequestQueue.remove(0);
					if (mRequestQueue.isEmpty()) {
						continue;
					}
					// Reinit start conditions
					request = mRequestQueue.get(0);
					attempt = Math.max(request.urlAddress.size(), CONNECTION_ATTEMPTS);
					firstAttempt = true;
					shouldRemove = false;
				} else {
					--attempt;
				}
				if (firstAttempt) {
					publishProgress(PRE_LOAD, request.type);
					firstAttempt = false;
				}
				try {
					mIsProgressUpdated = false;
					InputStream responseInputStream = null;
					long lastModified = 0;
					final URL url = new URL(request.urlAddress.getAddress() + request.path);
					final String protocol = url.getProtocol();
					HttpURLConnection connection = null;
					if (protocol.equals("http")) {
						connection = (HttpURLConnection) url.openConnection();
					} else if (protocol.equals("https")) {
						trustCertificate();
						final HttpsURLConnection sslConnection = (HttpsURLConnection) url.openConnection();
						sslConnection.setHostnameVerifier(new UberHostnameVerifier());
						connection = sslConnection;
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
						lastModified = connection.getLastModified();
						responseInputStream = connection.getInputStream();
					}
					if (responseInputStream != null) {
						final Response response = Response.create(request.type, responseInputStream, lastModified, request.reponseType);
						responseInputStream.close();
						shouldRemove = true;
						publishProgress(DONE, response);
						mIsConnected = true;
					} else {
						request.urlAddress.rotateAddress();
						if (attempt == 0) {
							if (mIsConnected) {
								publishProgress(ERROR, request.type);
								mIsConnected = false;
							}
						}
					}
				} catch (Exception e) {
					request.urlAddress.rotateAddress();
					if (attempt == 0) {
						if (mIsConnected) {
							publishProgress(ERROR, request.type);
							mIsConnected = false;
						}
					}
				} 
			}
		}
		return null;
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

	private static class Request {
		UrlAddress urlAddress;
		String path;
		String requestMethod;
		String body;
		String contentType;
		int reponseType;
		int type;
	}

}
