package com.fastsigns.product;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.io.FileManager;

/****************************************************************************
 * <b>Title</b>: ImageLoader.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Oct 28, 2010<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class ImageLoader {
	protected static Logger log = Logger.getLogger(ImageLoader.class);
	private final String dbDriver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	private final String dbUrl = "jdbc:sqlserver://sql_server_db:1433;selectMethod=cursor;responseBuffering=adaptive";
	private final String dbUser = "sb_user";
	private final String dbPass = "sqll0gin";
	private Connection conn = null;
	private String baseFolder = "/Users/james/Public/FastSigns/FastSignsImagesLatest/";
	//private String ftsFolder = "/Users/james/Code/WebCrescendo_org/FTS";
	private String ftsFolder = "/Users/james/Desktop/images";
	
	public ImageLoader() {
		BasicConfigurator.configure();
		this.openDBConnection();
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ImageLoader il = new ImageLoader();
		log.debug("Starting ...");
		
		il.process();
		
		try {
			il.conn.close();
		} catch(Exception e) {}
		
		log.debug("Complete");
	}

	
	public void process() {
		//Map<String, String> imageLocs = this.getImageLocs();
		Map<String, String> imageLocs = getProductImageLocs();
		
		//System.exit(-1);
		int fCount = 0;
		Set<String> s = imageLocs.keySet();
		for (Iterator<String> iter = s.iterator(); iter.hasNext(); ) {
			String guid = iter.next();
			String path = imageLocs.get(guid);
			String ext = path.substring(path.length() - 4);
			
			if (guid == null) continue;
			guid = guid.toLowerCase();
			String fldr = guid.substring(0,2);
			
			String origFile = baseFolder + fldr + "/" + guid + ext;
			if (! checkFile(fldr, guid, ext)) {
				fCount++;
				//System.out.println(guid + "\t" + path);
				continue;
			}
			
			// Copy the image to the WC folder
			String newBaseFldr = ftsFolder + path.substring(0, path.lastIndexOf("/") + 1);
			this.copyFile(newBaseFldr, origFile, path.substring(path.lastIndexOf("/") + 1));
		}
		
		log.debug("Numbers: " + imageLocs.size() + "|" + fCount);
	}
	
	/**
	 * 
	 * @param fldr
	 * @param guid
	 * @param ext
	 * @return
	 */
	public boolean checkFile(String fldr, String guid, String ext) {
		File f = new File(baseFolder + fldr + "/" + guid + ext);
		return f.exists();		
	}
	
	/**
	 * 
	 * @param folder WC folder to create
	 * @param src original folder with paths
	 * @param dest fully qualified destination
	 */
	public void copyFile(String folder, String src, String destFileName) {
		File f = new File(folder);
		f.mkdirs();
		FileManager fm = new FileManager();
		try {
			//fm.copy(src, dest);
			byte[] b = fm.retrieveFile(src);
			fm.writeFiles(b, folder, destFileName, false, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public Map<String, String> getImageLocs() {
		Map<String, String> imageLocs = new LinkedHashMap<String, String>();
		StringBuilder s = new StringBuilder();
		s.append("SELECT Replace(REPLACE(nodealiaspath, '-(1)',''), '/Franchise/', '/org/FTS_') , ");
		s.append("FileName, FileAttachment, lower(DocumentType) FROM fastsigns.dbo.CONTENT_File a ");
		s.append("INNER JOIN fastsigns.dbo.CMS_Tree b ON a.FileName = b.NodeName ");
		s.append("INNER JOIN fastsigns.dbo.CMS_Document c ON b.NodeID = c.DocumentNodeID ");
		s.append("and c.DocumentForeignKeyValue = a.FileID ");
		s.append("where NodeAliasPath like '/Franch%'  ");
		s.append("order by NodeAliasPath  ");
		log.debug("SQL: " + s);
		
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(s.toString());
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				String loc = rs.getString(1);
				String fileName = rs.getString(2).replaceAll(" ", "_");
				String folder = loc.substring(0, loc.lastIndexOf("/") + 1);
				if (fileName.length() < 4 || fileName.charAt(fileName.length() - 4) != '.') {
					fileName += rs.getString(4);
					
				}
				
				imageLocs.put(rs.getString(3), folder + fileName);
				log.debug(rs.getString(3) + "|" + folder + fileName);
			}
		} catch(Exception e) {
			log.error("Unable to retrieve image locs", e);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		return imageLocs;
	}
	
	public Map<String, String> getProductImageLocs() {
		Map<String, String> imageLocs = new LinkedHashMap<String, String>();
		StringBuilder s = new StringBuilder();
		s.append("select REPLACE(NodeAliasPath, '/Content-Folder','') as path, ");
		s.append("FileName, FileAttachment, lower(DocumentType) ");
		s.append("from fastsigns.dbo.cms_tree a  ");
		s.append("inner join fastsigns.dbo.CMS_Document b on a.NodeID = b.DocumentNodeID "); 
		s.append("inner join fastsigns.dbo.CONTENT_File c on b.DocumentForeignKeyValue = c.FileID ");
		s.append("where NodeAliasPath like '%/Products/%' ");
		s.append("and len(DocumentType) > 0 ");
		s.append("order by path  ");
		log.debug("SQL: " + s);
		
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(s.toString());
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				String loc = rs.getString(1);
				String fileName = rs.getString(2).replaceAll(" ", "_");
				String folder = loc.substring(0, loc.lastIndexOf("/") + 1);
				if (fileName.length() < 4 || fileName.charAt(fileName.length() - 4) != '.') {
					fileName += rs.getString(4);
					
				}
				
				imageLocs.put(rs.getString(3), folder + fileName);
				log.debug(rs.getString(3) + "|" + folder + fileName);
			}
		} catch(Exception e) {
			log.error("Unable to retrieve image locs", e);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		return imageLocs;
	}
	
	private void openDBConnection() {
		DatabaseConnection dbc = new DatabaseConnection(dbDriver, dbUrl, dbUser, dbPass);
		try {
			conn = dbc.getConnection();
		} catch (Exception de) {
			de.printStackTrace();
			System.exit(-1);
		}
	}
}
