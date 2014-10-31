package com.codman.cu.tracking;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import com.codman.cu.tracking.vo.AccountVO;
import com.codman.cu.tracking.vo.PersonVO;
import com.codman.cu.tracking.vo.PhysicianVO;
import com.codman.cu.tracking.vo.TransactionVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: TransAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since Nov 04, 2010
 ****************************************************************************/
public class TransAction extends AbstractTransAction {
	
	public static final int STATUS_PENDING = 10;
	public static final int STATUS_APPROVED = 20;
	public static final int STATUS_COMPLETE = 30;
	public static final int STATUS_DECLINED = 50;
		
	private Object msg = null;
	
	/**
	 * 
	 */
	public TransAction() {
		super();
	}
	
	public static String getStatusName(int id) {
		if (id == TransAction.STATUS_PENDING) return "Pending";
		if (id == TransAction.STATUS_APPROVED) return "Approved";
		if (id == TransAction.STATUS_COMPLETE) return "Completed";
		if (id == TransAction.STATUS_DECLINED) return "Declined";
		else return "";
	}
	
	/**
	 * 
	 * @param arg0
	 */
	public TransAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.ActionController#build(com.siliconmtn.http.SMTServletRequest)
	 */
	public void build(SMTServletRequest req) throws ActionException {
		TransactionVO vo = new TransactionVO(req);
		String status = StringUtil.checkVal(req.getParameter("statusId"));
		
		try {
			if (vo.getTransactionId() == null || vo.getTransactionId().length() == 0) {
				//insert the new transaction
				insertTransaction(vo);
				
			} else if (status.length() > 0) {
				saveTransactionStatus(vo);
				
				//if complete, bind the unit(s) to this Transaction
				if (Convert.formatInteger(status) == STATUS_COMPLETE) {
					UnitLedgerAction ula = new UnitLedgerAction(actionInit);
					ula.setAttributes(attributes);
					ula.setDBConnection(dbConn);
					ula.build(req);
				}
			}

			// send email if necessary
			try {
				this.sendEmail(req, vo);
			} catch (NullPointerException npe) {
				log.warn(npe); //simply means the email failed
			}
			
		} catch (SQLException sqle) {
			log.error(sqle); //could not write the SQL
		} catch (ActionException ae) {
			log.error(ae); //could not send the emails
		}
		
		// Setup the redirect
    	StringBuffer url = new StringBuffer();
    	url.append(req.getRequestURI());
    	url.append("?type=").append(req.getParameter("type"));
    	url.append("&accountId=").append(req.getParameter("accountId"));
    	url.append("&msg=").append(msg);
    	req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
    	req.setAttribute(Constants.REDIRECT_URL, url.toString());
    	log.debug("redirUrl = " + url);
	}
	
	/**
	 * 
	 * @param req
	 * @param vo
	 * @param msg
	 */
	private void sendEmail(SMTServletRequest req, TransactionVO trans)
		throws ActionException, NullPointerException {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		TrackingEmailer emailer = null;
		
		//load email addresses for all the site admins
		List<UserDataVO> admins = this.retrieveAdministrators(req);
		
		if (site.getOrganizationId().equals("CODMAN_EU")) {
			emailer = new TrackingEmailerEU(this.actionInit);
			if (trans.getStatusId() == STATUS_APPROVED) {
				admins = this.retrieveUsers(req, Integer.valueOf(5));
			}
		} else {
			emailer = new TrackingEmailer(this.actionInit);
		}
		
		emailer.setAttributes(attributes);
		emailer.setDBConnection(dbConn);
			
		//we always need the record for informational use
		AccountVO acct = this.retrieveRecord(req, trans);
		trans = acct.getTransactions().get(0);
		PhysicianVO phys = acct.getPhysicians().get(0);
		PersonVO rep = acct.getRep();
		log.debug("rep=" + rep.getProfileId() + ", sampleAcct#=" + rep.getSampleAccountNo());
		
		if (trans.getStatusId() == STATUS_PENDING) {
			log.debug("sending email for new request submission");
			emailer.submitRequest(req, admins, rep, phys, trans, acct);
			
		} else if (trans.getStatusId() == STATUS_APPROVED) {
			log.debug("sending email for new request approval");
			emailer.approveRequestCS(req, admins, rep, phys, trans, acct);
			emailer.approveRequestRep(req, admins, rep, phys, trans);
			
		} else if (trans.getStatusId() == STATUS_COMPLETE) {
			log.debug("sending email for completed (unit shipped) request");
			emailer.unitShipped(req, admins, rep, phys, trans, acct);
			
		} else if (trans.getStatusId() == STATUS_DECLINED) {
			log.debug("sending email for request declined");
			emailer.requestDeclined(req, admins, rep, trans, acct, phys);
		}
		emailer = null;
	}
	
	private void insertTransaction(TransactionVO vo) throws SQLException {
		log.debug("inserting new Transaction");
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuffer sql = new StringBuffer();
				
		vo.setTransactionId(new UUIDGenerator().getUUID());
		vo.setStatusId(STATUS_PENDING);
		
		sql.append("insert into ").append(customDb).append("codman_cu_transaction ");
		sql.append("(transaction_type_id, status_id, account_id, physician_id, ");
		sql.append("unit_cnt_no, dropship_flg, address_txt, ");
		sql.append("address2_txt, city_nm, state_cd, zip_cd, country_cd, ");
		sql.append("requesting_party_nm, ship_to_nm, notes_txt, create_dt, transaction_id, approving_party_nm) ");
		sql.append("values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ");
		log.debug(sql + vo.getTransactionId());
		log.debug(vo);
		
		PreparedStatement ps = null;
		int i = 0;
		try {
			ps = dbConn.prepareStatement(sql.toString());
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
			ps.executeUpdate();
			msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
			
		} catch (SQLException sqle) {
			msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
			throw(sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
	}
	
	private void saveTransactionStatus(TransactionVO vo) throws SQLException {
		log.debug("updating Transaction status");
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuffer sql = new StringBuffer();
		sql.append("update ").append(customDb).append("codman_cu_transaction ");
		sql.append("set status_id=?, update_dt=?, approving_party_nm=?");
		if (vo.getStatusId() == STATUS_APPROVED) sql.append(", approval_dt=?");
		if (vo.getNotesText() != null) sql.append(", notes_txt=? ");
		sql.append(" where transaction_id=?");
		
		log.debug(sql + " " + vo.getStatusId() + " " + vo.getTransactionId());
		PreparedStatement ps = null;
		int i = 0;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(++i, vo.getStatusId());
			ps.setTimestamp(++i, Convert.getCurrentTimestamp());
			ps.setString(++i, vo.getApprovorName());
			if (vo.getStatusId() == STATUS_APPROVED) ps.setTimestamp(++i, Convert.getCurrentTimestamp());
			if (vo.getNotesText() != null) ps.setString(++i, vo.getNotesText());
			ps.setString(++i, vo.getTransactionId());
			ps.executeUpdate();
			msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
			
		} catch (SQLException sqle) {
			msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
			throw(sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
	}
		
}
