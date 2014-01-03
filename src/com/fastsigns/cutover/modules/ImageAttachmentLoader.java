package com.fastsigns.cutover.modules;

// JDK 1.6.x
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.io.FileManager;

/****************************************************************************
 * <b>Title</b>: ContentParser.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Nov 12, 2010<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class ImageAttachmentLoader {
	private Connection conn = null;
	private static final String dbDriver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	private static final String dbUrl = "jdbc:sqlserver://sql_server_db:1433;selectMethod=cursor;responseBuffering=adaptive";
	private static final String dbUser = "fastsigns";
	private static final String dbPass = "fastsigns";
	public static final String BASE_FILE_LOC = "/Users/James/Code/WebCrescendo_org/FTS";
	public static final String BASE_FILE_LOCALT = "/Users/James/Code/WebCrescendo_org1/FTS";
	public List<String> existingFiles = new ArrayList<String>();
	public List<String> newFiles = new ArrayList<String>();
	
	/**
	 * 
	 */
	public ImageAttachmentLoader(Connection conn) {
		this.conn = conn;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("Starting Image Attachment ....");
		
		DatabaseConnection dbc = new DatabaseConnection(dbDriver, dbUrl, dbUser, dbPass);
		System.out.println("Connecting DB....");
		Connection conn = dbc.getConnection();
		ImageAttachmentLoader cp = new ImageAttachmentLoader(conn);
		System.out.println("DB Connected ....");
		cp.getContentList();
		
		try {
			conn.close();
		} catch(Exception e) {}
		
		System.out.println("Existing Files: " + cp.existingFiles.size());
		StringBuffer sb = new StringBuffer();
		sb.append("Existing Files\r\n");
		FileManager fm = new FileManager();
		for (int i=0; i < cp.existingFiles.size(); i++) {
			sb.append(cp.existingFiles.get(i)).append("\r\n");
		}
		
		System.out.println("New Files: " + cp.newFiles.size());
		sb.append("New Files\r\n");
		for (int i=0; i < cp.newFiles.size(); i++) {
			sb.append(cp.newFiles.get(i)).append("\r\n");
		}
		
		fm.writeFiles(sb.toString().getBytes(), "/Users/james/Desktop/", "fileswritten.txt", false, false);
		System.out.println("Complete ....");
	}

	
	/**
	 * 
	 * @return
	 */
	protected void getContentList() {
		String sql = "select nodealiaspath, attachmentname, AttachmentBinary, AttachmentSize from CMS_Attachment a ";
		sql += "inner join CMS_Document b on a.AttachmentDocumentID = b.DocumentID ";
		sql += "inner join CMS_Tree c on b.DocumentNodeID = c.NodeID ";
		sql += "where attachmentBinary is not null "; //and NodeAliasPath like '/Franchise/%' ";
		sql += "and NodeAliasPath not like '%/center-image/%' order by nodealiaspath";
		
		PreparedStatement ps = null;
		
		try {
			ps = conn.prepareStatement(sql);
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				String path = rs.getString(1);
				String name = rs.getString(2);
				String loc = this.parseLocPath(path, name);
				File f = new File(BASE_FILE_LOC + loc);
				
				if (! f.exists()) {
					existingFiles.add(BASE_FILE_LOC + loc);
					byte[] fileData = rs.getBytes(3);
					int size = rs.getInt(4);
					if (size != fileData.length) System.out.println("Size MisMatch: " + size + "|" + fileData.length);
					
					String filePath = BASE_FILE_LOC + loc;
					this.writeFile(filePath.substring(0, filePath.lastIndexOf("/") + 1),filePath.substring(filePath.lastIndexOf("/") + 1) ,fileData);
				} else {
					newFiles.add(BASE_FILE_LOC + loc);
				}
			} 
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void writeFile(String path, String fileName, byte[] data) {
		System.out.println("Writing file... " + path + fileName);
		FileManager fm = new FileManager();
		try {
			fm.writeFiles(data, path, fileName, false, false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String parseLocPath(String bPath, String name) {
		String path = bPath;
		if (path.indexOf("/Franchise/") > -1) {
			path = "_" + path.substring(11);
		}
		
		if (path.indexOf("/Content-Folder/") > -1) path = path.substring(15);
		if (path.indexOf("-(1)") > -1) path = path.replace("-(1)", "");
		String base = path;
		if (path.lastIndexOf("/") > -1) base =  path.substring(0, path.lastIndexOf("/") + 1);
		else base += "/";

		base = base + name;
		
		return base;
	}
}
