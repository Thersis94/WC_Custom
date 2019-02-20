package com.restpeer.data;

// JDK 1.8.x
import java.sql.ResultSet;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.user.UserVO;

/****************************************************************************
 * <b>Title</b>: UserVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> manages the extended (non-encrypted) user information
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 15, 2019
 * @updates:
 ****************************************************************************/
@Table(name="rp_user")
public class RPUserVO extends UserVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8846752021614138570L;
	
	// Members
	private String driverLicense;
	private String driverLicensePath;

	/**
	 * 
	 */
	public RPUserVO() {
		super();
	}

	/**
	 * @param req
	 */
	public RPUserVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public RPUserVO(ResultSet rs) {
		super(rs);
	}

	/*
	 * (non-Javadoc)
	 * @see com.siliconmtn.data.parser.BeanDataVO#toString()
	 */
	@Override
	public String toString() {
		return StringUtil.getToString(this);
	}
	/**
	 * @return the driverLicense
	 */
	@Column(name="driver_license_txt")
	public String getDriverLicense() {
		return driverLicense;
	}

	/**
	 * @return the driverLicensePath
	 */
	@Column(name="driver_license_path")
	public String getDriverLicensePath() {
		return driverLicensePath;
	}

	/**
	 * @param driverLicense the driverLicense to set
	 */
	public void setDriverLicense(String driverLicense) {
		this.driverLicense = driverLicense;
	}

	/**
	 * @param driverLicensePath the driverLicensePath to set
	 */
	public void setDriverLicensePath(String driverLicensePath) {
		this.driverLicensePath = driverLicensePath;
	}

}

