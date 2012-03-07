package com.uber.network;

import java.net.URL;

public class Request {

	private UrlAddress urlAddress;
	private String path;
	private String requestMethod;
	private String bodyString;
	private byte[] body;
	private String contentType;
	private int responseType;
	private int type;
	private int responseCode;
	private int rotationCount;
	private int attemptCount;
	private int priority;
	private boolean isFirstAttempt;
	private Object tag;
	private OnDownloadListener listener;
	
	public Request(UrlAddress urlAddress) {
		this.urlAddress = urlAddress;
		this.path = "";
		this.requestMethod = "GET";
		this.responseType = Response.JSON_TYPE;
		this.type = -1;
		this.priority = Downloader.DOWNLOADER_NORMAL_PRIORITY;
	}
	
	public Request(UrlAddress urlAddress, String path, String requestMethod, String bodyString, String contentType, int responseType, int type, Object tag, int priority) {
		this.urlAddress = urlAddress;
		this.path = path;
		this.requestMethod = requestMethod;
		this.bodyString = bodyString;
		this.contentType = contentType;
		this.responseType = responseType;
		this.type = type;
		this.tag = tag;
		this.priority = priority;
		init();
	}

	public void init() {
		this.rotationCount = this.urlAddress == null ? 0 : this.urlAddress.size();
		this.attemptCount = 1;
		this.responseCode = -1;
		this.isFirstAttempt = true;
	}

	public String getProtocol() {
		try {
			return new URL(this.urlAddress.getAddress() + this.path).getProtocol();
		} catch (Exception e) {
			return "";
		}
	}
	
	public UrlAddress getUrlAddress() {
		return urlAddress;
	}

	public void setUrlAddress(UrlAddress urlAddress) {
		this.urlAddress = urlAddress;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getRequestMethod() {
		return requestMethod;
	}

	public void setRequestMethod(String requestMethod) {
		this.requestMethod = requestMethod;
	}

	public byte[] getBody() {
		if (this.body == null && this.bodyString != null) {
			this.body = this.bodyString.getBytes();
		}
		return this.body;
	}

	public void setBody(byte[] body) {
		this.body = body;
	}
	
	public String getBodyString() {
		return this.bodyString;
	}
	
	public void setBodyString(String bodyString) {
		this.bodyString = bodyString;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public int getResponseType() {
		return responseType;
	}

	public void setResponseType(int responseType) {
		this.responseType = responseType;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getResponseCode() {
		return responseCode;
	}

	public void setResponseCode(int responseCode) {
		this.responseCode = responseCode;
	}

	public int getRotationCount() {
		return rotationCount;
	}

	public void setRotationCount(int rotationCount) {
		this.rotationCount = rotationCount;
	}

	public int getAttemptCount() {
		return attemptCount;
	}

	public void setAttemptCount(int attemptCount) {
		this.attemptCount = attemptCount;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public boolean isFirstAttempt() {
		return isFirstAttempt;
	}

	public void setFirstAttempt(boolean isFirstAttempt) {
		this.isFirstAttempt = isFirstAttempt;
	}

	public Object getTag() {
		return tag;
	}

	public void setTag(Object tag) {
		this.tag = tag;
	}
	
	public OnDownloadListener getListener() {
		return this.listener;
	}

	public void setListener(OnDownloadListener listener) {
		this.listener = listener;
	}
	
}