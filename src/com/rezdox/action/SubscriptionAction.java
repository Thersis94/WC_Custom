package com.rezdox.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.rezdox.vo.MembershipVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: SubscriptionAction.java<p/>
 * <b>Description: Manages member subscriptions, checks for needs to upgrade.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2018<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Feb 7, 2018
 ****************************************************************************/
public class SubscriptionAction extends SBActionAdapter {
	
	public SubscriptionAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public SubscriptionAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		// Get the possible subscriptions to be shown to the member for purchase
	}
	
	/**
	 * Checks if a member needs to purchase a residence subscription upgrade
	 * 
	 * @param req
	 */
	protected boolean checkResidenceUpgrade(String memberId) {
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		boolean needsUpgrade = true;
		
		StringBuilder sql = new StringBuilder(350);
		sql.append("select sum(s.qty_no) - (select count(residence_id) from custom.rezdox_residence_member_xr where member_id = ?) as available_qty ");
		sql.append("from ").append(schema).append("rezdox_subscription s inner join ");
		sql.append(schema).append("rezdox_membership m on s.membership_id = m.membership_id ");
		sql.append("where group_cd = ? and member_id = ? ");
		sql.append("group by group_cd ");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			int idx = 0;
			ps.setString(++idx, memberId);
			ps.setString(++idx, MembershipVO.Group.HO.toString());
			ps.setString(++idx, memberId);
			
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				int availableQty = rs.getInt("available_qty");
				needsUpgrade = availableQty > 0 ? false : true;
			}
		} catch (SQLException e) {
			log.error("Unable to validate current Residence subscriptions. ", e);
		}
		
		return needsUpgrade;
	}
}