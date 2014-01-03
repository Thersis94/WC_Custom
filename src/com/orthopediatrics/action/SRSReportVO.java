package com.orthopediatrics.action;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.admin.action.ModuleAction;

/****************************************************************************
 * <b>Title</b>: SRSReportVO.java <p/>
 * <b>Project</b>: SB_Orthopediatrics <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Jul 7, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class SRSReportVO extends SBModuleVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	// Collection of elements for the report
	List<SRSFieldVO> fields = new ArrayList<SRSFieldVO>();
	
	/**
	 * 
	 */
	public SRSReportVO() {
		
	}

	public SRSReportVO(ResultSet rs) {
		this.assignData(rs);
	}
	
	public SRSReportVO(SMTServletRequest req) {
		this.assignData(req);
	}
	
	/**
	 * 
	 * @param rs
	 */
	public void assignData(ResultSet rs) {
		DBUtil db = new DBUtil();
		
		actionId = db.getStringVal("action_id", rs);
		actionName = db.getStringVal("action_nm", rs);
		actionDesc = db.getStringVal("action_desc", rs);
		introText = db.getStringVal("intro_txt", rs);
		this.setAttribute(ATTRIBUTE_1, db.getStringVal("attrib1_txt", rs));
		
		if (StringUtil.checkVal(db.getStringVal("field_id", rs)).length() > 0) {
			fields.add(new SRSFieldVO(rs));
		}
	}
	
	/**
	 * 
	 * @param req
	 */
	public void assignData(SMTServletRequest req) {
		actionId = req.getParameter(ModuleAction.SB_ACTION_ID);
		actionName = req.getParameter("actionName");
		actionDesc = req.getParameter("actionDesc");
		
		if (StringUtil.checkVal(req.getParameter("fieldId")).length() > 0) {
			fields.add(new SRSFieldVO(req));
		}
	}
	
	// Adds an entry to the collection of fields
	public void addField(SRSFieldVO vo) {
		fields.add(vo);
	}
	
	/**
	 * @return the fields
	 */
	public List<SRSFieldVO> getFields() {
		return fields;
	}

	/**
	 * @param fields the fields to set
	 */
	public void setFields(List<SRSFieldVO> fields) {
		this.fields = fields;
	}
}
