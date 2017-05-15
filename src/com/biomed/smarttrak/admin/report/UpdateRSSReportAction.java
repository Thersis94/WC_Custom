package com.biomed.smarttrak.admin.report;

import java.util.ArrayList;
import java.util.List;

import com.biomed.smarttrak.vo.UpdateVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.rss.RSSCreatorReport;
import com.smt.sitebuilder.action.rss.RSSCreatorVO;
import com.smt.sitebuilder.action.tools.SearchVO;
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
		//build query of updates with twitter text, since yesterday
		List<UpdateVO> updates = getTwitterUpdates();
		
		//transpose updates into search vos
		List<SearchVO> searchItems = buildSearchItems(updates);
		
		//populate the general RSS Feed Information
		RSSCreatorVO rss = setRSSData(searchItems);
		
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
		sql.append("select update_id, title_txt, twitter_txt, market_id, product_id, company_id ");
		sql.append("from ").append(schema).append("biomedgps_update ");
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
			log.debug("Update document url: " + ((UpdateVO)object).getDocumentUrl());
			updates.add((UpdateVO) object);		
		}
		return updates;
	}
	
	/**
	 * Builds a list of SearchVOs from UpdateVOs
	 * @param updates
	 * @return
	 */
	private List<SearchVO> buildSearchItems(List<UpdateVO> updates){
		List<SearchVO> searchItems = new ArrayList<>();
		
		//add the relevant pieces to create the search vo
		for (UpdateVO update : updates) {
			SearchVO vo = new SearchVO();
			vo.setActionId(update.getUpdateId());
	        vo.setDocumentUrl(update.getDocumentUrl()); 
	        vo.setTitle(update.getTitle());
	        vo.setSummary(update.getTwitterTxt());
		}
		
		return searchItems;
	}
	
	
	private RSSCreatorVO setRSSData(List<SearchVO> searchItems){
		//Retrieve the site data to help populate RSS Feed information
		//**This returns null 
		SiteVO site = (SiteVO) getAttribute(Constants.SITE_DATA); 
		

		RSSCreatorVO rss = new RSSCreatorVO();
		rss.setTitle("[Populate from site data]");
		rss.setLink("[Populate from site data]");
		rss.setBaseUrl("[Populate from site data]");
		rss.setLanguage("[Populate from site data]");
		rss.setTtl(60);
		rss.setDescription("Updates from smarttrak.com");
		rss.setCategory(new GenericVO("SmartTRAK_Updates", "SmartTRAK Updates"));
		StringBuilder copyRight = new StringBuilder(150);
		copyRight.append("&copy; 2017 BioMed GPS — SmartTRAK All trademarks and copyrighted material ");
		copyRight.append("are the property of BioMedGPS LLC or the respective owners thereof.");
		rss.setCopyright(copyRight.toString());
		rss.setArticles(searchItems); //set the search items
		
		return rss;
	}
	
}
