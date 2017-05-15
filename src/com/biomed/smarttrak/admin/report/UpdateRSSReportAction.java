package com.biomed.smarttrak.admin.report;
//jdk 1.8
import java.util.ArrayList;
import java.util.List;

//wc custom libs
import com.biomed.smarttrak.vo.UpdateVO;
//smt base libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.rss.RSSCreatorReport;
import com.smt.sitebuilder.action.rss.RSSCreatorVO;
import com.smt.sitebuilder.action.tools.SearchVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * Title: UpdateRSSReportAction.java <p/>
 * Project: WC_Custom <p/>
 * Description: Action that generates an RSS Feed based on Updates related 
 * Twitter article text. The corresponding RSS Feed is made publicly available.<p/>
 * Copyright: Copyright (c) 2017<p/>
 * Company: Silicon Mountain Technologies<p/>
 * @author Devon Franklin
 * @version 1.0
 * @since May 11, 2017
 ****************************************************************************/

public class UpdateRSSReportAction extends SBActionAdapter {

	/**
	 * No arg-constructor
	 */
	public UpdateRSSReportAction(){
		super();
	}
	
	/**
	 * @param init
	 */
	public UpdateRSSReportAction(ActionInitVO init){
		super(init);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req)throws ActionException{
		//build query of updates with twitter text, then transpose updates into search vos
		List<SearchVO> searchItems = buildSearchItems(getTwitterUpdates());
		
		//populate the general RSS Feed Information
		RSSCreatorVO rss = setRSSData(searchItems, req);
		
		//generate the report
		RSSCreatorReport rpt = new RSSCreatorReport();
		rpt.setData(rss);
		
		//stream the rss feed back as the response
		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
		req.setAttribute(Constants.BINARY_DOCUMENT, rpt);
		
	}
	
	/**
	 * Returns the SQL of updates with twitter text with yesterday's date
	 * @return
	 */
	protected List<UpdateVO> getTwitterUpdates(){
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		
		StringBuilder sql = new StringBuilder(400);
		sql.append("select * from ").append(schema).append("biomedgps_update ");
		sql.append("where tweet_flg = 1 ");
		sql.append("and create_dt >= date_trunc('day', current_timestamp) - interval '1' day ");
		sql.append("and create_dt < date_trunc('day', current_timestamp) ");
		sql.append("order by create_dt ");
		log.debug(sql);
		
		//execute the query
		DBProcessor dbp = new DBProcessor(dbConn, schema);
		List<Object> data = dbp.executeSelect(sql.toString(), null, new UpdateVO());
		
		//downcast into appropriate type
		List<UpdateVO> updates = new ArrayList<>();
		for (Object object : data) {
			updates.add((UpdateVO) object);		
		}
		log.debug("Updates retrieved: " + updates.size());
		return updates;
	}
	
	/**
	 * Builds a list of SearchVOs from UpdateVOs
	 * @param updates
	 * @return
	 */
	private List<SearchVO> buildSearchItems(List<UpdateVO> updates){
		List<SearchVO> searchItems = new ArrayList<>();
		UUIDGenerator uuid = new UUIDGenerator();
		
		//add the relevant pieces to create the search vo
		for (UpdateVO update : updates) {
			SearchVO vo = new SearchVO();
			vo.setActionId(update.getUpdateId());
	       	vo.setTitle(update.getTitleTxt());
	        vo.setSummary(update.getTwitterTxt());
	        vo.setCreateDate(update.getCreateDt());
	        vo.setUpdateDate(update.getUpdateDt());
	        //ensure each document url is unique
	        vo.setDocumentUrl(update.getDocumentUrl() +"?"+ uuid.getUUID());        
	        searchItems.add(vo); //add the item
		}
		
		return searchItems;
	}
	
	/**
	 * Sets the Smarttrak Updates RSS feed information for creation
	 * @param searchItems
	 * @param req
	 * @return
	 */
	private RSSCreatorVO setRSSData(List<SearchVO> searchItems, ActionRequest req){
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		SiteVO site =  (SiteVO) req.getAttribute(Constants.SITE_DATA);
		
		//Build the unique pieces for feed
		String locale = site.getLanguageCode() +"-"+ site.getCountryCode();
		StringBuilder copyRight = new StringBuilder(150);
		copyRight.append("&#169; 2017 BioMed GPS — SmartTRAK All trademarks and copyrighted material ");
		copyRight.append("are the property of BioMedGPS LLC or the respective owners thereof.");
		String feedUrl = site.getFullSiteAlias() + page.getFullPath(); //this page is the feed
		
		//Set the general RSS Feed information
		RSSCreatorVO rss = new RSSCreatorVO();
		rss.setTitle("SmartTRAK Updates");
		rss.setLink(site.getFullSiteAlias());
		rss.setBaseUrl(site.getFullSiteAlias());
		rss.setLanguage(locale.toLowerCase());
		rss.setFeedUrl(feedUrl);
		rss.setTtl(60);
		rss.setRssDocs("http://www.w3.org/2005/Atom");
		rss.setDescription("Updates from SmartTRAK");
		rss.setCategory(new GenericVO("STUpdates", "SmartTRAK Updates"));
		rss.setCopyright(copyRight.toString());
		rss.setArticles(searchItems); //set the search items
		
		return rss;
	}
}
