package com.uber.network;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

public class UberTrustManager implements X509TrustManager {

	@Override
	public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
		// Nothing to do
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		// Nothing to do
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return new java.security.cert.X509Certificate[] {};
	}
	
}
