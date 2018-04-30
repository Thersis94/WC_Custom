package com.sigmapi.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.common.constants.Operator;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.UserDataComparator;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.data.DataContainer;
import com.smt.sitebuilder.data.DataManagerFacade;
import com.smt.sitebuilder.data.vo.GenericQueryVO;
import com.smt.sitebuilder.data.vo.GenericQueryVO.ColumnName;

import com.smt.sitebuilder.data.vo.QueryParamVO;
import com.smt.sitebuilder.security.SecurityController;

/****************************************************************************
 * <b>Title</b>: AlumniDatabaseAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Aug 17, 2011
 ****************************************************************************/
public class AlumniDatabaseAction extends SimpleActionAdapter {

	private static final String SIGMAPI_FORM = ""; //pkId of the Form inside the black box

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);

		//get the profileIds of all our users...
		List<AlumniVO> data = new ArrayList<>();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select b.profile_id, max(c.login_dt) from profile_role a ");
		sql.append("inner join profile b on a.profile_id=b.profile_id ");
		sql.append("left outer join authentication_log c on b.authentication_id=c.authentication_id ");
		sql.append("where a.site_id=? and a.status_id > ? ");
		sql.append("group by b.profile_id");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, site.getSiteId());
			ps.setInt(2, SecurityController.STATUS_PENDING);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				AlumniVO v = new AlumniVO();
				v.setProfileId(rs.getString(1));
				v.setLastLoginDate(rs.getDate(2));
				data.add(v);
			}

		} catch (SQLException sqle) {
			log.error("could not load alumni list", sqle);
		}

		DataContainer dc = loadFormResponses(site, data);

		if (dc.hasErrors())
			log.error(dc.getErrors().values());

		//get the core (profile) data for these users
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		pm.setOrganizationId(site.getOrganizationId());
		try {
			pm.populateRecords(dbConn, data);
		} catch (DatabaseException e) {
			log.error(e);
		}


		//merge the data across our 3 collection sources
		mergeData(data, dc);

		//sort the results by lastName
		Collections.sort(data, new UserDataComparator());
		putModuleData(data);
	}


	/**
	 * @param data
	 * @param dc
	 */
	private void mergeData(List<AlumniVO> data, DataContainer dc) {
		for (AlumniVO vo : data) {
			vo.setExtData(dc.getTransactions().get(vo.getProfileId()));

			//set status by evaluating opt-in/out
			String optReason = StringUtil.checkVal(vo.getAllowCommunicationReason());
			if (vo.getAllowCommunication() == 0 && optReason.isEmpty() || "no reason given".equalsIgnoreCase(optReason)) {
				//user opt-out but gave no reason
				vo.setAllowCommunicationReason("opt-out");
			} else if (vo.getAllowCommunication() == 1 && vo.getValidEmailFlag() == 0) {
				//user opt-in, but has invalid email address
				vo.setAllowCommunicationReason("not receiving emails");
			} else if (vo.getAllowCommunication() == 1 && vo.getValidEmailFlag() == 1) {
				//user opt-in, and has valid email address
				vo.setAllowCommunicationReason("");
			}
		}
	}


	/**
	 * @return
	 */
	private DataContainer loadFormResponses(SiteVO site, List<AlumniVO> data) {
		List<String> profileIds = new ArrayList<>();
		for (AlumniVO vo : data)
			profileIds.add(vo.getProfileId());

		//call the black box for our extended profile data
		QueryParamVO param1 = new QueryParamVO();
		param1.setColumnNm(ColumnName.FORM_SUBMITTAL_ID);
		param1.setOperator(Operator.IN);
		param1.setValues(profileIds.toArray(new String[profileIds.size()]));

		GenericQueryVO query = new GenericQueryVO(SIGMAPI_FORM);
		query.setOrganizationId(site.getOrganizationId());
		query.addConditional(param1);

		DataContainer dc = new DataContainer();
		dc.setQuery(query);
		DataManagerFacade dmf = new DataManagerFacade(attributes, dbConn, null);
		dmf.loadTransactions(dc);

		return dc;
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		//unused
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}
}