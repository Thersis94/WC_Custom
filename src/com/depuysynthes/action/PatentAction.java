package com.depuysynthes.action;

// Java 8
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

//SMT Base Libs
import com.depuysynthes.action.PatentActivityAction.ActivityType;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;

// WC Libs
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: PatentAction <p/>
 * <b>Description:</b> A SimpleAction that loads DePuy Synthes Patents from 
 *  a table in CustomDB.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Dec 4, 2015<p/>
 * Change Log:
 * 2018-04-02: DBargerhuff, DS-392, removed Excel file import in lieu of creating
 * data tool for managing patent records.
 ****************************************************************************/
public class PatentAction extends SimpleActionAdapter {

	public static final String PATENT_ID = "patentId";
	
	public PatentAction () {
		super();
	}

	public PatentAction (ActionInitVO actionInit) {
		super(actionInit);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(ActionRequest req) throws ActionException {
		super.update(req);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		//ensure we were given something to search for, otherwise a query is not needed (the search form is displayed)
		if (!req.hasParameter("code")) return;
		SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);

		StringBuilder sql = new StringBuilder(150);
		sql.append("select item_txt, desc_txt, code_txt, patents_txt, redirect_nm, redirect_address_txt from ");
		sql.append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("dpy_syn_patent where code_txt=? and organization_id=? ");
		sql.append("and status_flg=? ");
		sql.append("limit 1 ");
		log.debug(sql + "|" + req.getParameter("code") + "|" + site.getOrganizationId());

		//note this lookup only returns one record, ever.  0 or 1 matching to the searched value.
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, req.getParameter("code"));
			ps.setString(2, site.getOrganizationId());
			ps.setInt(3, ActivityType.ACTIVE.getTypeId());
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				super.putModuleData(new PatentVO(rs));


		} catch (SQLException sqle) {
			log.error("could not load patents", sqle);
		}
	}
	
}