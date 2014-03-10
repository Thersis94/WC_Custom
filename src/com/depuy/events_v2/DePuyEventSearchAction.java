package com.depuy.events_v2;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.event.EventFacadeAction;
import com.smt.sitebuilder.action.event.EventTypeAction;
import com.smt.sitebuilder.action.event.vo.EventEntryVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: DePuyEventSearchAction.java<p/>
 * <b>Description: handles Seminar search for public facing websites; KR, HR, and Shoulder. 
 * Builds upon the stock Events portlet by adding-in joint to the query. </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Mar 4, 2014
 ****************************************************************************/
public class DePuyEventSearchAction extends SimpleActionAdapter {

	public DePuyEventSearchAction() {
		super();
	}

	public DePuyEventSearchAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {		
		//test to ensure we're not asking for the search form; only load data if we have the params to do so!
		if (! req.hasParameter("specialty") && ! req.hasParameter("eventEntryId") && 
				! req.hasParameter("rsc") && ! req.hasParameter("locatorSubmit")) return;
		
		//transpose locator products to a specialty
		if (req.hasParameter("locatorSubmit")) {
			String specialtyId = req.getParameter("specialty");
			if (req.hasParameter("product")) specialtyId = getSpecialty(req.getParameter("product")); 
			req.setParameter("specialtyId", specialtyId);
		}
		
		//query the system for qualifying Seminars to display
		loadData(req);
	}
	
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		actionInit.setActionId((String) mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		mod.setActionId(actionInit.getActionId());
		setAttribute(Constants.MODULE_DATA, mod);
		
		SMTActionInterface ae = new EventFacadeAction(actionInit);
		ae.setAttributes(attributes);
		ae.setDBConnection(dbConn);
		ae.build(req);
		
		//add specialty back onto the redirect URL
		if (req.hasParameter("specialty")) {
			String url = (String) req.getAttribute(Constants.REDIRECT_URL);
			url += "&specialty=" + req.getParameter("specialty");
			req.setAttribute(Constants.REDIRECT_URL, url);
		}
	}
	
	
	@Override
	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	
	/**
	 * load a list of APPROVED seminars who's specialty is the passed AAMD Specialty (joint)
	 * that are also within driving distance of the requestee, and are not CPSEMs.
	 * @param req
	 */
	private void loadData(SMTServletRequest req) {
		EventTypeAction eta = new EventTypeAction();
		eta.setAttributes(attributes);
		
		String distSql = eta.buildSpatialClause(req); 
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		Integer specialtyId = Convert.formatInteger(req.getParameter("specialty"));
		if (req.hasParameter("specialtyId")) specialtyId = Convert.formatInteger(req.getParameter("specialtyId"));
		
		StringBuilder sql = new StringBuilder();
		sql.append("select ee.*, et.*, eg.header_txt, ").append(distSql).append(" as distance, des.surgeon_nm as contact_nm ");
		sql.append("from event_entry ee ");
		sql.append("inner join event_type et on ee.event_type_id=et.event_type_id ");
		sql.append("inner join event_group eg on et.action_id=eg.action_id ");
		sql.append("inner join sb_action sb on eg.action_id=sb.action_group_id and sb.action_group_id=? ");
		sql.append("inner join event_postcard_assoc epa on ee.event_entry_id=epa.event_entry_id ");
		sql.append("inner join event_postcard ep on epa.event_postcard_id=ep.event_postcard_id ");
		sql.append("inner join ").append(customDb).append("depuy_event_specialty_xr sxr on ep.event_postcard_id=sxr.event_postcard_id and sxr.joint_id=? ");
		sql.append("inner join ").append(customDb).append("depuy_event_surgeon des on ep.event_postcard_id=des.event_postcard_id ");
		sql.append("where et.type_nm != 'CPSEM' and ep.status_flg=? ");
		if (req.hasParameter("eventEntryId")) {
			sql.append("and ee.event_entry_id=? ");
		} else if (req.hasParameter("rsc")) {
			sql.append("and ee.rsvp_code_txt=? ");
		} else {
			sql.append("and ee.start_dt >= ? and ").append(distSql).append(" <= ? ");
		}
		sql.append("order by distance");
		log.debug(sql + (String) mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		
		List<EventEntryVO> data = new ArrayList<EventEntryVO>();
		PreparedStatement ps = null;
		int x = 1;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(x++, (String) mod.getAttribute(ModuleVO.ATTRIBUTE_1));
			ps.setInt(x++, specialtyId);
			ps.setInt(x++, EventFacadeAction.STATUS_APPROVED);
			if (req.hasParameter("eventEntryId")) {
				ps.setString(x++, req.getParameter("eventEntryId"));
			} else if (req.hasParameter("rsc")) {
				ps.setString(x++, req.getParameter("rsc"));
			} else {
				ps.setDate(x++, Convert.formatSQLDate(Calendar.getInstance().getTime()));
				ps.setInt(x++, Convert.formatInteger(req.getParameter("radius"), 50));
			}
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				EventEntryVO vo = new EventEntryVO(rs);
				vo.setActionDesc(rs.getString("header_txt"));
				data.add(vo);
			}
		} catch (SQLException sqle) {
			log.error("could not load Seminars", sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		mod.setDataSize(data.size());
		log.debug("loaded " + mod.getDataSize() + " Seminars");
		mod.setActionData(data);
		setAttribute(Constants.MODULE_DATA, mod);
	}
	
	
	/**
	 * transposes a productId into the AAMD specialty it belongs to.
	 * These are static and driven by AAMD, see:
	 * http://www.allaboutmydoc.com/AAMD/productList.jsp
	 * @param productId
	 * @return
	 */
	private static String getSpecialty(String productId) {
		Set<Integer> knee = new HashSet<Integer>(Arrays.asList(new Integer[] {1,205,241,340,541,548}));
		Set<Integer> hip = new HashSet<Integer>(Arrays.asList(new Integer[] {120,121,201,202,360,400,480,401,540}));
		Set<Integer> shoulder = new HashSet<Integer>(Arrays.asList(new Integer[] {100,203}));
		Set<Integer> finger = new HashSet<Integer>(Arrays.asList(new Integer[] {242,280}));
		Set<Integer> ankle = new HashSet<Integer>(Arrays.asList(new Integer[] {240}));
		
		Integer id = Convert.formatInteger(productId);
		if (knee.contains(id)) return "5";
		if (hip.contains(id)) return "4";
		if (shoulder.contains(id)) return "6";
		if (finger.contains(id)) return "3";
		if (ankle.contains(id)) return "1";
		
		return "";
	}
	
}
