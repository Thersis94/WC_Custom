package com.fastsigns.action.approval.vo;

import java.sql.ResultSet;

import com.siliconmtn.http.SMTServletRequest;

/****************************************************************************
 * <b>Title</b>: ChangeLogVO.java
 * <p/>
 * <b>Project</b>: SB_FastSigns
 * <p/>
 * <b>Description: </b> Base ChangeLog that can be instantiated.  
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Oct. 9, 2012
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public class ChangeLogVO extends AbstractChangeLogVO {

	public ChangeLogVO(){
		super();
	}
	
	public ChangeLogVO(SMTServletRequest req){
		super(req);
		super.setTypeId(getTypeId());

	}
	
	public ChangeLogVO(ResultSet rs){
		super(rs);
	}
	
	@Override
	public String getActionClassPath() {
		return null;
	}

	@Override
	public String getHFriendlyType() {
		return null;
	}

	@Override
	public void setData(ResultSet rs) {
		
	}
}
