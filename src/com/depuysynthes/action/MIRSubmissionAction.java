package com.depuysynthes.action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrDocument;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.CaptchaException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.siliconmtn.util.databean.FilePartDataBean;
import com.siliconmtn.util.parser.AnnotationParser;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.action.search.SolrResponseVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.util.MessageSender;
import com.smt.sitebuilder.util.google.ReCaptchaUtil;
import com.smt.sitebuilder.util.solr.SolrActionUtil;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

/****************************************************************************
 * <b>Title</b>: MIRSubmissionAction.java<p/>
 * <b>Description: Wraps a Solr call around a file upload.  This action mimicks a contact-us widget - 
 * the end user completes a compex form, inclusive of some type-head Solr MLT calls for products to include.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Sep 07, 2017
 ****************************************************************************/
public class MIRSubmissionAction extends SimpleActionAdapter {

	protected static final String MIR_INDEX_TYPE = "DPYSYN_MIR_PRODUCT";


	public MIRSubmissionAction() {
		super();
	}

	public MIRSubmissionAction(ActionInitVO arg0) {
		super(arg0);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		//if MLT product lookup keywords are passed, call to Solr and return a JSON object.
		if (!req.hasParameter("searchData")) return;

		//call to solr for a list of products
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		String solrActionId = StringUtil.checkVal(mod.getAttribute(SBModuleVO.ATTRIBUTE_1)); //the solrActionId we're wrapping
		actionInit.setActionId(solrActionId);
		SolrAction sa = new SolrAction(actionInit);
		sa.setAttributes(getAttributes());
		sa.setDBConnection(dbConn);
		sa.retrieve(req);
		mod = (ModuleVO) sa.getAttribute(Constants.MODULE_DATA);

		//format the results
		formatResponse(mod);
	}


	/**
	 * formats the solr response into a JSON object we can return via the ajax call.
	 * @param mod
	 */
	private void formatResponse(ModuleVO mod) {
		SolrResponseVO solrResponse = (SolrResponseVO) mod.getActionData();
		List<MIRProductVO> data = new ArrayList<>();

		//if a response came back from solr, use it to populate the list
		if (solrResponse != null && solrResponse.getTotalResponses() > 0) {
			for (SolrDocument solrDoc : solrResponse.getResultDocuments())
				data.add(new MIRProductVO(solrDoc));
		}
		log.debug("data=" + data);
		Collections.sort(data);
		putModuleData(data);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		boolean captchaPassed = testCaptcha(req);

		if (captchaPassed) {
			//turn the request into a VO using annotations/reflection
			MIRSubmissionVO vo = new MIRSubmissionVO(req);
			log.debug("submission: " + vo + "|files: " + req.getFiles().size());

			//turn the vo into an email
			EmailMessageVO msg = new MIREmailMessageVO(vo, req, getAttributes());

			//send the email
			new MessageSender(getAttributes(), getDBConnection()).sendMessage(msg);
		}

		//redirect the user
		sendRedirect(captchaPassed, req);
	}


	/**
	 * varies the redirect depending on whether the user passed validation
	 * @param captchaPassed
	 * @param req
	 */
	private void sendRedirect(boolean captchaPassed, ActionRequest req) {
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		StringBuilder url = new StringBuilder(150);
		url.append(page.getRequestURI());
		if (captchaPassed) {
			url.append("?complete=1");
		} else {
			url.append("?captchaFailed=1&subregion=").append(req.getParameter("subregion"));
			url.append("&region=").append(req.getParameter("region"));
			url.append("&country=").append(req.getParameter("countryCode"));
		}
		sendRedirect(url.toString(), null, req);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SimpleActionAdapter#update(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void update(ActionRequest req) throws ActionException {
		//save the widget
		super.update(req);

		Object msg = attributes.get(AdminConstants.KEY_SUCCESS_MESSAGE);
		int saveCnt = 0;

		if (req.getFile("xlsFile") != null)
			saveCnt = processUpload(req);

		//return some stats to the administrator
		super.adminRedirect(req, msg, buildRedirect(saveCnt));
	}


	/**
	 * Append extra parameters to the redirect url so we can display some stats
	 * about the transaction performed
	 * @param req
	 * @return
	 */
	private String buildRedirect(int saveCnt) {
		StringBuilder redirect = new StringBuilder(150);
		redirect.append(getAttribute(AdminConstants.ADMIN_TOOL_PATH));
		redirect.append("?saveCnt=").append(saveCnt);
		return redirect.toString();
	}


	/**
	 * processes the file upload and imports each row as a new event to add to the 
	 * desired event calendar. 
	 * @param req
	 * @throws ActionException
	 */
	private int processUpload(ActionRequest req) throws ActionException {
		int cnt = 0;
		AnnotationParser parser;
		FilePartDataBean fpdb = req.getFile("xlsFile");
		try {
			parser = new AnnotationParser(MIRProductVO.class, fpdb.getExtension());
		} catch(InvalidDataException e) {
			throw new ActionException("could not load import file", e);
		}
		try {
			//Gets the xls file from the request object, and passes it to the parser.
			//Parser then returns the list of populated beans
			Map<Class<?>, Collection<Object>> beans = parser.parseFile(fpdb, true);
			Collection<Object> beanList = new ArrayList<>(beans.get(MIRProductVO.class));
			Collection<SolrDocumentVO> data = new ArrayList<>(beanList.size());

			UUIDGenerator uuid = new UUIDGenerator();
			for (Object o : beanList) {
				//set the eventTypeId for each
				MIRProductVO vo = (MIRProductVO) o;

				//weed out empty rows in the Excel file
				if (!vo.hasData()) continue;

				vo.setDocumentId(uuid.getUUID());
				vo.addRole(SecurityController.PUBLIC_ROLE_LEVEL);
				vo.addOrganization(req.getParameter("organizationId"));
				data.add(vo);
			}

			//push the new assets to Solr
			cnt = data.size();
			pushToSolr(data);

		} catch (InvalidDataException e) {
			log.error("could not process MIR Product import", e);
		}
		return cnt;
	}


	/**
	 * pushes the assembled List of VOs over to Solr.  Purges all existing records first.
	 * @param beanList
	 * @throws ActionException
	 */
	private void pushToSolr(Collection<SolrDocumentVO> beanList) throws ActionException {
		try (SolrActionUtil util = new SolrActionUtil(getAttributes())) {
			util.removeByQuery(SearchDocumentHandler.INDEX_TYPE, MIR_INDEX_TYPE);
			util.addDocuments(beanList);
			util.commitSolr(false, true);
		} catch (Exception e) {
			log.error("could not push MIR products to solr", e);
		}
	}


	/**
	 * tests if the passed captcha response is valid.
	 * @param req
	 * @param publicUser
	 * @throws CaptchaException
	 */
	protected boolean testCaptcha(ActionRequest req) {
		String resp = req.getParameter(Constants.RECAPTCHA_USER_RESPONSE);
		log.debug("testing captcha using response token value of: " + resp);
		return ReCaptchaUtil.validateReCaptcha(getAttributes(), resp);
	}
}