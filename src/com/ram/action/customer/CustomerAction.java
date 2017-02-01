package com.ram.action.customer;

// JDK 7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ram.action.data.RAMCustomerSearchVO;
// RAMDataFeed
import com.ram.datafeed.data.CustomerLocationVO;
import com.ram.datafeed.data.CustomerVO;
// SMTBaseLibs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
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
			data = new ArrayList<CustomerVO>();
			data.add(new CustomerVO());
		} else if (svo.getCustomerId() > 0) {
			data = new ArrayList<CustomerVO>();
			data.add(getCustomerData(svo));
		} else {
			//Get filtered customer list for grid view.
			data = getCustomers(svo);
		}
		ModuleVO modVo = (ModuleVO) attributes.get(Constants.MODULE_DATA);

		//Only go after record count if we are doing a list.
        modVo.setDataSize((svo.getCustomerId() > 0 ? 1 : getRecordCount(svo)));
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
	public List<CustomerVO> getCustomers(RAMCustomerSearchVO svo) {
		List<CustomerVO> data = new ArrayList<>();
		int index = 1;

		//Build Prepared Statement and iterate the results.
		try(PreparedStatement ps = dbConn.prepareStatement(getCustomerLookupQuery(svo));){
			if (svo.getCustomerTypeId().length() > 0) {
				ps.setString(index++, svo.getCustomerTypeId());
			} else if (svo.getExcludeTypeId().length() > 0) {
				ps.setString(index++, svo.getExcludeTypeId());
			}

			//Add Kit Flag Value.
			if(svo.isKitsOnly()) {
				ps.setInt(index++, 1);
			}

			//Add Paginations.
			if(svo.isPaginated()) {
				ps.setInt(index++, svo.getStart());
				ps.setInt(index++, svo.getLimit());
			}
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
	protected int getRecordCount(RAMCustomerSearchVO svo) {
		svo.setCount(true);
		svo.setPaginated(false);
		List<CustomerVO> customers = getCustomers(svo);

		log.debug("Count: " + customers.size());
		return customers.size();
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
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

	public String getCustomerLookupQuery(RAMCustomerSearchVO svo) {
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(260);
		if(svo.isPaginated()) {
			sql.append("select * from (");
		}
		sql.append("select ROW_NUMBER() OVER (order by CUSTOMER_NM) ");
		sql.append("as RowNum, a.* from ").append(schema).append("ram_customer a ");

		//Add Where Clause
		sql.append(getCustomerLookupWhereClause(svo));

		//Limit Paginated Result Set.
		if(svo.isPaginated()) {
			sql.append(") as paginatedResult where RowNum >= ? and RowNum < ? order by RowNum");
		}

		log.debug("Customer retrieve SQL: " + sql.toString() + "|" + svo.getCustomerTypeId() + " | " + svo.getExcludeTypeId());

		return sql.toString();
	}

	public String getCustomerLookupWhereClause(RAMCustomerSearchVO svo) {
		StringBuilder sql = new StringBuilder(150);

		//Build Where Clause based of req Params that were passed.
		sql.append("where 1 = 1 ");

		if (svo.getCustomerTypeId().length() > 0) {
			sql.append("and customer_type_id = ? ");
		} else if (svo.getExcludeTypeId().length() > 0) {
			sql.append("and customer_type_id != ? ");
		}

		if(svo.isKitsOnly()) {
			sql.append("and customer_id in (select distinct CUSTOMER_ID from ");
			sql.append(getAttribute(Constants.CUSTOM_DB_SCHEMA)).append("RAM_PRODUCT ");
			sql.append("where KIT_FLG = ?) ");
		}
		return sql.toString();
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
