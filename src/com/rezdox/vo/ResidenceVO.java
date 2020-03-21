package com.rezdox.vo;

//Java 8
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

// SMTBaseLibs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.util.Convert;

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
	private Map<String, Object> attributes = new HashMap<>();
	private Date createDate;
	private Date updateDate;
	private double projectsTotal;
	private double projectsValuation;
	private double inventoryTotal;
	private int statusFlag; //comes from the member_xr - used to denote a shared residence

	/**
	 * Special use keys for values from the attributes table in the attibutes map
	 */
	private static final String BEDS_NO = "bedsNo";
	private static final String BATHS_NO = "bathsNo";
	private static final String SQFT_NO = "sqftNo";
	private static final String ZESTIMATE_NO = "zestimateNo";
	private static final String LAST_SOLD = "lastSoldPrice";

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
	 * @return the bedsNo
	 */
	@Column(name="beds_no", isReadOnly=true)
	public Integer getBedsNo() {
		return (Integer) attributes.get(BEDS_NO);
	}

	/**
	 * @param bedsNo the bedsNo to set
	 */
	public void setBedsNo(Integer bedsNo) {
		attributes.put(BEDS_NO, bedsNo);
	}

	/**
	 * @return the bathsNo
	 */
	@Column(name="baths_no", isReadOnly=true)
	public Double getBathsNo() {
		return (Double) attributes.get(BATHS_NO);
	}

	/**
	 * @param bathsNo the bathsNo to set
	 */
	public void setBathsNo(Double bathsNo) {
		attributes.put(BATHS_NO, bathsNo);
	}

	/**
	 * @return the sqftNo
	 */
	@Column(name="sqft_no", isReadOnly=true)
	public Integer getSqftNo() {
		return (Integer) attributes.get(SQFT_NO);
	}

	/**
	 * @param sqftNo the sqftNo to set
	 */
	public void setSqftNo(Integer sqftNo) {
		attributes.put(SQFT_NO, sqftNo);
	}

	/**
	 * @return the zestimateNo
	 */
	@Column(name="zestimate_no", isReadOnly=true)
	public Double getZestimateNo() {
		return (Double) attributes.get(ZESTIMATE_NO);
	}

	/**
	 * @param zestimateNo the zestimateNo to set
	 */
	public void setZestimateNo(Double zestimateNo) {
		attributes.put(ZESTIMATE_NO, zestimateNo);
	}

	/**
	 * @return the lastSoldPrice
	 */
	@Column(name=LAST_SOLD, isReadOnly=true)
	public Double getLastSold() {
		return (Double) attributes.get(LAST_SOLD);
	}

	/**
	 * @param zestimateNo the zestimateNo to set
	 */
	public void setLastSold(Double purchasePriceNo) {
		attributes.put(LAST_SOLD, purchasePriceNo);
	}

	/**
	 * @return the attributes
	 */
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	/**
	 * @param attributes the attributes to set
	 */
	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	/**
	 * @param attributes the attributes to set
	 */
	@BeanSubElement
	public void addAttribute(ResidenceAttributeVO attr) {
		this.attributes.put(attr.getSlugText(), attr.getValueText());
	}

	/**
	 * @return the latitude
	 */
	@Override
	@Column(name="latitude_no")
	public Double getLatitude() {
		return latitude;
	}

	/**
	 * @return the longitude
	 */
	@Override
	@Column(name="longitude_no")
	public Double getLongitude() {
		return longitude;
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

	/**
	 * @return the projectsTotal (raw total - no coefficient applied)
	 */
	@Column(name="projects_total", isReadOnly=true)
	public double getProjectsTotal() {
		return projectsTotal;
	}

	/**
	 * @param projectsTotal the projectsTotal to set
	 */
	public void setProjectsTotal(double projectsTotal) {
		this.projectsTotal = projectsTotal;
	}

	/**
	 * returns a business-decision calculation of  zestimate + valuation of improvements valuation math no longer occurs in sql.  business logic belongs to java and is now done in projects action.
	 * @return
	 */
	public double getRealMarketValue() {
		return Convert.formatDouble(getZestimateNo()) + getProjectsValuation();
	}

	/**
	 * returns a business-decision calculation of  zestimate + improvements (fractal)
	 * @return
	 */
	public double getRealMarketValueDashboard() {
		return Convert.formatDouble(getZestimateNo()) + getProjectsValuation();
	}

	@Column(name="inventory_total", isReadOnly=true)
	public double getInventoryTotal() {
		return inventoryTotal;
	}

	public void setInventoryTotal(double inventoryTotal) {
		this.inventoryTotal = inventoryTotal;
	}

	@Column(name="status_flg", isReadOnly=true)
	public int getStatusFlag() {
		return statusFlag;
	}

	public void setStatusFlag(int statusFlag) {
		this.statusFlag = statusFlag;
	}

	public boolean isShared() {
		return 2 == statusFlag;
	}

	@Column(name="projects_valuation", isReadOnly=true)
	public double getProjectsValuation() {
		return projectsValuation;
	}

	public void setProjectsValuation(double projectsValuation) {
		this.projectsValuation = projectsValuation;
	}
}