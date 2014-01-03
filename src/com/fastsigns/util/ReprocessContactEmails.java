package com.fastsigns.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

import com.fastsigns.action.RequestAQuoteSTF;
import com.fastsigns.action.RequestAQuoteSTF.TransactionStep;
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
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		SAFConfig safConfig = SAFConfig.getInstance(site.getCountryCode());
		
		String sql = "select a.* from contact_submittal a ";
		sql += "inner join contact_data b on a.contact_submittal_id=b.contact_submittal_id ";
		sql += "and b.contact_field_id=? ";
		sql += "where cast(b.value_txt as varchar(50))=?";
		log.debug(sql);
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql);
			ps.setString(1, safConfig.getTransactionStageFieldId());
			ps.setString(2, RequestAQuoteSTF.TransactionStep.fileSaved.toString());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) { //for each submittal
				log.debug("********* processing " + rs.getString("contact_submittal_id"));
				req.setParameter("csi", rs.getString("contact_submittal_id"));
				
				try {
					RequestAQuoteSTF raq = new RequestAQuoteSTF(actionInit);
					raq.setAttributes(attributes);
					raq.setDBConnection(dbConn);
					raq.sendEmail(req, safConfig);
					raq.recordStep(req.getParameter("csi"), TransactionStep.complete, safConfig);
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