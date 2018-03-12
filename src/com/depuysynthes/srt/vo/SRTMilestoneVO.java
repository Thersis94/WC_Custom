package com.depuysynthes.srt.vo;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title:</b> SRTMilestoneVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Stores Project Milestone Data.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Feb 23, 2018
 ****************************************************************************/
@Table(name="DPY_SYN_SRT_MILESTONE")
public class SRTMilestoneVO extends BeanDataVO {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private String milestoneId;
	private String opCoId;
	private String parentId;
	private Date createDt;
	private List<SRTMilestoneRuleVO> rules;

	public SRTMilestoneVO() {
		rules = new ArrayList<>();
	}

	public SRTMilestoneVO(ActionRequest req) {
		this();
		populateData(req);
	}

	public SRTMilestoneVO(ResultSet rs) {
		this();
		populateData(rs);
	}

	/**
	 * @return the milestoneId
	 */
	@Column(name="MILESTONE_ID")
	public String getMilestoneId() {
		return milestoneId;
	}

	/**
	 * @return the opCoId
	 */
	@Column(name="OP_CO_ID")
	public String getOpCoId() {
		return opCoId;
	}

	/**
	 * @return the parentId
	 */
	@Column(name="PARENT_ID")
	public String getParentId() {
		return parentId;
	}

	/**
	 * @return the createDt
	 */
	@Column(name="CREATE_DT", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDt() {
		return createDt;
	}

	/**
	 * @param parentId the parentId to set.
	 */
	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	/**
	 * @param opCoId the opCoId to set.
	 */
	public void setOpCoId(String opCoId) {
		this.opCoId = opCoId;
	}

	/**
	 * @param milestoneId the milestoneId to set.
	 */
	public void setMilestoneId(String milestoneId) {
		this.milestoneId = milestoneId;
	}

	/**
	 * @param createDt the createDt to set.
	 */
	public void setCreateDt(Date createDt) {
		this.createDt = createDt;
	}

	/**
	 * @return the rules
	 */
	public List<SRTMilestoneRuleVO> getRules() {
		return rules;
	}

	/**
	 * @param rules the rules to set.
	 */
	public void setRules(List<SRTMilestoneRuleVO> rules) {
		this.rules = rules;
	}

	/**
	 * @param srtMilestoneRuleVO
	 */
	@BeanSubElement
	public void addRule(SRTMilestoneRuleVO rule) {
		if(rule != null && !StringUtil.isEmpty(rule.getMilestoneRuleId())) {
			rules.add(rule);
		}
	}
}