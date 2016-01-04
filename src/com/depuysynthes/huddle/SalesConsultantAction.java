package com.depuysynthes.huddle;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.databean.FilePartDataBean;
import com.siliconmtn.util.parser.AnnotationParser;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: SalesConsultantAction.java<p/>
 * <b>Description: Wraps Solr to search for sales consultants.
 * Admin methods handle bulk file upload of consultant data.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Dec 30, 2015
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

		if (req.getFile("xlsFile") != null) {
			processUpload(req);
		
			//TODO Trigger the Solr Indexer to rebuild the Solr Index of Sales Consultants
		}
	}


	/**
	 * processes the file upload and imports each row as database record
	 * @param req
	 * @throws ActionException
	 */
	private void processUpload(SMTServletRequest req) throws ActionException {
		AnnotationParser parser;
		FilePartDataBean fpdb = req.getFile("xlsFile");
		/*
		try {
			parser = new AnnotationParser(CourseCalendarVO.class, fpdb.getExtension());
		} catch(InvalidDataException e) {
			throw new ActionException("could not load import file", e);
		}
		try {
			//Gets the xls file from the request object, and passes it to the parser.
			//Parser then returns the list of populated beans
			Map< Class<?>, Collection<Object>> beans = parser.parseFile(fpdb, true);

			ArrayList<Object> beanList = null;
			EventEntryAction eventAction = new EventEntryAction();
			eventAction.setDBConnection(dbConn);

			//Disable the db autocommit for the insert batch
			dbConn.setAutoCommit(false);

			beanList = new ArrayList<>(beans.get(CourseCalendarVO.class));
			for (Object o : beanList) {
				//set the eventTypeId for each
				CourseCalendarVO vo = (CourseCalendarVO) o;
				vo.setEventTypeId(req.getParameter("eventTypeId"));
				vo.setStatusFlg(EventFacadeAction.STATUS_APPROVED);
			}
			eventAction.importBeans(beanList, req.getParameter("attrib1Text"));

			//commit only after the entire import succeeds
			dbConn.commit();

		} catch (InvalidDataException | SQLException e) {
			log.error("could not process DSI calendar import", e);
		} finally {
			try {
				//restore autocommit state
				dbConn.setAutoCommit(true);
			} catch (SQLException e) {}
		}
		*/
	}
}