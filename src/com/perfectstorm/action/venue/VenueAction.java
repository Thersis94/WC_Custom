package com.perfectstorm.action.venue;

// JDK 1.8.x
import java.util.Map;

// SMT Base Libs 3.x
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.action.SimpleActionAdapter;

//WC Libs 3.x
import com.smt.sitebuilder.common.constants.Constants;

// Perfect Storm Libs
import com.perfectstorm.data.VenueTourVO;

/****************************************************************************
 * <b>Title</b>: VenueAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the venues widget
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Feb 15, 2019
 * @updates:
 ****************************************************************************/

public class VenueAction extends SimpleActionAdapter {
	
	/**
	 * Key for the Facade / Ajax Controller to utilize when calling this class
	 */
	public static final String AJAX_KEY = "venue";
	
	/**
	 * 
	 */
	public VenueAction() {
		super();
	}
	
	/**
	 * 
	 * @param attributes
	 * @param dbConn
	 */
	public VenueAction(Map<String, Object> attributes, SMTDBConnection dbConn ) {
		super();
		
		this.attributes = attributes;
		this.dbConn = dbConn;
	}

	/**
	 * @param actionInit
	 */
	public VenueAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		UserDataVO profile = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		String venueTourId = req.getParameter("venueTourId");
		boolean isJson = req.getBooleanParameter("json");
		
		try {
			putModuleData(getVenueTour(venueTourId));
		} catch (DatabaseException | InvalidDataException e) {
			log.error("Unable to retrieve venue tour event: " + venueTourId, e);
			this.putModuleData("", 0, false, e.getLocalizedMessage(), true);
		}
	}
	
	/**
	 * Gets the specified event.
	 * 
	 * @param venueTourId
	 * @return
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public VenueTourVO getVenueTour(String venueTourId) throws InvalidDataException, DatabaseException {
		VenueTourVO event = new VenueTourVO();
		event.setVenueTourId(venueTourId);
		
		DBProcessor dbp = new DBProcessor(getDBConnection(), getCustomSchema());
		dbp.getByPrimaryKey(event);
		
		return event;
	}
}

