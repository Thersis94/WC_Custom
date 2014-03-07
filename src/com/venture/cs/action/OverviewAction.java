package com.venture.cs.action;

// JDK 1.6
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// SMTBaseLibs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.common.constants.ErrorCode;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.io.FileWriterException;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.siliconmtn.util.databean.FilePartDataBean;

// WebCrescendo 2.0
import com.smt.sitebuilder.action.FileLoader;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.dealer.DealerInfoAction;
import com.smt.sitebuilder.action.dealer.DealerLocationVO;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
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
    	log.debug("OverviewAction retrieve...");
    	String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
    	String vehicleId = req.getParameter("vehicleId");

    	ModuleVO modVo = (ModuleVO) getAttribute(Constants.MODULE_DATA);

        // Get the vehicle and owner information
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT 1 as rank, a.VENTURE_VEHICLE_ID, a.VIN, a.MAKE, a.MODEL, a.YEAR, a.PURCHASE_DT, a.FREEZE_FLG, ");
        sql.append("p.PROFILE_ID, p.FIRST_NM, p.LAST_NM, p.EMAIL_ADDRESS_TXT, pn.PHONE_NUMBER_TXT, null as DEALER_ID, ");
        sql.append("null as DEALER_LOCATION_ID, pa.ADDRESS_TXT, pa.ADDRESS2_TXT, pa.CITY_NM, pa.CITY_NM, pa.ZIP_CD, null as LOCATION_NM, pa.STATE_CD, ");
        sql.append("pa.COUNTRY_CD, null as VENTURE_TICKET_ID, null as VENTURE_TICKET_FILE_ID, null as FILE_URL, null as COMMENT, null as CREATE_DT, null as ACTION_REQ_FLG ");
        sql.append("FROM ").append(customDb).append("VENTURE_VEHICLE a ");
        sql.append("left join PROFILE p on p.PROFILE_ID = a.OWNER_PROFILE_ID ");
        sql.append("left join PROFILE_ADDRESS pa on p.PROFILE_ID = pa.PROFILE_ID ");
        sql.append("left join PHONE_NUMBER pn on p.PROFILE_ID = pn.PROFILE_ID ");
        sql.append("WHERE a.VENTURE_VEHICLE_ID = ? ");
        sql.append("union ");
        
        //Get the dealership information
        sql.append("SELECT 2 as rank, null as VENTURE_VEHICLE_ID, a.VIN, a.MAKE, a.MODEL, a.YEAR, a.PURCHASE_DT, a.FREEZE_FLG, ");
        sql.append("null as PROFILE_ID, null as FIRST_NM, null as LAST_NM, EMAIL_ADDRESS_TXT, PRIMARY_PHONE_NO as PHONE_NUMBER_TXT, ");
        sql.append("dl.DEALER_ID, dl.DEALER_LOCATION_ID, dl.ADDRESS_TXT, dl.ADDRESS2_TXT, dl.CITY_NM, dl.CITY_NM, dl.ZIP_CD, dl.LOCATION_NM, dl.STATE_CD, ");
        sql.append("dl.COUNTRY_CD, null as VENTURE_TICKET_ID, null as VENTURE_TICKET_FILE_ID, null as FILE_URL, null as COMMENT, null as CREATE_DT, null as ACTION_REQ_FLG ");
        sql.append("FROM ").append(customDb).append("VENTURE_VEHICLE a ");
        sql.append("left join DEALER_LOCATION dl on dl.DEALER_ID = a.DEALER_ID ");
        sql.append("WHERE a.VENTURE_VEHICLE_ID = ? ");
        sql.append("union ");
        
        //Get the ticket and file information
        sql.append("SELECT 3 as rank, null as VENTURE_VEHICLE_ID, null as VIN, null as MAKE, null as MODEL, null as YEAR, null as PURCHASE_DT, null as FREEZE_FLG, ");
        sql.append("null as PROFILE_ID, p.FIRST_NM, p.LAST_NM, null as EMAIL_ADDRESS_TXT, null as PHONE_NUMBER_TXT, null as DEALER_ID, null as DEALER_LOCATION_ID, ");
        sql.append("null as ADDRESS_TXT, null as ADDRESS2_TXT, null as CITY_NM, null as CITY_NM, null as ZIP_CD, null as LOCATION_NM, null as STATE_CD,  ");
        sql.append("null as COUNTRY_CD, vt.VENTURE_TICKET_ID, vtf.VENTURE_TICKET_FILE_ID, vtf.FILE_URL, cast(vt.COMMENT as nvarchar(1000)), vt.CREATE_DT, vt.ACTION_REQ_FLG ");
        sql.append("FROM ").append(customDb).append("VENTURE_TICKET vt ");
        sql.append("left join ").append(customDb).append("VENTURE_VEHICLE a on a.VENTURE_VEHICLE_ID = vt.VENTURE_VEHICLE_ID ");
        sql.append("left join ").append(customDb).append("VENTURE_TICKET_FILE vtf on vtf.VENTURE_TICKET_ID = vt.VENTURE_TICKET_ID ");
        sql.append("left join PROFILE p on p.PROFILE_ID = vt.PROFILE_ID ");
        sql.append("WHERE a.VENTURE_VEHICLE_ID = ? ");
        sql.append("union ");
        
        //Get the activity trail for this vehicle
        sql.append("SELECT 4 as rank, null as VENTURE_VEHICLE_ID, null as VIN, null as MAKE, null as MODEL, null as YEAR, null as PURCHASE_DT, null as FREEZE_FLG, ");
        sql.append("null as PROFILE_ID, p.FIRST_NM, p.LAST_NM, null as EMAIL_ADDRESS_TXT, null as PHONE_NUMBER_TXT, null as DEALER_ID, null as DEALER_LOCATION_ID, ");
        sql.append("null as ADDRESS_TXT, null as ADDRESS2_TXT, null as CITY_NM, null as CITY_NM, null as ZIP_CD, null as LOCATION_NM, null as STATE_CD, ");
        sql.append("null as COUNTRY_CD, null as VENTURE_TICKET_ID, null as VENTURE_TICKET_FILE_ID, null as FILE_URL, cast(vat.COMMENT as nvarchar(1000)) as COMMENT, vat.CREATE_DT, null as ACTION_REQ_FLG ");
        sql.append("FROM ").append(customDb).append("VENTURE_ACTIVITY_TRAIL vat ");
        sql.append("left join PROFILE p on p.PROFILE_ID = vat.PROFILE_ID ");
        sql.append("WHERE vat.VENTURE_VEHICLE_ID = ? ");
        sql.append("ORDER BY RANK, CREATE_DT ASC ");
        
        log.info("OverviewAction retrieve SQL: " + sql + "|" + vehicleId);
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
	        			vo.setOwner(parseProfile(rs.getString("profile_id")));
	        			log.debug("user zip code: " + rs.getString("zip_cd"));
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
		        				ticket.addFile(new TicketFileVO(rs));
	        				}
	        				vo.addTicket(ticket);
	        			} else if (StringUtil.checkVal(rs.getString("FILE_URL")).length() > 0 && vo.getTickets().size() > 0) {
	        				vo.getTickets().get(vo.getTickets().size()-1).addFile(new TicketFileVO(rs));
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
	        	} catch (Exception e) {log.error("Error closing PreparedStatement, ", e);}
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
    	log.debug("OverviewAction build..., reqType: " + reqType);
    	String activityMsg = null;
    	
    	if (reqType.equals("manageTicket")) {
    		manageTicket(req);
    		activityMsg = "Added ticket";
    		
    	} else if (reqType.equals("freeze")) {
    		freezeCase(req);
    		if ("1".equals(req.getParameter("freezeFlag"))) {
    			activityMsg = "Froze case";
    		} else {
    			activityMsg = "Unfroze case";
    		}
    		
    	} else if (reqType.equals("follow")) {
    		followCase(req);
    		activityMsg = "Followed case";
    		
    	} else if (reqType.equals("updateComments")) {
    		updateTicketComments(req);
    		activityMsg = "Updated ticket comments";
    		
    	} else if (reqType.equals("deleteFile")) {
    		deleteFile(req);
    		activityMsg = "Deleted file";
    		
    	} else if (reqType.equals("closeTicket")) {
    		closeTicket(req);
    		activityMsg = "Closed ticket";
    		
    	} else if (reqType.equals("editOwner")) {
    		editOwner(req);
    		activityMsg = "Updated owner";
    		
    	} else if (reqType.equals("changeOwner")) {
    		changeOwner(req);
    		activityMsg = "Changed owner";
    		
    	} else if (reqType.equals("editDealer")) {
    		editDealer(req);
    		activityMsg = "Updated dealer";
    		
    	} else if (reqType.equals("changeDealer")) {
    		changeDealer(req);
    		activityMsg = "Changed dealer";
    		
    	} 
    	
    	// log the msg
    	StringBuilder url = new StringBuilder();
    	url.append("result?vehicleId=").append(req.getParameter("vehicleId"));
    	
    	if (activityMsg != null) {
    		logActivity(req, activityMsg);
    	} else {
    		url.append("&msg=We were unable to process your update.  Please contact your system administrator.");
    	}
    	log.debug("redirect url: " + url.toString());    	
        req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
    	req.setAttribute(Constants.REDIRECT_URL, url.toString());
    	    
    }
    
    /**
     * Adds a ticket to the current vehicle.
     * @param req
     * @throws ActionException
     */
    private void manageTicket(SMTServletRequest req) throws ActionException {
    	log.debug("OverviewAction manageTicket...");
    	String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
        String ticketId = StringUtil.checkVal(req.getParameter("ticketId"));
        Boolean isInsert = (ticketId.length() == 0);
        StringBuilder sb = new StringBuilder();
        
        if (isInsert) {  
            sb.append("INSERT INTO ").append(customDb).append("VENTURE_TICKET ");
            sb.append("(VENTURE_VEHICLE_ID, PROFILE_ID, ACTION_REQ_FLG, ");
            sb.append("COMMENT, CREATE_DT, VENTURE_TICKET_ID) ");
            sb.append("VALUES(?, ?, ?, ?, ?, ?)");
            
            // Get a new ID
            ticketId = new UUIDGenerator().getUUID();
            req.setAttribute("ticketId", ticketId);
            
        } else {
            sb.append("UPDATE ").append(customDb).append("VENTURE_VEHICLE ");
            sb.append("SET VENTURE_VEHICLE_ID=?, PROFILE_ID=?, ");
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
	        	} catch (Exception e) {log.error("Error closing PreparedStatement, ", e);}
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
		log.debug("OverviewAction saveFile...");
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
	        	manageFileRecord(ticketId, null, newFile, true);
	    	} catch (Exception e) {
	    		log.error("Error Writing Ticket File", e);
	    	}
	    	log.debug("finished write");
    	}
    	
    	fpdb = null;
    	fl = null;
	}
    
    /**
     * Deletes the file specified by the paramNm value passed to the method.
     * @param req
     * @param paramNm
     * @param ticketId
     * @throws ActionException
     */
    private void deleteFile(SMTServletRequest req) throws ActionException {
		log.debug("OverviewAction deleteFile...");
        String ticketId = StringUtil.checkVal(req.getParameter("ticketId"));
		String ticketFileId = StringUtil.checkVal(req.getParameter("ticketFileId"));
		String fileUrl = StringUtil.checkVal(req.getParameter("fileUrl"));
				
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		
		StringBuilder filePath =  new StringBuilder((String)getAttribute("pathToBinary"));
		filePath.append(getAttribute("orgAlias")).append(site.getOrganizationId());
		filePath.append("/").append(site.getSiteId()).append("/tickets/");
		
		if (ticketId.length() > 0 && ticketFileId.length() > 0) {
    		FileLoader fl = new FileLoader(attributes);
			try {
				manageFileRecord(ticketId, ticketFileId, fileUrl, false);
				fl.deleteFile(filePath.toString(), fileUrl);
			} catch (FileWriterException e) {
				log.error("Error deleting file, ", e);
				throw new ActionException(e.getMessage());
			}
		}
				
    }
    
    /**
     * Updates the comments field for the specified ticket.
     * @param req
     * @throws ActionException
     */
    private void updateTicketComments(SMTServletRequest req) throws ActionException {
    	log.debug("updating ticket comments...");
    	String ticketId = StringUtil.checkVal(req.getParameter("ticketId"));
    	String ticketComments = StringUtil.checkVal(req.getParameter("ticketComments"));
    	log.debug("ticketComments: " + ticketComments);
    	String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
        StringBuilder sb = new StringBuilder();
        sb.append("update ").append(customDb).append(" VENTURE_TICKET ");
        sb.append("set COMMENT = ? where VENTURE_TICKET_ID = ?");
        log.debug("update tickets SQL: " + sb.toString());
        PreparedStatement ps = null;
        try {
        	ps = dbConn.prepareStatement(sb.toString());
        	ps.setString(1, ticketComments);
        	ps.setString(2, ticketId);
        	ps.execute();
        } catch (SQLException sqle) {
        	log.error("Error editing ticket comments, ", sqle);
        	throw new ActionException("Error updating ticket comments.");
        } finally {
			if (ps != null) {
	        	try {
	        		ps.close();
	        	} catch (Exception e) {log.error("Error closing PreparedStatement, ", e);}
			}
        }
    }
    
    /**
     * Creates entires for every file associated with the newly created ticket.
     * @param req
     * @param ticketId
     * @throws ActionException
     */
    private void manageFileRecord(String ticketId, String ticketFileId, String fileUrl, boolean insert) 
    		throws ActionException {
    	log.debug("managing file record...");
    	String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
        StringBuilder sb = new StringBuilder();
        log.debug("Creating file for:" + ticketId);
        
        String msg = null;
        if (insert) {
	        sb.append("INSERT INTO ").append(customDb).append("VENTURE_TICKET_FILE ");
	        sb.append("(VENTURE_TICKET_FILE_ID, VENTURE_TICKET_ID, FILE_URL, CREATE_DT) ");
	        sb.append("VALUES(?, ?, ?, ?)");
	        msg = "inserting";
        } else {
        	sb.append("delete from ").append(customDb).append("VENTURE_TICKET_FILE ");
	        sb.append("where VENTURE_TICKET_FILE_ID = ?");
	        msg = "deleting";
        }
       log.debug("file record SQL: " + sb.toString());
       log.debug("ticketId|ticketFileId|fileUrl|insert: " + ticketId + "|" + ticketFileId + "|" + fileUrl + "|" + insert);
        PreparedStatement ps = null;
        try {
    		ps = dbConn.prepareStatement(sb.toString());
    		if (insert) {
                ps.setString(1, new UUIDGenerator().getUUID());
                ps.setString(2, ticketId);
                ps.setString(3, fileUrl);
                ps.setTimestamp(4, Convert.getCurrentTimestamp());
    		} else {
    			ps.setString(1, ticketFileId);
    		}
        	
            ps.executeUpdate();
            
        } catch (SQLException sqle) {
        	StringBuilder err = new StringBuilder("Error ").append(msg).append(" file for this ticket.");
            log.error("Error managing ticket :",sqle);
            throw new ActionException(err.toString());
        } finally {
			if (ps != null) {
	        	try {
	        		ps.close();
	        	} catch (Exception e) {log.error("Error closing PreparedStatement, ", e);}
			}
        }
    }

    /**
     * Freezes the case in order to prevent people from adding tickets to it
     * @param req
     * @throws ActionException
     */
    private void freezeCase(SMTServletRequest req) throws ActionException {
    	log.debug("freezing case...");
    	String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
    	String vehicleId = StringUtil.checkVal(req.getParameter("vehicleId"));
    	StringBuilder sb = new StringBuilder();
    	sb.append("UPDATE ").append(customDb).append("VENTURE_VEHICLE ");
    	sb.append("SET FREEZE_FLG = ? WHERE VENTURE_VEHICLE_ID = ?");
    	
    	try {
			PreparedStatement ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, req.getParameter("freezeFlag"));
			ps.setString(2, vehicleId);
			
			if (ps.executeUpdate() < 1)
                throw new SQLException("Error freezing this case.");
			
		} catch (SQLException e) {
			log.error("Error freezing this case, " + vehicleId, e);
            throw new ActionException(e.getMessage());
		}
    	
    }
    
    /**
     * Adds a user's profileId database along with the vehicle they wish to follow.
     * This will allow them to be notified whenever emails related to this vehicle are sent out.
     * @param req
     * @throws ActionException
     */
    private void followCase(SMTServletRequest req) throws ActionException {
    	log.debug("follow case...");
    	// check to see if this user is already following this vehicle.
    	String profileId = checkFollowers(req);
    	if (profileId == null) {
    		// user is not a follower of this vehicle, so add them
	    	String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
	    	StringBuilder sb = new StringBuilder();
	    	sb.append("INSERT INTO ").append(customDb);
	    	sb.append("VENTURE_NOTIFICATION (VENTURE_NOTIFICATION_ID, VENTURE_VEHICLE_ID, PROFILE_ID, CREATE_DT) ");
	    	sb.append("VALUES (?,?,?,?)");
	    	PreparedStatement ps = null;
	    	try {
				ps = dbConn.prepareStatement(sb.toString());
				ps.setString(1, new UUIDGenerator().getUUID());
				ps.setString(2, req.getParameter("vehicleId"));
				ps.setString(3, req.getParameter("submissionId"));
				ps.setTimestamp(4, Convert.getCurrentTimestamp());
				if (ps.executeUpdate() < 1)
	                throw new ActionException("Error adding user to list followers of this case.");
				
			} catch (SQLException e) {
				log.error("Error adding user to list of followers of this case, " + req.getParameter("vehicleId"), e);
			} finally {
				if (ps != null) {
					try {
						ps.close();
					} catch (Exception e) {log.error("Error closing PreparedStatement, ", e);}
				}
			}
    	}
    }
    
    /**
     * Queries the notification table to see if user is already a follower
     * of the vehicle ID being requested.
     * @param req
     * @return
     */
    private String checkFollowers(SMTServletRequest req) {
    	String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
    	String vehicleId = StringUtil.checkVal(req.getParameter("vehicleId"));
    	String profileId = StringUtil.checkVal(req.getParameter("submissionId"));
    	
    	String tmpProfileId = null;
    	StringBuilder sb = new StringBuilder();
    	sb.append("select PROFILE_ID from ").append(customDb).append("VENTURE_NOTIFICATION ");
    	sb.append("where PROFILE_ID = ? and VENTURE_VEHICLE_ID = ?");

    	PreparedStatement ps = null;
    	try {
    		ps = dbConn.prepareStatement(sb.toString());
    		ps.setString(1, profileId);
    		ps.setString(2,  vehicleId);
    		ResultSet rs = ps.executeQuery();
    		
    		if (rs.next()) {
    			tmpProfileId = rs.getString(1);
    		}
    	} catch (SQLException sqle) {
    		log.error("Error retrieving");
    	} finally {
    		if (ps != null) {
    			try {
    				ps.close();
    			} catch (Exception e) {log.error("Error closing PreparedStatement, e");}
    		}
    	}
    	
    	return tmpProfileId;
    	
    }

    /**
     * Closes a ticket for a vehicle
     * @param req
     * @throws ActionException
     */
    private void closeTicket(SMTServletRequest req) throws ActionException {
    	log.debug("closing ticket...");
    	StringBuilder sb = new StringBuilder();
    	
    	sb.append("UPDATE " + getAttribute(Constants.CUSTOM_DB_SCHEMA) + "VENTURE_TICKET ");
    	sb.append("SET ACTION_REQ_FLG = 0 ");
    	sb.append("WHERE VENTURE_TICKET_ID = ?");
    	
    	PreparedStatement ps = null;
    	try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, req.getParameter("ticketId"));
			
			ps.executeUpdate();
			
		} catch (SQLException e) {
			log.debug("Could not close ticket " + req.getParameter("ticketId"), e);
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {log.error("Error closing PreparedStatement, ", e);}
			}
		}
    }
    
    /**
     * Edit the owner of the vehicle
     * @param req
     * @throws ActionException
     */
    private void editOwner(SMTServletRequest req) throws ActionException {
    	log.debug("edit owner...");
    	StringBuilder ownerSQL = new StringBuilder();
    	ownerSQL.append("UPDATE " + attributes.get(Constants.CUSTOM_DB_SCHEMA) + "VENTURE_VEHICLE ");
    	ownerSQL.append("SET OWNER_PROFILE_ID=? ");
    	if (StringUtil.checkVal(req.getParameter("purchaseDate")).length() > 0) {
    		ownerSQL.append(", PURCHASE_DT=? ");
    	}
    	ownerSQL.append("WHERE VENTURE_VEHICLE_ID=?");

    	UserDataVO user = new UserDataVO(req);
    	SBProfileManager sb = new SBProfileManager(attributes);
    	PreparedStatement ps = null;
        try {
        	String ownerId = sb.checkProfile(user, dbConn);
        	if (ownerId == null ) {
        		sb.updateProfile(user, dbConn);
        		ownerId = user.getProfileId();
        	}
        	
			ps = dbConn.prepareStatement(ownerSQL.toString());
	        ps.setString(1, ownerId);
	        ps.setString(2, req.getParameter("purchaseDate"));
	        ps.setString(3, req.getParameter("vehicleId"));
	        ps.executeUpdate();
	        
		} catch (SQLException e) {
			log.error("Could not create new owner ", e);
			throw new ActionException("Could not create new owner.");
		} catch (DatabaseException e) {
			log.error("Could not find or create profile ", e);
			throw new ActionException("Could not find or create owner's profile.");
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {log.error("Error closing PreparedStatement, ", e);}
			}
		}
    }
    
    
    /**
     * Edit the existing vehicle owner's profile information.
     * @param req
     * @throws ActionException
     */
    private void changeOwner(SMTServletRequest req) throws ActionException {
    	log.debug("change owner...");
    	// TODO 
    	
    	// check for existing profile
    	// if profile found, use that profile
    	// else create profile.
    	// set 'new' owner profile as vehicle's owner id.
    }
    
    /**
     * Edit's the dealer's information
     * @param req
     * @throws ActionException
     */
    private void editDealer(SMTServletRequest req) throws ActionException {
    	log.debug("editing dealer location...");
    	DealerInfoAction sai = new DealerInfoAction(actionInit);
    	sai.setAttributes(attributes);
    	sai.setDBConnection(dbConn);
    	
    	try {
    		sai.updateDealerLocation(req, true);
    	} catch (DatabaseException dbe) {
    		log.error("Error updating dealer location, ", dbe);
    		throw new ActionException(dbe.getMessage());
    	}
    }
    
    /**
     * Reassigns the dealer on the request to the vehicle on the request.
     * @param req
     * @throws ActionException
     */
    private void changeDealer(SMTServletRequest req) throws ActionException {
    	log.debug("change dealer...");
    	String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
    	String dealerId = StringUtil.checkVal(req.getParameter("dealerId"));
    	String vehicleId = StringUtil.checkVal(req.getParameter("vehicleId"));

        StringBuilder sb = new StringBuilder();
        sb.append("update ").append(customDb).append(" VENTURE_VEHICLE ");
        sb.append("set DEALER_ID = ? where VENTURE_VEHICLE_ID = ?");
        
        PreparedStatement ps = null;
        try {
        	ps = dbConn.prepareStatement(sb.toString());
        	ps.setString(1, dealerId);
        	ps.setString(2, vehicleId);
        	ps.executeUpdate();
        } catch (SQLException sqle) {
        	log.error("Error updating vehicle dealer association, ", sqle);
        	throw new ActionException("Error changing vehicle's dealer.");
        } finally {
        	try {
        		ps.close();
        	} catch (Exception e) {}
        }
	
    }
    
    /**
     * Called whenever any of the other build actions this class can take are completed
     * in order to record who did what and when.
     * @param req
     * @param comment
     * @throws ActionException
     */
    protected void logActivity(SMTServletRequest req, String comment) throws ActionException {
    	log.debug("logging activity...");
    	String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
    	StringBuilder sb = new StringBuilder();
    	
    	sb.append("INSERT INTO ").append(customDb).append(" VENTURE_ACTIVITY_TRAIL ");
    	sb.append("(VENTURE_ACTIVITY_TRAIL_ID, VENTURE_VEHICLE_ID, COMMENT, PROFILE_ID, CREATE_DT) ");
    	sb.append("VALUES (?,?,?,?,GETDATE())");
    	
    	PreparedStatement ps = null;
    	try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, new UUIDGenerator().getUUID());
			ps.setString(2, req.getParameter("vehicleId"));
			ps.setString(3, comment);
			ps.setString(4, req.getParameter("submissionId"));
			
			if (ps.executeUpdate() < 1)
                throw new ActionException("Error logging activity.");
			
		} catch (SQLException e) {
			log.error("Error logging activity for vehicle " + req.getParameter("vehicleId"), e);
		} finally {
        	try {
        		ps.close();
        	} catch (Exception e) {}
        }
    }
    
    /**
     * Performs a profile lookup via ProfileManager and the supplied profile ID.
     * @param profileId
     * @return Populated UserDataVOe retrieved by ProfileManager or else an empty UserDataVO
     */
    private UserDataVO parseProfile(String profileId) {
    	UserDataVO user = null;
    	if (profileId.length() > 0) {
    		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
    		try {
    			user = pm.getProfile(profileId, dbConn, ProfileManager.PROFILE_ID_LOOKUP);
    		} catch (DatabaseException de) {
    			log.error("Error retrieving vehicle owner's profile, ", de);
    		}
    	} else {
    		user = new UserDataVO();
    	}
    	
    	return user;
    }
 
}
