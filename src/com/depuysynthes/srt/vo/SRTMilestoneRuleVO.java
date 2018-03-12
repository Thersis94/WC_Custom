package com.depuysynthes.srt.vo;

import java.io.Serializable;
import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.smt.sitebuilder.data.vo.GenericQueryVO.Operator;

/****************************************************************************
 * <b>Title:</b> SRTMilestoneRuleVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Stores Milestone Rule Information.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Mar 12, 2018
 ****************************************************************************/
@Table(name="DPY_SYN_SRT_MILESTONE_RULE")
public class SRTMilestoneRuleVO implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private String milestoneRuleId;
	private String milestoneId;
	private String fieldNm;
	private Operator operandType;
	private String fieldVal;
	private Date createDt;

	public SRTMilestoneRuleVO() {
		
	}

	public SRTMilestoneRuleVO(ActionRequest req) {
		
	}

	/**
	 * @return the milestoneRuleId
	 */
	@Column(name="milestone_rule_id", isPrimaryKey=true)
	public String getMilestoneRuleId() {
		return milestoneRuleId;
	}

	/**
	 * @return the milestoneId
	 */
	@Column(name="milestone_id")
	public String getMilestoneId() {
		return milestoneId;
	}

	/**
	 * @return the fieldNm
	 */
	@Column(name="field_nm")
	public String getFieldNm() {
		return fieldNm;
	}

	/**
	 * @return the operandType
	 */
	@Column(name="operand_type")
	public Operator getOperandType() {
		return operandType;
	}

	/**
	 * @return the fieldVal
	 */
	@Column(name="field_val")
	public String getFieldVal() {
		return fieldVal;
	}

	/**
	 * @return the createDt
	 */
	@Column(name="create_dt", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDt() {
		return createDt;
	}

	/**
	 * @param milestoneRuleId the milestoneRuleId to set.
	 */
	public void setMilestoneRuleId(String milestoneRuleId) {
		this.milestoneRuleId = milestoneRuleId;
	}

	/**
	 * @param milestoneId the milestoneId to set.
	 */
	public void setMilestoneId(String milestoneId) {
		this.milestoneId = milestoneId;
	}

	/**
	 * @param fieldNm the fieldNm to set.
	 */
	public void setFieldNm(String fieldNm) {
		this.fieldNm = fieldNm;
	}

	/**
	 * @param operandType the operandType to set.
	 */
	public void setOperandType(Operator operandType) {
		this.operandType = operandType;
	}

	/**
	 * @param fieldVal the fieldVal to set.
	 */
	public void setFieldVal(String fieldVal) {
		this.fieldVal = fieldVal;
	}

	/**
	 * @param createDt the createDt to set.
	 */
	public void setCreateDt(Date createDt) {
		this.createDt = createDt;
	}
}