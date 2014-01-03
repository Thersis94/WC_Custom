package com.depuy.sitebuilder.datafeed;

import java.util.Map;

import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;

/****************************************************************************
 * <b>Title</b>: Report.java<p/>
 * <b>Description: Interface that defines the Report charactersistics for the 
 * various data feed reports</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2007<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Mar 9, 2007
 ****************************************************************************/
public interface Report {
		
	/**
	 * Stores the attributes necessary to generate the report
	 * @param attributes
	 */
	public void setAttibutes(Map<String, Object> attributes);
	
	/**
	 * Dtabase Connection
	 * @param conn
	 */
	public void setDatabaseConnection(SMTDBConnection conn);
	
	/**
	 * Performs the report retrieval.
	 * @param req
	 * @return
	 * @throws DatabaseException When database in inaccessible or the qwuery fails
	 * @throws InvalidDataException Not enough data provided to generate the report
	 */
	public Object retrieveReport(SMTServletRequest req) 
	throws DatabaseException,InvalidDataException; 
}
