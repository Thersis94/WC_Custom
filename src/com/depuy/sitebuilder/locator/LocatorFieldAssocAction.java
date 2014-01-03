package com.depuy.sitebuilder.locator;

// JDK 1.5.0
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

// SMT Base Libs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.UUIDGenerator;

// SB Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.admin.action.SBModuleAction;
import com.smt.sitebuilder.common.SiteBuilderUtil;
import com.smt.sitebuilder.common.constants.AdminConstants;

/*****************************************************************************
 <p><b>Title</b>: LocatorFieldAssocAction.java</p>
 <p>Action that manages the fields associates to a locator instance</p>
 <p>Copyright: Copyright (c) 2000 - 2005 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James Camire
 @version 2.0
 @since Jun 19, 2006
 Code Updates
 James Camire, Jun 19, 2006 - Creating Initial Class File
 ***************************************************************************/

public class LocatorFieldAssocAction extends SBActionAdapter {
    private SiteBuilderUtil util = null;
    public static final String LOCATOR_FIELD_LIST = "locatorFieldList";
    public static final String LOCATOR_FIELD_RETRV = "locatorFieldRetrieve";
    
    /**
     * 
     */
    public LocatorFieldAssocAction() {
        super();
        util = new SiteBuilderUtil();
    }

    /**
     * @param arg0
     */
    public LocatorFieldAssocAction(ActionInitVO arg0) {
        super(arg0);
        util = new SiteBuilderUtil();
    }

    /* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#delete(com.siliconmtn.http.SMTServletRequest)
     */
    @Override
    public void delete(SMTServletRequest req) throws ActionException {
        StringBuffer sb = new StringBuffer();
        sb.append("delete from locator_field_assoc where action_id = ?");
        String actionId = (String) req.getAttribute(SBModuleAction.SB_ACTION_ID);
        
        log.info("Delete rss assoc sql: " + sb + " - " + actionId);
        PreparedStatement ps = null;
        try {
            ps = dbConn.prepareStatement(sb.toString());
            ps.setString(1, actionId);
            
            ps.executeUpdate();
        } catch (SQLException sqle) {
            log.error("Error deleting locaotr assoc field: " + sqle.getMessage());
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch(Exception e) {}
            }
        }
    }

    /* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#update(com.siliconmtn.http.SMTServletRequest)
     */
    @Override
    public void update(SMTServletRequest req) throws ActionException {
        Object msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
        
        // Delete the current entries
        this.delete(req);
        
        // Loop the checked boxes and add to the assoc
        StringBuffer sb = new StringBuffer();
        sb.append("insert into locator_field_assoc (locator_field_assoc_id, ");
        sb.append("locator_field_id, action_id, create_dt) ");
        sb.append("values (?,?,?,?)");
        
        PreparedStatement ps = null;
        String[] lfid = req.getParameterValues("locatorFieldId");
        if (lfid == null) lfid = new String[0];
        
        try {
            dbConn.setAutoCommit(false);
            ps = dbConn.prepareStatement(sb.toString());
            
            for (int i=0; i < lfid.length; i++) {
                ps.setString(1, new UUIDGenerator().getUUID());
                ps.setString(2, lfid[i]);
                ps.setString(3, (String) req.getAttribute(SBModuleAction.SB_ACTION_ID));
                ps.setDate(4, new java.sql.Date(new Date().getTime()));
                
                ps.addBatch();
            }
            
            // If there were entries add, commit the batch
            if (lfid.length > 0) {
                ps.executeBatch();
                dbConn.commit();
            }
        } catch (SQLException sqle) {
            try {
                dbConn.rollback();
            } catch(Exception e) {}
            
            log.error("Error updating Loc Field Assoc: " + sqle.getMessage());
            msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch(Exception e) {}
            }
        }
        
        util.adminRedirect(req, msg, (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
    }


    /* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
     */
    @Override
    public void retrieve(SMTServletRequest req) throws ActionException {
        log.info("Listing fields for locator");
        
        StringBuffer sql = new StringBuffer();
        sql.append("select b.locator_field_id, b.field_nm ");
        sql.append("from locator_field_assoc a inner join locator_field b ");
        sql.append("on a.locator_field_id = b.locator_field_id ");
        sql.append("where action_id = ? ");
        
        log.info("LocatorField Action Retrieve SQL: " + sql.toString());
        PreparedStatement ps = null;
        Map<String, Boolean> data = new HashMap<String, Boolean>();
        try {
            ps = dbConn.prepareStatement(sql.toString());
            ps.setString(1, actionInit.getActionId());
            
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                data.put(rs.getString(1), new Boolean(true));
            }
        } catch (SQLException sqle) {
            
            throw new ActionException("Error Gettting Content Action: " + sqle.getMessage());
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch(Exception e) {}
            }
        }
        
        // Add the collection to the request object
        req.setAttribute(LOCATOR_FIELD_RETRV, data);
    }

    /* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#list(com.siliconmtn.http.SMTServletRequest)
     */
    @Override
    public void list(SMTServletRequest req) throws ActionException {
        log.info("Listing fields for locator");
        String sbActionId = req.getParameter(SBModuleAction.SB_ACTION_ID);
        
        StringBuffer sql = new StringBuffer();
        sql.append("select a.locator_field_id, locator_field_assoc_id ");
        sql.append("from locator_field a right outer join locator_field_assoc b ");
        sql.append("on a.locator_field_id = b.locator_field_id ");
        sql.append("where b.action_id = ? ");
        sql.append("union ");
        sql.append("select locator_field_id, null from locator_field ");
        sql.append("where locator_field_id not in ");
        sql.append("(select locator_field_id from locator_field_assoc ");
        sql.append("where action_id = ?) ");
        sql.append("order by a.locator_field_id");
        
        log.info("LocatorField Action List SQL: " + sql.toString());
        LocatorFieldVO vo = null;
        PreparedStatement ps = null;
        Map<String, Boolean> data = new HashMap<String, Boolean>();
        try {
            ps = dbConn.prepareStatement(sql.toString());
            ps.setString(1, sbActionId);
            ps.setString(2, sbActionId);
            
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                vo = new LocatorFieldVO();
                vo.setActionId(rs.getString(1));
                
                // If there is an Assoc Id, set selected true
                String lfai = rs.getString(2);
                if (lfai != null && lfai.length() > 0) vo.setSelected(true);
                else vo.setSelected(false);
                
                data.put(vo.getActionId(), new Boolean(vo.isSelected()));
            }
        } catch (SQLException sqle) {
            
            throw new ActionException("Error getting LocatorFieldAssocAction: " + sqle.getMessage());
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch(Exception e) {}
            }
        }
        
        // Add the collection to the request object
        req.setAttribute(LOCATOR_FIELD_LIST, data);
    }

}
