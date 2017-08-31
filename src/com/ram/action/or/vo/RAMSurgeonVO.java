package com.ram.action.or.vo;

// JDK 1.8
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// WC Libs
import com.ram.datafeed.data.CustomerVO;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/********************************************************************
 * <b>Title: </b>RAMSurgeonVO.java<br/>
 * <b>Description: </b>Java bean holding surgeon information<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author James Camire
 * @version 3.x
 * @since Aug 15, 2017
 * Last Updated: 
 *******************************************************************/
@Table(name="ram_surgeon")
public class RAMSurgeonVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3201387955239747560L;

	// Member Variables
	private String surgeonId;
	private String firstName;
	private String lastName;
	private String emailAddress;
	private String uniqueId;
	private Integer birthYear;
	private int activeFlag;
	private int receiveEmailFlag;
	private String gender;
	private Date createDate;
	private Date updateDate;
	
	// Associated Data
	private List<CustomerVO> providers = new ArrayList<>();
	
	/**
	 * 
	 */
	public RAMSurgeonVO() {
		super();
	}

	/**
	 * Populates member variables form request object
	 * @param req
	 */
	public RAMSurgeonVO(ActionRequest req) {
		super();
		this.populateData(req);
	}
	
	/**
	 * Populates member variables form result set object
	 * @param req
	 */
	public RAMSurgeonVO(ResultSet rs) {
		super();
		this.populateData(rs);
	}

	/**
	 * @return the surgeonId
	 */
	@Column(name="surgeon_id", isPrimaryKey=true)
	public String getSurgeonId() {
		return surgeonId;
	}

	/**
	 * @return the firstName
	 */
	@Column(name="first_nm")
	public String getFirstName() {
		return firstName;
	}

	/**
	 * @return the lastName
	 */
	@Column(name="last_nm")
	public String getLastName() {
		return lastName;
	}

	/**
	 * @return the uniqueId
	 */
	@Column(name="unique_id")
	public String getUniqueId() {
		return uniqueId;
	}

	/**
	 * @return the birthYear
	 */
	@Column(name="birth_yr")
	public Integer getBirthYear() {
		return birthYear;
	}

	/**
	 * @return the gender
	 */
	@Column(name="gender_cd")
	public String getGender() {
		return gender;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @return the updateDate
	 */
	@Column(name="update_dt", isUpdateOnly=true)
	public Date getUpdateDate() {
		return updateDate;
	}

	/**
	 * @return the providers
	 */
	public List<CustomerVO> getProviders() {
		return providers;
	}

	/**
	 * @param surgeonId the surgeonId to set
	 */
	public void setSurgeonId(String surgeonId) {
		this.surgeonId = surgeonId;
	}

	/**
	 * @param firstName the firstName to set
	 */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	/**
	 * @param lastName the lastName to set
	 */
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	/**
	 * @param uniqueId the uniqueId to set
	 */
	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}

	/**
	 * @param birthYear the birthYear to set
	 */
	public void setBirthYear(Integer birthYear) {
		this.birthYear = birthYear;
	}

	/**
	 * @param gender the gender to set
	 */
	public void setGender(String gender) {
		this.gender = gender;
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
	 * @param providers the providers to set
	 */
	public void setProviders(List<CustomerVO> providers) {
		this.providers = providers;
	}
	
	/**
	 * Adds a provider to the collection of providers
	 * @param provider
	 */
	@BeanSubElement
	public void addProvider(CustomerVO provider) {
		providers.add(provider);
	}

	/**
	 * @return the emailAddress
	 */
	@Column(name="email_address_txt")
	public String getEmailAddress() {
		return emailAddress;
	}

	/**
	 * @param emailAddress the emailAddress to set
	 */
	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	/**
	 * @return the activeFlag
	 */
	@Column(name="active_flg")
	public int getActiveFlag() {
		return activeFlag;
	}

	/**
	 * @param activeFlag the activeFlag to set
	 */
	public void setActiveFlag(int activeFlag) {
		this.activeFlag = activeFlag;
	}

	/**
	 * @return the receiveEmailFlag
	 */
	@Column(name="receive_email_flg")
	public int getReceiveEmailFlag() {
		return receiveEmailFlag;
	}

	/**
	 * @param receiveEmailFlag the receiveEmailFlag to set
	 */
	public void setReceiveEmailFlag(int receiveEmailFlag) {
		this.receiveEmailFlag = receiveEmailFlag;
	}
}