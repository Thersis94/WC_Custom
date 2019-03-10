package com.perfectstorm.data;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: TimezoneVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object for a time zone element
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 11, 2019
 * @updates:
 ****************************************************************************/
@Table(name="timezone")
public class TimezoneVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9095781094078263285L;

	// Members
	private String timezoneCode;
	private String countryCode;
	private String name;
	private double offset;
	private double dstOffset;
	private Date createDate;
	
	/**
	 * 
	 */
	public TimezoneVO() {
		super();
	}

	/**
	 * @param req
	 */
	public TimezoneVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public TimezoneVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the timezoneCode
	 */
	@Column(name="timezone_cd")
	public String getTimezoneCode() {
		return timezoneCode;
	}

	/**
	 * @return the countryCode
	 */
	@Column(name="country_cd")
	public String getCountryCode() {
		return countryCode;
	}

	/**
	 * @return the name
	 */
	@Column(name="zone_nm")
	public String getName() {
		return name;
	}

	/**
	 * @return the offset
	 */
	@Column(name="offset_no")
	public double getOffset() {
		return offset;
	}

	/**
	 * @return the dstOffset
	 */
	@Column(name="dst_offset_no")
	public double getDstOffset() {
		return dstOffset;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param timezoneCode the timezoneCode to set
	 */
	public void setTimezoneCode(String timezoneCode) {
		this.timezoneCode = timezoneCode;
	}

	/**
	 * @param countryCode the countryCode to set
	 */
	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param offset the offset to set
	 */
	public void setOffset(double offset) {
		this.offset = offset;
	}

	/**
	 * @param dstOffset the dstOffset to set
	 */
	public void setDstOffset(double dstOffset) {
		this.dstOffset = dstOffset;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

}

