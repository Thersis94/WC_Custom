package com.depuysynthes.srt.vo;

import java.sql.ResultSet;
import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title:</b> SRTOpCoVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Stores SRT Op Co Data.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Sep 21, 2018
 ****************************************************************************/
@Table(name="dpy_syn_srt_op_co")
public class SRTOpCoVO extends BeanDataVO {

	/**
	 *
	 */
	private static final long serialVersionUID = -5549243138341412319L;
	private String opCoId;
	private String opCoNm;
	private String opCoDesc;
	private Date createDt;

	public SRTOpCoVO() {
		super();
	}

	public SRTOpCoVO(ActionRequest req) {
		super(req);
	}

	public SRTOpCoVO(ResultSet rs) {
		super(rs);
	}
	/**
	 * @return the opCoId
	 */
	@Column(name="OP_CO_ID", isPrimaryKey=true)
	public String getOpCoId() {
		return opCoId;
	}

	/**
	 * @return the opCoNm
	 */
	@Column(name="OP_CO_NM")
	public String getOpCoNm() {
		return opCoNm;
	}

	/**
	 * @return the opCoDesc
	 */
	@Column(name="OP_CO_DESC")
	public String getOpCoDesc() {
		return opCoDesc;
	}

	/**
	 * @return the createDt
	 */
	@Column(name="CREATE_DT", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDt() {
		return createDt;
	}

	/**
	 * @param opCoId the opCoId to set.
	 */
	public void setOpCoId(String opCoId) {
		this.opCoId = opCoId;
	}

	/**
	 * @param opCoNm the opCoNm to set.
	 */
	public void setOpCoNm(String opCoNm) {
		this.opCoNm = opCoNm;
	}

	/**
	 * @param opCoDesc the opCoDesc to set.
	 */
	public void setOpCoDesc(String opCoDesc) {
		this.opCoDesc = opCoDesc;
	}

	/**
	 * @param createDt the createDt to set.
	 */
	public void setCreateDt(Date createDt) {
		this.createDt = createDt;
	}
}
