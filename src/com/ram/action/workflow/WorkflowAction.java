package com.ram.action.workflow;

// JDK 7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ram.workflow.data.WorkflowConfigParamVO;
import com.ram.workflow.data.WorkflowModuleVO;
// RAMDataFeed
import com.ram.workflow.data.WorkflowVO;
import com.ram.workflow.data.WorkflowModuleConfigXrVO;
import com.ram.datafeed.data.CustomerLocationVO;
// SMTBaseLibs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.db.util.RecordDuplicator;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.siliconmtn.http.parser.StringEncoder;
// WebCrescendo 2.0
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;

// WC_Custom 2.0
import com.ram.action.customer.CustomerLocationAction;

//Google Gson 2.2.4
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

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
public class WorkflowAction extends AbstractWorkflowAction {
	
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

	public void copy(SMTServletRequest req) throws ActionException {

		//Validate we have a workflowId to copy.
		if(req.hasParameter("workflowId")) {

			//Copy the Workflow
			String newId = copyWorkflow(req.getParameter("workflowId"));

			/*
			 * Copy the Workflow Module Xr Records
			 * Need to capture new Ids for use in copying the Workflow/ModConfig
			 * Records.  Can use RecordDuplicator to perform the heavy Lifting.
			 * The below should be close.
			 */
			RecordDuplicator rd = new RecordDuplicator(dbConn, "RAM_WORKFLOW_MODULE_XR", "WORKFLOW_MODULE_XR_ID", true);
			rd.setSchemaNm((String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
			rd.addWhereClause("WORKFLOW_ID", req.getParameter("workflowId"));
			try {
				Map<String, String> ids = rd.copyRecords();
			} catch (Exception e) {
				log.error(e);
			}
			/*
			 * Copy the Workflow Module Config Xr Records.  Can use
			 * RecordDuplicator to perform the Heavy Lifting.
			 */

			//If we have a customerLocationId, write the CustomerLoc/WorkflowXR
			if(req.hasParameter("customerLocationId")) {
				insertCustomerWorkflowXr(newId, req.getParameter("customerLocationId"));
			}
		} else {
			//Redirect User with Error Status, Missing WorkflowId
		}
	}

	/**
	 * Helper method that manages inserting a CustomerWorkflowXr Record
	 * @param newId
	 * @param parameter
	 * @throws ActionException
	 */
	private void insertCustomerWorkflowXr(String workflowId, String customerLocationId) throws ActionException {
		String sql = getCustomerWorkflowSql();
		String newId = new UUIDGenerator().getUUID();

		try(PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setString(1, newId);
			ps.setString(2, customerLocationId);
			ps.setString(3, workflowId);
			ps.setTimestamp(5, Convert.getCurrentTimestamp());
			ps.executeUpdate();
		} catch(SQLException sqle) {
			log.error(sqle);
			throw new ActionException("Error Adding Customer Workflow XR", sqle);
		}
	}

	/**
	 * Helper method that returns CustomerWorkflowXr Sql.
	 * @return
	 */
	private String getCustomerWorkflowSql() {
		StringBuilder sql = new StringBuilder();
		sql.append("insert into ").append((String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("RAM_CUSTOMER_WORKFLOW_XR (CUSTOMER_WORKFLOW_XR_ID, ");
		sql.append("CUSTOMER_LOCATION_ID, WORKFLOW_ID, CREATE_DT) values(?,?,?,?)");

		return sql.toString();
	}

	/**
	 * Helper method that manages copying a Workflow.
	 * @param workflowId
	 * @return
	 * @throws ActionException
	 */
	private String copyWorkflow(String workflowId) throws ActionException {
		String sql = getWorkflowCopySql();
		String newId = new UUIDGenerator().getUUID();

		try(PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setString(1, newId);
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			ps.setString(3, workflowId);
			ps.executeUpdate();
		} catch(SQLException sqle) {
			log.error(sqle);
			throw new ActionException("Error Adding Copying Workflow", sqle);
		}

		return newId;
	}

	/**
	 * Helper method that returns WorkflowCopy Sql.
	 * @return
	 */
	public String getWorkflowCopySql() {
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(325);
		sql.append("insert into ").append(schema);
		sql.append("RAM_WORKFLOW (WORKFLOW_ID, WORKFLOW_EVENT_TYPE_CD, SERVICE_CD, ");
		sql.append("WORKFLOW_NM, WORKFLOW_DESC, ACTIVE_FLG, CREATE_DT) select ");
		sql.append("?, WORKFLOW_EVENT_TYPE_CD, SERVICE_CD, WORKFLOW_NM, ");
		sql.append("WORKFLOW_DESC, ACTIVE_FLG, ? from ").append(schema);
		sql.append("RAM_WORKFLOW where WORKFLOW_ID=?");

		return sql.toString();
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
			
			//Get the locations associated to the workflow
			data.setLocations(getWorkflowLocations(workflowId));
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
	 * Gets all Customer Locations associated to a workflow.
	 * @param workflowId
	 * @return
	 */
	private List<CustomerLocationVO> getWorkflowLocations(String workflowId) {
		List<CustomerLocationVO> locations = new ArrayList<>();
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		
		//Build the SQL Query.
		StringBuilder sql = new StringBuilder(75);
		sql.append("select cwxr.CUSTOMER_WORKFLOW_XR_ID, cwxr.CUSTOMER_LOCATION_ID, ");
		sql.append("cwxr.WORKFLOW_ID, cl.LOCATION_NM ");
		sql.append("from ").append(schema).append("RAM_CUSTOMER_WORKFLOW_XR cwxr ");
		sql.append("inner join ").append(schema).append("RAM_CUSTOMER_LOCATION cl ");
		sql.append("on cwxr.CUSTOMER_LOCATION_ID = cl.CUSTOMER_LOCATION_ID ");
		sql.append("where cwxr.WORKFLOW_ID = ? ");

		log.debug("Workflow Location retrieve SQL: " + sql.toString() + "|" + workflowId);

		//Build Prepared Statement and iterate the results.
		int index = 1;
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString());){
			ps.setString(index++, workflowId);
			ResultSet rs = ps.executeQuery();

			//Add the CustomerLocationVOs to the List.
			while(rs.next())
				locations.add(new CustomerLocationVO(rs, false));

		} catch (SQLException e) {
			log.error("Error retrieving RAM workflow location data, ", e);
		}

		return locations;
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
		boolean success = true;
		boolean isInsert = false;
		String msg = null;
		
		//Get WorkflowVO off the request.
		WorkflowVO wf = new WorkflowVO(req);
		wf.setModules(getWorkflowModuleData(req));
		wf.setLocations(getCustomerLocationData(req));
		
		if (wf.getWorkflowId().startsWith("ext")) {
			isInsert = true;
			wf.setWorkflowId(new UUIDGenerator().getUUID());
		}
		
		//Update the Database with all of the workflow data.
		try {
			// Save the Workflow record
			saveObject(wf, isInsert);
			
			// Save the Module Data & Configs
			WorkflowModuleAction wma = new WorkflowModuleAction(actionInit);
			wma.setAttributes(attributes);
			wma.setDBConnection(dbConn);
			for (WorkflowModuleVO module : wf.getModules()) {
				module.setWorkflowId(wf.getWorkflowId());
				wma.modifyModuleXr(module);
				saveConfigs(module);
			}
			
			// Save the Customer Location Data
			CustomerLocationAction cla = new CustomerLocationAction(actionInit);
			cla.setAttributes(attributes);
			cla.setDBConnection(dbConn);
			for (CustomerLocationVO location : wf.getLocations()) {
				location.setWorkflowId(wf.getWorkflowId());
				cla.modifyLocationXr(location);
			}
		} catch(Exception e) {
			log.error("Problem occured while inserting/updating a Workflow Record", e);
			success = false;
		}

		//Update Msg
		if(!success && isInsert) {
			msg = "There was a problem creating the Workflow.";
		} else if (!success) {
			msg = "There was a problem updating the Workflow.";
		}
		
		boolean isJson = Convert.formatBoolean(StringUtil.checkVal(req.getParameter("amid")).length() > 0);
		if (isJson) {
			Map<String, Object> res = new HashMap<>(); 
			res.put("success", true);
			res.put("workflowId", wf.getWorkflowId());
			putModuleData(res);
		} else {
	        // Build the redirect and messages
			// Setup the redirect.
			StringBuilder url = new StringBuilder(50);
			PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
			url.append(page.getRequestURI());
			if (msg != null) url.append("?msg=").append(msg);
			
			log.debug("WorkflowAction redir: " + url);
			req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
			req.setAttribute(Constants.REDIRECT_URL, url.toString());
		}
	}
	
	/**
	 * Gets all of the workflow module data from the JSON parameter
	 * 
	 * @param req
	 * @return
	 */
	private List<WorkflowModuleVO> getWorkflowModuleData(SMTServletRequest req) {
		StringEncoder se = new StringEncoder();
		String modules = se.decodeValue(req.getParameter("modules"));
		log.debug(modules);
		
		Gson gson = new Gson();
		JsonParser parser = new JsonParser();
		List<WorkflowModuleVO> workflowModuleData = new ArrayList<WorkflowModuleVO>();
		JsonArray jArray = parser.parse(modules).getAsJsonArray();
		
		for (JsonElement obj : jArray) {
			WorkflowModuleVO wmvo = gson.fromJson(obj, WorkflowModuleVO.class);
			workflowModuleData.add(wmvo);
		}

		return workflowModuleData;
	}
	
	/**
	 * Gets all of the workflow location data from the JSON parameter
	 * @param req
	 * @return
	 */
	private List<CustomerLocationVO> getCustomerLocationData(SMTServletRequest req) {
		StringEncoder se = new StringEncoder();
		String locations = se.decodeValue(req.getParameter("locations"));
		log.debug(locations);
		
		Gson gson = new Gson();
		JsonParser parser = new JsonParser();
		List<CustomerLocationVO> customerLocationData = new ArrayList<CustomerLocationVO>();
		JsonArray jArray = parser.parse(locations).getAsJsonArray();
		
		for (JsonElement obj : jArray) {
			CustomerLocationVO clvo = gson.fromJson(obj, CustomerLocationVO.class);
			customerLocationData.add(clvo);
		}

		return customerLocationData;
	}
	
	/**
	 * Helper method to save all parameters associated to a Workflow's modules
	 * @param module
	 * @throws Exception
	 */
	private void saveConfigs(WorkflowModuleVO module) throws Exception {
		List<WorkflowConfigParamVO> params = null;
		boolean configIsInsert = false;
		
		params = new ArrayList<WorkflowConfigParamVO>(module.getConfig().values());
		for (WorkflowConfigParamVO param : params) {
			for (WorkflowModuleConfigXrVO config : param.getConfigValues()) {
				configIsInsert = false;
				if (config.getWorkflowModuleConfigXrId().startsWith("ext")) {
					configIsInsert = true;
					config.setWorkflowModuleConfigXrId(new UUIDGenerator().getUUID());
				}
				config.setWorkflowModuleXrId(module.getWorkflowModuleXRId());
				saveObject(config, configIsInsert);
			}
		}
	}

	@Override
	public void delete(SMTServletRequest req) throws ActionException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void update(SMTServletRequest req) throws ActionException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void list(SMTServletRequest req) throws ActionException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected String buildRedirectSupplement(SMTServletRequest req) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String getInUseSql() {
		// TODO Auto-generated method stub
		return null;
	}
}
