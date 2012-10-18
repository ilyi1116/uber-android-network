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

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

public class JsonResponse extends Response {
	
	private DataNode mDataNode;
	
	public JsonResponse(String json, int responseCode) throws ResponseException, IOException {
		try {
			JSONObject jsonObject = null;
			if (json != null && json.length() > 0) {
				jsonObject = new JSONObject(json);
			}
			mDataNode = new JsonNode("", jsonObject);
		} catch (JSONException e) {
			throw new ResponseException(String.format("Response code: %d, could not create root JSON object:\n %s", responseCode, json));
		}
	}
	
	public DataNode getDataNode() {
		return mDataNode;
	}

}
