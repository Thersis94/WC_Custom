package com.mts.publication.data;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// SMT Base Libs 3.x
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: SponsorVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Data bean storing the information for the sponsor table
 * <b>Copyright:</b> Copyright (c) 2020
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 1, 2020
 * @updates:
 ****************************************************************************/
@Table(name="mts_sponsor")
public class SponsorVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5010062305218878233L;
	
	// Members
	private String sponsorId;
	private String name;
	private String description;
	private String shortDesc;
	private String websiteUrl;
	private String logo;
	private int activeFlag;
	private Date createDate;
	private Date updateDate;
	
	// Helper members
	private int totalImages;
	private List<AssetVO> assets = new ArrayList<>();

	/**
	 * 
	 */
	public SponsorVO() {
		super();
	}

	/**
	 * @param req
	 */
	public SponsorVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public SponsorVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the sponsorId
	 */
	@Column(name="sponsor_id", isPrimaryKey = true)
	public String getSponsorId() {
		return sponsorId;
	}

	/**
	 * @return the name
	 */
	@Column(name="sponsor_nm")
	public String getName() {
		return name;
	}

	/**
	 * @return the description
	 */
	@Column(name="sponsor_desc")
	public String getDescription() {
		return description;
	}

	/**
	 * @return the shortDesc
	 */
	@Column(name="short_desc")
	public String getShortDesc() {
		return shortDesc;
	}

	/**
	 * @return the websiteUrl
	 */
	@Column(name="website_url")
	public String getWebsiteUrl() {
		return websiteUrl;
	}

	/**
	 * @return the logo
	 */
	@Column(name="image_path")
	public String getLogo() {
		return logo;
	}

	/**
	 * @return the activeFlag
	 */
	@Column(name="active_flg")
	public int getActiveFlag() {
		return activeFlag;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly = true, isAutoGen = true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @return the updateDate
	 */
	@Column(name="update_dt", isUpdateOnly = true, isAutoGen = true)
	public Date getUpdateDate() {
		return updateDate;
	}

	/**
	 * @return the totalImages
	 */
	@Column(name="total_images", isReadOnly = true)
	public int getTotalImages() {
		return totalImages;
	}

	/**
	 * @return the assets
	 */
	public List<AssetVO> getAssets() {
		return assets;
	}

	/**
	 * @param sponsorId the sponsorId to set
	 */
	public void setSponsorId(String sponsorId) {
		this.sponsorId = sponsorId;
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
	 * @param shortDesc the shortDesc to set
	 */
	public void setShortDesc(String shortDesc) {
		this.shortDesc = shortDesc;
	}

	/**
	 * @param websiteUrl the websiteUrl to set
	 */
	public void setWebsiteUrl(String websiteUrl) {
		this.websiteUrl = websiteUrl;
	}

	/**
	 * @param logo the logo to set
	 */
	public void setLogo(String logo) {
		this.logo = logo;
	}

	/**
	 * @param activeFlag the activeFlag to set
	 */
	public void setActiveFlag(int activeFlag) {
		this.activeFlag = activeFlag;
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
	 * @param totalImages the totalImages to set
	 */
	public void setTotalImages(int totalImages) {
		this.totalImages = totalImages;
	}

	/**
	 * @param assets the assets to set
	 */
	public void setAssets(List<AssetVO> assets) {
		this.assets = assets;
	}

}