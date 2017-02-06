/**
 *
 */
package com.biomed.smarttrak.admin.vo;

import java.io.Serializable;
import java.sql.ResultSet;

import com.biomed.smarttrak.vo.ProductAttributeTypeVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: GapAttributeVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> VO For managing Gap Analysis Column Attribute XR Data.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Feb 1, 2017
 ****************************************************************************/
@Table(name="biomedgps_ga_column_attribute_xr")
public class GapColumnAttributeVO extends ProductAttributeTypeVO implements Serializable {

	private static final long serialVersionUID = -2155190732531775349L;
	private String columnAttributeXRId;
	private String gaColumnId;

	public GapColumnAttributeVO() {
		super();
	}

	public GapColumnAttributeVO(String attributeId) {
		this();
		setAttributeId(attributeId);
	}

	public GapColumnAttributeVO(ActionRequest req) {
		this();
		setData(req);
	}

	public GapColumnAttributeVO(ResultSet rs) {
		this();
		setData(rs);
	}

	protected void setData(ActionRequest req) {
		super.setData(req);
		columnAttributeXRId = req.getParameter("columnAttributeXRId");
		gaColumnId = req.getParameter("gaColumnId");
	}

	protected void setData(ResultSet rs) {
		super.setData(rs);
		DBUtil db = new DBUtil();
		columnAttributeXRId = db.getStringVal("column_attribute_xr_id", rs);
		gaColumnId = db.getStringVal("ga_column_id", rs);
	}

	/**
	 * @return the columnAttributeXRId
	 */
	@Column(name="column_attribute_xr_id", isPrimaryKey=true)
	public String getColumnAttributeXRId() {
		return columnAttributeXRId;
	}

	/**
	 * @return the gaColumnId
	 */
	@Column(name="ga_column_id")
	public String getGaColumnId() {
		return gaColumnId;
	}

	/**
	 * @param columnAttributeXRId the columnAttributeXRId to set.
	 */
	public void setColumnAttributeXRId(String columnAttributeXRId) {
		this.columnAttributeXRId = columnAttributeXRId;
	}

	/**
	 * @param gaColumnId the gaColumnId to set.
	 */
	public void setGaColumnId(String gaColumnId) {
		this.gaColumnId = gaColumnId;
	}
}