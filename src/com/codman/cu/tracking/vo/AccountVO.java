package com.codman.cu.tracking.vo;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.codman.cu.tracking.AbstractTransAction.Status;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataComparator;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: AccountVO<p/>
 * <b>Description: Data bean for Account</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since Nov 02, 2010
 ****************************************************************************/
public class AccountVO implements java.io.Serializable {
	
	private static final long serialVersionUID = 3155730860990205513L;
	private String accountId = null;
	private String accountNo = null;
	private String accountName = null;
	private String accountAddress = null;
	private String accountAddress2 = null;
	private String accountCity = null;
	private String accountState = null;
	private String accountZipCode = null;
	private String accountCountry = null;
	private String accountPhoneNumber = null;
	private Date createDate = null;
	private String organizationId = null;
	
	private Map<String, PhysicianVO> physicians = new HashMap<String, PhysicianVO>();
	private Map<String, TransactionVO> transactions = new HashMap<String, TransactionVO>();
	private PersonVO rep = new PersonVO();
				
	public AccountVO() {
	}
	
	public AccountVO(SMTServletRequest req) {
		accountId = req.getParameter("accountId");
		accountNo = req.getParameter("accountNo");
		accountName = req.getParameter("accountName");
		accountAddress = req.getParameter("accountAddress");
		accountAddress2 = req.getParameter("accountAddress2");
		accountCity = req.getParameter("accountCity");
		accountState = req.getParameter("accountState");
		accountZipCode = req.getParameter("accountZipCode");
		accountCountry = req.getParameter("accountCountry");
		accountPhoneNumber = req.getParameter("accountPhoneNumber");
		rep.setPersonId(req.getParameter("personId"));
		
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		setOrganizationId(site.getOrganizationId());
		rep.setOrganizationId(site.getOrganizationId());
	}

	public AccountVO(ResultSet rs) {
		DBUtil util = new DBUtil();
		accountId = util.getStringVal("account_id", rs);
		accountNo = util.getStringVal("account_no", rs);
		accountName = util.getStringVal("account_nm", rs);
		accountAddress = util.getStringVal("address_txt", rs);
		accountAddress2 = util.getStringVal("address2_txt", rs);
		accountCity = util.getStringVal("city_nm", rs);
		accountState = util.getStringVal("state_cd", rs);
		accountZipCode = util.getStringVal("zip_cd", rs);
		accountCountry = util.getStringVal("country_cd", rs);
		accountPhoneNumber = util.getStringVal("phone_no_txt", rs);
		createDate = util.getDateVal("create_dt", rs);
		rep.setProfileId(util.getStringVal("profile_id", rs));
		rep.setPersonId(util.getStringVal("person_id", rs));
		rep.setSampleAccountNo(util.getStringVal("sample_acct_no", rs));
		rep.setTerritoryId(util.getStringVal("territory_id", rs));
		rep.setOrganizationId(util.getStringVal("organization_id", rs));
		util = null;
	}

	public String toString() {
		return StringUtil.getToString(this) + super.toString();
	}

	/**
	 * @return the accountId
	 */
	public String getAccountId() {
		return accountId;
	}

	/**
	 * @param accountId the accountId to set
	 */
	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	/**
	 * @return the person
	 */
	public PersonVO getRep() {
		return rep;
	}

	/**
	 * @param person the person to set
	 */
	public void setRep(PersonVO p) {
		this.rep = p;
	}

	/**
	 * @return the accountNo
	 */
	public String getAccountNo() {
		return accountNo;
	}

	/**
	 * @param accountNo the accountNo to set
	 */
	public void setAccountNo(String accountNo) {
		this.accountNo = accountNo;
	}

	/**
	 * @return the accountName
	 */
	public String getAccountName() {
		return accountName;
	}

	/**
	 * @param accountName the accountName to set
	 */
	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	/**
	 * @return the accountAddress
	 */
	public String getAccountAddress() {
		return accountAddress;
	}

	/**
	 * @param accountAddress the accountAddress to set
	 */
	public void setAccountAddress(String accountAddress) {
		this.accountAddress = accountAddress;
	}

	/**
	 * @return the accountAddress2
	 */
	public String getAccountAddress2() {
		return accountAddress2;
	}

	/**
	 * @param accountAddress2 the accountAddress2 to set
	 */
	public void setAccountAddress2(String accountAddress2) {
		this.accountAddress2 = accountAddress2;
	}

	/**
	 * @return the accountCity
	 */
	public String getAccountCity() {
		return accountCity;
	}

	/**
	 * @param accountCity the accountCity to set
	 */
	public void setAccountCity(String accountCity) {
		this.accountCity = accountCity;
	}

	/**
	 * @return the accountState
	 */
	public String getAccountState() {
		return accountState;
	}

	/**
	 * @param accountState the accountState to set
	 */
	public void setAccountState(String accountState) {
		this.accountState = accountState;
	}

	/**
	 * @return the accountZipCode
	 */
	public String getAccountZipCode() {
		return accountZipCode;
	}

	/**
	 * @param accountZipCode the accountZipCode to set
	 */
	public void setAccountZipCode(String accountZipCode) {
		this.accountZipCode = accountZipCode;
	}

	/**
	 * @return the accountCountry
	 */
	public String getAccountCountry() {
		return accountCountry;
	}

	/**
	 * @param accountCountry the accountCountry to set
	 */
	public void setAccountCountry(String accountCountry) {
		this.accountCountry = accountCountry;
	}

	/**
	 * @return the accountPhoneNumber
	 */
	public String getAccountPhoneNumber() {
		return accountPhoneNumber;
	}

	/**
	 * @param accountPhoneNumber the accountPhoneNumber to set
	 */
	public void setAccountPhoneNumber(String accountPhoneNumber) {
		this.accountPhoneNumber = accountPhoneNumber;
	}

	/**
	 * @return the createDate
	 */
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}
	
	public void addPhysician(PhysicianVO vo) {
		this.physicians.put(vo.getPhysicianId(), vo);
	}
	
	public void setPhysicians(Map<String, PhysicianVO> l) {
		this.physicians = l;
	}
	
	public List<PhysicianVO> getPhysicians() {
		List<PhysicianVO> data = new ArrayList<PhysicianVO>(physicians.values());
		Collections.sort(data, new UserDataComparator());
		return data;
	}

	public void addTransaction(TransactionVO vo) {
		this.transactions.put(vo.getTransactionId(), vo);
	}
	
	public void setTransactions(Map<String, TransactionVO> l) {
		this.transactions = l;
	}
	
	public List<TransactionVO> getTransactions() {
		List<TransactionVO> data = new ArrayList<TransactionVO>(transactions.values());
		Collections.sort(data, new TransactionComparator());
		return data;
	}
	
	public Map<String, TransactionVO> getTransactionMap() {
		return transactions;
	}
	
	public Integer getUnitCount() {
		Integer cnt = Integer.valueOf(0);
		for (TransactionVO t : transactions.values())
			if (t.getStatus() == Status.COMPLETE)
				cnt+= t.getUnitMap().size();
		
		return cnt;
	}

	public Integer getPendingRequestCount() {
		Integer cnt = Integer.valueOf(0);
		for (TransactionVO t : transactions.values())
			if (t.getStatus() == Status.PENDING) 
				++cnt;
		
		return cnt;
	}
	
	public Integer getApprovedRequestCount() {
		Integer cnt = Integer.valueOf(0);
		for (TransactionVO t : transactions.values())
			if (t.getStatus() == Status.APPROVED) 
				++cnt;
		
		return cnt;
	}

	public void setOrganizationId(String organizationId) {
		this.organizationId = organizationId;
	}

	public String getOrganizationId() {
		return organizationId;
	}
}
