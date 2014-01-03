package com.fastsigns.action.approval.vo;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
/****************************************************************************
 * <b>Title</b>: WhiteBoardLogVO.java
 * <p/>
 * <b>Project</b>: SB_FastSigns
 * <p/>
 * <b>Description: </b> ChangeLog used for handling WhiteBoard Requests. 
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
public class WhiteBoardLogVO extends ChangeLogVO {
	public final static String TYPE_ID = "ctrWhiteBrd";
	public final static String WHITE_BOARD_TEXT = "NEW_WHITE_BOARD_TEXT";
	public static final String FRIENDLY_NAME = "Center White Board";

	public WhiteBoardLogVO(){
		
	}
	
	public WhiteBoardLogVO(SMTServletRequest req) {
		super(req);
	}

	public WhiteBoardLogVO(ResultSet rs) throws SQLException {
		super(rs);
	}

	@Override
	public String getActionClassPath() {
		return "com.fastsigns.action.approval.WhiteBoardApprovalAction";
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
		setModName(db.getStringVal(WHITE_BOARD_TEXT, rs));
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
