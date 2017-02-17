package com.biomed.smarttrak.action;

//java
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


//WC custom
import com.biomed.smarttrak.vo.NoteVO;
import com.biomed.smarttrak.vo.TeamVO;
import com.biomed.smarttrak.vo.UserVO;
//STM baselibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.exception.NotAuthorizedException;
import com.siliconmtn.http.session.SMTSession;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.file.transfer.ProfileDocumentAction;
import com.smt.sitebuilder.action.file.transfer.ProfileDocumentVO;
//WebCrescendo
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: NoteAction.java <p/>
 * <b>Project</b>: WC_Custom <p/>
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
public class NoteAction extends SBActionAdapter {

	public enum NoteType {
		COMPANY,
		PRODUCT,
		MARKET,
	}

	private static final String PRODUCT_ID = "productId" ;
	private static final String MARKET_ID = "marketId";
	private static final String COMPANY_ID = "companyId";
	private static final String ATTRIBUTE_ID = "attributeId";
	private static final String NOTE_TYPE = "noteType";
	private static final String NOTE_ENTITY_ID = "noteEntityId";
	private static final String CUSTOM_SCHEMA = "custom.";

	public NoteAction() {
		super();
	}

	public NoteAction(ActionInitVO arg0) {
		super(arg0);
	}

	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("Notes Action Retrieve called");

		SMTSession ses = req.getSession();
		UserVO uvo = (UserVO) ses.getAttribute(Constants.USER_DATA);

		//this section of the retrieve is used by file handler to process and send back
		//the correct vo

		if (!StringUtil.isEmpty(req.getParameter("profileDocumentId"))){
			String profileDocumentId = req.getParameter("profileDocumentId");

			try {
				if (isAuthorTeam(uvo,profileDocumentId)){

					ProfileDocumentAction pda = new ProfileDocumentAction();
					pda.setActionInit(actionInit);
					pda.setDBConnection(dbConn);
					pda.setAttributes(attributes);

					ProfileDocumentVO pvo = pda.getDocumentByProfileDocumentId(profileDocumentId);



					//will need a module data vo to send data back to the file handler
					ModuleVO modVo = new ModuleVO();
					modVo.setActionData(pvo);
					attributes.put(Constants.MODULE_DATA, modVo);
				}
			} catch (NotAuthorizedException e) {
				log.error("error in authorizing use of note ", e);
			}

		}else{

			String encKey = (String) getAttribute(Constants.ENCRYPT_KEY);
			String productId = StringUtil.checkVal(req.getParameter(PRODUCT_ID));
			String companyId = StringUtil.checkVal(req.getParameter(COMPANY_ID));
			String marketId = StringUtil.checkVal(req.getParameter(MARKET_ID));
			String attributeId = StringUtil.checkVal(req.getParameter(ATTRIBUTE_ID));
			String noteId = StringUtil.checkVal(req.getParameter("noteId"));
			String noteType = StringUtil.checkVal(req.getParameter(NOTE_TYPE));
			String noteEntityId = StringUtil.checkVal(req.getParameter(NOTE_ENTITY_ID));

			Date cal = Convert.formatDate(new Date(), Calendar.HOUR_OF_DAY, 3);

			try  {
				ModuleVO modVo = (ModuleVO) attributes.get(Constants.MODULE_DATA);
				StringEncrypter se = new StringEncrypter(encKey);

				String fileToken = se.encrypt(Long.toString(cal.getTime()));

				log.debug("file token " + fileToken);

				//if the request is for a particular note get that note
				if (!noteId.isEmpty()){
					//send the userId so we are sure the requester can see the note.
					NoteVO vo = getNote(noteId, uvo.getUserId());
					modVo.setActionData(vo);
				}

				//if there is an id for a list of notes ret that list of notes
				if (!productId.isEmpty() || !marketId.isEmpty()|| !companyId.isEmpty()){
					modVo.setActionData(refreshNoteList(productId, marketId,companyId, attributeId,ses));
				}

				modVo.setAttribute("noteToken", fileToken );
				modVo.setAttribute("filePrefix", attributes.get("smarttrakPathToBinary"));
				modVo.setAttribute("primaryId", setPrimaryId(productId, companyId, marketId, attributeId));
				modVo.setAttribute(NOTE_TYPE, noteType.isEmpty()? getNoteType(productId, companyId, marketId) : noteType);
				modVo.setAttribute(ATTRIBUTE_ID, attributeId);
				modVo.setAttribute(NOTE_ENTITY_ID, noteEntityId.isEmpty()? setEntityId(productId, companyId, marketId) : noteEntityId);
				attributes.put(Constants.MODULE_DATA, modVo);

			} catch (EncryptionException e) {
				log.error("error during string encryption " , e);
			}

		}

	}

	/**
	 * takes the profile document id and it back to the original note 
	 * this is done to ensure that whoever requests that file is the owner of the note or 
	 * on the team.
	 * @param uvo
	 * @param profileDocumentId
	 * @throws NotAuthorizedException 
	 */
	private boolean isAuthorTeam(UserVO uvo, String profileDocumentId) throws NotAuthorizedException {

		if (dbConn == null || uvo == null) {
			throw new NotAuthorizedException("NOT_AUTHORIZED - user null or data base connect null");
		}


		//this doesn't go through the standard path so attributes are not set here

		StringBuilder sb = new StringBuilder(181);
		sb.append("select n.user_id, n.team_id from profile_document pd ");
		sb.append("inner join ").append(CUSTOM_SCHEMA).append("biomedgps_note n ");
		sb.append("on n.note_id = pd.feature_id ");
		sb.append("where profile_document_id = ? ");

		log.debug("sql: " + sb.toString() +"|" + profileDocumentId );

		try (PreparedStatement ps = dbConn.prepareStatement(sb.toString())) {

			ps.setString(1, profileDocumentId);

			ResultSet rs = ps.executeQuery();

			if (rs.next()) {

				String userId = StringUtil.checkVal(rs.getString("user_id"));
				String teamId = StringUtil.checkVal(rs.getString("team_id"));

				if (!StringUtil.isEmpty(userId) && userId.equals(uvo.getUserId())){
					return true;
				}

				if(!StringUtil.isEmpty(teamId)&& uvo.getTeams() != null  && uvo.getTeams().contains(teamId)){
					return true;
				}

			}

		} catch(SQLException sqle) {
			log.error("could not confirm security by id ", sqle);
		}

		return false;
	}

	/**
	 * gets the id of the main object in this case the main company product or market
	 * @param productId
	 * @param companyId
	 * @param marketId
	 * @return
	 */
	private Object setEntityId(String productId, String companyId, String marketId) {
		return StringUtil.checkVal(productId, StringUtil.checkVal(companyId, marketId));
	}

	/**
	 * used the ids to tell which note type it is and mark it on the mod vo
	 * @param productId
	 * @param companyId
	 * @param marketId
	 * @return
	 */
	private String getNoteType(String productId, String companyId, String marketId) {
		if (!StringUtil.isEmpty(productId)) {
			return NoteType.PRODUCT.name();
		}
		if (!StringUtil.isEmpty(companyId)){
			return NoteType.COMPANY.name();
		}
		if (!StringUtil.isEmpty(marketId)){
			return NoteType.MARKET.name();
		}

		return null;
	}

	/**
	 * looks up a note by id and user
	 * @param noteId
	 * @param teams 
	 * @param userId 
	 * @return
	 */
	protected NoteVO getNote(String noteId, String userId) {

		StringBuilder sb = new StringBuilder(32);
		sb.append("select * from ").append((String)attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("biomedgps_note n ");
		sb.append("where note_id = ? and user_id = ?");

		log.debug(sb.toString() +"|" + noteId +"|"+ userId );

		try (PreparedStatement ps = dbConn.prepareStatement(sb.toString())) {

			ps.setString(1, noteId);
			ps.setString(2, userId);

			ResultSet rs = ps.executeQuery();
			ProfileDocumentAction pda = new ProfileDocumentAction();
			pda.setAttributes(attributes);
			pda.setDBConnection(dbConn);
			pda.setActionInit(actionInit);
			if (rs.next()) {

				NoteVO vo =  new NoteVO(rs);

				vo.setProfileDocuments(pda.getDocumentByFeatureId(vo.getNoteId()));

				return vo;
			}
		} catch(SQLException | ActionException sqle) {
			log.error("could not select notes by id ", sqle);
		}


		return null;
	}

	/**
	 * looks at the ids, and sets a primary id for use in the jsp files.  if there attr id exists
	 * it has priority.
	 * @param attributeId 
	 * @param marketId 
	 * @param companyId 
	 * @param productId 
	 * @return
	 */
	private String setPrimaryId(String productId, String companyId, String marketId, String attributeId) {
		if (StringUtil.isEmpty(attributeId)){

			return StringUtil.checkVal(productId, 
					StringUtil.checkVal(companyId, marketId));

		}
		return attributeId;
	}

	/**
	 * used to return a single directly requested list of notes
	 * @param productId
	 * @param marketId
	 * @param companyId
	 * @param attributeId
	 * @param ses2 
	 * @return
	 */
	private List<NoteVO> refreshNoteList(String productId, String marketId,	String companyId, String attributeId, SMTSession ses) {

		//in the generic note the key is the target id and the value is the note type
		GenericVO type = calculateNoteType(marketId, productId, companyId);

		UserVO uvo = (UserVO) ses.getAttribute(Constants.USER_DATA);
		//if no user return an empty list
		if (uvo == null){
			log.debug("no logged in user");
			return new ArrayList<>();
		}

		log.debug("teams=" + uvo.getTeams().size());
		log.debug("user id = " + uvo.getUserId());

		String userId = uvo.getUserId();
		List<String> teams = new ArrayList<>();

		for (TeamVO tvo : uvo.getTeams()){
			teams.add(tvo.getTeamId());
		}

		List<String> attributes = Arrays.asList(attributeId);
		List<String> targetIds = Arrays.asList((String)type.getKey());
		if (targetIds == null){
			return new ArrayList<>();
		}

		getNoteList(targetIds, NoteType.COMPANY, userId, teams);

		Map<String, List<NoteVO>> targetNotes = getNotes(userId, teams,attributes, targetIds, (NoteType)type.getValue() );		


		if (targetNotes != null && targetNotes.containsKey(attributes.get(0))) {
			log.debug("sending back a list of notes with the attribute id " + attributes.get(0));
			return targetNotes.get(attributes.get(0));
		}

		if (targetNotes != null && targetNotes.containsKey(targetIds.get(0)) && StringUtil.isEmpty(attributes.get(0))){
			log.debug("sending back a list of notes with the company id ");
			return targetNotes.get(targetIds.get(0));
		}

		log.debug("The target list was not located empty list returned");
		return new ArrayList<>();
	}

	/**
	 * looks at the ids sent and returns the correct note type
	 * @param marketId
	 * @param productId
	 * @param companyId
	 * @return
	 */
	private GenericVO calculateNoteType(String marketId, String productId,String companyId) {

		if (!StringUtil.isEmpty(productId, true))return new GenericVO(productId,NoteType.PRODUCT);
		if (!StringUtil.isEmpty(companyId, true))return new GenericVO(companyId,NoteType.COMPANY);
		else return new GenericVO(marketId,NoteType.MARKET);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.debug("Notes Action Build called");

		String noteType = StringUtil.checkVal(req.getParameter(NOTE_TYPE));
		String attributeId = StringUtil.checkVal(req.getParameter(ATTRIBUTE_ID));
		String noteEntityId = StringUtil.checkVal(req.getParameter(NOTE_ENTITY_ID));


		DBProcessor db = new DBProcessor(dbConn, (String) attributes.get(Constants.CUSTOM_DB_SCHEMA));
		NoteVO vo= new NoteVO(req);

		SMTSession ses = req.getSession();
		UserVO uvo = (UserVO) ses.getAttribute(Constants.USER_DATA);
		log.debug("user id = " + uvo.getUserId());

		vo.setUserId(uvo.getUserId());

		log.debug("companyId " + vo.getCompanyId());

		setTargetId(req, vo);

		//if a user decided to not share the note, then the team id is set to null 
		//  to stop everyone else in the same team from seeing it
		if ("user".equalsIgnoreCase(vo.getTeamId().toLowerCase())){
			vo.setTeamId(null);
		}

		ModuleVO modVo = (ModuleVO) attributes.get(Constants.MODULE_DATA);

		if(req.hasParameter("isDelete")) {
			if (!StringUtil.isEmpty(vo.getNoteId())) {

				ProfileDocumentAction pda = new ProfileDocumentAction();
				pda.setAttributes(attributes);
				pda.setDBConnection(dbConn);
				pda.setActionInit(actionInit);
				//goes looking for documents to delete
				vo.setProfileDocuments(pda.getDocumentByFeatureId(vo.getNoteId()));
				//deletes the note
				deleteNote(vo, db);	
				
				//if there are files to delete remove then and they document record
				if (vo.getProfileDocuments() != null && vo.getProfileDocuments().size() >0){
					for (ProfileDocumentVO pvo : vo.getProfileDocuments()){
						pda.delete(pvo);
					}

				}

			}

		} else {		
			log.debug("save note with id: " + vo.getNoteId() + " is it savable " + vo.isNoteSaveable() );
			if (vo.isNoteSaveable()) {
				saveNote(vo, db);
			}
			vo.setNoteId(db.getGeneratedPKId());

			if(!StringUtil.isEmpty(vo.getFilePathText())){
				processProfileDocumentCreation(vo, req, uvo.getProfileId());
			}


			modVo.setAttribute("newNoteId", vo.getNoteId() );
			modVo.setAttribute("newNote", vo);
			log.debug("added new note " + vo);
		}

		modVo.setAttribute(NOTE_TYPE, noteType);
		modVo.setAttribute(ATTRIBUTE_ID, attributeId);
		modVo.setAttribute(NOTE_ENTITY_ID, noteEntityId);
		attributes.put(Constants.MODULE_DATA, modVo);
	}

	/**
	 * this method will make and save a profile document entry for the new note.
	 * @param vo
	 * @param req
	 * @param profileId 
	 */
	private void processProfileDocumentCreation(NoteVO vo, ActionRequest req, String profileId) {
		log.debug("process profile document creation called ");
		ProfileDocumentAction pda = new ProfileDocumentAction();
		pda.setAttributes(attributes);
		pda.setDBConnection(dbConn);
		pda.setActionInit(actionInit);

		String orgId = ((SiteVO)req.getAttribute(Constants.SITE_DATA)).getOrganizationId();

		req.setParameter("profileId", profileId);
		req.setParameter("featureId", vo.getNoteId());
		req.setParameter("organizationId", orgId);
		req.setParameter("actionId", actionInit.getActionId());

		try {
			pda.build(req);
		} catch (ActionException e) {
			log.error("error occcured during profile document generation " , e);
		}
	}

	/**
	 * looks for the different params on the request and sets the id needed
	 * 
	 * @param req
	 * @param vo 
	 */
	private void setTargetId(ActionRequest req, NoteVO vo) {

		if (!StringUtil.checkVal(req.getParameter(COMPANY_ID)).isEmpty()){
			vo.setCompanyId(StringUtil.checkVal(req.getParameter(COMPANY_ID)));
		}else if (!StringUtil.checkVal(req.getParameter(PRODUCT_ID)).isEmpty()){
			vo.setProductId(StringUtil.checkVal(req.getParameter(PRODUCT_ID)));
		}else if (!StringUtil.checkVal(req.getParameter(MARKET_ID)).isEmpty()){
			vo.setMarketId(StringUtil.checkVal(req.getParameter(MARKET_ID)));
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
			db.save(vo);
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
			db.delete(vo);
		}catch(Exception e) {
			throw new ActionException(e);
		}
	}

	/**
				if( vo.getProfileDocuments() != null){
					for(ProfileDocumentVO pvo : vo.getProfileDocuments()){

					}
				}
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

		sb.append(") and (n.EXPIRATION_DT > CURRENT_TIMESTAMP or n.EXPIRATION_DT is null) ");

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
	 * pulls every note with the accompanying  id
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

		sql.append("select * from ").append((String)attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("biomedgps_note n ");
		sql.append("inner join ").append((String)attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("BIOMEDGPS_USER u on u.user_id = n.user_id ");
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

			ProfileDocumentAction pda = new ProfileDocumentAction();
			pda.setActionInit(actionInit);
			pda.setDBConnection(dbConn);
			pda.setAttributes(attributes);

			while (rs.next()) {

				NoteVO vo = new NoteVO(rs);
				String firstName = pm.getStringValue("FIRST_NM", rs.getString("FIRST_NM"));
				String lastName = pm.getStringValue("LAST_NM", rs.getString("LAST_NM"));

				vo.setUserName(firstName +" "+ lastName);

				vo.setProfileDocuments(pda.getDocumentByFeatureId(vo.getNoteId()));

				processNote(noteType, data, vo);
			}

		}catch(SQLException | ActionException sqle) {
			log.error("could not select notes ", sqle);
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
	 * allows the call with a single id, wraps string in a list and forwards to other message signature
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
	 * allows the call with a single id, wraps string in a list and forwards to other message signature
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
	 * allows the call with a single id, wraps string in a list and forwards to other message signature
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
