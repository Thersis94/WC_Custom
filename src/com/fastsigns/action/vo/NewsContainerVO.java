package com.fastsigns.action.vo;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.content.ContentVO;

/****************************************************************************
 * <b>Title</b>: NewsContainerVO.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Jan 9, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class NewsContainerVO implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	// Member Variables
	private Map<String, ContentVO> articles = new LinkedHashMap<String, ContentVO>();
	private SBModuleVO actionData = new SBModuleVO();

	/**
	 * 
	 */
	public NewsContainerVO(Map<String, ContentVO> articles, SBModuleVO actionData) {
		this.articles = articles;
		this.actionData = actionData;
	}
	
	/**
	 * 
	 */
	public NewsContainerVO() {
		
	}

	/**
	 * @return the articles
	 */
	public Map<String, ContentVO> getArticles() {
		return articles;
	}

	/**
	 * @param articles the articles to set
	 */
	public void setArticles(Map<String, ContentVO> articles) {
		this.articles = articles;
	}

	/**
	 * @return the actonData
	 */
	public SBModuleVO getActionData() {
		return actionData;
	}

	/**
	 * @param actonData the actonData to set
	 */
	public void setActionData(SBModuleVO actionData) {
		this.actionData = actionData;
	}
	
	/**
	 * Calculates the number of pages using the action data RPP
	 * @return
	 */
	public int getNumberPages() {
		int pages = 0;
		
		int rpp = Convert.formatInteger((String)actionData.getAttribute(SBModuleVO.ATTRIBUTE_1));
		if (rpp == 0 || articles.size() < rpp) return 1;
		pages = (int) articles.size()/rpp;
		if ((pages % rpp) > 0) pages ++; 
		
		return pages;
	}
}
