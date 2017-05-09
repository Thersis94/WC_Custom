package com.biomed.smarttrak.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.data.GenericVO;
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: MarkdownConverter.java<p/>
 * <b>Description: Reads Markdown syntax from specific tables, Converts the syntax to HTML.  
 * 	Writes the HTML back into the database/record.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Feb 22, 2017
 ****************************************************************************/
public class LegacyImageInserter extends CommandLineUtil {

	/**
	 * default constructor
	 * @param args
	 */
	public LegacyImageInserter(String[] args) {
		super(args);
		loadProperties("scripts/bmg_smarttrak/markdown.properties");
		loadDBConnection(props);
	}


	/**
	 * main method
	 * @param args
	 */
	public static void main(String[] args) {
		LegacyImageInserter eui = new LegacyImageInserter(args);
		eui.run();
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 * call this method from main() to iterate the enum and execute all tables
	 */
	@Override
	public void run() {
		//run graphics, then run attachments.
		processGraphics();
		//processAttachments();
	}
	
	protected void processGraphics() {
		Map<String,List<GenericVO>> recs = readGraphicRecords();
		combineAndSaveGraphics(recs);
		try {
			log.info("sleeping 5 seconds for DB to commit");
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			log.error("could not sleep", e);
		}
		parseAndSaveContent(recs);
	}
	
	protected void processAttachments() {
		Map<String,List<String[]>> recs = readAttachmentRecords();
		combineAndSaveAttachments(recs);
	}


	/**
	 * reads the key/value pairings from the database using the getSelectSql() query defined in the enum
	 * @param t
	 * @return
	 */
	protected Map<String,List<GenericVO>> readGraphicRecords() {
		Map<String,List<GenericVO>> records = new HashMap<>();
		String sql = "select image, name, object_id from biomedgps.images_image where content_type_id=105 order by object_id, \"order\", name";
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String objId = rs.getString(3);
				if (StringUtil.isEmpty(objId) || StringUtil.isEmpty(rs.getString(1))) continue;

				List<GenericVO> list = records.get(objId);
				if (list == null) list = new ArrayList<>();
				list.add(new GenericVO(rs.getString(1), StringUtil.checkVal(rs.getString(2))));
				records.put(objId, list);
			}

		} catch (SQLException sqle) {
			log.error("could not read records", sqle);
		}
		return records;
	}


	/**
	 * converts the markup to html for each of the records retrieved
	 * @param records
	 */
	protected void combineAndSaveGraphics(Map<String,List<GenericVO>> records) {
		for (Map.Entry<String, List<GenericVO>> entry : records.entrySet()) {
			if (entry.getValue().isEmpty()) continue;
			StringBuilder html = new StringBuilder(500);
			//header wrapping html
			html.append("<!--GRAPHICS--><div class=\"insightGraphics\"><h4>Graphics</h4>\n<div class=\"sidebar-image\">\n");
			for (GenericVO data : entry.getValue()) {
				//append each image, in the order given
				html.append("<div class=\"insightGraphic\">\n");
				html.append("<a class=\"use-modal\" title=\"").append(data.getValue()).append("\" href=\"/secBinary/org/BMG_SMARTTRAK/").append(data.getKey()).append("\">");
				html.append("<img alt=\"").append(data.getValue()).append("\" src=\"/secBinary/org/BMG_SMARTTRAK/").append(data.getKey()).append("\"/></a></div>");
			}
			//footer wrapping html
			html.append("\n</div></div>\n");

			log.debug("insight " + entry.getKey() + " has " + entry.getValue().size() + " graphics");
			saveRecord(entry.getKey(), html.toString(), false);
		}
	}


	/**
	 * writes the converted html back to the database using the getUpdateSql() query in the enum
	 * @param records
	 * @param t
	 */
	protected void saveRecord(String id, String html, boolean isContent) {
		String sql;
		if (isContent) { 
			sql = "update custom.biomedgps_insight set content_txt=? where insight_id=?";
		} else {
			sql = "update custom.biomedgps_insight set side_content_txt=coalesce(side_content_txt,'')+? where insight_id=?";
		}

		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setString(1, html);
			ps.setString(2, id);
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("could not write record " + id, sqle);
		}
	}


	/**
	 * @param recs
	 */
	protected void parseAndSaveContent(Map<String, List<GenericVO>> recs) {
		for (Map.Entry<String, List<GenericVO>> entry : recs.entrySet()) {
			if (entry.getValue().isEmpty()) continue;
			String content = getInsightContent(entry.getKey());
			boolean saveRecord = false;

			for (GenericVO data : entry.getValue()) {
				//first determine if this image is even embedded in the content
				String placeholder = "image:" + data.getValue();
				int idx = content.indexOf(placeholder);
				if (idx == -1) continue;
				log.debug("insight " + entry.getKey() + " has embedded " + placeholder);

				//replace the placeholder with the image, wrapped with a link.
				StringBuilder html = new StringBuilder(content.length() + 500);
				html.append(content.substring(0, idx));
				html.append("<!--EMBED--><a class=\"use-modal insightGraphicInline\" title=\"").append(data.getValue()).append("\" href=\"/secBinary/org/BMG_SMARTTRAK/").append(data.getKey()).append("\">");
				html.append("<img alt=\"").append(data.getValue()).append("\" src=\"/secBinary/org/BMG_SMARTTRAK/").append(data.getKey()).append("\"/></a><!--ENDEMBED-->");
				html.append(content.substring(idx+placeholder.length()));
				content = html.toString();
				saveRecord = true;
			}
			if (saveRecord)
				saveRecord(entry.getKey(), content, true);
		}
	}


	/**
	 * loads the Insight record from the DB
	 * @param key
	 * @return
	 */
	protected String getInsightContent(String key) {
		String sql = "select content_txt from custom.biomedgps_insight where insight_id=?";
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setString(1,  key);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				return rs.getString(1);

		} catch (SQLException sqle) {
			log.error("could not read content", sqle);
		}
		return "";
	}
	
	
	/*************************************************************************
	 * 						INSIGHT ATTACHMENTS
	 *************************************************************************/

	/**
	 * read the attachments from the attachments table
	 * @return
	 */
	protected Map<String,List<String[]>> readAttachmentRecords() {
		Map<String,List<String[]>> records = new HashMap<>();
		String sql = "select object_id,name,attachment,description from biomedgps.attachments_attachment where content_type_id=105 order by object_id,\"order\",name;";
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String objId = rs.getString(1);
				if (StringUtil.isEmpty(objId) || StringUtil.isEmpty(rs.getString(2)) || StringUtil.isEmpty(rs.getString(3))) continue;

				List<String[]> list = records.get(objId);
				if (list == null) list = new ArrayList<>();
				list.add(new String[]{ rs.getString(2), rs.getString(3), rs.getString(4)});
				records.put(objId, list);
			}

		} catch (SQLException sqle) {
			log.error("could not read records", sqle);
		}
		return records;
	}


	/**
	 * @param recs
	 */
	protected void combineAndSaveAttachments(Map<String, List<String[]>> recs) {
		for (Map.Entry<String, List<String[]>> entry : recs.entrySet()) {
			if (entry.getValue().isEmpty()) continue;
			StringBuilder html = new StringBuilder(500);
			//header wrapping html
			html.append("\n<!--ATTACHMENTS--><div class=\"insightAttachments\"><h4>Attachments</h4><div class=\"sidebar-fileLink\">\n");
			for (String[] arr : entry.getValue()) {
				//append each image, in the order given
				html.append("<div class=\"insightAttachment\">");
				html.append("<a title=\"").append(arr[2]).append("\" href=\"/secBinary/org/BMG_SMARTTRAK/").append(arr[1]).append("\" target=\"_blank\">");
				html.append(arr[0]).append("</a></div>\n");
			}
			//footer wrapping html
			html.append("\n</div></div>\n");

			log.debug("insight " + entry.getKey() + " has " + entry.getValue().size() + " attachments");
			saveRecord(entry.getKey(), html.toString(), false);
		}


		//	
		//		<! -- for each -->
		//		<div class="insightAttachment">
		//			<a name="[[file name]]" href="[[place a url to attached file here]]" target="_blank" >
		//				[[human readable file name like  1st-quarter-sales.xls]]
		//			</a>
		//		</div>
		//		<!-- end for each -->
		//	</div>
		//</div>
	}
}