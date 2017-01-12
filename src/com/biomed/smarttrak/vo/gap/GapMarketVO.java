package com.biomed.smarttrak.vo.gap;
import java.sql.ResultSet;
import java.util.Date;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: GapMarketVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Gap Analysis Market VO
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Jan 12, 2017
 ****************************************************************************/
@Table(name="BIOMEDGPS_GA_MARKET")
public class GapMarketVO {

	private String gaMarketId;
	private String parentId;
	private String marketNm;
	private int orderNo;
	private int sectionFlg;
	private Date createDt;
	private Date updateDt;

	public GapMarketVO(){}
	public GapMarketVO(SMTServletRequest req) {
		setData(req);
	}
	public GapMarketVO(ResultSet rs) {
		setData(rs);
	}

	private void setData(SMTServletRequest req) {
		this.gaMarketId = req.getParameter("gaMarketId");
		this.parentId = req.getParameter("parentId");
		this.marketNm = req.getParameter("marketNm");
		this.orderNo = Convert.formatInteger(req.getParameter("orderNo"));
		this.sectionFlg = Convert.formatInteger(req.getParameter("sectionFlg"));
	}

	private void setData(ResultSet rs) {
		DBUtil db = new DBUtil();
		this.gaMarketId = db.getStringVal("GA_MARKET_ID", rs);
		this.parentId = db.getStringVal("PARENT_ID", rs);
		this.marketNm = db.getStringVal("MARKET_NM", rs);
		this.orderNo = db.getIntVal("ORDER_NO", rs);
		this.sectionFlg = db.getIntVal("SECTION_FLG", rs);
		this.createDt = db.getDateVal("CREATE_DT", rs);
		this.updateDt = db.getDateVal("UPDATE_DT", rs);
	}
	/**
	 * @return the gaMarketId
	 */
	@Column(name="GA_MARKET_ID", isPrimaryKey=true)
	public String getGaMarketId() {
		return gaMarketId;
	}
	/**
	 * @return the parentId
	 */
	@Column(name="PARENT_ID")
	public String getParentId() {
		return parentId;
	}
	/**
	 * @return the marketNm
	 */
	@Column(name="MARKET_ID")
	public String getMarketNm() {
		return marketNm;
	}
	/**
	 * @return the orderNo
	 */
	@Column(name="ORDER_NO")
	public int getOrderNo() {
		return orderNo;
	}
	/**
	 * @return the secionFlg
	 */
	@Column(name="SECTION_FLG")
	public int getSectionFlg() {
		return sectionFlg;
	}
	/**
	 * @return the createDt
	 */
	@Column(name="CREATE_DT", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDt() {
		return createDt;
	}
	/**
	 * @return the updateDt
	 */
	@Column(name="UPDATE_DT", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDt() {
		return updateDt;
	}
	/**
	 * @param gaMarketId the gaMarketId to set.
	 */
	public void setGaMarketId(String gaMarketId) {
		this.gaMarketId = gaMarketId;
	}
	/**
	 * @param parentId the parentId to set.
	 */
	public void setParentId(String parentId) {
		this.parentId = parentId;
	}
	/**
	 * @param marketNm the marketNm to set.
	 */
	public void setMarketNm(String marketNm) {
		this.marketNm = marketNm;
	}
	/**
	 * @param orderNo the orderNo to set.
	 */
	public void setOrderNo(int orderNo) {
		this.orderNo = orderNo;
	}
	/**
	 * @param secionFlg the secionFlg to set.
	 */
	public void setSectionFlg(int sectionFlg) {
		this.sectionFlg = sectionFlg;
	}
	/**
	 * @param createDt the createDt to set.
	 */
	public void setCreateDt(Date createDt) {
		this.createDt = createDt;
	}
	/**
	 * @param updateDt the updateDt to set.
	 */
	public void setUpdateDt(Date updateDt) {
		this.updateDt = updateDt;
	}

	
}
