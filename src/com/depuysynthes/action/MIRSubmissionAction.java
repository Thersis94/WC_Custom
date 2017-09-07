package com.depuysynthes.action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrDocument;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
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
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.util.MessageSender;
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
		putModuleData(data);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		//turn the request into a VO using annotations/reflection
		MIRSubmissionVO vo = new MIRSubmissionVO(req);
		log.debug("submission: " + vo + "|files: " + req.getFiles().size());

		//turn the vo into an email
		EmailMessageVO msg = createEmail(vo, req);

		//send the email
		new MessageSender(getAttributes(), getDBConnection()).sendMessage(msg);
	}


	/**
	 * creates the email from the VO of data submitted.
	 * This involves creating some type of report
	 * @param vo
	 * @param req
	 * @return
	 */
	private EmailMessageVO createEmail(MIRSubmissionVO vo, ActionRequest req) {
		EmailMessageVO msg = new EmailMessageVO();

		//attach any uploaded files:
		if (req.hasFiles()) {
			for (FilePartDataBean file : req.getFiles())
				msg.addAttachment(file.getFileName(), file.getFileData());
		}

		//determine recipient
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		msg.setSubject("MIR Submission");
		msg.setInstance((String)getAttribute(AdminConstants.INSTANCE_NM));
		try {
			msg.addRecipient("");
			msg.setFrom(site.getMainEmail());
		} catch (InvalidDataException e) {
			log.error("could not set recipient emails", e);
		}

		//build the report and attach it to the email


		//return the email, ready to send
		return msg;
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SimpleActionAdapter#update(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void update(ActionRequest req) throws ActionException {
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
				if (vo.getName() == null) continue;

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
}