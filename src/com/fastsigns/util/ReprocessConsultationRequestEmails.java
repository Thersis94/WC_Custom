/**
 * 
 */
package com.fastsigns.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.fastsigns.action.tvspot.TVSpotDlrContactAction;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: ReprocessConsultationRequestEmails.java
 * <p/>
 * <b>Project</b>: WC_Custom
 * <p/>
 * <b>Description: </b> Utility Class used to reprocess Consultation requests
 * in the event that something happens with the email system.  See also 
 * TVSpotDlrContactAction and ContactFacadeAction for instructions on how to
 * prep those actions for email reprocessing.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author raptor
 * @version 1.0
 * @since Jul 8, 2014
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public class ReprocessConsultationRequestEmails extends SBActionAdapter {

	public ReprocessConsultationRequestEmails() {
        super();
    }

    public ReprocessConsultationRequestEmails(ActionInitVO arg0) {
        super(arg0);
    }
	
    /**
     * Call out to this method which retrieves the records, updates the request object
     * and calls back out to the TVSpotDlrContactAction to reprocess the email submissin.
     * @param req
     */
	public void procEmails(SMTServletRequest req) {
		String dlrLocnFieldId = req.getParameter("reprocessDealerKey");
		String sql = getSQLStatement();
		log.debug(sql);
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql);
			int i = 0;
			//Update this if outside the US
			ps.setString(1, "c0a8023727d3d72cdfdc15044ef2d020");
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) { //for each submittal
				if(i > 4) {
				log.debug("********* processing " + rs.getString("contact_submittal_id"));
				req.setParameter("csi", rs.getString("contact_submittal_id"));
				req.setParameter(dlrLocnFieldId, rs.getString("DEALER_LOCATION_ID"));
				req.setParameter("cust_email_address_txt", rs.getString("email_address_txt"));
				req.setParameter("actionName", rs.getString("action_nm"));
				req.setParameter(Constants.REQ_FORM_VALIDATED, "true");
				try {
					TVSpotDlrContactAction tvca = new TVSpotDlrContactAction(actionInit);
					tvca.setAttributes(attributes);
					tvca.setDBConnection(dbConn);
					tvca.build(req);
				} catch (ActionException ae) {
					log.error(ae);
				}
				log.error("finished!  Sent emails to " + req.getParameter("cust_email_address_txt") + " and " + req.getParameter("contactEmailAddress"));
				}
				i++;
			}
		} catch (SQLException sqle) {
			log.error(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
	}

	/**
	 * Update the where clause to retrieve the necessary records.  
	 * @return
	 */
	public String getSQLStatement() {
		String sql = "select distinct contact_submittal_id, dealer_location_id, email_Address_txt, action_nm ";
		sql += "from contact_submittal a inner join sb_action b on a.action_id = b.action_group_id ";
		sql += "where (a.CREATE_DT between '2014-07-07 17:00:00' and '2014-07-08 09:30:00' ";
		sql += "or a.CREATE_DT between '2014-07-04 13:00:00' and '2014-07-05 22:30:00') ";
		sql += "and a.action_id=?";
		
		return sql;
	}
}
