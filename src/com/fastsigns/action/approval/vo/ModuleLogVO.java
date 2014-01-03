package com.fastsigns.action.approval.vo;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
/****************************************************************************
 * <b>Title</b>: ModuleLogVO.java
 * <p/>
 * <b>Project</b>: SB_FastSigns
 * <p/>
 * <b>Description: </b> ChangeLog used for handling Module Requests. 
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
public class ModuleLogVO extends ChangeLogVO {
	public final static String TYPE_ID = "ctrPgModule";
	public static final String OPTION_NM = "OPTION_NM";
	public static final String FRIENDLY_NAME = "Center Page Module";

	public ModuleLogVO(){
		
	}
	
	public ModuleLogVO(SMTServletRequest req) {
		super(req);
	}

	public ModuleLogVO(ResultSet rs) throws SQLException {
		super(rs);
	}

	@Override
	public String getActionClassPath() {
		return "com.fastsigns.action.approval.ModuleApprovalAction";
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
		setFranchiseId(db.getStringVal("modfranchise_id", rs));
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
