package com.fastsigns.cutover.modules;

// JDK 1.6.x
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// FS Libs
import com.fastsigns.cutover.ContentParser;
import com.fastsigns.cutover.FranchiseVO;
import com.fastsigns.cutover.TextModuleVO;

// SMT Base Libs
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.util.Convert;

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
public class TextModuleParser {
	private Connection conn = null;
	private static final String dbDriver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	private static final String dbUrl = "jdbc:sqlserver://sql_server_db:1433;selectMethod=cursor;responseBuffering=adaptive";
	private static final String dbUser = "sb_user";
	private static final String dbPass = "sqll0gin";
	
	private static final String dbFSUser = "fastsigns";
	private static final String dbFSPass = "fastsigns";
	ContentParser parser = null;
	
	public static final String TEXT_MODULE_ID = "edittextfsplc_lt_myfscentermodules_modules_ctl0";
	
	/**
	 * 
	 */
	public TextModuleParser(Connection conn, Connection fsConn) {
		this.conn = conn;
		parser = new ContentParser(fsConn);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		DatabaseConnection dbc = new DatabaseConnection(dbDriver, dbUrl, dbUser, dbPass);
		DatabaseConnection fsDbc = new DatabaseConnection(dbDriver, dbUrl, dbFSUser, dbFSPass);
		TextModuleParser cp = new TextModuleParser(dbc.getConnection(), fsDbc.getConnection());
		cp.process();

		/*
		String val = "<content><webpart id=\"edittextfsplc_lt_myfscentermodules_modules_ctl02\"><![CDATA[<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" width=\"100%\" align=\"center\">      <tbody>          <tr>              <td style=\"text-align: center\">&nbsp;<img alt=\"dec_10_banner.jpg\" style=\"width: 444px; height: 133px\" src=\"~/getattachment/c26b5c6f-022d-4d6a-b5d9-6d39464c5d4c/dec_10_banner.jpg.aspx\" /></td>          </tr>      </tbody>  </table>]]></webpart><webpart id=\"editableimage1;ead54678-066b-4936-9715-44b0c6672ba2\"><![CDATA[<image><property name=\"imagepath\">~/getfile/175a07f2-6462-4399-b10a-3fed2f13c370/479_center-photo.aspx</property></image>]]></webpart><webpart id=\"edittextfsplc_lt_myfscentermodules_modules_ctl00\"><![CDATA[<table border=\"0\" cellspacing=\"1\" cellpadding=\"1\" width=\"100%\" align=\"center\">      <tbody>          <tr>              <td>&nbsp;<img alt=\"Staff-(3).jpg\" style=\"width: 442px; height: 179px\" src=\"~/getattachment/c9cc73a4-163a-40e2-90c4-b74d7f9a0742/Staff-(3).jpg.aspx\" /></td>          </tr>      </tbody>  </table>]]></webpart><webpart id=\"edittextfsplc_lt_myfscentermodules_modules_ctl03\"><![CDATA[<a target=\"_blank\" href=\"#http://www.facebook.com/profile.php?id=100000629434468\"><img alt=\"facebook-(2).gif\" style=\"border-bottom: 0px solid; border-left: 0px solid; width: 199px; height: 100px; border-top: 0px solid; border-right: 0px solid\" src=\"~/getattachment/d5112f8f-c6bc-4d1c-9c11-0cd94fd2f3ea/facebook-(2).gif.aspx\" /></a><a target=\"_blank\" href=\"http://twitter.com/FastSigns479\"><img alt=\"twitter.gif\" style=\"border-bottom: 0px solid; border-left: 0px solid; width: 106px; height: 100px; border-top: 0px solid; border-right: 0px solid\" src=\"~/getattachment/5431c27e-5c60-4e53-8cde-909a6cdd7234/twitter.gif.aspx\" /></a>]]></webpart><webpart id=\"edittextfsplc_lt_myfscentermodules_modules_ctl01\"><![CDATA[<h3 style=\"text-align: center\">Click to <a href=\"~/479/Photos-of-Work\">View photos of work we&nbsp;have done</a></h3>  <br />  <table border=\"0\" cellspacing=\"1\" cellpadding=\"1\" width=\"517\" align=\"center\" height=\"179\">      <tbody>          <tr>              <td style=\"text-align: center\">&nbsp;<a href=\"~/getdoc/7df62629-ec84-4fcb-bcdc-7910d49ca9be/View-our-Catalogs\"><img alt=\"view_our_catalogs.jpg\" style=\"width: 494px; height: 163px; vertical-align: middle\" src=\"~/getfile/fd17fadc-f0c7-4d33-aeda-55bf31bb7c91/view_catalogs.aspx?width=494&height=163\" /></a></td>          </tr>      </tbody>  </table>  <div style=\"text-align: center\">&nbsp;</div>]]></webpart></content> ";
		
		List<String> data = cp.parseContentText(val);
		for (int i=0; i < data.size(); i++) {
			String cnt = data.get(i);
			//System.out.println(cnt);
			cnt = parser.parseAll(cnt);
			//System.out.println(cnt);
		}
		*/
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	public void process() throws Exception {
		List<FranchiseVO> franchises = this.getFranchiseData();
		for (int i=0; i < franchises.size(); i++) {
			FranchiseVO vo = franchises.get(i);
			this.getContentList(vo);
		}
		
		this.insertFTSCustomData(franchises);
	}
	
	
	public void insertFTSCustomData(List<FranchiseVO> data) {
		int moduleOptionId = 20000;
		for (int i = 0; i < data.size(); i++) {
			FranchiseVO vo = data.get(i);
			moduleOptionId = this.insertModuleOption(vo, moduleOptionId);
			
		}
	}
	
	
	public int insertModuleOption(FranchiseVO fr, int id) {
		String s = "insert into SiteBuilder_custom.dbo.FTS_CP_MODULE_OPTION (CP_MODULE_OPTION_ID, FTS_CP_MODULE_TYPE_ID, ";
		s += "OPTION_NM, ARTICLE_TXT, CREATE_DT, STANDARD_FLG, APPROVAL_FLG, FRANCHISE_ID) ";
		s += "values(?,?,?,?,?,?,?,?) ";
		
		PreparedStatement ps = null;
		try {
			for (int i = 0; i < fr.getTextModules().size(); i++) {
				TextModuleVO tmvo = fr.getTextModules().get(i);
				id++;
				ps = conn.prepareStatement(s);
				ps.setInt(1, id);
				ps.setInt(2, 9);
				ps.setString(3, "Text Module");
				ps.setString(4, tmvo.getDataText());
				ps.setTimestamp(5, Convert.getCurrentTimestamp());
				ps.setInt(6, 0);
				ps.setInt(7, 1);
				ps.setInt(8, fr.getFranchiseId());
				
				ps.executeUpdate();
				
				// Call the location module
				int modLocId = this.insertLocationModule(fr.getFranchiseId(), (String)tmvo.getValue(), (String)tmvo.getKey());
				
				//Call the module franchise
				this.insertFranchiseModule(modLocId, id);
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		return ++id;
	}
	
	/**
	 * 
	 */
	public void insertFranchiseModule(int modLocId, int modOptionId) {
		String s = "insert into sitebuilder_custom.dbo.fts_cp_module_franchise_xr ";
		s += "(cp_location_module_xr_id, cp_module_option_id, create_dt) values (?,?,?)";
		
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(s);
			ps.setInt(1, modLocId);
			ps.setInt(2, modOptionId);
			ps.setTimestamp(3, Convert.getCurrentTimestamp());
			
			ps.executeUpdate();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
	}
	
	/**
	 * Inserts the location module
	 * @param fid
	 * @param moduleId
	 * @param locationId
	 * @return
	 */
	public int insertLocationModule(int fid, String moduleId, String locationId) {
		String s = "select cp_location_module_xr_id from sitebuilder_custom.dbo.fts_cp_location_module_xr ";
		s+= "where franchise_id = ? and cp_location_id = ?";
		int id = 0;
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(s);
			ps.setInt(1, fid);
			ps.setInt(2, Convert.formatInteger(locationId));
			ResultSet rs = ps.executeQuery();
			if (rs.next()) id = rs.getInt(1);
			System.out.println(fid + "|" + locationId + "|" + " ID: " + id);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		/*
		String s = "insert into SiteBuilder_custom.dbo.fts_cp_location_module_xr ";
		s += "(cp_location_id, franchise_id, cp_module_id, create_dt) values (?,?,?,?) ";
		
		int id = 0;
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(s, Statement.RETURN_GENERATED_KEYS);
			ps.setInt(1, Convert.formatInteger(locationId));
			ps.setInt(2, fid);
			ps.setInt(3, Convert.formatInteger(moduleId));
			ps.setTimestamp(4, Convert.getCurrentTimestamp());
			
			ps.executeUpdate();
			
			ResultSet rs = ps.getGeneratedKeys();
			if (rs.next()) id = rs.getInt(1);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		*/
		return id;
	}
	
	/**
	 * 
	 * @param val
	 * @return
	 */
	protected List<String> parseContentText(String val) {
		StringBuilder sb = new StringBuilder(val);
		List<String> data = new ArrayList<String>();
		int start = 0;
		
		while(true) {
			int loc = sb.indexOf(TEXT_MODULE_ID, start);
			if (loc == -1) break;
			
			loc += 59;
			String content = sb.substring(loc, sb.indexOf("</webpart>", loc) - 3);
			data.add(parser.parseAll(content));
			
			start = loc;
		}
		
		return data;
	}

	/**
	 * 
	 * @return
	 * @throws SQLException
	 */
	public List<FranchiseVO> getFranchiseData() throws SQLException {
		String s = "Select * from fastsigns.dbo.custom_franchise a ";
		s+= "inner join SiteBuilder_custom.dbo.FTS_FRANCHISE b on a.StoreNumber = b.FRANCHISE_ID  ";
		s += "where Module1 in (1,2) or Module2 in (1,2) ";
		s += "or Module3 in (1,2) or Module4 in (1,2) ";
		s += "or Module5 in (1,2) or Module6 in (1,2)  order by storenumber";
		//System.out.println("Franchise Data: " + s);
		
		List<FranchiseVO> data = new ArrayList<FranchiseVO>();
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(s);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				FranchiseVO franchise = new FranchiseVO(rs);
				
				if (rs.getInt("Module1") == 1 || rs.getInt("Module1") == 2) franchise.setModuleLoc("1", rs.getInt("Module1") + "");
				if (rs.getInt("Module2") == 1 || rs.getInt("Module2") == 2) franchise.setModuleLoc("2", rs.getInt("Module2") + "");
				if (rs.getInt("Module3") == 1 || rs.getInt("Module3") == 2) franchise.setModuleLoc("3", rs.getInt("Module3") + "");
				if (rs.getInt("Module4") == 1 || rs.getInt("Module4") == 2) franchise.setModuleLoc("4", rs.getInt("Module4") + "");
				if (rs.getInt("Module5") == 1 || rs.getInt("Module5") == 2) franchise.setModuleLoc("5", rs.getInt("Module5") + "");
				if (rs.getInt("Module6") == 1 || rs.getInt("Module6") == 2) franchise.setModuleLoc("6", rs.getInt("Module6") + "");
				
				// Get the content
				List<String> content = this.getContentList(franchise);
				franchise.addContent(content);
				
				//System.out.println(franchise.getFranchiseId() + ": " + franchise.getTextModules().size() + "|" + content.size());
				data.add(franchise);
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
	 * @return
	 */
	protected List<String> getContentList(FranchiseVO vo) {
		String sql = "select * from fastsigns.dbo.cms_tree a ";
		sql += "inner join fastsigns.dbo.CMS_Document b on a.NodeID = b.DocumentNodeID ";
		sql += "where DocumentContent like '%" + TEXT_MODULE_ID + "%' ";
		sql += "and NodeAliasPath = '/Franchise/" + vo.getFranchiseId() + "'  ";
		sql += "order by NodeAliasPath";
		
		PreparedStatement ps = null;
		List<String> data = new ArrayList<String>();
		try {
			ps = conn.prepareStatement(sql);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String content = rs.getString("DocumentContent");
				data = this.parseContentText(content);
			} 
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return data;
	}

}
