package com.mts.scheduler.job;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title</b>: ContentFeedVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Document VO replacement for the XML feed. Supports a 
 * different set of fields than the MTSDocumentVO
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jun 11, 2019
 * @updates:
 ****************************************************************************/

public class ContentFeedVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2919886766209555392L;
	
	
	// Members
	private String title;
	private String description;
	private String link;
	private String locale;
	private Date lastBuildDate;
	
	// Sub-Beans
	private List<ContentFeedItemVO> items = new ArrayList<>();
	
	
	/**
	 * 
	 */
	public ContentFeedVO() {
		super();
	}

	/**
	 * @param req
	 */
	public ContentFeedVO(ActionRequest req) {
		super(req);
		
	}

	/**
	 * @param rs
	 */
	public ContentFeedVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @return the link
	 */
	public String getLink() {
		return link;
	}

	/**
	 * @return the locale
	 */
	public String getLocale() {
		return locale;
	}

	/**
	 * @return the lastBuildDate
	 */
	public Date getLastBuildDate() {
		return lastBuildDate;
	}

	/**
	 * @return the items
	 */
	public List<ContentFeedItemVO> getItems() {
		return items;
	}
	
	
	/**
	 * loop the items and get a set of all the ids
	 * @return
	 */
	public List<String> getUniqueIds(){
		List <String> ids = new ArrayList<>();
		
		for (ContentFeedItemVO item : getItems()) {
			ids.add(item.getItemId());
		}
		
		return ids;
	}

	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @param link the link to set
	 */
	public void setLink(String link) {
		this.link = link;
	}

	/**
	 * @param items the items to set
	 */
	public void setItems(List<ContentFeedItemVO> items) {
		this.items = items;
	}

	/**
	 * @param locale the locale to set
	 */
	public void setLocale(String locale) {
		this.locale = locale;
	}

	/**
	 * @param lastBuildDate the lastBuildDate to set
	 */
	public void setLastBuildDate(Date lastBuildDate) {
		this.lastBuildDate = lastBuildDate;
	}

}

