package com.biomed.smarttrak.action;

//java
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.biomed.smarttrak.security.SmarttrakRoleVO;
//WC custom
import com.biomed.smarttrak.vo.NoteVO;
import com.biomed.smarttrak.vo.TeamVO;
import com.biomed.smarttrak.vo.UserVO;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
//STM baselibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.exception.NotAuthorizedException;
import com.siliconmtn.http.filter.fileupload.ProfileDocumentFileManagerStructureImpl;
import com.siliconmtn.http.session.SMTSession;
import com.siliconmtn.security.EncryptionException;
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
		ACCOUNT
	}

	private static final String PRODUCT_ID = "productId";
	private static final String MARKET_ID = "marketId";
	private static final String COMPANY_ID = "companyId";
	private static final String ATTRIBUTE_ID = "attributeId";
	private static final String NOTE_TYPE = "noteType";
	private static final String NOTE_ENTITY_ID = "noteEntityId";
	public static final String NOTES_DIRECTORY_PATH = "/note";
	private static final String ID_NOTE_LIST = "#notes-list-";
	private static final String NOTE_TABLE = "biomedgps_note n ";

	public NoteAction() {
		super();
	}

	public NoteAction(ActionInitVO arg0) {
		super(arg0);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
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
		UserVO user = (UserVO) ses.getAttribute(Constants.USER_DATA);

		//this section of the retrieve is used by file handler to process and send back the correct vo
		if (!StringUtil.isEmpty(req.getParameter("profileDocumentId"))) {
			String profileDocumentId = req.getParameter("profileDocumentId");
			processProfileDocumentRequest(profileDocumentId, user, ses.getAttribute(Constants.ROLE_DATA));
		} else {
			if (Convert.formatBoolean(req.getParameter("loadCount"))) {
				processNoteCounts(req);
			} else {
				processNoteRetrieve(ses, user, req);
			}
		}
	}


	/**
	 * gets the list of notes in each note group and sets a json string to the action data for front end placement
	 * @param req 
	 * @throws ActionException 
	 */
	private void processNoteCounts(ActionRequest req) throws ActionException {
		log.debug("loading count");

		//if they are not searching for anything return an empty map
		JsonParser jsonParser = new JsonParser();
		JsonArray ja = (JsonArray)jsonParser.parse(req.getParameter("notesGroups"));
		if (ja.size() == 0) {
			putModuleData(Collections.emptyMap());
			return;
		}

		//if no user return an empty list
		SMTSession ses = req.getSession();
		UserVO user = (UserVO) ses.getAttribute(Constants.USER_DATA);
		if (user == null) {
			log.debug("no logged in user");
			putModuleData(Collections.emptyMap());
			return;
		}

		//used to hold all the ids so Zeros are recorded
		List<String> params = new ArrayList<>();
		Map<String, Integer> counts = new HashMap<>();

		//this query was not compatible with dp processor, when the vo is annotated the count column 
		//threw an exception or didnt fill in the vo.  
		int x=1;
		try (PreparedStatement ps = dbConn.prepareStatement(getCountSql(ja, params,user.getTeams()))) {
			for (String id : params)
				ps.setString(x++, id);
			ps.setString(x++, user.getUserId());
			for ( TeamVO tvo : user.getTeams())
				ps.setString(x++, tvo.getTeamId());
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				processResults(rs, counts);

		} catch (SQLException e) {
			throw new ActionException(e);
		}

		processZeroIds(counts, params);
		putModuleData(counts);
	}


	/**
	 * adds zero counts for ids 
	 * @param counts 
	 * @param params 
	 * 
	 */
	private void processZeroIds(Map<String, Integer> counts, List<String> params) {
		for (String id : params) {
			String key= ID_NOTE_LIST+ id;
			if (!counts.containsKey(key))
				counts.put(key, 0);
		}
	}


	/**
	 * adds the correct key and count to the map so the front end can place the data in the correct div
	 * @param counts 
	 * @param rs 
	 * @throws SQLException 
	 * 
	 */
	private void processResults(ResultSet rs, Map<String, Integer> counts) throws SQLException {
		String key = "";
		if (!StringUtil.isEmpty(rs.getString("attribute_id"))) {
			key = ID_NOTE_LIST+ rs.getString("attribute_id");
		} else if (!StringUtil.isEmpty(rs.getString("market_id"))) {
			key = ID_NOTE_LIST+ rs.getString("market_id");
		} else if (!StringUtil.isEmpty(rs.getString("company_id"))) {
			key = ID_NOTE_LIST+ rs.getString("company_id");
		} else if (!StringUtil.isEmpty(rs.getString("product_id"))) {
			key = ID_NOTE_LIST+ rs.getString("product_id");
		}
		log.debug("key: " + key + "  value: " +Convert.formatInteger(rs.getString("LIST_COUNT")) );
		counts.put(key, Convert.formatInteger(rs.getString("LIST_COUNT")));

	}


	/**
	 * generates the sql for getting a count of notes 
	 * @param params 
	 * @param ja 
	 * @param list 
	 * @return
	 */
	private String getCountSql(JsonArray ja, List<String> params, List<TeamVO> teams) {
		StringBuilder countSql = new StringBuilder(100);
		countSql.append("select count(*) as list_count, COMPANY_ID, MARKET_ID, PRODUCT_ID, ATTRIBUTE_ID from ");
		countSql.append(getAttribute(Constants.CUSTOM_DB_SCHEMA)).append("biomedgps_note ");
		countSql.append("where ");

		boolean firstId = true;
		for (JsonElement jo : ja) {
			if (firstId) {
				countSql.append(" ( ");
				firstId = false;
			} else {
				countSql.append("or ( ");
			}

			if (!jo.getAsJsonObject().get(COMPANY_ID).getAsString().isEmpty()) {
				countSql.append("company_id = ? and attribute_id ");
				params.add(jo.getAsJsonObject().get(COMPANY_ID).getAsString());
			} else if (!jo.getAsJsonObject().get(PRODUCT_ID).getAsString().isEmpty()) {
				countSql.append("product_id = ? and attribute_id ");
				params.add(jo.getAsJsonObject().get(PRODUCT_ID).getAsString());
			} else if (!jo.getAsJsonObject().get(MARKET_ID).getAsString().isEmpty()) {
				countSql.append("market_id = ? and attribute_id ");
				params.add(jo.getAsJsonObject().get(MARKET_ID).getAsString());
			}

			if (jo.getAsJsonObject().get(ATTRIBUTE_ID)!= null && !jo.getAsJsonObject().get(ATTRIBUTE_ID).getAsString().isEmpty()){
				countSql.append("= ? ) ");
				params.add(jo.getAsJsonObject().get(ATTRIBUTE_ID).getAsString());
			} else {
				countSql.append("is null ) ");
			}
		}

		countSql.append("and (user_id = ? or team_id in ( ");
		DBUtil.preparedStatmentQuestion(teams.size(), countSql);
		countSql.append(")) ");

		countSql.append("group by company_id, market_id, product_id, attribute_id ");
		countSql.append("order by list_count desc");
		log.debug("count sql " + countSql.toString());
		return countSql.toString();
	}


	/**
	 * processes a request for notes
	 * @param ses 
	 * @param filePrefix 
	 * @param user 
	 * @param req 
	 */
	protected void processNoteRetrieve(SMTSession ses, UserVO user, ActionRequest req) {
		String productId = StringUtil.checkVal(req.getParameter(PRODUCT_ID));
		String companyId = StringUtil.checkVal(req.getParameter(COMPANY_ID));
		String marketId = StringUtil.checkVal(req.getParameter(MARKET_ID));
		String attributeId = StringUtil.checkVal(req.getParameter(ATTRIBUTE_ID));
		String noteId = StringUtil.checkVal(req.getParameter("noteId"));
		String noteType = StringUtil.checkVal(req.getParameter(NOTE_TYPE));
		String noteEntityId = StringUtil.checkVal(req.getParameter(NOTE_ENTITY_ID));

		ModuleVO modVo = (ModuleVO) getAttribute(Constants.MODULE_DATA);

		//if the request is for a particular note get that note
		if (!noteId.isEmpty()) {
			//send the userId so we are sure the requester can see the note.
			NoteVO vo = getNote(noteId, user.getUserId());
			modVo.setActionData(vo);
		}
		//if there is an id for a list of notes ret that list of notes
		if (!productId.isEmpty() || !marketId.isEmpty()|| !companyId.isEmpty()) {
			modVo.setActionData(refreshNoteList(productId, marketId,companyId, attributeId,ses));
		}
		setupAttributes(modVo);
		modVo.setAttribute("primaryId", setPrimaryId(productId, companyId, marketId, attributeId));
		modVo.setAttribute(NOTE_TYPE, noteType.isEmpty()? getNoteType(productId, companyId, marketId) : noteType);
		modVo.setAttribute(ATTRIBUTE_ID, attributeId);
		modVo.setAttribute(NOTE_ENTITY_ID, noteEntityId.isEmpty()? setEntityId(productId, companyId, marketId) : noteEntityId);
		setAttribute(Constants.MODULE_DATA, modVo);
	}


	/**
	 * processes a request for a profile document
	 * @param user 
	 * @param profileDocumentId 
	 * @throws ActionException 
	 */
	private void processProfileDocumentRequest(String profileDocumentId, UserVO user, Object roleVo) throws ActionException {
		SmarttrakRoleVO role = (SmarttrakRoleVO) roleVo;
		try {
			if (isAuthorTeam(user,profileDocumentId, role)) {
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
	}


	/**
	 * takes the profile document id and it back to the original note 
	 * this is done to ensure that whoever requests that file is the owner of the note or 
	 * on the team.
	 * @param user
	 * @param profileDocumentId
	 * @throws NotAuthorizedException 
	 */
	private boolean isAuthorTeam(UserVO user, String profileDocumentId, SmarttrakRoleVO role) throws NotAuthorizedException {
		if (user == null)
			throw new NotAuthorizedException("NOT_AUTHORIZED - user not logged in");

		//if the user role is Staff or Site Administrator they are authorized.  This is used by Account Notes inside "/manage"
		if (AdminControllerAction.STAFF_ROLE_LEVEL <= role.getRoleLevel()) return true;

		//this doesn't go through the standard path so attributes are not set here
		StringBuilder sb = new StringBuilder(150);
		sb.append("select n.user_id, n.team_id from profile_document pd ");
		sb.append("inner join ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA)).append(NOTE_TABLE);
		sb.append("on n.note_id = pd.feature_id ");
		sb.append("where profile_document_id = ? ");
		log.debug("sql: " + sb +"|" + profileDocumentId );

		try (PreparedStatement ps = dbConn.prepareStatement(sb.toString())) {
			ps.setString(1, profileDocumentId);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				return processAuthorizationResults(rs, user);

		} catch(SQLException sqle) {
			log.error("could not confirm security by id ", sqle);
		}

		return false;
	}


	/**
	 * processes the teams and ids for note profile document interaction
	 * @param rs
	 * @param user 
	 * @return 
	 * @throws SQLException 
	 */
	private boolean processAuthorizationResults(ResultSet rs, UserVO user) throws SQLException {
		String userId = StringUtil.checkVal(rs.getString("user_id"));
		String teamId = StringUtil.checkVal(rs.getString("team_id"));

		if (userId.equals(user.getUserId()))
			return true;

		if (user.getTeams() == null)
			return false;

		for ( TeamVO team :user.getTeams()){
			if (teamId.equals(team.getTeamId())){
				return true;
			}
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
		} else if (!StringUtil.isEmpty(companyId)){
			return NoteType.COMPANY.name();
		} else if (!StringUtil.isEmpty(marketId)){
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
		StringBuilder sb = new StringBuilder(100);
		sb.append("select * from ").append((String)getAttribute(Constants.CUSTOM_DB_SCHEMA)).append(NOTE_TABLE);
		sb.append("where note_id = ? and user_id = ?");
		log.debug(sb +"|" + noteId +"|"+ userId );

		ProfileDocumentAction pda = new ProfileDocumentAction();
		pda.setAttributes(attributes);
		pda.setDBConnection(dbConn);
		pda.setActionInit(actionInit);
		try (PreparedStatement ps = dbConn.prepareStatement(sb.toString())) {
			ps.setString(1, noteId);
			ps.setString(2, userId);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				NoteVO vo =  new NoteVO(rs);
				vo.setProfileDocuments(pda.getDocumentByFeatureId(vo.getNoteId()));
				return vo;
			}
		} catch(SQLException sqle) {
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
		if (!StringUtil.isEmpty(attributeId)) return attributeId;
		return StringUtil.checkVal(productId,  StringUtil.checkVal(companyId, marketId));
	}


	/**
	 * used to return a single directly requested list of notes.
	 * in the generic note the key is the target id and the value is the note type.
	 * @param productId
	 * @param marketId
	 * @param companyId
	 * @param attributeId
	 * @param ses2 
	 * @return
	 */
	private List<NoteVO> refreshNoteList(String productId, String marketId,	String companyId, String attributeId, SMTSession ses) {
		GenericVO type = calculateNoteType(marketId, productId, companyId);
		log.debug("note type: " + type);
		UserVO user = (UserVO) ses.getAttribute(Constants.USER_DATA);
		//if no user return an empty list
		if (user == null) {
			log.debug("no logged in user");
			return Collections.emptyList();
		}

		log.debug("teams=" + user.getTeams().size());
		log.debug("user id = " + user.getUserId());

		String userId = user.getUserId();
		List<String> teams = new ArrayList<>();

		for (TeamVO tvo : user.getTeams())
			teams.add(tvo.getTeamId());

		List<String> attrs = Arrays.asList(attributeId);
		List<String> targetIds = Arrays.asList((String)type.getKey());
		if (targetIds == null)
			return Collections.emptyList();

		getNoteList(targetIds, NoteType.COMPANY, userId, teams);
		Map<String, List<NoteVO>> targetNotes = getNotes(userId, teams, targetIds, (NoteType)type.getValue());		

		if (targetNotes != null && targetNotes.containsKey(attrs.get(0))) {
			log.debug("sending back a list of notes with the attribute id " + attrs.get(0));
			return targetNotes.get(attrs.get(0));
		}

		if (targetNotes != null && targetNotes.containsKey(targetIds.get(0)) && StringUtil.isEmpty(attrs.get(0))) {
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
		if (!StringUtil.isEmpty(productId)) {
			return new GenericVO(productId,NoteType.PRODUCT);
		} else if (!StringUtil.isEmpty(companyId)) {
			return new GenericVO(companyId,NoteType.COMPANY);
		} else { 
			return new GenericVO(marketId,NoteType.MARKET);
		}
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.debug("Notes Action Build called");
		ModuleVO modVo = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		String noteType = StringUtil.checkVal(req.getParameter(NOTE_TYPE));
		String attributeId = StringUtil.checkVal(req.getParameter(ATTRIBUTE_ID));
		String noteEntityId = StringUtil.checkVal(req.getParameter(NOTE_ENTITY_ID));
		DBProcessor db = new DBProcessor(dbConn, (String) getAttribute(Constants.CUSTOM_DB_SCHEMA));
		NoteVO vo= new NoteVO(req);

		SMTSession ses = req.getSession();
		UserVO user = (UserVO) ses.getAttribute(Constants.USER_DATA);
		log.debug("user id = " + user.getUserId());
		vo.setUserId(user.getUserId());

		setTargetId(req, vo);

		//if a user decided to not share the note, then the team id is set to null 
		//  to stop everyone else in the same team from seeing it
		if ("user".equalsIgnoreCase(vo.getTeamId()))
			vo.setTeamId(null);

		if (req.hasParameter("isDelete")) {
			if (!StringUtil.isEmpty(vo.getNoteId())) {
				ProfileDocumentAction pda = new ProfileDocumentAction();
				pda.setAttributes(attributes);
				pda.setDBConnection(dbConn);
				pda.setActionInit(actionInit);
				//goes looking for documents to delete
				vo.setProfileDocuments(pda.getDocumentByFeatureId(vo.getNoteId()));
				//deletes the note
				deleteNote(vo, db);	

				//if there are files to delete remove then and their document record
				if (vo.getProfileDocuments() != null && vo.getProfileDocuments().isEmpty()){
					deleteProfileDocuments(pda , vo.getProfileDocuments());
				}
			}

		} else {		
			log.debug("save note with id: " + vo.getNoteId() + " is it savable " + vo.isNoteSaveable());
			if (vo.isNoteSaveable()) {
				saveNote(vo, db);
			}

			if (!StringUtil.isEmpty(vo.getFilePathText()))
				processProfileDocumentCreation(vo, req, user.getProfileId());

			modVo.setAttribute("newNoteId", vo.getNoteId());
			modVo.setAttribute("newNote", vo);
			log.debug("added new note " + vo);
		}

		modVo.setAttribute(NOTE_TYPE, noteType);
		modVo.setAttribute(ATTRIBUTE_ID, attributeId);
		modVo.setAttribute(NOTE_ENTITY_ID, noteEntityId);
		attributes.put(Constants.MODULE_DATA, modVo);
	}


	/**
	 * loops the profile documemts deleting each one from the file system.
	 * @param pda 
	 * @param profileDocuments
	 * @throws ActionException 
	 */
	protected void deleteProfileDocuments(ProfileDocumentAction pda, List<ProfileDocumentVO> profileDocuments) throws ActionException {
		for (ProfileDocumentVO pvo : profileDocuments)
			pda.deleteFileFromDisk(pvo);
	}


	/**
	 * this method will make and save a profile document entry for the new note.
	 * @param vo
	 * @param req
	 * @param profileId 
	 */
	protected void processProfileDocumentCreation(NoteVO vo, ActionRequest req, String profileId) {
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
			log.error("error occured during profile document generation " , e);
		}
	}


	/**
	 * looks for the different params on the request and sets the id needed
	 * 
	 * @param req
	 * @param vo 
	 */
	private void setTargetId(ActionRequest req, NoteVO vo) {
		if (!StringUtil.isEmpty(req.getParameter(COMPANY_ID))) {
			vo.setCompanyId(StringUtil.checkVal(req.getParameter(COMPANY_ID)));
		} else if (!StringUtil.isEmpty(req.getParameter(PRODUCT_ID))) {
			vo.setProductId(StringUtil.checkVal(req.getParameter(PRODUCT_ID)));
		} else if (!StringUtil.isEmpty(req.getParameter(MARKET_ID))) {
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
		} catch(Exception e) {
			throw new ActionException(e);
		}
	}


	/**
	 * based on the note request type and the size of the attributes list returns the where clause.
	 * @param vo
	 * @param noteRequestType
	 * @return
	 */
	private void addWhereClause(StringBuilder sb, List<String> targetIds, List<String> teams, NoteType type) {
		switch(type) {
			case COMPANY :
				sb.append("where n.company_id in (");
				DBUtil.preparedStatmentQuestion(targetIds.size(), sb);
				sb.append(") ");
				break;
			case PRODUCT :
				sb.append("where n.product_id in (");
				DBUtil.preparedStatmentQuestion(targetIds.size(), sb);
				sb.append(") ");
				break;
			case MARKET :
				sb.append("where n.market_id in (");
				DBUtil.preparedStatmentQuestion(targetIds.size(), sb);
				sb.append(") ");
				break;
			case ACCOUNT:
				sb.append("where n.account_id in (");
				DBUtil.preparedStatmentQuestion(targetIds.size(), sb);
				sb.append(") order by n.create_dt desc");
				return;
			default:
		}

		sb.append("and (n.user_id = ? ");

		if (teams != null && !teams.isEmpty()) {
			sb.append("or n.team_id in (");
			DBUtil.preparedStatmentQuestion(teams.size(), sb);
			sb.append(") ");
		}

		sb.append(") and (n.EXPIRATION_DT > CURRENT_TIMESTAMP or n.EXPIRATION_DT is null) ");
	}


	/**
	 * based on note type returns the correct method call.
	 * @param na
	 * @param type
	 * @param targetIds 
	 * @return
	 */
	public Map<String, List<NoteVO>> getNotes(String userId, List<String> teams, List<String> targetIds, NoteType type) {
		switch(type) {
			case COMPANY :
				return this.getCompanyNotes(userId, teams, targetIds);
			case PRODUCT :
				return this.getProductNotes(userId, teams, targetIds);
			case MARKET :
				return this.getMarketNotes(userId, teams, targetIds);
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
		Map<String, List<NoteVO>> data = new HashMap<>();
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		ProfileDocumentAction pda = new ProfileDocumentAction();
		pda.setActionInit(actionInit);
		pda.setDBConnection(dbConn);
		pda.setAttributes(attributes);

		String custom = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(207);
		sql.append("select n.*, u.user_id, p.profile_id, p.first_nm, p.last_nm from ").append(custom).append(NOTE_TABLE);
		sql.append("inner join ").append(custom).append("BIOMEDGPS_USER u on u.user_id = n.user_id ");
		sql.append("inner join PROFILE p  on p.profile_id = u.profile_id ");
		addWhereClause(sql, targetIds, teams, noteType);
		log.debug(sql.toString() +"|" + targetIds +"|"+ userId );

		int i = 1;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			if (targetIds != null) {
				for (String targetId : targetIds) {
					ps.setString(i++, targetId);
				}
			}
			if (NoteType.ACCOUNT != noteType) //account notes is an admin (/manage) function.  We do not filter by user
				ps.setString(i++, userId);

			if (teams != null) {
				for (String team : teams) {
					ps.setString(i++, team);
				}
			}
			log.debug("prepared statment has: " + (i-1) +" variables ");

			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				NoteVO vo = new NoteVO(rs);
				String firstName = pm.getStringValue("FIRST_NM", rs.getString("FIRST_NM"));
				String lastName = pm.getStringValue("LAST_NM", rs.getString("LAST_NM"));
				vo.setUserName(firstName +" " + lastName);
				vo.setProfileDocuments(pda.getDocumentByFeatureId(vo.getNoteId()));
				processNote(noteType, data, vo);
			}
		} catch(SQLException sqle) {
			log.error("could not load notes ", sqle);
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
		switch (noteType) {
			case COMPANY:
				targetKey = vo.getCompanyId();
				break;
			case PRODUCT:
				targetKey = vo.getProductId();
				break;
			case MARKET:
				targetKey = vo.getMarketId();
				break;
			case ACCOUNT:
				targetKey = vo.getAccountId();
				break;
		}

		if (!StringUtil.isEmpty(vo.getAttributeId()))
			targetKey = vo.getAttributeId();

		if (data.containsKey(targetKey)) {
			data.get(targetKey).add(vo);
		} else {
			List<NoteVO> lvo = new ArrayList<>();
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
	public Map<String, List<NoteVO>> getCompanyNotes(String userId, List<String> teams, String companyId) {
		List<String> companyIds = new ArrayList<>();
		companyIds.add(companyId);
		return getCompanyNotes(userId, teams, companyIds);
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
	public Map<String, List<NoteVO>> getCompanyNotes(String userId, List<String> teams, List<String> companyIds){
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
	public Map<String, List<NoteVO>> getProductNotes(String userId, List<String> teams, String productId) {
		List<String> productIds = new ArrayList<>();
		productIds.add(productId);
		return getProductNotes(userId, teams, productIds);
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
	public Map<String, List<NoteVO>> getProductNotes(String userId, List<String> teams, List<String> productIds) {
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
	public Map<String, List<NoteVO>> getMarketNotes(String userId, List<String> teams, String marketId) {
		List<String> marketIds = new ArrayList<>();
		marketIds.add(marketId);
		return getMarketNotes(userId, teams, marketIds);
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
	public Map<String, List<NoteVO>> getMarketNotes(String userId, List<String> teams, List<String> marketIds) {
		log.debug("Notes Action get market notes called");
		return getNoteList(marketIds, NoteType.MARKET, userId, teams);
	}


	/**
	 * @param accountIds
	 * @return
	 */
	public Map<String, List<NoteVO>> getAccountNotes(String... accountIds) {
		log.debug("loading account notes");
		return getNoteList(Arrays.asList(accountIds), NoteType.ACCOUNT, null, null);
	}

	/**
	 * @param modVo
	 */
	public void setupAttributes(ModuleVO modVo) {
		String fileToken = null;
		try {
			fileToken = ProfileDocumentFileManagerStructureImpl.makeDocToken((String)getAttribute(Constants.ENCRYPT_KEY));
		} catch (EncryptionException e) {
			log.error("could not generate fileToken", e);
		}
		modVo.setAttribute(ProfileDocumentFileManagerStructureImpl.DOC_TOKEN, fileToken);
		modVo.setAttribute("filePrefix", NOTES_DIRECTORY_PATH);
		modVo.setAttribute(ProfileDocumentFileManagerStructureImpl.DOC_TOKEN, fileToken );
	}
}