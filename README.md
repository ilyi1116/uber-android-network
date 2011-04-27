Uber Android Network
====================

Quick description
-----------------
This library encapsulates most server requests: image downloads, JSON or XML requests, HEAD requests, https or http. You don't have to worry about the type of the response (JSON, XML or Bitmap) as an abstraction layer wraps all of that for you. It is really useful when you want to migrate from XML to JSON, or the other way around (really?).


Usage
-----
You start with the Downloader. This is an example of how to use it. Remember that it inherits from AsyncTask, so you have to re-instantiate it once it has finished doing its previous job.

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
	
The UrlAddress makes it possible to have a set of url strings to try subsequently if the server returns an error on a given one. First, it will try "http://www.uber.com", then "http://www.uberawesome.com", and finally "http://www.ubercool.fr", and loop back. If you don't want any rotations, add only one url string to it, that's fine. There is a constructor for that:

	final UrlAddress urlAddress = new UrlAddress("http://www.petitourson.com");
	

Then, depending on the response type, you can get:

* a bitmap using:

	final Bitmap bitmap = response.getBitmap();

* a structured response (JSON or XML) using


	final DataNode rootNode = response.getDataNode();
	if (rootNode != null) {
		final DataNode usernameNode = rootNode.findNode("user_name");
		if (usernameNode != null) {
			final String username = usernameNode.getString("default_name");
			// DO SOMETHING!!!
		}
	}

* a raw string using:

	final String rawResponse = response.getStringData();





