package com.mts.scheduler.job;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.util.RandomAlphaNumeric;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.tools.SiteRedirVO;

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
	private String creator;
	private Date pubDate;
	private String content;
	private transient String publicationId;
	private transient String imagePath;
	private transient String shortUrl;
	
	/**
	 * Builds a redirect URL for shortening the url to 8 characters
	 * @return
	 */
	public SiteRedirVO getRedir() {
		SiteRedirVO vo = new SiteRedirVO();
		vo.setSiteRedirectId(new UUIDGenerator().getUUID());
		vo.setSiteId("MTS_2");
		vo.setActiveFlag(1);
		vo.setCreateDate(new Date());
		vo.setGlobalFlag(0);
		vo.setPermRedirFlag(0);
		vo.setLogRedirFlag(0);
		vo.setDestinationUrl("/" + publicationId.toLowerCase() + "/article/" + link);
		vo.setAliasText("/" + RandomAlphaNumeric.generateRandom(8));
		shortUrl = vo.getAliasText();
		return vo;
	}
	
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
	@Column(name="direct_access_pth")
	public String getLink() {
		return link;
	}

	/**
	 * @return the authorName
	 */
	@Column(name="author_nm")
	public String getCreator() {
		return creator;
	}

	/**
	 * @return the publishDate
	 */
	@Column(name="publish_dt")
	public Date getPubDate() {
		return pubDate;
	}

	/**
	 * @return the source
	 */
	@Column(name="document_txt")
	public String getContent() {
		return content;
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
	public void setCreator(String creator) {
		this.creator = creator;
	}

	/**
	 * @param publishDate the publishDate to set
	 */
	public void setPubDate(Date pubDate) {
		this.pubDate = pubDate;
	}

	/**
	 * @param source the source to set
	 */
	public void setContent(String content) {
		this.content = content;
	}

	/**
	 * @return the publicationId
	 */
	@Column(name="publication_id")
	public String getPublicationId() {
		return publicationId;
	}

	/**
	 * @param publicationId the publicationId to set
	 */
	public void setPublicationId(String publicationId) {
		this.publicationId = publicationId;
	}

	/**
	 * @return the imagePath
	 */
	@Column(name="document_path")
	public String getImagePath() {
		return imagePath;
	}

	/**
	 * @param imagePath the imagePath to set
	 */
	public void setImagePath(String imagePath) {
		this.imagePath = imagePath;
	}

	/**
	 * @return the shortUrl
	 */
	public String getShortUrl() {
		return shortUrl;
	}

	/**
	 * @param shortUrl the shortUrl to set
	 */
	public void setShortUrl(String shortUrl) {
		this.shortUrl = shortUrl;
	}
}

