package eu.philcar.csg.OBC.server;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.SystemControl;
import eu.philcar.csg.OBC.helpers.DLog;
import okhttp3.Authenticator;
import okhttp3.Call;
import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;

@Deprecated
public class HttpsConnector {
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

	public HttpsConnector(Context ctx) {
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

		dlog.i("Execute:" + entity.getClass().getName());
		receiveDataTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, entity);

	}

	private class ReceiveDataTask extends AsyncTask<RemoteEntityInterface, Context, String> {

		private Context ctx;
		private RemoteEntityInterface entity;
		private String responseBody = "";
		private int EntityId;

		private long start;

		public ReceiveDataTask(Context ctx) {
			this.ctx = ctx;
		}

		@Override
		protected String doInBackground(RemoteEntityInterface... entity) {
			start = System.currentTimeMillis();

			this.entity = entity[0];

			String URL = this.entity.GetRemoteUrl();

			if (URL == null) {
				dlog.e("ABORTED : URL is null, probably connection is down or connector is busy");
				return null;
			}

			if (this.entity.getDirection() == RemoteEntityInterface.eDirection.DOWNLOAD) {
				DownloadJson(URL, HttpMethod, this.entity.GetParams());
				//updateTime();
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
				msg.obj = entity;
				_handler.sendMessage(msg);
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

		public String DownloadJson(String url) {
			return DownloadJson(url, METHOD_GET);
		}

		public String DownloadJson(String url, int method) {
			return DownloadJson(url, method, null);
		}

		public HttpUrl buildQuery(String urlstr, int method, List<NameValuePair> paramsList) {

			HttpUrl.Builder urlBuilder = new HttpUrl.Builder();
			URL url;
			try {
				url = new URL(urlstr);
			} catch (MalformedURLException e) {
				return null;
			}
			urlBuilder.scheme(url.getProtocol());
			urlBuilder.host(url.getHost());
			if (url.getPort() > 0)
				urlBuilder.port(url.getPort());

			String path = url.getPath();
			if (path != null)
				urlBuilder.encodedPath(path);

			String query = url.getQuery();
			if (query != null)
				urlBuilder.query(query);

			if (paramsList != null && method == METHOD_GET) {
				for (NameValuePair param : paramsList) {
					urlBuilder.addQueryParameter(param.getName(), param.getValue());
				}
			}
			return urlBuilder.build();
		}

		public void addPostBody(Builder builder, List<NameValuePair> paramsList) {

			if (paramsList != null) {
				FormBody.Builder formBodyBuilder = new FormBody.Builder();
				for (NameValuePair param : paramsList) {
					formBodyBuilder.add(param.getName(), param.getValue());
				}
				RequestBody formBody = formBodyBuilder.build();
				builder.post(formBody);
			}
		}

		public Authenticator getAuthenticator() {
			return new Authenticator() {

				@Override
				public Request authenticate(Route rt, Response res) throws IOException {
					String credential = Credentials.basic("CSDEMO01", "");
					Request req = res.request().newBuilder().header("Authorization", credential).build();
					return req;
				}
			};

		}

		public String DownloadJson(String urlstr, int method, List<NameValuePair> paramsList) {

			//Build client connection
			OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

			clientBuilder.sslSocketFactory(SslConnection.getSSLContext().getSocketFactory());
			clientBuilder.authenticator(getAuthenticator());
			clientBuilder.connectTimeout(App.ConnectionTimeout, TimeUnit.MILLISECONDS);
			clientBuilder.readTimeout(App.ConnectionTimeout, TimeUnit.MILLISECONDS);
			OkHttpClient client = clientBuilder.build();

			HttpUrl url = buildQuery(urlstr, method, paramsList);

			// Build request
			Builder builder = new Request.Builder();

			builder.url(url);
			dlog.d("HTTPS URL :" + url);
			if (method == METHOD_POST) {
				addPostBody(builder, paramsList);

				//dlog.d("HTTP post params :" + url + paramsList.toString());
			}
			// Add pre-authorization
			String credential = Credentials.basic(App.CarPlate, App.CarPlate);
			builder.addHeader("Authorization", credential);

			Request request = builder.build();

			//Build server call
			Call call = client.newCall(request);
			Response response = null;
			try {
				response = call.execute();
			} catch (IOException e) {

				App.networkExceptions++;
				if (App.networkExceptions % 100 == 0) {
					dlog.e("getting http/s", e);
					dlog.e("Network exception: " + App.networkExceptions);
				}
				if (System.currentTimeMillis() - App.lastConnReset > 10 * 60 * 1000 && exceptionCount++ > 15) {
					exceptionCount = 0;
					App.lastConnReset = System.currentTimeMillis();
					dlog.d("Reset 3g Connection exception");
					SystemControl.Reset3G(null);
				}
				return null;
			}

			try {
				if (response.code() == 200) {
					responseBody = response.body().string();
					exceptionCount = 0;
				} else {
					dlog.e("HTTP ERROR : " + response.code());
				}
			} catch (IOException e) {
				response.body().close();
				dlog.e("Getting responseBody", e);
				return null;
			}

			response.body().close();
			App.networkExceptions = 0;

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
			}

			return responseBody;

		}

		void updateTime() {
			if (responseBody == null || responseBody.isEmpty()) {
				dlog.e("Empty response");
				return;
			}

			final JSONArray jArray;
			try {
				jArray = new JSONArray(responseBody);
			} catch (JSONException e) {
				dlog.e("Errore estraendo array json", e);
				return;
			}
		}

	}
}
