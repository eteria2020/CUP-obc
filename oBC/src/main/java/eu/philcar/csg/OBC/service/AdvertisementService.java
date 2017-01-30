package eu.philcar.csg.OBC.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.helpers.DLog;
import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class AdvertisementService extends IntentService {
	
	public static final long ADVERTISEMENT_UPDATE_PERIOD = 1000*60*60*24;	// milliseconds
	public static final int ADVERTISEMENT_UPDATE_RANGE = 180; 				// minutes;
	
	public static final String ACTION_UPDATE_ADVERTISEMENT = "eu.philcar.csg.update_advertisment";
	
	public static final String PARAM_AUTENTICATION_ID = "eu.philcar.csg.autentication.id";

	@SuppressLint("SdCardPath")
	private final String BANNER_FOLDER_ABSOLUTE_PATH = "/sdcard/csg/ads/"; 
	
	public AdvertisementService() {
		super(AdvertisementService.class.getName());
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		
		String action = intent.getAction();
		if (action == null) {
			return;
		}
		
		if (action.equalsIgnoreCase(ACTION_UPDATE_ADVERTISEMENT)) {
			
			Log.e(AdvertisementService.class.getCanonicalName(), "AdvertisementService: starting advertisement update");
			
			initDirectory();
			
			String id = intent.getStringExtra(PARAM_AUTENTICATION_ID);
			if (id == null || id.length() <= 0) {
				DLog.E("AdvertisementService-onHandleIntent: no ID provided for WS authentication");
				return;
			}
			
			ResponseWrapper rw = downloadAdvertisementList(id);
			if (rw == null || rw.getStatusCode() != 200) {
				return;
			}
			
			BannerWrapper[] banner = parseAdvertisementListResponse(rw.getResponse());
			if (banner == null || banner.length <= 0) {
				return;
			}
			
			File aBanner;
			for (int i=0; i<banner.length; i++) {
				
				aBanner = new File(BANNER_FOLDER_ABSOLUTE_PATH, banner[i].getFilename());
				
				if (aBanner != null && aBanner.exists()) {
					
					byte[] byteFile = toByte(aBanner);
					if (byteFile == null) {
						continue;
					}
					
					String md5sum = MD5(byteFile);
					if (md5sum == null) {
						continue;
					}
					
					if (!banner[i].getMd5Sum().equals(md5sum)) {
						
						ResponseWrapper downloadRW = downloadBannerFile(id, banner[i].getFilename());
						if (downloadRW == null || downloadRW.getStatusCode() != 200) {
							Log.e(AdvertisementService.class.getCanonicalName(), "banner (" + banner[i].getFilename() +"): unable to download after MD5 mismatch");
							continue;
						} else {
							Log.e(AdvertisementService.class.getCanonicalName(), "banner (" + banner[i].getFilename() +"): downloaded with success after MD5 mismatch");
						}
					} else {
						Log.e(AdvertisementService.class.getCanonicalName(), "banner (" + banner[i].getFilename() +"): no need to download, MD5 hasn't changed");
					}
						
				} else {
					
					ResponseWrapper downloadRW = downloadBannerFile(id, banner[i].getFilename());
					if (downloadRW == null || downloadRW.getStatusCode() != 200) {
						Log.e(AdvertisementService.class.getCanonicalName(), "banner (" + banner[i].getFilename() +"): unable to download after file doesn't exist");
						continue;
					} else {
						Log.e(AdvertisementService.class.getCanonicalName(), "banner (" + banner[i].getFilename() +"): downloaded with success after file doesn't exist");
					}
				}
			}
			
			// Persist last synchronization time
			SharedPreferences sharedPreferences = getSharedPreferences(App.COMMON_PREFERENCES, Context.MODE_PRIVATE);
			if (sharedPreferences != null) {
				Editor editor = sharedPreferences.edit();
				editor.putLong(App.KEY_LastAdvertisementListDownloaded, System.currentTimeMillis());
				editor.commit();
			}
			
			// Remove unused banners
			File[] existingBanner = computeAdList();
			if (existingBanner == null) {
				return;
			}
			
			for (int i=existingBanner.length-1; i>=0; i--) {
				
				File anExistingBanner = existingBanner[i];
				
				String fileName = anExistingBanner.getName();
				
				boolean found = false;
				for (int j=0; j<banner.length; j++) {
					BannerWrapper aBannerWrapper = banner[j];
					
					if (aBannerWrapper.getFilename().equals(fileName)) {
						found = true;
						break;
					}
				}
				
				if (!found) {
					Log.e(AdvertisementService.class.getCanonicalName(), "banner (" + anExistingBanner.getName() +"): will be deleted since it's not in use anymore");
					if (!anExistingBanner.delete()) {
						DLog.W("AdvertisementService-onHandleIntent: unable to delete file " + fileName);
					}
				}
			}
			
			// Notify activity
			Intent notifyIntent = new Intent();
			notifyIntent.setAction(AdvertisementService.ACTION_UPDATE_ADVERTISEMENT);
			sendBroadcast(notifyIntent);
		}
	}
	
	private void initDirectory() {
		
		// Ads
		File adsFolder = new File(BANNER_FOLDER_ABSOLUTE_PATH);
		
		if (!adsFolder.exists()) {
			adsFolder.mkdirs();
		}
	}
	
	private ResponseWrapper downloadAdvertisementList(String id) {
		
		HttpURLConnection urlConnection = null;
		
		try {
			URL url = new URL("http://31.220.50.249:9856/twist/advertisement.php");
			
			urlConnection = (HttpURLConnection)url.openConnection();
			
			urlConnection.setDoOutput(true);
		    urlConnection.setChunkedStreamingMode(0);
		    
		    OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
		    out.write( ("id="+id).getBytes() );
		    out.flush();
		    out.close();
			
		    int statusCode = urlConnection.getResponseCode();
		    if (statusCode != 200) {
		    	DLog.E("AdvertisementService-downloadData: response code " + statusCode);
		    	return new ResponseWrapper(statusCode, null);
		    }
		    
		    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
		    
		    byte[] contents = new byte[1024];

		    int bytesRead=0;
		    String strFileContents = "";
		    while( (bytesRead = in.read(contents)) != -1){ 
		       strFileContents = new String(contents, 0, bytesRead);               
		    }
		    
		    return new ResponseWrapper(statusCode, strFileContents);
		    
		} catch (MalformedURLException e) {
			e.printStackTrace();
			DLog.E("AdvertisementService-downloadData: " + e.toString());
		} catch (IOException e) {
			e.printStackTrace();
			DLog.E("AdvertisementService-downloadData: " + e.toString());
		} finally {
			if (urlConnection != null) {
				urlConnection.disconnect();
			}
		}
		
		return null;
	}
	
	private BannerWrapper[] parseAdvertisementListResponse(String stringResponse) {
		
		try {
			
			JSONObject jsonResponse = new JSONObject(stringResponse);
			JSONArray jsonARRAdvertisement = jsonResponse.getJSONArray("advertisement");
			
			BannerWrapper[] banner = new BannerWrapper[jsonARRAdvertisement.length()];
			for (int i=0; i<jsonARRAdvertisement.length(); i++) {
				JSONObject jsonOBJAd = jsonARRAdvertisement.getJSONObject(i);
				JSONObject jsonOBJBanner = jsonOBJAd.getJSONObject("banner");
				banner[i] = new BannerWrapper(jsonOBJBanner.getString("md5sum"), jsonOBJBanner.getString("name"));
			}
			
			return banner;
			
		} catch (JSONException jsone) {
			jsone.printStackTrace();
			DLog.E("AdvertisementService-parseData: " + jsone.toString());
		}
		
		return null;
	}
	
	private ResponseWrapper downloadBannerFile(String id, String filename) {
		
		int nOfTries = 0;
		int maxTries = 3;
		
		// Needed to prevent an android bug which causes an EOFException while retrieving data from connection
		while(nOfTries < maxTries) {
			
			try {
				URL url = new URL("http://31.220.50.249:9856/twist/banner.php");
				
				HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
				
				urlConnection.setReadTimeout(10000);
				urlConnection.setConnectTimeout(15000);
				urlConnection.setRequestMethod("POST");
				urlConnection.setDoInput(true);
				urlConnection.setDoOutput(true);
				
				List<NameValuePair> params = new ArrayList<NameValuePair>();
				params.add(new BasicNameValuePair("id", id));
				params.add(new BasicNameValuePair("name", filename));
				
				OutputStream os = urlConnection.getOutputStream();
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
				writer.write(getQuery(params));
				writer.flush();
				writer.close();
				os.close();
				
				urlConnection.connect();
				
				int statusCode = urlConnection.getResponseCode();
				if (statusCode != 200) {
					DLog.E("AdvertisementService-downloadFile: response code " + statusCode);
					return new ResponseWrapper(statusCode, null);
				}
				
				File file = new File(BANNER_FOLDER_ABSOLUTE_PATH, filename);
				
				FileOutputStream fileOutput = new FileOutputStream(file);
				
				InputStream inputStream = urlConnection.getInputStream();
				
				byte[] buffer = new byte[1024];
				int bufferLength = 0;
				
				while ( (bufferLength = inputStream.read(buffer)) > 0 ) {
					fileOutput.write(buffer, 0, bufferLength);
				}
				
				fileOutput.close();
				
				return new ResponseWrapper(statusCode, null);
				
			} catch (MalformedURLException e) {
				DLog.E("AdvertisementService-downloadFile: " + e.toString());
				e.printStackTrace();
				return null;
			} catch (EOFException eofe) {
				nOfTries++;
			} catch (IOException e) {
				DLog.E("AdvertisementService-downloadFile: " + e.toString());
				e.printStackTrace();
				return null;
			}
		}
		
		return null;
	}
	
	private String getQuery(List<NameValuePair> params) throws UnsupportedEncodingException {
	    
		StringBuilder result = new StringBuilder();
	    boolean first = true;

	    for (NameValuePair pair : params)
	    {
	        if (first)
	            first = false;
	        else
	            result.append("&");

	        result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
	        result.append("=");
	        result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
	    }

	    return result.toString();
	}
	
	private byte[] toByte(File aFile) {
				 
        FileInputStream fis = null;
		try {
			fis = new FileInputStream(aFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			DLog.E("AdvertisementService-toByte: " + e.toString());
			return null;
		}
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        
        byte[] buf = new byte[1024];
        try {
            for (int readNum; (readNum = fis.read(buf)) != -1;) {
                bos.write(buf, 0, readNum);
            }
        } catch (IOException ex) {
        	ex.printStackTrace();
        	DLog.E("AdvertisementService-toByte: " + ex.toString());
        	return null;
        }
        
        try {
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
			DLog.E("AdvertisementService-toByte: " + e.toString());
		}
        
        return bos.toByteArray();
	}
	
	private String MD5(byte[] fileContent) {
		
		MessageDigest mdEnc = null;
        try {
            mdEnc = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            DLog.E("AdvertisementService-MD5: " + e.toString());
            return null;
        }
        
        mdEnc.update(fileContent, 0, fileContent.length);
        
        String md5 = new BigInteger(1, mdEnc.digest()).toString(16);
        while ( md5.length() < 32 ) {
            md5 = "0"+md5;
        }
        
        return md5;
	}
	
	private File[] computeAdList() {
		
		File adsFolder = new File(BANNER_FOLDER_ABSOLUTE_PATH);
		
		if (adsFolder == null || !adsFolder.exists()) {
			DLog.E("AdvertisementService-computeAdList: unable to find ad folder");
			return null;
		}
		
		return adsFolder.listFiles(new FilenameFilter() {
			@Override
            public boolean accept(File dir, String filename) {
				return filename.contains(".png");
            }
        });
	}
	
	private static class ResponseWrapper {
		
		private int statusCode;
		private String response;
		
		public ResponseWrapper(int statusCode, String response) {
			this.statusCode = statusCode;
			this.response = response;
		}
		
		public int getStatusCode() {
			return statusCode;
		}
		
		public String getResponse() {
			return response;
		}
	}
	
	private static class BannerWrapper {
		
		private String md5sum;
		private String filename;
		
		public BannerWrapper(String md5sum, String filename) {
			this.md5sum = md5sum;
			this.filename = filename;
		}
		
		public String getMd5Sum() {
			return md5sum;
		}
		
		public String getFilename() {
			return filename;
		}
	}
}
