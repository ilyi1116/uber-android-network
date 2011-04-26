package com.uber.network;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

public class UberHostnameVerifier implements HostnameVerifier {

	@Override
	public boolean verify(String hostname, SSLSession session) {
		// Accept all servers
		return true;
	}

}
