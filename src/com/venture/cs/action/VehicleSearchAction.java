package com.venture.cs.action;

// JDK 1.7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// SMTBaseLibs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

// WebCrescendo
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.user.SBProfileManager;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.venture.cs.action.vo.TicketVO;
import com.venture.cs.action.vo.VehicleVO;

/****************************************************************************
 *<b>Title</b>: VehicleSearchAction<p/>
 * Gathers the base information about vehicles for the venture vehicle search <p/>
 *Copyright: Copyright (c) 2013<p/>
 *Company: SiliconMountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since July 23, 2013
 * 
 * Changes:
 * Jul 23, 2013: Eric Damschroder: created class
 * Jan 31, 2014: DBargerhuff: Added comments, etc.
 ****************************************************************************/

public class VehicleSearchAction extends SBActionAdapter {
	
	public static final String SEARCH_CURRENT = "searchCurrent";
	public static final String SEARCH_ARCHIVE = "searchArchive";
	public static final String VENTURE_VEHICLE_ORG_ID = "VENTURE_RV";
	public static final int VENTURE_VEHICLE_DEALER_TYPE = 10;
	
	/**
	 * 
	 */
	public VehicleSearchAction() {
		super();
	}
	
	/**
	 * 
	 * @param arg0
	 */
	public VehicleSearchAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	/**
	 * 
	 */
	public void copy(SMTServletRequest req) throws ActionException{
		//Call the Super method which will copy the sb_action entry for the class.
		super.copy(req);	
	}
	
	/**
	 * 
	 */
	public void list(SMTServletRequest req) throws ActionException {
		log.debug("calling super.list(req)...");
		super.list(req);
	}
	
	/**
	 * 
	 */
	public void update(SMTServletRequest req) throws ActionException {
		super.update(req);
	}
	
	/**
	 * 
	 */
	public void delete(SMTServletRequest req) throws ActionException {
		super.delete(req);
	}
	
	/**
     * Retrieves the action data for a specified action id
     */
    public void retrieve(SMTServletRequest req) throws ActionException {
    	if (Convert.formatBoolean(req.getParameter(SEARCH_CURRENT))) {
    		searchVehicles(req, SEARCH_CURRENT);
    	} else if (Convert.formatBoolean(req.getParameter(SEARCH_ARCHIVE))) {
    		searchVehicles(req, SEARCH_ARCHIVE);
    	}
    	
    }
    
    /**
     * Retrieves vehicle information based on the search parameters passed in.
     * @param req
     * @throws ActionException
     */
    public void searchVehicles(SMTServletRequest req, String type) throws ActionException {
    	log.debug("searchVehicles, type: " + type);
   		String dealerId = StringUtil.checkVal(req.getParameter("dealerId"));
   		String dealerName = StringUtil.checkVal(req.getParameter("dealerName"));
   		String owner = StringUtil.checkVal(req.getParameter("owner"));
       	String vin = StringUtil.checkVal(req.getParameter("vin"));
    	
       	String sql = null;
       	if (type.equals(SEARCH_CURRENT)) {
       		sql = buildCurrentSearchString(vin, owner, dealerName, dealerId);
       	} else {
       		sql = buildArchiveSearchString(vin);
       	}
        
        List<VehicleVO> data = new ArrayList<VehicleVO>();
        ModuleVO modVo = (ModuleVO) getAttribute(Constants.MODULE_DATA);
        
        try {
        	// search
        	data = performSearch(vin, owner, dealerName, dealerId, sql, type, true);
        } catch (SQLException sqle) {
        	String errMsg = "Error searching for vehicles: " + sqle.getMessage();
        	log.error(errMsg);
        	modVo.setError(errMsg, sqle);
        }
        
        // if this is an archive search, return vin if search returned no results, we will still
        // return a data size of 0.
        if (type.equals(SEARCH_ARCHIVE)) {
	        if (data.size() == 0) {
	        	// put the VIN from the request on the data so we can populate the form
	        	VehicleVO vo = new VehicleVO();
	        	log.debug("no results, setting search vin on vo: " + vin);
	        	vo.setVin(vin);
	        	data.add(vo);
	        }
        }
        
        log.debug("modVO errorCondition/errorMessage: " + modVo.getErrorCondition() + "/" + modVo.getErrorMessage());
        modVo.setActionData(data);
        modVo.setDataSize(data.size());
        
        log.debug("actionData|dataSize: " + modVo.getActionData() + "|" + modVo.getDataSize());
        req.setAttribute(Constants.MODULE_DATA, modVo);    	
    	
    }
    
	/**
	 * Build the vehicle search sql
	 * @param vin
	 * @param owner
	 * @param dealerName
	 * @param dealerId
	 * @return
	 */
	private String buildCurrentSearchString(String vin, String owner, String dealerName, String dealerId) {
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder();
        
        //Build the base SQL statement
        sql.append("SELECT a.DEALER_ID, a.LOCATION_NM, b.VENTURE_VEHICLE_ID, b.VIN, b.OWNER_PROFILE_ID, ");
        sql.append("c.LAST_NM, c.FIRST_NM, MAX(d.ACTION_REQ_FLG) as ACTION_REQ_FLG ");
        sql.append("FROM DEALER_LOCATION a ");
        sql.append("inner join ").append(customDb).append("VENTURE_VEHICLE b on a.DEALER_ID = b.DEALER_ID ");
        sql.append("left join PROFILE c on b.OWNER_PROFILE_ID = c.PROFILE_ID ");
        sql.append("left join ").append(customDb).append("VENTURE_TICKET d on b.VENTURE_VEHICLE_ID = d.VENTURE_VEHICLE_ID ");
        sql.append("where 1 = 1 ");

        StringBuilder orderBy = new StringBuilder("a.LOCATION_NM");
        // add dealer ID or dealer name, but not both
        if (dealerId.length() > 0) {
        	// dealer ID comes from the select list
        	sql.append("AND a.DEALER_ID = ? ");
        } else {
            if (dealerName.length() > 0) {
            	// dealer name comes from text input field
            	sql.append("AND a.LOCATION_NM like ? ");
            }
        }
        
        // add owner text if specified
        if (owner.length() > 0) {
        	sql.append("AND b.OWNER_SEARCH_TXT like ? ");
        	orderBy.append(", b.OWNER_SEARCH_TXT");
        }
        
        // add vin if specified
        if (vin.length() > 0) {
        	sql.append("AND b.VIN like ? ");
        	orderBy.append(", b.VIN");
        }
        
        sql.append("GROUP BY a.DEALER_ID, a.LOCATION_NM, b.VENTURE_VEHICLE_ID, ");
        sql.append("b.VIN, b.OWNER_PROFILE_ID, b.OWNER_SEARCH_TXT, c.LAST_NM, c.FIRST_NM, d.ACTION_REQ_FLG ");
                
        // set the 'order by'
        sql.append("ORDER BY ").append(orderBy);
        log.debug("Vehicle search sql: " + SEARCH_CURRENT + ": " + sql);
        return sql.toString();
	}
	
	/**
	 * Build the vehicle search query stringl
	 * @param vin
	 * @return
	 */
	private String buildArchiveSearchString(String vin) {
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		
		StringBuilder sql = new StringBuilder();
        //Build the base SQL statement
        sql.append("SELECT b.* ");
        sql.append("FROM DEALER a ");
        sql.append("inner join ").append(customDb).append("VENTURE_VEHICLE_OWNER_ARCHIVE b on a.DEALER_ID = b.DEALER_ID ");
        sql.append("where 1 = 1 ");
        sql.append("and a.ORGANIZATION_ID = ? ");
        sql.append("and a.DEALER_TYPE_ID = ? ");
        
        // add vin if specified
        if (vin.length() > 0) {
        	sql.append("AND b.VIN like ? ");
        }
        
        //sql.append("GROUP BY a.DEALER_ID, a.LOCATION_NM, b.VIN ");
                
        // set the 'order by'
        sql.append("ORDER BY b.VIN");
        log.debug("Vehicle search sql: " + SEARCH_ARCHIVE + ": " + sql);
        return sql.toString();
	}
    
    /**
     * Performs a vehicle search
     * @param vin
     * @param owner
     * @param dealerName
     * @param dealerId
     * @param sql
     * @return
     * @throws SQLException
     */
	private List<VehicleVO> performSearch(String vin, String owner, String dealerName, 
			String dealerId, String sql, String type, boolean fuzzyVin) throws SQLException {
        List<VehicleVO> data = new ArrayList<VehicleVO>();
        PreparedStatement ps = null;
        try {
	        ps = dbConn.prepareStatement(sql);
	        int i=0;
	        String likeStr = null;
	        if (type.equals(SEARCH_ARCHIVE)) {
	        	ps.setString(++i, VENTURE_VEHICLE_ORG_ID);
	        	ps.setInt(++i, VENTURE_VEHICLE_DEALER_TYPE);
	        	
	        } else {
		        if(dealerId.length() > 0) {
		        	ps.setString(++i, dealerId);
		        	log.debug("set val " + i + " to: " + dealerId);
		        	
		        } else {
		        	if(dealerName.length() > 0) {
		        		likeStr = "%" + dealerName + "%";
		        		ps.setString(++i, likeStr);
		        		log.debug("set val " + i + " to: " + likeStr);
		        	}
		        }
		        
		        if (owner.length() > 0) {
		        	likeStr = "%" + owner + "%";
		        	ps.setString(++i, "%" + owner + "%");
		        	log.debug("set val " + i + " to: " + likeStr);
		        }
	        }
	        
	        if(vin.length() > 0) {
	        	if (fuzzyVin) {
	        		likeStr = "%" + vin + "%";
	        	} else {
	        		likeStr = vin;
	        	}
	        	ps.setString(++i, likeStr);
	        	log.debug("set val " + i + " to: " + likeStr);
	        }
	        	        
	        ResultSet rs = ps.executeQuery();
	        VehicleVO vo = null;
	        StringEncrypter se = null;
	        try {
	        	se = new StringEncrypter((String) getAttribute(Constants.ENCRYPT_KEY));
	        } catch (EncryptionException ee) {
	        	log.error("Error instantiating StringEncrypter, ", ee);
	        }
	        
	        if (type.equals(SEARCH_ARCHIVE)) {
	        	if (rs.next()) {
	        		vo = new VehicleVO(rs);
	        		parseSpecificData(rs,vo,se,type);
	        		data.add(vo);
	        	}
	        } else {
		        // Each row has both the vehicle and one ticket, we set both
		        String prevId = "";
		        String currId = null;
		        int actionFlag = 0;
		        while (rs.next()) {
		        	currId = rs.getString("VENTURE_VEHICLE_ID");
		        	if (currId.equals(prevId)) {
		        		// if another record for same vehicle, just check for action required
			        	actionFlag = rs.getInt("ACTION_REQ_FLG");
			        	if (actionFlag > vo.getRequiresAction()) {
			        		TicketVO t = new TicketVO(rs);
			        		vo.addTicket(t);
			        	}
			        	
		        	} else {
		        		if (vo != null) {
		        			data.add(vo);
		        		}
			    		vo = new VehicleVO(rs);
			    		parseSpecificData(rs, vo, se, type);	        		
		        	}
		        	
		            prevId = currId;
		            actionFlag = 0;
		            
		        }
		        
		        // pick up the dangling record.
		        if (vo != null) data.add(vo);
	        }
	        
        } finally {
        	if (ps != null) {
        		try {
        			ps.close();
        		} catch (Exception e) {}
        	}
        	
        }
        
        return data;
		
	}
	
	/**
	 * Parses owner information to set on the VehicleVO based on the search type
	 * @param rs
	 * @param vo
	 * @param se
	 * @param type
	 */
	private void parseSpecificData(ResultSet rs, VehicleVO vo, StringEncrypter se, String type) {
		
		if (type.equals(SEARCH_CURRENT)) {
			// decrypt first/last names
			if (se != null) {
    			try {
	    			if (vo.getOwner().getFirstName() != null) {
	    				vo.getOwner().setFirstName(se.decrypt(vo.getOwner().getFirstName()));
	    			}
	    			if (vo.getOwner().getLastName() != null) {
	    				vo.getOwner().setLastName(se.decrypt(vo.getOwner().getLastName()));
	    			}
    			} catch (EncryptionException ee) {
    				StringBuilder errMsg = new StringBuilder("Error decrypting vehicle owner name data: ");
    				errMsg.append(vo.getOwner().getFirstName()).append(" ").append(vo.getOwner().getLastName());
    				errMsg.append(", ").append(ee.getMessage());
    				log.error(errMsg);
    			}
			}
		} else if (type.equals(SEARCH_ARCHIVE)) {
			// build owner info from archive data
			try {
				UserDataVO uvo = new UserDataVO();
				uvo.setName(rs.getString("OWNER_NM"));
				uvo.setAddress(rs.getString("ADDRESS_TXT"));
				uvo.setCity(rs.getString("CITY_NM"));
				uvo.setState(rs.getString("STATE_CD"));
				uvo.setZipCode(rs.getString("ZIP_CD"));
				uvo.setCountryCode(rs.getString("COUNTRY_CD"));
				vo.setOwner(uvo);
				
				// parse date info
				vo.setPurchaseYear(StringUtil.checkVal(rs.getString("PURCHASE_DT")));
				String year = vo.getPurchaseYear();
				if (year.length() > 4) {
					year = year.substring(year.length() - 4);
				}
				vo.setYear(year);
				
			} catch (SQLException sqle) {
				log.error("Error setting owner information for archival vehicle, ", sqle);
			}
		}
		
	}
	
	/**
	 * Inserts or updates a base vehicle record.  If the vehicle already exists
	 */
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("VehicleSearchAction build...");
    	final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
        String msg = null;
        
        // get the vehicle info from the request.
        VehicleVO vehicle = new VehicleVO(req);
        
        // check to see if the vehicle/VIN is already in the current vehicle table.
        String tmpVehicleId = null;
        try {
        	tmpVehicleId = checkCurrentVehicles(vehicle);
        } catch (SQLException sqle) {
        	log.error("Error checking current vehicle table before new case creation, ", sqle);
        	throw new ActionException(sqle.getMessage());
        }

        if (tmpVehicleId != null) {
        	// this is a duplicate case creation attempt, set message for the redirect.
        	msg = "A case associated with this VIN already exists and you have been redirected to the case.";

        } else {
        	// insert the vehicle, create owner's profile.
	        StringBuilder sb = new StringBuilder();
	        sb.append("INSERT INTO ").append(customDb).append("VENTURE_VEHICLE ");
	        sb.append("(VIN, DEALER_ID, OWNER_PROFILE_ID, MAKE, MODEL, YEAR, ");
	        sb.append("PURCHASE_DT, FREEZE_FLG, OWNER_SEARCH_TXT, CREATE_DT, VENTURE_VEHICLE_ID) ");
	        sb.append("VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
	        log.debug("Venture vehicle sql: " + sb.toString());
	        
	        // Get a new ID
	        vehicle.setVehicleId(new UUIDGenerator().getUUID());
	        msg = "You successfully created a new case.";
	        
	        String profileId = null; 
	        PreparedStatement ps = null;
	        try {
	        	profileId = updateProfile(req);
	            ps = dbConn.prepareStatement(sb.toString());
	            ps.setString(1, vehicle.getVin());
	            ps.setString(2, vehicle.getDealer().getDealerId());
	            ps.setString(3, profileId);
	            ps.setString(4, vehicle.getMake());
	            ps.setString(5, vehicle.getModel());
	            ps.setString(6, vehicle.getYear());
	            ps.setString(7, vehicle.getPurchaseYear());
	            ps.setInt(8, vehicle.getFreezeFlag());
	            ps.setString(9, vehicle.getOwner().getLastName());
	            ps.setTimestamp(10, Convert.getCurrentTimestamp());
	            ps.setString(11, vehicle.getVehicleId());
	            ps.executeUpdate();
	        } catch (DatabaseException de) {
	            log.error("Error creating owner's profile for this case,", de);
	            msg = "Error creating owner's profile for this case, " + de.getMessage();
	        } catch (SQLException sqle) {
	            log.error("Error inserting vehicle record: ",sqle);
	        } finally {
	        	if (ps != null) {
		        	try {
		        		ps.close();
		        	} catch(Exception e) {}
	        	}
	        }
        }
        
    	StringBuffer url = new StringBuffer();
    	url.append("/result?vehicleId=").append(vehicle.getVehicleId());
        if (msg != null) url.append("&msg=").append(msg);
        log.debug("VehicleAction 'build' redirect url: " + url.toString());
    	req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
    	req.setAttribute(Constants.REDIRECT_URL, url.toString());

    }
	
	/**
	 * Searches the current vehicle table for the existence of the vehicle VIN.  This check
	 * is performed before a new case is created so that a duplicate creation attempt is not 
	 * performed.
	 * @param v
	 * @return
	 * @throws SQLException 
	 */
	private String checkCurrentVehicles(VehicleVO v) throws SQLException {
        // make sure this vehicle isn't already in the 'current' vehicle db.
		log.debug("checking current VINs...");
		String id = null;
        String sb = this.buildCurrentSearchString(v.getVin(), "", "", "");
       	List<VehicleVO> found = this.performSearch(v.getVin(), "", "", "", sb, SEARCH_CURRENT, false);
       	if (found.size() > 0) {
       		id = found.get(0).getVehicleId();
           	log.debug("found vehicle with ID: " + id);
       	}
       	return id;
	}
    
    /**
     * Update or create the vehicle owner
     * @throws DatabaseException 
     * 
     */
    private String updateProfile(SMTServletRequest req) throws SQLException, ActionException, DatabaseException {
        String profileId = StringUtil.checkVal(req.getParameter("profileId"));
        /* TODO remove if not used
        if(!StringUtil.checkVal(req.getParameter("unitedStates")).equals(""))
    		req.setParameter("state", req.getParameter("unitedStates"));
    	else
    		req.setParameter("state", req.getParameter("canada"));
    	*/
    	SBProfileManager sb = new SBProfileManager(attributes);
    	UserDataVO user = new UserDataVO(req);
    	
    	if(profileId.length() == 0) profileId = sb.checkProfile(user, dbConn);
        sb.updateProfile(user, dbConn);
        
        return user.getProfileId();
    }
	
}
