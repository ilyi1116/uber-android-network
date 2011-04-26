package com.uber.network;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class ImageResponse extends Response {
	
	private Bitmap mBitmap;
	
	public ImageResponse(InputStream data) throws ResponseException {
        try {
        	BufferedInputStream bis = new BufferedInputStream(data); 
    		mBitmap = BitmapFactory.decodeStream(bis); 
			bis.close();
		} catch (IOException e) {
			throw new ResponseException();
		} 
	}

	@Override
	public Bitmap getBitmap() {
		return mBitmap;
	}
	
}
