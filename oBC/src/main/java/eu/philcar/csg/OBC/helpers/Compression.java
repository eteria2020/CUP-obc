package eu.philcar.csg.OBC.helpers;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Compression {

    public static byte[] compress(String str)  {
       if (str == null || str.length() == 0) {
           return null;
       }

       ByteArrayOutputStream obj=new ByteArrayOutputStream();
       try {
	       
	       GZIPOutputStream gzip;
		   gzip = new GZIPOutputStream(obj);
	       gzip.write(str.getBytes("UTF-8"));
	       gzip.close();
	    
	   	} catch (IOException e) {	   		

		}
       return obj.toByteArray();
    }

     public static String decompress(byte[] bytes)  {
       if (bytes == null || bytes.length == 0) {
           return null;
       }
      
	   String outStr = "";
	   try {
	       GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(bytes));
	       BufferedReader bf = new BufferedReader(new InputStreamReader(gis, "UTF-8"));
	       
	       String line;
	       while ((line=bf.readLine())!=null) {
	         outStr += line;
	       }
	       bf.close(); gis.close();
	   	} catch (IOException e) {	   		

		}
	   
       return outStr;
    }
     
     
     public static void unzip(File zipFile, File targetDirectory) {
    	 	ZipInputStream zis = null;
    	    try {
    	        ZipEntry ze;
    	        int count;
    	        byte[] buffer = new byte[8192];
    	        
        	    zis = new ZipInputStream(
        	            new BufferedInputStream(new FileInputStream(zipFile)));
        	    
    	        while ((ze = zis.getNextEntry()) != null) {
    	            File file = new File(targetDirectory, ze.getName());
    	            File dir = ze.isDirectory() ? file : file.getParentFile();
    	            if (!dir.isDirectory() && !dir.mkdirs())
    	                throw new FileNotFoundException("Failed to ensure directory: " +
    	                        dir.getAbsolutePath());
    	            if (ze.isDirectory())
    	                continue;
    	            FileOutputStream fout = new FileOutputStream(file);
    	            try {
    	                while ((count = zis.read(buffer)) != -1)
    	                    fout.write(buffer, 0, count);
    	            } finally {
    	                fout.close();
    	            }
    	            
    	            long time = ze.getTime();
    	            if (time > 0)
    	                file.setLastModified(time);
    	            
    	        }
    	    } catch ( IOException e) {

    	    } finally {
    	        if (zis!=null)
					try {
						zis.close();
					} catch (IOException e) {

					}
    	    }
    	}
}
