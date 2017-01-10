package com.depuy.forefront.action;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import com.siliconmtn.http.session.SMTSession;

import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.AuthorizationException;
import com.siliconmtn.security.UserRoleVO;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.user.LoginAction;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.DBRoleModule;

/*****************************************************************************
 <p><b>Title</b>: FFDBLoginModule.java</p>
 <p><b>Description: </b>Implements user login for all site activities.  This 
 class is called via a factory pattern.  It is identified in the config file.</p>
 <p> Uses the SB data base to retrieve authentication info
 <p>Copyright: Copyright (c) 2000 - 2012 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James McKain
 @version 1.0
 @since Jul 08, 2012
 Code Updates
 ***************************************************************************/

public class FFDBRoleModule extends DBRoleModule {
	
	private static final String REGISTER_KNEE_ACTION_ID = "c0a802376a53b21c3affd06f9bf57f78"; //for lack of a better way at this time!
	private static final String REGISTER_SITE_ID_FLD = "c0a802376a5f4de87a51a76bd46ead00";
	private static final String REGISTER_SURGERY_DATE_FLD = "c0a802376a6171f3e8a74d60af4f7690";
	private static final String ACTION_PLAN_URL = "/myactionplan";
	
    public FFDBRoleModule() {
        super();
    }

    public FFDBRoleModule(Map<String, Object> arg0) {
        super(arg0);
    }

    /* (non-Javadoc)
     * @see com.siliconmtn.security.AbstractLoginModule#authenticate(java.lang.String, java.lang.String)
     */
    @Override
    public UserRoleVO getUserRole(String profileId, String siteId)
    		throws AuthorizationException {
    	log.debug("loading custom FF role");
        UserRoleVO vo = super.getUserRole(profileId, siteId);
        
        //load the user's siteId (hospital) and surgery date based on their regisration data.
        this.loadCustomData(profileId);
        
        return vo;
    }

    
    private void loadCustomData(String profileId) throws AuthorizationException {
    	Connection dbConn = (Connection)initVals.get(GlobalConfig.KEY_DB_CONN);
		ActionRequest req = (ActionRequest)this.initVals.get(GlobalConfig.HTTP_REQUEST);
    	
        StringBuilder sql = new StringBuilder();
        sql.append("select d.value_txt as 'surgery_dt', e.alias_path_nm ");
        sql.append("from register_submittal b ");
        sql.append("inner join register_data c on b.register_submittal_id=c.register_submittal_id and c.register_field_id=? "); //site (hospital) they registered on
        sql.append("inner join register_data d on b.register_submittal_id=d.register_submittal_id and d.register_field_id=? "); //their surgery date in MM/DD/YYYY
        sql.append("inner join site e on cast(c.value_txt as nvarchar(32))=e.site_id ");
        sql.append("where b.action_id=? and b.profile_id=?");
        log.debug(sql + profileId);
        
        PreparedStatement ps = null;
        try {
        	ps = dbConn.prepareStatement(sql.toString());
        	ps.setString(1, REGISTER_SITE_ID_FLD);
        	ps.setString(2, REGISTER_SURGERY_DATE_FLD);
        	ps.setString(3, REGISTER_KNEE_ACTION_ID);
        	ps.setString(4, profileId);
        	
        	ResultSet rs = ps.executeQuery();
        	if (rs.next()) {
        		this.setScopeVariables(req, rs.getString("surgery_dt"), rs.getString("alias_path_nm"));
        		
        	} else {
        		//no RS means registration was incomplete or is invalid.
        		//the system will not function properly under this condition.
        		
        		//first see if the user just registered...the database may have queued the write transaction,
        		//causing the above 'read' to return no RS.
        		//this is not a security risk!  We are merely setting some cosmetic values on the session,
        		//not Authenticating OR Authorizing the user!
        		if (req.hasParameter("reg_||" + REGISTER_SURGERY_DATE_FLD)) {
        			log.debug("request-scope login");
        			this.setScopeVariables(req, req.getParameter("reg_||" + REGISTER_SURGERY_DATE_FLD), 
        					(String)req.getAttribute(Constants.SITE_ALIAS_PATH));
        			
        		} else {
	        		//reject the user's login attempt.
	        		throw new AuthorizationException("missing registration data for " + profileId);
        		}
        	}
        } catch (SQLException sqle) {
        	log.error("could not load user data for " + profileId, sqle);
        } finally {
        	try { ps.close(); } catch (Exception e) {}
        }
        
    }
    
    private void setScopeVariables(ActionRequest req, String surgDtStr, String siteAliasPath) {
    	//calc week# offset between 'today' and their surgery date
		Long currentTime = Calendar.getInstance().getTime().getTime();
		Date surgDt = Convert.formatDate(Convert.DATE_SLASH_PATTERN, surgDtStr);
		Long surgTime = surgDt.getTime();
		log.debug("surgery date =" + surgDt);
		
		int wkNo = EditMyProgressAction.calcTimeDiff(surgTime, currentTime);
                
		//set some hard limits on the high/low ranges; these are merely safeguards for display aesthetics
		if (wkNo > 4) wkNo = 4;
		if (wkNo < -4) wkNo = -4;
		
		log.debug("surgWeek=" + wkNo + ", from " + surgDt);
		SMTSession ses = req.getSession();
		ses.setAttribute("surgeryWeek", wkNo);
		ses.setAttribute("surgeryDate", surgDt);
		ses.setAttribute("hospSitePath", siteAliasPath);
		
		//redirect all but "WebEdit" logins
		if (!req.getServletPath().startsWith("/admin")) {
			//setup the redirect to the user's ActionPlan on their hospital's website
			String redirUrl = initVals.get(Constants.CONTEXT_PATH) + "/" + siteAliasPath + ACTION_PLAN_URL + initVals.get(Constants.PAGE_ALIAS);
			log.debug("redirUrl = " + redirUrl);
			ses.setAttribute(LoginAction.DESTN_URL, redirUrl);
		}
    }
}
