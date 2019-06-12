package com.mts.scheduler.job;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;

/****************************************************************************
 * <b>Title</b>: ContentFeedItemVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> VO holding information for an individual item in the data feed
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jun 11, 2019
 * @updates:
 ****************************************************************************/

public class ContentFeedItemVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2919886766209555392L;
	
	
	// Members
	private String itemId;
	private String title;
	private String description;
	private String link;
	private String authorName;
	private Date publishDate;
	private String source;
	
	/**
	 * 
	 */
	public ContentFeedItemVO() {
		super();
	}

	/**
	 * @param req
	 */
	public ContentFeedItemVO(ActionRequest req) {
		super(req);
		
	}

	/**
	 * @param rs
	 */
	public ContentFeedItemVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the itemId
	 */
	@Column(name="unique_cd", isPrimaryKey=true)
	public String getItemId() {
		return itemId;
	}

	/**
	 * @return the title
	 */
	@Column(name="action_nm")
	public String getTitle() {
		return title;
	}

	/**
	 * @return the description
	 */
	@Column(name="action_desc")
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
	 * @return the authorName
	 */
	@Column(name="author_nm")
	public String getAuthorName() {
		return authorName;
	}

	/**
	 * @return the publishDate
	 */
	@Column(name="publish_dt")
	public Date getPublishDate() {
		return publishDate;
	}

	/**
	 * @return the source
	 */
	@Column(name="document_txt")
	public String getSource() {
		return source;
	}

	/**
	 * @param itemId the itemId to set
	 */
	public void setItemId(String itemId) {
		this.itemId = itemId;
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
	 * @param authorName the authorName to set
	 */
	public void setAuthorName(String authorName) {
		this.authorName = authorName;
	}

	/**
	 * @param publishDate the publishDate to set
	 */
	public void setPublishDate(Date publishDate) {
		this.publishDate = publishDate;
	}

	/**
	 * @param source the source to set
	 */
	public void setSource(String source) {
		this.source = source;
	}

}

