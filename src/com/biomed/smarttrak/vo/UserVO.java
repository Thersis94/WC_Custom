package com.biomed.smarttrak.vo;

// Java 7
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.biomed.smarttrak.admin.user.HumanNameIntfc;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

// SMTBaseLibs
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;

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
	private String statusCode;
	private List<TeamVO> teams;
	private Date expirationDate;
	private Date loginDate;
	private Date createDate;
	private Date updateDate;

	/**
	 * Smarttrak status dropdowns - stored in the DB using code, label displayed on user mgmt screens.
	 */
	public enum Status {
		COMPUPDATES("T","Comp Updates"),
		ACTIVE("A","SmartTRAK User"),
		REPORTS("M","EU Reports"),
		EUPLUS("P","EU Plus"),
		TRIAL("K","SmartTRAK Trial"),
		COMPLIMENTART("C","SmartTRAK Complimentary"),
		EXTRA("E","SmartTRAK Extra Seat"),
		UPDATES("U","Updates Only"),
		TEST("D","Temporary/ Test"),
		STAFF("S","Staff");

		private String cd;
		private String label;
		private Status(String cd, String lbl) {
			this.cd = cd;
			this.label = lbl;
		}
		public String getCode() { return cd; }
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
		DIVISIONS("31037d2e3f859f100a001421e77994f4", "divisions");

		private String fieldId;
		private String reqParam;
		private RegistrationMap(String registerFieldId, String reqParam) { 
			this.fieldId = registerFieldId;
			this.reqParam = reqParam;
		}
		public String getFieldId() { return fieldId; }
		public String getReqParam() { return reqParam; }
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
		setStatusCode(req.getParameter("statusCode"));
		setExpirationDate(Convert.formatDate(Convert.DATE_SLASH_PATTERN, req.getParameter("expirationDate")));
		populateRegistrationFields(req);
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
	@Column(name="create_dt", isInsertOnly=true)
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
	@Column(name="update_dt", isUpdateOnly=true)
	public Date getUpdateDate() {
		return updateDate;
	}

	/**
	 * @param updateDate the updateDate to set
	 */
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	@Column(name="status_cd")
	public String getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}

	@Column(name="expiration_dt")
	public Date getExpirationDate() {
		return expirationDate;
	}

	public void setExpirationDate(Date expirationDate) {
		this.expirationDate = expirationDate;
	}

	@Column(name="login_dt", isReadOnly=true)
	public Date getLoginDate() {
		return loginDate;
	}

	public void setLoginDate(Date loginDate) {
		this.loginDate = loginDate;
	}

	/**
	 * iterates the RegistrationMap, taking each value off the request object and putting it into the UserVO.
	 * When we save this data we'll do a similar iteration (in the Action's save method).
	 *		e.g. forEach(enum) saveRegField(e.fieldId, user.getAttribute(e.fieldId));
	 * @param req
	 */
	private void populateRegistrationFields(ActionRequest req) {
		for (RegistrationMap entry : RegistrationMap.values())
			addAttribute(entry.getFieldId(), req.getParameter(entry.getReqParam()));
	}

	/*********************
	 *  SOME DECOUPLING OF FIELDS STORED IN REGISTRATION DATA
	 *********************/

	public String getTitle() {
		return (String)getAttribute(RegistrationMap.TITLE.getFieldId());
	}
	public String getUpdates() {
		return (String)getAttribute(RegistrationMap.UPDATES.getFieldId());
	}
	public String getFavoriteUpdates() {
		return (String)getAttribute(RegistrationMap.FAVORITEUPDATES.getFieldId());
	}
	public String getCompany() {
		return (String)getAttribute(RegistrationMap.COMPANY.getFieldId());
	}
	public String getCompanyUrl() {
		return (String)getAttribute(RegistrationMap.COMPANYURL.getFieldId());
	}
	public String getSource() {
		return (String)getAttribute(RegistrationMap.SOURCE.getFieldId());
	}
	public Date getDemoDate() {
		return formatDateField(getAttribute(RegistrationMap.DEMODT.getFieldId()));
	}
	public Date getTrainingDate() {
		return formatDateField(getAttribute(RegistrationMap.TRAININGDT.getFieldId()));
	}
	public Date getInitialTrainingDate() {
		return formatDateField(getAttribute(RegistrationMap.INITTRAININGDT.getFieldId()));
	}
	public Date getAdvancedTrainingDate() {
		return formatDateField(getAttribute(RegistrationMap.ADVTRAININGDT.getFieldId()));
	}
	public Date getOtherTrainingDate() {
		return formatDateField(getAttribute(RegistrationMap.OTHERTRAININGDT.getFieldId()));
	}
	public String getNotes() {
		return (String)getAttribute(RegistrationMap.NOTES.getFieldId());
	}
	public String getJobCategory() {
		return (String)getAttribute(RegistrationMap.JOBCATEGORY.getFieldId());
	}
	public String getJobLevel() {
		return (String)getAttribute(RegistrationMap.JOBLEVEL.getFieldId());
	}
	public String getIndustry() {
		return (String)getAttribute(RegistrationMap.INDUSTRY.getFieldId());
	}
	public String getDivisions() {
		return (String)getAttribute(RegistrationMap.DIVISIONS.getFieldId());
	}

	/**
	 * turns an object...likely a String...into a Date with our given default MM/DD/YYY format used site-wide
	 * @param dt
	 * @return
	 */
	private Date formatDateField(Object dt) {
		if (dt == null) return null;
		return Convert.formatDate(Convert.DATE_SLASH_PATTERN, (String)dt);
	}

	/*********************
	 *  SOME OVERRIDES FOR O.R.M. TO WORK PROPERLY WITH SUPERCLASS FIELDS
	 *********************/

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

	@Column(name="email_address_txt", isReadOnly=true)
	public String getEmailAddress() {
		return super.getEmailAddress();
	}
}