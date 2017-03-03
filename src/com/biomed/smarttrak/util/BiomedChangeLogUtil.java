/**
 *
 */
package com.biomed.smarttrak.util;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.pool.SMTDBConnection;
//import com.smt.sitebuilder.changelog.ChangeLogUtil;

/****************************************************************************
 * <b>Title</b>: BiomedChangeLogUtil.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Helper Util for Generating WC_Sync and ChangeLog Records
 * for Biomed records.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Mar 3, 2017
 ****************************************************************************/
public class BiomedChangeLogUtil {

	private SMTDBConnection dbConn;
	//private ChangeLogUtil clu;

	public BiomedChangeLogUtil(SMTDBConnection dbConn) {
		this.dbConn = dbConn;
		//clu = new ChangeLogUtil(dbConn);
	}

	public void createChangeLog(ActionRequest req) {
		//Get Attributes off Req.
		
		//Save to Approvals.

		//Save to ChangeLog.
	}

}
