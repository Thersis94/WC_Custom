package com.biomed.smarttrak.vo;

//Java 8
import static java.time.temporal.ChronoUnit.DAYS;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

// SMTBaseLibs
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.user.HumanNameIntfc;

/*****************************************************************************
 <p><b>Title</b>: SmarttrakUserVO.java</p>
 <p><b>Description: </b>Value object that encapsulates a Smarttrak user.</p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Jan 31, 2017
 <b>Changes:</b> 
 ***************************************************************************/
@Table(name="BIOMEDGPS_USER")
public class UserVO extends UserDataVO implements HumanNameIntfc {
	private static final long serialVersionUID = -8619730513300299951L;
	private String accountId;
	private String userId;
	private String registerSubmittalId;
	private String licenseType;
	private List<TeamVO> teams;
	private Date expirationDate;
	private Date loginDate;
	private int loginAge = -1;
	private Date createDate;
	private int fdAuthFlg;
	private int gaAuthFlg;
	private int mktAuthFlg;
	private int acctOwnerFlg;
	private String loginOperSys;
	private String loginBrowser;
	private int statusFlg;

	/**
	 * Smarttrak status dropdowns - stored in the DB using code, label displayed on user mgmt screens.
	 */
	public enum LicenseType {
		COMPUPDATES("T","Comp Updates"),
		ACTIVE("A","SmartTRAK User"),
		EUREPORTS("M","EU Reports"),
		EUPLUS("P","EU Plus"),
		TRIAL("K","SmartTRAK Trial"),
		COMPLIMENTARY("C","SmartTRAK Complimentary"),
		EXTRA("E","SmartTRAK Extra Seat"),
		UPDATES("U","Updates Only"),
		TEST("D","Temporary / Test"),
		INACTIVE("I","Inactive"),
		STAFF("S","Staff");

		private String cd;
		private String label;
		private LicenseType(String cd, String lbl) {
			this.cd = cd;
			this.label = lbl;
		}
		public String getCode() { return cd; }
		public String getLabel() { return label; }
	}

	public enum Status {
		ACTIVE(1,"Active"),
		INACTIVE(0,"Inactive"),
		OPEN(-1,"Open License"),
		DEMO(5,"Demo");
		private int cd;
		private String label;
		private Status(int cd, String lbl) {
			this.cd = cd;
			this.label = lbl;
		}
		public int getCode() { return cd; }
		public String getLabel() { return label; }
	}


	/**
	 * Static mapping to the registration fields stored in the database.
	 * The reqParam value is what we use on our forms, so we know what to expect on the incoming request 
	 * when we go to save the data.
	 */
	public enum RegistrationMap {
		TITLE("dd64d07fb37c2c067f0001012b4210ff", "title"),
		UPDATES("9b079506b37cc0de7f0001014b63ad3c", "updates"),
		FAVORITEUPDATES("d5ed674eb37da7fd7f000101d875b114", "favUpdates"),
		PARENTCOMPANY("f6890a383eecc13f0a001421223e1a8c", "parentCompany"),
		COMPANY("e6890a383eecc13f0a001421223e1a8b", "company"),
		COMPANYURL("8e326f4c3ef49ae10a0014218aae436b", "companyUrl"),
		//below are all on the 'sales' tab on the admin edit form
		SOURCE("9cc5d1003ef592210a001421ccb8df2e", "source"),
		DEMODT("63347a903ef637ee0a001421c0d224c9", "demoDate"),
		TRAININGDT("5b44eca13ef688fe0a001421b4f167c8", "trainingDate"),
		INITTRAININGDT("b84694683ef8913b0a001421809cb366", "initialTrainingDate"),
		ADVTRAININGDT("b0be57103ef8f5b70a0014219084f190", "advancedTrainingDate"),
		OTHERTRAININGDT("9512e1b23f762ff70a0014211c33fc78", "otherTrainingDate"),
		NOTES("4bf7dbae3f767aa40a0014217d25df70", "notes"),
		JOBCATEGORY("ef6444293f7c69630a001421545e4917", "jobCategory"),
		JOBLEVEL("7bff2e9e3f7f7fb90a0014217397e884", "jobLevel"),
		INDUSTRY("5291b7693f8104240a001421db8d04ab", "industry"),
		DIVISIONS("31037d2e3f859f100a001421e77994f4", "divisions", true);

		private boolean isArray;
		private String fieldId;
		private String reqParam;
		private RegistrationMap(String registerFieldId, String reqParam) {
			this(registerFieldId, reqParam, false);
		}
		private RegistrationMap(String registerFieldId, String reqParam, boolean isArray) {
			this.fieldId = registerFieldId;
			this.reqParam = reqParam;
			this.isArray = isArray;
		}
		public String getFieldId() { return fieldId; }
		public String getReqParam() { return reqParam; }
		public boolean isArray() { return isArray; }
	}

	public UserVO() {
		teams = new ArrayList<>();
	}

	public UserVO(ActionRequest req) {
		super(req);
		teams = new ArrayList<>();
		setUserId(req.getParameter("userId"));
		setAccountId(req.getParameter("accountId"));
		setRegisterSubmittalId(req.getParameter("registerSubmittalId"));
		setLicenseType(req.getParameter("licenseType"));
		setStatusFlg(Convert.formatInteger(req.getParameter("statusFlg")));
		setExpirationDate(Convert.formatDate(Convert.DATE_SLASH_PATTERN, req.getParameter("expirationDate")));
		setFdAuthFlg(Convert.formatInteger(req.getParameter("fdAuthFlg")));
		setGaAuthFlg(Convert.formatInteger(req.getParameter("gaAuthFlg")));
		setMktAuthFlg(Convert.formatInteger(req.getParameter("mktAuthFlg")));
		setAcctOwnerFlg(Convert.formatInteger(req.getParameter("acctOwnerFlg")));
	}


	/**
	 * @return the userId
	 */
	@Column(name="user_id", isPrimaryKey=true)
	public String getUserId() {
		return userId;
	}

	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/**
	 * @return the accountId
	 */
	@Column(name="account_id")
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
	 * @return the registerSubmittalId
	 */
	@Column(name="register_submittal_id")
	public String getRegisterSubmittalId() {
		return registerSubmittalId;
	}

	/**
	 * @param registerSubmittalId the registerSubmittalId to set
	 */
	public void setRegisterSubmittalId(String registerSubmittalId) {
		this.registerSubmittalId = registerSubmittalId;
	}

	/**
	 * @return the teams
	 */
	public List<TeamVO> getTeams() {
		return teams;
	}

	/**
	 * Helper method for adding a team to the List of teams
	 * @param team
	 */
	public void addTeam(TeamVO team) {
		teams.add(team);
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	/**
	 * @return the updateDate
	 */
	@Override
	@Column(name="update_dt", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDate() {
		return updateDate;
	}

	/**
	 * @param updateDate the updateDate to set
	 */
	@Override
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	public String getLicenseName() {
		for (LicenseType s : LicenseType.values()) {
			if (s.getCode().equals(getLicenseType()))
				return s.getLabel();
		}
		return "";
	}

	/*
	 * NOTE: status_cd on the back end is actually used as License Type in the UI.  This was an oversight in the v2.0 rebuild
	 */
	@Column(name="status_cd")
	public String getLicenseType() {
		return licenseType;
	}

	public void setLicenseType(String t) {
		this.licenseType = t;
	}

	@Column(name="expiration_dt")
	public Date getExpirationDate() {
		return expirationDate;
	}

	public void setExpirationDate(Date expirationDate) {
		this.expirationDate = expirationDate;
	}

	public boolean isExpired() {
		if (Status.INACTIVE.getCode() == statusFlg) {
			return true;
		} else if (expirationDate != null) {
			return Calendar.getInstance().getTime().after(expirationDate);
		}
		return false;
	}

	@Column(name="login_dt", isReadOnly=true)
	public Date getLoginDate() {
		return loginDate;
	}

	public void setLoginDate(Date loginDate) {
		this.loginDate = loginDate;
	}


	/*********************
	 *  SOME DECOUPLING OF FIELDS STORED IN REGISTRATION DATA
	 *********************/

	@Column(name="title_txt", isReadOnly=true)
	public String getTitle() {
		return getFirstFrom(getAttribute(RegistrationMap.TITLE.getFieldId()));
	}
	public String getUpdates() {
		return getFirstFrom(getAttribute(RegistrationMap.UPDATES.getFieldId()));
	}
	public String getFavoriteUpdates() {
		return getFirstFrom(getAttribute(RegistrationMap.FAVORITEUPDATES.getFieldId()));
	}
	public String getCompany() {
		return getFirstFrom(getAttribute(RegistrationMap.COMPANY.getFieldId()));
	}
	public String getCompanyUrl() {
		return getFirstFrom(getAttribute(RegistrationMap.COMPANYURL.getFieldId()));
	}
	public String getParentCompany() {
		return getFirstFrom(getAttribute(RegistrationMap.PARENTCOMPANY.getFieldId()));
	}
	public String getSource() {
		return getFirstFrom(getAttribute(RegistrationMap.SOURCE.getFieldId()));
	}

	/**
	 * @deprecated - removed from user edit form 8-16-17
	 */
	@Deprecated
	public String getDemoDate() {
		return getFirstFrom(getAttribute(RegistrationMap.DEMODT.getFieldId()));
	}

	/**
	 * @deprecated - removed from user edit form 8-16-17
	 */
	@Deprecated
	public String getTrainingDate() {
		return getFirstFrom(getAttribute(RegistrationMap.TRAININGDT.getFieldId()));
	}

	/**
	 * @deprecated - removed from user edit form 8-16-17
	 */
	@Deprecated
	public String getInitialTrainingDate() {
		return getFirstFrom(getAttribute(RegistrationMap.INITTRAININGDT.getFieldId()));
	}

	/**
	 * @deprecated - removed from user edit form 8-16-17
	 */
	@Deprecated
	public String getAdvancedTrainingDate() {
		return getFirstFrom(getAttribute(RegistrationMap.ADVTRAININGDT.getFieldId()));
	}

	/**
	 * @deprecated - removed from user edit form 8-16-17
	 */
	@Deprecated
	public String getOtherTrainingDate() {
		return getFirstFrom(getAttribute(RegistrationMap.OTHERTRAININGDT.getFieldId()));
	}

	@Column(name="notes_txt", isReadOnly=true)
	public String getNotes() {
		return getFirstFrom(getAttribute(RegistrationMap.NOTES.getFieldId()));
	}
	public void setNotes(String note) {
		getAttributes().put(RegistrationMap.NOTES.getFieldId(), note);
	}

	public String getJobCategory() {
		return getFirstFrom(getAttribute(RegistrationMap.JOBCATEGORY.getFieldId()));
	}
	public String getJobLevel() {
		return getFirstFrom(getAttribute(RegistrationMap.JOBLEVEL.getFieldId()));
	}
	public String getIndustry() {
		return getFirstFrom(getAttribute(RegistrationMap.INDUSTRY.getFieldId()));
	}

	/**
	 * this is a multi-select, it could be returned from responseloader as a String or List depending on the values stored.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<String> getDivisions() {
		Object obj = getAttribute(RegistrationMap.DIVISIONS.getFieldId());
		List<String> data;
		if (obj instanceof String) {
			data = new ArrayList<>();
			data.add((String)obj);
		} else {
			data = (List<String>) obj;
		}
		return data;
	}

	/**
	 * separates whether registration has a List<String> or String saved internally from the methods above that only care about the data
	 * @param obj
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private String getFirstFrom(Object obj) {
		if (obj == null || obj instanceof String) {
			return (String) obj;
		} else {
			List<String> data = (List<String>) obj;
			return data.isEmpty() ? null : data.get(0);
		}
	}

	/**
	 * According to Mike each user should have only one division, yet the data supports multiple.  Take the 1st as their primary.
	 * @return
	 */
	@Column(name="division_txt", isReadOnly=true)
	public String getPrimaryDivision() {
		List<String> divs = getDivisions();
		return divs != null && !divs.isEmpty() ? divs.get(0) : null;
	}

	/**
	 * sets a title to the attributes list 
	 **/
	public void setPrimaryDivision(String div) {
		getAttributes().put(RegistrationMap.DIVISIONS.getFieldId(), div);
	}


	/**
	 * returns a list of fields presented on the public side of registration, so we know which fields to manage 
	 * when the user submits their form.  These fields will get deleted in SubmittalAction of registration, before 
	 * the new data is written.
	 * @return
	 */
	public static List<RegistrationMap> getPublicRegFields() {
		List<RegistrationMap> data = new ArrayList<>();
		data.add(RegistrationMap.TITLE);
		data.add(RegistrationMap.UPDATES);
		data.add(RegistrationMap.FAVORITEUPDATES);
		return data;
	}

	@Column(name="fd_auth_flg")
	public int getFdAuthFlg() {
		return fdAuthFlg;
	}

	public void setFdAuthFlg(int fdAuthFlg) {
		this.fdAuthFlg = fdAuthFlg;
	}

	@Column(name="ga_auth_flg")
	public int getGaAuthFlg() {
		return gaAuthFlg;
	}

	public void setGaAuthFlg(int gaAuthFlg) {
		this.gaAuthFlg = gaAuthFlg;
	}

	/**
	 * @deprecated no longer used for permissions -JM- 08.23.2017
	 * @return
	 */
	@Deprecated
	@Column(name="mkt_auth_flg")
	public int getMktAuthFlg() {
		return mktAuthFlg;
	}

	/**
	 * @deprecated no longer used for permissions -JM- 08.23.2017
	 */
	@Deprecated
	public void setMktAuthFlg(int mktAuthFlg) {
		this.mktAuthFlg = mktAuthFlg;
	}

	/*********************
	 *  SOME OVERRIDES FOR O.R.M. TO WORK PROPERLY WITH SUPERCLASS FIELDS
	 *********************/

	@Override
	@Column(name="profile_id")
	public String getProfileId() {
		return super.getProfileId();
	}

	@Column(name="first_nm", isReadOnly=true)
	public String getFirstName() {
		return super.getFirstName();
	}

	@Column(name="last_nm", isReadOnly=true)
	public String getLastName() {
		return super.getLastName();
	}

	@Override
	@Column(name="email_address_txt", isReadOnly=true)
	public String getEmailAddress() {
		return super.getEmailAddress();
	}

	@Column(name="acct_owner_flg")
	public int getAcctOwnerFlg() {
		return acctOwnerFlg;
	}

	public void setAcctOwnerFlg(int acctOwnerFlg) {
		this.acctOwnerFlg = acctOwnerFlg;
	}

	@Column(name="oper_sys_txt", isReadOnly=true)
	public String getLoginOperSys() {
		return loginOperSys;
	}

	public void setLoginOperSys(String loginOperSys) {
		this.loginOperSys = loginOperSys;
	}

	@Column(name="browser_txt", isReadOnly=true)
	public String getLoginBrowser() {
		return loginBrowser;
	}

	public void setLoginBrowser(String loginBrowser) {
		this.loginBrowser = loginBrowser;
	}
	/**
	 * sets a title to the attributes list 
	 **/
	public void setTitle(String title) {
		getAttributes().put(RegistrationMap.TITLE.getFieldId(), title);
	}
	/**
	 * sets a title to the attributes list
	 * @deprecated - title should never be a list.  If you're using this method you're doing something wrong, or data is bad. 
	 **/
	@Deprecated
	public void setTitle(List<String> title) {
		if (title != null && !title.isEmpty())
			getAttributes().put(RegistrationMap.TITLE.getFieldId(), title.get(0));
	}

	/**
	 * returns a constant int based on the last time the user logged-in to the website - used on Userrs list page (legend)
	 * @param loginDate
	 * @return
	 */
	public Integer getLoginAge() {
		if (loginAge != -1) return loginAge;

		if (loginDate == null) {
			loginAge = 0;
		} else {
			Instant instant = Instant.ofEpochMilli(loginDate.getTime());
			LocalDate login = LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalDate();
			long days = DAYS.between(login, LocalDate.now());
			if (days < 30) {
				loginAge = 30;
			} else if (days <= 90) {
				loginAge = 60;
			} else {
				loginAge = 90;
			}
		}
		return loginAge;
	}

	@Column(name="active_flg")
	public int getStatusFlg() {
		return statusFlg;
	}

	public void setStatusFlg(int statusFlg) {
		this.statusFlg = statusFlg;
	}

	/**
	 * @return
	 */
	public String getStatusName() {
		for (Status s : Status.values()) {
			if (s.getCode() == getStatusFlg())
				return s.getLabel();
		}
		return "";
	}
}