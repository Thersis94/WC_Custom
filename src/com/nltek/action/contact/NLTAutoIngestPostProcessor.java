package com.nltek.action.contact;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.SMTMail;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: NLTAutoIngestPostProcessor<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Mar 07, 2011
 ****************************************************************************/
public class NLTAutoIngestPostProcessor extends SBActionAdapter {
	
	public NLTAutoIngestPostProcessor() {
	}

	public NLTAutoIngestPostProcessor(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		log.info("starting NLT post processor");
		
		String rcpt = req.getParameter("pfl_EMAIL_ADDRESS_TXT");
		if (!StringUtil.isValidEmail(rcpt)) return;  //can't send an email to an invalid address!
		
		SMTMail mail = new SMTMail((String)getAttribute(Constants.CFG_SMTP_SERVER));
		mail.setUser((String)getAttribute(Constants.CFG_SMTP_USER));
		mail.setPassword((String)getAttribute(Constants.CFG_SMTP_PASSWORD));
		mail.setFrom("jim_mckain@nltek.com", "Jim McKain");
		mail.setBCC(new String[] { "info@nltek.com" });
		mail.setRecpt(new String[] { rcpt });
		mail.setSubject("Your AutoIngest Direct Inquiry");
		mail.setHtmlBody(this.getMessage(req));
		
		try {
			mail.postMail();
		} catch (Exception e) {
			log.error(e);
		}
		
	}
	
	protected String getMessage(SMTServletRequest req) {
		StringBuilder msg = new StringBuilder();
		msg.append("<p>Dear ").append(req.getParameter("pfl_PREFIX_NM"));
		msg.append(" ").append(req.getParameter("pfl_FIRST_NM")).append(" ");
		msg.append(req.getParameter("pfl_LAST_NM")).append(",</p>");
		
		msg.append("<p>I would be happy to assist you in any way with questions or help ");
		msg.append("setting up AutoIngest for use in the Avid environment. If you need ");
		msg.append("anything please do <a href=\"mailto:jim_mckain@nltek.com\">email me</a>. ");
		msg.append("I am available to help if you request it.</p>");
		
		msg.append("<p>When using our ingest tool with Avid Interplay, here is a link ");
		msg.append("to an <a href=\"http://support.autoingest.com/stream.asp?Other/SAT_AI_Direct_Installation_and_Configuration.pdf\">Avid setup guide for AutoIngest Direct</a>. ");
		msg.append("If you are not using Interplay, AutoIngest can be used directly with the Avid ");
		msg.append("Editor by using a drag and drop to the bin, it is also simpler to setup.</p>");
		
		msg.append("<p>Also find the latest <a href=\"http://support.autoingest.com/stream.asp?Other/SAT_AI_Reference_Guide-beta\">AutoIngest manual</a>.</p>");
		
		msg.append("--<br/>Best Regards,<br/>Jim McKain<br/>");
		msg.append("Office  978 686 1700 x205<br/>Mobile 978 815 3960<br/>");
		msg.append("<a href=\"http://www.autoingest.com\">http://www.autoingest.com</a><br/>");
		msg.append("<a href=\"http://www.nltek.com\">http://www.nltek.com</a><br/><br/>");
		
		return msg.toString();
	}

}
