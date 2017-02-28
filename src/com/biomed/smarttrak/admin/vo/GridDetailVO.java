package com.biomed.smarttrak.admin.vo;

// JDK 1.8
import java.sql.ResultSet;
import java.util.Date;

//SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;

/********************************************************************
 * <b>Title: </b>GridDetailVO.java<br/>
 * <b>Description: </b>Manages the detail data for the grids<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author james
 * @version 3.x
 * @since Feb 24, 2017
 * Last Updated:
 * 	
 *******************************************************************/
@Table(name="biomedgps_grid_detail")
public class GridDetailVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	// Member Variables
	private String gridDetailId;
	private String gridId;
	private String detailType;
	private String label;
	private int order;
	private Date updateDate;
	private Date createDate;
	
	// Create a container to hold all values
	private String[] values = new String[10];
	
	/**
	 * 
	 */
	public GridDetailVO() {
		super();
	}
	
	/**
	 * Populates the bean from the request object
	 * @param req
	 */
	public GridDetailVO(ActionRequest req) {
		this.populateData(req);
	}
	
	/**
	 * Populates the bean from the result object
	 * @param req
	 */
	public GridDetailVO(ResultSet rs) {
		this.populateData(rs);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return StringUtil.getToString(this, false, 1, ",");
	}
	
	/**
	 * @return the gridDetailId
	 */
	@Column(name="grid_detail_id", isPrimaryKey=true)
	public String getGridDetailId() {
		return gridDetailId;
	}

	/**
	 * @return the gridType
	 */
	@Column(name="grid_id")
	public String getGridId() {
		return gridId;
	}
	
	/**
	 * @return the label
	 */
	@Column(name="label_txt")
	public String getLabel() {
		return label;
	}

	/**
	 * @return the order
	 */
	@Column(name="order_no")
	public int getOrder() {
		return order;
	}
	
	/**
	 * Returns the first value
	 * @return
	 */
	@Column(name="value_1_txt")
	public String getValue1() {
		return values[0];
	}
	
	/**
	 * Returns the second value
	 * @return
	 */
	@Column(name="value_2_txt")
	public String getValue2() {
		return values[1];
	}
	
	/**
	 * Returns the third value
	 * @return
	 */
	@Column(name="value_3_txt")
	public String getValue3() {
		return values[2];
	}
	
	/**
	 * Returns the fourth value
	 * @return
	 */
	@Column(name="value_4_txt")
	public String getValue4() {
		return values[3];
	}
	
	/**
	 * Returns the fifth value
	 * @return
	 */
	@Column(name="value_5_txt")
	public String getValue5() {
		return values[4];
	}
	
	/**
	 * Returns the sixth value
	 * @return
	 */
	@Column(name="value_6_txt")
	public String getValue6() {
		return values[5];
	}

	/**
	 * Returns the seventh value
	 * @return
	 */
	@Column(name="value_7_txt")
	public String getValue7() {
		return values[6];
	}
	
	/**
	 * Returns the eigth value
	 * @return
	 */
	@Column(name="value_8_txt")
	public String getValue8() {
		return values[7];
	}
	
	/**
	 * Returns the ninth value
	 * @return
	 */
	@Column(name="value_9_txt")
	public String getValue9() {
		return values[8];
	}
	
	/**
	 * Returns the tenth value
	 * @return
	 */
	@Column(name="value_10_txt")
	public String getValue10() {
		return values[9];
	}

	/**
	 * @param gridDetailId the gridDetailId to set
	 */
	public void setGridDetailId(String gridDetailId) {
		this.gridDetailId = gridDetailId;
	}

	/**
	 * @param label the label to set
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * @param order the order to set
	 */
	public void setOrder(int order) {
		this.order = order;
	}
	
	/**
	 * @param value value to set
	 */
	public void setValue1(String value) {
		this.values[0] = value;
	}
	
	/**
	 * @param value value to set
	 */
	public void setValue2(String value) {
		this.values[1] = value;
	}
	
	/**
	 * @param value value to set
	 */
	public void setValue3(String value) {
		this.values[2] = value;
	}
	
	/**
	 * @param value value to set
	 */
	public void setValue4(String value) {
		this.values[3] = value;
	}
	
	/**
	 * @param value value to set
	 */
	public void setValue5(String value) {
		this.values[4] = value;
	}
	
	/**
	 * @param value value to set
	 */
	public void setValue6(String value) {
		this.values[5] = value;
	}
	
	/**
	 * @param value value to set
	 */
	public void setValue7(String value) {
		this.values[6] = value;
	}
	
	/**
	 * @param value value to set
	 */
	public void setValue8(String value) {
		this.values[7] = value;
	}
	
	/**
	 * @param value value to set
	 */
	public void setValue9(String value) {
		this.values[8] = value;
	}
	
	/**
	 * @param value value to set
	 */
	public void setValue10(String value) {
		this.values[9] = value;
	}

	/**
	 * @return the updateDate
	 */
	@Column(name="update_dt", isUpdateOnly=true)
	public Date getUpdateDate() {
		return updateDate;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param gridId the gridId to set
	 */
	public void setGridId(String gridId) {
		this.gridId = gridId;
	}

	/**
	 * @param updateDate the updateDate to set
	 */
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	/**
	 * @return the detailType
	 */
	@Column(name="grid_detail_type_cd")
	public String getDetailType() {
		return detailType;
	}

	/**
	 * @param detailType the detailType to set
	 */
	public void setDetailType(String detailType) {
		this.detailType = detailType;
	}

	/**
	 * @return the values
	 */
	public String[] getValues() {
		return values;
	}

}

