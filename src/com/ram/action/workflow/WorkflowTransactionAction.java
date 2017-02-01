package com.ram.action.workflow;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.workflow.data.WorkflowTransactionVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: WorkflowTransactionAction.java
 * <p/>
 * <b>Project</b>: WC_Custom
 * <p/>
 * <b>Description: </b> Action that handles all basic workflow transaction
 * related interactions.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author Tim Johnson
 * @version 1.0
 * @since June 18, 2015
 *        <p/>
 *        <b>Changes: June 18, 2015 - Created Class</b>
 ****************************************************************************/
public class WorkflowTransactionAction extends SBActionAdapter {
	
	// Constant for the default of how far back we retrieve transactions.
	public static final int DAYS_BACK = 7;

	/**
	 * Default Constructor
	 */
	public WorkflowTransactionAction() {
		super();
	}

	/**
	 * General Constructor with ActionInitVO data
	 * @param actionInit
	 */
	public WorkflowTransactionAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	
	/**
	 * If we have a service and/or eventType then retrieve the transactions
	 * for that type of workflow, otherwise retrieve all transactions.
	 * 
	 * We only return transactions created within the configured amount of time above.
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		//Prevent double call at page load.  This ensures only the ajax call triggers load.
		if(req.hasParameter("amid")) retrieveTransactions(req);
	}
	
	public void retrieveTransactions(ActionRequest req) throws ActionException {
		//Instantiate the transaction list for results and check for lookup type.
		List<WorkflowTransactionVO> transactions = new ArrayList<WorkflowTransactionVO>();
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		boolean isServiceLookup = req.hasParameter("service");
		boolean isEventTypeLookup = req.hasParameter("eventType");
		String[] eventTypeCodes = null;

		StringBuilder sb = new StringBuilder(250);
		sb.append("select * from ").append(schema).append("RAM_WORKFLOW_TRANSACTION wt ");
		sb.append("inner join ").append(schema).append("RAM_WORKFLOW w ");
		sb.append("ON wt.WORKFLOW_ID = w.WORKFLOW_ID ");
		sb.append("left join ").append(schema).append("RAM_CUSTOMER_LOCATION cl ");
		sb.append("ON wt.CUSTOMER_LOCATION_ID = cl.CUSTOMER_LOCATION_ID ");
		sb.append("where wt.CREATE_DT > dateadd(day, -?, getdate()) ");
		
		// Handle service and event type lookups
		if (isServiceLookup)
			sb.append("and SERVICE_CD = ? ");

		if (isEventTypeLookup) {
			eventTypeCodes = req.getParameterValues("eventType");
			
			sb.append("and WORKFLOW_EVENT_TYPE_CD in (");
			for (int i = 1; i < eventTypeCodes.length; i++)
				sb.append("?,");
			sb.append("?) ");
		}
		
		//Log sql Statement for verification
		log.debug("sql: " + sb.toString());
		
		//Build the Statement and execute
		PreparedStatement ps = null;
		int i = 1;
		
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setInt(i++, DAYS_BACK);
			
			if (isServiceLookup)
				ps.setString(i++, req.getParameter("service"));

			if (isEventTypeLookup)
				for (String eventTypeCode : eventTypeCodes)
					ps.setString(i++, eventTypeCode);
			
			//Loop the results and add to transactions list.
			ResultSet rs = ps.executeQuery();
			while(rs.next())
				transactions.add(new WorkflowTransactionVO(rs));
		} catch(SQLException sqle) {
			log.error("Error retrieving transaction list", sqle);
			throw new ActionException(sqle);
		} finally {
			DBUtil.close(ps);
		}
		
		//Return List to View
		this.putModuleData(transactions);
	}
}
