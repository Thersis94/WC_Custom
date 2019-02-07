package com.perfectstorm.action.admin;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.List;

// PS Libs
import com.perfectstorm.data.VenueVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: VenueWidget.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the creation of Venues in the system
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 5, 2019
 * @updates:
 ****************************************************************************/

public class VenueWidget extends SBActionAdapter {
	/**
	 * Key to access the widget through the controller
	 */
	public static final String AJAX_KEY = "venue";
	
	/**
	 * 
	 */
	public VenueWidget() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public VenueWidget(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.info("retrieving widget");
		Integer activeFlag = req.hasParameter("activeFlag") ? req.getIntegerParameter("activeFlag") : null;
		this.setModuleData(getVenues(activeFlag, new BSTableControlVO(req, VenueVO.class)));
	}
	
	/**
	 * 
	 * @param bst
	 * @param activeFlag
	 * @return
	 */
	public GridDataVO<VenueVO> getVenues(Integer activeFlag, BSTableControlVO bst) {
		// Add the params
		List<Object> vals = new ArrayList<>(); 
		StringBuilder sql = new StringBuilder(80);
		sql.append("select * from ").append(getCustomSchema()).append("ps_venue ");
		sql.append("where 1=1 ");
		
		// Add the search filter
		if (bst.hasSearch()) {
			sql.append("and (venue_nm like ? or venue_desc like ?) ");
			vals.add(bst.getLikeSearch());
			vals.add(bst.getLikeSearch());
		}
		
		// Add the active flag filter
		if (activeFlag != null) {
			sql.append("and active_flg = ? ");
			vals.add(activeFlag);
		}
		
		sql.append(DBUtil.ORDER_BY).append(bst.getDBSortColumnName("venue_nm"));
		sql.append(" ").append(bst.getOrder("asc"));
		log.info(sql.length() + "|" + sql);
		
		// execute the sql
		DBProcessor db = new DBProcessor(dbConn);
		return db.executeSQLWithCount(sql.toString(), vals, new VenueVO(), bst);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.info("*** Building: " + req.getParameter("venueId"));
		VenueVO venue = new VenueVO(req);
		log.info(venue);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		
		try {
			db.save(venue);
			putModuleData(venue);
		} catch (Exception e) {
			log.error("Unable to add venue: " + venue, e);
			putModuleData(venue, 1, false, e.getLocalizedMessage(), true);
		}
	}
}

