package com.restpeer.data;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.restpeer.common.RPConstants.MemberType;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: MemberVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the Member Information 
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 15, 2019
 * @updates:
 ****************************************************************************/
@Table(name="rp_member")
public class MemberVO extends BeanDataVO {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1529501398777594661L;

	// Members
	private String memberId;
	private String name;
	private MemberType memberType;
	private int activeFlag;
	private Date createDate;
	
	// Helpers
	private long numLocations;
	private String memberTypeName;
	
	// Sub-Beans
	private List<MemberLocationVO> locations = new ArrayList<>(); 
	
	/**
	 * 
	 */
	public MemberVO() {
		super();
	}

	/**
	 * @param req
	 */
	public MemberVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public MemberVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the memberId
	 */
	@Column(name="member_id", isPrimaryKey=true)
	public String getMemberId() {
		return memberId;
	}

	/**
	 * @return the name
	 */
	@Column(name="member_nm")
	public String getName() {
		return name;
	}

	/**
	 * @return the memberType
	 */
	@Column(name="member_type_cd")
	public MemberType getMemberType() {
		return memberType;
	}

	/**
	 * @return the activeFlag
	 */
	@Column(name="active_flg")
	public int getActiveFlag() {
		return activeFlag;
	}

	/**
	 * @return the numLocations
	 */
	@Column(name="locations_no", isReadOnly=true)
	public long getNumLocations() {
		return numLocations;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @return the locations
	 */
	public List<MemberLocationVO> getLocations() {
		return locations;
	}

	/**
	 * @return the memberTypeName
	 */
	@Column(name="type_nm", isReadOnly=true)
	public String getMemberTypeName() {
		return memberTypeName;
	}

	/**
	 * @param memberId the memberId to set
	 */
	public void setMemberId(String memberId) {
		this.memberId = memberId;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param memberType the memberType to set
	 */
	public void setMemberType(MemberType memberType) {
		this.memberType = memberType;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	/**
	 * @param locations the locations to set
	 */
	public void setLocations(List<MemberLocationVO> locations) {
		this.locations = locations;
	}

	/**
	 * @param activeFlag the activeFlag to set
	 */
	public void setActiveFlag(int activeFlag) {
		this.activeFlag = activeFlag;
	}

	/**
	 * @param numLocations the numLocations to set
	 */
	public void setNumLocations(long numLocations) {
		this.numLocations = numLocations;
	}

	/**
	 * @param memberTypeName the memberTypeName to set
	 */
	public void setMemberTypeName(String memberTypeName) {
		this.memberTypeName = memberTypeName;
	}

}

