package com.ansmed.datafeed;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.siliconmtn.io.ftp.SFTPClient;

/****************************************************************************
 * <b>Title</b>: ZipStreamWriter.java<p/>
 * <b>Description: </b> Zips and FTP's files.  Based on ZipManager.java by
 * James Camire, Dec. 20, 2007
 * <p/>
 * <b>Copyright:</b> (c) 2007<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since December 2007
 * Updated 12/8/2008 - added secureFTPFile method.
 ****************************************************************************/

public class ZipStreamWriter {
	ByteArrayOutputStream sOut = null;
	ZipOutputStream zOut = null;
	
	public ZipStreamWriter() {
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Starting");
		ZipStreamWriter t = new ZipStreamWriter();
		
		String str1 = "Hello World.";
		String str2 = "Goodbye World.";
		
		
		try {
			t.createZipStream();
			t.addEntry("/test/hello.txt", str1.getBytes());
			t.addEntry("/test/goodbye.txt", str2.getBytes());
			t.close();
			t.ftpFile("servername",21,"username","password","filename.zip");
			
		} catch(Exception e) {
			System.out.println("Error creating file in stream...");
		}
		
		System.out.println("Completed");
	}
	
	/**
	 * Creates a new zip file using an underlying ByteArrayOutputStream.
	 * @throws IOException
	 */
	public void createZipStream() throws IOException {
		sOut = new ByteArrayOutputStream();
		zOut = new ZipOutputStream(sOut);
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
	 * Calls appropriate FTP send type (secure or standard insecure) based
	 * on the useSecureFtp boolean value passed in.
	 * @param name
	 * @param port
	 * @param user
	 * @param pwd
	 * @param fileName
	 * @param useSecureFtp
	 * @throws IOException
	 */
	public void executeFtpSend(String name, int port, String user, String pwd, 
			String fileName, boolean useSecureFtp) throws IOException {
		
		if (useSecureFtp) {
			secureFTPFile(name, user, pwd, fileName);
		} else {
			ftpFile(name, port, user, pwd, fileName);
		}
		
	}

	/**
	 * Connects to FTP server using passed parameters and writes file with file
	 * name passed as parameter.
	 * @param name
	 * @param port
	 * @param user
	 * @param pwd
	 * @param fileName
	 * @throws IOException
	 */
	public void ftpFile(String name, int port, String user, String pwd, String fileName)
		throws IOException {
	
		SimpleFTP ftp = new SimpleFTP();
			
		ftp.connect(name, port, user, pwd);
		ftp.stor(new ByteArrayInputStream(sOut.toByteArray()), fileName);
		ftp.disconnect();
		
	}
	
	/**
	 * FTPs a file using Secure FTP.
	 * @param host
	 * @param user
	 * @param pwd
	 * @param fileName
	 * @throws IOException
	 */
	public void secureFTPFile(String host, String user, String pwd, String fileName)
		throws IOException {
		
		SFTPClient sftp = null;
		
		sftp = new SFTPClient(host,user,pwd);
		sftp.writeData(sOut.toByteArray(), fileName);
		sftp.disconnect(); 
	}
	
	/**
	 * Writes a file to the file system given the specified path.
	 * @param fileName
	 * @param fileData
	 * @param path
	 */
	protected void createLocalFile(String fileName, String path)
		throws FileNotFoundException, IOException {
		
		FileOutputStream fos = null;
		
		try {
			fos = new FileOutputStream(path + fileName);
			fos.write(sOut.toByteArray());
			fos.close();
		} catch (FileNotFoundException fnfe) {
			throw new FileNotFoundException(fnfe.toString());
		} catch (IOException ioe) {
			throw new IOException(ioe.toString());
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (Exception e) {}
			}
		}
	}
	
	/**
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		zOut.close();
		sOut.close();
	}

}