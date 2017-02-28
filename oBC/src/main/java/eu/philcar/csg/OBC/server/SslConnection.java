package eu.philcar.csg.OBC.server;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;




import android.content.Context;

public class SslConnection {
	private static KeyStore keyStore;
	private static SSLContext sslContext;
	private static PublicKey  key;
	
	
	public  static void init(Context ctx) {
		try {
			keyStore = KeyStore.getInstance("PKCS12");
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//InputStream is = new ByteArrayInputStream(key.getBytes());
		
		try {
			InputStream is = ctx.getAssets().open("client_car1.p12");
			keyStore.load(is, "x9aStp:k4:".toCharArray());
			is.close();
			
			is = ctx.getAssets().open("ca.cer");
			Certificate cert = CertificateFactory.getInstance("X.509").generateCertificate(is);
			is.close();
			keyStore.setCertificateEntry("ca", cert);
			key = cert.getPublicKey();
			
			
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("X509");
			kmf.init(keyStore,null);
			
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(keyStore);
			
			KeyManager[] keyManagers = kmf.getKeyManagers();
			TrustManager[] trustManagers = tmf.getTrustManagers();
			
			sslContext = SSLContext.getInstance("TLS");
			sslContext.init(keyManagers, trustManagers, null);
			
			
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnrecoverableKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
		//HttpsURLConnection.setDefaultHostnameVerifier(new LocalHostnameVerifier());
		
	}
	
	public static SSLContext  getSSLContext() {
		return sslContext;
	}
	
	public static String  HttpsRequest(String url) {
		String result;
		HttpURLConnection urlConnection;
		SslConnection dummy= new SslConnection();
		
		try {
			URL requestUrl = new URL(url);
			urlConnection = (HttpURLConnection) requestUrl.openConnection();
			if (urlConnection instanceof HttpsURLConnection) {
				((HttpsURLConnection)urlConnection).setSSLSocketFactory(sslContext.getSocketFactory());
				((HttpsURLConnection)urlConnection).setHostnameVerifier(dummy.new LocalHostnameVerifier());

				
			}

			urlConnection.setRequestMethod("GET");
			urlConnection.setConnectTimeout(3000);
			urlConnection.setReadTimeout(3000);
			
			urlConnection.connect();
			
			int responseCode = urlConnection.getResponseCode();
			String encoding =  urlConnection.getContentEncoding();
			
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
			
			String line;
			StringBuilder content = new StringBuilder();
			while ((line = bufferedReader.readLine()) != null) {
				content.append(line).append("\n");
			}
			bufferedReader.close();	
			
			return content.toString();
			
		      
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
			
	}
	
	public class LocalHostnameVerifier implements HostnameVerifier {

		@Override
		public boolean verify(String hostname, SSLSession session) {
			
			return true;
		}
		
	}
	
	
}
