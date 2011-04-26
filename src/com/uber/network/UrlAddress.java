package com.uber.network;

import java.util.ArrayList;

public class UrlAddress {
	
	private ArrayList<String> mAddresses = new ArrayList<String>();
	
	public UrlAddress() {
	}
	
	public UrlAddress(String server) {
		addAddress(server);
	}
	
	public UrlAddress(String host, int port) {
		addAddress(host, port);
	}
	
	public void addAddresses(ArrayList<String> servers) {
		mAddresses.addAll(servers);
	}
	
	public void addAddress(String server) {
		mAddresses.add(server);
	}
	
	public void addAddress(String host, int port) {
		mAddresses.add("http://" + host + ":" + port);
	}
	
	public int size() {
		return mAddresses.size();
	}
	
	public void clear() {
		mAddresses.clear();
	}
	
	public String getAddress(int index) {
		if (mAddresses.size() > 0) {
			return mAddresses.get(index);
		}
		return "";
	}
	
	public String getAddress() {
		return getAddress(0);
	}
	
	public void rotateAddress() {
		if (mAddresses.size() > 0) {
			mAddresses.add(mAddresses.remove(0));
		}
	}
	
}
