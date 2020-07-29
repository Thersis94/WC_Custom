package com.mts.hootsuite;

//SMT Base Libs
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: HootsuiteRefreshTokenVO.java
 * <b>Project</b>: Hootsuite
 * <b>Description: </b> VO for capturing the response from the database with the current refresh token
 * <b>Copyright:</b> Copyright (c) 2020
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author justinjeffrey
 * @version 3.0
 * @since May 18, 2020
 * @updates:
 ****************************************************************************/
@Table(name="schedule_job_instance_data")
public class HootsuiteRefreshTokenVO extends BeanDataVO {

	String refreshToken;

	/**
	 * @return the refreshToken
	 */
	@Column(name="value_txt")
	public String getRefreshToken() {
		return refreshToken;
	}

	/**
	 * @param refreshToken the refreshToken to set
	 */
	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}
	
}
