Uber Android Network
====================

Quick description
-----------------
This library encapsulates most server requests: image downloads, JSON or XML requests, HEAD requests, https or http. You don't have to worry about the type of the response (JSON, XML or Bitmap) as an abstraction layer wraps all of that for you. It is really useful when you want to migrate from XML to JSON, or the other way around (really?).


Usage
-----
You start with the Downloader


    public class MyDownloader {

        private OnDownloadListener mDownloadListener;

	    public void setOnDownloadListener(OnDownloadListener listener) {
                    mDownloadListener = listener;
		    if (mDownloader != null) {
			    mDownloader.setOnDownloadListener(listener);
		    }
	    }

	    public synchronized void send(UrlAddress urlAddress, String path, String postRequest, String contentType, int type, int responseType) {
	            if (mDownloadListener != null) {
			    if (mDownloader == null || mDownloader.getStatus() == AsyncTask.Status.FINISHED) {
				    mDownloader = new Downloader();
				    mDownloader.setOnDownloadListener(mDownloadListener);
			    }
			    mDownloader.addPost(urlAddress, path, postRequest, contentType, type, responseType);
		    }
	     }
     }
