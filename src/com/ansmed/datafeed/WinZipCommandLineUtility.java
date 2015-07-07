package com.ansmed.datafeed;

// JDK 1.6
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

// Log4j
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>:WinZipCommandLineUtility.java<p/>
 * <b>Description: </b> Leverages WinZip's command-line utility to encrypt, compress, and password-
 * protect files.
 * interface.  The command-line interface is called as a remote process.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since Nov 12, 2012
 ****************************************************************************/
public class WinZipCommandLineUtility {
	
	private Logger log = Logger.getLogger("WinZipCommandLineUtility");
	private Properties config = null;
	private String zipExecRootPath = null;
	private String zipFilePassword = null;
	private String rawFileSourcePath = null;
	private List<String> rawFilesWritten = null;
	private String zipFileDestinationPath = null;
	private String zipFileName = null;
	private String finalZipFilePath = null; // path to the zipped file
	private Map<String, String> execMap = null;
	
	public WinZipCommandLineUtility(Properties config) {
		this.config = config;
		execMap = new HashMap<String,String>();
		init();
	}
	
    public static void main(String[] args) {
    	PropertyConfigurator.configure("scripts/ans_log4j.properties");
    	Properties config = new Properties();    	
    	WinZipCommandLineUtility util = new WinZipCommandLineUtility(config);
    	util.setZipExecRootPath("C:\\PROGRA~2\\WinZip\\");
    	util.setZipFilePassword("\"These files are 4 C0ry#\"");
    	util.setRawFileSourcePath("C:\\Temp\\sjm_test\\files\\");
    	util.setZipFileDestinationPath("C:\\Temp\\sjm_test\\files\\");
    	util.setZipFileName("TestZipFile.zip");
    	
    	System.out.println("init complete...");
    	try {
	    	System.out.println("writing zipped file...");
    		util.writeZippedFiles();
    		System.out.println("zipped write complete.");
    	} catch (IOException ioe) {
	    	System.out.println("error...IOE");
    		System.out.println("Error executing process, " + ioe);
    	} catch (InterruptedException ie) {
	    	System.out.println("error...IE");
    		System.out.println("Process interrupted, " + ie);
    	}
    	for (String key : util.getExecMap().keySet()) {
    		System.out.println("key/val: " + key + "/" + util.getExecMap().get(key));
    	}
    	
    }
    
    /**
     * Sets some initial parameters based on values in the Properties file.
     */
    private void init() {
    	this.setZipExecRootPath(config.getProperty("zipExecRootPath"));
    	this.setZipFilePassword(config.getProperty("zipFilePassword"));
    	this.setZipFileName(config.getProperty("zipFileName"));
    	this.setRawFileSourcePath(config.getProperty("zipFileSourcePath"));
    	this.setZipFileDestinationPath(config.getProperty("zipFileDestinationPath"));
    }

    /**
     * Writes source files to the file system to the raw file source path.  These files
     * will be used to create the zipped, password-protected, encrypted archive.
     * @param fileData
     */
    public void writeRawFiles(Map<String, StringBuilder> fileData) 
    		throws FileNotFoundException, IOException {
	    log.info("Attempting to write raw source files...");
    	String rawPath = this.fixPath(this.getRawFileSourcePath());
    	rawFilesWritten = new ArrayList<String>();
    	FileOutputStream fOut = null;
    	for (String fileName : fileData.keySet()) {
   			fOut = new FileOutputStream(rawPath + fileName);
   			fOut.write(fileData.get(fileName).toString().getBytes());
   			rawFilesWritten.add(rawPath + fileName);
   			fOut.flush();
   			fOut.close();
    	}
    	log.info("Finished writing raw source files, number of files written: " + rawFilesWritten.size());
    }
    
    /**
     * Performs a remote process call to WinZip's command-line utility 
     * to create a zipped file containing password-protected files that are also
     * encrypted using 128-bit AES encryption.
     * and an encrypted files.
     * @throws IOException
     * @throws InterruptedException
     */
    public void writeZippedFiles() throws IOException, InterruptedException {
    	log.info("Attempting to execute remote Winzip process...");
    	String[] command = this.buildExecCommand();
    	//int x = 1; if (x == 1) return;
    	Process p = Runtime.getRuntime().exec(command);
    	// 'eat' the output and error streams so that the process will execute and return without hanging.
    	RemoteExecStreamManager outputGobbler = new RemoteExecStreamManager(p.getInputStream(), "OUTPUT");
    	RemoteExecStreamManager errorGobbler = new RemoteExecStreamManager(p.getErrorStream(), "ERROR");            
		outputGobbler.start();
		errorGobbler.start();
		// retrieve the exit code when the process completes
	   	int xVal = p.waitFor();
	   	p.destroy();
	   	execMap.put("returnVal", String.valueOf(xVal));
	   	log.info("Remote Winzip process completed with an exit value of: " + xVal);
    }
    
    /**
     * Builds the WinZip command line executable command
     * @return
     * NOTES:  wzzip.exe command line switches:
     * -ybc: -yb indicates automatic or batch mode treatment of prompts; 'c' tells WinZip to supply a 'yes' or 'ok' if prompted
     * -ex: indicates to use maximum 'portable' compression (max compression yet compatible with other zip utilities)
     * -s[somepassword]: -s indicates to use encryption, password is optional and is specified without the brackets. 
     *  	If you want to use a password with spaces in it, enclose the password in double-quotes.
     *   	Examples: -sMyPassword, -s"My Password with spaces"
     * -yc or -ycAES or -ycAES128: use AES 128-bit encryption (256-bit AES encryption uses this flag: -ycAES256)
     * -a: add files, zipped filename first followed by a space and then the file(s) to add.  Wildcards allowed.
     */
    private String[] buildExecCommand() {
    	log.debug("building exec command...");
    	StringBuilder command = new StringBuilder();
    	command.append(this.fixPath(this.getZipExecRootPath()));
    	command.append("wzzip -ybc -ex -s");
    	command.append(this.getZipFilePassword());
    	command.append(" -ycAES256 -a ");
    	command.append(this.fixPath(this.getZipFileDestinationPath()));
    	command.append(this.getZipFileName());
    	command.append(" ");
    	command.append(this.fixPath(this.getRawFileSourcePath()));
    	command.append("*.*");
    	log.info("exec command string: " + command.toString());
    	String[] exec =
    	{
    		"cmd.exe",
    		"/Q",
    		"/C",
    		command.toString()
    	};
    	return exec;
    }
    
    /**
     * Retrieves the zipped file from the file system and returns it in a 
     * Map using the filename as the key and the byte array of file data as 
     * the key's value.
     * @return
     */
    public Map<String, byte[]> retrieveZippedFile() throws Exception {
    	finalZipFilePath = this.fixPath(this.getZipFileDestinationPath()) + this.getZipFileName();
    	Map<String, byte[]> fileMap = new HashMap<String,byte[]>();
    	File f = new File(finalZipFilePath);
    	FileInputStream fis = null;
    	ByteArrayOutputStream baos = null;
    	int b = 0;
    	try {
    		fis = new FileInputStream(f);
    		baos = new ByteArrayOutputStream();
    		while ((b = fis.read()) != -1) {
    			baos.write(b);
    		}
    		fileMap.put(this.getZipFileName(), baos.toByteArray());
    	} catch (FileNotFoundException fnfe) {
    		log.error("Error trying to retrieve zipped file, file not found!", fnfe);
    		throw new Exception();
    	} catch (IOException ioe) {
    		log.error("Error trying to retrieve zipped file, ", ioe);
    		throw new Exception();
    	} finally {
			try {
				baos.close();
				fis.close();
			} catch (Exception e) {}
    	}
    	return fileMap;
    }
    
    /**
     * Deletes any raw files written to the file system that were used as the source
     * for the zipped file.
     */
    public void deleteRawFiles() {
    	if (rawFilesWritten != null && ! rawFilesWritten.isEmpty()) {
	    	log.info("Deleting raw source files, number of files to delete is: " + rawFilesWritten.size());
    		int count = 0;
    		try {
	    		for (String filePath : rawFilesWritten) {
		    		count += this.deleteTheFile(filePath);
	    		}
	    		log.info("Deleted raw source files, number of files deleted is: " + count);
    		} catch (Exception e) {
	    		log.error("Error deleting raw source files, ", e);
    		}
    	}
    }
    
    /**
     * Deletes the source zipped file. 
     * @throws Exception
     */
    public void deleteZippedFile() throws Exception {
    	if (StringUtil.checkVal(finalZipFilePath).length() > 0) {
    		log.info("Deleting zipped file from file system.");
    		int count = 0;
   			count = this.deleteTheFile(finalZipFilePath);
   			if (count == 0) throw new Exception("Zipped file deletion failed.");
    	}
    }
    
    /**
    * Helper method for deleting a file
    */
    private int deleteTheFile(String filePath) {
	    int count = 0;
	    boolean deleted = false;
	    File f = new File(filePath);
		if (f.exists()) {
			deleted = f.delete();
			if (deleted) {
    			log.info("deleted file: " + filePath);
    			count++;
			} else {
    			log.info("could not delete file: " + filePath);
			}
		} else {
			log.info("file does not exist, cannot delete: " + filePath);
		}
		f = null;
		return count;
    }
    
    /**
     * Checks for appropriate 'slash' chars in the path
     * @param path
     * @return
     */
    private String fixPath(String path) {
    	String newPath = path;
    	if (path.indexOf("/") > -1) newPath = path.replace("/", "\\");
   		if (newPath.lastIndexOf("\\") < (newPath.length() - 1)) newPath += "\\";
    	return newPath;
    }
    
	/**
	 * @return the config
	 */
	public Properties getConfig() {
		return config;
	}

	/**
	 * @param config the config to set
	 */
	public void setConfig(Properties config) {
		this.config = config;
	}

	/**
	 * @return the execPath
	 */
	public String getZipExecRootPath() {
		return zipExecRootPath;
	}

	/**
	 * @param execPath the execPath to set
	 */
	public void setZipExecRootPath(String execPath) {
		this.zipExecRootPath = execPath;
	}

	/**
	 * @return the zipPassword
	 */
	public String getZipFilePassword() {
		return zipFilePassword;
	}

	/**
	 * @param zipPassword the zipPassword to set
	 */
	public void setZipFilePassword(String zipFilePassword) {
		this.zipFilePassword = zipFilePassword;
	}

	/**
	 * @return the zipFilePath
	 */
	public String getRawFileSourcePath() {
		return rawFileSourcePath;
	}

	/**
	 * @param zipFilePath the zipFilePath to set
	 */
	public void setRawFileSourcePath(String rawFileSourcePath) {
		this.rawFileSourcePath = rawFileSourcePath;
	}

	/**
	 * @return the zipFileDestinationPath
	 */
	public String getZipFileDestinationPath() {
		return zipFileDestinationPath;
	}

	/**
	 * @param zipFileDestinationPath the zipFileDestinationPath to set
	 */
	public void setZipFileDestinationPath(String zipFileDestinationPath) {
		this.zipFileDestinationPath = zipFileDestinationPath;
	}

	/**
	 * @return the zipFileName
	 */
	public String getZipFileName() {
		return zipFileName;
	}

	/**
	 * @param zipFileName the zipFileName to set
	 */
	public void setZipFileName(String zipFileName) {
		this.zipFileName = zipFileName;
	}

	/**
	 * @return the finalZipFilePath
	 */
	public String getFinalZipFilePath() {
		return finalZipFilePath;
	}

	/**
	 * @param finalZipFilePath the finalZipFilePath to set
	 */
	public void setFinalZipFilePath(String finalZipFilePath) {
		this.finalZipFilePath = finalZipFilePath;
	}

	/**
	 * @return the execMap
	 */
	public Map<String, String> getExecMap() {
		return execMap;
	}

	/**
	 * @param execMap the execMap to set
	 */
	public void setExecMap(Map<String, String> execMap) {
		this.execMap = execMap;
	}
    
}
