package com.venture.cs.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.common.constants.ErrorCode;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.PhoneVO;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.siliconmtn.util.databean.FilePartDataBean;
import com.smt.sitebuilder.action.FileLoader;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.dealer.DealerLocationVO;
import com.smt.sitebuilder.action.user.SBProfileManager;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 *<b>Title</b>: TicketAction<p/>
 * Gathers all the information related to a vehicle's tickets.
 * This includes the owner, the vehicle, the dealer it is associated with,
 * all files related to the tickets, and the list of all actions taken on
 * the associated vehicle. <p/>
 *Copyright: Copyright (c) 2013<p/>
 *Company: SiliconMountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since July 23, 2013
 ****************************************************************************/

public class OverviewAction extends SBActionAdapter {
	
	public OverviewAction() {
		super();
	}

	public OverviewAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	/**
     * Retrieves the action data for a specified action id
     */
    public void retrieve(SMTServletRequest req) throws ActionException {
    	String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
    	String vehicleId = req.getParameter("vehicleId");

    	ModuleVO modVo = (ModuleVO) getAttribute(Constants.MODULE_DATA);

        // Get the vehicle and owner information
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT 1 as rank, vv.VENTURE_VEHICLE_ID, vv.VIN, vv.MAKE, vv.MODEL, vv.YEAR, vv.PURCHASE_DT, vv.FREEZE_FLG, ");
        sql.append("p.PROFILE_ID, p.FIRST_NM, p.LAST_NM, p.EMAIL_ADDRESS_TXT, pn.PHONE_NUMBER_TXT, pa.ADDRESS_TXT, ");
        sql.append("pa.ADDRESS2_TXT, pa.CITY_NM, pa.CITY_NM, pa.ZIP_CD, null as LOCATION_NM, pa.STATE_CD, ");
        sql.append("null as VENTURE_TICKET_ID, null as FILE_URL, null as COMMENT, null as CREATE_DT, null as ACTION_REQ_FLG ");
        sql.append("FROM ").append(customDb).append("VENTURE_VEHICLE vv ");
        sql.append("left join PROFILE p on p.PROFILE_ID = vv.OWNER_ID ");
        sql.append("left join PROFILE_ADDRESS pa on p.PROFILE_ID = pa.PROFILE_ID ");
        sql.append("left join PHONE_NUMBER pn on p.PROFILE_ID = pn.PROFILE_ID ");
        sql.append("WHERE vv.VENTURE_VEHICLE_ID = ? ");
        sql.append("union ");
        
        //Get the dealership information
        sql.append("SELECT 2 as rank, null as VENTURE_VEHICLE_ID, vv.VIN, vv.MAKE, vv.MODEL, vv.YEAR, vv.PURCHASE_DT, vv.FREEZE_FLG, ");
        sql.append("null as PROFILE_ID, null as FIRST_NM, null as LAST_NM, EMAIL_ADDRESS_TXT, PRIMARY_PHONE_NO as PHONE_NUMBER_TXT, ");
        sql.append("dl.ADDRESS_TXT, dl.ADDRESS2_TXT, dl.CITY_NM, dl.CITY_NM, dl.ZIP_CD, dl.LOCATION_NM, dl.STATE_CD, ");
        sql.append("null as VENTURE_TICKET_ID, null as FILE_URL, null as COMMENT, null as CREATE_DT, null as ACTION_REQ_FLG ");
        sql.append("FROM ").append(customDb).append("VENTURE_VEHICLE vv ");
        sql.append("left join DEALER_LOCATION dl on dl.DEALER_ID = vv.DEALER_ID ");
        sql.append("WHERE vv.VENTURE_VEHICLE_ID = ? ");
        sql.append("union ");
        
        //Get the ticket and file information
        sql.append("SELECT 3 as rank, null as VENTURE_VEHICLE_ID, null as VIN, null as MAKE, null as MODEL, null as YEAR, null as PURCHASE_DT, null as FREEZE_FLG, ");
        sql.append("null as PROFILE_ID, p.FIRST_NM, p.LAST_NM, null as EMAIL_ADDRESS_TXT, null as PHONE_NUMBER_TXT, ");
        sql.append("null as ADDRESS_TXT, null as ADDRESS2_TXT, null as CITY_NM, null as CITY_NM, null as ZIP_CD, null as LOCATION_NM, null as STATE_CD,  ");
        sql.append("vt.VENTURE_TICKET_ID, vtf.FILE_URL, cast(vt.COMMENT as nvarchar(1000)), vt.CREATE_DT, vt.ACTION_REQ_FLG ");
        sql.append("FROM ").append(customDb).append("VENTURE_TICKET vt ");
        sql.append("left join ").append(customDb).append("VENTURE_VEHICLE vv on vv.VENTURE_VEHICLE_ID = vt.VENTURE_VEHICLE_ID ");
        sql.append("left join ").append(customDb).append("VENTURE_TICKET_FILE vtf on vtf.VENTURE_TICKET_ID = vt.VENTURE_TICKET_ID ");
        sql.append("left join PROFILE p on p.PROFILE_ID = vt.SUBMITTED_BY ");
        sql.append("WHERE vv.VENTURE_VEHICLE_ID = ? ");
        sql.append("union ");
        
        //Get the activity trail for this vehicle
        sql.append("SELECT 4 as rank, null as VENTURE_VEHICLE_ID, null as VIN, null as MAKE, null as MODEL, null as YEAR, null as PURCHASE_DT, null as FREEZE_FLG, ");
        sql.append("null as PROFILE_ID, p.FIRST_NM, p.LAST_NM, null as EMAIL_ADDRESS_TXT, null as PHONE_NUMBER_TXT, ");
        sql.append("null as ADDRESS_TXT, null as ADDRESS2_TXT, null as CITY_NM, null as CITY_NM, null as ZIP_CD, null as LOCATION_NM, null as STATE_CD, ");
        sql.append("null as VENTURE_TICKET_ID, null as FILE_URL, cast(vat.COMMENT as nvarchar(1000)) as COMMENT, vat.CREATE_DT, null as ACTION_REQ_FLG ");
        sql.append("FROM ").append(customDb).append("VENTURE_ACTIVITY_TRAIL vat ");
        sql.append("left join PROFILE p on p.PROFILE_ID = vat.PROFILE_ID ");
        sql.append("WHERE vat.VENTURE_VEHICLE_ID = ? ");
        sql.append("ORDER BY RANK, CREATE_DT ASC ");
        
        log.info("Ticket Action SQL: " + sql + "|" + vehicleId);
        PreparedStatement ps = null;
        VehicleVO vo = new VehicleVO();
        try {
			String encKey = (String) getAttribute(Constants.ENCRYPT_KEY);
			StringEncrypter se = new StringEncrypter(encKey);
            ps = dbConn.prepareStatement(sql.toString());
            ps.setString(1, vehicleId);
            ps.setString(2, vehicleId);
            ps.setString(3, vehicleId);
            ps.setString(4, vehicleId);
            
            ResultSet rs = ps.executeQuery();
            String lastTicket = null;
            // Only retrieve the records that will be displayed on that page
            while (rs.next()) {
        		switch(rs.getInt("rank")) {
	        		case 1:
	        			vo = new VehicleVO(rs);
	            		vo.getOwner().setFirstName(se.decrypt(vo.getOwner().getFirstName()));
	            		vo.getOwner().setLastName(se.decrypt(vo.getOwner().getLastName()));
	            		vo.getOwner().setEmailAddress(se.decrypt(vo.getOwner().getEmailAddress()));
	            		vo.getOwner().setAddress(se.decrypt(vo.getOwner().getAddress()));
	            		vo.getOwner().addPhone(new PhoneVO(se.decrypt(rs.getString("PHONE_NUMBER_TXT"))));
	        			break;
	        		case 2:
	        			vo.setDealer(new DealerLocationVO(rs));
	        			vo.getDealer().setPhone(rs.getString("PHONE_NUMBER_TXT"));
	        			break;
	        		case 3:
	        			if (!rs.getString("VENTURE_TICKET_ID").equals(lastTicket)) {
	        				lastTicket = rs.getString("VENTURE_TICKET_ID");
	        				TicketVO ticket = new TicketVO(rs);
	        				ticket.setFirstName(se.decrypt(ticket.getFirstName()));
	        				ticket.setLastName(se.decrypt(ticket.getLastName()));
	        				if (StringUtil.checkVal(rs.getString("FILE_URL")).length() > 0) {
		        				ticket.addFile(rs.getString("FILE_URL"));
	        				}
	        				vo.addTicket(ticket);
	        			} else if (StringUtil.checkVal(rs.getString("FILE_URL")).length() > 0 && vo.getTickets().size() > 0) {
	        				vo.getTickets().get(vo.getTickets().size()-1).addFile(rs.getString("FILE_URL"));
	        			}
	        			break;
	        		case 4:
	        			ActivityVO activity = new ActivityVO(rs);
	        			activity.setFirstName(se.decrypt(activity.getFirstName()));
	        			activity.setLastName(se.decrypt(activity.getLastName()));
	        			vo.addActivity(activity);
	        			break;
        		}
            }
            
        } catch (SQLException sqle) {
        	log.error(sqle);
            modVo.setError(ErrorCode.SQL_ERROR, sqle);
        } catch (EncryptionException e) {
        	log.error("Unable to decrypt user information ", e);
		} finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch(Exception e) {}
            }
        }
        modVo.setActionData(vo);
        log.debug("actionData=" + modVo.getActionData());
        
		// Add the data for viewing
        setAttribute(Constants.MODULE_DATA, modVo);
    }

    /**
     * Looks at the the ReqType parameter in order to figure out what we need to do with the request
     */
    public void build(SMTServletRequest req) throws ActionException {
    	String reqType = StringUtil.checkVal(req.getParameter("reqType"));
    	log.debug("ReqType="+reqType);
    	
    	String notificationMsg = "";
    	if (reqType.equals("addTicket")) {
    		addTicket(req);
    		notificationMsg = "added a ticket";
    	} else if (reqType.equals("freeze")) {
    		freezeCase(req);
    		if ("1".equals(req.getParameter("freezeFlag")))
    			notificationMsg = "froze this case";
    		else
    			notificationMsg = "unfroze this case";
    	} else if (reqType.equals("follow")) {
    		followCase(req);
    		notificationMsg = "followed this case";
    	} else if (reqType.equals("closeTicket")) {
    		closeTicket(req);
    		notificationMsg = "closed a ticket";
    	} else if (reqType.equals("editOwner")) {
    		changeOwner(req);
    		notificationMsg = "changed the vehicle's owner";
    	}
    	
    	addNotification(req, notificationMsg);
    	
        req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
    	req.setAttribute(Constants.REDIRECT_URL, req.getQueryString() + "result?vehicleId=" + req.getParameter("vehicleId"));	
    
    }
    
    /**
     * Adds a ticket to the current vehicle.
     * @param req
     * @throws ActionException
     */
    private void addTicket(SMTServletRequest req) throws ActionException {
    	String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
        String ticketId = StringUtil.checkVal(req.getParameter("ticketId"));
        Boolean isInsert = (ticketId.length() == 0);
        StringBuilder sb = new StringBuilder();
        
        if (isInsert) {  
            sb.append("INSERT INTO ").append(customDb).append("VENTURE_TICKET ");
            sb.append("(VENTURE_VEHICLE_ID, SUBMITTED_BY, ACTION_REQ_FLG, ");
            sb.append("COMMENT, CREATE_DT, VENTURE_TICKET_ID) ");
            sb.append("VALUES(?, ?, ?, ?, ?, ?)");
            
            // Get a new ID
            ticketId = new UUIDGenerator().getUUID();
            req.setAttribute("ticketId", ticketId);
            
        } else {
            sb.append("UPDATE ").append(customDb).append("VENTURE_VEHICLE ");
            sb.append("SET VENTURE_VEHICLE_ID=?, SUBMITTED_BY=?, ");
            sb.append("ACTION_REQ_FLG=?, COMMENT=? UPDATE_DT=? ");
            sb.append("WHERE VENTURE_TICKET_ID=?");
            
        }
        PreparedStatement ps = null;
        try {
            ps = dbConn.prepareStatement(sb.toString());
            ps.setString(1, req.getParameter("vehicleId"));
            ps.setString(2, req.getParameter("submissionId"));
            ps.setInt(3, Convert.formatInteger(req.getParameter("action_required")));
            ps.setString(4, req.getParameter("comment"));
            ps.setTimestamp(5, Convert.getCurrentTimestamp());
            ps.setString(6, ticketId);
            
            if (ps.executeUpdate() < 1)
                throw new ActionException("Error Updating SBAction");
            
            if(isInsert) {
            	for (int i=1; req.getFile("file"+i) != null; i++) {
                	saveFile(req, "file"+i, ticketId);
            	}
            }
            
        } catch (Exception sqle) {
            log.error("Error adding ticket to " + req.getParameter("vehicleId") + ": ",sqle);
        } finally {
        	if (ps != null) {
	        	try {
	        		ps.close();
	        	} catch(Exception e) {}
        	}
        }
    }
    
    /**
     * Saves the uploaded files for the ticket
     * @param req
     * @param paramNm
     * @param ticketId
     * @return
     */
    private void saveFile(SMTServletRequest req, String paramNm, String ticketId) {
		log.debug("starting saveFile");
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);

		StringBuilder filePath =  new StringBuilder((String)getAttribute("pathToBinary"));
		filePath.append(getAttribute("orgAlias")).append(site.getOrganizationId());
		filePath.append("/").append(site.getSiteId()).append("/tickets/");

    	FileLoader fl = null;
    	FilePartDataBean fpdb = req.getFile(paramNm);
		String newFile = fpdb.getFileName();
		log.debug("newFile=" + newFile);
    	
    	// Write new file
    	if (newFile.length() > 0) {
    		try {
	    		fl = new FileLoader(attributes);
	        	fl.setFileName(fpdb.getFileName());
	        	fl.setPath(filePath.toString());
	        	fl.setRename(true);
	    		fl.setOverWrite(false);
	        	fl.setData(fpdb.getFileData());
	        	newFile = fl.writeFiles();
	        	createFiles(newFile, ticketId);
	    	} catch (Exception e) {
	    		log.error("Error Writing Ticket File", e);
	    	}
	    	log.debug("finished write");
    	}
    	
    	fpdb = null;
    	fl = null;
	}
    
    /**
     * Creates entires for every file associated with the newly created ticket.
     * @param req
     * @param ticketId
     * @throws ActionException
     */
    private void createFiles(String fileName, String ticketId) throws ActionException {
    	String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
        StringBuilder sb = new StringBuilder();
        log.debug("Creating file for:" + ticketId);
        
        sb.append("INSERT INTO ").append(customDb).append("VENTURE_TICKET_FILE ");
        sb.append("(VENTURE_TICKET_FILE_ID, VENTURE_TICKET_ID, FILE_URL, CREATE_DT) ");
        sb.append("VALUES(?, ?, ?, ?)");
       
        PreparedStatement ps = null;
        try {
    		ps = dbConn.prepareStatement(sb.toString());
                ps.setString(1, new UUIDGenerator().getUUID());
                ps.setString(2, ticketId);
                ps.setString(3, fileName);
                ps.setTimestamp(4, Convert.getCurrentTimestamp());
        	
            ps.executeUpdate();
            
        } catch (Exception sqle) {
            log.error("Error adding ticket :",sqle);
        } finally {
        	if (ps != null) {
	        	try {
	        		ps.close();
	        	} catch(Exception e) {}
        	}
        }
    }

    /**
     * Freezes the case in order to prevent people from adding tickets to it
     * @param req
     * @throws ActionException
     */
    private void freezeCase(SMTServletRequest req) throws ActionException {
    	String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
    	
    	try {
			PreparedStatement ps = dbConn.prepareStatement("UPDATE " + customDb + "VENTURE_VEHICLE SET FREEZE_FLG = ? WHERE VENTURE_VEHICLE_ID = ?");
			ps.setString(1, req.getParameter("freezeFlag"));
			ps.setString(2, req.getParameter("vehicleId"));
			
			if (ps.executeUpdate() < 1)
                throw new ActionException("Error Freezing Case");
			
		} catch (SQLException e) {
			log.error("Could not freeze case for vehicle " + req.getParameter("vehicleId"), e);
		}
    	
    }
    
    /**
     * Adds a user's profileId database along with the vehicle they wish to follow.
     * This will allow them to be notified whenever emails related to this vehicle are sent out.
     * @param req
     * @throws ActionException
     */
    private void followCase(SMTServletRequest req) throws ActionException {
    	String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
    	StringBuilder sb = new StringBuilder();
    	
    	sb.append("INSERT INTO ").append(customDb);
    	sb.append("VENTURE_NOTIFICATION (VENTURE_NOTIFICATION_ID, VENTURE_VEHICLE_ID, PROFILE_ID, CREATE_DT) ");
    	sb.append("VALUES (?,?,?,?)");
    	
    	try {
			PreparedStatement ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, new UUIDGenerator().getUUID());
			ps.setString(2, req.getParameter("vehicleId"));
			ps.setString(3, req.getParameter("submissionId"));
			ps.setTimestamp(4, Convert.getCurrentTimestamp());
			if (ps.executeUpdate() < 1)
                throw new ActionException("Error Freezing Case");
			
		} catch (SQLException e) {
			log.error("Could not freeze case for vehicle " + req.getParameter("vehicleId"), e);
		}
    }

    /**
     * Closes a ticket for a vehicle
     * @param req
     * @throws ActionException
     */
    private void closeTicket(SMTServletRequest req) throws ActionException {
    	StringBuilder sb = new StringBuilder();
    	
    	sb.append("UPDATE " + getAttribute(Constants.CUSTOM_DB_SCHEMA) + "VENTURE_TICKET ");
    	sb.append("SET ACTION_REQ_FLG = 0 ");
    	sb.append("WHERE VENTURE_TICKET_ID = ?");
    	
    	try {
			PreparedStatement ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, req.getParameter("ticketId"));
			
			ps.executeUpdate();
			
		} catch (SQLException e) {
			log.debug("Could not close ticket " + req.getParameter("ticketId"), e);
		}
    }
    
    /**
     * Change the owner of the vehicle
     * @param req
     * @throws ActionException
     */
    private void changeOwner(SMTServletRequest req) throws ActionException {
    	StringBuilder ownerSQL = new StringBuilder();
    	
    	if (!StringUtil.checkVal(req.getParameter("unitedStates")).equals(""))
    		req.setParameter("state", req.getParameter("unitedStates"));
    	else
    		req.setParameter("state", req.getParameter("canada"));
    	
    	SBProfileManager sb = new SBProfileManager(attributes);
    	
    	
    	UserDataVO user = new UserDataVO(req);
    	
    	ownerSQL.append("UPDATE " + attributes.get(Constants.CUSTOM_DB_SCHEMA) + "VENTURE_VEHICLE ");
    	ownerSQL.append("SET OWNER_ID=?, PURCHASE_DT=? ");
    	ownerSQL.append("WHERE VENTURE_VEHICLE_ID=?");
        
        try {
        	String ownerId = sb.checkProfile(user, dbConn);
        	if (ownerId == null ) {
        		sb.updateProfile(user, dbConn);
        		ownerId = user.getProfileId();
        	}
        	
			PreparedStatement ps = dbConn.prepareStatement(ownerSQL.toString());
			
	        ps.setString(1, ownerId);
	        ps.setString(2, req.getParameter("purchaseDate"));
	        ps.setString(3, req.getParameter("vehicleId"));
	        
	        ps.executeUpdate();
	        
		} catch (SQLException e) {
			log.error("Could not create new owner ", e);
		} catch (DatabaseException e) {
			log.error("Could not find or create profile ", e);
		}
    }
    
    /**
     * Called whenever any of the other build actions this class can take are completed
     * in order to record who did what and when.
     * @param req
     * @param comment
     * @throws ActionException
     */
    protected void addNotification(SMTServletRequest req, String comment) throws ActionException {
    	String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
    	StringBuilder sb = new StringBuilder();
    	
    	sb.append("INSERT INTO ").append(customDb).append(" VENTURE_ACTIVITY_TRAIL ");
    	sb.append("(VENTURE_ACTIVITY_TRAIL_ID, VENTURE_VEHICLE_ID, COMMENT, PROFILE_ID, CREATE_DT) ");
    	sb.append("VALUES (?,?,?,?,GETDATE())");
    	
    	
    	try {
			PreparedStatement ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, new UUIDGenerator().getUUID());
			ps.setString(2, req.getParameter("vehicleId"));
			ps.setString(3, comment);
			ps.setString(4, req.getParameter("submissionId"));
			
			if (ps.executeUpdate() < 1)
                throw new ActionException("Error Freezing Case");
			
		} catch (SQLException e) {
			log.error("Could not freeze case for vehicle " + req.getParameter("vehicleId"), e);
		}
    }
 
}
