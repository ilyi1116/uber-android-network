/**
    UberAndroidNetwork: A JSON/XML network abstraction layer
    Copyright (c) 2011 by Jordan Bonnet, Uber Technologies

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in
    all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    THE SOFTWARE.
*/

package com.uber.network;

import java.util.ArrayList;

public abstract class DataNode {
	
	public abstract String getName();
	
	public abstract DataNode getNode(String name);
	
	public abstract ArrayList<DataNode> getChildren();
	
	public abstract ArrayList<DataNode> getArray();
	
	public abstract String getString(String defaultString);
	
	public abstract int getInt(int defaultInt);
	
	public abstract Long getLong(Long defaultLong);
	
	public abstract double getDouble(double defaultDouble);
	
	public abstract Boolean getBoolean(Boolean defaultBoolean);

	
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
	
	public Long findLong(String name, Long defaultLong) {
		final DataNode node = findNode(name);
		if (node != null) {
			return node.getLong(defaultLong);
		}
		return defaultLong;
	}
	
	public Boolean findBoolean(String name, Boolean defaultBoolean) {
		final DataNode node = findNode(name);
		if (node != null) {
			return node.getBoolean(defaultBoolean);
		}
		
		return defaultBoolean;
	}	
}
