package com.fastsigns.action.franchise.vo.pages;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.siliconmtn.data.GenericVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.content.ContentVO;
import com.smt.sitebuilder.action.menu.MenuContainer;
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
public abstract class PageContainerVO implements Serializable {

	private static final long serialVersionUID = 1L;
	protected static final Logger log = Logger.getLogger(PageContainerVO.class);
	protected Map<String, MenuObj> bizRules = new LinkedHashMap<String, MenuObj>();
	protected List<MenuObj> menuPages = new ArrayList<MenuObj>();
	protected MenuContainer menus = new MenuContainer();
	protected List<MenuObj> nonMenuPages = new ArrayList<MenuObj>();
	protected GenericVO selectedPage = new GenericVO();
	protected String sharedId = "FTS_SHARED";
	
	public PageContainerVO() {
		assignTypeVals();
	}
	
	/**
	 * Assigns the page to the appropriate collection
	 * @param page
	 */
	public abstract void addPages(List<MenuObj> pages);
	
	public abstract Map<String, String> getCustomVals();
	/**
	 * turns our Collection<MenuObj> into a List<Node> for the MenuContainer to sort
	 * @return List<Node>
	 */
	protected List<Node> castNodeList() {
		List<Node> data = new ArrayList<Node>(menuPages.size());
		for (MenuObj p : menuPages) {
			Node n = new Node(p.getPageId(), p.getParentId());
			n.setNodeName(p.getDisplayName());
			n.setUserObject(p);
			data.add(n);
		}
		return data;
	}
	
	/**
	 * Returns a map of page aliases and the associated page data.  Page Data 
	 * is null if no page is created for the site
	 * @return
	 */
	public List<MenuObj> getMenuPages() {
		List<MenuObj> data = new ArrayList<MenuObj>();
		for (Node n : menus.getMenuPages()) {
			//log.debug(((MenuObj)n.getUserObject()).getDisplayName());
			data.add((MenuObj) n.getUserObject());
		}
		return data;
	}
	
	/**
	 * Returns a list of secondary pages in the system
	 * @return
	 */
	public List<MenuObj> getSecPages() {
		return nonMenuPages;
	}
	
	public Collection<MenuObj> getBizRules() {
		return bizRules.values();
	}
	
	/**
	 * 
	 * @return
	 */
	public MenuObj getPageById() {
		MenuObj page = null;
		if (nonMenuPages.size() > 0) page = nonMenuPages.get(0);
		
		return page;
	}
	
	
	/**
	 * creates a new MenuObj reflecting the arguments passed
	 * @param alias
	 * @param displayNm
	 * @param externalUrl
	 * @return
	 */
	public MenuObj makePage(String alias, String displayNm, String externalUrl) {
		return makePage(alias, displayNm, externalUrl, false);
	}
	
	protected MenuObj makePage(String alias, String displayNm, String externalUrl, boolean newWindow) {
		MenuObj p = new MenuObj();
		p.setDisplayName(displayNm);
		p.setExternalPageUrl(externalUrl);
		p.setAliasName(alias);
		p.setParentPath("/");
		if (newWindow) p.setLinkTarget("new");
		return p;
	}

	/**
	 * @return the selectedPage
	 */
	public GenericVO getSelectedPage() {
		return selectedPage;
	}

	/**
	 * @param selectedPage the selectedPage to set
	 */
	public void setSelectedPageContent(ContentVO content) {
		this.selectedPage.setValue(content);
	}
	
	public String getSharedId(){
		return sharedId;
	}

	
/****************** FASTSIGNS BUSINESS LOGIC **********************************/
	/* Permissions Mapping
	 * @returns a mapping of pages and what level of "editability" the franchises have.
	 * Used by JSTL to determine which forms/fields we can offer the admins (to change).
	 * This logic has no impact on the live site functionality.
	 */
	public static final Integer CUSTOM_OR_CORP = Integer.valueOf(10);
	public static final Integer CUSTOM_TEMPLATED = Integer.valueOf(30);
	public static final Integer EXTERNAL = Integer.valueOf(50);
	public static final Integer CORP = Integer.valueOf(100);
	public abstract Map<String, Integer> getBusinessLogic();
	
	/**
	 * This loads a default page list.
	 * All sites CAN have these pages, defaulting to these URLs, displayNames, etc.
	 */
	public abstract void assignTypeVals();
	
	public abstract Map<String, Object> getFreemarkerTags(SMTServletRequest req);

/************************ END BUSINESS LOGIC **********************************/
}
