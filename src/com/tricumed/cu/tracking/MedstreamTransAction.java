package com.tricumed.cu.tracking;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.tricumed.cu.tracking.vo.AccountVO;
import com.tricumed.cu.tracking.vo.PersonVO;
import com.tricumed.cu.tracking.vo.PhysicianVO;
import com.tricumed.cu.tracking.vo.TransactionVO;

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
public class MedstreamTransAction extends AbstractTransAction {

	public MedstreamTransAction() {
		super();
	}

	/**
	 * 
	 * @param arg0
	 */
	public MedstreamTransAction(ActionInitVO arg0) {
		super(arg0);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.ActionController#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		Object msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		TransactionVO vo = new TransactionVO(req);

		try {
			if (vo.getTransactionId() == null || vo.getTransactionId().length() == 0) {
				//insert the new transaction
				insertTransaction(vo);

			} else if (vo.getStatus() != null) {
				saveTransactionStatus(vo);

				//if complete, bind the unit(s) to this Transaction
				if (vo.getStatus() == Status.COMPLETE) {
					UnitLedgerAction ula = new UnitLedgerAction(actionInit);
					ula.setAttributes(attributes);
					ula.setDBConnection(dbConn);
					ula.build(req);
				}
			}

			// send email if necessary
			sendEmailNotif(req, vo);

		} catch (SQLException sqle) {
			log.error(sqle); //could not write the SQL
			msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
		}

		// Setup the redirect
		StringBuilder url = new StringBuilder(200);
		url.append(req.getRequestURI());
		url.append("?type=").append(req.getParameter("type"));
		url.append("&accountId=").append(req.getParameter("accountId"));
		url.append("&msg=").append(msg);
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, url.toString());
		log.debug("redirUrl = " + url);
	}


	private void sendEmailNotif(ActionRequest req, TransactionVO trans) {
		try {
			sendEmail(req, trans);
		} catch (Exception npe) {
			log.error("could not send medstream email", npe);
		}
	}


	/**
	 * 
	 * @param req
	 * @param vo
	 * @param msg
	 */
	private void sendEmail(ActionRequest req, TransactionVO trans) throws ActionException {
		MedstreamEmailer emailer = new MedstreamEmailer(dbConn, attributes);

		//load email addresses for all the site admins
		List<UserDataVO> admins = this.retrieveAdministrators(req);

		//we always need the record for informational use
		AccountVO acct = this.retrieveRecord(req, trans);
		trans = acct.getTransactions().get(0);
		PhysicianVO phys = acct.getPhysicians().get(0);
		PersonVO rep = acct.getRep();
		log.debug("rep=" + rep.getProfileId() + ", sampleAcct#=" + rep.getSampleAccountNo());

		if (trans.getStatus() == Status.PENDING) {
			log.debug("sending email for new request submission");
			emailer.submitRequest(req, admins, rep, phys, trans, acct);

		} else if (trans.getStatus() == Status.APPROVED) {
			log.debug("sending email for new request approval");
			emailer.approveRequestCS(req, admins, rep, trans, acct);
			emailer.approveRequestRep(req, admins, rep, phys, trans);

		} else if (trans.getStatus() == Status.COMPLETE) {
			log.debug("sending email for completed (unit shipped) request");
			emailer.unitShipped(req, rep, phys, trans, acct);

		} else if (trans.getStatus() == Status.DECLINED) {
			log.debug("sending email for request declined");
			emailer.requestDeclined(req, rep, trans, acct, phys);
		}
	}


	private void saveTransactionStatus(TransactionVO vo) throws SQLException {
		log.debug("updating Transaction status");
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder();
		sql.append("update ").append(customDb).append("tricumed_cu_transaction ");
		sql.append("set status_id=?, update_dt=?, approving_party_nm=?");
		if (vo.getStatus() == Status.APPROVED) sql.append(", approval_dt=?");
		if (vo.getNotesText() != null) sql.append(", notes_txt=? ");
		sql.append(" where transaction_id=?");
		log.debug(sql + " " + vo.getStatusId() + " " + vo.getTransactionId());

		int i = 0;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setInt(++i, vo.getStatusId());
			ps.setTimestamp(++i, Convert.getCurrentTimestamp());
			ps.setString(++i, vo.getApprovorName());
			if (vo.getStatus() == Status.APPROVED) ps.setTimestamp(++i, Convert.getCurrentTimestamp());
			if (vo.getNotesText() != null) ps.setString(++i, vo.getNotesText());
			ps.setString(++i, vo.getTransactionId());
			ps.executeUpdate();

		} catch (SQLException sqle) {
			throw(sqle);
		}
	}
}