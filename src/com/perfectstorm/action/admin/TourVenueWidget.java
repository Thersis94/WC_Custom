package com.perfectstorm.action.admin;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.List;

// PS Libs
import com.perfectstorm.data.VenueTourVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: TourVenueWidget.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the venues assigned to a tour
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 5, 2019
 * @updates:
 ****************************************************************************/

public class TourVenueWidget extends SBActionAdapter {

	/**
	 * 
	 */
	public TourVenueWidget() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public TourVenueWidget(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		this.setModuleData(getTourVenues(req.getParameter("tourId")));
	}
	
	/**
	 * 
	 * @param bst
	 * @param activeFlag
	 * @return
	 */
	public List<VenueTourVO> getTourVenues(String tourId) {
		// Add the params
		List<Object> vals = new ArrayList<>(); 
		vals.add(tourId);
		
		// Build the SQL
		StringBuilder sql = new StringBuilder(80);
		sql.append("select * from ").append(getCustomSchema()).append("ps_venue_tour_xr a ");
		sql.append("inner join ").append(getCustomSchema()).append("ps_venue b ");
		sql.append("on a.venue_id = b.venue_id ");
		sql.append("where tour_id = ? ");
		sql.append(DBUtil.ORDER_BY).append("event_dt");
		
		// execute the sql
		DBProcessor db = new DBProcessor(dbConn);
		return db.executeSelect(sql.toString(), vals, new VenueTourVO());
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		VenueTourVO tourVenue = new VenueTourVO(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		
		try {
			db.save(tourVenue);
		} catch(Exception e) {
			putModuleData(tourVenue, 1, false,e.getLocalizedMessage(), true);
		}
	}
}

