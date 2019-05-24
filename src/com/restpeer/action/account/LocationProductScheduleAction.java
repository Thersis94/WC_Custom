package com.restpeer.action.account;

// JDK 1.8.x
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

// RP Libs
import com.restpeer.data.ProductScheduleVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: LocationProductScehduleAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the schedule for a given product
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Mar 23, 2019
 * @updates:
 ****************************************************************************/
public class LocationProductScheduleAction extends SBActionAdapter {

	/**
	 * Json key to access this action
	 */
	public static final String AJAX_KEY = "locationProductSchedule";
	
	/**
	 * 
	 */
	public LocationProductScheduleAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public LocationProductScheduleAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.user.UserBaseWidget#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		setModuleData(getSchedule(req.getParameter("locationProductId")));
	}
	
	/**
	 * Retrieves a list of the items and quantities supported by a member location 
	 * @param mlid
	 * @return
	 */
	public List<ProductScheduleVO> getSchedule(String lpid) {
		List<Object> vals = new ArrayList<>();
		vals.add(lpid);
		StringBuilder sql = new StringBuilder(128);
		sql.append("select * ");
		sql.append("from ").append(getCustomSchema()).append("rp_product_schedule ");
		sql.append("where location_product_id = ? ");
		sql.append("order by ( ");
		sql.append("day_of_week_txt = 'SUNDAY', day_of_week_txt = 'MONDAY', ");
		sql.append("day_of_week_txt = 'TUESDAY', day_of_week_txt = 'WEDNESDAY', ");
		sql.append("day_of_week_txt = 'THURSDAY', day_of_week_txt = 'FRIDAY', ");
		sql.append("day_of_week_txt = 'SATURDAY' ");
		sql.append(") desc");
		log.debug(sql.length() + "|" + sql + vals);
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), vals, new ProductScheduleVO());
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.user.UserBaseWidget#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		ProductScheduleVO psvo = new ProductScheduleVO(req);
		try {
			DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
			String[] daysOfWeek = req.getParameterValues("daysOfWeek");
			
			if (req.getBooleanParameter("isDelete")) {
				db.delete(psvo);
			} else if (daysOfWeek != null) {
				for (int i = 0; i < daysOfWeek.length; i++) {
					psvo = new ProductScheduleVO(req);
					psvo.setDayOfWeek(DayOfWeek.valueOf(daysOfWeek[i]));
					db.insert(psvo);
				}
			}
			
			putModuleData("success", 1, false, null, false);
		} catch (Exception e) {
			log.error("Unable to save / delete location product schedule info: " + psvo);
			setModuleData(psvo, 1, e.getLocalizedMessage());
		}
		
	}
}
