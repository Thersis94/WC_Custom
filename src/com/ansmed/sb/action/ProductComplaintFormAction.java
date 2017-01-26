package com.ansmed.sb.action;

// JDK 1.6
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//SMT Base Libs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.SMTMail;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.databean.FilePartDataBean;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.form.FormFacadeAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.data.DataContainer;
import com.smt.sitebuilder.data.DataManagerFacade;
import com.smt.sitebuilder.data.TransactionParserIntfc;
import com.smt.sitebuilder.data.vo.FormFieldVO;
import com.smt.sitebuilder.data.vo.FormPageVO;
import com.smt.sitebuilder.data.vo.FormTransactionVO;
import com.smt.sitebuilder.data.vo.FormVO;
import com.smt.sitebuilder.data.vo.SMTServletRequestQueryVO;

/****************************************************************************
 * <b>Title</b>: ProductComplaintAction.java<p/>
 * <b>Description: Wraps FormFacadeAction using a specific SJM multi-page complaint form.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Apr 12, 2012
 * Change Log:
 * Aug 31, 2012; David Bargerhuff: implemented email response send for submitter of form.
 ****************************************************************************/
public class ProductComplaintFormAction extends SBActionAdapter {
	
	public static final String COMPLAINT_FORM_PAGE_MAP = "complaintFormPageMap";
	private boolean lastPage = false; // tells JSTL that this is the last page to be processed
	/**
	 * 
	 */
	public ProductComplaintFormAction() {
		super();		
	}

	/**
	 * @param actionInit
	 */
	public ProductComplaintFormAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("Starting ProductComplaintWrapperAction retrieve...");
	   	String oldInitId = actionInit.getActionId();
    	ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
    	actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		
		// load form
		ActionInterface ffa = new FormFacadeAction(this.actionInit);
		ffa.setAttributes(attributes);
		ffa.setDBConnection(dbConn);
		ffa.retrieve(req);
    	
		//reset the actionId
		actionInit.setActionId(oldInitId);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.debug("Starting ProductComplaintWrapperAction build...");
	   	String oldInitId = actionInit.getActionId();
    	ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
    	actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		
		// build page map or check session for page map
		this.checkPageMap(req);
		
		// override profile creation
		req.setParameter(TransactionParserIntfc.PROCESS_PROFILE, "false");
		
		// save form data for the given page ('currentPageNo' param on request)
		ActionInterface ffa = new FormFacadeAction(this.actionInit);
		ffa.setAttributes(attributes);
		ffa.setDBConnection(dbConn);
		ffa.build(req);
		
		// determine if there is a next page or if we have finished
		int nextPage = this.findNextPage(req);
		boolean finalPageSubmitted = false;
		if (nextPage == 0) {

			// this is last page, process notification email
			this.processNotification(req);
			// clean up loose ends
			finalPageSubmitted = true;
			req.getSession().removeAttribute(COMPLAINT_FORM_PAGE_MAP);
		}
		
		this.processRedirect(req, nextPage, finalPageSubmitted);
		
		//reset the actionId
		actionInit.setActionId(oldInitId);
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
    public void list(ActionRequest req) throws ActionException {
    	super.retrieve(req);    	
    }
	
	/**
	 * Builds a map of pages that will be displayed for this form based on the 
	 * fields selected on the first page of the form.  The map is then placed on the session.
	 * @param req
	 */
	private void checkPageMap(ActionRequest req) {
		log.debug("checking page map...");
		// if first page, build page map from request params.
		if (Convert.formatInteger(req.getParameter("currentPageNo")) == 1) {
			log.debug("putting pageList on session...");
			List<Integer> pageList = new ArrayList<Integer>();
			// evaluate req params
			if (StringUtil.checkVal(req.getParameter("compPage2")).length() > 0) {
				pageList.add(2);
			}
			if (StringUtil.checkVal(req.getParameter("compPage3")).length() > 0) {
				pageList.add(3);
			}
			if (StringUtil.checkVal(req.getParameter("compPage4")).length() > 0) {
				pageList.add(4);
			}
			if (StringUtil.checkVal(req.getParameter("compPage5")).length() > 0) {
				pageList.add(5);
			}
			req.getSession().setAttribute(COMPLAINT_FORM_PAGE_MAP, pageList);
		}
	}
	
	/**
	 * Determines the next page of the form that should be displayed.  A 'next page' number
	 * of zero (0) means there are no more pages to display/process, so we are done.
	 * @param req
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private int findNextPage(ActionRequest req) {
		log.debug("determining next form page to display...");
		int currentPageNo = Convert.formatInteger(req.getParameter("currentPageNo"));
		log.debug("currentPageNo: " + currentPageNo);
		List<Integer> pageList = null;
		if (req.getSession().getAttribute(COMPLAINT_FORM_PAGE_MAP) != null) {
			pageList = (ArrayList<Integer>) req.getSession().getAttribute(COMPLAINT_FORM_PAGE_MAP);
		}
		int nextPageNo = 0;
		if (pageList != null && ! pageList.isEmpty()) {
			int listSize = pageList.size();
			int maxIndex = listSize - 1;
			log.debug("page listSize/maxIndex: " + listSize + "/" + maxIndex);
			if (currentPageNo == 1) {
				// we just processed page 1, so return first page in the page list
				nextPageNo = pageList.get(0);
				if (maxIndex == 0) setLastPage(true);  // first page in list is also the last page, set flag
			} else if (currentPageNo > 1) {
				// we just processed a secondary page, get the current page's index 
				int currIndex = pageList.indexOf(currentPageNo);
				// check for more pages to process
				if (currIndex < maxIndex) {
					// there is at least one more page to process, return next page number
					int nextIndex = currIndex + 1;
					nextPageNo = pageList.get(nextIndex);
					// check to see if next page is the last page
					if (nextIndex == maxIndex) {
						// next page is the last page, set last page flag
						setLastPage(true);
					}
				}
			}
		}
		log.debug("returning...nextPageNo/lastPage are: " + nextPageNo + "/" + isLastPage());
		return nextPageNo;
	}
	
	/**
	 * Sends notification email to the site admin and a response email to the submitter.
	 * @param req
	 */
	@SuppressWarnings("unchecked")
	private void processNotification(ActionRequest req) {
		log.debug("processing notification...");
	   	DataContainer formData = this.retrieveSubmittedForm(req);
    	if (formData.hasErrors()) {
			log.error("Error retrieving submitted form data for email: ");
    		for (String err : formData.getErrors().keySet()) {
    			log.error(formData.getErrors().get(err).getStackTrace());
    		}
    	} else {
    		//get page map of pages submitted
    		List<Integer> pageList = null;
    		if (req.getSession().getAttribute(COMPLAINT_FORM_PAGE_MAP) != null) {
    			pageList = (ArrayList<Integer>) req.getSession().getAttribute(COMPLAINT_FORM_PAGE_MAP);
    		}
        	SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
        	StringBuilder msgBody = this.buildMessageBody(req, site, formData, pageList);
    		this.sendAdminEmail(req, site, msgBody);
    		this.sendResponseEmail(req, site, formData, msgBody);
    	}
	}
	
	/**
	 * Sends form response data to form contact email address.
	 * @param req
	 * @param site
	 * @param formData
	 * @param pageList
	 */
    private void sendAdminEmail(ActionRequest req, SiteVO site, StringBuilder msg) {
    	log.debug("sending admin email...");

		//allow actions to overwrite the source email address
		String senderEmail = site.getMainEmail();
		if (req.getAttribute("senderEmail") != null) {
			senderEmail = (String) req.getAttribute("senderEmail");
		}
		StringEncoder se = new StringEncoder();
		String subject = req.getParameter("formEmailSubject");
		if (subject == null || subject.length() == 0) {
			subject = se.decodeValue(req.getParameter("actionName")) + " Form Response on " + site.getSiteAlias();
		}	
		this.sendEmail(req, senderEmail, req.getParameterValues("formEmailAddress"), subject, msg);
	}
    
    /**
     * Formats and sends response email to submitter of complaint form.
     * @param req
     * @param site
     * @param formData
     */
    private void sendResponseEmail(ActionRequest req, SiteVO site, DataContainer formData, StringBuilder msg) {
    	String subject = "SalesNet Complaint Form Submission";
    	FormVO form = formData.getForm();
    	StringBuilder sb = new StringBuilder(form.getResponseText());
    	sb.append("<br/>").append(msg);
		String senderEmail = site.getMainEmail();
		//allow actions to overwrite the source email address
		if (req.getAttribute("senderEmail") != null) {
			senderEmail = (String) req.getAttribute("senderEmail");
		}
    	this.sendEmail(req, senderEmail, req.getParameterValues("emailAddressText"), subject, sb);
    }
    
    /**
     * Sends email to recipient.
     * @param req
     * @param senderEmail
     * @param recipient
     * @param subject
     * @param msg
     */
    private void sendEmail(ActionRequest req, String senderEmail, String[] recipients, String subject, StringBuilder msg) {    	
		SMTMail mail = new SMTMail((String)getAttribute(Constants.CFG_SMTP_SERVER));
		mail.setUser((String)getAttribute(Constants.CFG_SMTP_USER));
		mail.setPassword((String)getAttribute(Constants.CFG_SMTP_PASSWORD));
		mail.setRecpt(recipients);
		mail.setSubject(subject);
		mail.setFrom(senderEmail);
		mail.setHtmlBody(msg.toString());
		
		// Add any attachments that were submitted
		List<FilePartDataBean> files = req.getFiles();
		for (int i=0; i < files.size(); i++) {
			FilePartDataBean file = files.get(i);
			mail.addAttachment(file.getFileName(), file.getFileData());
		}
		
		if (log.isDebugEnabled()) {
			log.debug("recipients: " + StringUtil.getDelimitedList(mail.getStringRecpt(), false, ","));
			log.debug("Mail Info: " + mail.toString());
		}
		
		try {
			mail.postMail();
		} catch (MailException me) {
			log.error("Error: Unable to send form response email, ", me);
		}
    }
    
    /**
     * Builds the email message body
     * @param req
     * @param site
     * @param formData
     * @param pageList
     * @return
     */
    private StringBuilder buildMessageBody(ActionRequest req, SiteVO site, DataContainer formData, List<Integer> pageList) {
		// unpack the data container
		List<FormPageVO> pages = null;
		FormTransactionVO submittal = null;
		Map<String, FormFieldVO> responses = null;
		// get form submittal Id from request
		String fsi = StringUtil.checkVal(req.getParameter("fsi"));
		log.debug("form submittal ID: " + fsi);
		if (formData != null) {
			if (formData.getForm() != null) {
				// get form page data
				if (formData.getForm().getPages() != null) {
					pages = formData.getForm().getPagesList();
				}
				// get form transaction and response data
				if (! formData.getTransactions().isEmpty()) {
					submittal = formData.getTransactions().get(fsi);
					if (submittal != null) {
						responses = submittal.getCustomData();
					}
				}
			}
		}
		StringBuilder msg = null;
		// if we don't have data, don't send an email.
		if (pages == null || pages.isEmpty() || responses == null || submittal == null) return msg;
		msg = new StringBuilder();
		msg.append("<p><font color=\"blue\"><b>SalesNet: Product Complaint Form Submission</b></font></p>");
		msg.append("<table style=\"width:850px;border:solid 1px black;\">");
		msg.append("<tr><th colspan='2'>").append(req.getParameter("actionName")).append("</th></tr>");
		msg.append("<tr style=\"background:#E1EAFE;\"><td style=\"padding-right:10px;\">Website");
		msg.append("</td><td>").append(site.getSiteName()).append("</td></tr>");
		
		// loop pages, print field data for each page
		int x = 0;
		for (FormPageVO page : pages) {
			// if this isn't first page, and page number isn't in pageList, skip this page.
			if (page.getPageNo() > 1 && pageList.indexOf(page.getPageNo()) == -1) {
				log.debug("skipping page: " + page.getPageNo());
				continue;
			}
			
			// append the page header:
			msg.append("<tr><td colspan=\"2\">&nbsp;</td></tr>");
			msg.append("<tr><td colspan=\"2\" style=\"text-align: center; font-weight: bold;\">");
			msg.append(this.retrievePageHeader(page.getPageNo())).append("</td></tr>");
			
			// append the field name/response data for this page
			List<FormFieldVO> fields = page.getPageFields();
			if (fields == null || fields.isEmpty()) continue;
			for (FormFieldVO field : fields) {
				String color = ((x++ % 2) == 0) ? "#C0D2EC" : "#E1EAFE";
				String questionNm = StringUtil.replace(field.getFieldName(),"#hide#","");
				String[] vals = null;
				if (responses.get(field.getFormFieldId()) != null) {
					List<String> valsList = responses.get(field.getFormFieldId()).getResponses();
					if (valsList != null) vals = valsList.toArray(new String[0]); 
				}
				String value = StringUtil.getToString(vals, false, false, ",");
				msg.append("<tr style=\"background:").append(color);
				msg.append(";\"><td style=\"width: 550px; padding-right:10px;\" nowrap valign=\"top\">").append(questionNm);
				msg.append("</td><td>").append(value).append("&nbsp;</td></tr>");
			}
		}
		msg.append("</table>");
		msg.append("<br>");
		return msg;
    }
	
    /**
     * Retrieves the submitted form data
     * @param req
     * @return
     */
    private DataContainer retrieveSubmittedForm(ActionRequest req) {
       	log.debug("retrieving submitted form data...");
    	String formId = req.getParameter("formId");
    	String formSubmittalId = req.getParameter("fsi");
		req.setParameter("formSubmittalId", formSubmittalId, true);

		// first retrieve the form
		DataManagerFacade dfm = new DataManagerFacade(attributes,dbConn);
		DataContainer formData = new DataContainer();
		formData = dfm.loadForm(formId);
		
		// now retrieve the specific transaction and responses
		SMTServletRequestQueryVO sqv = new SMTServletRequestQueryVO(req);
		sqv.setFormId(formId);
		formData.setQuery(sqv);
		formData = dfm.loadTransactions(formData);
		return formData;
    }
		
	/**
	 * Builds redirect url
	 * @param req
	 * @param nextPage
	 * @param finalPageSubmitted
	 */
	private void processRedirect(ActionRequest req, int nextPage, boolean finalPageSubmitted) {
		log.debug("lastPage: " + isLastPage());
		// redirect
		StringBuffer url = new StringBuffer();
	   	url.append(req.getRequestURI());
	   	// append form submittal Id value first.
	   	url.append("?fsi=").append(req.getParameter("fsi"));
		if (nextPage > 0) 	url.append("&nextPage=").append(nextPage);
		if (this.isLastPage()) url.append("&lastPage=true");
		if (finalPageSubmitted) url.append("&formSubmitted=true");
    	req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
    	req.setAttribute(Constants.REDIRECT_URL, url.toString());
		log.debug("ProductComplaintWrapperAction redirect URL: " + url.toString());
	}
	
	private String retrievePageHeader(int pageNo) {
		String header = "";
		switch(pageNo) {
		case 1:
			header = "Employee/Customer/Product Information";
			break;
		case 2:
			header = "Additional Surgical Information";
			break;
		case 3:
			header = "Infection Information";
			break;
		case 4:
			header = "Lead Migration/Fracture Information";
			break;
		case 5:
			header = "Eon Mini IPG Charging/Communication Information";
			break;
		}
		return header;
	}

	protected boolean isLastPage() {
		return lastPage;
	}
	
	protected void setLastPage(boolean lastPage) {
		this.lastPage = lastPage;
	}

}
