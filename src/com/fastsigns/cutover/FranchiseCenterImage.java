package com.fastsigns.cutover;

//JDK 1.6.0
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Log4j 1.2.28
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

// SMT Base Libs
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;

/****************************************************************************
 * <b>Title</b>: FranchiseCreator.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Nov 3, 2010<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class FranchiseCenterImage {
	
	// Database Connection info
	private final String dbDriver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	private final String dbUrl = "jdbc:sqlserver://sql_server_db:1433;selectMethod=cursor;responseBuffering=adaptive";
	private final String fsUser = "fastsigns";
	private final String fsPass = "fastsigns";
	private final String sbUser = "sb_user";
	private final String sbPass = "sqll0gin";
	private Connection fsConn = null;
	private Connection sbConn = null;
	
	// Website/WC params
	public static final String FS_SITE_PREFIX = "FTS_";
	public static final String CENTER_IMAGE_CDATA = "<property name=\"imagepath\">~/getfile/";
	
	// Misc Parameters
	private static final Logger log = Logger.getLogger("FranchiseCenterImage");
	public static final String CUSTOM_PORTLET_EXT = ""; 
	
	/**
	 * 
	 */
	public FranchiseCenterImage() throws Exception {
		log.debug("Starting FCI");
		
		// Connect to FastSigns DB
		fsConn = this.getDBConnection(fsUser, fsPass);
		
		// Connect to SB Database
		sbConn = this.getDBConnection(sbUser, sbPass);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		BasicConfigurator.configure();
		FranchiseCenterImage fc = new FranchiseCenterImage();
		fc.process();
		log.debug("Complete");
	}

	/**
	 * Processes the business logic for the class
	 * @throws Exception
	 */
	public void process() throws Exception {
		// Retrieve List of Franchises
		List<FranchiseVO> franchises = this.getFranchiseData();
		
		// Loop franchises
		for (int i=0; i < franchises.size(); i++) {
			FranchiseVO vo = franchises.get(i);
			
			// Get the list of Custom Pages for the franchise
			String content = this.getCustomPages(vo.getFranchiseId());
			if (content == null) {
				continue;
			}
			
			// Parse the data 
			String centerImage = this.parseCenterPageContent(content);
			this.assignImagePath(vo.getFranchiseId(), centerImage);
		}
	}
	
	/**
	 * 
	 * @param content
	 * @return
	 */
	public String parseCenterPageContent(String content) {
		int begin = 0, end = 0;
		
		if (content.indexOf(CENTER_IMAGE_CDATA) == -1) return null;
		
		String data = "";
		begin = content.indexOf(CENTER_IMAGE_CDATA) + CENTER_IMAGE_CDATA.length();
		end = content.indexOf("/", begin);
		data = content.substring(begin, end);
		
		return data;
	}
	
	/**
	 * 
	 * @param franchiseId
	 * @param guid
	 * @throws SQLException
	 */
	public void assignImagePath(int franchiseId, String guid) throws SQLException {
		String s = "select NodeAliasPath, documenttype "; 
		s += "from cms_tree a inner join cms_document b ";
		s += "on a.NodeID = b.DocumentNodeID ";
		s += "where nodeguid=?  ";
		
		PreparedStatement ps = null;
		try {
			ps = fsConn.prepareStatement(s);
			ps.setString(1, guid);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				String path = rs.getString(1);
				
				path = "/binary/org/" + FS_SITE_PREFIX + path.substring(11); 
				path += rs.getString(2);
				
				if (path.indexOf("FS_Default") > -1) return;
				
				if (path.indexOf("opening_soon") > -1) {
					path = "/binary/org/FTS/Uploaded-Files/Images/Store_Images/opening_soon.jpg";
				}
				
				log.debug(franchiseId + ": " + path);
				
				this.updateFranchiseData(franchiseId, path);
			}
		} finally {
			try {
				ps.close();
			} catch(Exception e){}
		}
	}
	
	/**
	 * 
	 */
	public void updateFranchiseData(int id, String path) throws SQLException {
		String s = "update sitebuilder_custom.dbo.fts_franchise ";
		s+= "set center_image_url = ? where franchise_id = ?";
		
		PreparedStatement ps = null;
		try {
			ps = sbConn.prepareStatement(s);
			ps.setString(1, path);
			ps.setString(2, id + "");
			ps.executeUpdate();
			
		} finally {
			try {
				ps.close();
			} catch(Exception e){}
		}
	}
	
	/**
	 * 
	 * @return
	 * @throws SQLException
	 */
	public List<FranchiseVO> getFranchiseData() throws SQLException {
		String s = "Select * from CMS_Document INNER JOIN custom_franchise b ";
		s += "ON FranchiseID = DocumentForeignKeyValue ";
		s += "INNER JOIN CMS_Tree on CMS_Tree.NodeID = CMS_Document.DocumentNodeID ";
		s += "left outer join customtable_StoreBlurbs c on b.StoreBlurb = c.ItemID ";
		s += "where DocumentPageTemplateID = '120' order by storenumber";
		
		List<FranchiseVO> data = new ArrayList<FranchiseVO>();
		PreparedStatement ps = null;
		try {
			ps = fsConn.prepareStatement(s);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				data.add(new FranchiseVO(rs));
			}
		} finally {
			try {
				ps.close();
			} catch(Exception e){}
		}
		
		return data;
	}
	
	/**
	 * 
	 * @param id
	 * @return
	 * @throws SQLException
	 */
	public String getCustomPages(int id) throws SQLException {
		StringBuilder s = new StringBuilder();
		s.append("select DocumentContent from cms_tree a ");
		s.append("inner join CMS_Document b on a.NodeID = b.DocumentNodeID ");
		s.append("where nodealiaspath like '/Franchise/" + id + "' ");
		s.append("and NodeAliasPath not like '%/Assets%' ");
		s.append("order by NodeLevel, NodeAliasPath  ");
		
		String data = "";
		PreparedStatement ps = null;
		try {
			ps = fsConn.prepareStatement(s.toString());
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				data = rs.getString(1);
			}
		} finally {
			try {
				ps.close();
			} catch(Exception e){}
		}
		
		return data;
	}
	
	/**
	 * Creates a connection to the database
	 * @param dbUser Database User
	 * @param dbPass Database Password
	 * @return JDBC connection for the supplied user
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	private Connection getDBConnection(String dbUser, String dbPass) 
	throws InvalidDataException, DatabaseException  {
		DatabaseConnection dbc = new DatabaseConnection(dbDriver, dbUrl, dbUser, dbPass);
		return dbc.getConnection();
	}
	
}
