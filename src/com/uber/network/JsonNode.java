package com.uber.network;

import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonNode extends DataNode {
	
	private String mName;
	private Object mValue;
	
	public JsonNode(String name, Object value) {
		mName = name;
		mValue = value;
	}
	
	@Override
	public String getName() {
		return mName;
	}

	@Override
	public DataNode getNode(String name) {
		DataNode dataNode = null;
		try {
			if (mValue instanceof JSONObject) {
				dataNode = new JsonNode(name, ((JSONObject) mValue).get(name));
			}
		} catch (JSONException e) {
			dataNode = null;
		}
		return dataNode;
	}
	
	@Override
	public ArrayList<DataNode> getChildren() {
		final ArrayList<DataNode> dataNodes = new ArrayList<DataNode>();
		if (mValue instanceof JSONArray) {
			dataNodes.addAll(getArray());
		} else if (mValue instanceof JSONObject) {
			final JSONObject jsonObject = (JSONObject) mValue;
			final Iterator<?> keys = jsonObject.keys();
			while (keys.hasNext()) {
				final String key = (String) keys.next();
				final DataNode node = getNode(key);
				if (node != null) {
					dataNodes.add(node);
				}
			}
		}
		return dataNodes;
	}

	@Override
	public ArrayList<DataNode> getArray() {
		final ArrayList<DataNode> dataNodes = new ArrayList<DataNode>();
		try {
			if (mValue instanceof JSONArray) {
				final JSONArray jsonArray = (JSONArray) mValue;
				for (int i = 0; i < jsonArray.length(); ++i) {
					dataNodes.add(new JsonNode("", jsonArray.get(i)));
				}
			}
		} catch (JSONException e) {
			// Nothing else to do
		}
		return dataNodes;
	}
	
	@Override
	public String getString(String defaultString) {
		if (mValue != null) {
			return mValue.toString();
		}
		return defaultString;
	}
	
	@Override
	public int getInt(int defaultInt) {
		if (mValue instanceof Integer) {
			return ((Integer) mValue).intValue();
		}
		return defaultInt;
	}
	
	@Override
	public double getDouble(double defaultDouble) {
		if (mValue instanceof Double) {
			return ((Double) mValue).doubleValue();
		}
		return defaultDouble;
	}

}
