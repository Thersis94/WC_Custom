package com.ram.action.or.vo;

import java.sql.ResultSet;

import com.ram.datafeed.data.RAMProductVO;

/****************************************************************************
 * <b>Title:</b> SPDRAMProductVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> RAMProductVO with additional case Data for when SPD Scan
 * is initiated for a lookup.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Jul 10, 2017
 ****************************************************************************/
public class SPDRAMProductVO extends RAMProductVO {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private String caseId;
	private String caseKitId;

	public SPDRAMProductVO() {
		super();
	}

	public SPDRAMProductVO(ResultSet rs) {
		super(rs);
	}

	public String getCaseId() {
		return caseId;
	}

	public String getCaseKitId() {
		return caseKitId;
	}

	public void setCaseId(String caseId) {
		this.caseId = caseId;
	}

	public void setCaseKitId(String caseKitId) {
		this.caseKitId = caseKitId;
	}
}
