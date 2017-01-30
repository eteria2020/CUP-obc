package eu.philcar.csg.OBC.server;




import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import eu.philcar.csg.OBC.App;
import eu.philcar.csg.OBC.db.DbManager;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.helpers.FileTools;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

public class UploaderLog {

	public static void StartLogUploadTask(File file,  Message msg) {
		UploadDbTask task = new UploadDbTask();		
		task.file = file;
		task.msgNotify = msg;
		task.execute();
	}
	
	
	
	
	private static class UploadDbTask extends AsyncTask<Void, Void, Void> {
		
		private DLog dlog = new DLog(this.getClass());
		

		public File file;
		public ProgressDialog  pb;
		public Message msgNotify;


		
		protected Void doInBackground(Void... voids) {
		    
			
			File file = collectFiles(this.file); 
			if (file!=null) {
				uploadFile(file,App.URL_UploadLogs);
				file.delete();
			}
			return null;
			
			
		}
		
	


		public  File collectFiles(File file) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
			String[] files; 
			
			File backupPath = new File("/sdcard/upload");
			
			if (file!=null) {
				files = new String[1];
				files[0] = file.toString();
			} else {
				File logFile = new File("/sdcard/log.txt");
				if (logFile.length()> 256*1024*1024) {
					dlog.e("Log file too big:" + logFile.length());
					return null;
				}
				
				File dbFile=  new File(DbManager.DATABASE_NAME);
				if (dbFile.length()> 10*1024*1024) {
					dlog.e("DB file too big:" + dbFile.length());
					return null;
				}
				files = new String[2];
				
				files[0] =logFile.toString();
				files[1] =dbFile.toString();
			} 
			
			
			
			File uploadFile = new File(backupPath,"Log_"+App.CarPlate+"_"+sdf.format(new Date())+".zip");
			
			DLog.D("uploadFile package : " + uploadFile.toString());
			
			
			
			FileTools.zip(files, uploadFile.toString());
			
			return uploadFile;
		}
		
		
		
		 public int uploadFile(File sourceFile, String upLoadServerUri) {
	           
	          HttpURLConnection conn = null;
	          DataOutputStream dos = null;  
	          String lineEnd = "\r\n";
	          String twoHyphens = "--";
	          String boundary = "*****";
	          int bytesRead, bytesAvailable, bufferSize;
	          byte[] buffer;
	          int maxBufferSize = 1 * 1024 * 1024; 
	          int serverResponseCode=0;

	           
	          if (sourceFile==null || !sourceFile.isFile()) {
	               dlog.e("File to upload null or not a file");
	            
	          }
	          
	          
	          dlog.d("Starting upload: " + sourceFile.getAbsolutePath());
	          
               try { 
                    
                     // open a URL connection to the Servlet
                   FileInputStream fileInputStream = new FileInputStream(sourceFile);
                   URL url = new URL(upLoadServerUri);
                    
                   // Open a HTTP  connection to  the URL
                   conn = (HttpURLConnection) url.openConnection(); 
                   conn.setConnectTimeout(5000);                 
                   conn.setDoInput(true); // Allow Inputs
                   conn.setDoOutput(true); // Allow Outputs
                   conn.setUseCaches(false); // Don't use a Cached Copy
                   conn.setRequestMethod("POST");
                   conn.setRequestProperty("Connection", "Keep-Alive");
                   conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                   conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                   conn.setRequestProperty("uploaded_file", sourceFile.getName()); 
                    
                   dos = new DataOutputStream(conn.getOutputStream());
                   
                   String row = twoHyphens + boundary + lineEnd;
                   dos.writeBytes(row);
                   
                   row = "Content-Disposition: form-data; name=\"uploaded_file\";filename=\"" + sourceFile.getName() + "\"" + lineEnd;
                   dos.writeBytes(row);
                   
                   row = "Content-Type: application/octet-stream"+lineEnd;
                   dos.writeBytes(row);
                   
                   dos.writeBytes(lineEnd);
          
                   // create a buffer of  maximum size
                   bytesAvailable = fileInputStream.available(); 
          
                   bufferSize = Math.min(bytesAvailable, maxBufferSize);
                   buffer = new byte[bufferSize];
          
                   // read file and write it into form...
                   bytesRead = fileInputStream.read(buffer, 0, bufferSize);  
                      
                   while (bytesRead > 0) {
                        
                     dos.write(buffer, 0, bufferSize);
                     bytesAvailable = fileInputStream.available();
                     bufferSize = Math.min(bytesAvailable, maxBufferSize);
                     bytesRead = fileInputStream.read(buffer, 0, bufferSize);   
                      
                    }
          
                   // send multipart form data necesssary after file data...
                   dos.writeBytes(lineEnd);
                   dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
          
                   // Responses from the server (code and message)
                   serverResponseCode = conn.getResponseCode();
                   String serverResponseMessage = conn.getResponseMessage();
                                      
                     
                   dlog.d("HTTP Response is : "+ serverResponseMessage + ": " + serverResponseCode);
                    
                   //close the streams //
                   fileInputStream.close();
                   dos.flush();
                   dos.close();
                     
              } catch (Exception e) {
                   
            	  dlog.e("Error uploading :",e);
              }

              return serverResponseCode; 
               


	         } 
	}
		
	
}
