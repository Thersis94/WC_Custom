package com.depuy.events_v2;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.depuy.events.CoopAdsAction;
import com.depuy.events.vo.CoopAdVO;
import com.depuy.events_v2.vo.DePuyEventSeminarVO;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.event.EventFacadeAction;
import com.smt.sitebuilder.action.event.vo.EventEntryVO;

/****************************************************************************
 * <b>Title</b>: OutstandingItems.java<p/>
 * <b>Description: Applies business rules to build a list of Outstanding Items (ActionItems) 
 * for the passed seminar.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 27, 2015
 ****************************************************************************/
public class OutstandingItems {
	
	private SMTDBConnection dbConn;
	protected static Logger log = Logger.getLogger(OutstandingItems.class);
	
	public OutstandingItems(SMTDBConnection dbConn) {
		this.dbConn = dbConn;
	}
	
	/**
	 * Switch contracts used on the Outstanding Items page
	 */
	public enum ActionItem {
		surgeonContract, postcardApproval, adClientApproval, adSurgeonApproval, adHospitalApproval,
		consumableBox, survey, attendance,
		//processes the user must manually acknowledge/complete on their own
		printFlyers(true), meetWithHospital(true), educateSurgeon(true), getAVEquipment(true),
		testAVEquipment(true), printLocator(true), callRSVPs(true), pcpInviteFile;
		
		private boolean offline;  //flags whether this is a manually-completed item, not something we complete via processes.
		ActionItem(){}
		ActionItem(boolean offline) {
			this.offline = offline;
		}
		public boolean isOffline() { return offline; }
}
		
	
	
	/**
	 * applies business rules to build a list of Outstanding Items (ActionItems) 
	 * for the given seminar.
	 * @param sem
	 */
	@SuppressWarnings("incomplete-switch")
	public void attachActionItems(DePuyEventSeminarVO sem) {
		EventEntryVO event = (sem.getEventCount() > 0) ? sem.getEvents().get(0) : new EventEntryVO();
		boolean isHSEM = "HSEM".equals(event.getEventTypeCd());
		boolean isFullyApproved = (sem.getStatusFlg().intValue() == EventFacadeAction.STATUS_APPROVED);
		
		List<ActionItem> completedItems = loadCompletedItems(sem.getEventPostcardId());
		
		if (isHSEM && isFullyApproved) {
			sem.addActionItem(ActionItem.attendance); // HSEM requiring attendance #s.
			
		} else if (sem.isComplete() && isFullyApproved) {
			sem.addActionItem(ActionItem.survey); // [CF|CP|E]SEM requiring survey completion
			
		} else if (isFullyApproved && !isHSEM && !sem.isComplete()) {
			boolean isCPSEM = "CPSEM".equals(event.getEventTypeCd()) || "MITEK-PEER".equals(event.getEventTypeCd());
			
			if (sem.getPostcardFileStatusFlg() == 2) {
				sem.addActionItem(ActionItem.postcardApproval); // awaiting postcard approval
			}
			if (sem.getConsumableOrderDate() == null && sem.isTimeToOrderConsumables()) {
				sem.addActionItem(ActionItem.consumableBox); // need to order consumable box
			}
			if (isCPSEM) {
				boolean sendingFile = Convert.formatInteger(sem.getInviteFileFlg()).intValue() == 2;
				if (sendingFile  && StringUtil.checkVal(sem.getInviteFileUrl()).length() == 0)
					sem.addActionItem(ActionItem.pcpInviteFile); //they said they're uploading a file, but haven't
			}
			
			//loop the ads
			for (CoopAdVO ad : sem.getAllAds()) {
				int adStatus = Convert.formatInteger(ad.getStatusFlg()).intValue();
				if (adStatus == CoopAdsAction.PENDING_CLIENT_APPROVAL) {
					sem.addActionItem(ActionItem.adClientApproval); //awaiting coordinator approval
					
				} else if (adStatus == CoopAdsAction.PENDING_SURGEON_APPROVAL) {
					sem.addActionItem(ActionItem.adSurgeonApproval); //awaiting surgeon approval
					
				} else if (adStatus == CoopAdsAction.PENDING_SURGEON_APPROVAL && ad.getSurgeonStatusFlg() == CoopAdsActionV2.SURG_APPROVED_AD) {
					sem.addActionItem(ActionItem.adHospitalApproval); //awaiting surgeon approval
				}
			}
			
		} else if (sem.getStatusFlg().intValue() == EventFacadeAction.STATUS_PENDING_SURG && !isHSEM) {
			sem.addActionItem(ActionItem.surgeonContract);
		}
		
		//loop through the manual tasks.  If they're not completed add them to the list to TO-DOs as they become due
		for (ActionItem item : ActionItem.values()) {
			if (!item.isOffline()) continue;
			if (!completedItems.contains(item)) {
				//once the event is over, these are pointless
				if (sem.isComplete()) continue;

				//apply business rules - once they're on the list we have to display them, otherwise the count is off
				switch(item) {
					case callRSVPs:
						if (sem.isMinDaysAway(3)) sem.addActionItem(item);
						break;
					case testAVEquipment:
					case printLocator:
						if (sem.isMinDaysAway(7)) sem.addActionItem(item);
						break;
					case educateSurgeon:
					case printFlyers:
					case getAVEquipment:
						if (sem.isMinDaysAway(28)) sem.addActionItem(item);
						break;
					case meetWithHospital:
						if (sem.isMinDaysAway(42)) sem.addActionItem(item);
						break;		
				}
			}
		}
		log.debug("outstanding complete");
	}
	
	
	/**
	 * loads a list of the ActionItems that have been completed for this seminar.
	 * @param postcardId
	 * @return
	 */
	private List<ActionItem> loadCompletedItems(String postcardId) {
		String sql = "select action_item_cd from event_postcard_action_item where event_postcard_id=?";
		List<ActionItem> data = new ArrayList<>();
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setString(1, postcardId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) 
				data.add(ActionItem.valueOf(rs.getString(1)));
			
		} catch (SQLException sqle) {
			log.error("could not load completed actionItems", sqle);
		}
		return data;
	}
}
