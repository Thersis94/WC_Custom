package com.ram.action.workflow;

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

// WebCrescendo 2.0
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title: </b>ServicesAction.java <p/>
 * <b>Project: </b>WC_Custom <p/>
 * <b>Description: </b>
 * </p>
 * <b>Copyright: </b>Copyright (c) 2015<p/>
 * <b>Company: </b>Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0<p/>
 * @since Apr 28, 2015<p/>
 *<b>Changes: </b>
 * Apr 28, 2015: Tim Johnson: Created class.
 ****************************************************************************/
public class ServicesAction extends SBActionAdapter {
	
	/**
	 * 
	 */
	public ServicesAction() {
		super(new ActionInitVO());
	}

	/**
	 * @param actionInit
	 */
	public ServicesAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("ServicesAction retrieve...");
		
		List<GenericVO> data = new ArrayList<>();
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder();
		sql.append("select SERVICE_CD, SERVICE_NM from ").append(schema);
		sql.append("RAM_SERVICE ");
		sql.append("order by SERVICE_NM");
		
		log.debug("Services retrieve SQL: " + sql.toString());
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				data.add(new GenericVO(rs.getString("SERVICE_CD"),rs.getString("SERVICE_NM")));
			}
		} catch (SQLException e) {
			log.error("Error retrieving RAM service data, ", e);
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
