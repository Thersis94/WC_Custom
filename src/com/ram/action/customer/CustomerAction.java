package com.ram.action.customer;

// JDK 7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// RAMDataFeed
import com.ram.datafeed.data.CustomerLocationVO;
import com.ram.datafeed.data.CustomerVO;

// SMTBaseLibs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
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
	
	/**
	 * 
	 */
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
	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("CustomerAction retrieve...");
		int customerId = Convert.formatInteger(req.getParameter("customerId"), 0);
		List<CustomerVO> data = null;

		/*
		 * Check if this is a DropDown request or not.  DropDown returns all
		 * customers, Grid has search and sort capabilities that are handled
		 * separately.  If this is an addCustomer request, simply put an empty
		 * vo on the list.
		 */
		if(req.hasParameter("isDropDown")) {
			data = getUnFilteredCustomers();
		} else if (Convert.formatBoolean(req.getParameter("addCustomer"))) {
			data = new ArrayList<CustomerVO>();
			data.add(new CustomerVO());
		} else if (req.hasParameter("customerId")) {
			data = new ArrayList<CustomerVO>();
			data.add(getCustomerData(Convert.formatInteger(req.getParameter("customerId"))));
		} else {
			//Gather Request Params.
			int start = Convert.formatInteger(req.getParameter("start"), 0);
			int end = Convert.formatInteger(req.getParameter("limit"), 25) + start;
			String customerTypeId = StringUtil.checkVal(req.getParameter("customerTypeId"));
			String excludeTypeId = StringUtil.checkVal(req.getParameter("excludeTypeId"));

			//Get filtered customer list for grid view.
			data = getFilteredCustomers(start, end, customerTypeId, excludeTypeId);
		}
		ModuleVO modVo = (ModuleVO) attributes.get(Constants.MODULE_DATA);

		//Only go after record count if we are doing a list.
        modVo.setDataSize((customerId > 0 ? 1 : getRecordCount()));
        modVo.setActionData(data);
        this.setAttribute(Constants.MODULE_DATA, modVo);
	}

	/**
	 * Helper method that returns the complete list of RAM_CUSTOMERS for a
	 * dropdown menu sorted by customer_nm.
	 * @return
	 */
	private List<CustomerVO> getUnFilteredCustomers() {
		List<CustomerVO> data = new ArrayList<>();

		//Build SQL Query
		StringBuilder sql = new StringBuilder(80);
		sql.append("select * from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("RAM_CUSTOMER order by CUSTOMER_NM");

		//Query for Results and add to list.
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			while(rs.next())
				data.add(new CustomerVO(rs, false));
		} catch (SQLException e) {
			log.error(e);
		}

		//Return CustomerVO Data.
		return data;
	}

	/**
	 * Helper method that retrieves Customer Data for a given customerId
	 * @param customerId
	 * @return
	 */
	public CustomerVO getCustomerData(int customerId) {
		CustomerVO c = null;
		List<CustomerLocationVO> locs = new ArrayList<CustomerLocationVO>();
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);

		//Build Query.
		StringBuilder sql = new StringBuilder(180);
		sql.append("select * from ").append(schema).append("RAM_CUSTOMER a ");
		sql.append("left outer join ").append(schema).append("ram_customer_location b ");
		sql.append("on a.customer_id = b.customer_id where a.CUSTOMER_ID = ? ");
		sql.append("order by b.LOCATION_NM");

		//Query for Results and build CustomerVO Object.
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setInt(1, customerId);
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
			c.setCustomerLocations(locs);
		} catch (SQLException e) {
			log.error(e);
		}
		return c;
	}

	/**
	 * Helper method that returns a sorted collection of Customer Information
	 * based on the search parameters of the Grid.
	 * @param req
	 * @return
	 */
	public List<CustomerVO> getFilteredCustomers(int start, int end, String customerTypeId, String excludeTypeId) {
		List<CustomerVO> data = new ArrayList<>();

		//Build the SQL Query.
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(260);
		sql.append("select * from (select ROW_NUMBER() OVER (order by CUSTOMER_NM) ");
		sql.append("as RowNum, a.* from ").append(schema).append("ram_customer a ");

		//Build Where Clause based of req Params that were passed.
		sql.append("where 1 = 1 ");
		if (customerTypeId.length() > 0) {
			sql.append("and customer_type_id = ? ");
		} else if (excludeTypeId.length() > 0) {
			sql.append("and customer_type_id != ? ");
		}

		//Limit Paginated Result Set.
		sql.append(") as paginatedResult where RowNum >= ? and RowNum < ? order by RowNum");

		log.debug("Customer retrieve SQL: " + sql.toString() + "|" + customerTypeId + " | " + excludeTypeId);
		int index = 1;

		//Build Prepared Statement and iterate the results.
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString());){
			if (customerTypeId.length() > 0) {
				ps.setString(index++, customerTypeId);
			} else if (excludeTypeId.length() > 0) {
				ps.setString(index++, excludeTypeId);
			}
			ps.setInt(index++, start);
			ps.setInt(index++, end);
			ResultSet rs = ps.executeQuery();

			//Add the CustomerVOs to the List.
			while(rs.next())
				data.add(new CustomerVO(rs, false));

		} catch (SQLException e) {
			log.error("Error retrieving RAM customer data, ", e);
		}

		return data;
	}
	
	/**
	 * Gets the count of the records
	 * @param customerId
	 * @param term
	 * @return
	 * @throws SQLException
	 */
	protected int getRecordCount() {

		StringBuilder sb = new StringBuilder(80);
		sb.append("select count(customer_id) from ");
		sb.append(getAttribute(Constants.CUSTOM_DB_SCHEMA)).append("RAM_CUSTOMER");

		int cnt = 0;
		try(PreparedStatement ps = dbConn.prepareStatement(sb.toString());) {

		//Get the count off the first row.
		ResultSet rs = ps.executeQuery();
		if(rs.next())
			cnt = rs.getInt(1);
		} catch(SQLException sqle) {
			log.error("Error retrieving customer Count", sqle);
		}

		log.debug("Count: " + cnt);
		return cnt;		
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {

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
		boolean isJson = Convert.formatBoolean(StringUtil.checkVal(req.getParameter("amid")).length() > 0);
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
