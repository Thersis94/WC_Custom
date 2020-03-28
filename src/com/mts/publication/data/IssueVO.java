package com.mts.publication.data;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.mts.common.MTSConstants;
import com.mts.subscriber.data.MTSUserVO;
// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: IssueVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object for the MTS publication issue data
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 2, 2019
 * @updates:
 ****************************************************************************/
@Table(name="mts_issue")
public class IssueVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3825342634566115331L;
	
	// String Members
	private String issueId;
	private String publicationId;
	private String name;
	private String description;
	private String number;
	private String volume;
	private String seoPath;
	private String editorId;
	private String category;
	private String issuePdfUrl;
	
	// Numeric Members
	private int approvalFlag;
	private long numberArticles;
	
	// Date Members
	private Date issueDate;
	private Date createDate;
	private Date updateDate;
	
	// Sub-Beans
	private List<AssetVO> assets = new ArrayList<>();
	private MTSUserVO editor;
	
	// Helpers
	private String publicationName;
	private List<MTSDocumentVO> documents = new ArrayList<>();
	
	/**
	 * 
	 */
	public IssueVO() {
		super();
	}

	/**
	 * @param req
	 */
	public IssueVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public IssueVO(ResultSet rs) {
		super(rs);
	}
	
	/**
	 * Returns the link for the publication
	 * @return
	 */
	public String getSubscribeLink() {
		return MTSConstants.SUBSCRIBE_LINKS.get(StringUtil.checkVal(publicationId).toUpperCase());
	}

	/**
	 * @return the issueId
	 */
	@Column(name="issue_id", isPrimaryKey=true)
	public String getIssueId() {
		return issueId;
	}

	/**
	 * @return the publicationId
	 */
	@Column(name="publication_id")
	public String getPublicationId() {
		return publicationId;
	}

	/**
	 * @return the name
	 */
	@Column(name="issue_nm")
	public String getName() {
		return name;
	}

	/**
	 * @return the description
	 */
	@Column(name="issue_desc")
	public String getDescription() {
		return description;
	}

	/**
	 * @return the number
	 */
	@Column(name="issue_number_txt")
	public String getNumber() {
		return number;
	}

	/**
	 * @return the volume
	 */
	@Column(name="volume_number_txt")
	public String getVolume() {
		return volume;
	}

	/**
	 * @return the seoPath
	 */
	@Column(name="seo_friendly_path")
	public String getSeoPath() {
		return seoPath;
	}

	/**
	 * @return the editorId
	 */
	@Column(name="editor_id")
	public String getEditorId() {
		return editorId;
	}

	/**
	 * @return the approvalFlag
	 */
	@Column(name="approval_flg")
	public int getApprovalFlag() {
		return approvalFlag;
	}

	/**
	 * @return the issueDate
	 */
	@Column(name="issue_dt")
	public Date getIssueDate() {
		return issueDate;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @return the updateDate
	 */
	@Column(name="update_dt", isUpdateOnly=true, isAutoGen=true)
	public Date getUpdateDate() {
		return updateDate;
	}

	/**
	 * @return the assets
	 */
	public List<AssetVO> getAssets() {
		return assets;
	}

	/**
	 * @return the numberArticles
	 */
	@Column(name="article_no", isReadOnly=true)
	public long getNumberArticles() {
		return numberArticles;
	}

	/**
	 * @return the editor
	 */
	public MTSUserVO getEditor() {
		return editor;
	}

	/**
	 * @return the category
	 */
	@Column(name="category_cd")
	public String getCategory() {
		return category;
	}

	/**
	 * @param issueId the issueId to set
	 */
	public void setIssueId(String issueId) {
		this.issueId = issueId;
	}

	/**
	 * @param publicationId the publicationId to set
	 */
	public void setPublicationId(String publicationId) {
		this.publicationId = publicationId;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @param number the number to set
	 */
	public void setNumber(String number) {
		this.number = number;
	}

	/**
	 * @param seoPath the seoPath to set
	 */
	public void setSeoPath(String seoPath) {
		this.seoPath = seoPath;
	}

	/**
	 * @param editorId the editorId to set
	 */
	public void setEditorId(String editorId) {
		this.editorId = editorId;
	}

	/**
	 * @param approvalFlag the approvalFlag to set
	 */
	public void setApprovalFlag(int approvalFlag) {
		this.approvalFlag = approvalFlag;
	}

	/**
	 * @param issueDate the issueDate to set
	 */
	public void setIssueDate(Date issueDate) {
		this.issueDate = issueDate;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	/**
	 * @param updateDate the updateDate to set
	 */
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
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
		if (asset == null || StringUtil.isEmpty(asset.getDocumentPath())) return;
		
		// Sometimes a string agg function is used.  Parse the list into separate items
		if (asset.getDocumentPath().indexOf(',') > -1) {
			String[] dp = asset.getDocumentPath().split("\\,");
			for (String doc : dp) {
				AssetVO a = new AssetVO();
				a.setDocumentPath(doc);
				a.setObjectKeyId(asset.getObjectKeyId());
			}
		} else {
			this.assets.add(asset);
		}
	}

	/**
	 * @param volume the volume to set
	 */
	public void setVolume(String volume) {
		this.volume = volume;
	}

	/**
	 * @param numberArticles the numberArticles to set
	 */
	public void setNumberArticles(long numberArticles) {
		this.numberArticles = numberArticles;
	}

	/**
	 * @param editor the editor to set
	 */
	@BeanSubElement
	public void setEditor(MTSUserVO editor) {
		this.editor = editor;
	}

	/**
	 * @param category the category to set
	 */
	public void setCategory(String category) {
		this.category = category;
	}

	/**
	 * @return the publicationName
	 */
	@Column(name="publication_nm", isReadOnly=true)
	public String getPublicationName() {
		return publicationName;
	}

	/**
	 * @param publicationName the publicationName to set
	 */
	public void setPublicationName(String publicationName) {
		this.publicationName = publicationName;
	}

	/**
	 * @return the issuePdfUrl
	 */
	@Column(name="issue_pdf_url")
	public String getIssuePdfUrl() {
		return issuePdfUrl;
	}

	/**
	 * @param issuePdfUrl the issuePdfUrl to set
	 */
	public void setIssuePdfUrl(String issuePdfUrl) {
		this.issuePdfUrl = issuePdfUrl;
	}

	/**
	 * @return the documents
	 */
	public List<MTSDocumentVO> getDocuments() {
		return documents;
	}

	/**
	 * @param documents the documents to set
	 */
	public void setDocuments(List<MTSDocumentVO> documents) {
		this.documents = documents;
	}

}

