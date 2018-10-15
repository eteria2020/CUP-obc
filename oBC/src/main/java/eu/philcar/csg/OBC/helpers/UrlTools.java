package eu.philcar.csg.OBC.helpers;

import org.apache.http.NameValuePair;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import okhttp3.HttpUrl;

public class UrlTools {

	public static HttpUrl buildQuery(String urlstr, List<NameValuePair> paramsList) {

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

		if (paramsList != null) {
			for (NameValuePair param : paramsList) {
				urlBuilder.addQueryParameter(param.getName(), param.getValue());
			}
		}
		return urlBuilder.build();
	}

}
