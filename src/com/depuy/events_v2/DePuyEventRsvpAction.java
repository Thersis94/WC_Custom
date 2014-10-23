/**
 * 
 */
package com.depuy.events_v2;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.depuy.events_v2.vo.DePuyEventRsvpVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.siliconmtn.util.databean.FilePartDataBean;
import com.siliconmtn.util.parser.AnnotationXlsParser;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;

/****************************************************************************
 * <b>Title</b>: DePuyEventRsvpAction.java <p/>
 * <b> Handles uploading RSVP data for the DePuy Seminar site<p/>
 * <b>Copyright:</b> Copyright (c) 2014 <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * @author Erik Wingo
 * @since Oct 15, 2014
 ****************************************************************************/
public class DePuyEventRsvpAction extends SimpleActionAdapter {

	/**
	 * 
	 */
	public DePuyEventRsvpAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public DePuyEventRsvpAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	public void build( SMTServletRequest req ) throws ActionException{
		//If the request is for a file import, do so. Check exists in case the
		//single rsvp insertions are moved to this class
		if ( Convert.formatBoolean( req.getParameter("import"))){
			processFile(req);
		}
	}
	
	/**
	 * Process an excel file and inserts the records into the rsvp table
	 * @param req
	 */
	private void processFile( SMTServletRequest req ) throws ActionException{
		String eventEntryId = StringUtil.checkVal( req.getParameter("eventEntryId"));
		if ( eventEntryId.isEmpty() ){
			log.error("Missing event entry id.");
			throw new ActionException("Missing Event Entry Id");
		}
		
		try {
			//Read from the spreadsheet
			Map< Class<?>, Collection<Object> > beanMap = parseFile( req.getFile("batchFile") );
			//Set of unique email addresses in the spreadsheet
			Set<String> emailAddr = new LinkedHashSet<>();
			ArrayList<DePuyEventRsvpVO> voList = new ArrayList<>();
			//VO's marked for update
			ArrayList<DePuyEventRsvpVO> updateList = new ArrayList<>();
			//VO's marked for insertion
			ArrayList<DePuyEventRsvpVO> insertList = new ArrayList<>();
			
			for (Object obj : beanMap.get(DePuyEventRsvpVO.class)){
				DePuyEventRsvpVO vo = (DePuyEventRsvpVO) obj;
				//grab the email address for each vo (so we can lookup the profile id)
				emailAddr.add( vo.getEmailAddress() );
				voList.add(vo);
			}
			//map of profile id and rsvp id to email addresses
			Map< String, Map<String,String> > profileIdMap = getProfileIdList(emailAddr);
			
			for ( DePuyEventRsvpVO bean : voList ){
				//get the profile id for each record
				Map<String, String> pInfo = profileIdMap.get( bean.getEmailAddress() );
				String pId = ( pInfo != null ? pInfo.get("profile_id") : null );
				bean.setProfileId( pId );
				bean.setEventEntryId(eventEntryId);
				bean.setEventRsvpId( ( pInfo != null ? pInfo.get("event_rsvp_id") : null ) );
				
				//Update the profile information
				updateUserProfile( bean );
				//delegate records with id's to the update line, and the rest to
				//the insert line
				if (StringUtil.checkVal( bean.getEventRsvpId() ).isEmpty()){
					insertList.add(bean);
				} else {
					updateList.add(bean);
				}
			}
			voList = null;
			
			//execute the db updates
			importRecords(insertList, false);
			importRecords(updateList, true);
			
		} catch (InvalidDataException ie) {
			log.error("Error parsing the file");
			throw new ActionException(ie);
		} catch (SQLException se){
			log.error("Error accessing rsvp table");
			throw new ActionException(se);
		} catch (DatabaseException de) {
			log.error("Error updating profile information");
			throw new ActionException(de);
		}
	}
	
	private Map<Class<?>, Collection<Object>> parseFile( FilePartDataBean file ) 
	throws InvalidDataException{
		
		if (file == null){
			log.error("Missing file data");
			throw new InvalidDataException("Missing file data.");
		}
		
		//Create the lists of VO objects and table names that will be used by the parser
		List<Class<?>> classes= new LinkedList<Class<?>>();
		classes.add(DePuyEventRsvpVO.class);
		
		AnnotationXlsParser parser = new AnnotationXlsParser();
		return parser.readFileData( file.getFileData(), classes);
	}

	/**
	 * Get a list of profile_ids and rsvp id's corresponding to email addresses
	 * @param emailAddr List of email addresses
	 * @return Map with profile_id and event_rsvp_id mapped to email addresses
	 * @throws SQLException
	 */
	protected Map<String, Map<String,String> > getProfileIdList( Set<String> emailAddr )
	throws SQLException{
		Map<String, Map<String,String>> profileMap = new HashMap<String,Map<String,String>>();
		//Used for decoding and encoding db email addresses
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		
		StringBuilder sql = new StringBuilder(100);
		sql.append("select p.PROFILE_ID, p.EMAIL_ADDRESS_TXT, e.EVENT_RSVP_ID from PROFILE p ");
		sql.append("left join EVENT_RSVP e on p.PROFILE_ID=e.PROFILE_ID ");
		sql.append("where p.SEARCH_EMAIL_TXT in (");
		
		//Emails are added to the query's in clause
		int commaCount = 0;
		Iterator<String> iter = emailAddr.iterator();
		while ( iter.hasNext() ){
			String val = iter.next();
			sql.append("'").append(pm.getEncValue("SEARCH_EMAIL_TXT", val.toUpperCase())).append("'");
			if ( commaCount < emailAddr.size() - 1 ){
				sql.append(",");
				commaCount++;
			}
		}
		sql.append(")");
		log.debug("Profile Id Search:"+sql.toString());
		
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())){
			ResultSet rs = ps.executeQuery();
			while ( rs.next() ){
				Map<String,String> entryMap = new HashMap<String,String>();
				entryMap.put("profile_id", rs.getString("profile_id"));
				entryMap.put("event_rsvp_id", rs.getString("event_rsvp_id"));
				profileMap.put( pm.getStringValue("SEARCH_EMAIL_TXT", rs.getString("email_address_txt")), 
						entryMap);
			}
		}
		
		return profileMap;
	}
	
	/**
	 * Updates the user profile as necessary
	 * @param vo
	 * @throws DatabaseException 
	 */
	private void updateUserProfile( DePuyEventRsvpVO vo ) throws DatabaseException{
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		UserDataVO usr = new UserDataVO();
		
		//values used to update the profile entry
		usr.setProfileId(vo.getProfileId());
		usr.setEmailAddress(vo.getEmailAddress());
		usr.setValidEmailFlag( StringUtil.isValidEmail(vo.getEmailAddress()) ? 1:0 );
		usr.setFirstName(vo.getFirstName());
		usr.setLastName(vo.getLastName());
		usr.setPrefixName( vo.getPrefix() );
		usr.setAddress( vo.getAddress1Text() );
		usr.setAddress2( vo.getAddress2Text() );
		usr.setCity( vo.getCity() );
		usr.setState( vo.getState() );
		usr.setZipCode( vo.getZipCode() );
		usr.setMainPhone( vo.getPhoneNum() );
		
		//If there is a profile id, update the record. If not, create a new profile
		if ( ! StringUtil.checkVal( usr.getProfileId() ).isEmpty() ){
			pm.updateProfilePartially(usr.getDataMap(), usr.getProfileId(), dbConn);
		} else {
			pm.updateProfile(usr, dbConn);
			vo.setProfileId( usr.getProfileId() );
		}
	}
	
	/**
	 * Sql statement for updating rsvp table
	 * @return
	 */
	private String getUpdate(){
		StringBuilder sql = new StringBuilder(400);
		sql.append("update EVENT_RSVP set EVENT_ENTRY_ID = ?, PROFILE_ID = ?, ");
		sql.append("GUESTS_NO = ?, REFERRAL_TXT = ?, INFO_KIT_FLG = ?, REMINDER_DT = ?, ");
		sql.append("REMINDER_TYPE_TXT = ?, OPT_IN_FLG = ?, RSVP_STATUS_FLG = ?, ");
		sql.append("CALL_CENTER_FLG = ?, NOTES_TXT = ?, UPDATE_DT = ?, FIRST_CALLBACK_FLG = ?, ");
		sql.append("SECOND_CALLBACK_FLG = ?,ATTRIB1_TXT = ? ");
		sql.append("where EVENT_RSVP_ID = ?");
		return sql.toString();
	}
	
	/**
	 * SQL statement for inserting into the rsvp table
	 * @return
	 */
	private String getInsert(){
		StringBuilder sql = new StringBuilder(370);
		sql.append("insert into EVENT_RSVP ( EVENT_ENTRY_ID, ");
		sql.append("PROFILE_ID, GUESTS_NO, REFERRAL_TXT, INFO_KIT_FLG, REMINDER_DT, ");
		sql.append("REMINDER_TYPE_TXT, OPT_IN_FLG, RSVP_STATUS_FLG, CALL_CENTER_FLG, ");
		sql.append("NOTES_TXT, CREATE_DT, FIRST_CALLBACK_FLG, ");
		sql.append("SECOND_CALLBACK_FLG, ATTRIB1_TXT, EVENT_RSVP_ID) ");
		sql.append("values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
		return sql.toString();
	}
	
	/**
	 * Import batch of records into the rsvp table
	 * @param voList List of beans to insert
	 * @param isUpdate True if update statement should be used, false otherwise
	 * @throws SQLException
	 */
	private void importRecords( ArrayList<DePuyEventRsvpVO> voList, boolean isUpdate) 
	throws SQLException{
		//get statement
		String sql = ( isUpdate ? getUpdate() : getInsert() );
		String type = ( isUpdate ? "Updating " : "Inserting ");
		log.debug(type+voList.size()+" record(s).");
		
		try( PreparedStatement ps = dbConn.prepareStatement(sql.toString())){
			//To prevent partial updates
			dbConn.setAutoCommit(false);
			
			for ( DePuyEventRsvpVO vo : voList ){
				int i = 0;
				ps.setString(++i, vo.getEventEntryId());
				ps.setString(++i, vo.getProfileId());
				ps.setInt(++i, vo.getGuestsNo());
				ps.setString(++i, vo.getReferral());
				ps.setInt(++i, vo.getInfoKitFlg());
				ps.setTimestamp(++i, Convert.getTimestamp(vo.getReminderDt(), true));
				ps.setString(++i, vo.getReminderTypeNm());
				ps.setInt(++i, vo.getOptInFlg());
				ps.setInt(++i, (isUpdate ? vo.getRsvpStatusFlg() : 1) );
				ps.setInt(++i, vo.getCallCenterFlg());
				ps.setString(++i, vo.getNotes());
				ps.setTimestamp(++i, Convert.getCurrentTimestamp() );
				ps.setInt(++i, vo.getFirstCallbackFlg());
				ps.setInt(++i, vo.getSecondCallbackFlg());
				ps.setString(++i, vo.getAttrib1Text());
				ps.setString(++i, (isUpdate ? vo.getEventRsvpId() : 
					new UUIDGenerator().getUUID()) );
				
				ps.addBatch();
				ps.clearParameters();
			}
			
			ps.executeBatch();
			dbConn.commit();
			
		} finally {
			dbConn.setAutoCommit(true);
		}
	}
}
