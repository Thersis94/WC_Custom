package com.fastsigns.cutover;

//JDK 1.6.0
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

// Log4j 1.2.28
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

// SMT Base Libs
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: DefinedListMapper.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Maps the center pages choices to the appropriate
 * cross reference table in the DB.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Nov 3, 2010<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class DefinedListMapper {
	// Database Connection info
	private final String dbDriver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	private final String dbUrl = "jdbc:sqlserver://sql_server_db:1433;selectMethod=cursor;responseBuffering=adaptive";
	private final String fsUser = "fastsigns";
	private final String fsPass = "fastsigns";
	private final String sbUser = "sb_user";
	private final String sbPass = "sqll0gin";
	private Connection fsConn = null;
	private Connection sbConn = null;
	
	// Misc Parameters
	private static final Logger log = Logger.getLogger("FranchiseCreator");
	
	/**
	 * 
	 */
	public DefinedListMapper() throws Exception {
		log.debug("Starting");
		
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
		DefinedListMapper fc = new DefinedListMapper();
		fc.process();
		log.debug("Complete");
	}

	/**
	 * Processes the business logic for the class
	 * @throws Exception
	 */
	public void process() throws Exception {
		
		for (int i=1; i < 7; i++) {
			this.getFranchiseData(i);
		}
	}

	/**
	 * Retrieves the franchises that have an entry
	 * @return
	 */
	protected void getFranchiseData(int loc) throws SQLException {
		String s = "select * ";
		s += "from custom_franchise a inner join sitebuilder_sb.dbo.dealer_location b on a.storenumber = b.dealer_location_id ";
		s += "where Module" + loc + " in (14,15,16) "; //and storenumber in (196) ";
		s+= "order by storenumber";
		
		Statement stmt = fsConn.createStatement();
		ResultSet rs = stmt.executeQuery(s);
		while (rs.next()) {
			log.debug("Starting Franchise: " + rs.getString("storenumber") + " in location number " + loc);
			
			int type = rs.getInt("module" + loc);
			String params = "";
			if (type == 14) params = rs.getString("DefinedListModule1");
			if (type == 15) params = rs.getString("DefinedListModule2");
			if (type == 16) params = rs.getString("DefinedListModule3");
			log.debug("Params for type[" + type + "] : " + params);
			
			List<Integer> options = this.parseValues(params, type);
			this.assignElements(rs.getInt("storenumber"), loc, options);
		}
		
	}
	
	/**
	 * 
	 * @param fId
	 * @param loc
	 * @param options
	 */
	public void assignElements(int fId, int loc, List<Integer> options) throws SQLException {
		String s = "select cp_location_module_xr_id from sitebuilder_custom.dbo.FTS_CP_LOCATION_MODULE_XR ";
		s += "where FRANCHISE_ID = ? and CP_LOCATION_ID = ?  ";
		
		// Get the location id
		PreparedStatement ps = sbConn.prepareStatement(s);
		ps.setInt(1, fId);
		ps.setInt(2, loc);
		ResultSet rs = ps.executeQuery();
		int locId = 0;
		if (rs.next()) locId = rs.getInt(1);
		ps.close();
		
		// Assign the cross ref for each option
		s = "insert into sitebuilder_custom.dbo.FTS_CP_MODULE_FRANCHISE_XR ";
		s += "(CP_LOCATION_MODULE_XR_ID, CP_MODULE_OPTION_ID, ORDER_NO, create_dt)  ";
		s += "values(?, ?, ?, ?)";
		ps = sbConn.prepareStatement(s);
		
		for (int i=0; i < options.size(); i++) {
			ps.setInt(1, locId);
			ps.setInt(2, options.get(i));
			ps.setInt(3, i + 1);
			ps.setTimestamp(4, Convert.getCurrentTimestamp());
			
			ps.executeUpdate();
		}
		
		ps.close();
	}
	
	
	/**
	 * Parses the pipe delimited data
	 * @param params
	 * @param type
	 * @return
	 */
	public List<Integer> parseValues(String params, int type) {
		List<Integer> data = new ArrayList<Integer>();
		StringTokenizer st = new StringTokenizer(params, "|");
	
		while(st.hasMoreTokens()) {
			Integer val = Convert.formatInteger(st.nextToken());
			val = val + ((type - 3) * 1000);
			
			data.add(val);
			log.debug("Adding val: " + val);
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
