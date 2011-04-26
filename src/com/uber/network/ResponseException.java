package com.uber.network;

public class ResponseException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public ResponseException() {
		super();
	}
	
	public ResponseException(String detailMessage) {
		super(detailMessage);
	}

}
