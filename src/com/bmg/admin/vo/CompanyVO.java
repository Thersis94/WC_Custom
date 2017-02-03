package com.bmg.admin.vo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.biomed.smarttrak.vo.NoteVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: CompanyVO.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> DBProcessor enabled VO that stores all information about
 * a company, including investors, alliances, and locations.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Jan 16, 2017<p/>
 * <b>Changes: </b>
 ****************************************************************************/

@Table(name="BIOMEDGPS_COMPANY")
public class CompanyVO  implements NoteEntityInterface {
	private String companyId;
	private String parentId;
	private String companyName;
	private String shortName;
	private String aliasName;
	private String currencyTypeId;
	private String holdingText;
	private String stockAbbr;
	private String exchangeId;
	private String archiveReason;
	private String fiscalYearEnd;
	private int startupFlag;
	private String statusNo;
	private double revenueNo;
	private int revenueYear;
	private int foundedYear;
	private int completionScore;
	private int productNo;
	private int profileNo;
	private int peopleNo;
	private int investedFlag;
	private List<String> investors;
	private List<LocationVO> locations;
	private List<AllianceVO> alliances;
	private List<NoteInterface> attributes;
	private List<NoteVO> notes;
	
	
	public CompanyVO() {
		investors = new ArrayList<>();
		locations = new ArrayList<>();
		alliances = new ArrayList<>();
		attributes = new ArrayList<>();
	}
	
	public CompanyVO(ActionRequest req) {
		this();
		setData(req);
	}
	
	public void setData(ActionRequest req) {
		companyId = req.getParameter("companyId");
		parentId = StringUtil.checkVal(req.getParameter("parentId"), null);
		companyName = req.getParameter("companyName");
		shortName = req.getParameter("shortName");
		aliasName = req.getParameter("aliasName");
		currencyTypeId = req.getParameter("currencyId");
		holdingText = req.getParameter("holdingText");
		stockAbbr = req.getParameter("stockAbbr");
		exchangeId = req.getParameter("exchangeId");
		archiveReason = req.getParameter("archiveReason");
		startupFlag = Convert.formatInteger(req.getParameter("startupFlag"));
		statusNo = req.getParameter("statusNo");
		revenueNo = Convert.formatDouble(req.getParameter("revenueNo"));
		revenueYear = Convert.formatInteger(req.getParameter("revenueYear"));
		foundedYear = Convert.formatInteger(req.getParameter("foundedYear"));
		completionScore = Convert.formatInteger(req.getParameter("completionScore"));
		productNo = Convert.formatInteger(req.getParameter("productNo"));
		profileNo = Convert.formatInteger(req.getParameter("profileNo"));
		peopleNo = Convert.formatInteger(req.getParameter("peopleNo"));
		fiscalYearEnd = req.getParameter("fiscalYearEnd");
		if (req.hasParameter("investors")) {
			for (String s : req.getParameterValues("investors")) {
				investors.add(s);
			}
		}
	}
	
	@Column(name="company_id", isPrimaryKey=true)
	public String getCompanyId() {
		return companyId;
	}
	public void setCompanyId(String companyId) {
		this.companyId = companyId;
	}
	@Column(name="parent_id")
	public String getParentId() {
		return parentId;
	}
	public void setParentId(String parentId) {
		this.parentId = parentId;
	}
	@Column(name="company_nm")
	public String getCompanyName() {
		return companyName;
	}
	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}
	@Column(name="short_nm_txt")
	public String getShortName() {
		return shortName;
	}
	public void setShortName(String shortName) {
		this.shortName = shortName;
	}
	@Column(name="alias_nm")
	public String getAliasName() {
		return aliasName;
	}
	public void setAliasName(String aliasName) {
		this.aliasName = aliasName;
	}
	@Column(name="currency_type_id")
	public String getCurrencyTypeId() {
		return currencyTypeId;
	}
	public void setCurrencyTypeId(String currencyTypeId) {
		this.currencyTypeId = currencyTypeId;
	}
	@Column(name="holding_txt")
	public String getHoldingText() {
		return holdingText;
	}
	public void setHoldingText(String holdingText) {
		this.holdingText = holdingText;
	}
	@Column(name="stock_abbr_txt")
	public String getStockAbbr() {
		return stockAbbr;
	}
	public void setStockAbbr(String stockAbbr) {
		this.stockAbbr = stockAbbr;
	}
	@Column(name="exchange_id")
	public String getExchangeId() {
		return exchangeId;
	}
	public void setExchangeId(String exchangeId) {
		this.exchangeId = exchangeId;
	}
	@Column(name="archive_reason_txt")
	public String getArchiveReason() {
		return archiveReason;
	}
	public void setArchiveReason(String archiveReason) {
		this.archiveReason = archiveReason;
	}
	@Column(name="fiscal_yr_end_mon")
	public String getFiscalYearEnd() {
		return fiscalYearEnd;
	}
	public void setFiscalYearEnd(String fiscalYearEnd) {
		this.fiscalYearEnd = fiscalYearEnd;
	}
	@Column(name="status_no")
	public String getStatusNo() {
		return statusNo;
	}
	public void setStatusNo(String statusNo) {
		this.statusNo = statusNo;
	}
	@Column(name="revenue_no")
	public double getRevenueNo() {
		return revenueNo;
	}
	public int getRevenueYear() {
		return revenueYear;
	}

	public void setRevenueYear(int revenueYear) {
		this.revenueYear = revenueYear;
	}

	public void setRevenueNo(double revenueNo) {
		this.revenueNo = revenueNo;
	}
	@Column(name="startup_flg")
	public int getStartupFlag() {
		return startupFlag;
	}
	public void setStartupFlag(int startupFlag) {
		this.startupFlag = startupFlag;
	}
	@Column(name="founded_yr")
	public int getFoundedYear() {
		return foundedYear;
	}
	public void setFoundedYear(int foundedYear) {
		this.foundedYear = foundedYear;
	}
	@Column(name="completion_score_no")
	public int getCompletionScore() {
		return completionScore;
	}
	public void setCompletionScore(int completionScore) {
		this.completionScore = completionScore;
	}
	@Column(name="product_no")
	public int getProductNo() {
		return productNo;
	}
	public void setProductNo(int productNo) {
		this.productNo = productNo;
	}
	@Column(name="profile_no")
	public int getProfileNo() {
		return profileNo;
	}
	public void setProfileNo(int profileNo) {
		this.profileNo = profileNo;
	}
	@Column(name="people_no")
	public int getPeopleNo() {
		return peopleNo;
	}
	public void setPeopleNo(int peopleNo) {
		this.peopleNo = peopleNo;
	}
	@Column(name="invested_flg", isReadOnly=true)
	public int getInvestedFlag() {
		return investedFlag;
	}

	public void setInvestedFlag(int investedFlag) {
		this.investedFlag = investedFlag;
	}

	public List<String> getInvestors() {
		return investors;
	}

	public void setInvestors(List<String> investors) {
		this.investors = investors;
	}
	
	public void addInvestor(String investor) {
		investors.add(investor);
	}

	public List<LocationVO> getLocations() {
		return locations;
	}

	public void setLocations(List<LocationVO> locations) {
		this.locations = locations;
	}
	
	public void addLocation(LocationVO location) {
		this.locations.add(location);
	}

	public List<AllianceVO> getAlliances() {
		return alliances;
	}

	public void setAlliances(List<AllianceVO> alliances) {
		this.alliances = alliances;
	}
	
	public void addAlliance(AllianceVO alliance) {
		this.alliances.add(alliance);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.bmg.admin.vo.NoteEntityInterface#getAttributes()
	 */
	@Override
	public List<NoteInterface> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<NoteInterface> attributes) {
		this.attributes = attributes;
	}
	
	public void addAttribute(CompanyAttributeVO attribute) {
		this.attributes.add(attribute);
	}
	

	// These functions exists only to give the DBProcessor a hook to autogenerate dates on
	@Column(name="UPDATE_DT", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDate() {return null;}
	@Column(name="CREATE_DT", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDate() {return null;}

	/* (non-Javadoc)
	 * @see com.bmg.admin.vo.BiomedNoteInterface#setNotes(java.util.List)
	 */
	@Override
	public void setNotes(List<NoteVO> notes) {
		this.notes = notes;
	}
	
	/*
	 * returns the list of notes
	 */
	public List<NoteVO> getNotes(){
		return notes;
	}

	/* (non-Javadoc)
	 * @see com.bmg.admin.vo.BiomedNoteInterface#getId()
	 */
	@Override
	public String getId() {
		//each vo will return its own primary id.
		return getCompanyId();
	}

}
