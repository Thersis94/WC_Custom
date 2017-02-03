package com.biomed.smarttrak.action;

//java
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


//WC custom
import com.biomed.smarttrak.vo.NoteVO;

//STM baselibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

//WebCrescendo
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: NoteAction.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> can be used to request a map of list of notes, and build can be called to 
 * add update or delete a note.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Ryan Riker
 * @version 2.0
 * @since Jan 24, 2017<p/>
 * @updates:
 ****************************************************************************/
public class NoteAction extends SimpleActionAdapter {

	public enum NoteType {
		COMPANY,
		PRODUCT,
		MARKET,

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
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("Notes Action Build called");
		DBProcessor db = new DBProcessor(dbConn, (String) attributes.get(Constants.CUSTOM_DB_SCHEMA));
		NoteVO vo= new NoteVO(req);

		if(req.hasParameter("isDelete")) {
			deleteNote(vo, db);	
		} else {			
			saveNote(vo, db);
		}

	}

	/**
	 * inserts or updates a note
	 * @param vo
	 * @param db2 
	 * @throws ActionException 
	 */
	private void saveNote(NoteVO vo, DBProcessor db) throws ActionException {
		log.debug("Notes Action insert note called ");

		try {
			if (StringUtil.isEmpty(vo.getNoteId())) {
				vo.setNoteId(new UUIDGenerator().getUUID());
				log.debug("inserting new note with id: " + vo.getNoteId() + " is it savable " + vo.isNoteSaveable() );
				if (vo.isNoteSaveable()) {
					db.insert(vo);
				}

			} else {
				log.debug("updating note with id: " + vo.getNoteId() + " is it savable " + vo.isNoteSaveable() );
				if (vo.isNoteSaveable()) {
					db.update(vo);
				}
			}
		} catch (Exception e) {
			throw new ActionException(e);
		}

	}

	/**
	 * deletes a note
	 * @param req
	 * @throws ActionException 
	 */
	private void deleteNote(NoteVO vo, DBProcessor db) throws ActionException {
		log.debug("Notes Action delete note called");

		try {
			if (!StringUtil.isEmpty(vo.getNoteId())) {
				db.delete(vo);
			}

		}catch(Exception e) {
			throw new ActionException(e);
		}
	}

	/**
	 * based on the note request type and the size of the attributes list returns the where clause.
	 * @param vo
	 * @param noteRequestType
	 * @return
	 */
	private String getWhereSql(List<String> targetIds, List<String> teams, NoteType type) {
		StringBuilder sb = new StringBuilder(90);

		switch(type) {
		case COMPANY :
			sb.append("where company_id in (? ");
			appendSqlPlaceholder(targetIds.size(), sb);
			break;
		case PRODUCT :
			sb.append("where product_id in (? ");
			appendSqlPlaceholder(targetIds.size(), sb);
			break;
		case MARKET :
			sb.append("where market_id in (? ");
			appendSqlPlaceholder(targetIds.size(), sb);
			break;
		}

		sb.append("and (n.user_id = ? ");

		if (teams != null && !teams.isEmpty()){
			sb.append("or  n.team_id in (?");
			appendSqlPlaceholder(teams.size(), sb);
		}

		sb.append(") and (EXPIRATION_DT > CURRENT_TIMESTAMP or EXPIRATION_DT is null) ");

		return sb.toString();
	}

	/**
	 * used to place the correct number of commas and question marks in the prepared statement
	 * @param listSize
	 * @param sb 
	 * @return
	 */
	private void appendSqlPlaceholder(int listSize, StringBuilder sb) {
		for (int x = 0 ; x < listSize-1; x++ ){
			sb.append(", ?");
		}
		sb.append(") ");
	}


	/**
	 * based on note type returns the correct method call.
	 * @param na
	 * @param type
	 * @param targetIds 
	 * @return
	 */
	public Map<String, List<NoteVO>> getNotes(String userId, List<String> teams,List<String> attributeIds, List<String> targetIds, NoteType type ) {

		switch(type) {
		case COMPANY :
			return this.getCompanyNotes(userId, teams, attributeIds, targetIds);
		case PRODUCT :
			return this.getProductNotes(userId, teams, attributeIds, targetIds);
		case MARKET :
			return this.getMarketNotes(userId, teams, attributeIds, targetIds);
		default :
			return null;
		}
	}

	/**
	 * pulls every note with the accompanying company id
	 * @param companyId 
	 * @param companyAttrIds 
	 * @param teams 
	 * @param userId 
	 * @param company 
	 * @return
	 */
	private Map<String, List<NoteVO>> getNoteList(List<String> targetIds, NoteType noteType, String userId, List<String> teams) {

		StringBuilder sql = new StringBuilder(207);
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		Map<String, List<NoteVO>> data = new HashMap<>();

		sql.append("select * from ").append((String)attributes.get("customDbSchema")).append("biomedgps_note n ");
		sql.append("inner join ").append((String)attributes.get("customDbSchema")).append("BIOMEDGPS_USER u on u.user_id = n.user_id ");
		sql.append("inner join PROFILE p  on p.profile_id = u.profile_id ");

		sql.append(getWhereSql(targetIds, teams, noteType));

		log.debug(sql.toString() +"|" + targetIds +"|"+ userId );

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			int i = 1;

			if (targetIds != null){
				for (String targetId : targetIds){
					ps.setString(i++, targetId);
				}
			}

			ps.setString(i++, userId);

			if (teams != null){
				for (String team : teams){
					ps.setString(i++, team);
				}
			}

			log.debug("prepared statment has: " + (i-1) +" variables ");

			ResultSet rs = ps.executeQuery();

			while (rs.next()) {

				NoteVO vo = new NoteVO(rs);

				String firstName = pm.getStringValue("FIRST_NM", rs.getString("FIRST_NM"));
				String lastName = pm.getStringValue("LAST_NM", rs.getString("LAST_NM"));

				vo.setUserName(firstName +" "+ lastName);

				processNote(noteType, data, vo);
			}

		}catch(SQLException sqle) {
			log.error("could not select company notes ", sqle);
		}

		log.debug("data size " + data.size());
		return data;
	}


	/**
	 * looks at the note type and the current map and places the note vo on the correct list.
	 * @param noteType
	 * @param data
	 * @param vo
	 */
	private void processNote(NoteType noteType, Map<String, List<NoteVO>> data, NoteVO vo) {

		String targetKey = null;

		switch(noteType) {
		case COMPANY :
			targetKey = vo.getCompanyId();
			break;
		case PRODUCT :
			targetKey = vo.getProductId();
			break;
		case MARKET :
			targetKey = vo.getMarketId();
			break;
		}

		if(vo.getAttributeId() != null && !vo.getAttributeId().isEmpty()){
			targetKey = vo.getAttributeId();
		}

		List<NoteVO> lvo;

		if (data.containsKey(targetKey)){
			lvo = data.get(targetKey);
			lvo.add(vo);
		}else {
			lvo = new ArrayList<>();
			lvo.add(vo);
			data.put(targetKey, lvo);
		}

	}

	/**
	 * allows the call with a single id
	 * @param userId
	 * @param teams
	 * @param marketAttrIds
	 * @param marketId
	 * @return
	 */
	public Map<String, List<NoteVO>> getCompanyNotes(String userId, List<String> teams, List<String> companyAttrIds, String companyId){
		List<String> companyIds = new ArrayList<>();
		companyIds.add(companyId);

		return getCompanyNotes(userId, teams, companyAttrIds, companyIds);

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
	public Map<String, List<NoteVO>> getCompanyNotes(String userId, List<String> teams, List<String> companyAttrIds, List<String> companyIds){
		log.debug("Notes Action get company notes called");

		return getNoteList(companyIds, NoteType.COMPANY, userId, teams);


	}

	/**
	 * allows the call with a single id
	 * @param userId
	 * @param teams
	 * @param marketAttrIds
	 * @param marketId
	 * @return
	 */
	public Map<String, List<NoteVO>> getProductNotes(String userId, List<String> teams, List<String> productAttrIds, String productId){
		List<String> productIds = new ArrayList<>();
		productIds.add(productId);

		return getProductNotes(userId, teams, productAttrIds, productIds);

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
	public Map<String, List<NoteVO>> getProductNotes(String userId, List<String> teams, List<String> productAttrIds, List<String> productIds){
		log.debug("Notes Action get product notes called");

		return getNoteList(productIds, NoteType.PRODUCT, userId, teams);
	}


	/**
	 * allows the call with a single id
	 * @param userId
	 * @param teams
	 * @param marketAttrIds
	 * @param marketId
	 * @return
	 */
	public Map<String, List<NoteVO>> getMarketNotes(String userId, List<String> teams, List<String> marketAttrIds, String marketId){
		List<String> marketIds = new ArrayList<>();
		marketIds.add(marketId);

		return getMarketNotes(userId, teams, marketAttrIds, marketIds);

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
	public Map<String, List<NoteVO>> getMarketNotes(String userId, List<String> teams, List<String> marketAttrIds, List<String> marketIds){
		log.debug("Notes Action get market notes called");

		return getNoteList(marketIds, NoteType.MARKET, userId, teams);

	}



}
