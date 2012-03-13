package com.uber.network;

public class UADownloadItem {
	
	private Request request;
	private Response response;
	private ResponseException exception;
	
	public UADownloadItem(Request request) {
		this.request = request;
	}
	
	public Request getRequest() {
		return request;
	}
	
	public Response getResponse() {
		return response;
	}
	
	public void setResponse(Response response) {
		this.response = response;
	}
	
	public ResponseException getException() {
		return exception;
	}
	
	public void setException(ResponseException exception) {
		this.exception = exception;
	}
}
