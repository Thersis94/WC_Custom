package com.ram.action;

// JDK 7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// RAMDataFeed libs
import com.ram.datafeed.data.RegionVO;

// SMTBaseLibs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;

// WebCrescendo 2.0
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title: </b>RegionAction.java <p/>
 * <b>Project: </b>WC_Custom <p/>
 * <b>Description: </b>
 * </p>
 * <b>Copyright: </b>Copyright (c) 2014<p/>
 * <b>Company: </b>Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0<p/>
 * @since Jun 19, 2014<p/>
 *<b>Changes: </b>
 * Jun 19, 2014: David Bargerhuff: Created class.
 ****************************************************************************/
public class RegionAction extends SBActionAdapter {
	
	/**
	 * 
	 */
	public RegionAction() {
		super(new ActionInitVO());
	}

	/**
	 * @param actionInit
	 */
	public RegionAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("RegionAction retrieve...");
		String regionId = StringUtil.checkVal(req.getParameter("regionId"));
		List<RegionVO> data = new ArrayList<>();
		String schema = (String)getAttribute("customDbSchema");
		StringBuilder sql = new StringBuilder();
		sql.append("select a.* from ").append(schema);
		sql.append("RAM_REGION a where 1 = 1 ");
		if (regionId.length() > 0) {
			sql.append("and REGION_ID = ? ");
		}
		
		sql.append("order by REGION_NM");
		
		log.debug("RegionAction retrieve SQL: " + sql.toString() + "|" + regionId);
		int index = 1;
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			if (regionId.length() > 0) {
				ps.setString(index++, regionId);
			}
			ResultSet rs = ps.executeQuery();
			RegionVO region = null;
			while (rs.next()) {
				region = new RegionVO();
				region.setRamRegionId(rs.getString("REGION_ID"));
				region.setRegionName(rs.getString("REGION_NM"));
				data.add(region);
			}
		} catch (SQLException e) {
			log.error("Error retrieving RAM region data, ", e);
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
