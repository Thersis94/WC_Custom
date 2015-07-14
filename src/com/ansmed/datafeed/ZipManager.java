package com.ansmed.datafeed;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/****************************************************************************
 * <b>Title</b>:ZipManager.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2007<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Dec 20, 2007
 ****************************************************************************/
public class ZipManager {
	OutputStream fOut = null;
	ZipOutputStream zOut = null;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Starting");
		ZipManager t = new ZipManager();
		try {
			t.createZipFile("C:\\temp\\Desktop\\test.zip");
			t.addEntry("/test/hello2.txt", "hello world".getBytes());
			t.close();
		} catch(Exception e) {
			System.out.println("Error Creating file");
		}
		
		System.out.println("Completed");
	}
	
	/**
	 * creates a new zip file
	 * @param filePath Fully qualified path and file name
	 * @throws IOException
	 */
	public void createZipFile(String filePath) throws IOException {
		fOut = new BufferedOutputStream(new FileOutputStream(filePath));
		zOut = new ZipOutputStream(fOut);
	}
	
	/**
	 * Adds a new entry to the zip file
	 * @param fileName Name of the file.  Can have a path such as: /test/hello.txt
	 * @param b Data to be added
	 * @throws IOException
	 */
	public void addEntry(String fileName, byte[] b) throws IOException {
		zOut.putNextEntry(new ZipEntry(fileName));
		zOut.write(b);
		zOut.closeEntry();
	}
	
	/**
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		zOut.close();
		fOut.close();
	}

}
