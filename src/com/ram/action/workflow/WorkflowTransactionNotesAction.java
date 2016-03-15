package com.ram.action.workflow;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.workflow.data.WorkflowTransactionStepNoteVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: WorkflowTransactionNotesAction.java
 * <p/>
 * <b>Project</b>: WC_Custom
 * <p/>
 * <b>Description: </b> Action that handles all basic workflow transaction
 * notes related interactions.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author Tim Johnson
 * @version 1.0
 * @since June 19, 2015
 *        <p/>
 *        <b>Changes: June 19, 2015 - Created Class</b>
 ****************************************************************************/
public class WorkflowTransactionNotesAction extends SBActionAdapter {
	
	/**
	 * Default Constructor
	 */
	public WorkflowTransactionNotesAction() {
		super();
	}

	/**
	 * General Constructor with ActionInitVO data
	 * @param actionInit
	 */
	public WorkflowTransactionNotesAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	
	/**
	 * Returns transaction note/error data for the specified workflow transaction
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		//Prevent double call at page load.  This ensures only the ajax call triggers load.
		if(req.hasParameter("amid")) retrieveTransactionNotes(req);
	}
	
	public void retrieveTransactionNotes(SMTServletRequest req) throws ActionException {
		//Instantiate the transaction note list for results
		List<WorkflowTransactionStepNoteVO> transactionNotes = new ArrayList<WorkflowTransactionStepNoteVO>();
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);

		StringBuilder sb = new StringBuilder(200);
		sb.append("select * from ").append(schema).append("RAM_WORKFLOW_TRANSACTION_STEP_NOTE wtsn ");
		sb.append("inner join ").append(schema).append("RAM_WORKFLOW_TRANSACTION_STEP wts ");
		sb.append("ON wtsn.WORKFLOW_TRANSACTION_STEP_ID = wts.WORKFLOW_TRANSACTION_STEP_ID ");
		sb.append("where wts.WORKFLOW_TRANSACTION_ID = ? ");
		
		//Log sql Statement for verification
		log.debug("sql: " + sb.toString());
		
		//Build the Statement and execute
		PreparedStatement ps = null;
		
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, req.getParameter("workflowTransactionId"));
			
			//Loop the results and add to transactions list.
			ResultSet rs = ps.executeQuery();
			while(rs.next())
				transactionNotes.add(new WorkflowTransactionStepNoteVO(rs));
		} catch(SQLException sqle) {
			log.error("Error retrieving transaction step note list", sqle);
			throw new ActionException(sqle);
		} finally {
			DBUtil.close(ps);
		}
		
		//Return List to View
		this.putModuleData(transactionNotes);
	}
}
