package com.mts.publication.data;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: PublicationVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object for the MTS publication
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 2, 2019
 * @updates:
 ****************************************************************************/
@Table(name="mts_publication")
public class PublicationVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -549887264910232067L;
	
	// Members
	private String publicationId;
	private String name;
	private String description;
	private String editorId;
	private String publicationNumber;
	private String seoPath;
	
	// Numeric Members
	private int approvalFlag;
	
	// Date Members
	private Date initialPublicationDate;
	private Date createDate;
	private Date updateDate;
	
	// Sub-Beans
	private List<IssueVO> issues = new ArrayList<>();
	private List<AssetVO> assets = new ArrayList<>();
	
	/**
	 * 
	 */
	public PublicationVO() {
		super();
	}

	/**
	 * @param req
	 */
	public PublicationVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public PublicationVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the publicationId
	 */
	@Column(name="publication_id", isPrimaryKey=true)
	public String getPublicationId() {
		return publicationId;
	}

	/**
	 * @return the name
	 */
	@Column(name="publication_nm")
	public String getName() {
		return name;
	}

	/**
	 * @return the description
	 */
	@Column(name="publication_desc")
	public String getDescription() {
		return description;
	}

	/**
	 * @return the editorId
	 */
	@Column(name="editor_id")
	public String getEditorId() {
		return editorId;
	}

	/**
	 * @return the publicationNumber
	 */
	@Column(name="publication_number_txt")
	public String getPublicationNumber() {
		return publicationNumber;
	}

	/**
	 * @return the seoPath
	 */
	@Column(name="seo_friendly_path")
	public String getSeoPath() {
		return seoPath;
	}

	/**
	 * @return the approvalFlag
	 */
	@Column(name="approval_flg")
	public int getApprovalFlag() {
		return approvalFlag;
	}

	/**
	 * @return the initialPublicationDate
	 */
	@Column(name="initial_publication_dt")
	public Date getInitialPublicationDate() {
		return initialPublicationDate;
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
	 * @return the issues
	 */
	public List<IssueVO> getIssues() {
		return issues;
	}

	/**
	 * @return the assets
	 */
	public List<AssetVO> getAssets() {
		return assets;
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
	 * @param editorId the editorId to set
	 */
	public void setEditorId(String editorId) {
		this.editorId = editorId;
	}

	/**
	 * @param publicationNumber the publicationNumber to set
	 */
	public void setPublicationNumber(String publicationNumber) {
		this.publicationNumber = publicationNumber;
	}

	/**
	 * @param seoPath the seoPath to set
	 */
	public void setSeoPath(String seoPath) {
		this.seoPath = seoPath;
	}

	/**
	 * @param approvalFlag the approvalFlag to set
	 */
	public void setApprovalFlag(int approvalFlag) {
		this.approvalFlag = approvalFlag;
	}

	/**
	 * @param initialPublicationDate the initialPublicationDate to set
	 */
	public void setInitialPublicationDate(Date initialPublicationDate) {
		this.initialPublicationDate = initialPublicationDate;
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
	 * @param issues the issues to set
	 */
	public void setIssues(List<IssueVO> issues) {
		this.issues = issues;
	}
	/**
	 * @param asset Add an issue to the issues set
	 */
	@BeanSubElement
	public void addIssue(IssueVO issue) {
		this.issues.add(issue);
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
}

