package com.depuy.events;

// JDK
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// SMT BaseLibs
import com.depuy.events.vo.DePuyEventEntryVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;

// SB Libs
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.event.EventEntryAction;

/****************************************************************************
 * <b>Title</b>: DePuyEventEntryAction.java<p/>
 * <b>Description: Manages EventActions for the SB Sites</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2005<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 1.0
 * @since Mar 15, 2006
 ****************************************************************************/
public class DePuyEventEntryAction extends SBActionAdapter {
	public static final String RETR_EVENTS = "retrEvents";
	
	public DePuyEventEntryAction() {
		super();
	}

	public DePuyEventEntryAction(ActionInitVO arg0) {
		super(arg0);
	}


	public void update(ActionRequest req) throws ActionException {
		//call EventEntryAction for the core-table insertion
		DePuyEventEntryVO vo = new DePuyEventEntryVO(req);
		vo.setEventGroupId(actionInit.getActionId());
		
		//save the core data to the core table, via the core action
		EventEntryAction ac = new EventEntryAction(actionInit);
		ac.setAttributes(this.attributes);
    	ac.setDBConnection(dbConn);
    	vo.setActionId(ac.update(req, vo));
    	ac = null;
    	
    	StringBuilder sql = new StringBuilder();
    	PreparedStatement ps = null;
    	boolean insertRecord = true;

    	//first test to see if the entry has already been created; this prevented dupliates in v1 of Events site
		sql.append("select event_entry_id from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DEPUY_EVENT_ENTRY where event_entry_id=?");
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, vo.getActionId());
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				insertRecord = false;
			}
		} catch (SQLException sqle) {
			log.error("Error Deleting DePuyEventEntry", sqle);
		} finally {
			try {
				ps.close();
	        } catch(Exception e) {}
		}
		
		sql = new StringBuilder();
		if (insertRecord) {
			sql.append("INSERT INTO ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sql.append("DEPUY_EVENT_ENTRY (DISPLAY_NM_FLG, ");
			sql.append("CLINIC_NM, FEAT_PROD_FLG, FOCUS_MRKT_FLG, ");
			sql.append("DISPLAY_SRVC_FLG, ATTENDANCE_CNT, CREATE_DT, SURGEON_BIO_TXT, ");
			sql.append("EVENT_ENTRY_ID) values (?,?,?,?,?,?,?,?,?)");

		} else {
			sql.append("update ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sql.append("DEPUY_EVENT_ENTRY set DISPLAY_NM_FLG=?, clinic_nm=?, ");
			sql.append("feat_prod_flg=?, focus_mrkt_flg=?, display_srvc_flg=?, ");
			sql.append("attendance_cnt=?, update_dt=?, SURGEON_BIO_TXT=? where event_entry_id=?");
		}
		log.debug("DePuyEventEntry Update SQL: " + sql.toString());
        
		// perform the execute
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(1, vo.getShowNameOnPostcardFlg());
			ps.setString(2, vo.getClinicName());
			ps.setInt(3, vo.getFeaturedProductFlg());
			ps.setInt(4, vo.getFocusMrktFlg());
			ps.setInt(5, vo.getShowServiceFlg());
			ps.setInt(6, vo.getAttendanceCnt());
			ps.setTimestamp(7, Convert.getCurrentTimestamp());
			ps.setString(8, vo.getSurgeonBioText());
			ps.setString(9, vo.getActionId());
			
            if (ps.executeUpdate() < 1) {
                log.error("No DePuyEventEntry records updated");
                throw new SQLException();
            }
		} catch (SQLException sqle) {
			log.error("Error Update DePuyEventEntry", sqle);
			throw new ActionException("Error Update DePuyEventEntry", sqle);
            
		} finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch(Exception e) {}
            }
        }
		req.setAttribute("eventEntryId", vo.getActionId());
	}
}
