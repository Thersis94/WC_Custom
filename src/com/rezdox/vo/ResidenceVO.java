package com.rezdox.vo;

//Java 8
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

// SMTBaseLibs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.gis.GeocodeLocation;

/*****************************************************************************
 <p><b>Title</b>: ResidenceVO.java</p>
 <p><b>Description: </b>Value object that encapsulates a RezDox residence.</p>
 <p> 
 <p>Copyright: (c) 2018 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author Tim Johnson
 @version 1.0
 @since Feb 7, 2018
 <b>Changes:</b> 
 ***************************************************************************/

@Table(name="REZDOX_RESIDENCE")
public class ResidenceVO extends GeocodeLocation implements Serializable {
	private static final long serialVersionUID = -6288149815547303962L;

	private String residenceId;
	private String residenceName;
	private String profilePicPath;
	private Date lastSoldDate;
	private Date forSaleDate;
	private int privacyFlag;
	private Map<String, String> attributes;
	private Date createDate;
	private Date updateDate;

	public ResidenceVO() {
		super();
	}

	/**
	 * @param req
	 */
	public ResidenceVO(ActionRequest req) {
		this();
		populateData(req);
	}
	
	/**
	 * @return the residenceId
	 */
	@Column(name="residence_id", isPrimaryKey=true)
	public String getResidenceId() {
		return residenceId;
	}

	/**
	 * @param residenceId the residenceId to set
	 */
	public void setResidenceId(String residenceId) {
		this.residenceId = residenceId;
	}

	/**
	 * @return the residenceName
	 */
	@Column(name="residence_nm")
	public String getResidenceName() {
		return residenceName;
	}

	/**
	 * @param residenceName the residenceName to set
	 */
	public void setResidenceName(String residenceName) {
		this.residenceName = residenceName;
	}

	/**
	 * @return the profilePicPath
	 */
	@Column(name="profile_pic_pth")
	public String getProfilePicPath() {
		return profilePicPath;
	}

	/**
	 * @param profilePicPath the profilePicPath to set
	 */
	public void setProfilePicPath(String profilePicPath) {
		this.profilePicPath = profilePicPath;
	}

	/**
	 * @return the lastSoldDate
	 */
	@Column(name="last_sold_dt")
	public Date getLastSoldDate() {
		return lastSoldDate;
	}

	/**
	 * @param lastSoldDate the lastSoldDate to set
	 */
	public void setLastSoldDate(Date lastSoldDate) {
		this.lastSoldDate = lastSoldDate;
	}

	/**
	 * @return the forSaleDate
	 */
	@Column(name="for_sale_dt")
	public Date getForSaleDate() {
		return forSaleDate;
	}

	/**
	 * @param forSaleDate the forSaleDate to set
	 */
	public void setForSaleDate(Date forSaleDate) {
		this.forSaleDate = forSaleDate;
	}

	/**
	 * @return the privacyFlag
	 */
	@Column(name="privacy_flg")
	public int getPrivacyFlag() {
		return privacyFlag;
	}

	/**
	 * @param privacyFlag the privacyFlag to set
	 */
	public void setPrivacyFlag(int privacyFlag) {
		this.privacyFlag = privacyFlag;
	}

	/**
	 * @return the attributes
	 */
	public Map<String, String> getAttributes() {
		return attributes;
	}

	/**
	 * @param attributes the attributes to set
	 */
	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	/**
	 * @return the updateDate
	 */
	@Column(name="update_dt", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDate() {
		return updateDate;
	}

	/**
	 * @param updateDate the updateDate to set
	 */
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}
}
