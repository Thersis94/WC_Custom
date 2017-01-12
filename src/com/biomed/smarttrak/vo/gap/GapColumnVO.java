/**
 *
 */
package com.biomed.smarttrak.vo.gap;

import java.sql.ResultSet;
import java.util.Date;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: GapColumnVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Gap Analysis Column VO
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Jan 12, 2017
 ****************************************************************************/
@Table(name="BIOMEDGPS_GA_COLUMN")
public class GapColumnVO {

	private String gaColumnId;
	private String gaMarketId;
	private String columnNm;
	private int orderNo;
	private Date createDt;
	private Date updateDt;

	public GapColumnVO() {}
	public GapColumnVO(SMTServletRequest req) {
		setData(req);
	}
	public GapColumnVO(ResultSet rs) {
		setData(rs);
	}

	public void setData(SMTServletRequest req) {
		this.gaColumnId = req.getParameter("gaColumnId");
		this.gaMarketId = req.getParameter("gaMarketId");
		this.columnNm = req.getParameter("columnNm");
		this.orderNo = Convert.formatInteger(req.getParameter("orderNo"));
	}

	public void setData(ResultSet rs) {
		DBUtil db = new DBUtil();
		this.gaColumnId = db.getStringVal("GA_COLUMN_ID", rs);
		this.gaMarketId = db.getStringVal("GA_MARKET_ID", rs);
		this.columnNm = db.getStringVal("COLUMN_NM", rs);
		this.orderNo = db.getIntVal("ORDER_NO", rs);
	}
	/**
	 * @return the gaColumnId
	 */
	@Column(name="GA_COLUMN_ID", isPrimaryKey=true)
	public String getGaColumnId() {
		return gaColumnId;
	}
	/**
	 * @return the gaMarketId
	 */
	@Column(name="GA_MARKET_ID")
	public String getGaMarketId() {
		return gaMarketId;
	}
	/**
	 * @return the columnNm
	 */
	@Column(name="COLUMN_NM")
	public String getColumnNm() {
		return columnNm;
	}
	/**
	 * @return the orderNo
	 */
	@Column(name="ORDER_NO")
	public int getOrderNo() {
		return orderNo;
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
	 * @param gaColumnId the gaColumnId to set.
	 */
	public void setGaColumnId(String gaColumnId) {
		this.gaColumnId = gaColumnId;
	}
	/**
	 * @param gaMarketId the gaMarketId to set.
	 */
	public void setGaMarketId(String gaMarketId) {
		this.gaMarketId = gaMarketId;
	}
	/**
	 * @param columnNm the columnNm to set.
	 */
	public void setColumnNm(String columnNm) {
		this.columnNm = columnNm;
	}
	/**
	 * @param orderNo the orderNo to set.
	 */
	public void setOrderNo(int orderNo) {
		this.orderNo = orderNo;
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