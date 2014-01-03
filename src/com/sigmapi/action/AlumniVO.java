/**
 * 
 */
package com.sigmapi.action;

import java.io.Serializable;
import java.util.Date;

import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.data.vo.FormTransactionVO;

/****************************************************************************
 * <b>Title</b>: AlumniVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Aug 18, 2011
 ****************************************************************************/
public class AlumniVO extends UserDataVO implements Serializable {

	private static final long serialVersionUID = -3410583156068170049L;
	private UserDataVO user = null;
	private FormTransactionVO extData = null;
	private Date lastLoginDate = null;
	
	public AlumniVO() {
	}
	
	public void setUserData(UserDataVO user) {
		super.setData(user.getDataMap());
	}
	
	public void setExtData(FormTransactionVO vo) {
		extData = vo;
	}
	
	public UserDataVO getCoreData() {
		return user;
	}
	
	public FormTransactionVO getExtData() {
		return extData;
	}
	
	public void setLastLoginDate(Date d) {
		lastLoginDate = d;
	}
	public Date getLastLoginDate() {
		return lastLoginDate;
	}
}
