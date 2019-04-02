package com.biomed.smarttrak.vo;

import java.sql.ResultSet;
import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title:</b> MarketArchiveVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Store Generated Market Archive Data.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * @author Billy Larsen
 * @version 3.3.1
 * @since Oct 8, 2018
 ****************************************************************************/
@Table(name="BIOMEDGPS_MARKET_ARCHIVE")
public class MarketArchiveVO extends BeanDataVO{

	/**
	 *
	 */
	private static final long serialVersionUID = -8977714692218096064L;
	private String marketArchiveId;
	private String marketId;
	private String regionCode;
	private String sectionId;
	private String archiveTxt;
	private Date createDt;

	public MarketArchiveVO() {
		super();
	}

	public MarketArchiveVO(String marketId, String archiveTxt) {
		this.marketId = marketId;
		this.archiveTxt = archiveTxt;
	}

	public MarketArchiveVO(String sectionId, String regionCode, String archiveTxt) {
		this.sectionId = sectionId;
		this.regionCode = regionCode;
		this.archiveTxt = archiveTxt;
	}

	public MarketArchiveVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @param req
	 */
	public MarketArchiveVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @return the marketArchiveId
	 */
	@Column(name="MARKET_ARCHIVE_ID", isPrimaryKey=true)
	public String getMarketArchiveId() {
		return marketArchiveId;
	}

	/**
	 * @return the marketId
	 */
	@Column(name="MARKET_ID")
	public String getMarketId() {
		return marketId;
	}

	/**
	 * @return the regionCode
	 */
	@Column(name="REGION_CD")
	public String getRegionCode() {
		return regionCode;
	}

	/**
	 * @return the sectionId
	 */
	@Column(name="SECTION_ID")
	public String getSectionId() {
		return sectionId;
	}

	/**
	 * @return the archiveTxt
	 */
	@Column(name="ARCHIVE_TXT")
	public String getArchiveTxt() {
		return archiveTxt;
	}

	/**
	 * @return the createDt
	 */
	@Column(name="CREATE_DT", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDt() {
		return createDt;
	}

	/**
	 * @param marketArchiveId the marketArchiveId to set.
	 */
	public void setMarketArchiveId(String marketArchiveId) {
		this.marketArchiveId = marketArchiveId;
	}

	/**
	 * @param marketId the marketId to set.
	 */
	public void setMarketId(String marketId) {
		this.marketId = marketId;
	}

	/**
	 * @param regionCode the regionCode to set
	 */
	public void setRegionCode(String regionCode) {
		this.regionCode = regionCode;
	}

	/**
	 * @param sectionId the sectionId to set
	 */
	public void setSectionId(String sectionId) {
		this.sectionId = sectionId;
	}

	/**
	 * @param archiveTxt the archiveTxt to set.
	 */
	public void setArchiveTxt(String archiveTxt) {
		this.archiveTxt = archiveTxt;
	}

	/**
	 * @param createDt the createDt to set.
	 */
	public void setCreateDt(Date createDt) {
		this.createDt = createDt;
	}
}