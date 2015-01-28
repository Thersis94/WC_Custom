package com.depuy.events_v2;

import com.depuy.events.CoopAdsAction;
import com.depuy.events.vo.CoopAdVO;
import com.depuy.events_v2.vo.DePuyEventSeminarVO;
import com.siliconmtn.util.Convert;
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
	
	/**
	 * Switch contracts used on the Outstanding Items page
	 */
	public enum ActionItem {
		surgeonContract, postcardApproval, adClientApproval, adSurgeonApproval, 
		consumableBox, survey, attendance
	}
	
	
	/**
	 * applies business rules to build a list of Outstanding Items (ActionItems) 
	 * for the given seminar.
	 * @param sem
	 */
	public static void attachActionItems(DePuyEventSeminarVO sem) {
		EventEntryVO event = (sem.getEventCount() > 0) ? sem.getEvents().get(0) : new EventEntryVO();
		boolean isHSEM = "HSEM".equals(event.getEventTypeCd());
		boolean isFullyApproved = (sem.getStatusFlg().intValue() == EventFacadeAction.STATUS_APPROVED);
		
		if (isHSEM && isFullyApproved) {
			sem.addActionItem(ActionItem.attendance); // HSEM requiring attendance #s.
			
		} else if (sem.isComplete() && isFullyApproved) {
			sem.addActionItem(ActionItem.survey); // [CF|CP|E]SEM requiring survey completion
			
		} else if (isFullyApproved && !isHSEM && !sem.isComplete()) {
			if (sem.getPostcardFileStatusFlg() == 2) {
				sem.addActionItem(ActionItem.postcardApproval); // awaiting postcard approval
			}
			if (sem.getConsumableOrderDate() == null && sem.isTimeToOrderConsumables()) {
				sem.addActionItem(ActionItem.consumableBox); // need to order consumable box
			}
			//loop the ads
			for (CoopAdVO ad : sem.getAllAds()) {
				int adStatus = Convert.formatInteger(ad.getStatusFlg()).intValue();
				if (adStatus == CoopAdsAction.PENDING_CLIENT_APPROVAL) {
					sem.addActionItem(ActionItem.adClientApproval); //awaiting coordinator approval
					
				} else if (adStatus == CoopAdsAction.PENDING_SURGEON_APPROVAL) {
					sem.addActionItem(ActionItem.adSurgeonApproval); //awaiting surgeon approval
				}
			}
			
		} else if (sem.getStatusFlg().intValue() == EventFacadeAction.STATUS_PENDING_SURG && !isHSEM) {
			sem.addActionItem(ActionItem.surgeonContract);
		}
	}
}
