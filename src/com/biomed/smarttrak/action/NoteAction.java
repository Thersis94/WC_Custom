package com.biomed.smarttrak.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.biomed.smarttrak.vo.NoteVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: NoteAction.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> Put Something Here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Ryan Riker
 * @version 2.0
 * @since Jan 24, 2017<p/>
 * @updates:
 ****************************************************************************/
public class NoteAction extends SimpleActionAdapter {

	public static final String ALL_NOTES = "AllNotes";
	public static final String TEAM_TYPE = "teamType";
	public static final String ATTR_TYPE = "AttrType";
	
	public enum NoteTypes {
	    COMPANY,
	    PRODUCT,
	    MARKET
	}

	public NoteAction() {
		super();
	}

	public NoteAction(ActionInitVO arg0) {
		super(arg0);
	}

	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("Notes Action Retrieve called");
		//TODO add a testing call here.
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("Notes Action Build called");
		if(req.hasParameter("isDelete")) {
			deleteNote(req);
		} else {
			insertUpdateNote(req);
		}

	}

	/**
	 * inserts or updates a note
	 * @param req
	 */
	private void insertUpdateNote(SMTServletRequest req) {
		log.debug("Notes Action insert note called");

		NoteVO n = new NoteVO(req);		
		//Execute and store Data
		try (PreparedStatement ps = dbConn.prepareStatement( getInsertUpdateStatement(req) )) {
			ps.setString(1, n.getUserId());
			ps.setString(2, n.getTeamId());
			ps.setString(3, n.getCompanyId());
			ps.setString(4, n.getCompanyAttributeId());
			ps.setString(5, n.getProductId());
			ps.setString(6, n.getProductAttributeId());
			ps.setString(7, n.getMarketId());
			ps.setString(8, n.getMarketAttributeId());
			ps.setString(9, n.getNoteName());
			ps.setString(10, n.getNoteText());
			ps.setString(11, n.getFilePathText());
			ps.setTimestamp(12, Convert.formatTimestamp(n.getExpirationDate()));
			ps.setTimestamp(13, Convert.getCurrentTimestamp());
			
			if(n.getNoteId() != null || !n.getNoteId().isEmpty()){		
				ps.setString(14, n.getNoteId());
			}else{
				ps.setString(14, new UUIDGenerator().getUUID());
			}

			ps.executeUpdate();

		} catch(SQLException sqle) {
			log.error("could not insert or update note", sqle);
		}

		//Return results to view.
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		if (mod == null) mod = new ModuleVO(); //this is null when we call from other actions and intentionally don't pass attributes (Map)
		setAttribute(Constants.MODULE_DATA, mod);

	}

	/**
	 * returns an insert of update statement depending on an attribute 
	 * @param req 
	 * @return
	 */
	private String getInsertUpdateStatement(SMTServletRequest req) {
		//Build Sql statement
		StringBuilder sql = new StringBuilder(320);

		if (Convert.formatBoolean(req.getAttribute(INSERT_TYPE))) {
			sql.append("insert into ").append((String)attributes.get("customDbSchema")).append("biomedgps_note ");
			sql.append("USER_ID,              TEAM_ID,       COMPANY_ID,          COMPANY_ATTRIBUTE_ID, PRODUCT_ID, ");
			sql.append("PRODUCT_ATTRIBUTE_ID, MARKET_ID,     MARKET_ATTRIBUTE_ID, NOTE_NM,              NOTE_TXT, ");
			sql.append("FILE_PATH_TXT,        EXPIRATION_DT, UPDATE_DT,           NOTE_ID ");
			sql.append("values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
		}else{
			sql.append("update ").append((String)attributes.get("customDbSchema")).append("biomedgps_note ");
			sql.append(" set USER_ID = ?, TEAM_ID = ?, COMPANY_ID = ?, COMPANY_ATTRIBUTE_ID = ?, PRODUCT_ID = ?, ");
			sql.append("PRODUCT_ATTRIBUTE_ID = ?, MARKET_ID = ?, MARKET_ATTRIBUTE_ID = ?, NOTE_NM = ?, NOTE_TXT = ?, ");
			sql.append("FILE_PATH_TXT = ?, EXPIRATION_DT = ?, UPDATE_DT =? where NOTE_ID = ? ");
		}

		log.debug(sql);

		return sql.toString();
	}

	/**
	 * deletes a note
	 * @param req
	 */
	private void deleteNote(SMTServletRequest req) {
		log.debug("Notes Action delete note  called");

		StringBuilder sql = new StringBuilder(320);

		if (Convert.formatBoolean(req.getAttribute(INSERT_TYPE))) {
			sql.append("delete from ").append((String)attributes.get("customDbSchema")).append("biomedgps_note ");
			sql.append("where NOTE_ID = ? ");
		}
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, StringUtil.checkVal(req.getParameter("NOTE_ID")));

			ps.execute();
		}catch(SQLException sqle) {
			log.error("could not deletenote ", sqle);
		}
	}


	/**
	 * loops the list of notes and adds the notes to an other list when conditions are correct
	 * 
	 * @param team
	 * @param list
	 * @param teamType
	 * @return
	 */
	private List<NoteVO> processNotes(String targetId, List<NoteVO> allNotes, String searchType, String noteRequestType) {
		
		List<NoteVO> processedList = new ArrayList<>();
		
		for (NoteVO vo : allNotes){
			if (searchType.equals(TEAM_TYPE)){
				if(targetId.equals(vo.getTeamId())){
					processedList.add(vo);
				}
			}else{
				if(targetId.equals(getAttrValue(vo, noteRequestType))){
					processedList.add(vo);
				}
			}
		}
	
		return processedList;
	}

	/**
	 * based on the note request type the switch sends back the correct string to test.
	 * @param vo
	 * @param noteRequestType
	 * @return
	 */
	private String getAttrValue(NoteVO vo, String noteRequestType) {
		
		switch(noteRequestType) {
		   case "company" :
			   return vo.getCompanyAttributeId();
		   case "product" :
			   return vo.getProductAttributeId();
		   case "market" :
			   return vo.getMarketAttributeId();
		   default :
			   return null;
		}
	}

	/**
	 * pulls every note with the accompanying company id
	 * @param companyId 
	 * @return
	 */
	private List<NoteVO> getFullCompanyList(String companyId) {

		StringBuilder sql = new StringBuilder(320);
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		List<NoteVO> data = new ArrayList<>();

		sql.append("select * from ").append((String)attributes.get("customDbSchema")).append("biomedgps_note n");
		sql.append("inner join ").append((String)attributes.get("customDbSchema")).append("BIOMEDGPS_USERS u on u.user_id = n.user_id ");
		sql.append("inner join PROFILE p  on p.profile_id = u.profile_id ");
		sql.append("where company_id = ? ");

		log.debug(sql.toString() +"|" + companyId);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, companyId);

			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				NoteVO vo = new NoteVO(rs);

				String firstName = pm.getStringValue("FIRST_NM", rs.getString("FIRST_NM"));
				String lastName = pm.getStringValue("LAST_NM", rs.getString("LAST_NM"));

				vo.setUserName(firstName +" "+ lastName);

				data.add(vo);
			}

		}catch(SQLException sqle) {
			log.error("could not select company notes ", sqle);
		}

		return data;
	}
	
	/**
	 * pulls every note with the accompanying product id
	 * @param productId
	 * @return
	 */
	private List<NoteVO> getFullProductList(String productId) {
		StringBuilder sql = new StringBuilder(320);
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		List<NoteVO> data = new ArrayList<>();

		sql.append("select * from ").append((String)attributes.get("customDbSchema")).append("biomedgps_note n");
		sql.append("inner join ").append((String)attributes.get("customDbSchema")).append("BIOMEDGPS_USERS u on u.user_id = n.user_id ");
		sql.append("inner join PROFILE p  on p.profile_id = u.profile_id ");
		sql.append("where product_id = ? ");

		log.debug(sql.toString() +"|" + productId);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, productId);

			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				NoteVO vo = new NoteVO(rs);

				String firstName = pm.getStringValue("FIRST_NM", rs.getString("FIRST_NM"));
				String lastName = pm.getStringValue("LAST_NM", rs.getString("LAST_NM"));

				vo.setUserName(firstName +" "+ lastName);

				data.add(vo);
			}

		}catch(SQLException sqle) {
			log.error("could not select product notes ", sqle);
		}

		return data;
	}
	
	/**
	 * pulls every note with the accompanying market id
	 * @param marketId
	 * @return
	 */
	private List<NoteVO> getFullMarketList(String marketId) {
		StringBuilder sql = new StringBuilder(320);
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		List<NoteVO> data = new ArrayList<>();

		sql.append("select * from ").append((String)attributes.get("customDbSchema")).append("biomedgps_note n");
		sql.append("inner join ").append((String)attributes.get("customDbSchema")).append("BIOMEDGPS_USERS u on u.user_id = n.user_id ");
		sql.append("inner join PROFILE p  on p.profile_id = u.profile_id ");
		sql.append("where market_id = ? ");

		log.debug(sql.toString() +"|" + marketId);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, marketId);

			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				NoteVO vo = new NoteVO(rs);

				String firstName = pm.getStringValue("FIRST_NM", rs.getString("FIRST_NM"));
				String lastName = pm.getStringValue("LAST_NM", rs.getString("LAST_NM"));

				vo.setUserName(firstName +" "+ lastName);

				data.add(vo);
			}

		}catch(SQLException sqle) {
			log.error("could not select market notes ", sqle);
		}

		return data;
	}


	/**
	 * when called will return a map of note lists.  these lists will be keyed by 
	 * the team id or the attribute id, a list of all note with the primary id is also returned
	 * @param userId
	 * @param teams
	 * @param companyAttrIds
	 * @param companyId
	 * @return 
	 */
	public Map<String, List<NoteVO>> getCompanyNotes(String userId, List<String> teams, List<String> companyAttrIds, String companyId){
		log.debug("Notes Action get company notes called");

		Map<String, List<NoteVO>> noteResult = new HashMap<>();

		noteResult.put(NoteAction.ALL_NOTES, getFullCompanyList(companyId));
		
		for ( String team : teams){
			noteResult.put(team, processNotes(team, noteResult.get(NoteAction.ALL_NOTES), TEAM_TYPE, NoteTypes.COMPANY.name()));
		}
		for (String attribute:companyAttrIds ){
			noteResult.put(attribute, processNotes(attribute, noteResult.get(NoteAction.ALL_NOTES), ATTR_TYPE, NoteTypes.COMPANY.name()));
		}

		return noteResult;
	}
	
	/**
	 * when called will return a map of note lists.  these lists will be keyed by 
	 * the team id or the attribute id, a list of all note with the primary id is also returned
	 * @param userId
	 * @param teams
	 * @param companyAttrIds
	 * @param companyId
	 * @return 
	 */
	public Map<String, List<NoteVO>> getProductNotes(String userId, List<String> teams, List<String> productAttrIds, String productId){
		log.debug("Notes Action get product notes called");

		Map<String, List<NoteVO>> noteResult = new HashMap<>();

		noteResult.put(NoteAction.ALL_NOTES, getFullProductList(productId));
		
		for ( String team : teams){
			noteResult.put(team, processNotes(team, noteResult.get(NoteAction.ALL_NOTES), TEAM_TYPE, NoteTypes.PRODUCT.name()));
		}
		for (String attribute:productAttrIds ){
			noteResult.put(attribute, processNotes(attribute, noteResult.get(NoteAction.ALL_NOTES), ATTR_TYPE, NoteTypes.PRODUCT.name()));
		}

		return noteResult;
	}


	/**
	 * when called will return a map of note lists.  these lists will be keyed by 
	 * the team id or the attribute id, a list of all note with the primary id is also returned 
	 * @param userId
	 * @param teams
	 * @param companyAttrIds
	 * @param companyId
	 * @return 
	 */
	public Map<String, List<NoteVO>> getMarketNotes(String userId, List<String> teams, List<String> marketAttrIds, String marketId){
		log.debug("Notes Action get market notes called");

		Map<String, List<NoteVO>> noteResult = new HashMap<>();

		noteResult.put(NoteAction.ALL_NOTES, getFullMarketList(marketId));
		
		for ( String team : teams){
			noteResult.put(team, processNotes(team, noteResult.get(NoteAction.ALL_NOTES), TEAM_TYPE, NoteTypes.MARKET.name()));
		}
		for (String attribute:marketAttrIds ){
			noteResult.put(attribute, processNotes(attribute, noteResult.get(NoteAction.ALL_NOTES), ATTR_TYPE, NoteTypes.MARKET.name()));
		}

		return noteResult;
	}



}
