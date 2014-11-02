/**
 * 
 */
package com.codman.cu.tracking;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import com.codman.cu.tracking.vo.TransactionVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: TransIcpAction.java<p/>
 * <b>Description: Handles the processing of ICP unit refurbishment.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Erik Wingo
 * @version 1.0
 * @since Oct 28, 2014
 ****************************************************************************/
public class TransIcpAction extends AbstractTransAction {
	
	private Object msg = null;
	
	/**
	 * Possible statuses for an ICP transaction.
	 */
	public static enum Status{
		OLD_SENT(210,"Old Country Sent"),
		CREDIT_CONF(220,"GMED Credits Confirmed"),
		NEW_ORDER(230,"New Order Placed"),
		REPAIR_RECEIVED(240,"Received by Repair Center"),
		REPAIR_SERVICED(250, "Unit Refurbishment Complete"),
		REPAIR_SENT(260, "Sent Back To EDC"),
		NEW_SENT(270, "Sent to New Country");
		
		private final int code;
		private final String name;
		Status(int code, String name){
			this.code = code;
			this.name = name;
		}
		public String getStatusName(){ return name; }
		public Integer getStatusCode(){ return new Integer(code); }
	}
	
	/**
	 * Default Constructor
	 */
	public TransIcpAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public TransIcpAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	public void build( SMTServletRequest req ) throws ActionException {
		
		//Create the transaction 
		TransactionVO trans = new TransactionVO(req);
		//Check the request type
		String rType = StringUtil.checkVal( req.getParameter("rType") );
			
		try{
			msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
			
			switch( rType ){
			//When shipment sent out to GMED for credit (logs product serial, date...)
			case "shipOld":
				trans.setStatusId(Status.OLD_SENT.getStatusCode());
				insertTransaction( trans );
				break;
				
			//When a credit is applied to the old county for the unit
			case "credit":
				//change the status
				trans.setStatusId(Status.CREDIT_CONF.getStatusCode());
				changeTransaction(trans);
				break;
				
			//When the new hospital creates an order
			case "newOrder":
				//change the status
				trans.setStatusId(Status.NEW_ORDER.getStatusCode());
				changeTransaction(trans);
				
				//transfer the unit to the new account
				SMTActionInterface uta = new UnitTransferAction( this.actionInit );
				uta.setAttributes(this.attributes);
				uta.setDBConnection(this.dbConn);
				uta.build(req);
				uta = null;
				break;
				
			//When the repair center receives the box
			case "repairRecieved":
				trans.setStatusId(Status.REPAIR_RECEIVED.getStatusCode());
				changeTransaction( trans );
				break;
				
			//When the box is in service
			case "repairServiced":
				trans.setStatusId(Status.REPAIR_SERVICED.getStatusCode());
				changeTransaction( trans );
				break;
				
			//When the repairs are complete and the box is sent out to the edc
			case "repairComplete":
				trans.setStatusId(Status.REPAIR_SENT.getStatusCode());
				changeTransaction( trans );
				break;
			
			//When the edc has sent the box to the new country
			case "shipNew":
				trans.setStatusId(Status.REPAIR_SENT.getStatusCode());
				changeTransaction( trans );
				break;
			
			//For editing values without necessarily updating the status
			case "edit":
				changeTransaction(trans);
				break;
			//Unknown request, just log and return
			default:
				log.error("Invalid Request.");
				msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
				break;
			}
		} catch (SQLException se){
			msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
			log.error(se);
		}
		
		//Send necessary Emails
		sendEmail( req, trans );
		
		//Redirect 
		setupRedir( req );
	}
	
	/**
	 * Redirects after the build method is completed
	 * @param req
	 */
	private void setupRedir( SMTServletRequest req ){
		
    	StringBuilder url = new StringBuilder();
    	url.append(req.getRequestURI());
    	url.append("?type=").append(req.getParameter("type"));
    	url.append("&accountId=").append(req.getParameter("accountId"));
    	url.append("&msg=").append(msg);
    	req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
    	req.setAttribute(Constants.REDIRECT_URL, url.toString());
    	log.debug("redirUrl = " + url);
	}
	
	/**
	 * Inserts an entry into the transaction table for this unit
	 * @param vo
	 */
	private void insertTransaction( TransactionVO vo ) throws SQLException{
		log.debug("Creating new transaction.");
		
		//generate the insert statement
		StringBuilder sql = makeTransInsert();
		vo.setTransactionId(new UUIDGenerator().getUUID());
		log.debug(sql+"|"+vo.getTransactionId());
		
		try( PreparedStatement ps = dbConn.prepareStatement(sql.toString())){
			int i = 0;
			ps.setInt(++i, vo.getTransactionTypeId());
			ps.setString(++i, String.valueOf(vo.getStatusId()));
			ps.setString(++i, vo.getAccountId());
			ps.setString(++i, vo.getPhysician().getPhysicianId());
			ps.setInt(++i, vo.getUnitCount());
			ps.setInt(++i, vo.getDropShipFlag());
			ps.setString(++i, vo.getDropShipAddress().getAddress());
			ps.setString(++i, vo.getDropShipAddress().getAddress2());
			ps.setString(++i, vo.getDropShipAddress().getCity());
			ps.setString(++i, vo.getDropShipAddress().getState());
			ps.setString(++i, vo.getDropShipAddress().getZipCode());
			ps.setString(++i, vo.getDropShipAddress().getCountry());
			ps.setString(++i, vo.getRequestorName());
			ps.setString(++i, vo.getShipToName());
			ps.setString(++i, vo.getNotesText());
			ps.setTimestamp(++i, Convert.getCurrentTimestamp());
			ps.setString(++i, vo.getTransactionId());
			ps.setString(++i, vo.getApprovorName());
			ps.setString(++i, vo.getCreditText());
			ps.executeUpdate();
		}
	}
	
	/**
	 * Generates the insert statement for new transactions.
	 * @return SQL statement
	 */
	private StringBuilder makeTransInsert(){
		
		StringBuilder sql = new StringBuilder(440);
		sql.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA)).append("codman_cu_transaction ");
		sql.append("(transaction_type_id, status_id, account_id, physician_id, ");
		sql.append("unit_cnt_no, dropship_flg, address_txt, address2_txt, ");
		sql.append("city_nm, state_cd, zip_cd, country_cd, requesting_party_nm, ");
		sql.append("ship_to_nm, notes_txt, create_dt, transaction_id, ");
		sql.append("approving_party_nm, credit_txt ) ");
		sql.append("values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ");
		
		return sql;
	}
	
	/**
	 * Changes the status of a 
	 * @param vo
	 */
	private void changeTransaction( TransactionVO vo ) throws SQLException{
		log.debug("Changing Transaction Status.");
		
		//build statement
		StringBuilder sql = new StringBuilder(170);
		sql.append("update ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA)).append("CODMAN_CU_TRANSACTION ");
		//The approving party is the person updating the status of the transaction
		sql.append("set STATUS_ID=?, UPDATE_DT=?, APPROVING_PARTY_NM=?, APPROVAL_DT=?");
		if (vo.getNotesText() != null){
			sql.append(", NOTES_TXT=?");
		}
		if ( ! StringUtil.checkVal( vo.getCreditText() ).isEmpty() ){
			sql.append(",CREDIT_TXT=?");
		}
		sql.append(" where TRANSACTION_ID=?");
		log.debug(sql+" | "+vo.getTransactionId());
		
		try( PreparedStatement ps = dbConn.prepareStatement(sql.toString())){
			int i = 0;
			ps.setInt(++i, vo.getStatusId());
			ps.setTimestamp(++i, Convert.getCurrentTimestamp());
			ps.setString(++i, vo.getApprovorName());
			ps.setTimestamp(++i, Convert.getCurrentTimestamp());
			if (vo.getNotesText() != null) 
				ps.setString(++i, vo.getNotesText());
			if (! StringUtil.checkVal( vo.getCreditText() ).isEmpty() ) 
				ps.setString(++i, vo.getCreditText());
			ps.setString(++i, vo.getTransactionId());
			ps.executeUpdate();
		}
	}
	
	/**
	 * Sends notification email to designated recipients.
	 * @param req
	 * @param trans
	 */
	@SuppressWarnings("unused")
	private void sendEmail( SMTServletRequest req, TransactionVO trans ) 
	throws ActionException{
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		TrackingEmailerICP mailer = new TrackingEmailerICP(this.actionInit);
		
		//get admins
		List<UserDataVO> adminList = retrieveAdministrators( req );
		
		//setup mailer
		mailer.setAttributes(this.attributes);
		mailer.setDBConnection(this.dbConn);
		
		mailer.sendICPMessage( req, adminList, trans );
	}

}
