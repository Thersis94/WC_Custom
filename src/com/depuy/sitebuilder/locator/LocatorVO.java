package com.depuy.sitebuilder.locator;

// JDK 1.5.2
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

// SMT Base Libs 2.0
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;

// SB Libs
import com.smt.sitebuilder.action.SBModuleVO;

/*****************************************************************************
 <p><b>Title</b>: LocatorVO.java</p>
 <p>Stores the data for the Locator action</p>
 <p>Copyright: Copyright (c) 2000 - 2005 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James Camire
 @version 2.0
 @since Jun 19, 2006
 Code Updates
 James Camire, Jun 19, 2006 - Creating Initial Class File
 ***************************************************************************/

public class LocatorVO extends SBModuleVO {
    private static final long serialVersionUID = 1l;
    private String searchId = null;
    private String country = "US";
    private String language = "en";
    private String headerText = null;
    private String footerText = null;
    private String privacyUrl = null;
    private String surveyId = null;
    private Integer surveyReqFlag = null;
    private String registrationId = null;
    private Integer registrationReqFlag = null;
    private String emailFriendId = null;
    private Integer emailFriendReqFlag = null;
    private Integer searchTypeId = null;
    private Integer resultsPerPage = null;
    private Map<String, Boolean> fields = null;
    
    /**
     * 
     */
    public LocatorVO() {
        super();
        fields = new HashMap<String, Boolean>();
    }
    
    /**
     * Sets the data from the request object to the param value
     * @param req
     */
    public void setData(SMTServletRequest req) {
        actionId = req.getParameter("sbActionId");
        organizationId = req.getParameter("organizationId");
        searchId = req.getParameter("searchId");
        country = req.getParameter("country");
        language = req.getParameter("language");
        headerText = req.getParameter("headerText");
        footerText = req.getParameter("footerText");
        privacyUrl = req.getParameter("privacyUrl");
        surveyId = req.getParameter("surveyId");
        surveyReqFlag = Convert.formatInteger(req.getParameter("surveyReqFlag"));
        registrationId = req.getParameter("registrationId");
        registrationReqFlag = Convert.formatInteger(req.getParameter("registrationReqFlag"));
        emailFriendId = req.getParameter("emailFriendId");
        emailFriendReqFlag = Convert.formatInteger(req.getParameter("emailFriendReqFlag"));
        searchTypeId = Convert.formatInteger(req.getParameter("searchTypeId"));
        resultsPerPage = Convert.formatInteger(req.getParameter("resultsPerPage"));
        pendingSyncFlag = Convert.formatInteger(req.getParameter("pendingSyncFlag"));
        actionGroupId = req.getParameter("sbActionGroupId");
    }
    
    /**
     * Sets the data from the result set to the param value
     * @param rs
     */
    public void setData(ResultSet rs) {
        DBUtil db = new DBUtil();
        actionId = db.getStringVal("action_id", rs);
        organizationId = db.getStringVal("organization_id", rs);
        searchId = db.getStringVal("search_id", rs);
        country = db.getStringVal("country_cd", rs);
        language = db.getStringVal("language_cd", rs);
        headerText = db.getStringVal("header_txt", rs);
        footerText = db.getStringVal("footer_txt", rs);
        privacyUrl = db.getStringVal("privacy_url", rs);
        searchTypeId = db.getIntegerVal("search_type_id", rs);
        resultsPerPage = db.getIntegerVal("results_page_no", rs);
        surveyReqFlag = db.getIntegerVal("survey_req_flg", rs);
        surveyId = db.getStringVal("survey_id", rs);
        surveyReqFlag = db.getIntegerVal("survey_req_flg", rs);
        registrationReqFlag = db.getIntegerVal("registration_req_flg", rs);
        registrationId = db.getStringVal("registration_id", rs);
        emailFriendReqFlag = db.getIntegerVal("email_friend_req_flg", rs);
        emailFriendId = db.getStringVal("email_friend_id", rs);
		pendingSyncFlag = db.getIntVal("pending_sync_flg", rs);
        actionGroupId = db.getStringVal("action_group_id", rs);
    }
    
    /**
     * @return Returns the country.
     */
    public String getCountry() {
        return country;
    }

    /**
     * @return Returns the footerText.
     */
    public String getFooterText() {
        return footerText;
    }

    /**
     * @return Returns the headerText.
     */
    public String getHeaderText() {
        return headerText;
    }

    /**
     * @return Returns the language.
     */
    public String getLanguage() {
        return language;
    }

    /**
     * @return Returns the privacyUrl.
     */
    public String getPrivacyUrl() {
        return privacyUrl;
    }

    /**
     * @return Returns the resultsPerPage.
     */
    public Integer getResultsPerPage() {
        return resultsPerPage;
    }

    /**
     * @return Returns the searchId.
     */
    public String getSearchId() {
        return searchId;
    }

    /**
     * @return Returns the searchTypeId.
     */
    public Integer getSearchTypeId() {
        return searchTypeId;
    }

    /**
     * @param country The country to set.
     */
    public void setCountry(String country) {
        this.country = country;
    }

    /**
     * @param footerText The footerText to set.
     */
    public void setFooterText(String footerText) {
        this.footerText = footerText;
    }

    /**
     * @param headerText The headerText to set.
     */
    public void setHeaderText(String headerText) {
        this.headerText = headerText;
    }

    /**
     * @param language The language to set.
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * @param privacyUrl The privacyUrl to set.
     */
    public void setPrivacyUrl(String privacyUrl) {
        this.privacyUrl = privacyUrl;
    }

    /**
     * @param resultsPerPage The resultsPerPage to set.
     */
    public void setResultsPerPage(Integer resultsPage) {
        this.resultsPerPage = resultsPage;
    }

    /**
     * @param searchId The searchId to set.
     */
    public void setSearchId(String searchId) {
        this.searchId = searchId;
    }

    /**
     * @param searchTypeId The searchTypeId to set.
     */
    public void setSearchTypeId(Integer searchTypeId) {
        this.searchTypeId = searchTypeId;
    }

    /**
     * @return Returns the fields.
     */
    public Map<String, Boolean> getFields() {
        return fields;
    }

    /**
     * @param fields The fields to set.
     */
    public void setFields(Map<String, Boolean> fields) {
        this.fields = fields;
    }

	/**
	 * @return the surveyFlag
	 */
	public String getSurveyId() {
		return surveyId;
	}

	/**
	 * @param surveyFlag the surveyFlag to set
	 */
	public void setSurveyId(String surveyId) {
		this.surveyId = surveyId;
	}

	/**
	 * @return the surveyReqFlag
	 */
	public Integer getSurveyReqFlag() {
		return surveyReqFlag;
	}

	/**
	 * @param surveyReqFlag the surveyReqFlag to set
	 */
	public void setSurveyReqFlag(Integer surveyReqFlag) {
		this.surveyReqFlag = surveyReqFlag;
	}

	/**
	 * @return the registrationId
	 */
	public String getRegistrationId() {
		return registrationId;
	}

	/**
	 * @param registrationId the registrationId to set
	 */
	public void setRegistrationId(String registrationId) {
		this.registrationId = registrationId;
	}

	/**
	 * @return the registrationReqFlag
	 */
	public Integer getRegistrationReqFlag() {
		return registrationReqFlag;
	}

	/**
	 * @param registrationReqFlag the registrationReqFlag to set
	 */
	public void setRegistrationReqFlag(Integer registrationReqFlag) {
		this.registrationReqFlag = registrationReqFlag;
	}

	/**
	 * @return the emailFriendId
	 */
	public String getEmailFriendId() {
		return emailFriendId;
	}

	/**
	 * @param emailFriendId the emailFriendId to set
	 */
	public void setEmailFriendId(String emailFriendId) {
		this.emailFriendId = emailFriendId;
	}

	/**
	 * @return the emailFriendReqFlag
	 */
	public Integer getEmailFriendReqFlag() {
		return emailFriendReqFlag;
	}

	/**
	 * @param emailFriendReqFlag the emailFriendReqFlag to set
	 */
	public void setEmailFriendReqFlag(Integer emailFriendReqFlag) {
		this.emailFriendReqFlag = emailFriendReqFlag;
	}

}
