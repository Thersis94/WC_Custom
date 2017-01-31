package com.biomed.smarttrak.action;

//Java
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//WC_Custom
import com.biomed.smarttrak.action.NoteAction.NoteType;
import com.biomed.smarttrak.vo.NoteVO;
import com.bmg.admin.vo.NoteEntityInterface;

//SMTBaselibs
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.security.UserDataVO;

//WebCrescendo
import com.smt.sitebuilder.action.SimpleActionAdapter;

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
	 * takes the VOs provided and loads any notes on to those VOs, sets type to company
	 * @param companyVOs
	 */
	public void addCompanyNotes(List<NoteEntityInterface> companyVOs){
		log.debug("add company notes called");
		
		loadNotes((List<NoteEntityInterface>) companyVOs, NoteType.COMPANY);
	}

	/**
	 * takes the VOs provided and loads any notes on to those VOs, sets type to product
	 * @param ProductVOs
	 */
	public void addProductNotes(List<NoteEntityInterface> ProductVOs){
		log.debug("add product notes called");
		
		loadNotes((List<NoteEntityInterface>) ProductVOs, NoteType.PRODUCT);
	}
	/**
	 * takes the VOs provided and loads any notes on to those VOs, sets type to market
	 * @param MarketVOs
	 */
	public void addMarketNotes(List<NoteEntityInterface> MarketVOs){
		log.debug("add market notes called");
		
		loadNotes((List<NoteEntityInterface>) MarketVOs, NoteType.MARKET);
	}
	
	/**
	 * takes the vo and a note type and processes the right note action methods for
	 * each type and sorts the notes into the matching vo.  
	 * @param company 
	 * @param companyVOs
	 * @param company
	 */
	private void loadNotes(List<NoteEntityInterface> targetVOs, NoteType type) {
		List<String>targetIds = new ArrayList<>();

		if (targetVOs == null) return;

		for ( NoteEntityInterface vo : targetVOs){
			targetIds.add(vo.getId());
		}

		NoteAction na = new NoteAction();	
		na.setDBConnection(dbConn);
		na.setAttributes(attributes);
		
		if (this.userId != null){
			Map<String, List<NoteVO>> results = processNoteAction(na, type, targetIds);

			for (NoteEntityInterface co : targetVOs) {
				if (results.containsKey(co.getId())){
					log.debug("size of note list added to " + co.getId() + " is " + results.get(co.getId()).size());
					co.setNotes(results.get(co.getId()));
				}
			}
		}
	}


	/**
	 * based on note type returns the correct method call.
	 * @param na
	 * @param type
	 * @param targetIds 
	 * @return
	 */
	private Map<String, List<NoteVO>> processNoteAction(NoteAction na, NoteType type, List<String> targetIds) {
		
		switch(type) {
		case COMPANY :
			return na.getCompanyNotes(this.userId, this.teamIds, null, targetIds);
		case PRODUCT :
			return na.getProductNotes(this.userId, this.teamIds, null, targetIds);
		case MARKET :
			return na.getMarketNotes(this.userId, this.teamIds, null, targetIds);
		default :
			return null;
		}
	}
	
	/**
	 * checks the database for a user with the profile from the sent user data vo.  sets the note loaders 
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
}
