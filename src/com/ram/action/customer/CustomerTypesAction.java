package com.ram.action.customer;

// JDK 7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// SMTBaseLibs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;

// WebCrescendo 2.0
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title: </b>CustomerTypesAction.java <p/>
 * <b>Project: </b>WC_Custom <p/>
 * <b>Description: </b>
 * </p>
 * <b>Copyright: </b>Copyright (c) 2014<p/>
 * <b>Company: </b>Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0<p/>
 * @since Jun 18, 2014<p/>
 *<b>Changes: </b>
 * Jun 18, 2014: David Bargerhuff: Created class.
 ****************************************************************************/
public class CustomerTypesAction extends SBActionAdapter {
	
	/**
	 * 
	 */
	public CustomerTypesAction() {
		super(new ActionInitVO());
	}

	/**
	 * @param actionInit
	 */
	public CustomerTypesAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("CustomerTypesAction retrieve...");
		String excludeTypeId = StringUtil.checkVal(req.getParameter("excludeTypeId"));
		log.debug("excludeTypeId: " + excludeTypeId);
		
		List<GenericVO> data = new ArrayList<>();
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder();
		sql.append("select a.* from ").append(schema);
		sql.append("RAM_CUSTOMER_TYPE a where 1 = 1 ");
		if (excludeTypeId.length() > 0) {
			sql.append("and customer_type_id != ? ");
		}
		
		sql.append("order by TYPE_NM");
		
		log.debug("Customer types retrieve SQL: " + sql.toString() + "|" + excludeTypeId);
		int index = 1;
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			if (excludeTypeId.length() > 0) {
				ps.setString(index++, excludeTypeId);
			}
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				data.add(new GenericVO(rs.getString("CUSTOMER_TYPE_ID"),rs.getString("TYPE_NM")));
			}
		} catch (SQLException e) {
			log.error("Error retrieving RAM customer data, ", e);
		} finally {
			if (ps != null) {
				try { 	ps.close(); }
				catch (Exception e) { log.error("Error closing PreparedStatement, ", e); }
			}
		}
		ModuleVO modVo = (ModuleVO) attributes.get(Constants.MODULE_DATA);
        modVo.setDataSize(data.size());
        modVo.setActionData(data);
        this.setAttribute(Constants.MODULE_DATA, modVo);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		
	}
}
