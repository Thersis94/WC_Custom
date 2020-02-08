package com.tricumed.cu.tracking;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.tricumed.cu.tracking.vo.UnitVO;

/****************************************************************************
 * <b>Title</b>: UnitLedgerAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since Nov 04, 2010
 ****************************************************************************/
public class UnitLedgerAction extends SBActionAdapter {

	public UnitLedgerAction() {
		super();
	}

	public UnitLedgerAction(ActionInitVO arg0) {
		super(arg0);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.ActionController#update(com.siliconmtn.http.SMTServletRequest)
	 * 
	 * updates all existing ledger entries tied to this Unit and sets the activeFlag=0 (inactive)
	 */
	@Override
	public void update(ActionRequest req) throws ActionException {
		StringBuilder sql = new StringBuilder(150);
		sql.append("update ").append(getCustomSchema());
		sql.append("tricumed_cu_unit_ledger set active_record_flg=0 where unit_id=?");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, (String)req.getAttribute("unitId"));
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error(sqle);
		}
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.ActionController#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		Object msg = null;
		int unitCnt = Convert.formatInteger(req.getParameter("unitCnt"), 1);
		String transactionId = req.getParameter("transactionId");
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		String orgId = site.getOrganizationId();

		StringBuilder sql = new StringBuilder(200);		
		sql.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("tricumed_cu_unit_ledger (unit_id, transaction_id, active_record_flg, ");
		sql.append("create_dt, ledger_id) values (?,?,?,?,?) ");
		log.debug(sql);


		for (int x=1; x <= unitCnt; x++) {
			//check and skip rows missing a serial#, they shouldn't be recorded
			if (!req.hasParameter("serialNo_" + x))
				continue;

			String unitId = saveUnit(req.getParameter("serialNo_" + x), req.getParameter("softwareRev_" + x), orgId, req.getParameter("hardwareRev_" + x));
			req.setAttribute("unitId", unitId);
			update(req);  //break any existing associations

			try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
				ps.setString(1, unitId);
				ps.setString(2, transactionId);
				ps.setInt(3, 1);
				ps.setTimestamp(4, Convert.getCurrentTimestamp());
				ps.setString(5, new UUIDGenerator().getUUID());
				ps.executeUpdate();
				log.debug("inserted ledger for unit " + unitId);
				msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
			} catch (SQLException sqle) {
				log.error(sqle);
				msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
			}
		}

		ModuleVO mod = (ModuleVO)getAttribute(Constants.MODULE_DATA);
		if (msg != null) mod.setErrorMessage(msg.toString());
	}


	/**
	 * calls the UnitAction to save/update the Unit
	 * @param serialNo
	 * @param softwareRev
	 * @return
	 * @throws SQLException
	 */
	private String saveUnit(String serialNo, String softwareRev, String orgId, String hardwareRev) throws ActionException {
		StringBuilder sql= new StringBuilder("select * from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("tricumed_cu_unit where serial_no_txt = ? and (parent_id is null or len(parent_id)=0) ");
		sql.append("and organization_id=?");
		log.debug(sql);

		UnitVO vo = new UnitVO();
		try (PreparedStatement ps= dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, serialNo);
			ps.setString(2, orgId);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				vo = new UnitVO(rs);

		} catch(SQLException sqle){
			log.error(sqle);
		}

		if (vo.getSerialNo() == null) { //no unit found, we'll be adding it!
			vo.setSerialNo(serialNo);
			vo.setSoftwareRevNo(softwareRev);
			vo.setHardwareRevNo(hardwareRev);
		}
		vo.setParentId(vo.getUnitId());
		vo.setStatusId(UnitAction.STATUS_IN_USE);
		vo.setOrganizationId(orgId);
		vo.setDeployedDate(new java.util.Date());

		UnitAction ua = new UnitAction(actionInit);
		ua.setAttributes(attributes);
		ua.setDBConnection(dbConn);
		return ua.saveUnit(vo);
	}
}
