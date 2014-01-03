package com.fastsigns.action.approval.vo;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
/****************************************************************************
 * <b>Title</b>: PageModuleLogVO.java
 * <p/>
 * <b>Project</b>: SB_FastSigns
 * <p/>
 * <b>Description: </b> ChangeLog used for handling Page Module Requests. 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Dec. 4, 2012
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public class PageModuleLogVO extends ChangeLogVO {
	private String pageId = "";
	public final static String TYPE_ID = "pageModule";
	public static final String PAGE_ID = "PAGE_ID";
	public static final String SITE_ID = "SITE_ID";
	public static final String FRIENDLY_NAME = "Page Module";

	public PageModuleLogVO(){
		
	}
	
	public PageModuleLogVO(SMTServletRequest req) {
		super(req);
		setPageId(req.getParameter("pageId"));
	}

	public PageModuleLogVO(ResultSet rs) throws SQLException {
		super(rs);
	}

	@Override
	public String getActionClassPath() {
		return "com.fastsigns.action.approval.PageModuleApprovalAction";
	}

	public void setData(ResultSet rs) {
		String t = null;
		DBUtil db = new DBUtil();
		setFtsChangelogId(db.getStringVal(FTS_CHANGELOG_ID, rs));
		setTypeId(db.getStringVal(TYPE_ID, rs));
		setSubmitterId(db.getStringVal(SUBMITTER_ID, rs));
		setDescTxt(db.getStringVal(DESC_TXT, rs));
		setSubmittedDate(db.getDateVal(SUBMITTED_DT, rs));
		setUpdateDate(db.getDateVal(UPDATE_DT, rs));
		setModDescTxt(db.getStringVal(OPTION_DESC, rs));
		setModName(db.getStringVal("ACTION_NM", rs));
		if(StringUtil.checkVal(db.getStringVal(FRANCHISE_ID, rs)).length() > 0)
			t = db.getStringVal(FRANCHISE_ID, rs);
		else
			t = db.getStringVal(SITE_ID, rs);
		if(t != null)
			t = t.substring(0, t.length()-2).replace("_", " ");
		setFranchiseId(t);
		setPageId(db.getStringVal(PAGE_ID, rs));

	}
	
	@Override
	public String getHFriendlyType() {
		return FRIENDLY_NAME;
	}

	/**
	 * @param pageId the pageId to set
	 */
	public void setPageId(String pageId) {
		this.pageId = pageId;
	}

	/**
	 * @return the pageId
	 */
	public String getPageId() {
		return pageId;
	}
	
	@Override
	public String getTypeId(){
		return TYPE_ID;
	}
}
