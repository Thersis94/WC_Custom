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
import com.siliconmtn.barcode.BarcodeManager;
import com.siliconmtn.barcode.BarcodeOEM;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/****************************************************************************
 * <b>Title</b>: VSBarcodeLookupAction.java
 * <p/>
 * <b>Project</b>: WC_Custom
 * <p/>
 * <b>Description: </b> TODO
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author raptor
 * @version 1.0
 * @since Jun 25, 2015
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public class VSBarcodeLookupAction extends SBActionAdapter {

	public static String CUSTOMER_CACHE_KEY = "RAM_CUSTOMER_OEM_LIST";
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

	/**
	 * Creates a list of GTIN and HIBC codes for the barcode lookup
	 */
	protected List<BarcodeOEM> oems = null;

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(SMTServletRequest req) throws ActionException {

		SBUserRole r = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);

		oems = getOems(r);

		// Get the barcode data
		Set<String> scans = new HashSet<String>();
		scans.add(req.getParameter("barcode"));
		scans.add(req.getParameter("barcode2"));

		RAMProductVO product = null;
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
		List<BarcodeOEM> results = new ArrayList<BarcodeOEM>();

		//Retrieve entire map of scannable customers.
		Map<String, BarcodeOEM> barcodes = loadOems();

		/*
		 * If this customer is already bound to a center, 
		 */
		if(r != null && r.hasRoleAttribute("roleAttributeKey_1")) {
			results.add(barcodes.get(r.getAttribute("roleAttributeKey_1")));
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
		try(PreparedStatement ps = dbConn.prepareStatement(getProductSql(barcode.getBarcodeType()))) {
			ps.setString(1, barcode.getCustomerId());
			ps.setString(2, barcode.getProductId());
	
			ResultSet rs = ps.executeQuery();

			if(rs.next()) {
				p = new RAMProductVO(rs);
				p.setLotNumber(barcode.getLotCodeNumber());
			}
		} catch (SQLException e) {
			log.error(e);
		}
		return p;
	}

	private String getProductSql(BarcodeType type) {
		StringBuilder sql = new StringBuilder();
		sql.append("select p.*, p.GTIN_PRODUCT_ID as GTIN_NUMBER_TXT, c.CUSTOMER_NM from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("RAM_PRODUCT p ");
		sql.append("LEFT JOIN ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("RAM_CUSTOMER c on p.CUSTOMER_ID = p.CUSTOMER_ID ");
		sql.append("where p.CUSTOMER_ID = ? ");
		if(type.equals(BarcodeType.GTIN)) {
			sql.append("and GTIN_PRODUCT_ID = ?");
		} else {
			sql.append("and CUST_PRODUCT_ID = ?");
		}

		return sql.toString();
	}

	public void list(SMTServletRequest req) throws ActionException {
		super.list(req);
	}
}