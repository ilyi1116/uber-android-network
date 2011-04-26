package com.uber.network;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

public class JsonResponse extends Response {
	
	private DataNode mDataNode;
	
	public JsonResponse(String json) throws ResponseException, IOException {
		try {	
			final JSONObject jsonObject = new JSONObject(json);
			mDataNode = new JsonNode("", jsonObject);
		} catch (JSONException e) {
			throw new ResponseException("Could not create root JSON object:\n" + json);
		}
	}
	
	public DataNode getDataNode() {
		return mDataNode;
	}

}
