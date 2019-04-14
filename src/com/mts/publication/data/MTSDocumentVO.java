package com.mts.publication.data;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// MTS Libs
import com.mts.subscriber.data.MTSUserVO;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.smt.sitebuilder.action.content.DocumentVO;

/****************************************************************************
 * <b>Title</b>: MTSDocumentVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object for the MTS documents and articles
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 2, 2019
 * @updates:
 ****************************************************************************/
@Table(name="mts_document")
public class MTSDocumentVO extends DocumentVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6901558022608072385L;
	
	// Members
	private String documentId;
	private String issueId;
	private Date publishDate;
	private String uniqueCode;
	private String authorId;
	
	// Sub-Beans
	private List<AssetVO> assets = new ArrayList<>();
	private List<DocumentCategoryVO> categories = new ArrayList<>();
	private MTSUserVO author;
	
	// Helpers
	private String issueName;
	private String publicationId;
	private String publicationName;
	
	/**
	 * 
	 */
	public MTSDocumentVO() {
		super();
	}

	/**
	 * @param req
	 */
	public MTSDocumentVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public MTSDocumentVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the documentId
	 */
	@Column(name="document_id", isPrimaryKey=true)
	public String getDocumentId() {
		return documentId;
	}

	/**
	 * @return the issueId
	 */
	@Column(name="issue_id")
	public String getIssueId() {
		return issueId;
	}

	/**
	 * @return the assets
	 */
	public List<AssetVO> getAssets() {
		return assets;
	}

	/**
	 * @return the categories
	 */
	public List<DocumentCategoryVO> getCategories() {
		return categories;
	}

	/**
	 * @return the publishDate
	 */
	@Column(name="publish_dt")
	public Date getPublishDate() {
		return publishDate;
	}

	/**
	 * @return the articleCode
	 */
	@Column(name="unique_cd")
	public String getUniqueCode() {
		return uniqueCode;
	}

	/**
	 * @return the authorId
	 */
	@Column(name="author_id")
	public String getAuthorId() {
		return authorId;
	}

	/**
	 * @return the issueName
	 */
	@Column(name="issue_nm", isReadOnly=true)
	public String getIssueName() {
		return issueName;
	}

	/**
	 * @return the publicationId
	 */
	@Column(name="publication_id", isReadOnly=true)
	public String getPublicationId() {
		return publicationId;
	}

	/**
	 * @return the publicationName
	 */
	@Column(name="publication_nm", isReadOnly=true)
	public String getPublicationName() {
		return publicationName;
	}

	/**
	 * @return the author
	 */
	public MTSUserVO getAuthor() {
		return author;
	}

	/**
	 * @param documentId the documentId to set
	 */
	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}

	/**
	 * @param issueId the issueId to set
	 */
	public void setIssueId(String issueId) {
		this.issueId = issueId;
	}

	/**
	 * @param assets the assets to set
	 */
	public void setAssets(List<AssetVO> assets) {
		this.assets = assets;
	}
	
	/**
	 * @param asset Add an asset to the assets set
	 */
	@BeanSubElement
	public void addAsset(AssetVO asset) {
		this.assets.add(asset);
	}

	/**
	 * @param categories the categories to set
	 */
	public void setCategories(List<DocumentCategoryVO> categories) {
		this.categories = categories;
	}
	
	/**
	 * @param cat Add an category to the categories set
	 */
	@BeanSubElement
	public void addCategory(DocumentCategoryVO cat) {
		this.categories.add(cat);
	}

	/**
	 * @param publishDate the publishDate to set
	 */
	public void setPublishDate(Date publishDate) {
		this.publishDate = publishDate;
	}

	/**
	 * @param articleCode the articleCode to set
	 */
	public void setUniqueCode(String uniqueCode) {
		this.uniqueCode = uniqueCode;
	}

	/**
	 * @param authorId the authorId to set
	 */
	public void setAuthorId(String authorId) {
		this.authorId = authorId;
	}

	/**
	 * @param author the author to set
	 */
	@BeanSubElement
	public void setAuthor(MTSUserVO author) {
		this.author = author;
	}

	/**
	 * @param issueName the issueName to set
	 */
	public void setIssueName(String issueName) {
		this.issueName = issueName;
	}

	/**
	 * @param publicationId the publicationId to set
	 */
	public void setPublicationId(String publicationId) {
		this.publicationId = publicationId;
	}

	/**
	 * @param publicationName the publicationName to set
	 */
	public void setPublicationName(String publicationName) {
		this.publicationName = publicationName;
	}

}

