package com.biomed.smarttrak.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.biomed.smarttrak.vo.NoteVO;
import com.bmg.admin.vo.CompanyVO;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: NoteLoder.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> takes a list of VOs, and gets notes related to that VOs type
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Ryan Riker
 * @version 2.0
 * @since Jan 27, 2017<p/>
 * @updates:
 ****************************************************************************/
public class NoteLoader extends SimpleActionAdapter {

	private String userId = null;
	private List<String> teamIds = null;

	public NoteLoader() {
		super();
	}

	public NoteLoader(ActionInitVO arg0) {
		super(arg0);
	}

	/**
	 * takes the VOs provided and gets all the 
	 * @param companyVOs
	 */
	public void addCompanyNotes(List<CompanyVO> companyVOs){
		log.debug("add company notes called");

		List<String>companyIds = new ArrayList<>();

		if (companyVOs == null) return;

		for (CompanyVO co : companyVOs){
			companyIds.add(co.getCompanyId());
		}

		NoteAction na = new NoteAction();	
		na.setDBConnection(dbConn);
		na.setAttributes(attributes);
		
		if (this.userId != null){
			Map<String, List<NoteVO>> results = na.getCompanyNotes(this.userId, this.teamIds, null, companyIds);

			
			for (CompanyVO co : companyVOs) {
				if (results.containsKey(co.getCompanyId())){
					log.debug("size of note list added to " + co.getCompanyId() + " is " + results.get(co.getCompanyId()).size());
					co.setNotes(results.get(co.getCompanyId()));
				}
			}
		}
	}


	public void addProductNotes(List<Object> ProductVOs){
		//TODO replace with the right vo when it exists or make one now

	}

	public void addMarketNotes(List<Object> MarketVOs){
		//TODO replace with the right vo when it exists or make one now
	}
	/**
	 * @return the userId
	 */
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
	 * @return the teamIds
	 */
	public List<String> getTeamIds() {
		return teamIds;
	}

	/**
	 * @param teamIds the teamIds to set
	 */
	public void setTeamIds(List<String> teamIds) {
		this.teamIds = teamIds;
	}

	/**
	 * checks the database for a user with the profile on the sent user data vo.  sets the note loaders 
	 * user id. 
	 * @param user
	 */
	public void setUser(UserDataVO user) {

		if (user == null || user.getProfileId() ==null || user.getProfileId().isEmpty()){
			log.debug("returned no possible profile id");
			return;
		}


		StringBuilder sb = new StringBuilder(60);

		sb.append("select * from ").append((String)attributes.get("customDbSchema")).append("biomedgps_user u ");
		sb.append("where profile_id = ? ");


		try (PreparedStatement ps = dbConn.prepareStatement(sb.toString())) {

			ps.setString(1, user.getProfileId());

			ResultSet rs = ps.executeQuery();

			if(rs.next()) {
				this.setUserId(rs.getString("user_id"));
				log.debug("user id set to " + userId);
			}

		}catch(SQLException sqle) {
			log.error("could not select biomed smarttrak user ", sqle);
		}

		return;
	}





}
