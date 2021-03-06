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
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.util.StringUtil;
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
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException{
		//call to super retrieve for admin registration
		super.retrieve(req);
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
	 * Returns list of updates with twitter messages and publish dates of today  
	 * @return
	 */
	protected List<UpdateVO> getTwitterUpdates(){
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		
		StringBuilder sql = new StringBuilder(400);
		sql.append("select update_id, market_id, product_id, company_id, title_txt, type_cd, ");
		sql.append("message_txt, twitter_txt, tweet_flg, publish_dt, create_dt, update_dt, ");
		sql.append("'").append(getAttribute(Constants.QS_PATH)).append("' as qs_path ");
		sql.append("from ").append(schema).append("biomedgps_update ");
		sql.append("where tweet_flg = 1 and email_flg = 1 and status_cd in ('R','N') ");
		sql.append("and cast(publish_dt as date) = current_date ");
		sql.append("and COALESCE(update_dt, create_dt) + (interval '1 hour') <= current_timestamp "); //allow at least one hour before submitting live
		sql.append("order by publish_dt desc, create_dt desc ");
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
		
		//add the relevant pieces to create the search vo
		for (UpdateVO update : updates) {
			SearchVO vo = new SearchVO();
			vo.setActionId(update.getUpdateId());
	       	vo.setTitle(update.getTitle());
	        vo.setSummary(update.getTwitterTxt());
	        vo.setCreateDate(update.getCreateDt());
	        //ensure each document url is unique
	        vo.setDocumentUrl(buildRSSDocumentUrl(update));   
	        vo.setLinkOmitted(true); //omit the link for each item
	        searchItems.add(vo); //add the item
		}
		
		return searchItems;
	}
	
	/**
	 * builds the update RSS feed url
	 * @param update
	 * @return
	 */
	private String buildRSSDocumentUrl(UpdateVO update){
        StringBuilder docUrl = new StringBuilder(100);
        String updateUrl = update.getDocumentUrl();

        if(StringUtil.isEmpty(updateUrl)){
        	docUrl.append("?rss=1&amp;searchData=").append(StringEncoder.urlEncode(update.getTitle()));
        }else{        	
        	//remove the beginning slash, as the RSSCreatorReport will add one for us
        	if(updateUrl.indexOf('/') == 0){
        		updateUrl = updateUrl.substring(1);
        	}
        	
    		UUIDGenerator uuid = new UUIDGenerator();
    		docUrl.append(updateUrl).append("/").append(uuid.getUUID()); 
        }
        return docUrl.toString();
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
