package com.fastsigns.action.franchise.vo.pages;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.siliconmtn.data.Tree;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.menu.MenuObj;



/****************************************************************************
 * <b>Title</b>: FranchiseMenuObj.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Dec 27, 2010<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class PageContainerVO_m_en_GB extends PageContainerVO{

	private static final long serialVersionUID = 1L;
	protected static final Logger log = Logger.getLogger(PageContainerVO_m_en_GB.class);

	public PageContainerVO_m_en_GB() {
		assignTypeVals();
		sharedId = "FTS_SHARED_UK";
	}
	
	/**
	 * Assigns the page to the appropriate collection
	 * @param page
	 */
	public void addPages(List<MenuObj> pages) {
		for (MenuObj page : pages) {
			if (pages.size() == 1) {
				selectedPage.setKey(page);
			}
			if (page.isVisible()) {
				//menu pages
				if (bizRules.containsKey(page.getDisplayName())) {
					bizRules.put(page.getDisplayName(), page);
				} else if (bizRules.get("Custom Link One").getPageId() == null && "link1".equalsIgnoreCase(page.getAliasName())) {
					bizRules.put("Custom Link One", page);
				} else if (bizRules.get("Custom Link Two").getPageId() == null && "link2".equalsIgnoreCase(page.getAliasName())) {
					bizRules.put("Custom Link Two", page);
				} else if (bizRules.get("Custom Page One").getPageId() == null) {
					bizRules.put("Custom Page One", page);
				} else if (bizRules.get("Custom Page Two").getPageId() == null) {
					bizRules.put("Custom Page Two", page);
				}
				menuPages.add(page);
			} else {
				//non-menu pages
				nonMenuPages.add(page);
			}
		}
		menus.addAll(new Tree(castNodeList()).preorderList());
	}
	
	/**
	 * Enter custom data that is required in center page views.
	 * @return
	 */
	public Map<String, String> getCustomVals(){
		Map<String, String> vals = new HashMap<String, String>();
		/*mobile sites do not use log modules at this time
		 * vals.put("loginPmid", PageContainerVO_en_GB.LOGIN_MOD_ID);	//Pmid for parent org's login module.
		 */
		return vals;
	}
	
/****************** FASTSIGNS BUSINESS LOGIC **********************************/
	/* Permissions Mapping
	 * @returns a mapping of pages and what level of "editability" the franchises have.
	 * Used by JSTL to determine which forms/fields we can offer the admins (to change).
	 * This logic has no impact on the live site functionality.
	 */
	public Map<String, Integer> getBusinessLogic() {
		//pages not listed here are considered custom (fully customizable)
		//there are duplicates on this list because sometimes lookup is done by name
		//and other times by alias.  Others have duplicates because of the HTML characters
		//(sometimes looked-up escaped, other times un-escaped).  Se-la-vi!
		Map<String, Integer> pgs = new HashMap<String, Integer>();
		pgs.put("Photo Gallery", CUSTOM_TEMPLATED);
		pgs.put("Custom Link One", EXTERNAL);
		pgs.put("Custom Link Two", EXTERNAL);
		pgs.put("link1", EXTERNAL);
		pgs.put("link2", EXTERNAL);
		return pgs;
	}
	
	/**
	 * This loads a default page list.
	 * All sites CAN have these pages, defaulting to these URLs, displayNames, etc.
	 */
	public void assignTypeVals() {
		bizRules.put("Photo Gallery", makePage("gallery", "Photo Gallery", null)); //Gallery Content	
		bizRules.put("Custom Page One", makePage("page1","Custom Page One", ""));
		bizRules.put("Custom Page Two", makePage("page2","Custom Page Two", ""));
		bizRules.put("Custom Link One", makePage("link1","Custom Link One", null)); //external
		bizRules.put("Custom Link Two", makePage("link2","Custom Link Two", null)); //external
	}

	/**
	 * returns a list of tags/data that Freemarker will use to populate 
	 * dynamic text on the Center's page.
	 * @return
	 */
	public Map<String, Object> getFreemarkerTags(SMTServletRequest req) {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("franchiseId", req.getSession().getAttribute("webeditFranId"));
		return data;
	}
/************************ END BUSINESS LOGIC **********************************/
}
