package com.perfectstorm.action.admin;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.List;

// PS Libs
import com.perfectstorm.data.TourVO;

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
 * <b>Title</b>: TourWidget.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the creation of tours in the system
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 5, 2019
 * @updates:
 ****************************************************************************/

public class TourWidget extends SBActionAdapter {
	/**
	 * Key to access the widget through the controller
	 */
	public static final String AJAX_KEY = "tour";
	
	/**
	 * 
	 */
	public TourWidget() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public TourWidget(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		Integer activeFlag = req.hasParameter("activeFlag") ? req.getIntegerParameter("activeFlag") : null;
		
		this.setModuleData(getTours(activeFlag, new BSTableControlVO(req, TourVO.class)));
	}
	
	/**
	 * 
	 * @param bst
	 * @param activeFlag
	 * @return
	 */
	public GridDataVO<TourVO> getTours(Integer activeFlag, BSTableControlVO bst) {
		// Add the params
		List<Object> vals = new ArrayList<>(); 
		StringBuilder sql = new StringBuilder(256);
		sql.append("select a.*, b.*, c.customer_nm from ").append(getCustomSchema()).append("ps_tour a ");
		sql.append("left outer join ( ");
		sql.append("select tour_id, count(*) as venue_no "); 
		sql.append("from ").append(getCustomSchema()).append("ps_venue_tour_xr ");
		sql.append("group by tour_id ");
		sql.append(") as b on a.tour_id = b.tour_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("ps_customer c on a.customer_id = c.customer_id ");
		sql.append("where 1=1 ");
		
		// Add the search filter
		if (bst.hasSearch()) {
			sql.append("and (tour_nm like ? or tour_desc like ?) ");
			vals.add(bst.getLikeSearch());
			vals.add(bst.getLikeSearch());
		}
		
		// Add the active flag filter
		if (activeFlag != null) {
			sql.append("and active_flg = ? ");
			vals.add(activeFlag);
		}
		
		sql.append(DBUtil.ORDER_BY).append(bst.getDBSortColumnName("start_dt"));
		sql.append(" ").append(bst.getOrder("asc"));
		log.debug(sql.length() + "|" + sql);
		
		// execute the sql
		DBProcessor db = new DBProcessor(dbConn);
		return db.executeSQLWithCount(sql.toString(), vals, new TourVO(), bst);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		TourVO tour = new TourVO(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		
		try {
			db.save(tour);
			putModuleData(tour);
		} catch (Exception e) {
			log.error("Error saving tour: " + tour, e);
			putModuleData(tour, 1, false, e.getLocalizedMessage(), true);
		}
	}
}

