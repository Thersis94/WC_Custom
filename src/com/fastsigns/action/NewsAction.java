package com.fastsigns.action;

// JDK 6
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// SB_FastSigns
import com.fastsigns.action.vo.NewsContainerVO;

// SMTBaseLibs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WebCrescendo
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.content.ContentVO;
import com.smt.sitebuilder.action.rss.RSSCreatorReport;
import com.smt.sitebuilder.action.rss.RSSCreatorVO;
import com.smt.sitebuilder.action.tools.EmailFriendAction;
import com.smt.sitebuilder.action.tools.EmailFriendVO;
import com.smt.sitebuilder.action.tools.SearchVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: NewsAction.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Jan 8, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class NewsAction extends SBActionAdapter {

	public NewsAction() {
	}

	/**
	 * @param actionInit
	 */
	public NewsAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(SMTServletRequest req) throws ActionException {
		String cdb = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sb = new StringBuilder(900);
		String orgId = ((SiteVO)req.getAttribute("siteData")).getOrganizationId();
		boolean isRss = Convert.formatBoolean(this.getActionData(actionInit.getActionId()).getAttributes().get(SBModuleVO.ATTRIBUTE_2));
		if(isRss){
		sb.append("select BLOG_URL as item_action_id, TITLE_NM as item_action_nm, SHORT_DESC_TXT as item_action_desc, ");
		sb.append("BLOG_TXT as item_article_txt, PUBLISH_DT as item_create_dt, 'BLOG' as attribute1Text from blog ");
		sb.append("where action_id = (select action_id from SB_ACTION where MODULE_TYPE_ID = 'BLOG' and ORGANIZATION_ID= ? and PENDING_SYNC_FLG = 0 ) and approval_flg=1 ");
		sb.append("union ");
		}
		sb.append("select cast(CP_MODULE_OPTION_ID as nvarchar(32)) as item_action_id, OPTION_NM as item_action_nm, OPTION_DESC as item_action_desc, ");
		sb.append("ARTICLE_TXT as item_article_txt, START_DT as item_create_dt, 'NEWS' as attribute1Text from ").append(cdb).append("fts_cp_module_option ");
		sb.append("where fts_cp_module_type_id = 2 and franchise_id is null and START_DT is not null and approval_flg=1 and org_id = ? ");
		sb.append("order by item_create_dt desc, item_action_nm ");
		log.debug("SQL = " + sb.toString() + "|" + orgId);

		PreparedStatement ps = null;
		NewsContainerVO container = new NewsContainerVO();
		Map<String, ContentVO> data = new LinkedHashMap<String, ContentVO>();
		int i = 1;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			if(isRss)
				ps.setString(i++, orgId);
			ps.setString(i++, orgId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				ContentVO vo = new ContentVO();
				vo.setActionId(rs.getString("item_action_id"));
				vo.setActionName(rs.getString("item_action_nm"));
				vo.setActionDesc(rs.getString("item_action_desc"));
				vo.setArticle(rs.getString("item_article_txt"));
				vo.setCreateDate(rs.getDate("item_create_dt"));
				vo.setAttribute(ContentVO.ATTRIBUTE_1, rs.getString(ContentVO.ATTRIBUTE_1));
				data.put(vo.getActionId(), vo);
			}
			
			// Add the data to the container and store on the module 
			container.setArticles(data);
			container.setActionData(this.getActionData(actionInit.getActionId()));
			
			//boolean isRss = Convert.formatBoolean(container.getActionData().getAttributes().get(SBModuleVO.ATTRIBUTE_2));
			// Since Chrome does not have a built in RSS reader it's use will prevent rss reports from being created.
			if (isRss && !StringUtil.checkVal(req.getHeader("User-Agent")).contains("Chrome")) {
				this.createRSSReport(req, container);
			} else {
				this.putModuleData(container, data.size(), false);
			}
		} catch (Exception e) {
			log.error("Unable to retrieve news items", e);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
	}
	
	
	public void createRSSReport(SMTServletRequest req, NewsContainerVO data) {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		String url = "PressReleases?newsid=";
		
		// Get the portlet info
		RSSCreatorVO rss = this.getRSSInfo(req, data, url);
		rss.setBaseUrl(site.getSiteAlias());
		log.debug("RSS: " + rss);
		
		// Create the report
		RSSCreatorReport rcr = new RSSCreatorReport();
		rcr.setData(rss);
		rcr.setFileName("fastsigns_rss.xml");
		
		// Set the request to return a report
		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
		req.setAttribute(Constants.BINARY_DOCUMENT, rcr);
	}

	
	public RSSCreatorVO getRSSInfo(SMTServletRequest req, NewsContainerVO vo, String baseUrl) {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		String companyName = "FASTSIGNS";
		if(site.getOrganizationId().equals("FTS_AU")){
			companyName = "SIGNWAVE";
			}
		RSSCreatorVO rvo = new RSSCreatorVO();
		List<SearchVO> news = new ArrayList<SearchVO>();
		Set<String> s = vo.getArticles().keySet();
		for(Iterator<String> iter = s.iterator(); iter.hasNext(); ) {
			ContentVO cnt = vo.getArticles().get(iter.next());
			
			SearchVO search = new SearchVO();
			search.setActionId(cnt.getActionId());
			search.setActionName(cnt.getActionName());
			search.setCreateDate(cnt.getCreateDate());
			if(cnt.getAttribute(ContentVO.ATTRIBUTE_1).equals("NEWS"))
				search.setDocumentUrl(baseUrl + cnt.getActionId());
			else
				search.setDocumentUrl("Blog/qs/" + cnt.getActionId());
			search.setSummary(cnt.getActionDesc());
			search.setTitle(cnt.getActionName());
			news.add(search);
		}
		
		rvo.setArticles(news);
		rvo.setCategory(new GenericVO("news", companyName + " News and Press Releases"));
		rvo.setTitle(companyName + " News and Press Releases");
		rvo.setLanguage(site.getLocale().toString());
		rvo.setLink(baseUrl);
		rvo.setCopyright(companyName + ", Inc. All Rights Reserved");
		rvo.setDescription(companyName + " RSS Feed");
		return rvo;
	}
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	public void list(SMTServletRequest req) throws ActionException {
        String sbActionId = StringUtil.checkVal(req.getParameter(SB_ACTION_ID));
		if (sbActionId.length() == 0) {
			super.list(req);
			return;
		}
		
		this.putModuleData(getActionData(sbActionId), 1, true);
	}
	
	/**
	 * 
	 * @param sbActionId
	 * @return
	 */
	public 	SBModuleVO getActionData(String sbActionId) {
		String s = "select * from sb_action where action_id = ?";
		SBModuleVO module = new SBModuleVO();
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s);
			ps.setString(1, sbActionId);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
                module.setActionId(rs.getString("action_id"));
                module.setModuleTypeId(rs.getString("module_type_id"));
                module.setActionName(rs.getString("action_nm"));
                module.setActionDesc(rs.getString("action_desc"));
                module.setOrganizationId(rs.getString("organization_id"));
                module.setAttribute(SBModuleVO.ATTRIBUTE_1, rs.getString("attrib1_txt"));
                module.setAttribute(SBModuleVO.ATTRIBUTE_2, rs.getString("attrib2_txt"));
                module.setIntroText(rs.getString("intro_txt"));
			}
			
			
		} catch (Exception e) {
			log.error("Unable to retrieve news items", e);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		return module;
	}
	

	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("building - send the email-a-friend message");
		
		SiteVO site = (SiteVO)req.getAttribute("siteData");
		ActionInitVO ai = new ActionInitVO();

		//Here we use the same actionId for all Fastsigns branded orgs.
		ai.setActionId("c0a801653d87314d380475ef5668ddfb");
		ai.setName("Tell Someone About Us");
		
		//If the site is Signwave branded, use AU's ActionId.
		if(site.getCountryCode().equals("AU"))
			ai.setActionId("0a0014137c77504fed1c4b27b4e52892");
		
		ModuleVO mod = new ModuleVO();
		EmailFriendVO eVo = new EmailFriendVO();
		eVo.setCommentFlag(3); //override default w/user's comments
		eVo.setEmailSubject("News Article on " + site.getSiteAlias()); 
		eVo.setEmailMessage("");
		mod.setActionData(eVo);
		
		log.debug("******** Sending Email ...");
		
		SMTActionInterface sai = new EmailFriendAction(ai);
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		sai.build(req);
		
	}
}
