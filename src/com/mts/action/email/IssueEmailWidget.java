package com.mts.action.email;

// JDK 1.8.x
import java.util.Calendar;
import java.util.Date;
import java.util.List;

// MTS imports
import com.mts.publication.action.MTSDocumentAction;
import com.mts.publication.data.MTSDocumentVO;
import com.mts.publication.data.PublicationTeaserVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

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
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		// Get the id for the publication
		String id = req.getParameter("strategistPublicationId");
		if (StringUtil.isEmpty(id) || id.contains("#")) id = req.getParameter("pathwaysPublicationId");
		req.setParameter("filterPublicationId", id);
		
		// Send the data to the view
		setModuleData(getDocuments(id), 1);
	}
	
	/**
	 * Gets the document from cache or the db.  If from db, adds to cache
	 * @param id
	 * @return
	 */
	public List<MTSDocumentVO> getDocuments(String id) {
		// Check from cache first
		Object cacheItem = new CacheAdministrator(attributes).readObjectFromCache(WC_CACHE_KEY);
		if (cacheItem != null) return ((PublicationTeaserVO)cacheItem).getDocuments(); 
		
		// Get the articles within the last 7 days
		MTSDocumentAction mda = new MTSDocumentAction(getDBConnection(), getAttributes());
		PublicationTeaserVO ptvo = mda.getLatestArticles(id, Convert.formatDate(new Date(), Calendar.DAY_OF_YEAR, -7));
		
		// Add to cache for 3 days
		new CacheAdministrator(attributes).writeToCache(WC_CACHE_KEY, ptvo, 259200);
		
		return ptvo.getDocuments();
	}
}
