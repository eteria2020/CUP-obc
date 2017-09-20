package eu.philcar.csg.OBC.helpers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileTools {

	public static void copy(File origin, File destination) {

		
	    InputStream in = null;
	    OutputStream out = null;
	    try {

	    	String outputPath = destination.getPath();
	        //create output directory if it doesn't exist
	        File dir = new File (outputPath); 
	        if (!dir.exists()) {
	            dir.mkdirs();
	        }


	        in = new FileInputStream(origin);
	        out = new FileOutputStream(destination);

	        byte[] buffer = new byte[1024];
	        int read;
	        
	        while ((read = in.read(buffer)) != -1) {
	            out.write(buffer, 0, read);
	        }
	        
	        in.close();
	        in = null;

	            // write the output file (You have now copied the file)
	        out.flush();
	        out.close();
	        out = null;

	    }  catch (FileNotFoundException fnfe1) {
	        DLog.E("File copy", fnfe1);
	    }
	       catch (Exception e) {
	        DLog.E("File copy", e);
	    }

	}
	
	public static void zip(String file ) {
		
		zip (file, file + ".zip");
		
	}
	
	public static void zip(String file, String zipFileName) {
		
		String[] files = {file};
		zip(files,zipFileName);
		
	}
	
	
	private final static int BUFFER = 1024;
	public static void zip(String[] files, String zipFileName) {
		try {
			BufferedInputStream origin = null;
			File destination = new File(zipFileName);
			
			
			String outputPath = zipFileName.substring(0, zipFileName.lastIndexOf(File.separator));
	    	
	        //create output directory if it doesn't exist
	        File dir = new File (outputPath); 
	        if (!dir.exists()) {
	            dir.mkdirs();
	        }
			
			FileOutputStream dest = new FileOutputStream(zipFileName);
			ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
			
			byte data[] = new byte[BUFFER];
 
			for (int i = 0; i < files.length; i++) {
				DLog.D("Compress - Adding: " + files[i]);
				FileInputStream fi = new FileInputStream(files[i]);
				origin = new BufferedInputStream(fi, BUFFER);
 
				ZipEntry entry = new ZipEntry(files[i].substring(files[i].lastIndexOf("/") + 1));
				out.putNextEntry(entry);
				int count;
 
				while ((count = origin.read(data, 0, BUFFER)) != -1) {
					out.write(data, 0, count);
				}
				origin.close();
				fi.close();
			}
 
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static long getFileSize(final File file)
	{
		if(file==null||!file.exists())
			return 0;
		if(!file.isDirectory())
			return file.length();
		final List<File> dirs=new LinkedList<File>();
		dirs.add(file);
		long result=0;
		while(!dirs.isEmpty())
		{
			final File dir=dirs.remove(0);
			if(!dir.exists())
				continue;
			final File[] listFiles=dir.listFiles();
			if(listFiles==null||listFiles.length==0)
				continue;
			for(final File child : listFiles)
			{
				result+=child.length();
				if(child.isDirectory())
					dirs.add(child);
			}
		}
		return result;
	}
	
}
