package com.restpeer.action.mobilerest;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.restpeer.data.MemberLocationVO;
// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.SimpleActionAdapter;

/****************************************************************************
 * <b>Title</b>: MobileRestWidget.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Main widget for management of the mobile restaurateur data
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Mar 25, 2019
 * @updates:
 ****************************************************************************/
public class MobileRestWidget extends SimpleActionAdapter {

	/**
	 * 
	 */
	public MobileRestWidget() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public MobileRestWidget(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (! req.getBooleanParameter("json")) return;
		
		try {
			if (req.getBooleanParameter("closest")) {
				setModuleData(getClosestSearch(req));
			}
		} catch (Exception e) {
			putModuleData(null, 0, false, e.getLocalizedMessage(), true);
		}
	}
	
	/**
	 * Finds the closest 10 locations that match the search criteria
	 * @param mlid
	 * @return
	 * @throws DatabaseException
	 */
	public List<MemberLocationVO> getClosestSearch(ActionRequest req) throws DatabaseException {

		MemberLocationVO ml = new MemberLocationVO();
		ml.setMemberLocationId(req.getParameter("memberLocationId"));
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			db.getByPrimaryKey(ml);
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Unable to retreive source location", e);
			throw new DatabaseException("Unable to retreive source location", e);
		}
		
		List<Object> vals = new ArrayList<>();
		StringBuilder sql = new StringBuilder(384);
		sql.append(DBUtil.SELECT_CLAUSE).append("a.*, b.*, core.geoCalcDistance(cast(? as numeric), cast(? as numeric), ");
		sql.append("b.latitude_no, b.longitude_no, 'mi') as distance  ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("rp_member a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("rp_member_location b on a.member_id = b.member_id ");
		vals.add(ml.getLatitude());
		vals.add(ml.getLongitude());
		
		// Add in the search filters
		if (!StringUtil.isEmpty(req.getParameter("productCode"))) addClosestSearchFilters(req, sql, vals);
		
		sql.append(DBUtil.WHERE_CLAUSE).append("member_type_cd = 'KITCHEN' ");
		sql.append(DBUtil.ORDER_BY).append("distance limit 10 ");
		
		log.debug(sql.length() + "|" + sql);
		
		
		return db.executeSelect(sql.toString(), vals, new MemberLocationVO());
	}
	
	/**
	 * Adds search filters to the closest location search
	 * 
	 * @param req
	 * @param sql
	 * @param vals
	 */
	private void addClosestSearchFilters(ActionRequest req, StringBuilder sql, List<Object> vals) {
		List<String> productCodes = StringUtil.parseList(req.getParameter("productCode"));
		
		// Ensure we only have day of week values available by the Enum
		List<String> daysAvailable = Stream.of(DayOfWeek.values()).map(Enum::name).collect(Collectors.toList());
		daysAvailable.retainAll(StringUtil.parseList(req.getParameter("daysAvailable")));
		
		// Get the start/end times, converted to int as stored in the db
		int startTime = LocalTime.parse(StringUtil.checkVal(req.getParameter("startTimeText"), "23:59")).toSecondOfDay();
		int endTime = LocalTime.parse(StringUtil.checkVal(req.getParameter("endTimeText"), "00:00")).toSecondOfDay();

		// Each selected product must exist for the location to be a valid result
		for (int idx = 0; idx < productCodes.size(); idx++) {
			String productCode = productCodes.get(idx);
			sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("rp_location_product_xr lp").append(idx).append(" on b.member_location_id = lp").append(idx).append(".member_location_id ");
			sql.append(" and lp").append(idx).append(".product_cd = ? ");
			vals.add(productCode.split("_")[1]);

			if (productCode.startsWith("1_") && !daysAvailable.isEmpty()) {
				for (int dayIdx = 0; dayIdx < daysAvailable.size(); dayIdx++) {
					String idxText = idx + "_" + dayIdx;
					sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("rp_product_schedule ps").append(idxText).append(" on lp").append(idx).append(".location_product_id = ps").append(idxText).append(".location_product_id ");
					sql.append(" and ps").append(idxText).append(".day_of_week_txt = ? ").append(String.format(" and ps%s.start_time_no <= ? and ? <= ps%s.end_time_no ", idxText, idxText));
					vals.add(daysAvailable.get(dayIdx));
					vals.add(startTime);
					vals.add(endTime);
				}
			}
		}
	}
}
