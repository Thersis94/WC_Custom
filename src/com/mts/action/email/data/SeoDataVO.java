package com.mts.action.email.data;

import java.sql.ResultSet;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title</b>: SeoDataVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> CHANGE ME!!
 * <b>Copyright:</b> Copyright (c) 2020
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jun 8, 2020
 * @updates:
 ****************************************************************************/
public class SeoDataVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6322271782470170659L;
	
	// Members
	private String seoDescription;
	private boolean seoHidden;
	private String recordLabeltype;

	/**
	 * 
	 */
	public SeoDataVO() {
		super();
	}

	/**
	 * @param req
	 */
	public SeoDataVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public SeoDataVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the seoDescription
	 */
	public String getSeoDescription() {
		return seoDescription;
	}

	/**
	 * @return the seoHidden
	 */
	public boolean isSeoHidden() {
		return seoHidden;
	}

	/**
	 * @return the recordLabeltype
	 */
	public String getRecordLabeltype() {
		return recordLabeltype;
	}

	/**
	 * @param seoDescription the seoDescription to set
	 */
	public void setSeoDescription(String seoDescription) {
		this.seoDescription = seoDescription;
	}

	/**
	 * @param seoHidden the seoHidden to set
	 */
	public void setSeoHidden(boolean seoHidden) {
		this.seoHidden = seoHidden;
	}

	/**
	 * @param recordLabeltype the recordLabeltype to set
	 */
	public void setRecordLabeltype(String recordLabeltype) {
		this.recordLabeltype = recordLabeltype;
	}

}
