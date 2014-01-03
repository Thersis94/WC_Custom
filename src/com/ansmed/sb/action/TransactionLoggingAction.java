package com.ansmed.sb.action;

// JDK 1.6
import java.sql.PreparedStatement;

//Baselibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

// SB II libs
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: TransactionLoggingAction.java<p/>
 * <b>Description: </b> Adds an entry to the transaction table.  This is used 
 * for tracking which physicians were updated and when.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since July 15, 2010
 ****************************************************************************/
public class TransactionLoggingAction extends SimpleActionAdapter {
	
	public static final String TRANSACTION_TYPE = "transactionType";
	public static final String SURGEON_ID = "surgeonTransactionID";
	
	public TransactionLoggingAction(ActionInitVO ai) {
		super(ai);
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		
		// set the transaction type
		String type = (String)this.getAttribute(TRANSACTION_TYPE);
		if (type == null) return;
		
		// determine the surgeon ID to use for logging
		String surgLogId = (String)this.getAttribute(SURGEON_ID);
		if (surgLogId == null) surgLogId = StringUtil.checkVal(req.getParameter("surgeonId"));
				
		// write the transaction
		String schema = (String)getAttribute("customDbSchema");
		UserDataVO user = (UserDataVO)req.getSession().getAttribute(Constants.USER_DATA);
		StringBuffer sql = new StringBuffer();
		sql.append("insert into ").append(schema).append("ans_transaction ");
		sql.append("(transaction_id, surgeon_id, profile_id, transaction_type_nm, ");
		sql.append("create_dt) values (?,?,?,?,?)");
		log.debug("Transaction SQL: " + sql);
		log.debug("Params: surgeonId|profileId|type|timestamp: " + surgLogId + "|" 
				+ user.getProfileId() + "|" + type + "|" + Convert.getCurrentTimestamp());
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, new UUIDGenerator().getUUID());
			ps.setString(2, surgLogId);
			ps.setString(3, user.getProfileId());
			ps.setString(4, type);
			ps.setTimestamp(5, Convert.getCurrentTimestamp());
			ps.executeUpdate();
		} catch (Exception e) {
			StringBuffer msg = new StringBuffer();
			msg.append("Error adding transaction for ANS Physician update, Type:");
			msg.append(type).append("| User: " ).append(user.getFirstName());
			msg.append(" ").append(user.getLastName());
			log.error(msg.toString() , e);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
	}

}
