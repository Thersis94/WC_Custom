package com.depuysynthes.gfp;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;

/****************************************************************************
 * <b>Title</b>: GFPProgramVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Contains all information for a GFP program, including 
 * all associated workshops and resources
 * <b>Copyright:</b> Copyright (c) 2015
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Eric Damschroder
 * @version 1.0
 * @since July 6, 2015
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/

public class GFPProgramVO {

	private String programId;
	private String programName;
	private List<GFPWorkshopVO> workshops;
	private List<GFPResourceVO> resources;
	private String userId;
	
	
	public GFPProgramVO() {
		workshops = new ArrayList<>();
		resources = new ArrayList<>();
	}
	
	public GFPProgramVO(SMTServletRequest req) {
		this();
		assignData(req);
	}
	
	public void assignData(SMTServletRequest req) {
		programName = req.getParameter("programName");
		programId = req.getParameter("programId");
		userId = req.getParameter("userId");
	}
	
	public GFPProgramVO(ResultSet rs) {
		this();
		assignData(rs);
	}
	
	public void assignData(ResultSet rs) {
		DBUtil db = new DBUtil();
		programName = db.getStringVal("PROGRAM_NM", rs);
		programId = db.getStringVal("PROGRAM_ID", rs);
		userId = db.getStringVal("USER_ID", rs);
		db = null;
	}
	
	public String getProgramId() {
		return programId;
	}

	public void setProgramId(String programId) {
		this.programId = programId;
	}

	public String getProgramName() {
		return programName;
	}
	
	public void setProgramName(String programName) {
		this.programName = programName;
	}
	
	public List<GFPWorkshopVO> getWorkshops() {
		return workshops;
	}
	
	public void setWorkshops(List<GFPWorkshopVO> workshops) {
		this.workshops = workshops;
	}
	
	public void addWorkshop(GFPWorkshopVO workshop) {
		workshops.add(workshop);
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

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	public int getCompleted() {
		int completed = 0;
		for (GFPWorkshopVO workshop : workshops) {
			if (workshop.getCompleteState() == 2) {
				completed++;
			}
		}
		return completed;
	}
}
