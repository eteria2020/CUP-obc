package eu.philcar.csg.OBC.server;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.GZIPInputStream;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.SystemControl;
import eu.philcar.csg.OBC.helpers.DLog;

@Deprecated
public class HttpConnector {
	private DLog dlog = new DLog(this.getClass());

	public static final int METHOD_GET = 0;
	public static final int METHOD_POST = 1;
	private static int exceptionCount = 0;

	public final boolean AUTH_ENABLED = false;
	public final String AUTH_USER = "";
	public final String AUTH_PASSWORD = "";
	public final String AUTH_DOMAIN = "";

	private final boolean COMPRESSION_ENABLED = true;

	private ReceiveDataTask receiveDataTask;
	public int HttpMethod = METHOD_GET;

	public HttpConnector(Context ctx) {
		receiveDataTask = new ReceiveDataTask(ctx);
	}

	private Handler _handler;

	public void SetHandler(Handler hnd) {
		_handler = hnd;
	}

	private Messenger _messenger;

	public void setMessenger(Messenger msgr) {
		_messenger = msgr;
	}

	public void Execute(RemoteEntityInterface entity) {
		if (entity == null) {
			dlog.e("Execute: entity==NULL");
			return;
		}

		dlog.i(entity.getClass().getName());

		receiveDataTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, entity);

	}

	public void WaitTermination() {
		try {
			receiveDataTask.get(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private class ReceiveDataTask extends AsyncTask<RemoteEntityInterface, Context, String> {

		private Context ctx;
		private String responseBody = "";
		private RemoteEntityInterface entity;
		private int EntityId;

		private long start;

		public ReceiveDataTask(Context ctx) {
			this.ctx = ctx;
		}

		@Override
		protected String doInBackground(final RemoteEntityInterface... entity) {
			start = System.currentTimeMillis();

			this.entity = entity[0];
			String URL = this.entity.GetRemoteUrl();

			if (URL == null) {
				dlog.e("ABORTED : URL is null, probably connection is down or connector is busy");
				return null;
			}

			if (this.entity.getDirection() == RemoteEntityInterface.eDirection.DOWNLOAD) {
				DownloadJson(URL, HttpMethod, this.entity.GetParams());
				//updateTime(responseBody);
				EntityId = this.entity.DecodeJson(responseBody);
			} else {
				String data = this.entity.EncodeJson();
				UploadJson(URL, data);
				EntityId = this.entity.MsgId();
			}

			dlog.d("Completed in : " + (System.currentTimeMillis() - start));

			return null;
		}

		protected void onProgressUpdate(Context... ctx) {
		}

		protected void onPostExecute(String result) {

			if (_handler != null) {
				Message msg = _handler.obtainMessage();
				msg.what = EntityId;
				msg.obj = entity; //can't remove previous message, may have empty new message
				_handler.sendMessageDelayed(msg, 3000);
				dlog.d("Sent message to handler ID " + EntityId);
			} else if (_messenger != null) {
				Message msg = new Message();
				msg.what = EntityId;
				msg.obj = entity;
				try {
					_messenger.send(msg);
				} catch (RemoteException e) {
				}
				dlog.d("Sent message to messenger ID " + EntityId);
			} else {
				dlog.d("No notify handler");
			}

		}

		private String DeGzip(String str) {
			String outStr = "";

			GZIPInputStream gis;
			try {
				gis = new GZIPInputStream(new ByteArrayInputStream(str.getBytes()));
				BufferedReader bf = new BufferedReader(new InputStreamReader(gis));

				String line;
				while ((line = bf.readLine()) != null) {
					outStr += line;
				}
				bf.close();
			} catch (UnsupportedEncodingException e) {
				dlog.e("DeGzip", e);

			} catch (IOException e) {
				dlog.e("DeGzip", e);
			}

			return outStr;
		}

		private class GzipDecompressingEntity extends HttpEntityWrapper {
			public GzipDecompressingEntity(final HttpEntity entity) {
				super(entity);
			}

			@Override
			public InputStream getContent() throws IOException, IllegalStateException {
				InputStream wrappedin = wrappedEntity.getContent();
				return new GZIPInputStream(wrappedin);
			}

			@Override
			public long getContentLength() {
				// length of ungzipped content is not known
				return -1;
			}
		}

		public String DownloadJson(String url) {
			return DownloadJson(url, METHOD_GET);
		}

		public String DownloadJson(String url, int method) {
			return DownloadJson(url, method, null);
		}

		public String DownloadJson(String url, int method, List<NameValuePair> paramsList) {

			final HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParams, App.ConnectionTimeout);
			HttpConnectionParams.setStaleCheckingEnabled(httpParams, true);
			HttpConnectionParams.setSoTimeout(httpParams, App.ConnectionTimeout);
			StringBuilder logtxt = null;
			DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);

			Locale locale = ctx.getResources().getConfiguration().locale;

			if (AUTH_ENABLED) {
				UsernamePasswordCredentials authCredentials = new UsernamePasswordCredentials(AUTH_USER, AUTH_PASSWORD);
				httpclient.getCredentialsProvider().setCredentials(new AuthScope(AUTH_DOMAIN, 443), authCredentials);
				dlog.d("Http auth enabled");
			}

			HttpRequestBase httpRequest;

			if (method == METHOD_POST) {
				httpRequest = new HttpPost(url);
				if (paramsList != null)
					try {
						((HttpPost) httpRequest).setEntity(new UrlEncodedFormEntity(paramsList));
					} catch (UnsupportedEncodingException e) {
						dlog.e("setEntity:", e);
					}
				dlog.d("HTTP POST : " + url);
			} else {
				String params = "";
				if (paramsList != null)
					params = URLEncodedUtils.format(paramsList, "utf-8");

				httpRequest = new HttpGet(url + params);
				dlog.d("HTTP GET :" + url + params);

			}

			if (COMPRESSION_ENABLED) {
				httpRequest.addHeader("Accept-Encoding", "gzip");
				dlog.d("HTTP compression enabled");
			}

			try {

				long timer = System.currentTimeMillis();

				HttpResponse response = httpclient.execute(httpRequest);
				App.networkExceptions = 0;

				int responseCode = response.getStatusLine().getStatusCode();
				logtxt = new StringBuilder("Got HttpResponse (" + responseCode + ") in :" + (System.currentTimeMillis() - timer) + " ms");

				switch (responseCode) {

					case 200:
						exceptionCount = 0;
						HttpEntity entity = response.getEntity();
						logtxt.append("- Content length: ").append(entity.getContentLength());

						Header encoding = entity.getContentEncoding();

						if (encoding != null) {
							for (HeaderElement element : encoding.getElements()) {
								if (element.getName().equalsIgnoreCase("gzip")) {
									response.setEntity(new GzipDecompressingEntity(response.getEntity()));
									entity = response.getEntity();
									logtxt.append(" - Compressed entity");
									break;
								}
							}
						}

						if (entity != null) {
							responseBody = EntityUtils.toString(entity);
							logtxt.append(" Response body length: ").append(responseBody.length());
						}
						break;

					default:
						responseBody = "";
						dlog.e("HTTP ERROR : " + responseCode);
						break;
				}

			} catch (ClientProtocolException e) {
				dlog.e("Http ClientProtocolException", e);
			} catch (java.net.UnknownHostException e) {
				App.networkExceptions++;
				if (App.networkExceptions % 100 == 0) {
					dlog.e("Network exceptions: " + App.networkExceptions, e);
				}
			} catch (IOException e) {
				App.networkExceptions++;
				if (App.networkExceptions % 100 == 0) {
					dlog.e("Network exceptions: " + App.networkExceptions);
				}
				dlog.e("Http IOException", e);
				if (System.currentTimeMillis() - App.lastConnReset > 10 * 60 * 1000 && exceptionCount++ > 15) {
					exceptionCount = 0;
					App.lastConnReset = System.currentTimeMillis();
					dlog.d("Reset 3g Connection exception");
					SystemControl.Reset3G(null);
				}
			} catch (Exception e) {
				dlog.e("Http Unexpected exception", e);
			} finally {
				httpclient.getConnectionManager().shutdown();
			}

			if (logtxt != null)
				dlog.d(logtxt.toString());
			return responseBody;

		}

		public String UploadJson(String url, String data) {

			DefaultHttpClient httpclient = new DefaultHttpClient();

			Locale locale = ctx.getResources().getConfiguration().locale;

//		 		UsernamePasswordCredentials authCredentials = new UsernamePasswordCredentials("mnbzxc124","plmokn120.Qaz");
//		 		
//		 		httpclient.getCredentialsProvider().setCredentials(new AuthScope("www.fvgsnow.it",443), authCredentials);

			HttpPost httpPost = new HttpPost(url);
//		 		HttpGet  httpGet = new HttpGet(url);
			DLog.D(url);

			try {

				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
				nameValuePairs.add(new BasicNameValuePair("data", data));

//		 	        httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs,HTTP.UTF_8));
				httpPost.setEntity(new StringEntity(data));
				httpPost.setHeader("Accept", "application/json");
				httpPost.setHeader("Content-type", "application/json");

				//
//		 	        String params = URLEncodedUtils.format(nameValuePairs, "utf-8");

				// Execute HTTP Post Request
				HttpResponse response = httpclient.execute(httpPost);
				//HttpResponse response = httpclient.execute(httpGet);

				int responseCode = response.getStatusLine().getStatusCode();

				responseBody = "";

				switch (responseCode) {
					case 200:
						HttpEntity entity = response.getEntity();
						if (entity != null) {
							responseBody = EntityUtils.toString(entity);
						}
						break;

					default:
						responseBody = "";
						DLog.E("HTTP ERROR : " + responseCode);
						break;
				}

			} catch (ClientProtocolException e) {
				DLog.E(e.toString());
			} catch (IOException e) {
				DLog.E(e.toString());
			} catch (Exception e) {
				DLog.E("Http Unexpected exception", e);
			} finally {
				httpclient.getConnectionManager().shutdown();
			}

			return responseBody;

		}

	}
}
