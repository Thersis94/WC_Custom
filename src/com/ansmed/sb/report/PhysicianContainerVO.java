package com.ansmed.sb.report;

import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

import com.ansmed.sb.physician.BusinessPlanVO;
import com.ansmed.sb.physician.SurgeonVO;

/****************************************************************************
 * <b>Title</b>:PhysicianContainerVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2008<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Apr 2, 2008
 ****************************************************************************/
public class PhysicianContainerVO extends SurgeonVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Map<String, BusinessPlanVO> businessPlan = null;
	
	/**
	 * 
	 */
	public PhysicianContainerVO() {
		super();
		businessPlan = new LinkedHashMap<String, BusinessPlanVO>();
	}
	
	/**
	 * 
	 */
	public PhysicianContainerVO(ResultSet rs) {
		super(rs);
		businessPlan = new LinkedHashMap<String, BusinessPlanVO>();
	}
	
	/**
	 * 
	 * @param vo
	 */
	public void addBusinessPlan(BusinessPlanVO vo) {
		if (!businessPlan.containsKey(vo.getBusinessPlanId()))
				businessPlan.put(vo.getBusinessPlanId(), vo);
	}
	
	/**
	 * @return the businessPlan
	 */
	public Map<String, BusinessPlanVO> getBusinessPlan() {
		return businessPlan;
	}

	/**
	 * @param businessPlan the businessPlan to set
	 */
	public void setBusinessPlan(Map<String, BusinessPlanVO> businessPlan) {
		this.businessPlan = businessPlan;
	}
	
	/**
	 * 
	 * @param num
	 * @return
	 */
	private String convertFlag(int num) {
		String val = "";
		switch(num) {
			case 0:
				val = "No";
				break;
			case 1:
				val = "Yes";
				break;
			case 2:
				val = "";
				break;
		}
		
		return val;
	}
	
	// Helper Methods to convert flag fields
	public String getSpanishString() { return convertFlag(this.getSpanishFlag()); }
	public String getFellowshipString() { return convertFlag(this.getFellowshipFlag()); }
	public String getAllowMailString() { return convertFlag(this.getAllowMail()); }
	public String getStatusString() {
		String val = "";
		if (this.getStatusId() == 0) val = "No Locator";
		if (this.getStatusId() == 1) val = "Locator";
		if (this.getStatusId() == 10) val = "Deactivated";
		
		return val;
	}
}
