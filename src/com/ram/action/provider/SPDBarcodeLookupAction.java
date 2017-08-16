package com.ram.action.provider;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ram.action.or.vo.SPDRAMProductVO;
import com.ram.action.or.vo.RAMCaseVO.RAMCaseStatus;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.barcode.BarcodeItemVO;
import com.siliconmtn.barcode.BarcodeItemVO.BarcodeType;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.barcode.BarcodeManager;
import com.siliconmtn.barcode.BarcodeOEM;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/****************************************************************************
 * <b>Title:</b> SPDBarcodeLookupAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Class that manages taking a Barcode input from the RAM
 * SPD scanner lookup, decoding it, and performing a lookup for a
 * product.  If the product exists and is part of then we return information related to the
 * scan.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Jul 9, 2017
 ****************************************************************************/
public class SPDBarcodeLookupAction extends SimpleActionAdapter {

	public static final String CUSTOMER_CACHE_KEY = "RAM_CUSTOMER_OEM_LIST";

	/**
	 * Creates a list of GTIN and HIBC codes for the barcode lookup
	 */
	protected List<BarcodeOEM> oems = null;

	/**
	 * 
	 */
	public SPDBarcodeLookupAction() {
	}

	/**
	 * @param actionInit
	 */
	public SPDBarcodeLookupAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {

		SBUserRole r = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);

		oems = getOems(r);

		// Get the barcode data
		Set<String> scans = new HashSet<>();
		scans.add(req.getParameter("barcode"));
		scans.add(req.getParameter("barcode2"));

		SPDRAMProductVO product = null;
		String errorMsg = null;
		try {
			// Parse the barcodes
			BarcodeManager bcm = new BarcodeManager(oems);
			BarcodeItemVO barcode = bcm.parseBarCode(scans);
			log.info("barcode: " + barcode);

			// Call the SOLR Query to populate
			if (barcode == null) throw new Exception("Invalid Barcode Recieved");

			product = this.retrieveProduct(barcode);
		} catch(Exception e) {
			errorMsg = e.getLocalizedMessage();
		}

		// Add the data to the collection for return to the view
		this.putModuleData(product, 1, false, errorMsg, errorMsg == null ? false: true);
	}

	/**
	 * Helper method that returns a list of OEM's that are valid for this user.
	 * @param r 
	 * @return
	 * @throws ActionException 
	 */
	private List<BarcodeOEM> getOems(SBUserRole r) throws ActionException {
		List<BarcodeOEM> results = new ArrayList<>();

		//Retrieve entire map of scannable customers.
		Map<String, BarcodeOEM> barcodes = loadOems();

		results.addAll(barcodes.values());

		return results;
	}

	/**
	 * Helper method that manages the Cache Layer.  Attempts to read Customer
	 * Map out of cache and if it's not present, we create it and write it.  If
	 * there are any problems, an Exception is thrown so we don't have to worry
	 * about empty cache objects.
	 * @return
	 * @throws ActionException
	 */
	@SuppressWarnings("unchecked")
	private Map<String, BarcodeOEM> loadOems() throws ActionException {

		//Try to retrieve off cache.
		ModuleVO mod = readCustomersFromCache();

		//If not on cache, go to database.
		if(mod == null) {
			mod = buildCustomerMap();

			//Write result to cache for next time.
			super.writeToCache(mod);
		}

		//Return Map from mod data.
		return (Map<String, BarcodeOEM>)mod.getActionData();
	}

	/**
	 * Helper method that builds the Map of BarcodeOEM Vos from the database
	 * and stores it on a ModuleVO ready for caching.
	 * @return
	 * @throws ActionException
	 */
	private ModuleVO buildCustomerMap() throws ActionException {

		//Create Map for Barcodes.
		Map<String, BarcodeOEM> barcodes = new HashMap<>();

		//Create ModuleVO for caching.
		ModuleVO mod = new ModuleVO();
		mod.setPageModuleId(CUSTOMER_CACHE_KEY);
		mod.setCacheable(true);
		mod.setActionId(actionInit.getActionId());

		//Query DB for OEM Customers.
		try (PreparedStatement ps = dbConn.prepareStatement(getCustomerLookupSql())) {
			ps.setString(1, "OEM");
			ps.setInt(2, 1);

			ResultSet rs = ps.executeQuery();
			BarcodeOEM oem;
			if(rs.next()) {
				oem = new BarcodeOEM(rs);

				while(rs.next()) {
					if(!rs.getString("CUSTOMER_ID").equals(oem.getCustomerId())) {
						barcodes.put(oem.getCustomerId(), oem);
						oem = new BarcodeOEM(rs);		
					} else {
						oem.addCustomerCode(rs.getString("CUSTOMER_CODE_VALUE"), BarcodeType.valueOf(rs.getString("CODE_TYPE")));
					}
				}
				barcodes.put(oem.getCustomerId(), oem);
			}

			//Set Map on the ModuleVO and update Size.
			mod.setActionData(barcodes);
			mod.setDataSize(barcodes.size());

			return mod;
		} catch (SQLException e) {
			log.error(e);
			throw new ActionException("Customer List could not be retrieved");
		}
	}

	/**
	 * Helper method that builds the SQL Query to retrieve all Customer Records
	 * @return
	 */
	private String getCustomerLookupSql() {
		StringBuilder sql = new StringBuilder(1000);

		sql.append("select a.CUSTOMER_ID, a.ORGANIZATION_ID, a.CUSTOMER_TYPE_ID, ");
		sql.append("a.CUSTOMER_NM, b.CUSTOMER_CODE_VALUE, a.ACTIVE_FLG, b.CODE_TYPE ");
		sql.append("from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)); 
		sql.append("RAM_CUSTOMER a ");
		sql.append("inner join ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("RAM_CUSTOMER_CODE b ");
		sql.append("on a.CUSTOMER_ID = b.CUSTOMER_ID ");
		sql.append("where a.customer_type_id = ? and a.ACTIVE_FLG = ? ");
		sql.append("order by CUSTOMER_ID");

		return sql.toString();
	}

	/**
	 * @return
	 */
	private ModuleVO readCustomersFromCache() {
		return super.readFromCache(CUSTOMER_CACHE_KEY);
	}

	/**
	 * Retrieves the product information for the provided barcode
	 * @param barcode
	 * @return
	 * @throws ActionException 
	 */
	protected SPDRAMProductVO retrieveProduct(BarcodeItemVO barcode) {
		SPDRAMProductVO p = null;
		String sql = getProductSql(barcode.getBarcodeType());
		log.debug(sql + "|" + barcode.getCustomerId() + "|" + barcode.getProductId());
		try(PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setInt(1, Convert.formatInteger(barcode.getCustomerId()));
			ps.setString(2, barcode.getProductId());
			ps.setString(3, barcode.getBarcodeType().toString());
			ResultSet rs = ps.executeQuery();

			if(rs.next()) {
				p = new SPDRAMProductVO(rs);
				p.setLotNumber(barcode.getLotCodeNumber());
				p.setSerialNumber(barcode.getSerialNumber());

				if(p.getKitFlag() == 1) {
					performCaseLookup(p);
				}
			}
		} catch (SQLException e) {
			log.error(e);
		}
		return p;
	}

	/**
	 * @param p
	 */
	private void performCaseLookup(SPDRAMProductVO p) {
		try(PreparedStatement ps = dbConn.prepareStatement(getCaseLookupSql())) {
			ps.setInt(1, p.getProductId());
			ps.setString(2, RAMCaseStatus.OR_COMPLETE.toString());
			ps.setInt(3, 0);
			ps.setString(4, p.getSerialNumber().toLowerCase());

			ResultSet rs = ps.executeQuery();

			if(rs.next()) {
				p.setCaseId(rs.getString("case_id"));
				p.setCaseKitId(rs.getString("case_kit_id"));
			}
		} catch (SQLException e) {
			log.error("Error Processing Code", e);
		}
	}

	/**
	 * 
	 * @return
	 */
	private String getCaseLookupSql() {
		StringBuilder sql = new StringBuilder(400);
		String custom = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("select c.case_id, k.case_kit_id from ");
		sql.append(custom).append("ram_case c ");
		sql.append("inner join ").append(custom).append("ram_case_kit k ");
		sql.append("on c.case_id = k.case_id ");
		sql.append("inner join ").append(custom).append("ram_location_item_master i ");
		sql.append("on k.location_item_master_id = i.location_item_master_id ");
		sql.append("where i.product_id = ? and c.case_status_cd = ? ");
		sql.append("and processed_flg = ? and lower(i.serial_no_txt) = ?");
		return sql.toString();
	}

	private String getProductSql(BarcodeType type) {
		StringBuilder sql = new StringBuilder(300);
		String custom = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("select p.*, p.gtin_product_id as gtin_number_txt, c.customer_nm ").append(DBUtil.FROM_CLAUSE).append(custom);
		sql.append("ram_product p ");
		sql.append(DBUtil.INNER_JOIN).append(custom);
		sql.append("ram_customer c on c.customer_id = p.customer_id ");
		sql.append(DBUtil.INNER_JOIN).append(custom).append("ram_customer_code cc ");
		sql.append("on c.customer_id = cc.customer_id and c.customer_id = ? ");
		sql.append(DBUtil.WHERE_1_CLAUSE);
		if(type.equals(BarcodeType.GTIN)) {
			sql.append("and gtin_product_id = ? and cc.code_type = ?");
		} else {
			sql.append("and cust_product_id = ? and cc.code_type = ?");
		}

		return sql.toString();
	}

	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}
}