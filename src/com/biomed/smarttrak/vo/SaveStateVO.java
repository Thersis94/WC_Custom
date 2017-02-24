/**
 *
 */
package com.biomed.smarttrak.vo;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.Date;

import org.apache.commons.lang.StringEscapeUtils;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.http.session.SMTSession;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: SaveStateVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> VO Holds Gap Analysis Save State Data.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Feb 12, 2017
 ****************************************************************************/
@Table(name="biomedgps_ga_savestate")
public class SaveStateVO implements Serializable {

	private static final long serialVersionUID = 1L;
	private String saveStateId;
	private String userId;
	private String layoutNm;
	private int orderNo;
	private String slugTxt;
	private String saveData;
	private Date createDt;
	private Date updateDt;

	public SaveStateVO() {
		//Empty.
	}

	public SaveStateVO(ResultSet rs) {
		this.setData(rs);
	}

	public SaveStateVO(ActionRequest req) {
		this.setData(req);
	}

	protected void setData(ResultSet rs) {
		DBUtil db = new DBUtil();
		this.saveStateId = db.getStringVal("save_state_id", rs);
		this.userId = db.getStringVal("user_id", rs);
		this.layoutNm = db.getStringVal("layout_nm", rs);
		this.orderNo = db.getIntVal("order_no", rs);
		this.slugTxt = db.getStringVal("slug_txt", rs);
		this.saveData = new String(db.getBinaryVal("object_blob", rs));
	}

	protected void setData(ActionRequest req) {
		SMTSession ses = req.getSession();
		UserVO vo = (UserVO) ses.getAttribute(Constants.USER_DATA);
		if(vo != null) {
			this.userId = StringUtil.checkVal(vo.getUserId());
		}

		this.saveStateId = req.getParameter("saveStateId");
		this.layoutNm = req.getParameter("layoutNm");
		this.orderNo = Convert.formatInteger(req.getParameter("orderNo"));
		this.saveData = StringEscapeUtils.unescapeHtml(req.getParameter("saveData"));
		this.slugTxt = req.getParameter("slugTxt");

	}

	/**
	 * @return the saveStateId
	 */
	@Column(name="save_state_id", isPrimaryKey=true)
	public String getSaveStateId() {
		return saveStateId;
	}

	/**
	 * @return the userId
	 */
	@Column(name="user_id")
	public String getUserId() {
		return userId;
	}

	/**
	 * @return the layoutNm
	 */
	@Column(name="layout_nm")
	public String getLayoutNm() {
		return layoutNm;
	}

	/**
	 * @return the orderNo
	 */
	@Column(name="order_no")
	public int getOrderNo() {
		return orderNo;
	}

	/**
	 * @return the slugTxt
	 */
	@Column(name="slug_txt")
	public String getSlugTxt() {
		return slugTxt;
	}

	@Column(name="object_blob")
	public byte [] getObjectBloc() {
		return saveData.getBytes();
	}
	/**
	 * @return the objectBlob
	 */
	public String getSaveData() {
		return saveData;
	}

	/**
	 * 
	 * @return the createDt
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDt() {
		return createDt;
	}

	/**
	 * 
	 * @return the updateDt
	 */
	@Column(name="update_dt", isUpdateOnly=true, isAutoGen=true)
	public Date getUpdateDt() {
		return updateDt;
	}

	/**
	 * @param saveStateId the saveStateId to set.
	 */
	public void setSaveStateId(String saveStateId) {
		this.saveStateId = saveStateId;
	}

	/**
	 * @param userId the userId to set.
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/**
	 * @param layoutNm the layoutNm to set.
	 */
	public void setLayoutNm(String layoutNm) {
		this.layoutNm = layoutNm;
	}

	/**
	 * @param orderNo the orderNo to set.
	 */
	public void setOrderNo(int orderNo) {
		this.orderNo = orderNo;
	}

	/**
	 * @param slugTxt the slugTxt to set.
	 */
	public void setSlugTxt(String slugTxt) {
		this.slugTxt = slugTxt;
	}

	/**
	 * @param objectBlob the objectBlob to set.
	 */
	public void setSaveData(String saveData) {
		this.saveData = saveData;
	}

	
}