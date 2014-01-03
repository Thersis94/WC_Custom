package com.venture.cs.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.common.constants.ErrorCode;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.dealer.DealerLocationVO;
import com.smt.sitebuilder.action.user.SBProfileManager;
import com.smt.sitebuilder.admin.action.SBModuleAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.RecordDuplicatorUtility;

/****************************************************************************
 *<b>Title</b>: VehicleAction<p/>
 * Gathers the base information about vehicles for the venture vehicle search <p/>
 *Copyright: Copyright (c) 2013<p/>
 *Company: SiliconMountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since July 23, 2013
 ****************************************************************************/

public class VehicleAction extends SBActionAdapter {
	
	public VehicleAction() {
		super();
	}

	public VehicleAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	public void copy(SMTServletRequest req) throws ActionException{
		Object msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		Boolean isWizard = Convert.formatBoolean(req.getParameter("isWizard"));
		
		//Call the Super method which will copy the sb_action entry for the class.
		super.copy(req);	
		
		//Build our RecordDuplicatorUtility and set the where clause
		RecordDuplicatorUtility rdu = new RecordDuplicatorUtility(attributes, dbConn, "VENTURE_VEHICLE_SEARCH", "VENTURE_VEHICLE_SEARCH_ID", isWizard);
		
		if(req.hasParameter(SB_ACTION_ID))
			rdu.addWhereClause(DB_ACTION_ID, (String) req.getParameter(SB_ACTION_ID));
		else
			rdu.setWhereSQL(rdu.buildWhereListClause(DB_ACTION_ID));

		rdu.copy();
		
		// Redirect the user
		sbUtil.moduleRedirect(req, msg, (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
	}
	
	public void list(SMTServletRequest req) throws ActionException {
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String actionId = req.getParameter(SBModuleAction.SB_ACTION_ID);
		
		if (actionId == null || actionId.length() == 0) return;
		ModuleVO mod = (ModuleVO) attributes.get(AdminConstants.ADMIN_MODULE_DATA);
		
		StringBuilder sql = new StringBuilder();
		sql.append("select ACTION_ID, ACTION_DESC, ACTION_NM, DEALER_ID, ");
		sql.append("ORGANIZATION_ID, ACTION_GROUP_ID, PENDING_SYNC_FLG ");
        sql.append("FROM SB_ACTION sa left outer join ").append(customDb);
        sql.append("VENTURE_VEHICLE_SEARCH vvs ");
		sql.append("ON sa.ACTION_ID = vvs.VENTURE_VEHICLE_SEARCH_ID ");
		sql.append("where sa.action_id = ? ");
		
		log.info("Venture Vehicle Search List SQL: " + sql.toString() + "|" + actionId);
		VehicleVO vo = new VehicleVO();
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, actionId);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				vo = new VehicleVO();
				vo.setActionId(rs.getString(1));
				vo.setActionDesc(rs.getString(2));
				vo.setActionName(rs.getString(3));
				vo.setDealer(new DealerLocationVO());
				vo.getDealer().setDealerTypeId(rs.getString(4));
				vo.setOrganizationId(rs.getString(5));
                vo.setActionGroupId(rs.getString(6));
                vo.setPendingSyncFlag(rs.getInt(7));
				String built = rs.getString(1);
				if (built != null && built.length() > 0) vo.setBuilt(true);
			}
		} catch (SQLException sqle) {
			throw new ActionException("Error Gettting Venture Vehicle Search Action: " + sqle.getMessage());
		} finally {
        	if (ps != null) {
	        	try {
	        		ps.close();
	        	} catch(Exception e) {}
        	}
		}
		
		
		// Store the retrieved data in the ModuleVO.actionData and replace into
		// the Map
		mod.setActionData(vo);
        this.setAttribute(AdminConstants.ADMIN_MODULE_DATA, mod);		
	}
	
	public void update(SMTServletRequest req) throws ActionException {
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		super.update(req);
		String actionId = (String) req.getAttribute(SB_ACTION_ID);
		
        // Build the sql
        StringBuilder sql = new StringBuilder();
		if (Convert.formatBoolean(req.getAttribute(INSERT_TYPE))) {
			sql.append("insert into ").append(customDb);
			sql.append("VENTURE_VEHICLE_SEARCH (DEALER_ID, CREATE_DT, VENTURE_VEHICLE_SEARCH_ID ) ");
			sql.append("values (?,?,?)");
			
		} else {
			sql.append("update ").append(customDb);
			sql.append("VENTURE_VEHICLE_SEARCH set DEALER_ID = ?, CREATE_DT = ? ");
			sql.append("where VENTURE_VEHICLE_SEARCH_ID = ?");
		}
		log.debug(sql+"|"+req.getParameter("dealerType")+"|"+actionId);
		// perform the execute
		PreparedStatement ps = null;
		Object msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(1, Integer.parseInt(req.getParameter("dealerType")));
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			ps.setString(3, actionId);
			
            if (ps.executeUpdate() < 1) {
                msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
                log.info("No records updated: " + ps.getWarnings());
            }
		} catch (SQLException sqle) {
            msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
            log.error("Error Update Content", sqle);
		} finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch(Exception e) {}
            }
        }
		
		// Redirect after the update
        sbUtil.moduleRedirect(req, msg, (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
	}
	
	public void delete(SMTServletRequest req) throws ActionException {
    	final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		super.delete(req);
		StringBuffer sql = new StringBuffer();
		sql.append("DELETE FROM ").append(customDb).append("VENTURE_VEHICLE_SEARCH ");
		sql.append("WHERE VENTURE_VEHICLE_SEARCH_ID = ?");
		
		
		try {
			PreparedStatement ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, (String) req.getAttribute(SB_ACTION_ID));
			ps.execute();
		} catch (SQLException e) {
			log.error("Could not delete Venture Vehicle Search", e);
		}
	}
	
	/**
     * Retrieves the action data for a specified action id
     */
    public void retrieve(SMTServletRequest req) throws ActionException {
    	if (Convert.formatBoolean(req.getParameter("searchDone"))) {
    		retrieveVehicles(req);
    	} else {
    		retrieveAction(actionInit.getActionId());
    	}
    }
    
    /**
     * Gets the dealer type so that we will only show certain dealers in the search from
     * @param actionId
     */
    private void retrieveAction(String actionId) {
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder();
    	
    	ModuleVO modVo = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		
		sql.append("SELECT DEALER_ID FROM ").append(customDb).append("VENTURE_VEHICLE_SEARCH ");
		sql.append("WHERE VENTURE_VEHICLE_SEARCH_ID = ?");
		
		PreparedStatement ps;
		VehicleVO vo = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, actionId);
			
			ResultSet rs = ps.executeQuery();
			
			if (rs.next()) {
				vo = new VehicleVO();
				vo.setDealer(new DealerLocationVO());
				vo.getDealer().setDealerTypeId(rs.getString(1));
				
			}
		} catch (SQLException e) {
			log.error("Could not retrieve search module, ", e);
            modVo.setError(ErrorCode.SQL_ERROR, e);
		}
		
        modVo.setDataSize(1);
        modVo.setActionData(vo);
        
        setAttribute(Constants.MODULE_DATA, modVo);
	}

    /**
     * Get the vehicles for a submitted search
     * @param req
     */
	private void retrieveVehicles(SMTServletRequest req) {
    	SBProfileManager sb = new SBProfileManager(attributes);
    	
    	String vin = StringUtil.checkVal(req.getParameter("vin"));
    	String owner = StringUtil.checkVal(req.getParameter("owner"));
    	String dealerName = StringUtil.checkVal(req.getParameter("dealerName"));
    	String dealerId = StringUtil.checkVal(req.getParameter("dealerId"));
    	
    	log.debug(actionInit.getActionId());
    	
    	ModuleVO modVo = (ModuleVO) getAttribute(Constants.MODULE_DATA);
        
        String sql = buildSearchString(vin, owner, dealerName, dealerId);
        
        log.info("Vehicle Action SQL: " + sql);
        PreparedStatement ps = null;
        List<VehicleVO> data = new ArrayList<VehicleVO>();
        try {
    		StringEncrypter se = new StringEncrypter((String) getAttribute(Constants.ENCRYPT_KEY));
            ps = dbConn.prepareStatement(sql.toString());
            int i=0;
            if(vin.length() > 0)
            	ps.setString(++i, "%" + vin + "%");
            if(dealerName.length() > 0)
            	ps.setString(++i, "%" + dealerName + "%");
            if(dealerId.length() > 0)
            	ps.setString(++i, dealerId);
            ResultSet rs = ps.executeQuery();
            VehicleVO vo;
            
            // Check our search parameters and add any that are relevent
        	List<String> owners = new ArrayList<String>();
            if (owner.length() > 0) {
            	Map<String, String> searchVals = new HashMap<String, String>();
            	searchVals.put("LAST_NM", owner);
            	try {
    				for(UserDataVO user : sb.searchProfile(dbConn, searchVals)) {
    					owners.add(user.getProfileId());
    				}
    			} catch (DatabaseException e) {
    				log.error("Unable to get users from database ", e);
    			}
            }
            
            // Each row has both the vehicle and one ticket, we set both
            while (rs.next()) {
            	if(owner.length() > 0 && !owners.contains(rs.getString("OWNER_ID")))
            		continue;
        		vo = new VehicleVO(rs);
        		vo.setDealer(new DealerLocationVO());
        		vo.getDealer().setDealerTypeId(rs.getString("DEALER_ID"));
        		vo.getOwner().setFirstName(se.decrypt(vo.getOwner().getFirstName()));
        		vo.getOwner().setLastName(se.decrypt(vo.getOwner().getLastName()));
        		vo.getOwner().setEmailAddress(se.decrypt(vo.getOwner().getEmailAddress()));
        		vo.addTicket(new TicketVO(rs));
                data.add(vo);
            }
        } catch (SQLException sqle) {
        	log.error(sqle);
            modVo.setError(ErrorCode.SQL_ERROR, sqle);
        } catch (EncryptionException e) {
        	log.error("Unable to decrypt user data ", e);
		}  finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch(Exception e) {}
            }
        }
        modVo.setDataSize(data.size());
        modVo.setActionData(data);
        log.debug("actionData=" + modVo.getActionData());
        
        setAttribute(Constants.MODULE_DATA, modVo);
       
		
	}

	/**
	 * Build the vehicle search sql
	 * @param vin
	 * @param owner
	 * @param dealerName
	 * @param dealerId
	 * @return
	 */
	private String buildSearchString(String vin, String owner,
			String dealerName, String dealerId) {
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder();
        StringBuilder searchString = new StringBuilder();
        
        //Build the base SQL statement
        sql.append("SELECT vv.OWNER_ID, vv.VENTURE_VEHICLE_ID, vv.VIN, p.FIRST_NM, p.LAST_NM, dl.LOCATION_NM, MAX(vt.ACTION_REQ_FLG) as ACTION_REQ_FLG, ");
        sql.append("vvs.DEALER_ID FROM ").append(customDb).append("VENTURE_VEHICLE vv ");
        sql.append("left join ").append(customDb).append("VENTURE_TICKET vt on vv.VENTURE_VEHICLE_ID = vt.VENTURE_VEHICLE_ID ");
        sql.append("left join PROFILE p on p.PROFILE_ID = vv.OWNER_ID ");
        sql.append("left join DEALER_LOCATION dl on dl.DEALER_ID = vv.DEALER_ID ");
        sql.append("left join ").append(customDb).append("VENTURE_VEHICLE_SEARCH vvs on vvs.VENTURE_VEHICLE_SEARCH_ID = vv.VENTURE_VEHICLE_SEARCH_ID ");
        
        if (vin.length() > 0)
        	searchString.append("OR vv.VIN like ? "); 
        if (dealerName.length() > 0)
        	searchString.append("OR dl.LOCATION_NM like ?");
        if (dealerId.length() > 0)
        	searchString.append("OR dl.DEALER_ID = ?");
        
        //If we created any search parameters we add the begining of the where line
        if (searchString.length() > 0)
        	sql.append("WHERE 1=2 ").append(searchString.toString());
        
        sql.append(" GROUP BY  vv.OWNER_ID, vv.VENTURE_VEHICLE_ID, vv.VIN, p.FIRST_NM, p.LAST_NM, dl.LOCATION_NM, vvs.DEALER_ID ");
        
        return sql.toString();
	}

	public void build(SMTServletRequest req) throws ActionException {
    	final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
        String sbActionId = StringUtil.checkVal(req.getParameter(SB_ACTION_ID));
    	String vehicleId = StringUtil.checkVal(req.getParameter("vehicleId"));
        Boolean isInsert = (vehicleId.length() == 0);
        StringBuilder sb = new StringBuilder();
        
        if (isInsert) {  
            sb.append("INSERT INTO ").append(customDb).append("VENTURE_VEHICLE ");
            sb.append("(VIN, DEALER_ID, OWNER_ID, MAKE, MODEL, YEAR, ");
            sb.append("PURCHASE_DT, FREEZE_FLG, CREATE_DT, VENTURE_VEHICLE_SEARCH_ID, VENTURE_VEHICLE_ID) ");
            sb.append("VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            
            // Get a new ID
            vehicleId = new UUIDGenerator().getUUID();
            
        } else {
            sb.append("UPDATE ").append(customDb).append("VENTURE_VEHICLE ");
            sb.append("SET VIN=?, DEALER_ID=?, OWNER_ID=?, MAKE=?, MODEL=?, ");
            sb.append("YEAR=?, PURCHASE_DT=?, FREEZE_FLG=?, UPDATE_DT=?, VENTURE_VEHICLE_SEARCH_ID = ? ");
            sb.append("WHERE VENTURE_VEHICLE_ID=?");
           
        }
        
        String profileId = null; 
        PreparedStatement ps = null;
        try {
        	profileId = updateProfile(req);
            ps = dbConn.prepareStatement(sb.toString());
            ps.setString(1, req.getParameter("vin"));
            ps.setString(2, req.getParameter("dealerId"));
            ps.setString(3, profileId);
            ps.setString(4, req.getParameter("make"));
            ps.setString(5, req.getParameter("model"));
            ps.setString(6, req.getParameter("year"));
            ps.setString(7, req.getParameter("purchaseDate"));
            ps.setInt(8, Convert.formatInteger(req.getParameter("freezeFlag")));
            ps.setTimestamp(9, Convert.getCurrentTimestamp());
            ps.setString(10, sbActionId);
            ps.setString(11, vehicleId);
            
            if (ps.executeUpdate() < 1)
                throw new ActionException("Error Updating SBAction"); 
        } catch (Exception sqle) {
            log.error("Error updating SB Action: ",sqle);
        } finally {
        	if (ps != null) {
	        	try {
	        		ps.close();
	        	} catch(Exception e) {}
        	}
        }
        
    }
    
    /**
     * Update or create the vehicle owner
     * @throws DatabaseException 
     * 
     */
    private String updateProfile(SMTServletRequest req) throws SQLException, ActionException, DatabaseException {
        String profileId = StringUtil.checkVal(req.getParameter("profileId"));
        if(!StringUtil.checkVal(req.getParameter("unitedStates")).equals(""))
    		req.setParameter("state", req.getParameter("unitedStates"));
    	else
    		req.setParameter("state", req.getParameter("canada"));
    	
    	SBProfileManager sb = new SBProfileManager(attributes);
    	UserDataVO user = new UserDataVO(req);
    	
    	if(profileId == "")
    		profileId = sb.checkProfile(user, dbConn);
        
        sb.updateProfile(user, dbConn);
        
        return user.getProfileId();
    }
	
}
