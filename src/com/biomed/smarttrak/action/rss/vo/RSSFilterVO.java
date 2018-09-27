package com.biomed.smarttrak.action.rss.vo;

import java.sql.ResultSet;
import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title:</b> RSSFilterVO.java
 * <b>Project:</b> WebCrescendo
 * <b>Description:</b> Holds Filter Information for an RSS Entity.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Apr 26, 2017
 ****************************************************************************/
@Table(name="BIOMEDGPS_RSS_PARSER_FILTER")
public class RSSFilterVO extends BeanDataVO {

	/**
	 *
	 */
	private static final long serialVersionUID = 422029523131408569L;
	private String filterId;
	private String filterNm;
	private String typeCd;
	private String filterExpression;
	private String filterGroupXrId;
	private Date createDt;
	private Date updateDt;

	public RSSFilterVO() {
		//Empty constructor
	}

	public RSSFilterVO(ActionRequest req) {
		this();
		populateData(req);
	}

	public RSSFilterVO(ResultSet rs) {
		this();
		populateData(rs);
	}

	/**
	 * @return the filterId
	 */
	@Column(name="filter_id", isPrimaryKey=true)
	public String getFilterId() {
		return filterId;
	}

	/**
	 * @return the filterNm
	 */
	@Column(name="filter_nm")
	public String getFilterNm() {
		return filterNm;
	}

	/**
	 * @return the typeCd
	 */
	@Column(name="filter_type_cd")
	public String getTypeCd() {
		return typeCd;
	}

	/**
	 * @return the filterExpression
	 */
	@Column(name="filter_expression")
	public String getFilterExpression() {
		return filterExpression;
	}

	/**
	 * @return the createDt
	 */
	@Column(name="create_dt", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDt() {
		return createDt;
	}

	/**
	 * @return the updateDt
	 */
	@Column(name="update_dt", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDt() {
		return updateDt;
	}

	@Column(name="feed_filter_group_xr_id", isReadOnly=true)
	public String getFilterGroupXrId() {
		return filterGroupXrId;
	}

	/**
	 * @param filterId the filterId to set.
	 */
	public void setFilterId(String filterId) {
		this.filterId = filterId;
	}

	/**
	 * @param filterNm the filterNm to set.
	 */
	public void setFilterNm(String filterNm) {
		this.filterNm = filterNm;
	}

	/**
	 * @param typeCd the typeCd to set.
	 */
	public void setTypeCd(String typeCd) {
		this.typeCd = typeCd;
	}

	/**
	 * @param filterExpression the filterExpression to set.
	 */
	public void setFilterExpression(String filterExpression) {
		this.filterExpression = filterExpression;
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

	public void setFilterGroupXrId(String filterGroupXrId) {
		this.filterGroupXrId = filterGroupXrId;
	}
}