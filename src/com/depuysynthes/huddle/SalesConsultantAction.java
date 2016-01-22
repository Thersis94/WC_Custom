package com.depuysynthes.huddle;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;

import org.apache.solr.client.solrj.SolrQuery.ORDER;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.common.html.StateList;
import com.siliconmtn.common.html.state.USStateList;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
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
		// Save the current filter queries for later
		String[] fq = req.getParameterValues("fq");

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
		
		// Revert changes made to the request object
		req.setParameter("fq", fq, true);
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

			indexRecords(data, req.getParameter("organizationId"));

		} catch (Exception e) {
			log.error("could not process Sales Consultant import", e);
		}
	}
	
	
	
	/**
	 * takes the list of beans we built from Excel and pushes them to Solr
	 * @param data
	 * @param orgId
	 * @throws ActionException
	 */
	private void indexRecords(Collection<Object> data, String orgId) throws ActionException  {
		Map<String, String> states = invertStates(new USStateList()); 
		Map<String, SalesConsultantVO> repData = new HashMap<>(data.size());
		SalesConsultantVO vo;
		SolrActionUtil util = new SolrActionUtil(getAttributes());
		util.setHardCommit(false); //let the insert handle the commit of the delete; so we only fire one commit to Solr.
		
		//delete all existing Solr records, since we don't have a means of managing deltas
		util.removeByQuery(SearchDocumentHandler.INDEX_TYPE, HuddleUtils.SOLR_SALES_CONSULTANT_IDX_TYPE);
		
		//insert all records loaded from the file
		util.setHardCommit(true);
		
		for (Object obj : data) {
			SalesConsultantVO newVo = (SalesConsultantVO) obj;
			if (Convert.formatInteger(newVo.getREP_ID()) == 0) continue;
			
			String documentId = "dpy-rep-" + newVo.getREP_ID();
			newVo.setState(states.get(newVo.getState())); //replace the stateCd with stateNm
			newVo.setCity(StringUtil.capitalizePhrase(newVo.getCity(), 3));
			newVo.setCNSMR_NM(StringUtil.capitalizePhrase(newVo.getCNSMR_NM(), 3));
			vo = repData.get(documentId);
			if (vo != null) {
				//treat this as an additional hospital, add it to the existing VO
				String hier = newVo.getHierarchy();
				for (String existing : vo.getHierarchies()) {
					if (existing.equals(hier)) { //do not add if we already have it
						hier = null;
						break;
					}
				}
				if (hier != null) vo.addHierarchies(hier);
			} else {
				vo = newVo;
				vo.setDocumentId(documentId);
				vo.addOrganization(orgId);
				vo.addRole(SecurityController.PUBLIC_ROLE_LEVEL);
				vo.addHierarchies(newVo.getHierarchy()); //move the data from 3 separate fields to our hierarchy field
				vo.setCity(null); //flush these, because they don't apply to these records in the context implied
				vo.setState(null);
			}
			repData.put(documentId, vo);
		}
		
		util.addDocuments(repData.values());
	}
	
	
	/**
	 * inverts the Map of states maintained in the static bean
	 * @param states
	 * @return
	 */
	private Map<String, String> invertStates(StateList list) {
		Map<String, String> data = new HashMap<>(60);
		for(Map.Entry<Object, Object> entry : list.getStateList().entrySet())
			data.put(entry.getValue().toString(), entry.getKey().toString());

		return data;
	}
}