package com.depuy.events_v2;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
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
 * @updates
 * 		JM - 7.14.16 - changed actionId (attrib1) to support multiple values (saved as a comma delimited String).
 * 			this allows front-end searches to run across joint-recon seminars as well as Mitek seminars.
 ****************************************************************************/
public class DePuyEventSearchAction extends SimpleActionAdapter {

	public DePuyEventSearchAction() {
		super();
	}

	public DePuyEventSearchAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	@Override
	public void update(SMTServletRequest req) throws ActionException {		
		String[] attr1 = req.getParameterValues("attrib1Text");
		req.setParameter("attrib1Text", StringUtil.getToString(attr1, false, false, ","));
		log.debug("set attrib1Text=" + req.getParameter("attrib1Text"));
		super.update(req);
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
		//use the actionId off the request if we have one; because the value for attrib1 could be multiple actionIds
		String actionId = StringUtil.checkVal(req.getParameter("actionId"));
		if (actionId.length() == 0) {
			String[] actionIds = StringUtil.checkVal(mod.getAttribute(ModuleVO.ATTRIBUTE_1)).split(",");
			if (actionIds != null && actionIds.length > 0) 
				actionId = actionIds[0];
		}
		actionInit.setActionId(actionId);
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
		
		boolean isRobot = Convert.formatBoolean(req.getAttribute(Constants.BOT_REQUEST));
		log.debug("robot? " + isRobot);
		
		String distSql = !isRobot ? eta.buildSpatialClause(req): String.valueOf(Integer.MAX_VALUE); //don't let bots find any seminars; set their distance impossibly high
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String[] actionIds = StringUtil.checkVal(mod.getAttribute(ModuleVO.ATTRIBUTE_1)).split(",");
		String[] specialties = req.getParameterValues("specialty");
		if (req.hasParameter("specialtyId"))
			specialties = req.getParameterValues("specialtyId");
		if (specialties == null) specialties = new String[0];
		
		StringBuilder sql = new StringBuilder(1000);
		sql.append("select ee.*, et.*, eg.header_txt, ").append(distSql);
		sql.append(" as distance, des.surgeon_nm as contact_nm, sxr.joint_id, sb.action_group_id ");
		sql.append("from event_entry ee ");
		sql.append("inner join event_type et on ee.event_type_id=et.event_type_id ");
		sql.append("inner join event_group eg on et.action_id=eg.action_id ");
		sql.append("inner join sb_action sb on eg.action_id=sb.action_group_id and sb.action_group_id in ( ");
		for (int x=0; x < actionIds.length; x++) {
			if (x > 0) sql.append(",");
			sql.append("?");
		}
		sql.append(") ");
		sql.append("inner join event_postcard_assoc epa on ee.event_entry_id=epa.event_entry_id ");
		sql.append("inner join event_postcard ep on epa.event_postcard_id=ep.event_postcard_id ");
		sql.append("inner join ").append(customDb).append("depuy_event_specialty_xr sxr on ep.event_postcard_id=sxr.event_postcard_id ");
		if (specialties.length > 0) {
			sql.append("and sxr.joint_id in ( ");
			for (int x=0; x < specialties.length; x++) {
				if (x > 0) sql.append(",");
				sql.append("?");
			}
			sql.append(") ");
		}
		
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
		
		Map<String,EventEntryVO> data = new HashMap<String,EventEntryVO>();
		int x = 1;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			for (String s : actionIds) 
				ps.setString(x++, s);
			
			for (int i=0; i < specialties.length; i++) 
				ps.setInt(x++, Convert.formatInteger(specialties[i]));
			
			ps.setInt(x++, EventFacadeAction.STATUS_APPROVED);
			if (req.hasParameter("eventEntryId")) {
				ps.setString(x++, req.getParameter("eventEntryId"));
			} else if (req.hasParameter("rsc")) {
				ps.setString(x++, req.getParameter("rsc"));
			} else {
				ps.setDate(x++, Convert.formatSQLDate(Calendar.getInstance().getTime()));
				ps.setInt(x++, Convert.formatInteger(req.getParameter("radius"), 50));
			}

			EventEntryVO vo;
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				vo = data.get(rs.getString("event_entry_id"));
				if (vo == null) {
					vo = new EventEntryVO(rs);
					vo.setActionDesc(rs.getString("header_txt"));
					vo.setAttribute("jointId", rs.getInt("joint_id"));
				} else {
					//add possible second speaker to an existing seminar - JM 10.15.16
					String spkr2 = StringUtil.checkVal(rs.getString("contact_nm"));
					//if there is a 2nd speaker, and we don't already have them listed, add them to the roster
					if (!spkr2.isEmpty() && StringUtil.checkVal(vo.getContactName()).indexOf(spkr2) == -1)
						vo.setContactName(vo.getContactName() + " and " + spkr2);
				}
				data.put(vo.getActionId(),vo);
			}
		} catch (SQLException sqle) {
			log.error("could not load Seminars", sqle);
		}
		mod.setDataSize(data.size());
		log.debug("loaded " + mod.getDataSize() + " Seminars");
		mod.setActionData(new ArrayList<EventEntryVO>(data.values()));
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
