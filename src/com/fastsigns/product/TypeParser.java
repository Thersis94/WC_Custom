package com.fastsigns.product;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: TypeParser.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Oct 25, 2010<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class TypeParser {
	protected static Logger log = Logger.getLogger(TypeParser.class);
	private final String dbDriver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	private final String dbUrl = "jdbc:sqlserver://sql_server_db:1433;selectMethod=cursor;responseBuffering=adaptive";
	private final String dbUser = "sb_user";
	private final String dbPass = "sqll0gin";
	
	public static final String EVENT_TYPE = "events";
	public static final String INDUSTRY_TYPE = "industry";
	
	private Connection conn;
	
	Map<Integer, String> event = new HashMap<Integer, String>();
	Map<Integer, String> industry = new HashMap<Integer, String>();
	Map<Integer, String> prod = new HashMap<Integer, String>();
	
	/**
	 * 
	 */
	private TypeParser() {
		BasicConfigurator.configure();
		
		// Laod the maps
		this.loadEvent();
		this.loadIndustry();
		this.loadProducts();
		
		// open the db conn
		this.openDBConnection();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		TypeParser tp = new TypeParser();
		log.debug("Starting ...");
		
		Set<Integer> s = tp.prod.keySet();
		for (Iterator<Integer> iter = s.iterator(); iter.hasNext(); ) {
			Integer key = iter.next();
			String path = tp.prod.get(key);
			List<Integer> evIds = tp.getTypeList(EVENT_TYPE, key);
			List<Integer> inIds = tp.getTypeList(INDUSTRY_TYPE, key);
			
			// Process the event entries
			for(int i=0; i < evIds.size(); i++) {
				int catId = evIds.get(i);
				String catCode = tp.event.get(catId);
				tp.addCatXR(catCode, path);
			}
			log.debug("******************* " + evIds.size());
			// Process the industry entries
			for(int i=0; i < inIds.size(); i++) {
				int catId = inIds.get(i);
				String catCode = tp.industry.get(catId);
				tp.addCatXR(catCode, path);
			}
			
			log.debug("^^^^^^^^^^^^^^^^^^^^^ " + inIds.size());
		}
		
		try {
			tp.conn.close();
		} catch(Exception e) {}
		
		log.debug("Complete");
	}
	
	
	private void addCatXR(String catId, String pId) throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append("insert into product_category_xr (product_category_cd, product_id, ");
		sb.append("order_no, create_dt) values (?,?,?,?)");
		log.debug("Add Cat SQL: " + sb);
		
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sb.toString());
			ps.setString(1, catId);
			ps.setString(2, pId);
			ps.setInt(3, 1);
			ps.setTimestamp(4, Convert.getCurrentTimestamp());
			ps.executeUpdate();
			log.debug("adding: " + catId + "|" + pId);
		} finally {
			try {
				ps.close();
			} catch(Exception e){}
		}
	}
	
	private List<Integer> getTypeList(String type, int prodId) throws Exception {
		String s = "select " + type + " from fastsigns.dbo.custom_FastSignsProductSignType  ";
		s+= "where FastSignsProductSignTypeID = " + prodId;
		
		String ids = null;
		List<Integer>  data = new ArrayList<Integer>();
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(s);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				ids = rs.getString(1);
				StringTokenizer st = new StringTokenizer(ids, "|");
				while (st.hasMoreTokens()) {
					data.add(Convert.formatInteger(st.nextToken()));
				}
			}
		} finally {
			try {
				ps.close();
			} catch(Exception e){}
		}
		
		
		return data;
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
	
	
	private void loadEvent() {
		// Add the event types
		event.put(6,"Fundraiser-Signs");
		event.put(7,"Community-Event-Signs");
		event.put(8,"Concert-Signs");
		event.put(3,"Convention-Signs");
		event.put(9,"Corporate-Event-Signs");
		event.put(10,"Election-Signs");
		event.put(11,"Fair-Signs");
		event.put(15,"Grand-Opening-Signs");
		event.put(12,"Parade-Signs");
		event.put(5,"Presentation-Signs");
		event.put(13,"Press-Conference-Signs");
		event.put(14,"Product-Launch-Signs");
		event.put(2,"Sporting-Event-Signs");
		event.put(1,"Trade-Show-Signs");
	}
	
	private void loadIndustry() {
		// Add the industry types
		industry.put(1,"Agriculture-Signs");
		industry.put(2,"Apartment-Signs");
		industry.put(3,"Auto-Signs");
		industry.put(4,"Advertising-Signs");
		industry.put(5,"Architectural-Signs");
		industry.put(6,"Bank-Signs");
		industry.put(7,"Boat-Signs");
		industry.put(8,"Business-Services-Signs");
		industry.put(9,"Church-Signs");
		industry.put(10,"Construction-Signs");
		industry.put(11,"Entertainment-Signs");
		industry.put(12,"Political-Signs");
		industry.put(13,"Hospital-Signs");
		industry.put(14,"Hotel-Signs");
		industry.put(15,"Manufacturing-Signs");
		industry.put(16,"Media-Communication-Signs");
		industry.put(17,"Oil-and-Gas-Signs");
		industry.put(18,"Organization-Signs");
		industry.put(19,"Personal-Business-Signs");
		industry.put(20,"Commercial-Real-Estate-Signs");
		industry.put(21,"Residential-Real-Estate-Signs");
		industry.put(22,"Restaurant-Signs");
		industry.put(23,"Retail-Signs");
		industry.put(24,"School-Signs");
		industry.put(25,"Transportation-Signs");
		industry.put(26,"Utilities-Signs");
		industry.put(27,"Wholesale-Distribution-Signs");
	}
	
	/**
	 * Loads the old/new product ids
	 */
	private void loadProducts() {
		// Get the products
		prod.put(1,"Banners");
		prod.put(12,"Building-Signs");
		prod.put(4,"Digital-Signage");
		prod.put(7,"Electrical-Signs");
		prod.put(3,"Custom-Labels");
		prod.put(10,"Point-of-Purchase-Signs");
		prod.put(2,"Custom-Posters");
		prod.put(8,"Promotional-Products");
		prod.put(13,"Regulatory-Signs");
		prod.put(9,"Trade-Show-Display-Graphics");
		prod.put(6,"Trade-Show-Displays");
		prod.put(14,"Unique-Sign-Ideas");
		prod.put(5,"Vehicle-Graphics");
		prod.put(11,"Outdoor-Signs");
	}

}
