package com.depuysynthes.gfp;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.databean.FilePartDataBean;

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
	private String filePath;
	private FilePartDataBean file;
	private int sequenceNo;
	private int activeFlg;
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
		setFilePath(req.getParameter("filePath"));
		setFile(req.getFile("newFile"));
		setSequenceNo(Convert.formatInteger(req.getParameter("sequenceNo")));
		setActiveFlg(Convert.formatInteger(req.getParameter("activeFlg")));
	}
	
	public GFPWorkshopVO(ResultSet rs) {
		this();
		assignData(rs);
	}
	
	public void assignData(ResultSet rs) {
		DBUtil db = new DBUtil();
		setWorkshopId(db.getStringVal("WORKSHOP_ID", rs));
		setParentId(db.getStringVal("PROGRAM_ID", rs));
		setName(db.getStringVal("WORKSHOP_NM", rs));
		setDesc(db.getStringVal("WORKSHOP_DESC", rs));
		setShortDesc(db.getStringVal("SHORT_DESC", rs));
		setFilePath(db.getStringVal("THUMBNAIL_PATH", rs));
		setSequenceNo(db.getIntegerVal("SEQUENCE_NO", rs));
		setActiveFlg(db.getIntegerVal("ACTIVE_FLG", rs));
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

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public FilePartDataBean getFile() {
		return file;
	}

	public void setFile(FilePartDataBean file) {
		this.file = file;
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
	
	public int getCompleteState() {
		int completed = 0;
		for (GFPResourceVO resource : resources) {
			if (resource.isComplete())
				completed++;
		}
		
		if (completed == 0) {
			return 0;
		} else if (completed < resources.size()) {
			return 1;
		} else {
			return 2;
		}
	}
	
	public Date mostRecentCompletion() {
		Date d = null;
		for (GFPResourceVO resource : resources) {
			if (d == null && resource.getCompleteDate() != null)
				d = resource.getCompleteDate();
			
			if (d.before(resource.getCompleteDate())) {
				d = resource.getCompleteDate();
			}
		}
		
		return d;
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
