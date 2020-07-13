package com.mts.action.email;

// JDK 1.8.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.mts.common.MTSConstants;
// MTS imports
import com.mts.publication.action.MTSDocumentAction;
import com.mts.publication.data.MTSDocumentVO;
import com.mts.publication.data.PublicationTeaserVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBModuleVO;

// WC Imports
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.util.CacheAdministrator;

/****************************************************************************
 * <b>Title</b>: IssueEmailWidget.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> This widget injects the latest issue articles and overview
 * into an MTS email showing/describing the contents of the latest issue
 * <b>Copyright:</b> Copyright (c) 2020
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Mar 11, 2020
 * @updates:
 ****************************************************************************/
public class IssueEmailWidget extends SimpleActionAdapter {
	/**
	 * Cache key
	 */
	public static final String WC_CACHE_KEY = "MTS_CACHE_DOCUMENTS";
	
	/**
	 * 
	 */
	public IssueEmailWidget() {
		super();
	}

	/**
	 * @param arg0
	 */
	public IssueEmailWidget(ActionInitVO arg0) {
		super(arg0);
	}
	
	/*
	 * (non-javadoc)
	 * @see com.smt.sitebuilder.action.SimpleActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.list(req);
		
		// Add the attribute 1 and 2 text fields to the admin with the following fields
		req.setAttribute(SBModuleVO.ATTRIBUTE_1, "Articles for the Past # Days");
		req.setAttribute(SBModuleVO.ATTRIBUTE_2, "Publication ID<br/>MEDTECH-STRATEGIST <br/>MARKET-PATHWAYS)");
	}
	
	/*
	 * (non-javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		// Get the id for the publication and days from today
		GenericVO vo = this.retrieveWidgetData(req, actionInit.getActionId());
		List<MTSDocumentVO> docs = getDocuments(vo);
		for (MTSDocumentVO doc : docs) {
			doc.setRedirectUrl(req.getParameter("redirectUrl"));
		}
		
		// Send the data to the view
		GenericVO actionData = new GenericVO(MTSConstants.getEmailColor(vo.getValue()), docs);
		setModuleData(actionData, docs.size());
	}
	
	/**
	 * Grab the data from the action and populate into a vo
	 * @param req
	 * @param actionId
	 * @return
	 */
	public GenericVO retrieveWidgetData(ActionRequest req, String actionId) {
		String sql = "select attrib1_txt, attrib2_txt from sb_action where action_id = ? and pending_sync_flg = 0";
		GenericVO vo = new GenericVO();
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setString(1, actionId);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				vo = new GenericVO(Convert.formatInteger(rs.getString(1), -7), StringUtil.checkVal(rs.getString(2)).trim());
				if (StringUtil.isEmpty(vo.getValue() + "")) {
					String id = StringUtil.checkVal(req.getParameter("strategistPublicationId"), req.getParameter("pathwaysPublicationId"));
					vo.setValue(id.trim());
				}
			}
		} catch (SQLException e) {
			log.error("Unable to retrieve action data", e);
		}
		
		return vo;
	}
	
	/**
	 * Gets the document from cache or the db.  If from db, adds to cache
	 * @param id
	 * @return
	 */
	public List<MTSDocumentVO> getDocuments(GenericVO kv) {
		// Check from cache first
		Object cacheItem = new CacheAdministrator(attributes).readObjectFromCache(WC_CACHE_KEY);
		if (cacheItem != null) return ((PublicationTeaserVO)cacheItem).getDocuments(); 
		
		// Get the articles within the last 7 days
		MTSDocumentAction mda = new MTSDocumentAction(getDBConnection(), getAttributes());
		PublicationTeaserVO ptvo = mda.getLatestArticles((String)kv.getValue(), Convert.formatDate(new Date(), Calendar.DAY_OF_YEAR, (int)kv.getKey()));
		
		return ptvo.getDocuments();
	}
}
