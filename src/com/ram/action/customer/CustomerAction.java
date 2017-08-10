package com.ram.action.customer;

// JDK 7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//RAMDataFeed
import com.ram.action.data.RAMCustomerSearchVO;
import com.ram.datafeed.data.CustomerLocationVO;
import com.ram.datafeed.data.CustomerVO;

// SMTBaseLibs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WebCrescendo 2.0
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title: </b>CustomerAction.java <p/>
 * <b>Project: </b>WC_Custom <p/>
 * <b>Description: </b>
 * </p>
 * <b>Copyright: </b>Copyright (c) 2014<p/>
 * <b>Company: </b>Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0<p/>
 * @since May 20, 2014<p/>
 *<b>Changes: </b>
 * May 20, 2014: David Bargerhuff: Created class.
 ****************************************************************************/
public class CustomerAction extends SBActionAdapter {

	public CustomerAction() {
		super(new ActionInitVO());
	}

	/**
	 * @param actionInit
	 */
	public CustomerAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("CustomerAction retrieve...");
		RAMCustomerSearchVO svo = new RAMCustomerSearchVO(req);
		List<CustomerVO> data = null;

		/*
		 * Check if this is a DropDown request or not.  DropDown returns all
		 * customers, Grid has search and sort capabilities that are handled
		 * separately.  If this is an addCustomer request, simply put an empty
		 * vo on the list.
		 */
		if (svo.isAddCustomer()) {
			data = new ArrayList<>();
			data.add(new CustomerVO());
		} else if (svo.getCustomerId() > 0) {
			data = new ArrayList<>();
			data.add(getCustomerData(svo));
		} else {
			//Get filtered customer list for grid view.
			data = getCustomers(svo, req);
		}
		ModuleVO modVo = (ModuleVO) attributes.get(Constants.MODULE_DATA);

		//Only go after record count if we are doing a list.
		modVo.setDataSize((svo.getCustomerId() > 0 ? 1 : getRecordCount(svo, req)));
		modVo.setActionData(data);
		this.setAttribute(Constants.MODULE_DATA, modVo);
	}

	/**
	 * Helper method that retrieves Customer Data for a given customerId
	 * @param customerId
	 * @return
	 */
	public CustomerVO getCustomerData(RAMCustomerSearchVO svo) {
		CustomerVO c = null;
		List<CustomerLocationVO> locs = new ArrayList<>();
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);

		//Build Query.
		StringBuilder sql = new StringBuilder(180);
		sql.append("select * from ").append(schema).append("RAM_CUSTOMER a ");
		sql.append("left outer join ").append(schema).append("ram_customer_location b ");
		sql.append("on a.customer_id = b.customer_id where a.CUSTOMER_ID = ? ");
		sql.append("order by b.LOCATION_NM");

		//Query for Results and build CustomerVO Object.
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setInt(1, svo.getCustomerId());
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {

				//If this is the first row, instantiate the CustomerVO
				if(c == null) {
					c = new CustomerVO(rs, false);
				}

				//Add the CustomerLocationVO for the row to the Location List.
				locs.add(new CustomerLocationVO(rs, false));
			}

			//Set the Locations on the CustomerVO.
			if(c != null) {
				c.setCustomerLocations(locs);
			}
		} catch (SQLException e) {
			log.error(e);
		}
		return c;
	}

	/**
	 * Helper method that returns a sorted collection of Customer Information
	 * based on the search parameters of the Grid.
	 * @param req 
	 * @param req
	 * @return
	 */
	public List<CustomerVO> getCustomers(RAMCustomerSearchVO svo, ActionRequest req) {
		List<CustomerVO> customers = new ArrayList<>();
		List<Object> params = new ArrayList<>();
		DBProcessor db = new DBProcessor(getDBConnection());
		List<Object> data = db.executeSelect(getCustomerLookupQuery(svo, req, params ), params, new CustomerVO());

		for (Object ob : data) {
			CustomerVO c = (CustomerVO) ob;
			customers.add(c);
		}
		return customers;
	}

	public static List<Object> buildCustomerQueryParams(RAMCustomerSearchVO svo) {
		List<Object> params = new ArrayList<>();

		if (!StringUtil.isEmpty(svo.getCustomerTypeId())) {
			params.add(svo.getCustomerTypeId());
		} else if (!StringUtil.isEmpty(svo.getExcludeTypeId())) {
			params.add(svo.getExcludeTypeId());
		}

		if (svo.getCustomerId() > 0) {
			params.add(svo.getCustomerId());
		}

		//Add Kit Flag Value.
		if(svo.isKitsOnly()) {
			params.add(1);
		}

		//Add Paginations.
		if(svo.isPaginated()) {
			params.add(svo.getStart());
			params.add(svo.getLimit());
		}

		return params;
}
	
	/**
	 * @param svo
	 * @param req
	 * @param params
	 * @return
	 */
	private String getCountQuery(RAMCustomerSearchVO svo, ActionRequest req, List<Object> cParams) {
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder cSql = new StringBuilder(512);
		buildCountSelect(cSql,schema);
		
		cSql.append(getCustomerLookupWhereClause(svo, schema, req, cParams));
		buildFilter(cSql, req, cParams);
		return cSql.toString();
	}

	/**
	 * Gets the count of the records
	 * @param req 
	 * @param customerId
	 * @param term
	 * @return
	 * @throws SQLException
	 */
	protected int getRecordCount(RAMCustomerSearchVO svo, ActionRequest req) {
		svo.setCount(true);
		svo.setPaginated(false);
		List<Object> cParams = new ArrayList<>();
		DBProcessor db = new DBProcessor(getDBConnection());
		List<Object> count = db.executeSelect(getCountQuery(svo, req, cParams ), cParams, new GenericVO());
		
		GenericVO gen = (GenericVO)count.get(0);
		log.debug("count of records: " + gen.getKey());
		
		return Convert.formatInteger(StringUtil.checkVal(gen.getKey()));
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.debug("Customer action build");
		
		//Gather Req Params.
		CustomerVO vo = new CustomerVO(req);
		boolean reactivate = (Convert.formatBoolean(req.getParameter("activate")));
		boolean deactivate = (Convert.formatBoolean(req.getParameter("deactivate")));
		String msg;

		//Perform appropriate action based on req Param.  Trap any errors.
		try {
			if(reactivate) {
				updateActive(vo.getCustomerId(), true);
				msg = "activated";
			} else if(deactivate) {
				updateActive(vo.getCustomerId(), false);
				msg = "deactivated";
			} else {
				modifyCustomer(vo);
				if(vo.getCustomerId() > 0) {
					msg = "updated";
				} else {
					msg = "inserted";
				}
			}
		} catch(Exception e) {
			msg = "An error occurred";
			log.error("An error occurred while updating a Customer Record.", e);
		}

		//Check if this is an Ajax call and prepare appropriate response.
		boolean isJson = !StringUtil.isEmpty(req.getParameter("amid"));
		if (isJson) {
			Map<String, Object> res = new HashMap<>(); 
			res.put("success", true);
			putModuleData(res);
		} else {
			// Build the redirect and messages
			StringBuilder url = new StringBuilder(50);
			PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
			url.append(page.getRequestURI());
			url.append("?msg=").append(msg);

			// Setup the redirect.
			log.debug("CustomerAction redir: " + url);
			req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
			req.setAttribute(Constants.REDIRECT_URL, url.toString());
		}
	}

	/**
	 * Helper method that manages updating the Active_FLG on a Customer Record.
	 * @param cId
	 * @param isActivation
	 * @return
	 * @throws SQLException
	 */
	public void updateActive(int cId, boolean isActivation) throws SQLException {
		StringBuilder sql = new StringBuilder(100);

		//build Query.
		sql.append("update ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("RAM_CUSTOMER set ACTIVE_FLG = ?, UPDATE_DT = ? ");
		sql.append("where CUSTOMER_ID = ?");

		//Update Record.
		int i = 0;
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setInt(i++, Convert.formatInteger(isActivation));
			ps.setTimestamp(i++, Convert.getCurrentTimestamp());
			ps.setInt(i++, cId);

			ps.executeUpdate();
		}
	}

	/**
	 * builds the query to get the list of customers
	 * @param svo
	 * @param req
	 * @param params
	 * @return
	 */
	public String getCustomerLookupQuery(RAMCustomerSearchVO svo, ActionRequest req, List<Object> params) {
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);

		StringBuilder sql = new StringBuilder(512);
		buildSelect(sql,schema);



		//Add Where Clause
		sql.append(getCustomerLookupWhereClause(svo, schema, req,  params));
		// Build the filters
		buildFilter(sql, req, params);
		buildOrder(sql, req, params);

		log.info("Customer retrieve SQL: " + sql.toString() + "|" + svo.getCustomerTypeId() + " | " + svo.getExcludeTypeId());

		return sql.toString();
	}

	/**
	 * geneartes the sort of order information for the query
	 * @param sql
	 * @param req
	 * @param params
	 */
	private void buildOrder(StringBuilder sql, ActionRequest req, List<Object> params) {

		String sort = null;
		if (req.hasParameter("sort")) {
			String sortParam = req.getParameter("sort");
			
			if("locationCount".equalsIgnoreCase(sortParam)) sort = "location_count_no ";
			if("activeFlag".equalsIgnoreCase(sortParam)) sort = "active_flg ";
			if("customerName".equalsIgnoreCase(sortParam)) sort = "customer_nm ";
		}
		
		//if sort is still empty default back to customer name
		sort = StringUtil.checkVal(sort , " customer_nm ");
		sort += " " + StringUtil.checkVal(req.getParameter("order"), " asc ");
		
		sql.append("order by ").append(sort);
		sql.append(" limit ? offset ? ");
		params.add(Convert.formatInteger(req.getParameter("limit"), 10));
		params.add(Convert.formatInteger(req.getParameter("offset"), 0));
	}

	/**
	 * @param sql
	 * @param req
	 * @param params
	 */
	private void buildFilter(StringBuilder sql, ActionRequest req, List<Object> params) {

		// search the customer name
		if (req.hasParameter("search")) {
			String search = StringUtil.checkVal(req.getParameter("search"));
			sql.append("and (lower(customer_nm) like ? ) ");
			params.add("%" + search.toLowerCase() + "%");
		}
	}

	/**
	 * generates the select section fo the query
	 * @param sql
	 */
	private void buildSelect(StringBuilder sql, String schema) {
		sql.append("select a.*, coalesce( cc.location_count_no, 0) as location_count_no from ").append(schema).append("ram_customer a ");
		sql.append("left outer join ");
		sql.append("( select customer_id, count(*) as LOCATION_COUNT_NO from ").append(schema).append("ram_customer_location ");
		sql.append("group by customer_id ");
		sql.append(") cc on cc.customer_id = a.customer_id ");
	}

	/**
	 * geneates the select of the count query
	 * @param cSql
	 * @param schema 
	 */
	private void buildCountSelect(StringBuilder cSql, String schema) {
		cSql.append("select count(*) as key from ").append(schema).append("ram_customer a ");;
	}

	/**
	 * generates the where clause of the query
	 * @param svo
	 * @param req 
	 * @param req
	 * @param params
	 * @return
	 */
	public static String getCustomerLookupWhereClause(RAMCustomerSearchVO svo, String schema, ActionRequest req, List<Object> params) {
		StringBuilder sql = new StringBuilder(150);

		//Build Where Clause based of req Params that were passed.
		sql.append(DBUtil.WHERE_1_CLAUSE);

		if (svo.getCustomerTypeId().length() > 0) {
			sql.append("and customer_type_id = ? ");
			params.add(svo.getCustomerTypeId());
		} else if (svo.getExcludeTypeId().length() > 0) {
			sql.append("and customer_type_id != ? ");
			params.add(svo.getExcludeTypeId());
		}

		if (svo.getCustomerId() > 0) {
			sql.append("and customer_id = ? ");
		}
		
		if(svo.isKitsOnly()) {
			sql.append("and customer_id in (select distinct CUSTOMER_ID ").append(DBUtil.FROM_CLAUSE);
			sql.append(schema).append("RAM_PRODUCT ");
			sql.append(DBUtil.WHERE_CLAUSE).append("KIT_FLG = ?) ");
			params.add(1);
		}
		
		generateParamFilters(req, sql, params, schema);
		
		return sql.toString();
	}
	/**
	 * looks at the request object and sets filter queries and values
	 * @param req
	 * @param sql
	 * @param params 
	 */
	private static void generateParamFilters(ActionRequest req, StringBuilder sql, List<Object> params, String schema) {
		
		if (req.hasParameter("srchState") && !req.getParameter("srchState").isEmpty()) {
			String state = StringUtil.checkVal(req.getParameter("srchState"));
			sql.append("and a.customer_id in (select customer_id from ").append(schema).append("ram_customer_location where state_cd = ?  ) ");
			params.add(state.toUpperCase());
		}

		if (req.hasParameter("srchCity")) {
			String city = StringUtil.checkVal(req.getParameter("srchCity"));
			sql.append("and a.customer_id in (select customer_id from ").append(schema).append("ram_customer_location where lower(city_nm) like ? ) ");
			params.add("%" + city.toLowerCase() + "%");
		}

		if (req.hasParameter("srchActiveFlg")) {
			Integer activeFlg = Convert.formatInteger(StringUtil.checkVal(req.getParameter("srchActiveFlg")));
			sql.append("and a.active_flg = ?  ");
			params.add( activeFlg );
		}
		
	}

	/**
	 * Helper method that manages inserting and updating a Customer Record
	 * in the Database.
	 * @param v
	 * @return
	 * @throws SQLException
	 */
	public void modifyCustomer(CustomerVO vo) throws SQLException {
		StringBuilder sql = new StringBuilder(160);

		//Build appropriate SQL Statement.
		if (vo.getCustomerId() > 0) {
			// is an update
			sql.append("update ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
			sql.append("RAM_CUSTOMER ");
			sql.append("set ORGANIZATION_ID = ?, CUSTOMER_TYPE_ID = ?, ");
			sql.append("CUSTOMER_NM = ?, ACTIVE_FLG = ?, ");
			sql.append("UPDATE_DT = ? WHERE CUSTOMER_ID = ?");
		} else {
			// is an insert
			sql.append("insert into ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
			sql.append("RAM_CUSTOMER ");
			sql.append("(ORGANIZATION_ID, CUSTOMER_TYPE_ID, CUSTOMER_NM, ");
			sql.append("ACTIVE_FLG, CREATE_DT) values (?,?,?,?,?)");
		}

		//Exequte Query
		int i = 1;
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			//set insert/update params.
			ps.setString(i++, vo.getOrganizationId());
			ps.setString(i++, vo.getCustomerTypeId());
			ps.setString(i++, vo.getCustomerName());
			ps.setInt(i++, vo.getActiveFlag());
			ps.setTimestamp(i++, Convert.getCurrentTimestamp());
			if (vo.getCustomerId() > 0) ps.setInt(i++, vo.getCustomerId());

			ps.executeUpdate();
		}
	}


}
