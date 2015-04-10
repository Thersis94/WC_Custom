package com.fastsigns.action.franchise.vo;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.approval.ApprovalVO;

/****************************************************************************
 * <b>Title</b>: CenterModuleVO.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Nov 22, 2010<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class CenterModuleVO implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	// Member Variables
	private Integer pageLocationId = Integer.valueOf(0);
	private String displayFile = null;
	private String moduleName = null;
	private int numColumns = 0;
	private Map<Integer, CenterModuleOptionVO> moduleOptions = new LinkedHashMap<Integer, CenterModuleOptionVO>();
	private int moduleId = 0;
	private String displayName = null;
	private int moduleLocationId = 0;
	private String locationName = null;
	private boolean isKeystone = false;
	private String moduleDisplayId = null;
	private String moduleLocationXRId = null;
	/**
	 * 
	 */
	public CenterModuleVO() {
		
	}
	
	/**
	 * 
	 */
	public CenterModuleVO(ResultSet rs, boolean isKeystone) {
		this.isKeystone = isKeystone;
		this.assignVals(rs);
	}
	
	/**
	 * 
	 */
	public String toString() {
		return StringUtil.getToString(this);
	}
	
	/**
	 * 
	 * @param rs
	 */
	public void assignVals(ResultSet rs) {
		DBUtil db = new DBUtil();
		pageLocationId = db.getIntegerVal("cp_location_module_xr_id", rs);
		moduleId = db.getIntVal("cp_module_id", rs);
		numColumns = db.getIntegerVal("column_no", rs);
		displayFile = db.getStringVal("display_path_txt", rs);
		if(StringUtil.checkVal(db.getStringVal("display_path_txt_override", rs)).length() > 0)
			displayFile = db.getStringVal("display_path_txt_override", rs);
		moduleName = db.getStringVal("module_nm", rs);
		displayName = db.getStringVal("display_nm", rs);
		moduleLocationId = db.getIntVal("cp_location_id", rs);
		locationName = db.getStringVal("location_nm", rs);
		moduleDisplayId = db.getStringVal("FTS_CP_MODULE_DISPLAY_ID", rs);
		moduleLocationXRId = db.getStringVal("CP_LOCATION_MODULE_XR_ID", rs);
		// Add the options
		CenterModuleOptionVO opt = new CenterModuleOptionVO(rs);
		System.out.println(isKeystone);
		if (isKeystone) opt.setSyncData(new ApprovalVO(rs));
		if (isKeystone || StringUtil.checkVal(StringUtil.checkVal(db.getStringVal("WC_SYNC_STATUS_CD", rs))).length() == 0)
			this.addOption(opt);
	}
	
	
	/**
	 * 
	 * @param cmvo
	 */
	public void addOption(CenterModuleOptionVO cmvo) {

		Integer optId = cmvo.getParentId();
		if (optId == null || optId == 0) optId = cmvo.getModuleOptionId();
		
		if (isKeystone) {
			//determine if the one we already have is newer than the one we're getting
			CenterModuleOptionVO v = moduleOptions.get(optId);
			if (v == null || cmvo.getCreateDate()  == null || (cmvo.getCreateDate().after(v.getCreateDate()))) {
				//System.out.println("put " + cmvo.getModuleOptionId() + " at " + optId);
				moduleOptions.put(optId, cmvo);
				//System.out.println("replaced " + ((v== null) ? "" : v.getOptionName()) + " with " + cmvo.getOptionName());
			}
			else if (cmvo.getAttributes() != null && cmvo.getAttributes().size() > 0) {
				v.addAttribute(cmvo.getAttributes());
			}
		}
		
		if (moduleOptions.containsKey(optId)) {
			//System.out.println("isKY= " + isKeystone + " contains " + cmvo.getOptionName());
			CenterModuleOptionVO opt = moduleOptions.get(optId);
			if (cmvo.getAttributes() != null && cmvo.getAttributes().size() > 0) {
				opt.addAttribute(cmvo.getAttributes());
			}
		} else {
			moduleOptions.put(optId, cmvo);
		}
	}
	
	/**
	 * @return the pageLocationId
	 */
	public Integer getPageLocationId() {
		return pageLocationId;
	}

	/**
	 * @param pageLocationId the pageLocationId to set
	 */
	public void setPageLocationId(Integer pageLocationId) {
		this.pageLocationId = pageLocationId;
	}

	/**
	 * @return the displayFile
	 */
	public String getDisplayFile() {
		return displayFile;
	}

	/**
	 * @param displayFile the displayFile to set
	 */
	public void setDisplayFile(String displayFile) {
		this.displayFile = displayFile;
	}

	/**
	 * @return the moduleName
	 */
	public String getModuleName() {
		return moduleName;
	}

	/**
	 * @param moduleName the moduleName to set
	 */
	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}

	/**
	 * @return the numColumns
	 */
	public int getNumColumns() {
		return numColumns;
	}

	/**
	 * @param numColumns the numColumns to set
	 */
	public void setNumColumns(int numColumns) {
		this.numColumns = numColumns;
	}

	/**
	 * @return the moduleOptions
	 */
	public Map<Integer, CenterModuleOptionVO> getModuleOptions() {
		return moduleOptions;
	}

	/**
	 * @param moduleOptions the moduleOptions to set
	 */
	public void setModuleOptions(Map<Integer, CenterModuleOptionVO> moduleOptions) {
		this.moduleOptions = moduleOptions;
	}

	/**
	 * @return the moduleId
	 */
	public int getModuleId() {
		return moduleId;
	}

	/**
	 * @param moduleId the moduleId to set
	 */
	public void setModuleId(int moduleId) {
		this.moduleId = moduleId;
	}

	/**
	 * @return the displayName
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * @param displayName the displayName to set
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * @return the moduleLocationId
	 */
	public int getModuleLocationId() {
		return moduleLocationId;
	}

	/**
	 * @param moduleLocationId the moduleLocationId to set
	 */
	public void setModuleLocationId(int moduleLocationId) {
		this.moduleLocationId = moduleLocationId;
	}

	/**
	 * @return the locationName
	 */
	public String getLocationName() {
		return locationName;
	}

	/**
	 * @param locationName the locationName to set
	 */
	public void setLocationName(String locationName) {
		this.locationName = locationName;
	}

	public void setModuleDisplayId(String moduleDisplayId) {
		this.moduleDisplayId = moduleDisplayId;
	}

	public String getModuleDisplayId() {
		return moduleDisplayId;
	}

	public void setModuleLocationXRId(String moduleLocationXRId) {
		this.moduleLocationXRId = moduleLocationXRId;
	}

	public String getModuleLocationXRId() {
		return moduleLocationXRId;
	}
}
