package com.uber.network;

import java.util.ArrayList;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XmlNode extends DataNode {
	
	private Node mNode;
	
	public XmlNode(Node node) {
		mNode = node;
	}

	@Override
	public String getName() {
		return mNode.getNodeName();
	}

	@Override
	public DataNode getNode(String name) {
		final ArrayList<DataNode> dataNodes = getArray();
		for (DataNode dataNode : dataNodes) {
			if (dataNode.getName().equals(name)) {
				return dataNode;
			}
		}
		return null;
	}
	
	@Override
	public ArrayList<DataNode> getChildren() {
		return getArray();
	}

	@Override
	public ArrayList<DataNode> getArray() {
		final ArrayList<DataNode> dataNodes = new ArrayList<DataNode>();
		final NodeList nodes = mNode.getChildNodes();
		for (int i = 0; i < nodes.getLength(); ++i) {
			dataNodes.add(new XmlNode(nodes.item(i)));
		}
		return dataNodes;
	}

	@Override
	public String getString(String defaultString) {
		if (mNode.hasChildNodes()) {
			return mNode.getFirstChild().getNodeValue();
		}
		return defaultString;
	}

	@Override
	public int getInt(int defaultInt) {
		if (mNode.hasChildNodes()) {
			return Integer.valueOf(mNode.getFirstChild().getNodeValue());
		}
		return defaultInt;
	}

	@Override
	public double getDouble(double defaultDouble) {
		if (mNode.hasChildNodes()) {
			return Double.valueOf(mNode.getFirstChild().getNodeValue());
		}
		return defaultDouble;
	}
	
	@Override
	public String getAttribute(String attributeName, String defaultValue) {
		if (mNode.hasAttributes()) {
			final Node attributeNode = mNode.getAttributes().getNamedItem(attributeName);
			if (attributeNode != null) {
				return attributeNode.getNodeValue();
			}
		}
		return defaultValue;
	}
	
}
