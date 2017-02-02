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
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.workflow.data.WorkflowVO;
// WebCrescendo 2.0
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title: </b>WorkflowSearchAction.java <p/>
 * <b>Project: </b>WC_Custom <p/>
 * <b>Description: </b>
 * </p>
 * <b>Copyright: </b>Copyright (c) 2015<p/>
 * <b>Company: </b>Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0<p/>
 * @since Apr 29, 2015<p/>
 *<b>Changes: </b>
 * Apr 29, 2015: Tim Johnson: Created class.
 ****************************************************************************/
public class WorkflowSearchAction extends SBActionAdapter {
	
	/**
	 * 
	 */
	public WorkflowSearchAction() {
		super(new ActionInitVO());
	}

	/**
	 * @param actionInit
	 */
	public WorkflowSearchAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("WorkflowSearchAction retrieve...");
		
		String srchServiceCd = StringUtil.checkVal(req.getParameter("srchServiceCd"));
		String srchEventTypeCd = StringUtil.checkVal(req.getParameter("srchEventTypeCd"));
		int srchActiveFlag = Convert.formatInteger(req.getParameter("srchActiveFlag"), -1, false);
		log.debug("req param searchActiveFlag|converted val: " + req.getParameter("srchActiveFlag") + "|" + srchActiveFlag);
		int start = Convert.formatInteger(req.getParameter("start"), 0);
		int limit = Convert.formatInteger(req.getParameter("limit"), 25) + start;
		List<WorkflowVO> data = new ArrayList<>();

		String sql = buildSql(srchServiceCd, srchEventTypeCd, limit, srchActiveFlag);
		
		log.debug("WorkflowSearchAction retrieve SQL: " + sql.toString() + "|" + srchServiceCd + "|" + srchEventTypeCd);
		int index = 1;
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			if (srchServiceCd.length() > 0) ps.setString(index++, srchServiceCd);
			if (srchEventTypeCd.length() > 0) ps.setString(index++, srchEventTypeCd);
			if (srchActiveFlag > -1) ps.setInt(index++, srchActiveFlag);
			
			ResultSet rs = ps.executeQuery();
			for(int i=0; rs.next(); i++) {
				if (i >= start && i < limit)
					data.add(new WorkflowVO(rs));
			}
		} catch (SQLException e) {
			log.error("Error retrieving RAM workflow data, ", e);
		} finally {
			if (ps != null) {
				try { 	ps.close(); }
				catch (Exception e) { log.error("Error closing PreparedStatement, ", e); }
			}
		}

		ModuleVO modVo = (ModuleVO) attributes.get(Constants.MODULE_DATA);
        modVo.setDataSize(getTotal(srchServiceCd, srchEventTypeCd, limit, srchActiveFlag));
        modVo.setActionData(data);
        this.setAttribute(Constants.MODULE_DATA, modVo);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {}
	
	public int getTotal(String srchServiceCd, String srchEventTypeCd, int limit, int srchActiveFlag) {
		StringBuilder sql = new StringBuilder();
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		sql.append("select count(distinct WORKFLOW_ID) from ").append(schema);
		sql.append("RAM_WORKFLOW ");
		appendWhere(sql, srchServiceCd, srchEventTypeCd, limit, srchActiveFlag);
		
		int cnt = 0, index = 1;
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			if (srchServiceCd.length() > 0) ps.setString(index++, srchServiceCd);
			if (srchEventTypeCd.length() > 0) ps.setString(index++, srchEventTypeCd);
			if (srchActiveFlag > -1) ps.setInt(index++, srchActiveFlag);
			
			ResultSet rs = ps.executeQuery();
			if(rs.next())
				cnt = rs.getInt(1);
		} catch (SQLException e) {
			log.error("Error retrieving RAM workflow totals, ", e);
		} finally {
			DBUtil.close(ps);
		}	
	return cnt;	
	}
	
	public String buildSql(String srchServiceCd, String srchEventTypeCd, int limit, int srchActiveFlag) {
		StringBuilder sql = new StringBuilder();
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);

		sql.append("select top ").append(limit).append(" * from ").append(schema).append("RAM_WORKFLOW ");
		appendWhere(sql, srchServiceCd, srchEventTypeCd, limit, srchActiveFlag);
		sql.append("order by WORKFLOW_NM");
		
		return sql.toString();
	}
	
	public void appendWhere(StringBuilder sql, String srchServiceCd, String srchEventTypeCd, int limit, int srchActiveFlag) {
		sql.append("where 1=1 ");

		if (srchServiceCd.length() > 0) sql.append("and SERVICE_CD like ? ");
		if (srchEventTypeCd.length() > 0) sql.append("and WORKFLOW_EVENT_TYPE_CD = ? ");
		if (srchActiveFlag > -1) sql.append("and ACTIVE_FLG = ? ");
	}
}
