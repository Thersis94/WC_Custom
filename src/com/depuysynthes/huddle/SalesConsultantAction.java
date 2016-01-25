package com.depuysynthes.huddle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.siliconmtn.util.parser.AnnotationExcelParser;
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

		//change sort order to DESC if the results are sorted that way.  Otherwise the default is correct
		Cookie sort = req.getCookie(HuddleUtils.SORT_COOKIE);
		if (sort != null && sort.getValue() != null && "titleZA".equals(sort.getValue().toString()))
			req.setParameter("sortDirection", ORDER.desc.toString());

		Cookie rppCook = req.getCookie(HuddleUtils.RPP_COOKIE);
		if (rppCook != null)
			req.setParameter("rpp", rppCook.getValue());
		
		//if we have specialty add it as a filter - this allows the section homepages to target pre-filtered lists
		if (req.hasParameter("specialty")) {
			Set<String> fq = new HashSet<>();
			if (req.hasParameter("fq"))
				fq.addAll(Arrays.asList(req.getParameterValues("fq")));
			
			fq.add(HuddleUtils.SOLR_OPCO_FIELD + ":" + req.getParameter("specialty"));
			req.setParameter("fq", fq.toArray(new String[fq.size()]), true);
		}

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
		Collection<Object> alignData = parseAlignmentFile(req.getFile("xlsFileAlignment"));
		Map<String, SalesConsultantRepVO> repData = parseRepFile(req.getFile("xlsFile"));
		
		//don't make any changes if no data (or bad data) was provided.
		if (repData == null || repData.size() == 0) return;
		if (alignData == null || alignData.size() == 0) return;
		log.debug("align rows=" + alignData.size());
		log.debug("repData rows=" + repData.size());
		
		indexRecords(repData, alignData, req.getParameter("organizationId"));
	}
	
	
	/**
	 * parse the alignment file.  We'll get most of our data from here
	 * @param fpdb
	 * @return
	 * @throws ActionException
	 */
	private Collection<Object> parseAlignmentFile(FilePartDataBean fpdb) throws ActionException {
		AnnotationParser parser;
		try {
			parser = new AnnotationParser(SalesConsultantAlignVO.class, fpdb.getExtension());
		} catch(InvalidDataException e) {
			throw new ActionException("could not load import file", e);
		}

		try {
			//Gets the xls file from the request object, and passes it to the parser.
			//Parser then returns the list of populated beans
			Map< Class<?>, Collection<Object>> beans = parser.parseFile(fpdb, true);
			return (Collection<Object>) beans.get(SalesConsultantAlignVO.class);
			

		} catch (Exception e) {
			log.error("could not process Sales Consultant Alignment import", e);
		}
		return null;
	}
	
	
	/**
	 * parse the rep data file for Huddle - we'll get titles from here
	 * @param fpdb
	 * @return
	 * @throws ActionException
	 */
	private Map<String, SalesConsultantRepVO> parseRepFile(FilePartDataBean fpdb) throws ActionException {
		AnnotationExcelParser parser;
		try {
			List<Class<?>> lst = new ArrayList<>();
			lst.add(SalesConsultantRepVO.class);
			parser = new AnnotationExcelParser(lst);
			parser.setSheetNo(2); //ACTIVE_SALES_REPS sheet.
		} catch(Exception e) {
			throw new ActionException("could not load import file", e);
		}

		//load the main file
		try {
			//Gets the xls file from the request object, and passes it to the parser.
			//Parser then returns the list of populated beans
			Map<Class<?>, Collection<Object>> beans = parser.parseData(fpdb.getFileData(), true);
			Collection<Object> rawData =  (Collection<Object>) beans.get(SalesConsultantRepVO.class);
			Map<String, SalesConsultantRepVO> data = new HashMap<>();
			for (Object obj : rawData) {
				SalesConsultantRepVO vo = (SalesConsultantRepVO) obj;
				data.put(vo.getWWID(), vo);
			}
			
			return data;

		} catch (Exception e) {
			log.error("could not process Sales Rep import", e);
		}
		return null;
	}
	
	
	
	/**
	 * takes the list of beans we built from Excel and pushes them to Solr
	 * @param data
	 * @param orgId
	 * @throws ActionException
	 */
	private void indexRecords(Map<String, SalesConsultantRepVO> repData, 
			Collection<Object> data, String orgId) throws ActionException  {
		
		Map<String, String> states = invertStates(new USStateList()); 
		Map<String, SalesConsultantAlignVO> finalData = new HashMap<>(data.size());
		SalesConsultantAlignVO vo;
		SolrActionUtil util = new SolrActionUtil(getAttributes());
		util.setHardCommit(false); //let the insert handle the commit of the delete; so we only fire one commit to Solr.
		
		//delete all existing Solr records, since we don't have a means of managing deltas
		util.removeByQuery(SearchDocumentHandler.INDEX_TYPE, HuddleUtils.IndexType.HUDDLE_CONSULTANTS.toString());
		
		//insert all records loaded from the file
		util.setHardCommit(true);
		
		for (Object obj : data) {
			SalesConsultantAlignVO newVo = (SalesConsultantAlignVO) obj;
			if (Convert.formatInteger(newVo.getREP_ID()) == 0) continue;
			
			String documentId = "dpy-rep-" + newVo.getREP_ID();
			newVo.setState(states.get(newVo.getState())); //replace the stateCd with stateNm
			newVo.setCity(StringUtil.capitalizePhrase(newVo.getCity(), 3));
			newVo.setCNSMR_NM(StringUtil.capitalizePhrase(newVo.getCNSMR_NM(), 3));
			vo = finalData.get(documentId);
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
				vo.setCity(null); //flush these, because they don't apply to these records in the context implied (we use them in hierarchy)
				vo.setState(null);
				if (repData.containsKey(vo.getREP_ID())) //REP_ID = WWID
					vo.setRepTitle(repData.get(vo.getREP_ID()).getTitle());
			}
			finalData.put(documentId, vo);
		}
		
		util.addDocuments(finalData.values());
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