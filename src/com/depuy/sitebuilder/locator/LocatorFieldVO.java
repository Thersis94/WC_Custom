package com.depuy.sitebuilder.locator;

// JDK 1.5.0
import java.sql.ResultSet;

// SMT base Libs 2.0
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;

// SB Imports
import com.smt.sitebuilder.action.AbstractSiteBuilderVO;

/*****************************************************************************
 <p><b>Title</b>: LocatorFieldVO.java</p>
 <p>Manages the locator fields for the search</p>
 <p>Copyright: Copyright (c) 2000 - 2005 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James Camire
 @version 2.0
 @since Jun 19, 2006
 Code Updates
 James Camire, Jun 19, 2006 - Creating Initial Class File
 ***************************************************************************/

public class LocatorFieldVO extends AbstractSiteBuilderVO {
    private static final long serialVersionUID = 1l;
    private String fieldName = null;
    private boolean selected = false;
    
    public LocatorFieldVO() {
        super();
    }
    
    /**
     * Sets the data from the request object to the param value
     * @param req
     */
    public void setData(SMTServletRequest req) {
        actionId = req.getParameter("locatorFieldId");
        fieldName = req.getParameter("fieldName");
    }
    
    /**
     * Sets the data from the result set to the param value
     * @param rs
     */
    public void setData(ResultSet rs) {
        DBUtil db = new DBUtil();
        actionId = db.getStringVal("locator_field_id", rs);
        fieldName = db.getStringVal("field_nm", rs);
    }

    /**
     * @return Returns the fieldName.
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * @return Returns the selected.
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * @param fieldName The fieldName to set.
     */
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    /**
     * @param selected The selected to set.
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

}
