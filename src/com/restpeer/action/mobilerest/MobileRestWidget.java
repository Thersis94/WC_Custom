package com.restpeer.action.mobilerest;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// RP Custom Libs
import com.restpeer.common.RPConstants.MemberType;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.dealer.DealerInfoAction;
import com.smt.sitebuilder.action.dealer.DealerLocationVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

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
	public List<DealerLocationVO> getClosestSearch(ActionRequest req) throws DatabaseException {
		String orgId = ((SiteVO) req.getAttribute(Constants.SITE_DATA)).getOrganizationId();
		DealerInfoAction dia = new DealerInfoAction(getDBConnection(), getAttributes());
		DealerLocationVO dl;
		try {
			dl = dia.getDealerLocationInfo(req.getParameter("dealerLocationId"), orgId);
		} catch (com.siliconmtn.exception.DatabaseException e) {
			throw new DatabaseException(e);
		}
		
		List<Object> vals = new ArrayList<>();
		StringBuilder sql = new StringBuilder(384);
		sql.append(DBUtil.SELECT_CLAUSE).append("a.*, b.*, core.geoCalcDistance(cast(? as numeric), cast(? as numeric), ");
		sql.append("b.geo_lat_no, b.geo_long_no, 'mi') as distance  ");
		sql.append(DBUtil.FROM_CLAUSE).append("dealer a ");
		sql.append(DBUtil.INNER_JOIN).append("dealer_location b on a.dealer_id = b.dealer_id ");
		vals.add(dl.getLatitude());
		vals.add(dl.getLongitude());
		
		// Add in the search filters
		if (!StringUtil.isEmpty(req.getParameter("productId"))) addClosestSearchFilters(req, sql, vals);
		
		// Add in the dealer type & order by
		sql.append(DBUtil.WHERE_CLAUSE).append("dealer_type_id = ? ");
		vals.add(MemberType.KITCHEN.getDealerId());
		sql.append(DBUtil.ORDER_BY).append("distance limit 10 ");
		log.debug(sql.length() + "|" + sql);
		
		// Perform the search
		Map<String, DealerLocationVO> locations = new LinkedHashMap<>();
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			int idx = 0;
			for (Object value : vals) {
				ps.setObject(++idx, value);
			}
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				DealerLocationVO dealerLoc = new DealerLocationVO(rs);
				locations.put(rs.getString("dealer_location_id"), dealerLoc);
			}
		} catch (Exception e) {
			log.error(e);
			throw new DatabaseException("Unable to search for closest kitchen locations", e);
		}
		
		// Return unique locations
		return new ArrayList<>(locations.values());
	}
	
	/**
	 * Adds search filters to the closest location search
	 * 
	 * @param req
	 * @param sql
	 * @param vals
	 */
	private void addClosestSearchFilters(ActionRequest req, StringBuilder sql, List<Object> vals) {
		List<String> productIds = StringUtil.parseList(req.getParameter("productId"));
		
		// Ensure we only have day of week values available by the Enum
		List<String> daysAvailable = Stream.of(DayOfWeek.values()).map(Enum::name).collect(Collectors.toList());
		daysAvailable.retainAll(StringUtil.parseList(req.getParameter("daysAvailable")));
		
		// Get the start/end times, converted to int as stored in the db
		int startTime = LocalTime.parse(StringUtil.checkVal(req.getParameter("startTimeText"), "23:59")).toSecondOfDay();
		int endTime = LocalTime.parse(StringUtil.checkVal(req.getParameter("endTimeText"), "00:00")).toSecondOfDay();

		// Each selected product must exist for the location to be a valid result
		for (int idx = 0; idx < productIds.size(); idx++) {
			String productId = productIds.get(idx);
			sql.append(DBUtil.INNER_JOIN).append("dealer_location_product_xr lp").append(idx).append(" on b.dealer_location_id = lp").append(idx).append(".dealer_location_id ");
			sql.append(" and lp").append(idx).append(".product_id = ? ");
			vals.add(productId.split("_")[1]);

			if (productId.startsWith("1_") && !daysAvailable.isEmpty()) {
				for (int dayIdx = 0; dayIdx < daysAvailable.size(); dayIdx++) {
					String idxText = idx + "_" + dayIdx;
					sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("rp_product_schedule ps").append(idxText).append(" on lp").append(idx).append(".dealer_location_product_xr_id = ps").append(idxText).append(".location_product_id ");
					sql.append(" and ps").append(idxText).append(".day_of_week_txt = ? ").append(String.format(" and ps%s.start_time_no <= ? and ? <= ps%s.end_time_no ", idxText, idxText));
					vals.add(daysAvailable.get(dayIdx));
					vals.add(startTime);
					vals.add(endTime);
				}
			}
		}
	}
}
