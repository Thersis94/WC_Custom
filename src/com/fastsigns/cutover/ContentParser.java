package com.fastsigns.cutover;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.util.StringUtil;

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
public class ContentParser {
	private Connection conn = null;
	private static final String dbDriver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	private static final String dbUrl = "jdbc:sqlserver://sql_server_db:1433;selectMethod=cursor;responseBuffering=adaptive";
	private static final String dbUser = "fastsigns";
	private static final String dbPass = "fastsigns";
	private Set<String> attachments = new HashSet<String>();
	
	/**
	 * 
	 */
	public ContentParser(Connection conn) {
		this.conn = conn;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		String c = "<content><webpart id=\"editabletext\"><![CDATA[<table cellspacing=\"0\" cellpadding=\"0\" width=\"465\" align=\"center\" border=\"0\">      <tbody>          <tr>              <td><img height=\"81\" alt=\"lake Michigan LightHouse\" width=\"465\" align=\"absMiddle\" src=\"http://chuckgibson.net/storage/storeWeb/435/images/light_house.jpg\" /></td>          </tr>      </tbody>  </table>  <br />  <table cellspacing=\"0\" cellpadding=\"0\" width=\"91\" align=\"center\" border=\"1\">      <tbody>          <tr>              <td>&nbsp;<a href=\"~/getdoc/eca72c0f-96a9-4a77-af67-ac8f37c8b816/435\"><img height=\"19\" alt=\"\" width=\"90\" src=\"~/getfile/8c001c49-ce80-41e5-a7f1-b912575544c1/home_button.aspx\" /></a></td>              <td>&nbsp;<a href=\"~/getdoc/2ff64fc5-4810-41d0-a288-6b97e116585e/Our-Work-(1)\"><img height=\"19\" alt=\"\" width=\"90\" src=\"~/getfile/0f38aeb8-a5c2-49d2-9fc2-5804d17570c3/our_work_button.aspx\" /></a></td>              <td>&nbsp;<a href=\"~/getdoc/6ad8fe4d-62bf-487e-b35b-36799d9f7722/Our-Team\"><img height=\"19\" alt=\"\" width=\"90\" src=\"~/getfile/c93e6e1c-5941-4179-b82a-d9189425fca7/team_button.aspx\" /></a></td>              <td>&nbsp;<a href=\"~/getdoc/da807e90-80f8-4c97-9195-e0ca260222e0/Think-Green\"><img height=\"19\" alt=\"\" width=\"90\" src=\"~/getfile/e373ccb9-56b1-4265-b933-1c22ecd2b074/think_green_button.aspx\" /></a></td>              <td>&nbsp;<a href=\"~/getdoc/de245d05-891d-41b0-8661-1c2e4d011649/Sign-Guides\"><img height=\"19\" alt=\"\" width=\"90\" src=\"~/getfile/5ff3dc04-8c11-4828-97c1-4b73ef5e34da/sign_guides_button.aspx\" /></a></td>          </tr>      </tbody>  </table>  <br />  <table height=\"139\" cellspacing=\"0\" cellpadding=\"0\" width=\"110\" align=\"center\" border=\"0\">      <tbody>          <tr>              <td><img height=\"139\" alt=\"\" width=\"110\" src=\"~/getfile/4df2ce42-a513-4594-a2f7-01d607392315/sbg_cover.aspx\" /></td>              <td><img height=\"140\" alt=\"\" width=\"110\" src=\"~/getfile/30b148ae-6d89-4088-ac2d-7e8e91fef7ab/fcg_cover.aspx\" /></td>              <td><img height=\"139\" alt=\"\" width=\"110\" src=\"~/getfile/4201dd91-6890-4ce5-b548-62d580987501/edg_cover.aspx\" /></td>          </tr>          <tr>              <td>              <p class=\"style30\" align=\"center\"><span style=\"color: #000000\"><a target=\"_blank\" href=\"http://www.fastsigns.com/Uploaded-Files/PDF/SBG_Mini-2009\">Sign Idea Guide</a></span></p>              </td>              <td>              <div class=\"style30\" align=\"center\"><span style=\"color: #000000\"><a target=\"_blank\" href=\"http://www.fastsigns.com/Uploaded-Files/PDF/FASTSIGNS_FullColorGuide\">Full Color Graphics</a></span></div>              </td>              <td>              <div class=\"style30\" align=\"center\"><span style=\"color: #000000\"><a target=\"_blank\" href=\"http://www.fspresentationproducts.com\">Exhibit &amp; Display Guide</a></span></div>              </td>          </tr>      </tbody>  </table>  <br />  <br />  <table cellspacing=\"5\" cellpadding=\"0\" width=\"100%\" align=\"center\" border=\"0\">      <tbody>          <tr>              <td>              <h3 style=\"text-align: center\"><span style=\"color: #000000\">We offer a wide variety of banner stands and trade show displays. <br />              Click on a link below for more info!</span></h3>              <p style=\"text-align: center\"><span id=\"_ctl0__ctl3__ctl0_StoreLocations_Label2\"><a href=\"http://www.fsdisplay.com/\"><font color=\"#265da6\"><br />              </font></a><a target=\"_blank\" href=\"http://www.fastsigns.com/DDS\">Dynamic Digital Signage</a><a href=\"http://www.fsdisplay.com/\"><br />              </a><br />              <a href=\"http://www.fs-displaysystems.com/\"><font color=\"#265da6\">fs-displaysystems.com</font></a><br />              <span id=\"_ctl0__ctl7__ctl0_StoreLocations_Label2\"><a href=\"http://www.fspresentationproducts.com/\"><font color=\"#265da6\">fspresentationproducts.com</font></a></span><br />              <a href=\"http://www.fs-displayproducts.com/\"><font color=\"#265da6\">fs-displayproducts.com</font></a><br />              <a href=\"http://fs.tradeshowcityusa.com/\"><font color=\"#265da6\">fs.tradeshowcityusa.com</font></a><br />              <br />              </span></p>              <p align=\"center\"><span class=\"style26\"><font face=\"Georgia\" color=\"#000033\" size=\"1\">Disclaimer: This site contains links to other internet sites. These links are provided solely as a convenience to you. These third party sites may<br />              contain options and viewpoints that do not necessarily coincide with our opinions and viewpoints. These sites may also have privacy <br />              policies that are different from our policy.&nbsp;</font></span>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; <br />              <br />              &nbsp;</p>              </td>          </tr>      </tbody>  </table>  <table cellspacing=\"5\" cellpadding=\"0\" width=\"100%\" align=\"center\" border=\"0\">      <tbody>          <tr>              <td>              <p align=\"center\"><span id=\"_ctl0__ctl3__ctl0_StoreLocations_Label3\"><strong>Shop our Catalogs</strong></span><br />              &nbsp;</p>              <table cellspacing=\"5\" cellpadding=\"1\" width=\"100%\" align=\"center\" border=\"0\">                  <tbody>                      <tr>                          <td style=\"text-align: center\"><a target=\"_blank\" href=\"http://direct.fastsigns.com/catalog.asp?Browsable=5610%7C435%7C2688\"><font color=\"#265da6\">Apartments &amp; Condos </font></a></td>                      </tr>                      <tr>                          <td style=\"text-align: center\"><span id=\"_ctl0__ctl3__ctl0_StoreLocations_Label3\"><a target=\"_blank\" href=\"http://direct.fastsigns.com/catalog.asp?Browsable=5598%7C435%7C2682\"><font color=\"#265da6\">Banner Designs </font></a></span></td>                      </tr>                      <tr>                          <td style=\"text-align: center\"><span id=\"_ctl0__ctl3__ctl0_StoreLocations_Label3\"><a target=\"_blank\" href=\"http://direct.fastsigns.com/catalog.asp?Browsable=5611%7C435%7C2689\"><font color=\"#265da6\">Exhibits &amp; Displays</font></a></span></td>                      </tr>                      <tr>                          <td style=\"text-align: center\"><a target=\"_blank\" href=\"http://direct.fastsigns.com/catalog.asp?Browsable=5613|435|2691 \"><span id=\"_ctl0__ctl3__ctl0_StoreLocations_Label3\"><font color=\"#265da6\">Golf Signs </font></span></a></td>                      </tr>                      <tr>                          <td style=\"text-align: center\"><span id=\"_ctl0__ctl3__ctl0_StoreLocations_Label3\"><font color=\"#265da6\"><u><a target=\"_blank\" href=\"http://direct.fastsigns.com/catalog.asp?Browsable=5612|435|2690\">Property Signs </a></u></font></span></td>                      </tr>                  </tbody>              </table>              <p align=\"center\"><span id=\"_ctl0__ctl3__ctl0_StoreLocations_Label3\"><br />              <br />              <br />              <a href=\"http://direct.fastsigns.com/catalog.asp?Browsable=5610%7C435%7C2688\"><font color=\"#265da6\">&nbsp;<br />              </font></a><br />              </span><br />              <br />              <a target=\"_blank\" href=\"http://direct.fastsigns.com/catalog.asp?selected_category=1135&amp;cat=Exterior+Property+Signage\"><span id=\"_ctl0__ctl3__ctl0_StoreLocations_Label3\"><font color=\"#265da6\"><br />              </font></span><br />              </a></p>              </td>          </tr>      </tbody>  </table>]]></webpart></content> ";
		DatabaseConnection dbc = new DatabaseConnection(dbDriver, dbUrl, dbUser, dbPass);
		ContentParser cp = new ContentParser(dbc.getConnection());
		System.out.println(c);
		System.out.println(cp.parseAll(c));
	}
	
	public String parseAll(String data) {
		String val = removeCData(data);
		
		val = replaceGetFile(val);
		val = replaceGetAppTheme(val);
		val = this.replaceGetUPFile(val);
		val = this.parseUrl(val);
		val = this.replaceGetAttachment(val);
		
		return val;
	}
	
	/**
	 * 
	 * @param data
	 * @return
	 */
	private String removeCData(String data) {
		String pat = "<![CDATA[";
		Pattern pattern = Pattern.compile(pat, Pattern.LITERAL);
		Matcher matcher = pattern.matcher(data);
		String out = matcher.replaceAll("");
		
		pattern = Pattern.compile("]]>", Pattern.LITERAL);
		matcher = pattern.matcher(out);
		out = matcher.replaceAll("");
		
		// Remove the -(1) on certain orgs
		out = out.replace("-(1)", "");
		
		// replace the full URL path with the relative path for links
		out = out.replace("href=\"http://www.fastsigns.com/Franchise", "href=\"");
		out = out.replace("href='http://www.fastsigns.com/Franchise", "href='");
		
		// replace the full image path with the relative path
		out = out.replace("http://www.fastsigns.com/Franchise/", "/binary/org/FTS_");
		out = out.replace("http://www.fastsigns.com/Uploaded-Files/", "/binary/org/FTS/Uploaded-Files/");
		out = out.replace("http://www.fastsigns.com/", "/");

		return out;
	}
	
	
	private String replaceGetFile(String data) {
		StringBuilder pData = new StringBuilder(data);
		
		int start = 0;
		int loc = 0;
		while ((loc = pData.indexOf("/getfile/", start)) > -1) {
			//int begin = pData.indexOf("~/getfile/", loc);
			int begin = loc;
			if (begin == -1) {
				start = loc + 15;
				continue;
			}
			
			System.out.println("processing getfile: " + begin);
			int imgSrcEnd = pData.indexOf("\"", begin);
			if (imgSrcEnd == -1) imgSrcEnd = pData.indexOf("'", begin);
			if (imgSrcEnd == -1) break;
			
			String getFile = pData.substring(begin, imgSrcEnd);
			int slashIndex = getFile.lastIndexOf("/");
			System.out.println("Slash Index: " + slashIndex);
			if (slashIndex < 0) slashIndex = getFile.lastIndexOf("\"");
			if (slashIndex < 0) slashIndex = getFile.lastIndexOf(">");
			System.out.println("Get File: " + getFile + "|");
			
			String guid = getFile.substring(9, slashIndex);
			if (guid.indexOf("/") > -1) guid = guid.substring(1);
			System.out.println("GUID: " + guid);
			String name = getNameFromGuid(guid, "_");
			
			String insFile ="/binary/org/FTS" + name;
			insFile = insFile.replace("-(1)", "");
			
			if (pData.substring(begin - 1, begin).equals("~")) {
				begin = begin - 1;
			}
			
			pData.replace(begin, imgSrcEnd, insFile);
			start = imgSrcEnd + 1;
		}
		
		return pData.toString();
	}
	
	private String replaceGetAppTheme(String data) {
		StringBuilder pData = new StringBuilder(data);
		
		int start = 0;
		int loc = 0;
		while ((loc = pData.indexOf("~/App_Themes/FASTSIGNS/images/", start)) > -1) {
			//int begin = pData.indexOf("~/App_Themes/FASTSIGNS/images/", loc);
			int begin = loc;
			if (begin == -1) {
				return pData.toString();
			}

			String insFile ="/binary/org/FTS/AppTheme/";
			
			
			pData.replace(begin, begin + 30, insFile);
			start = begin + 31;
		}
		
		return pData.toString();
	}
	
	private String replaceGetUPFile(String data) {
		StringBuilder pData = new StringBuilder(data);
		
		int start = 0;
		int loc = 0;
		while ((loc = pData.indexOf("~/Uploaded-Files", start)) > -1) {
			
			//int begin = pData.indexOf("~/Uploaded-Files", loc);
			int begin = loc;
			if (begin == -1) {
				start = loc + 15;
				continue;
			}
			
			int imgSrcEnd = pData.indexOf("\"", begin);
			if (imgSrcEnd == -1) imgSrcEnd = pData.indexOf("'", begin);
			
			String getFile = pData.substring(begin, imgSrcEnd);
			int slashIndex = getFile.lastIndexOf(".");
			if (slashIndex == -1) slashIndex = getFile.length();
			
			String guid = getFile.substring(1, slashIndex);
			String insFile = "/binary/org/FTS" + getNameFromPath(guid);
			
			pData.replace(begin, imgSrcEnd, insFile);
			start = imgSrcEnd + 1;
		}
		
		return pData.toString();
	}
	
	private String parseUrl(String data) {
		StringBuilder pData = new StringBuilder(data);
		
		int start = 0;
		int loc = 0;
		while ((loc = pData.indexOf("href=", start)) > -1) {
			int begin = pData.indexOf("href=", loc);
			int cBegin = begin + 6;
			char delim = pData.charAt(begin + 5);
			int end = pData.indexOf(new Character(delim).toString(), begin + 6);
			String url = pData.substring(cBegin, end);
			String name = "";
			
			if (url.indexOf("~/getdoc") > -1) {
				String guid = url.substring(9, url.lastIndexOf("/"));
				System.out.println("GUID: " + guid);
				name = this.getNameFromGuid(guid, "/");
			} else if (url.indexOf("/getdoc") > -1) {
				String guid = url.substring(8, url.lastIndexOf("/"));
				System.out.println("GUID: " + guid);
				name = this.getNameFromGuid(guid, "/");
			} else {
				url = url.replace(".aspx", "");
				if (url.indexOf("http://") == -1 && url.indexOf("Uploaded-Files") > -1 && url.length() > 5) {
					if (url.substring(url.length() -5).indexOf(".") == -1) {
						url += ".pdf";
					}
				}
				name = url.replace("http://www.fastsigns.com", "");
			}

			url = name.replace("/Franchise", "");
			url = url.replace("/Assets", "");
			url = url.replace("/Misc-Files", "");
			url = url.replace("-(1)", "");
			System.out.println("Get New Doc Name: " + url);
			
			pData.replace(cBegin, end, url);
			start = end + 1;
		}
		
		return pData.toString();
	}
	
	private String getNameFromPath(String oPath) {
		String sql = "select * from cms_tree a ";
		sql += "inner join CMS_Document b on a.NodeID = b.DocumentNodeID "; 
		sql += "where NodeAliasPath = ?  ";

		PreparedStatement ps = null;
		String path = "";
		try {
			ps = conn.prepareStatement(sql);
			ps.setString(1, oPath);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				String alias = rs.getString("NodeAliasPath");
				alias = alias.substring(0, alias.lastIndexOf("/") + 1);
				alias += rs.getString("DocumentName").replace(" ", "_");
				if (alias.indexOf(".jpg") == -1) 
					alias += StringUtil.checkVal(rs.getString("DocumentType"));
				
				path = alias;

			} else System.out.println("No File for GUID: " + oPath);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return path;
	}
	
	private String getNameFromGuid(String guid, String delim) {
		String sql = "select * from cms_tree a ";
		sql += "inner join CMS_Document b on a.NodeID = b.DocumentNodeID "; 
		sql += "where NodeGUID = cast(? as uniqueidentifier)  ";
		
		PreparedStatement ps = null;
		String path = "";
		try {
			ps = conn.prepareStatement(sql);
			ps.setString(1, guid);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				String alias = rs.getString("NodeAliasPath");
				alias = alias.substring(0, alias.lastIndexOf("/") + 1);
				alias += rs.getString("DocumentName").replace(" ", "-");
				if (alias.indexOf(".jpg") == -1) 
					alias += StringUtil.checkVal(rs.getString("DocumentType"));
				
				alias = alias.replace("/Franchise/", delim);
				path = alias;

			} else System.out.println("No File for GUID: " + guid);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return path;
	}
	
	private String replaceGetAttachment(String data) {
		StringBuilder pData = new StringBuilder(data);
		
		int start = 0;
		int loc = 0;
		while ((loc = pData.indexOf("<img", start)) > -1) {
			int begin = pData.indexOf("~/getattachment/", loc);
			if (begin == -1) {
				start = loc + 15;
				continue;
			}
			
			int imgSrcEnd = pData.indexOf("\"", begin);
			if (imgSrcEnd == -1) imgSrcEnd = pData.indexOf("'", begin);
			if (imgSrcEnd == -1) break;
			
			String getFile = pData.substring(begin, imgSrcEnd);
			int slashIndex = getFile.lastIndexOf("/");
			String guid = getFile.substring(16, slashIndex);
			attachments.add(guid);
			String insFile ="/binary/org/FTS" + getNameFromAttachment(guid);
			
			pData.replace(begin, imgSrcEnd, insFile);
			start = imgSrcEnd + 1;
		}
		
		return pData.toString();
	}
	
	private String getNameFromAttachment(String guid) {
		String sql = "select NodeAliasPath,* from CMS_Attachment a ";
		sql += "inner join CMS_Document b on a.AttachmentDocumentID = b.DocumentID ";
		sql += "inner join CMS_Tree c on b.DocumentNodeID = c.NodeID ";
		sql += "where attachmentBinary is not null and NodeAliasPath like '/Franchise/%' ";
		sql += "and NodeAliasPath not like '%/center-image/%' ";
		sql += "and a.AttachmentGUID = ?";
		
		PreparedStatement ps = null;
		String alias = "";
		try {
			ps = conn.prepareStatement(sql);
			ps.setString(1, guid);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				
				String name = rs.getString("AttachmentName");
				String path = rs.getString("NodeAliasPath");
				int last = path.indexOf("/", 12);
				if (last == -1) last = path.length();
				String fId = path.substring(11,last);
				alias = "_" + fId;
				if (last > path.length()) alias += path.substring(last);
				alias += "/" + name;
				//System.out.println(alias);
			} else System.out.println("No Attachment for GUID: " + guid);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return alias;
	}

	/**
	 * @return the attachments
	 */
	public Set<String> getAttachments() {
		return attachments;
	}

	/**
	 * @param attachments the attachments to set
	 */
	public void setAttachments(Set<String> attachments) {
		this.attachments = attachments;
	}
}

