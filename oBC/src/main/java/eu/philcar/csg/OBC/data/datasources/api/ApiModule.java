package eu.philcar.csg.OBC.data.datasources.api;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import dagger.Module;
import dagger.Provides;
import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.data.common.SerializationExclusionStrategy;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.injection.ApplicationContext;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Fulvio on 15/02/2018.
 */

@Module
public class ApiModule {

	@Provides
	@Singleton
	SharengoApi provideSharengoApi(@ApplicationContext Context context) {

		Gson gson = new GsonBuilder()
				.addSerializationExclusionStrategy(new SerializationExclusionStrategy())
				.create();

		Retrofit retrofit = new Retrofit.Builder()
				.baseUrl(context.getString(R.string.endpointSharengo))
				//.baseUrl("http:gr3dcomunication.com/sharengo/")
				.client(provideOkHttpClientTrusted(context))
				.addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
				.addConverterFactory(GsonConverterFactory.create(gson))
				.build();

		return retrofit.create(SharengoApi.class);
	}

	@Provides
	@Singleton
	SharengoPhpApi provideSharengoPhpApi(@ApplicationContext Context context) {

       /* HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
		if (BuildConfig.DEBUG) {
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        } else {
            logging.setLevel(HttpLoggingInterceptor.Level.NONE);
        }

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(logging);*/

		Gson gson = new GsonBuilder()
				.addSerializationExclusionStrategy(new SerializationExclusionStrategy())
				.create();

		Retrofit retrofit = new Retrofit.Builder()
				.baseUrl(context.getString(R.string.endpointSharengoPhp))
				//.baseUrl("http:gr3dcomunication.com/sharengo/")
				.client(provideOkHttpClient())
				.addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
				.addConverterFactory(GsonConverterFactory.create(gson))
				.build();

		return retrofit.create(SharengoPhpApi.class);
	}

	@Provides
	@Singleton
	SharengoBeaconApi provideSharengoBeaconApi(@ApplicationContext Context context) {

		Gson gson = new GsonBuilder()
				.addSerializationExclusionStrategy(new SerializationExclusionStrategy())
				.create();

		Retrofit retrofit = new Retrofit.Builder()
				.baseUrl(context.getString(R.string.endpointSharengoBeacon))
				//.baseUrl("http:gr3dcomunication.com/sharengo/")
				.client(provideOkHttpClient())
				.addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
				.addConverterFactory(GsonConverterFactory.create(gson))
				.build();

		return retrofit.create(SharengoBeaconApi.class);
	}

	@Provides
	@Singleton
	OkHttpClient provideOkHttpClientTrusted(Context context) {
		/*HttpLogger logger = new HttpLogger();
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(logger){

        };
            logging.setLevel(HttpLoggingInterceptor.Level.HEADERS);*/

		LoggerInterceptor logging = new LoggerInterceptor();

		OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
		httpClient.addInterceptor(logging);

		try {

			final TrustManager[] trustAllCerts = new TrustManager[]{
					new X509TrustManager() {
						@Override
						public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {

						}

						@Override
						public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {

						}

						@Override
						public java.security.cert.X509Certificate[] getAcceptedIssuers() {
							return new java.security.cert.X509Certificate[]{};
						}
					}
			};

			CertificateFactory cf = CertificateFactory.getInstance("X509");
			Certificate caServer;
			//                    context.getResources().openRawResource(R.raw.client);
			InputStream cert2 = context.getAssets().open("ca.cer");
			try (InputStream cert = context.getAssets().open("client_car1.p12")) {
				// Key
				KeyStore keyStore = KeyStore.getInstance("PKCS12");
				keyStore.load(cert, "x9aStp:k4:".toCharArray());

				KeyManagerFactory kmf = KeyManagerFactory.getInstance("X509");
				kmf.init(keyStore, "x9aStp:k4:".toCharArray());
				KeyManager[] keyManagers = kmf.getKeyManagers();

				// Trust
				caServer = cf.generateCertificate(cert2);
				KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
				trustStore.load(null, null);
				trustStore.setCertificateEntry("ca", caServer);
				TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
				tmf.init(trustStore);
				TrustManager[] trustManagers = tmf.getTrustManagers();
				SSLContext sslContext = SSLContext.getInstance("SSL");
				sslContext.init(keyManagers, trustAllCerts, null);

				httpClient.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0]);
			}
		} catch (Exception e) {
			DLog.E("Exception while providing OKHTTPCLIENTTrusted", e);
		}

        /*httpClient.addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request original = chain.request();
                HttpUrl originalHttpUrl = original.url();

                HttpUrl url = originalHttpUrl.newBuilder()
                        //.addQueryParameter("apikey", "your-actual-api-key")
                        .build();

                // Request customization: add request headers
                Request.Builder requestBuilder = original.newBuilder()
                        .url(url)
                        .header("Authorization", Credentials.basic("francesco.galatro@gmail.com", "508c82b943ae51118d905553b8213c8a"));

                Request request = requestBuilder.build();
                return chain.proceed(request);
            }
        });*/

		httpClient.hostnameVerifier(new HostnameVerifier() {
										@Override
										public boolean verify(String hostname, SSLSession session) {
											return true;
										}
									}
		);
		httpClient.readTimeout(40, TimeUnit.SECONDS);
		httpClient.connectTimeout(40, TimeUnit.SECONDS);
		httpClient.writeTimeout(40, TimeUnit.SECONDS);
		httpClient.retryOnConnectionFailure(true);

		return httpClient.build();
	}

	@Provides
	@Singleton
	OkHttpClient provideOkHttpClient() {
/*        HttpLogger logger = new HttpLogger();
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(logger);
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);*/
		LoggerInterceptor logging = new LoggerInterceptor();
		OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
		httpClient.addInterceptor(logging);



        /*httpClient.addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request original = chain.request();
                HttpUrl originalHttpUrl = original.url();

                HttpUrl url = originalHttpUrl.newBuilder()
                        //.addQueryParameter("apikey", "your-actual-api-key")
                        .build();

                // Request customization: add request headers
                Request.Builder requestBuilder = original.newBuilder()
                        .url(url)
                        .header("Authorization", Credentials.basic("francesco.galatro@gmail.com", "508c82b943ae51118d905553b8213c8a"));

                Request request = requestBuilder.build();
                return chain.proceed(request);
            }
        });*/

		httpClient.readTimeout(20, TimeUnit.SECONDS);
		httpClient.connectTimeout(20, TimeUnit.SECONDS);
		//httpClient.retryOnConnectionFailure(false);

		return httpClient.build();
	}

}
