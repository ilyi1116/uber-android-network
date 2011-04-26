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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.graphics.Bitmap;

public class Response {
	
	public final static int XML_TYPE = 600;
	public final static int JSON_TYPE = 601;
	public final static int IMAGE_TYPE = 602;
	public final static int NO_TYPE = 603;
	
	private int mRequestType = -1;
	private String mStringData;
	private long mLastModified;
	
	public static Response create(int requestType, InputStream data, long lastModified, int responseType) throws ResponseException {
		Response response = null;
		if (data != null) {
			String stringData = null;
			if (responseType == XML_TYPE) {
				try {
					response = new XmlResponse(data);
				} catch (ResponseException e) {
					throw new ResponseException("Could not parse Xml.");
				}
			} else if (responseType == JSON_TYPE) {
				try {
					stringData = streamToString(data);
					response = new JsonResponse(stringData);
				} catch (IOException e) {
					throw new ResponseException("Could not convert stream to string for JSON response.");
				}
			} else if (responseType == IMAGE_TYPE) {
				response = new ImageResponse(data);
			} else {
				response = new Response();
			}
			if (response != null) {
				if (stringData == null) {
					try {
						stringData = streamToString(data);
					} catch (IOException e) {
						// Nothing to do
					}
				}
				response.setRequestType(requestType);
				response.setStringData(stringData);
				response.setLastModified(lastModified);
			}
		}
		return response;
	}
	
	private void setRequestType(int type) {
		mRequestType = type;
	}
	
	private void setStringData(String stringData) {
		mStringData = stringData;
	}
	
	private void setLastModified(long lastModified) {
		mLastModified = lastModified;
	}
	
	public int getRequestType() {
		return mRequestType;
	}
	
	public String getStringData() {
		return mStringData;
	}
	
	public long getLastModified() {
		return mLastModified;
	}
	
	public DataNode getDataNode() {
		return null;
	}
	
	public Bitmap getBitmap() {
		return null;
	}
	
	public static String streamToString(InputStream stream) throws IOException {
		final char[] buffer = new char[0x10000];
		final StringBuilder stringBuilder = new StringBuilder();
		final InputStreamReader isr = new InputStreamReader(stream, "UTF-8");
		int read;
		while ((read = isr.read(buffer, 0, buffer.length)) != -1) {
			stringBuilder.append(buffer, 0, read);
		}
		return stringBuilder.toString();
	}
	
}
