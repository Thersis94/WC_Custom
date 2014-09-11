package com.fastsigns.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fastsigns.action.approval.vo.AbstractChangeLogVO;
import com.fastsigns.action.vo.CareersVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.contact.ContactFacadeAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

public class CareersAction extends SBActionAdapter {

	public CareersAction(){
		super();
	}
	
	public CareersAction(ActionInitVO actionInit){
		super(actionInit);
	}
	
	/**
	 * Not implemented on site.  This would forward any form submittals to contact
	 * us to be processed there. The action is then cached for the next 24 hours.
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);		
		// Call the Contact Facade Retrieve Action
		SMTActionInterface sai = new ContactFacadeAction(this.actionInit);
		actionInit.setActionId((String) mod.getAttribute(SBModuleVO.ATTRIBUTE_1));
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		sai.build(req);
		
	}
	/**
	 * This method retrieves all career entries were created in the last 3 weeks 
	 * that are approved for viewing and puts them in a list for the view. 
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		Map<String, List<CareersVO>> postings = new LinkedHashMap<String, List<CareersVO>>();
		super.retrieve(req);
		String orgId = ((SiteVO)req.getAttribute("siteData")).getOrganizationId();
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		log.debug(mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		StringBuilder sb = new StringBuilder();
		sb.append("select a.*, b.LOCATION_NM, b.STATE_CD, b.CITY_NM, b.ATTRIB2_TXT from ");
		sb.append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sb.append("FTS_JOB_POSTING a left outer join DEALER_LOCATION b on a.FRANCHISE_ID = b.DEALER_LOCATION_ID ");
		sb.append("where a.ORGANIZATION_ID = ? and ");
		if (Convert.formatBoolean(mod.getAttribute(ModuleVO.ATTRIBUTE_1))) {
			sb.append("DATEDIFF(DAY, JOB_POST_DT, GETDATE()) < 21 ");
		} else {
			sb.append("((DATEDIFF(DAY, JOB_POST_DT, GETDATE()) < 21 and a.FRANCHISE_ID is null) or a.FRANCHISE_ID is not null) ");
		}
		sb.append("and JOB_APPROVAL_FLG = ? order by JOB_POST_DT");
		PreparedStatement ps = null;
		log.debug(sb + " | " + orgId + " | " + AbstractChangeLogVO.Status.APPROVED.ordinal());
		List<CareersVO> centerCareers = new ArrayList<CareersVO>();
		List<CareersVO> corpCareers = new ArrayList<CareersVO>();
		try{
			int ctr = 1;
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(ctr++, orgId);
			ps.setInt(ctr++, AbstractChangeLogVO.Status.APPROVED.ordinal());
			
			ResultSet rs = ps.executeQuery();
			while(rs.next()){
				if(rs.getString("FRANCHISE_ID") == null){
					corpCareers.add(new CareersVO(rs));
				} else {
					centerCareers.add(new CareersVO(rs));
				}
			}
			postings.put("0", corpCareers);
			postings.put("1", centerCareers);
			log.debug("retrieved " + postings.size() + " job postings");
		} catch(SQLException sqle){
			log.error("An error was thrown while retrieving ", sqle);
		} finally {
			try {
				if (ps != null) ps.close();
			} catch (SQLException e) {
				log.error(e);
			}
		}
		
		mod.setActionData(postings);
		if (mod.isCacheable()) {
			mod.setCacheTimeout(86400);
			int len = mod.getCacheGroups().length+1;
			String [] cacheGroups = Arrays.copyOf(mod.getCacheGroups(), len);
			cacheGroups[len-1] = orgId +"_CAREERS";
			mod.setCacheGroups(cacheGroups);
		}
		attributes.put(Constants.MODULE_DATA, mod);
	}
	
	
}
