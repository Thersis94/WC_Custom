/**
 * 
 */
package com.depuysynthes.pa;

import com.depuysynthes.pa.PatientAmbassadorStoriesTool.PAFConst;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.data.DataContainer;
import com.smt.sitebuilder.data.vo.FormTransactionVO;
import com.smt.sitebuilder.util.MessageSender;

/****************************************************************************
 * <b>Title</b>: PatientAmbassadorFormCallback.java
 * <p/>
 * <b>Project</b>: WC_Custom
 * <p/>
 * <b>Description: </b> Callback function to handle Emailing the Consent Form to
 * the submitter if they checked the Modal Box.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author raptor
 * @version 1.0
 * @since Mar 9, 2015
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public class PatientAmbassadorFormCallback extends SBActionAdapter {

	/**
	 * 
	 */
	public PatientAmbassadorFormCallback() {
	}

	/**
	 * @param actionInit
	 */
	public PatientAmbassadorFormCallback(ActionInitVO actionInit) {
		super(actionInit);
	}

	public void build(SMTServletRequest req) {
		DataContainer dc = (DataContainer)req.getAttribute("formDataVO");
		
		FormTransactionVO trans = dc.getTransactions().values().iterator().next();
		
		if(trans.getFieldById(PAFConst.EMAIL_CONSENT_ID.getId()).getResponses().get(0).equals("Yes")) {
			SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);

			StringBuilder sb = new StringBuilder();
			sb.append("<p>Thank you for submitting your story to the Patient Ambassador ");
			sb.append("program.  As requested, the consent statement has been attached as ");
			sb.append("a downloadable link <a href='" + site.getFullSiteAlias());
			sb.append("/binary/org/DPY_SYN/ambassador/DePuySynthesConsentAndRelease2014.pdf'>Here</a>.");

			//allow actions to overwrite the source email address
			String senderEmail = site.getMainEmail();
			if (req.getAttribute("senderEmail") != null) {
				senderEmail = (String) req.getAttribute("senderEmail");
			}
			String emailAddress = trans.getFieldById(PAFConst.EMAIL_ADDRESS_TXT.getId()).getResponses().get(0);
			try {
				EmailMessageVO mail = new EmailMessageVO();
				mail.addRecipients(emailAddress);
				mail.setSubject("Patient Ambassador Consent Document");
				mail.setFrom(senderEmail);
				mail.setHtmlBody(sb.toString());

				MessageSender ms = new MessageSender(attributes, dbConn);
				ms.sendMessage(mail);
			} catch (InvalidDataException ide) {
				log.error("could not send contact email", ide);
			}
		}
	}
}
