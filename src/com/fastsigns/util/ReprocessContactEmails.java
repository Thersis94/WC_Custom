package com.fastsigns.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.smt.sitebuilder.action.SBActionAdapter;

import com.fastsigns.action.RequestAQuoteSTF;
import com.fastsigns.action.saf.SAFConfig;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;

/****************************************************************************
 * <b>Title</b>: ReprocessContactEmails.java<p/>
 * <b>Description</b>: Called through RequestAQuoteSTF action to re-use the Request object,
 * this class reprocesses emails that were missed due to a down SMTP server.
 * It's only job is to send emails, NOT save data.
 * To run this class, add a hook to RequestAQuote that looks for a query string parameter (you name it).
 * if (req.getParameter(myParam) != null) ReprocessContactEmails.procEmails(req);
 * 
 * See Comments in RequestAQuoteSTF action and ContactFacadeAction for instructions on how to prep those
 * classes for reprocessing.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since May 3, 2011
 ****************************************************************************/
public class ReprocessContactEmails extends SBActionAdapter {

	public ReprocessContactEmails() {
        super();
    }

    public ReprocessContactEmails(ActionInitVO arg0) {
        super(arg0);
    }

    
	public void procEmails(SMTServletRequest req) {
		SAFConfig safConfig = SAFConfig.getInstance("US");
	
		//Update this query as necessary to retrieve the proper records.
		String sql = "select distinct contact_submittal_id from contact_submittal a ";
		sql += "where (a.CREATE_DT between '2014-07-07 17:00:00' and '2014-07-08 09:30:00' ";
		sql += "or a.CREATE_DT between '2014-07-04 13:00:00' and '2014-07-05 22:30:00') ";
		sql += "and action_id=?";
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql);
			ps.setString(1, safConfig.getContactUsActionId());
			//ps.setString(1, RequestAQuoteSTF.TransactionStep.fileSaved.toString());
			log.debug(sql);

			ResultSet rs = ps.executeQuery();
			while (rs.next()) { //for each submittal
				log.debug("********* processing " + rs.getString("contact_submittal_id"));
				req.setParameter("contactSubmittalId", rs.getString("contact_submittal_id"));
				
				try {
					RequestAQuoteSTF raq = new RequestAQuoteSTF(actionInit);
					raq.setAttributes(attributes);
					raq.setDBConnection(dbConn);
					raq.sendEmail(req, safConfig);
					//raq.recordStep(req.getParameter("csi"), TransactionStep.complete, safConfig);
				} catch (ActionException ae) {
					log.error(ae);
				}
				log.error("finished!  Sent emails to " + req.getParameter("userEmail") + " and " + req.getParameter("dealerEmail"));
			}
		} catch (SQLException sqle) {
			log.error(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
	}
}