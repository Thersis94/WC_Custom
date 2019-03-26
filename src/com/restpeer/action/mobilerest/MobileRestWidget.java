package com.restpeer.action.mobilerest;

import java.util.ArrayList;
import java.util.List;

import com.restpeer.data.MemberLocationVO;
// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
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
		
		StringBuilder sql = new StringBuilder(384);
		sql.append("select *, core.geoCalcDistance(cast(? as numeric), cast(? as numeric), ");
		sql.append("b.latitude_no, b.longitude_no, 'mi') as distance  ");
		sql.append("from rp_member a ");
		sql.append("inner join rp_member_location b on a.member_id = b.member_id ");
		sql.append("where member_type_cd = 'KITCHEN' ");
		sql.append("order by distance limit 10 ");
		log.debug(sql.length() + "|" + sql);
		
		List<Object> vals = new ArrayList<>();
		vals.add(ml.getLatitude());
		vals.add(ml.getLongitude());
		
		return db.executeSelect(sql.toString(), vals, new MemberLocationVO());
	}
}
