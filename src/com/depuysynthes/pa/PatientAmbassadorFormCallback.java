package com.depuysynthes.pa;

import com.depuysynthes.pa.PatientAmbassadorStoriesTool.PAFConst;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.form.FormAction;
import com.smt.sitebuilder.action.form.FormActionVO;
import com.smt.sitebuilder.action.form.FormSubmittalAction;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.data.DataContainer;
import com.smt.sitebuilder.data.TransactionParserIntfc;
import com.smt.sitebuilder.data.vo.FormTransactionVO;
import com.smt.sitebuilder.util.MessageSender;

/****************************************************************************
 * <b>Title</b>: PatientAmbassadorFormCallback.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Callback function to handle Emailing the Consent Form to
 * the submitter if they checked the Modal Box.
 * <b>Copyright:</b> Copyright (c) 2015
 * <b>Company:</b> Silicon Mountain Technologies
 * @author raptor
 * @version 1.0
 * @since Mar 9, 2015
 *        <b>Changes: </b>
 ****************************************************************************/
public class PatientAmbassadorFormCallback extends SBActionAdapter {

	public PatientAmbassadorFormCallback() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public PatientAmbassadorFormCallback(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) {
		DataContainer dc = (DataContainer)req.getAttribute(FormAction.FORM_DATA);

		FormTransactionVO trans = dc.getTransactions().values().iterator().next();

		FormActionVO form = (FormActionVO) req.getAttribute(FormSubmittalAction.FORM_ACTION_VO);

		if("Yes".equals(trans.getFieldById(PAFConst.EMAIL_CONSENT_ID.getId()).getResponses().get(0))) {
			SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);

			//allow actions to overwrite the source email address
			String senderEmail = site.getMainEmail();
			if (req.getAttribute("senderEmail") != null) {
				senderEmail = (String) req.getAttribute("senderEmail");
			}

			try {
				EmailMessageVO mail = new EmailMessageVO();
				mail.addRecipients(trans.getEmailAddress());
				mail.setSubject("Patient Ambassador Consent Document");
				mail.setFrom(senderEmail);
				mail.setHtmlBody(form.getOrgConsentText());

				MessageSender ms = new MessageSender(attributes, dbConn);
				ms.sendMessage(mail);
			} catch (InvalidDataException ide) {
				log.error("could not send contact email", ide);
			}
		}

		// Format the redirect to use with this request
		formatRedirect(req);

	}

	/**
	 * Check the response value for 'has replaced joint' and builds a custom redirect depending 
	 * upon the response.  We format the redirect here instead of relying on FormFacadeAction's 
	 * more generic redirect builder so that we can to pass a custom parameter back to the JSTL.
	 * We use this custom parameter to tailor the form submission response when the requestor 
	 * is from a visitor who does not have a DePuy Synthes implant.
	 * @param req
	 */
	private void formatRedirect(ActionRequest req) {
		String fieldVal = TransactionParserIntfc.FORM_FIELD_PREFIX + PatientAmbassadorStoriesTool.PAFConst.HAS_REPLACED_ID.getId();
		String hasReplacedJoint = req.getParameter(fieldVal);

		if ("yes".equalsIgnoreCase(hasReplacedJoint)) {
			return;
		}

		StringBuilder redir = new StringBuilder(300);
		SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
		redir.append(site.getFullSiteAlias());

		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		redir.append(page.getRequestURI()).append("?page=");
		redir.append(req.getParameter("page"));
		redir.append("&fsi=").append(req.getParameter("fsi"));
		redir.append("&submitNotDSJoint=").append("true");

		req.setParameter(Constants.REDIRECT_URL, redir.toString(), true);
	}
}
