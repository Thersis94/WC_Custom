package com.depuysynthes.huddle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;

import org.apache.solr.client.solrj.SolrQuery.ORDER;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.siliconmtn.util.databean.FilePartDataBean;
import com.siliconmtn.util.parser.AnnotationParser;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.util.solr.SolrActionUtil;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

/****************************************************************************
 * <b>Title</b>: SalesConsultantAction.java<p/>
 * <b>Description: Wraps Solr to search for sales consultants.
 * Admin methods handle bulk file upload of consultant data and push to Solr.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 10, 2016
 ****************************************************************************/
public class SalesConsultantAction extends SimpleActionAdapter {

	public SalesConsultantAction() {
		super();
	}

	public SalesConsultantAction(ActionInitVO arg0) {
		super(arg0);
	}

	@Override
	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req); 
	}


	/**
	 * loads a list of Sales Consultant records from Solr.
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);

		//change sort order to DESC if the results are sorted that way.  Otherwise the default is correct
		Cookie sort = req.getCookie(HuddleUtils.SORT_COOKIE);
		if (sort != null && sort.getValue() != null && "titleZA".equals(sort.getValue().toString()))
			req.setParameter("sortDirection", ORDER.desc.toString());

		Cookie rppCook = req.getCookie(HuddleUtils.RPP_COOKIE);
		if (rppCook != null)
			req.setParameter("rpp", rppCook.getValue());
		
		//if we have specialty add it as a filter - this allows the section homepages to target pre-filtered lists
		if (req.hasParameter("specialty"))
			req.setParameter("fq", HuddleUtils.SOLR_OPCO_FIELD + ":" + req.getParameter("specialty"));

		req.setParameter("fmid",mod.getPageModuleId());
		//NOTE: page & start get picked up by SolrActionVO automatically, because we set "fmid"
		

		//call to solr for a list of sales consultants
		String solrActionId = StringUtil.checkVal(mod.getAttribute(SBModuleVO.ATTRIBUTE_1)); //the solrActionId we're wrapping
		actionInit.setActionId(solrActionId);
		SolrAction sa = new SolrAction(actionInit);
		sa.setAttributes(getAttributes());
		sa.setDBConnection(dbConn);
		sa.retrieve(req);
	}


	/**
	 * processes batch file upload using Annotations to DPY_SYN_HUDDLE_CONSULTANT
	 */
	@Override
	public void update(SMTServletRequest req) throws ActionException {
		super.update(req);

		if (req.getFile("xlsFile") != null)
			processUpload(req);
	}


	/**
	 * processes the file upload and imports each row as database record
	 * @param req
	 * @throws ActionException
	 */
	private void processUpload(SMTServletRequest req) throws ActionException {
		AnnotationParser parser;
		FilePartDataBean fpdb = req.getFile("xlsFile");
		try {
			parser = new AnnotationParser(SalesConsultantVO.class, fpdb.getExtension());
		} catch(InvalidDataException e) {
			throw new ActionException("could not load import file", e);
		}

		//push to Solr
		try {
			//Gets the xls file from the request object, and passes it to the parser.
			//Parser then returns the list of populated beans
			Map< Class<?>, Collection<Object>> beans = parser.parseFile(fpdb, true);
			Collection<Object> data = (Collection<Object>) beans.get(SalesConsultantVO.class);
			
			//don't make any changes if no data was provided.  This likely means something is wrong with the uploaded file
			if (data == null || data.size() == 0) return;

			SolrActionUtil util = new SolrActionUtil(getAttributes());
			util.setHardCommit(false); //let the insert handle the commit of the delete; so we only fire one commit to Solr.
			
			//delete all existing Solr records, since we don't have a means of managing deltas
			util.removeByQuery(SearchDocumentHandler.INDEX_TYPE, HuddleUtils.SOLR_SALES_CONSULTANT_IDEX_TYPE);
			
			//insert all records loaded from the file
			util.setHardCommit(true);
			List<SolrDocumentVO> repData = new ArrayList<>(data.size());
			UUIDGenerator uuid = new UUIDGenerator();
			for (Object obj : data) {
				SalesConsultantVO vo = (SalesConsultantVO) obj;
				vo.setDocumentId(uuid.getUUID());
				vo.addOrganization(req.getParameter("organizationId"));
				vo.addRole(SecurityController.PUBLIC_ROLE_LEVEL);
				repData.add(vo);
			}
			util.addDocuments(repData);

		} catch (Exception e) {
			log.error("could not process Sales Consultant import", e);
		}
	}
}