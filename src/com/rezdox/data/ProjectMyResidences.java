package com.rezdox.data;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.rezdox.action.BusinessAction;
import com.rezdox.action.ResidenceAction;
import com.rezdox.action.RezDoxUtils;
import com.rezdox.vo.ResidenceVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;

/****************************************************************************
 * <b>Title:</b> ProjectMyResidences.java<br/>
 * <b>Description:</b> Returns a list of residences this business owner is connected to.  Used by Projects. 
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2018<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Mar 23, 2018
 ****************************************************************************/
public class ProjectMyResidences extends SimpleActionAdapter {

	protected static final String SPACE = " ";
	protected static final String CSPACE = ", ";

	public ProjectMyResidences() {
		super();
	}

	public ProjectMyResidences(ActionInitVO arg0) {
		super(arg0);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (req.hasParameter("personal")) {
			loadMyResidences(req);
		} else {
			loadBusinessContactResidences(req);
		}
	}


	/**
	 * Generates a list of residences tied to me - used on Home History log.
	 * @param req
	 */
	private void loadMyResidences(ActionRequest req) {
		ResidenceAction ra = new ResidenceAction(getDBConnection(), getAttributes());
		List<ResidenceVO> residences = ra.listMyResidences(RezDoxUtils.getMemberId(req), null);
		List<GenericVO> data = new ArrayList<>();

		for (ResidenceVO vo : residences)
			data.add(new GenericVO(vo.getResidenceId(), vo.getResidenceName()));

		log.debug(String.format("loaded %d residences", data.size()));
		putModuleData(data);
	}


	/**
	 * Generates a list of residences I'm connected to - tied to business' projects
	 * @param req
	 */
	private void loadBusinessContactResidences(ActionRequest req) {
		String schema = getCustomSchema();
		String businessId = StringUtil.checkVal(req.getSession().getAttribute(BusinessAction.REQ_BUSINESS_ID));
		StringBuilder sql = new StringBuilder(200);
		sql.append("select distinct r.residence_id, r.residence_nm, r.address_txt, r.city_nm, r.state_cd, r.zip_cd from ");
		sql.append(schema).append("REZDOX_RESIDENCE r ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("REZDOX_RESIDENCE_MEMBER_XR mxr on r.residence_id=mxr.residence_id and mxr.status_flg=1 ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("REZDOX_CONNECTION c on mxr.member_id=c.sndr_member_id or mxr.member_id=c.rcpt_member_id ");
		sql.append("where (c.sndr_business_id=? or c.rcpt_business_id=?) and c.approved_flg=1 ");
		sql.append("order by r.residence_nm, r.state_cd, r.city_nm, r.address_txt");
		log.debug(sql);

		List<GenericVO> data = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, businessId);
			ps.setString(2, businessId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				StringBuilder label = new StringBuilder(100);
				appendNotEmpty(rs.getString(2), label, CSPACE);
				appendNotEmpty(rs.getString(3), label, CSPACE);
				appendNotEmpty(rs.getString(4), label, CSPACE);
				appendNotEmpty(rs.getString(5), label, SPACE);
				appendNotEmpty(rs.getString(6), label, "");

				data.add(new GenericVO(rs.getString(1), label.toString()));
			}
		} catch (SQLException sqle) {
			log.error("could not load business's connected residences", sqle);
		}

		log.debug(String.format("loaded %d residences", data.size()));
		putModuleData(data);
	}

	/**
	 * helper to check for null/empty before appending the value, followed by its delimiter 
	 * @param value
	 * @param sb
	 * @param delim
	 */
	protected void appendNotEmpty(String value, StringBuilder sb, String delim) {
		if (StringUtil.isEmpty(value)) return;
		sb.append(value).append(delim);
	}
}