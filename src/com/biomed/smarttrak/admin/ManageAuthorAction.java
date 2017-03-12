package com.biomed.smarttrak.admin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.biomed.smarttrak.vo.AccountVO;
import com.biomed.smarttrak.vo.UserVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.databean.FilePartDataBean;
import com.siliconmtn.util.user.HumanNameIntfc;
import com.siliconmtn.util.user.NameComparator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.SBProfileManager;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: ManageAuthorAction.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> manages displaying and updating author images
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Ryan Riker
 * @version 2.0
 * @since Mar 10, 2017<p/>
 * @updates:
 ****************************************************************************/
public class ManageAuthorAction extends SimpleActionAdapter {

	
	public ManageAuthorAction() {
		super();
	}

	public ManageAuthorAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug(" ########### manage author action retrieve called");
		
		
		if(req.hasParameter("requestType")){
			//TODO major concern about this not going to the right method, why am i not getting req build.
			log.debug("##################### request type is: " + req.getParameter("requestType"));
			
			if("reqBuild".equals(req.getParameter("requestType")))
				this.build(req);
			
		}
		
		
		if (req.hasParameter("loadAuthorList")) {
			String schema = (String)getAttributes().get(Constants.CUSTOM_DB_SCHEMA);
			
			loadAuthors(req);
			@SuppressWarnings("unchecked")
			List<AccountVO> managers = (List<AccountVO>) req.getAttribute(AccountAction.MANAGERS);
			
			StringBuilder sql = new StringBuilder(199);
			sql.append("select profile_img, profile_id, last_nm, first_nm from profile where profile_id in ");
			
			sql.append("( ?");
			
			for (int i=1; i< managers.size(); i++){
				sql.append(", ?");
			}
			
			sql.append(" ) ");
			
			log.debug("sql " + sql);
			
			List<UserVO> users = new ArrayList<>();
			
			try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
				int x = 1;
				for (AccountVO manager : managers){
					ps.setString(x++, manager.getOwnerProfileId());
				}
				ResultSet rs = ps.executeQuery();
				while(rs.next()) {
					UserVO vo = new UserVO();
					vo.setProfileId(rs.getString("profile_id"));
					vo.setFirstName(rs.getString("first_nm"));
					vo.setLastName(rs.getString("last_nm"));
					vo.setProfileImage(rs.getString("profile_img"));
					users.add(vo);
				}
			} catch (SQLException sqle) {
				throw new ActionException("could not save account ACLs", sqle);
			}
			log.debug("loaded " + users.size() + " users");
			
			new NameComparator().decryptNames((List<? extends HumanNameIntfc>)(List<?>)users, (String)getAttribute(Constants.ENCRYPT_KEY));
			
			putModuleData(users);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.debug("#################33 manage author action build called");
		ProfileManager pm = new SBProfileManager(attributes);
		
		//Set the profile image and remove from profile Data
		FilePartDataBean profileImg = req.getFile("profileImg");
	
		if(profileImg != null){
		log.debug("did it grab the file if so its name is: " + profileImg.getFileName());
		log.debug("size? "+ profileImg.getFileSize());
		}else{
			log.debug("looks like the file is null");
		}

		
		
/*		//Check for profile image and process if needed.  This must be done after
		//updateProfile because we need the user to have the correct profileId.
		if (profileImg != null) {
			try {
				pm.updateProfileImage(user,profileImg,dbConn);
			} catch(Exception ide) {
				log.error("Error updating profile Image: " + ide.getMessage());
			}
		}
		*/
	}
	
	/**
	 * loads a list of to the request.
	 * @param req
	 * @throws ActionException 
	 */
	private void loadAuthors(ActionRequest req) throws ActionException {
		log.debug("loaded authors");
		
		AccountAction aa = new AccountAction();
		aa.setActionInit(actionInit);
		aa.setAttributes(attributes);
		aa.setDBConnection(dbConn);
		aa.loadManagerList(req, (String)getAttributes().get(Constants.CUSTOM_DB_SCHEMA));

	}
	
}
