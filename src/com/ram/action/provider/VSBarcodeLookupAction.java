/**
 *
 */
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

import com.ram.datafeed.data.RAMProductVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.barcode.BarcodeItemVO;
import com.siliconmtn.barcode.BarcodeItemVO.BarcodeType;
import com.siliconmtn.security.AbstractRoleModule;
import com.siliconmtn.barcode.BarcodeManager;
import com.siliconmtn.barcode.BarcodeOEM;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/****************************************************************************
 * <b>Title</b>: VSBarcodeLookupAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Class that manages taking a Barcode input from the RAM
 * VisionSystem scanner lookup, decoding it, and performing a lookup for a
 * product.  If the product exists then we return information related to the
 * scan.
 * <b>Copyright:</b> Copyright (c) 2015
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author raptor
 * @version 1.0
 * @since Jun 25, 2015
 *        <b>Changes: </b>
 ****************************************************************************/
public class VSBarcodeLookupAction extends SBActionAdapter {

	public static final String CUSTOMER_CACHE_KEY = "RAM_CUSTOMER_OEM_LIST";

	/**
	 * Creates a list of GTIN and HIBC codes for the barcode lookup
	 */
	protected List<BarcodeOEM> oems = null;

	/**
	 * 
	 */
	public VSBarcodeLookupAction() {
	}

	/**
	 * @param actionInit
	 */
	public VSBarcodeLookupAction(ActionInitVO actionInit) {
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

		RAMProductVO product = null;
		String errorMsg = null;
		try {
			// Parse the barcodes
			BarcodeManager bcm = new BarcodeManager(oems);
			BarcodeItemVO barcode = bcm.parseBarCode(scans);
			log.debug("barcode: " + barcode);

			// Call the SOLR Query to populate
			if (barcode == null) throw new Exception("Invalid Barcode Recieved");

			product = this.retrieveProduct(barcode);
			log.debug("Product: " + product);
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
		List<BarcodeOEM> results = new ArrayList<BarcodeOEM>();

		//Retrieve entire map of scannable customers.
		Map<String, BarcodeOEM> barcodes = loadOems();

		/*
		 * If this customer is already bound to a center, 
		 */
		if(r != null && r.hasRoleAttribute(AbstractRoleModule.ATTRIBUTE_KEY_1)) {
			results.add(barcodes.get(r.getAttribute(AbstractRoleModule.ATTRIBUTE_KEY_1)));
		} else {
			results.addAll(barcodes.values());
		}

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
		Map<String, BarcodeOEM> barcodes = new HashMap<String, BarcodeOEM>();

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
	protected RAMProductVO retrieveProduct(BarcodeItemVO barcode) throws ActionException {
		RAMProductVO p = null;
		log.debug("Performing lookup on productId: " + barcode.getProductId());
		try(PreparedStatement ps = dbConn.prepareStatement(getProductSql(barcode))) {
			ps.setString(1,barcode.getVendorCode());
			ps.setString(2, barcode.getProductId());

			ResultSet rs = ps.executeQuery();

			if(rs.next()) {
				p = new RAMProductVO(rs);
				p.setLotNumber(barcode.getLotCodeNumber());
				p.setExpiree(barcode.getExpirationDate());
				p.setSerialNumber(barcode.getSerialNumber());
				
				// If the product is a kit and the serial number is present, get the item master id
				if (p.getKitFlag() == 1 && ! StringUtil.isEmpty(barcode.getSerialNumber())) {
					getKitItemMasterId(p);
				}
			}
		} catch (SQLException e) {
			log.error(e);
		}
		return p;
	}
	
	/**
	 * Retrieves the location item master id for a given kit
	 * @param prod
	 * @return
	 */
	protected void getKitItemMasterId(RAMProductVO prod) {
		// Build the SQL Statement
		StringBuilder sql = new StringBuilder();
		sql.append("select location_item_master_id as key ");
		sql.append(DBUtil.FROM_CLAUSE).append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("ram_location_item_master ");
		sql.append(DBUtil.WHERE_CLAUSE).append("product_id = ? and serial_no_txt = ? ");
		log.debug("Getting kit: " + sql + "|" + prod.getProductId() + "|" + prod.getSerialNumber());
		
		// Add the params
		List<Object> params = new ArrayList<>();
		params.add(prod.getProductId());
		params.add(prod.getSerialNumber());
		
		//Get the data
		DBProcessor db = new DBProcessor(getDBConnection(), Constants.CUSTOM_DB_SCHEMA + "");
		List<Object> res = db.executeSelect(sql.toString(), params, new GenericVO());
		
		// assign the id
		if (! res.isEmpty()) prod.setLocationItemMasterId(((GenericVO)res.get(0)).getKey() + "");
	}
	
	/**
	 * Retrieves a scanned product
	 * @param bc
	 * @return
	 */
	private String getProductSql(BarcodeItemVO bc) {
		StringBuilder sql = new StringBuilder(300);
		String custom = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("select p.*, customer_code_value as gtin_number_txt, c.customer_nm ").append(DBUtil.FROM_CLAUSE).append(custom);
		sql.append("ram_product p ");
		sql.append(DBUtil.INNER_JOIN).append(custom);
		sql.append("ram_customer c on c.customer_id = p.customer_id ");
		sql.append(DBUtil.INNER_JOIN).append(custom).append("ram_customer_code cc ");
		sql.append("on c.customer_id = cc.customer_id and cc.customer_code_value = ? ");
		sql.append(DBUtil.WHERE_1_CLAUSE);
		if(BarcodeType.GTIN.equals(bc.getBarcodeType())) {
			sql.append("and gtin_product_id = ?");
		} else {
			sql.append("and cust_product_id = ?");
		}

		log.debug(sql.toString() + "|" + bc.getVendorCode() + "|" + bc.getProductId());
		return sql.toString();
	}
}