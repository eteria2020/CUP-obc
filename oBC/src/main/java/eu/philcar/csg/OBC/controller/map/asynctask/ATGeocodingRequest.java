package eu.philcar.csg.OBC.controller.map.asynctask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.AMainOBC.GeocodedLocation;
import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;

public class ATGeocodingRequest extends AsyncTask<String, String, ArrayList<GeocodedLocation>> {
	
	private static final String REQUEST_URL_START = "https://maps.googleapis.com/maps/api/geocode/json?address="; 
	private static final String REQUEST_URL_END   = "&component=administrative_area:MI&sensor=true";
	
	public interface ATGeocodingRequestDelegate {
		public void onResultReceived(ArrayList<GeocodedLocation> locations);
		public void onErrorMessageReceived(String error);
	}
	
	private ATGeocodingRequestDelegate delegate;
	private Context context;
	
	public ATGeocodingRequest(ATGeocodingRequestDelegate delegate, Context context) {
		this.delegate = delegate;
		this.context = context;
	}
	
	@Override
	protected ArrayList<GeocodedLocation> doInBackground(String... params) {
		
		String response = connect(REQUEST_URL_START + params[0] + REQUEST_URL_END);
		
		return (response != null) ? parse(response) : null;
	}
	
	private String connect(String request) {
		
		URL urlToRequest = null;
		try {
			urlToRequest = new URL(request);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			publishProgress(context.getResources().getString(R.string.error_malformed_url) + " " + request);
			return null;
		}
	    
		HttpsURLConnection urlConnection = null;
		try {
			urlConnection = (HttpsURLConnection)urlToRequest.openConnection();
		} catch (IOException e) {
			e.printStackTrace();
			publishProgress(context.getResources().getString(R.string.error_unable_to_open_connection));
			return null;
		}
		
	    urlConnection.setConnectTimeout(10000);
	    urlConnection.setReadTimeout(10000);
	    
	    int statusCode = 0;
		try {
			statusCode = urlConnection.getResponseCode();
		} catch (IOException e) {
			e.printStackTrace();
			publishProgress(context.getResources().getString(R.string.error_unable_to_get_request_response));
			return null;
		}
		
	    if (statusCode != HttpsURLConnection.HTTP_OK) {
	    	publishProgress(context.getResources().getString(R.string.error_http_code_failure));
			return null;
	    }
	    
	    InputStream is;
		try {
			is = urlConnection.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
			publishProgress(context.getResources().getString(R.string.error_unable_to_read_server_response));
			return null;
		}
	    BufferedReader rd = new BufferedReader(new InputStreamReader(is));
	    
	    String line;
	    StringBuffer response = new StringBuffer(); 
	    try {
			while((line = rd.readLine()) != null) {
				response.append(line);
			    response.append('\r');
			  }
		} catch (IOException e1) {
			e1.printStackTrace();
			publishProgress(context.getResources().getString(R.string.error_unable_to_read_message_content));
			return null;
		}
	    
	    try {
			rd.close();
		} catch (IOException e) {
			e.printStackTrace();
			publishProgress(context.getResources().getString(R.string.error_unable_to_close_buffer_reader));
			return null;
		}
		
		return response.toString();
	}
	
	private ArrayList<GeocodedLocation> parse(String response) {
		
		JSONObject jsonResponse = null;
		try {
			jsonResponse = new JSONObject(response);
		} catch (JSONException e) {
			e.printStackTrace();
			publishProgress(context.getResources().getString(R.string.error_parsing_response_not_json));
			return null;
		}
		
		String status = null;
		try {
			status = jsonResponse.getString("status");
		} catch (JSONException e) {
			e.printStackTrace();
			publishProgress(context.getResources().getString(R.string.error_parsing_missing_status));
			return null;
		}
		
		ArrayList<GeocodedLocation> locations = new ArrayList<GeocodedLocation>();
		
		if (status.equalsIgnoreCase("OK")) {
			
			JSONArray jsonResults = null;
			try {
				jsonResults = jsonResponse.getJSONArray("results");
			} catch (JSONException e) {
				e.printStackTrace();
				publishProgress(context.getResources().getString(R.string.error_parsing_missing_results));
				return null;
			}
			
			for (int i=0; i<jsonResults.length(); i++) {
				
				JSONObject jsonResult = null;
				try {
					jsonResult = jsonResults.getJSONObject(i);
				} catch (JSONException e) {
					e.printStackTrace();
					publishProgress(context.getResources().getString(R.string.error_parsing_result_not_json));
					return null;
				}
				
				JSONArray jsonTypes = null;
				try {
					jsonTypes = jsonResult.getJSONArray("types");
				} catch (JSONException e) {
					e.printStackTrace();
					publishProgress(context.getResources().getString(R.string.error_parsing_missing_types));
					return null;
				}
				
				boolean valid = false;
				for (int j=0; j<jsonTypes.length(); j++) {
					
					String aType = null;
					try {
						aType = jsonTypes.getString(j);
					} catch (JSONException e) {
						e.printStackTrace();
						publishProgress(context.getResources().getString(R.string.error_parsing_invalid_type));
						return null;
					}
					
					if (aType.equalsIgnoreCase("route") || aType.equalsIgnoreCase("street_address")) {
						valid = true;
						break;
					}
				}
				
				if (!valid) {
					continue;
				}
				
				GeocodedLocation geocodedLocation = new GeocodedLocation(null, null);
				
				try {
					geocodedLocation.address = jsonResult.getString("formatted_address");
				} catch (JSONException e) {
					e.printStackTrace();
					publishProgress(context.getResources().getString(R.string.error_parsing_missing_addresse));
					return null;
				}
				
				JSONObject jsonGeometry = null;
				try {
					jsonGeometry = jsonResult.getJSONObject("geometry");
				} catch (JSONException e) {
					e.printStackTrace();
					publishProgress(context.getResources().getString(R.string.error_parsing_missing_geometry));
					return null;
				}
				
				JSONObject jsonLocation = null;
				try {
					jsonLocation = jsonGeometry.getJSONObject("location");
				} catch (JSONException e) {
					e.printStackTrace();
					publishProgress(context.getResources().getString(R.string.error_parsing_missing_location));
					return null;
				}
				
				double latitude;
				try {
					latitude = jsonLocation.getDouble("lat");
				} catch (JSONException e) {
					e.printStackTrace();
					publishProgress(context.getResources().getString(R.string.error_parsing_missing_latitude));
					return null;
				}
				
				double longitude;
				try {
					longitude = jsonLocation.getDouble("lng");
				} catch (JSONException e) {
					e.printStackTrace();
					publishProgress(context.getResources().getString(R.string.error_parsing_missing_longitude));
					return null;
				}
				
				geocodedLocation.location = new Location(""); //TODO
				
				locations.add(geocodedLocation);
			}
		}
		
		return locations;
	}
	
	@Override
	protected void onProgressUpdate(String... values) {
		
		super.onProgressUpdate(values);
		
		if (isCancelled()) {
			return;
		}
		
		String errorMessage = "";
		for (String aValue : values) {
			errorMessage += aValue + " ";
		}
		
		delegate.onErrorMessageReceived(errorMessage);
	}

	@Override
	protected void onPostExecute(ArrayList<GeocodedLocation> result) {
		
		super.onPostExecute(result);
		
		if (isCancelled()) {
			return;
		}
		
		if (result != null) {
			delegate.onResultReceived(result);
		}
	}
}
