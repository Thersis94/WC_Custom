
package com.depuy.events_v2;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.depuy.events_v2.vo.DePuyEventSeminarVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: PostcardDeleteV2.java<p/>
 * <b>Description: Handles deleting items for DePuy seminars.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Erik Wingo
 * @version 1.0
 * @since Oct 10, 2014
 ****************************************************************************/
public class PostcardDeleteV2 extends SBActionAdapter {

	/**
	 * Default message for a successful deletion.
	 */
	private String successMsg = "Deleted Successfully";
	/**
	 * Default error message.
	 */
	private String errorMsg = "Error Processing Transaction";
	
	/**
	 * Possible requests sent to this class. If additional request type is needed,
	 * add entry to this enum and matching condition in build switch statement.
	 */
	public enum ReqType {
		deleteSeminar, deleteCoopAd;
	}
	
	/**
	 * Stores information needed for deleting from relevant tables (exists mainly
	 * to avoid redundant methods).
	 */
	private enum TableInfo{
		SEM_EVENT_SURGEON("DEPUY_EVENT_SURGEON", "EVENT_POSTCARD_ID",true),
		SEM_COOP_ADS("DEPUY_EVENT_COOP_AD", "EVENT_POSTCARD_ID",true),
		SEM_EVENT_PERSON_XR("DEPUY_EVENT_PERSON_XR", "EVENT_POSTCARD_ID",true),
		SEM_LOCATION_XR("DEPUY_EVENT_SPECIALTY_XR", "EVENT_POSTCARD_ID", true),
		SEM_EVENT_POSTCARD_ASSOC("EVENT_POSTCARD_ASSOC", "EVENT_POSTCARD_ID",false),
		SEM_EVENT_POSTCARD("EVENT_POSTCARD", "EVENT_POSTCARD_ID", false),
		SEM_EVENT_ENTRY("EVENT_ENTRY", "EVENT_ENTRY_ID", false),
		COOP_AD("DEPUY_EVENT_COOP_AD","COOP_AD_ID",true);
		private String table;
		private String idField;
		private boolean isCustom;
		TableInfo(String table, String idField, boolean isCustom){
			this.table = table;
			this.idField = idField;
			this.isCustom = isCustom;
		}
		
		public String getTable(){
			return table;
		}
		public String getIdField(){
			return idField;
		}
		public boolean isCustom(){
			return isCustom;
		}
	}
	
	/**
	 * Default Constructor
	 */
	public PostcardDeleteV2() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public PostcardDeleteV2(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.
	 * http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException{
		String resultMsg = successMsg;
		//Validate request type
		ReqType reqType = null;
		try {
			reqType = ReqType.valueOf(req.getParameter("reqType"));
		} catch (Exception e) {
			throw new ActionException("unknown request type " + req.getParameter("reqType"));
		}
		
		String nextPage = StringUtil.checkVal(req.getParameter("nextPage"));
		String eventPostcardId = StringUtil.checkVal(req.getParameter("eventPostcardId"));
		if ( eventPostcardId.isEmpty() ) {
			log.error("Missing eventPostcardId");
			return;
		}
		
		try{
			switch(reqType){
			case deleteSeminar:
				//deleting a seminar requires queries on multiple tables. Disable
				//auto-commit to avoid partially deleting seminar info on an error
				dbConn.setAutoCommit(false);
				deleteSeminar( req, eventPostcardId, getSeminar(eventPostcardId) );
				//finalize changes
				dbConn.commit();
				nextPage = "list";
				break;
			case deleteCoopAd:
				//delete a single ad
				genericDelete( TableInfo.COOP_AD, req.getParameter("coopId"));
				break;
			}
		} catch (Exception e){
			log.error(e);
			resultMsg = errorMsg;
		} finally {
			//Restore auto commit to true
			try{ 
				dbConn.setAutoCommit(true);
			} catch(SQLException e){}
		}
		
		//Setup redirect upon completion
		StringBuilder redir = new StringBuilder();
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		redir.append( page.getRequestURI() ).append("?reqType=").append(nextPage);
		
		super.sendRedirect(redir.toString(), resultMsg, req);
	}
	
	/**
	 * Deletes a single seminar.
	 * @param req
	 * @param eventPostcardId
	 */
	private void deleteSeminar( SMTServletRequest req, String eventPostcardId,
			DePuyEventSeminarVO vo) throws SQLException, InvalidDataException{
		//if either the postcard_id or the event_entry id is missing, return now
		String epId = StringUtil.checkVal(eventPostcardId);
		String eventId = StringUtil.checkVal(vo.getActionId());
		
		if ( epId.isEmpty() || eventId.isEmpty() ){
			String msg = "Missing Information: EventId="+eventId+" PostcardId="+epId;
			log.error(msg);
			throw new InvalidDataException(msg);
		}
		
		//Delete the entries corresponding to this seminar (all those listed in
		//the TableInfo enum).
		for (TableInfo ti : TableInfo.values()){
			//deleting from event_entry requires different id
			if ( ti == TableInfo.SEM_EVENT_ENTRY ){
				genericDelete(ti, eventId);
			} else if (ti.toString().toUpperCase().startsWith("SEM_")) {
				genericDelete(ti, epId);
			}
		}
	}
	
	/**
	 * Grab information about the target seminar
	 * @param eventPostcardId
	 * @return
	 * @throws SQLException
	 */
	private DePuyEventSeminarVO getSeminar(String eventPostcardId) throws SQLException{
		//Use PostcardSelectV2 to retrieve the seminar information
		DePuyEventSeminarVO vo = null;
		PostcardSelectV2 pSel = new PostcardSelectV2(this.actionInit);
		pSel.setDBConnection(dbConn);
		pSel.setAttributes(this.attributes);
		vo = pSel.loadOneSeminar(eventPostcardId, actionInit.getActionId(), null, null, null);
		//grab event_entry_id so we can delete from event_entry later
		vo.setActionId( fetchEventEntryId(eventPostcardId) );
		
		return vo;
	}
	
	/**
	 * Retrieves the event_entry_id for an event
	 * @param eventPostcardId
	 * @return
	 * @throws SQLException
	 */
	private String fetchEventEntryId( String eventPostcardId ) throws SQLException{
		if(StringUtil.checkVal(eventPostcardId).isEmpty()) { return null; }
		
		String id = null;
		StringBuilder sql = new StringBuilder(100);
		sql.append("select ee.EVENT_ENTRY_ID from EVENT_ENTRY ee ");
		sql.append("inner join EVENT_POSTCARD_ASSOC epa on ");
		sql.append("ee.EVENT_ENTRY_ID = epa.EVENT_ENTRY_ID ");
		sql.append("and epa.EVENT_POSTCARD_ID = ?");
		
		try( PreparedStatement ps = dbConn.prepareStatement(sql.toString())){
			ps.setString(1, eventPostcardId);
			ResultSet rs = ps.executeQuery();
			
			if ( rs.next() ){
				id = rs.getString("event_entry_id");
			}
		} 
		
		return id;
	}
	
	/**
	 * Used to delete from a table listed in TableInfo (deletes by eventPostcardId)
	 * @param info Table info to be used
	 * @param id Id value to be used in the where clause
	 * @throws SQLException
	 */
	private void genericDelete(TableInfo info, String eventPostcardId) throws SQLException{
		//check if any of the info is missing
		if ( StringUtil.checkVal(info.getTable()).isEmpty() 
				|| StringUtil.checkVal(eventPostcardId).isEmpty() ){
			log.error("Missing info required for deletion.");
			return;
		}
		
		StringBuilder sql = new StringBuilder(100);
		sql.append("delete from ");
		if ( info.isCustom() )
			sql.append( getAttribute(Constants.CUSTOM_DB_SCHEMA) );
		sql.append(info.getTable());
		sql.append(" where ").append( info.getIdField() ).append(" = ?");
		
		log.debug(sql+"|"+eventPostcardId);	
		
		//Execute delete operation. 
		try( PreparedStatement ps = dbConn.prepareStatement(sql.toString())){
			ps.setString(1, eventPostcardId );
			log.debug("Affected "+ps.executeUpdate()+" record(s)");
		}
	}
}
