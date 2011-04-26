package com.uber.network;

public interface OnDownloadListener {
	
	public void onPreLoad(int type);
	
	public void onLoad(Response response);
	
	public void onError(int type);
	
	public void onCancel();

}
