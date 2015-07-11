package com.depuysynthes.gfp;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: GFPWorkshopVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Contains all information for a GFP workshop, including 
 * a list of resources associated with this workshop.
 * <b>Copyright:</b> Copyright (c) 2015
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Eric Damschroder
 * @version 1.0
 * @since July 6, 2015
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/

public class GFPWorkshopVO {
	private String workshopId;
	private String parentId;
	private String name;
	private String desc;
	private String shortDesc;
	private int sequenceNo;
	private int activeFlg;
	private int completeFlg;
	private List<GFPResourceVO> resources;

	public GFPWorkshopVO() {
		setResources(new ArrayList<GFPResourceVO>());
	}
	
	public GFPWorkshopVO(SMTServletRequest req) {
		this();
		assignData(req);
	}
	
	public void assignData(SMTServletRequest req) {
		setWorkshopId(req.getParameter("workshopId"));
		setParentId(req.getParameter("parentId"));
		setName(req.getParameter("workshopName"));
		setDesc(req.getParameter("workshopDesc"));
		setShortDesc(req.getParameter("shortDesc"));
		setSequenceNo(Convert.formatInteger(req.getParameter("sequenceNo")));
		setActiveFlg(Convert.formatInteger(req.getParameter("activeFlg")));
		setCompleteFlg(Convert.formatInteger(req.getParameter("completeFlg")));
	}
	
	public GFPWorkshopVO(ResultSet rs) {
		this();
		assignData(rs);
	}
	
	public void assignData(ResultSet rs) {
		DBUtil db = new DBUtil();
		setWorkshopId(db.getStringVal("WORKSHOP_ID", rs));
		setParentId(db.getStringVal("PROGRAM_ID", rs));
		setName(db.getStringVal("PROGRAM_NM", rs));
		setDesc(db.getStringVal("PROGRAM_NM", rs));
		setShortDesc(db.getStringVal("PROGRAM_NM", rs));
		setSequenceNo(db.getIntegerVal("SEQUENCE_NO", rs));
		setActiveFlg(db.getIntegerVal("ACTIVE_FLG", rs));
		setCompleteFlg(db.getIntegerVal("COMPLETE_FLG", rs));
		db = null;
	}

	public String getWorkshopId() {
		return workshopId;
	}

	public void setWorkshopId(String workshopId) {
		this.workshopId = workshopId;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getShortDesc() {
		return shortDesc;
	}

	public void setShortDesc(String shortDesc) {
		this.shortDesc = shortDesc;
	}

	public int getSequenceNo() {
		return sequenceNo;
	}

	public void setSequenceNo(int sequenceNo) {
		this.sequenceNo = sequenceNo;
	}

	public int getActiveFlg() {
		return activeFlg;
	}
	
	public boolean isActive() {
		return Convert.formatBoolean(activeFlg);
	}

	public void setActiveFlg(int activeFlg) {
		this.activeFlg = activeFlg;
	}

	public int getCompleteFlg() {
		return completeFlg;
	}

	public void setCompleteFlg(int completeFlg) {
		this.completeFlg = completeFlg;
	}

	public List<GFPResourceVO> getResources() {
		return resources;
	}

	public void setResources(List<GFPResourceVO> resources) {
		this.resources = resources;
	}
	
	public void addResource(GFPResourceVO resource) {
		resources.add(resource);
	}
}
