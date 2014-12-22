package com.codman.cu.tracking;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.codman.cu.tracking.vo.AccountUnitReportVO;
import com.codman.cu.tracking.vo.AccountVO;
import com.codman.cu.tracking.vo.AccountReportVO;
import com.codman.cu.tracking.vo.PhysicianReportVO;
import com.codman.cu.tracking.vo.PhysicianVO;
import com.codman.cu.tracking.vo.RequestSearchVO;
import com.codman.cu.tracking.vo.TransactionReportVO;
import com.codman.cu.tracking.vo.TransactionVO;
import com.codman.cu.tracking.vo.UnitVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SecurityController;

/****************************************************************************
 * <b>Title</b>: AccountFacadeAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since Nov 03, 2010
 ****************************************************************************/
public class AccountFacadeAction extends SBActionAdapter {
	public static final String CODMAN_REPS = "codmanReps";
	
	public AccountFacadeAction() {
		super();
	}
	
	public AccountFacadeAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.ActionController#update(com.siliconmtn.http.SMTServletRequest)
	 */
	public void build(SMTServletRequest req) throws ActionException {
		String type = StringUtil.checkVal(req.getParameter("type"));
		String prodCd = StringUtil.checkVal(req.getParameter("productType"),UnitVO.ProdType.MEDSTREAM.toString());
		
		log.debug("facadeType: " + type);
		
		SMTActionInterface sai = null;
		if (type.equalsIgnoreCase("account")) {
			sai = new AccountAction(actionInit);
		} else if (type.equalsIgnoreCase("physician")) {
			sai = new PhysicianAction(actionInit);
		} else if (type.equalsIgnoreCase("transaction")) {
			sai = TransactionFactory.getInstance(prodCd, actionInit );
		} else if (type.equalsIgnoreCase("transUnit")) {
			sai = new UnitTransferAction(actionInit);
		} else if (type.equalsIgnoreCase("returnUnit")) {
			sai = new UnitReturnAction(actionInit);
		}
		if (sai == null) return;
		
		//execute the action
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		sai.build(req);
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.action.ActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@SuppressWarnings("null")
	public void retrieve(SMTServletRequest req) throws ActionException {
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		String type = StringUtil.checkVal(req.getParameter("type"));
		String accountId = StringUtil.checkVal(req.getParameter("accountId"));
		String physicianId = StringUtil.checkVal(req.getParameter("physicianId"));
		String transactionId = StringUtil.checkVal(req.getParameter("transactionId"));
		Boolean isNewTransaction = StringUtil.checkVal(req.getParameter("transactionId")).equals("ADD");
		String prodCd = (String)mod.getAttribute(ModuleVO.ATTRIBUTE_1);
		String unitId = StringUtil.checkVal(req.getParameter("unitId"));
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		RequestSearchVO search = new RequestSearchVO(req, (String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		
		if (physicianId.equals("ADD")) physicianId = "";
		if (transactionId.equals("ADD")) transactionId = "";
		
		
		/*
		 * load the entire data construct from the top down...although large,
		 * this method provides crucial and consistent information to all of our views.
		 * 
		 * This is preferred over calling a dozen actions each loading a single element, 
		 * then having to assemble or keep-track-of all of the individual components.
		 * 
		 * Rep has-a Account
		 * Account has-a Physician
		 * Account has-a Transaction
		 * Transaction has-a Unit
		 */
		StringBuilder sql = new StringBuilder();
		sql.append("select c.profile_id as phys_profile_id, d.product_cd as trans_product_cd, ");
		if (!type.equals("transaction") && !type.equals("unit")) {
			sql.append("* ");
		} else {
			sql.append("a.*, b.account_nm, b.account_no, b.person_id, c.*, d.*, d.create_dt as trans_create_dt, ");
			sql.append("e.*, e.create_dt as deployed_dt, d.update_dt as completed_dt, f.*, g.status_nm ");
		}
		sql.append("from ");
		sql.append(customDb).append("codman_cu_person a ");
		sql.append("inner join ").append(customDb).append("codman_cu_account b ");
		sql.append("on a.person_id=b.person_id ");
		if ((type.equals("transaction") || type.equals("unit")) && !isNewTransaction 
				&& !Convert.formatBoolean(req.getParameter("noTrans"))) {
			//join physicians to transaction
			sql.append("inner join ").append(customDb).append("codman_cu_transaction d ");
			sql.append("on b.account_id=d.account_id ");
			sql.append("inner join ").append(customDb).append("codman_cu_physician c ");
			sql.append("on c.physician_id=d.physician_id ");
		} else {
			//join physicians to the account
			sql.append("left outer join ").append(customDb).append("codman_cu_physician c ");
			sql.append("on b.account_id=c.account_id ");
			sql.append("left outer join ").append(customDb).append("codman_cu_transaction d ");
			sql.append("on b.account_id=d.account_id ");
		}
		sql.append("left outer join ").append(customDb);
		sql.append("codman_cu_unit_ledger e on d.transaction_id=e.transaction_id ");
		if (transactionId.length() == 0) sql.append("and e.active_record_flg=1 ");
		sql.append("left outer join ").append(customDb);
		sql.append("codman_cu_unit f on e.unit_id=f.unit_id ");
		sql.append("left outer join ").append(customDb).append("codman_cu_status g on f.unit_status_id=g.status_id ");
		sql.append("where a.organization_id=? ");
		
		//limit reps to their accounts
		if (role.getRoleLevel() != SecurityController.ADMIN_ROLE_LEVEL && role.getRoleLevel() != 5) {
			sql.append("and a.person_id in (select person_id from ").append(customDb);
			sql.append("CODMAN_CU_PERSON where ORGANIZATION_ID=? and PROFILE_ID=?) ");
		}
		
		if (accountId.length() > 0) sql.append("and b.account_id=? ");
		if (physicianId.length() > 0) sql.append("and c.physician_id=? ");
		if (transactionId.length() > 0) sql.append("and d.transaction_id=? ");
		if (unitId.length() > 0) sql.append("and f.unit_id=? ");
		if (prodCd.length() > 0) sql.append("and b.product_cd=? ");
		
		// add any search filters
		if (search.getStatusId() != null) sql.append("and d.status_id=? ");  //transaction status
		if (search.getAccountName() != null) sql.append("and b.account_nm like ? ");
		if (search.getTerritoryId() != null) sql.append("and a.territory_id=? ");
		if (search.getSerialNoText() != null) sql.append("and f.serial_no_txt like ? ");
		if (search.getRepId() != null) sql.append("and a.person_id=? ");

		getSortOrder(search, sql);
		log.debug(sql);

		TransactionVO trans = null;
		List<AccountVO> data = new ArrayList<AccountVO>();
		Set<String> profileIds = new HashSet<String>();
		PreparedStatement ps = null;
		int resultCnt = 0;
		int i = 1;
		String lastAcctId = "";
		AccountVO acctVo = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(i++, site.getOrganizationId());
			
			if (role.getRoleLevel() != SecurityController.ADMIN_ROLE_LEVEL && role.getRoleLevel() != 5) {
				ps.setString(i++, site.getOrganizationId());
				ps.setString(i++, role.getProfileId());
			}
			
			if (accountId.length() > 0) ps.setString(i++, accountId);
			if (physicianId.length() > 0) ps.setString(i++, physicianId);
			if (transactionId.length() > 0) ps.setString(i++, transactionId);
			if (unitId.length() > 0) ps.setString(i++, unitId);
			if (prodCd.length() > 0) ps.setString(i++, prodCd);
			
			// add any search filters
			if (search.getStatusId() != null) ps.setInt(i++, search.getStatusId());
			if (search.getAccountName() != null) ps.setString(i++, search.getAccountName() + "%");
			if (search.getTerritoryId() != null) ps.setString(i++, search.getTerritoryId());
			if (search.getSerialNoText() != null) ps.setString(i++, search.getSerialNoText() + "%");
			if (search.getRepId() != null) ps.setString(i++, search.getRepId());
			log.debug("start=" + search.getStart() + " end=" + search.getEnd());
			ResultSet rs = ps.executeQuery();
			boolean skip = false;
			while (rs.next()) {
				if (!lastAcctId.equals(rs.getString("account_id"))) {
					if (acctVo != null) {
						data.add(acctVo);
						acctVo = null;
					}
					++resultCnt;
					//log.debug("cnt=" + resultCnt + " " + (resultCnt < search.getStart()));
					lastAcctId = StringUtil.checkVal(rs.getString("account_id"));
					//inline pagination - one account is one row in the table.  Once we have what we need skip the rest
					if (resultCnt < search.getStart() || resultCnt > search.getEnd()) {
						skip = true;
						continue;
					} else {
						skip = false;
					}
					
					acctVo = new AccountVO(rs);
					profileIds.add(acctVo.getRep().getProfileId()); //we'll look these up later!
					//log.debug("added profileId " + acctVo.getRep().getProfileId());
					//lastAcctId = StringUtil.checkVal(acctVo.getAccountId());
					log.debug("loaded account# " + acctVo.getAccountName());
					
				}
				//do not load addtl data for accounts we're not loading
				if (skip) continue;
				
				
				if (rs.getString("physician_id") != null) {
					acctVo.addPhysician(new PhysicianVO(rs));
					profileIds.add(rs.getString("phys_profile_id"));
					//log.debug("added physician lookup for " + rs.getString("phys_profile_id"));
				}
				
				String transId = rs.getString("transaction_id");
				if (transId != null) {
					if (acctVo.getTransactionMap().containsKey(transId)) {
						trans = acctVo.getTransactionMap().get(transId);
					} else {
						trans = new TransactionVO(rs);
					}
					
					if (rs.getString("unit_id") != null)
						trans.addUnit(new UnitVO(rs));

					acctVo.addTransaction(trans);
					trans = null;
				}
			}
			if (acctVo != null)
				data.add(acctVo);
		} catch (SQLException sqle) {
			log.error(sqle);
		} finally { 
			try { ps.close(); } catch (Exception e) {}
		}
		
		//used in the JSP for prev/next buttons
		log.debug("resultCnt=" + resultCnt);
		req.setAttribute("resultCnt", resultCnt);
		
		//if we're adding/editing an account as an Admin, we need a list of Reps for the dropdown menu.
		if (role.getRoleLevel() == SecurityController.ADMIN_ROLE_LEVEL) {
			UserAction ua = new UserAction(actionInit);
			ua.setDBConnection(dbConn);
			ua.setAttributes(attributes);
			if (req.getSession().getAttribute(CODMAN_REPS) == null)
				req.getSession().setAttribute(CODMAN_REPS, ua.loadUserList(SecurityController.PUBLIC_REGISTERED_LEVEL, site.getOrganizationId()));
			ua = null;
			
		}
		if (profileIds.size() > 0) {
			//lookup the profiles for our Reps & Physicians
			ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
			List<AccountVO> newResults = new ArrayList<AccountVO>(data.size());
			try {
				Map<String,UserDataVO> profiles = pm.searchProfileMap(dbConn, new ArrayList<String>(profileIds));
				for (AccountVO vo: data) {
					if (!profiles.containsKey(vo.getRep().getProfileId())) {
						log.error("MISSING PROFILE " + vo.getRep().getProfileId() +" " + vo.getRep().getPersonId());
						log.error(vo.getRep());
					}
					
					vo.getRep().setData(profiles.get(vo.getRep().getProfileId()).getDataMap());
					
					//strip out any results not matching the desired rep.  
					//Since lastName is encrypted this has to be done here (post processing)
					if (search.getRepLastName() != null) {
						if (!search.getRepLastName().equalsIgnoreCase(vo.getRep().getLastName())) {
							continue;
						}
					}
					
					for (PhysicianVO phys : vo.getPhysicians()) {
						if (profiles.containsKey(phys.getProfileId()))
							phys.setData(profiles.get(phys.getProfileId()).getDataMap());
						
						//iterate the transaction and bind the physicians where appropriate
						for (TransactionVO t : vo.getTransactions()) {
							if (t.getPhysician().getPhysicianId().equals(phys.getPhysicianId()))
								t.setPhysician(phys);
							if (!t.isDropShip()) { //we need the Rep's address if not drop-shipping
								t.setRepsAddress(vo.getRep().getLocation());
								t.setShipToName(vo.getRep().getFirstName() + " " + vo.getRep().getLastName());
							}
						}
					}
					
					newResults.add(vo);
				}
				
				data = newResults;
			} catch (DatabaseException de) {
				log.error(de);
			}
			
		}
		
		
		mod.setDataSize(data.size());
		mod.setActionData(data);
		log.debug("data size=" + mod.getDataSize());
		
		
		//export the data as an Excel spreadsheet
		if (Convert.formatBoolean(req.getParameter("excel"))) {
			AbstractSBReportVO rpt = this.initReport(type, site);
			rpt.setData(data);
			req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
			req.setAttribute(Constants.BINARY_DOCUMENT, rpt);
		
		// retrieve additional info depending upon type requested
		} else if (type.equalsIgnoreCase("physician") || isNewTransaction || 
				(type.equals("transUnit") && req.getParameter("toAcctId") != null)) {
			if (req.getParameter("toAcctId") != null)
				req.setParameter("accountId", req.getParameter("toAcctId"));
			
			SMTActionInterface sai = new PhysicianAction(actionInit);
			sai.setAttributes(attributes);
			sai.setDBConnection(dbConn);
			sai.retrieve(req);
			req.setParameter("accountId", accountId);
		}
	}
	
	
	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	/*
	 * A simple Factory Method pattern to determine which report we need to generate
	 */
	private AbstractSBReportVO initReport(String type, SiteVO site) {
		AbstractSBReportVO vo = null;
		
		if ("transaction".equalsIgnoreCase(type)) {
			vo = new TransactionReportVO(site);
		} else if ("unit".equalsIgnoreCase(type)) {
			vo = new AccountUnitReportVO(site);
		} else if ("physician".equalsIgnoreCase(type)) {
			vo = new PhysicianReportVO();
		} else {
			vo = new AccountReportVO();
		}
		
		return vo;
	}
	
	/**
	 * Helper to append the order by clause to the retrieve statement. Some unit
	 * fields (the names of the reps, physicians, and modifying user) are not 
	 * fetched until after the retrieve query is done. So they will be sorted 
	 * with a comparator in UnitVO when requested.
	 * @param req
	 * @param sql
	 */
	private void getSortOrder(RequestSearchVO search, StringBuilder sql) {
		sql.append(" order by b.account_id, ");
		sql.append("d.request_no asc, c.physician_id, ");
		sql.append("d.transaction_id, b.account_nm, b.account_no");
	}
	
	/* with pagination in place, this no longer works -JM 11.19.14
	private class AcctRepComparator implements Comparator<AccountVO>{

		private boolean desc = false;
		
		public AcctRepComparator( boolean isDesc ){
			desc = isDesc;
		}
		public int compare(AccountVO o1, AccountVO o2) {
			// Check the objects for null
			if (o1 == null && o2 == null) return 0;
			if (o1 == null) return -1;
			else if (o2 == null) return 1;
			
			int result =  o1.getRep().getFullName().compareToIgnoreCase(
					o2.getRep().getFullName());
			return ( desc ? result *-1 : result);
		}
	}
	*/
}
