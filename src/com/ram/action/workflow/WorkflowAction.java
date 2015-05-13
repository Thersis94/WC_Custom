package com.ram.action.workflow;

// JDK 7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// RAMDataFeed
import com.ram.workflow.data.WorkflowVO;
import com.ram.workflow.data.WorkflowModuleVO;
import com.ram.workflow.data.WorkflowConfigParamVO;

// SMTBaseLibs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WebCrescendo 2.0
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title: </b>WorkflowAction.java <p/>
 * <b>Project: </b>WC_Custom <p/>
 * <b>Description: </b>
 * </p>
 * <b>Copyright: </b>Copyright (c) 2015<p/>
 * <b>Company: </b>Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0<p/>
 * @since Apr 27, 2015<p/>
 *<b>Changes: </b>
 * Apr 27, 2015: Tim Johnson: Created class.
 ****************************************************************************/
public class WorkflowAction extends SBActionAdapter {
	
	/**
	 * 
	 */
	public WorkflowAction() {
		super(new ActionInitVO());
	}

	/**
	 * @param actionInit
	 */
	public WorkflowAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("WorkflowAction retrieve...");
		String workflowId = StringUtil.checkVal(req.getParameter("workflowId"));
		String customerId = StringUtil.checkVal(req.getParameter("customerId"));
		String customerLocationId = StringUtil.checkVal(req.getParameter("customerLocationId"));
		String serviceCd = StringUtil.checkVal(req.getParameter("serviceCd"));
		String eventTypeCd = StringUtil.checkVal(req.getParameter("eventTypeCd"));

		List<WorkflowVO> data = null;

		/*
		 * Check if this is a DropDown request or not.  DropDown returns all
		 * workflows, Grid has search and sort capabilities that are handled
		 * separately.  If this is an addWorkflow request, simply put an empty
		 * vo on the list.
		 */
		if(req.hasParameter("isDropDown")) {
			data = getUnfilteredWorkflows(customerId, customerLocationId);
		} else if (Convert.formatBoolean(req.getParameter("addWorkflow"))) {
			data = new ArrayList<WorkflowVO>();
			data.add(new WorkflowVO());
		} else if (req.hasParameter("workflowId")) {
			data = new ArrayList<WorkflowVO>();
			data.add(getWorkflowData(workflowId));
		} else if (req.hasParameter("isStore")) {
			//Gather Request Params.
			int start = Convert.formatInteger(req.getParameter("start"), 0);
			int end = Convert.formatInteger(req.getParameter("limit"), 25) + start;

			//Get filtered workflow list for grid view.
			data = getFilteredWorkflows(start, end, serviceCd, eventTypeCd);
		}
		ModuleVO modVo = (ModuleVO) attributes.get(Constants.MODULE_DATA);

		//Only go after record count if we are doing a list.
        modVo.setDataSize((workflowId.equals("") ? 1 : getRecordCount()));
        modVo.setActionData(data);
        this.setAttribute(Constants.MODULE_DATA, modVo);
	}

	/**
	 * Helper method that returns the complete list of RAM_WORKFLOWS for a
	 * dropdown menu sorted by workflow_nm.
	 * @param customerId
	 * @param customerLocationId
	 * @return
	 */
	private List<WorkflowVO> getUnfilteredWorkflows(String customerId, String customerLocationId) {
		List<WorkflowVO> data = new ArrayList<>();
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);

		//Build SQL Query
		StringBuilder sql = new StringBuilder();
		sql.append("select WORKFLOW_ID, WORKFLOW_EVENT_TYPE_CD, SERVICE_CD, ");
		sql.append("WORKFLOW_NM, WORKFLOW_DESC");
		sql.append("from ").append(schema);
		sql.append("RAM_WORKFLOW w left join ").append(schema);
		sql.append("RAM_CUSTOMER_WORKFLOW_XR cw on w.WORKFLOW_ID = cw.WORKFLOW_ID ");
		sql.append("left join ").append(schema);
		sql.append("RAM_CUSTOMER_LOCATION cl on cw.CUSTOMER_LOCATION_ID = cl.CUSTOMER_LOCATION_ID ");

		//Build Where Clause based on the req params that were passed.
		sql.append("where ACTIVE_FLG = 1 ");
		if (customerId.length() > 0) {
			sql.append("and CUSTOMER_ID = ? ");
		} else if (customerLocationId.length() > 0) {
			sql.append("and cw.CUSTOMER_LOCATION_ID = ? ");
		} else {
			sql.append("and cw.CUSTOMER_LOCATION_ID is null ");
		}

		sql.append("order by WORKFLOW_NM ");

		int index = 1;
		//Query for Results and add to list.
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			if (customerId.length() > 0) {
				ps.setString(index++, customerId);
			} else if (customerLocationId.length() > 0) {
				ps.setString(index++, customerLocationId);
			}

			ResultSet rs = ps.executeQuery();
			while(rs.next())
				data.add(new WorkflowVO(rs));
		} catch (SQLException e) {
			log.error(e);
		}

		//Return WorkflowVO Data.
		return data;
	}

	/**
	 * Helper method that retrieves Workflow Data for a given workflowId
	 * @param workflowId
	 * @return
	 */
	public WorkflowVO getWorkflowData(String workflowId) {
		WorkflowVO data = null;
		//List<WorkflowModuleVO> wmods = new ArrayList<WorkflowModuleVO>();
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);

		//Build Query.
		StringBuilder sql = new StringBuilder();
		sql.append("select w.WORKFLOW_ID, w.WORKFLOW_EVENT_TYPE_CD, w.SERVICE_CD, ");
		sql.append("w.WORKFLOW_NM, w.WORKFLOW_DESC, w.ACTIVE_FLG as ACTIVE_FLG, ");
		sql.append("wm.WORKFLOW_MODULE_ID, wm.MODULE_NM, wm.MODULE_DESC, wm.ACTIVE_FLG as MODULE_ACTIVE_FLG, ");
		sql.append("wmx.WORKFLOW_MODULE_XR_ID, wmx.MODULE_ORDER_NO, wmx.CONTINUE_ON_ERROR_FLG, ");
		sql.append("ct.CONFIG_TYPE_CD, ct.CONFIG_TYPE_NM, ct.CONFIG_TYPE_DESC, ");
		sql.append("wmc.WORKFLOW_MODULE_CONFIG_ID, wmc.MODULE_REQUIRED_FLG, ");
		sql.append("wmc.WORKFLOW_REQUIRED_FLG, wmcx.CONFIG_VALUE_TXT, ");
		sql.append("wmcx.WORKFLOW_MODULE_CONFIG_XR_ID ");
		sql.append("from ").append(schema).append("RAM_WORKFLOW w ");
		sql.append("inner join ").append(schema).append("RAM_WORKFLOW_MODULE_XR wmx ");
		sql.append("on w.WORKFLOW_ID = wmx.WORKFLOW_ID ");
		sql.append("inner join ").append(schema).append("RAM_WORKFLOW_MODULE wm ");
		sql.append("on wmx.WORKFLOW_MODULE_ID = wm.WORKFLOW_MODULE_ID ");
		sql.append("inner join ").append(schema).append("RAM_WORKFLOW_MODULE_CONFIG wmc ");
		sql.append("on wm.WORKFLOW_MODULE_ID = wmc.WORKFLOW_MODULE_ID ");
		sql.append("inner join ").append(schema).append("RAM_CONFIG_TYPE ct ");
		sql.append("on wmc.CONFIG_TYPE_CD = ct.CONFIG_TYPE_CD ");
		sql.append("left join ").append(schema).append("RAM_WORKFLOW_MODULE_CONFIG_XR wmcx ");
		sql.append("on wmc.WORKFLOW_MODULE_CONFIG_ID = wmcx.WORKFLOW_MODULE_CONFIG_ID ");
		sql.append("and wmx.WORKFLOW_MODULE_XR_ID = wmcx.WORKFLOW_MODULE_XR_ID ");
		sql.append("where w.WORKFLOW_ID = ? ");
		sql.append("order by wmx.MODULE_ORDER_NO");
		
		log.debug("WorkflowAction getWorkflowData SQL: " + sql.toString() + "|" + workflowId);

		//Query for Results and build WorkflowVO Object.
		int currentModuleXr = -1;
		WorkflowModuleVO wm = null;
		WorkflowConfigParamVO wcp = null;
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, workflowId);
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				//If this is the first row, instantiate the WorkflowVO
				if (data == null) {
					data = new WorkflowVO(rs);
				}

				//Add the WorkflowModuleVO for the row to the Workflow Module List.
				if (rs.getInt("MODULE_ORDER_NO") != currentModuleXr) {
					if (wm != null) data.addWorkflowModule(wm);
					currentModuleXr = rs.getInt("MODULE_ORDER_NO");
					wm = new WorkflowModuleVO(rs);
					wcp = new WorkflowConfigParamVO(rs);
					wm.addConfig(wcp);
				} else {
					wcp = new WorkflowConfigParamVO(rs);
					wm.addConfig(wcp);
				}
			}
			
			//Add the last remaining WorkflowModuleVO
			data.addWorkflowModule(wm);
		} catch (SQLException e) {
			log.error(e);
		}
		return data;
	}

	/**
	 * Helper method that returns a sorted collection of Workflow information
	 * based on the search parameters of the Grid.
	 * @param req
	 * @return
	 */
	public List<WorkflowVO> getFilteredWorkflows(int start, int end, String serviceCd, String eventTypeCd) {
		List<WorkflowVO> data = new ArrayList<>();
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		
		//Build the SQL Query.
		StringBuilder sql = new StringBuilder();
		sql.append("select * from (select ROW_NUMBER() OVER (order by WORKFLOW_NM) ");
		sql.append("as RowNum, w.* from ").append(schema).append("RAM_WORKFLOW w ");

		//Build Where Clause based on the req params that were passed.
		sql.append("where 1=1 ");
		if (serviceCd.length() > 0) {
			sql.append("and SERVICE_CD = ? ");
		} else if (eventTypeCd.length() > 0) {
			sql.append("and WORKFLOW_EVENT_TYPE_CD != ? ");
		}

		//Limit Paginated Result Set.
		sql.append(") as paginatedResult where RowNum >= ? and RowNum < ? order by RowNum");

		log.debug("Workflow retrieve SQL: " + sql.toString() + "|" + serviceCd + " | " + eventTypeCd + " | " + start + " | " + end);
		int index = 1;

		//Build Prepared Statement and iterate the results.
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString());){
			if (serviceCd.length() > 0) {
				ps.setString(index++, serviceCd);
			} else if (eventTypeCd.length() > 0) {
				ps.setString(index++, eventTypeCd);
			}
			ps.setInt(index++, start);
			ps.setInt(index++, end);
			ResultSet rs = ps.executeQuery();

			//Add the WorkflowVOs to the List.
			while(rs.next())
				data.add(new WorkflowVO(rs));

		} catch (SQLException e) {
			log.error("Error retrieving RAM workflow data, ", e);
		}

		return data;
	}
	
	/**
	 * Gets the count of the records
	 * @return
	 * @throws SQLException
	 */
	protected int getRecordCount() {

		StringBuilder sql = new StringBuilder(80);
		sql.append("select count(WORKFLOW_ID) from ");
		sql.append(getAttribute(Constants.CUSTOM_DB_SCHEMA)).append("RAM_WORKFLOW");

		int cnt = 0;
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString());) {

		//Get the count off the first row.
		ResultSet rs = ps.executeQuery();
		if(rs.next())
			cnt = rs.getInt(1);
		} catch(SQLException sqle) {
			log.error("Error retrieving workflow Count", sqle);
		}

		log.debug("Count: " + cnt);
		return cnt;		
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
	}
}
