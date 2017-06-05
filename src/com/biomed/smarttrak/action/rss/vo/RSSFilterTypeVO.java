package com.biomed.smarttrak.action.rss.vo;

import java.io.Serializable;
import java.util.Date;

import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title:</b> RSSTypeVO.java
 * <b>Project:</b> WebCrescendo
 * <b>Description:</b> VO Manages Type Data.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author raptor
 * @version 1.0
 * @since May 3, 2017
 ****************************************************************************/
@Table(name="BIOMEDGPS_RSS_FILTER_TYPE")
public class RSSFilterTypeVO implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -5829777961814013396L;
	private String typeCd;
	private String typeNm;
	private Date createDt;

	public RSSFilterTypeVO() {
		super();
	}

	/**
	 * @return the typeCd
	 */
	@Column(name="filter_type_cd", isPrimaryKey=true)
	public String getTypeCd() {
		return typeCd;
	}

	/**
	 * @return the typeNm
	 */
	@Column(name="filter_type_nm")
	public String getTypeNm() {
		return typeNm;
	}

	/**
	 * @return the createDt
	 */
	@Column(name="create_dt", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDt() {
		return createDt;
	}

	/**
	 * @param typeCd the typeCd to set.
	 */
	public void setTypeCd(String typeCd) {
		this.typeCd = typeCd;
	}

	/**
	 * @param typeNm the typeNm to set.
	 */
	public void setTypeNm(String typeNm) {
		this.typeNm = typeNm;
	}

	/**
	 * @param createDt the createDt to set.
	 */
	public void setCreateDt(Date createDt) {
		this.createDt = createDt;
	}
}
