package com.depuysynthes.ifu;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Log4J
import org.apache.log4j.Logger;

// SMT Base Libs
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;

/****************************************************************************
 * <b>Title</b>: IFUExporter.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Class to export the IFU Data
 * <b>Copyright:</b> Copyright (c) 2020
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 9, 2020
 * @updates:
 ****************************************************************************/
public class IFUExporter {
	
	private Connection conn;
	private static final Logger log = Logger.getLogger(IFUExporter.class);

	/**
	 * @throws InvalidDataException 
	 * @throws DatabaseException 
	 * 
	 */
	public IFUExporter() throws Exception {
		super();
		conn = getConnection();
		log.info("Connected: " + (! conn.isClosed()));
	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		log.info("Starting");
		IFUExporter ie = new IFUExporter();
		Charset charset = Charset.forName("UTF-8");
		Path file = Paths.get("/home", "etewa", "Desktop", "ifu.txt");
		/*
		try (BufferedWriter writer = Files.newBufferedWriter(file, charset)) {
			ie.getAllData(writer);
		}
		log.info("---------------------");
		*/
		file = Paths.get("/home", "etewa", "Desktop", "ifu_article.txt");
		try (BufferedWriter writer = Files.newBufferedWriter(file, charset)) {
			ie.getArticleData(writer);
		}
		
		log.info("complete");
	}
	
	/**
	 * 
	 * @param bw
	 * @throws SQLException
	 * @throws IOException
	 */
	public void getArticleData(BufferedWriter bw) throws SQLException, IOException {
		String sql = "select depuy_ifu_impl_id, article_txt from custom.depuy_ifu_impl order by depuy_ifu_id";
		try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
			try (ResultSet rs = ps.executeQuery()) {
				ResultSetMetaData rsmd = rs.getMetaData();
				int numColumns = rsmd.getColumnCount();
				log.info("Column Count: " + numColumns);
				
				// Add the header to the file
				StringBuilder header = new StringBuilder();
				for (int i = 1; i <= numColumns; i++) {
					header.append(rsmd.getColumnName(i));
					if (i < numColumns) header.append("\t");
					else header.append("\n");
				}
				bw.write(header.toString(), 0, header.length());
				
				// Loop each row of db data and convert it to a row of tab delimited data
				int numRows = 0;
				while (rs.next()) {
					for (int i = 1; i <= numColumns; i++) {
						Object o = rs.getObject(i) == null ? "" : rs.getObject(i);
						bw.write(o.toString(), 0, o.toString().length());
						
						String sep = (i < numColumns) ? "\t" : "\n";
						bw.write(sep, 0, sep.length());
					}
					
					
					if ((++numRows % 500) == 0) log.info("\t" + numRows + " processed");
				}
			}
		}
	}
	
	/**
	 * Gets all of the data and stores it into a file
	 * @return
	 * @throws SQLException
	 * @throws IOException 
	 */
	public void getAllData(BufferedWriter bw) throws SQLException, IOException {
		StringBuilder sql = new StringBuilder(1024);
		sql.append("select a.*, c.*, d.*, b.depuy_ifu_impl_id, b.title_txt, b.url_txt, ");
		sql.append("b.dpy_syn_mediabin_id, b.language_cd, b.part_no_txt, b.default_msg_txt ");
		sql.append("from custom.depuy_ifu a ");
		sql.append("left outer join custom.depuy_ifu_impl b on a.depuy_ifu_id = b.depuy_ifu_id "); 
		sql.append("left outer join custom.depuy_ifu_tg_xr c on b.depuy_ifu_impl_id = c.depuy_ifu_impl_id "); 
		sql.append("left outer join custom.depuy_ifu_tg d on c.depuy_ifu_tg_id = d.depuy_ifu_tg_id ");
		sql.append("order by a.depuy_ifu_id");

		try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
			try (ResultSet rs = ps.executeQuery()) {
				ResultSetMetaData rsmd = rs.getMetaData();
				int numColumns = rsmd.getColumnCount();
				log.info("Column Count: " + numColumns);
				
				// Add the header to the file
				StringBuilder header = new StringBuilder();
				for (int i = 1; i <= numColumns; i++) {
					header.append(rsmd.getColumnName(i));
					if (i < numColumns) header.append("\t");
					else header.append("\n");
				}
				bw.write(header.toString(), 0, header.length());
				
				// Loop each row of db data and convert it to a row of tab delimited data
				int numRows = 0;
				while (rs.next()) {
					for (int i = 1; i <= numColumns; i++) {
						Object o = rs.getObject(i) == null ? "" : rs.getObject(i);
						bw.write(o.toString(), 0, o.toString().length());
						
						String sep = (i < numColumns) ? "\t" : "\n";
						bw.write(sep, 0, sep.length());
					}
					
					
					if ((++numRows % 500) == 0) log.info("\t" + numRows + " processed");
				}
			}
		}
	}
	
	/**
	 * Gets all of the IFU data
	 * @return
	 * @throws SQLException
	 */
	public List<IFUVO> getIfu() throws SQLException {
		String sql = "select * from custom.depuy_ifu";
		
		List<IFUVO> data = new ArrayList<>();
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			try (ResultSet rs = ps.executeQuery()) {
				while(rs.next()) data.add(new IFUVO(rs));
			}
		}
		
		return data;
	}
	
	/**
	 * 
	 * @param data
	 * @throws SQLException
	 */
	public int getIFUImpl(List<IFUVO> data) throws SQLException {
		int counter = 0;
		String sql = "select * from custom.depuy_ifu_impl where depuy_ifu_id = ?";
		for (IFUVO ifvo : data) {
			Map<String, IFUDocumentVO> impl = new HashMap<>();
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setString(1, ifvo.getIfuId());
				
				try(ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						counter ++;
						impl.put(rs.getString("depuy_ifu_id"), new IFUDocumentVO(rs));
					}
					
					// Add the result set to the data
					ifvo.setIfuDocuments(impl);
				}
			}
		}
		
		return counter;
	}
	
	/**
	 * 
	 * @return
	 * @throws InvalidDataException 
	 * @throws DatabaseException 
	 */
	private Connection getConnection() throws DatabaseException, InvalidDataException {
		DatabaseConnection dc = new DatabaseConnection();
		dc.setDriverClass("org.postgresql.Driver");
		dc.setUrl("jdbc:postgresql://sonic:5432/webcrescendo_dev012020_sb?defaultRowFetchSize=25&amp;prepareThreshold=3");
		dc.setUserName("ryan_user_sb");
		dc.setPassword("sqll0gin");
		return dc.getConnection();
	}

}
