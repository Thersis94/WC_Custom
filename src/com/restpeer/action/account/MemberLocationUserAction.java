package com.restpeer.action.account;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// RP Libs
import com.restpeer.common.RPConstants.RPRole;
import com.restpeer.data.LocationUserVO;
import com.restpeer.data.MemberVO.MemberType;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/****************************************************************************
 * <b>Title</b>: MemberLocationUserAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the assignment of Users to a location
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 19, 2019
 * @updates:
 ****************************************************************************/

public class MemberLocationUserAction extends SBActionAdapter {
	/**
	 * Json key to access this action
	 */
	public static final String AJAX_KEY = "locationUser";
	
	/**
	 * 
	 */
	public MemberLocationUserAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public MemberLocationUserAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/**
	 * 
	 * @param dbConn
	 * @param attributes
	 */
	public MemberLocationUserAction(SMTDBConnection dbConn, Map<String, Object> attributes) {
		super();
		setDBConnection(dbConn);
		setAttributes(attributes);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.user.UserBaseWidget#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		setModuleData(getUsers(req.getParameter("memberLocationId")));
	}

	/**
	 * 
	 * @param memberLocationId
	 * @return
	 */
	public List<LocationUserVO> getUsers(String memberLocationId) {
		List<Object> vals = new ArrayList<>();
		vals.add(memberLocationId);
		
		StringBuilder sql = new StringBuilder(256);
		sql.append("select * from ").append(getCustomSchema()).append("rp_location_user_xr a ");
		sql.append("inner join role b on a.role_id = b.role_id ");
		sql.append("inner join ").append(getCustomSchema()).append("rp_user c ");
		sql.append("on a.user_id = c.user_id ");
		sql.append("where member_location_id = ? ");
		sql.append("order by last_nm, first_nm ");
		log.debug(sql.length() + "|" + sql + vals);
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), vals, new LocationUserVO());
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.user.UserBaseWidget#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		LocationUserVO loc = new LocationUserVO(req);
		
		try {
			if (req.getBooleanParameter("isDelete")) {
				delete(loc);
			} else {
				if (StringUtil.isEmpty(loc.getRoleId())) loc.setRoleId(getRoleId(req));
				save(loc);
			}
			
			setModuleData(loc);
		} catch (Exception e) {
			log.error("Unable to update location user " + loc, e);
			setModuleData(loc, 0, e.getLocalizedMessage());
		}
	}
	
	/**
	 * 
	 * @param loc
	 * @throws DatabaseException
	 */
	public void save(LocationUserVO loc) throws DatabaseException {
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			db.save(loc);
		} catch (InvalidDataException | com.siliconmtn.db.util.DatabaseException e) {
			throw new DatabaseException("Unable to save location user", e);
		}
	}
	
	/**
	 * 
	 * @param loc
	 * @throws DatabaseException
	 */
	public void delete(LocationUserVO loc) throws DatabaseException {
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			db.delete(loc);
		} catch (InvalidDataException | com.siliconmtn.db.util.DatabaseException e) {
			throw new DatabaseException("Unable to delete location user", e);
		}
	}
	
	/**
	 * Gets the role.  this is used when assigning a user to a location outside
	 * of the admin screens
	 * @param req
	 * @return
	 * @throws InvalidDataException
	 */
	public String getRoleId(ActionRequest req) throws InvalidDataException {
		MemberType mt = MemberType.valueOf(req.getParameter("memberTypeCode"));
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		
		switch (mt) {
			case KITCHEN:
				return RPRole.KITCHEN.getRoleId();
			case CUSTOMER:
				return RPRole.MEMBER.getRoleId();
			case RESTAURANT_PEER:
				if (! role.getRoleId().equals(RPRole.ADMIN.getRoleId()))
					throw new InvalidDataException("Must be admin to add a Restaurant Peer Admin");
				
				return RPRole.ADMIN.getRoleId();
			default:
				return "0";
		}
	}
}

