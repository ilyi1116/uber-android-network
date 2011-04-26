package com.uber.network;

import java.util.ArrayList;

public abstract class DataNode {
	
	public abstract String getName();
	
	public abstract DataNode getNode(String name);
	
	public abstract ArrayList<DataNode> getChildren();
	
	public abstract ArrayList<DataNode> getArray();
	
	public abstract String getString(String defaultString);
	
	public abstract int getInt(int defaultInt);
	
	public abstract double getDouble(double defaultDouble);
	
	public String getAttribute(String attributeName, String defaultValue) {
		return defaultValue;
	}
	
	public DataNode findNode(String name) {
		final ArrayList<DataNode> children = getChildren();
		for (DataNode child : children) {
			if (child.getName().equals(name)) {
				return child;
			}
			final DataNode node = child.findNode(name);
			if (node != null) {
				return node;
			}
		}
		return null;
	}
	
	public int findInt(String name, int defaultInt) {
		final DataNode node = findNode(name);
		if (node != null) {
			return node.getInt(defaultInt);
		}
		return defaultInt;
	}
	
	public double findDouble(String name, double defaultDouble) {
		final DataNode node = findNode(name);
		if (node != null) {
			return node.getDouble(defaultDouble);
		}
		return defaultDouble ;
	}
	
	public String findString(String name, String defaultString) {
		final DataNode node = findNode(name);
		if (node != null) {
			return node.getString(defaultString);
		}
		return defaultString;
	}
	
}
