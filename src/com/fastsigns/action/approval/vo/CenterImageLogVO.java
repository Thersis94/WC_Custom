package com.fastsigns.action.approval.vo;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
/****************************************************************************
 * <b>Title</b>: CenterImageLogVO.java
 * <p/>
 * <b>Project</b>: SB_FastSigns
 * <p/>
 * <b>Description: </b> ChangeLog used for handling Franchise Image Requests. 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Sept. 20, 2012
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public class CenterImageLogVO extends ChangeLogVO {
	public final static String TYPE_ID = "ctrImage";
	public final static String NAME_TXT = "NEW_CENTER_IMAGE_URL";
	public static final String FRIENDLY_NAME = "Center Image";

	public CenterImageLogVO(){
		
	}
	
	public CenterImageLogVO(SMTServletRequest req) {
		super(req);
	}

	public CenterImageLogVO(ResultSet rs) throws SQLException {
		super(rs);
	}

	@Override
	public String getActionClassPath() {
		return "com.fastsigns.action.approval.CenterImageApprovalAction";
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
		setModName(db.getStringVal(NAME_TXT, rs));
		setFranchiseId(db.getStringVal(FRANCHISE_ID, rs));
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
