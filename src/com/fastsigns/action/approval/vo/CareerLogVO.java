package com.fastsigns.action.approval.vo;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: CareerLogVO.java
 * <p/>
 * <b>Project</b>: SB_FastSigns
 * <p/>
 * <b>Description: </b> ChangeLog used for handling Career Requests. 
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
public class CareerLogVO extends ChangeLogVO {
	public final static String TYPE_ID = "jobPosting";
	public static final String OPTION_NM = "JOB_TITLE_NM";
	public static final String FRIENDLY_NAME = "Job Posting";
	public CareerLogVO(){
		
	}
	
	public CareerLogVO(SMTServletRequest req) {
		super(req);
	}

	public CareerLogVO(ResultSet rs) throws SQLException {
		super(rs);
	}

	@Override
	public String getActionClassPath() {
		return "com.fastsigns.action.approval.CareerApprovalAction";
	}

	public void setData(ResultSet rs) {
		DBUtil db = new DBUtil();
		setFtsChangelogId(db.getStringVal(FTS_CHANGELOG_ID, rs));
		setTypeId(db.getStringVal(TYPE_ID, rs));
		setSubmitterId(db.getStringVal(SUBMITTER_ID, rs));
		setDescTxt(db.getStringVal(DESC_TXT, rs));
		setSubmittedDate(db.getDateVal(SUBMITTED_DT, rs));
		setUpdateDate(db.getDateVal(UPDATE_DT, rs));
		setModDescTxt(db.getStringVal(OPTION_DESC, rs));
		setModName(db.getStringVal(OPTION_NM, rs));
		
		if(StringUtil.checkVal(db.getStringVal(FRANCHISE_ID, rs)).length() > 0)
			setFranchiseId(db.getStringVal(FRANCHISE_ID, rs));
		else
			setFranchiseId("Corporate Post");
	}

	@Override
	public String getHFriendlyType() {
		return FRIENDLY_NAME;
	}
	
	@Override
	public String getTypeId(){
		return TYPE_ID;
	}
}
