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
	
	// Numeric Members
	private int approvalFlag;
	
	// Date Members
	private Date issueDate;
	private Date createDate;
	private Date updateDate;
	
	// Sub-Beans
	private List<AssetVO> assets = new ArrayList<>();

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
		this.assets.add(asset);
	}

	/**
	 * @param volume the volume to set
	 */
	public void setVolume(String volume) {
		this.volume = volume;
	}

}

