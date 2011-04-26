package com.uber.network;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class XmlResponse extends Response {
	
	private DataNode mDataNode;
	
	public XmlResponse(InputStream data) throws ResponseException {
		try {
			final DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			final Document doc = documentBuilder.parse(data);
			mDataNode = new XmlNode(doc);
		} catch (ParserConfigurationException e) {
			throw new ResponseException();
		} catch (FactoryConfigurationError e) {
			throw new ResponseException();
		} catch (SAXException e) {
			throw new ResponseException();
		} catch (IOException e) {
			throw new ResponseException();
		}
	}
	
	public DataNode getDataNode() {
		return mDataNode;
	}

}
	