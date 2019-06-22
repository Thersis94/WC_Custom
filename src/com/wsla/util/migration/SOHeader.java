package com.wsla.util.migration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.gis.AbstractGeocoder;
import com.siliconmtn.gis.GeocodeFactory;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.MapUtil;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.constants.Constants;
import com.wsla.action.admin.WarrantyAction;
import com.wsla.data.product.ProductCategoryVO;
import com.wsla.data.product.ProductSerialNumberVO;
import com.wsla.data.product.ProductVO;
import com.wsla.data.product.ProductWarrantyVO;
import com.wsla.data.product.WarrantyVO;
import com.wsla.data.provider.ProviderType;
import com.wsla.data.provider.ProviderVO;
import com.wsla.data.ticket.TicketAssignmentVO;
import com.wsla.data.ticket.TicketAssignmentVO.TypeCode;
import com.wsla.data.ticket.TicketDataVO;
import com.wsla.data.ticket.TicketVO.UnitLocation;
import com.wsla.data.ticket.UserVO;
import com.wsla.util.migration.vo.ExtTicketVO;
import com.wsla.util.migration.vo.SOHeaderFileVO;

/****************************************************************************
 * <p><b>Title:</b> SOLineItems.java</p>
 * <p><b>Description:</b> </p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Feb 1, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class SOHeader extends AbsImporter {

	private List<SOHeaderFileVO> data = new ArrayList<>(50000);

	private static List<String> fakeSKUs = new ArrayList<>();

	private static Map<String, String> oemMap = new HashMap<>(100);

	private static Map<String, String> countryMap = new HashMap<>(100);


	static {
		oemMap.put("1ecc07d03101fe41ac107866a7995adf", "RCA1000");
		oemMap.put("PHIL1000", "PHI1000");

		fakeSKUs.add("NOSN");
		fakeSKUs.add("WSLADESC");
		fakeSKUs.add("SPECTRA TV");

		countryMap.put("CSR", "CR");
		countryMap.put("MEX", "MX");
		countryMap.put("VEN", "VE");
		countryMap.put("USA", "US");

	}

	/* (non-Javadoc)
	 * @see com.wsla.util.migration.AbstractImporter#run()
	 */
	@Override
	void run() throws Exception {
		File[] files = listFilesMatching(props.getProperty("soHeaderFile"), "(.*)SOHDR(.*)");

		for (File f : files)
			data.addAll(readFile(f, SOHeaderFileVO.class, SHEET_1));

		String sql = StringUtil.join("delete from ", schema, "wsla_ticket where historical_flg=1");
		delete(sql);

		save();

		//update all of the tickets we just created to have traceability to Southware
		setHistoricalFlg();
	}


	/**
	 * Reset the warranty_valid_flg and set the historical marker - its only set on 
	 * records imported by this script (not part of code/VOs)
	 */
	private void setHistoricalFlg() {
		String sql = StringUtil.join("update ", schema, 
				"wsla_ticket set locked_by_id=null, historical_flg=1 where locked_by_id='MIGRATION'");
		new DBProcessor(dbConn, schema).executeSQLCommand(sql);
	}


	/**
	 * Save the imported providers to the database.
	 * @param data
	 * @throws Exception 
	 */
	@Override
	protected void save() throws Exception {
		//turn the list of data into a unique list of tickets
		Map<String, ExtTicketVO> tickets= new HashMap<>(data.size());
		for (SOHeaderFileVO dataVo : data) {
			ExtTicketVO vo = transposeTicketData(dataVo, new ExtTicketVO());
			if (tickets.containsKey(vo.getTicketId())) {
				log.debug(String.format("duplicate ticket %s", vo.getTicketId()));
				//TODO this will need a hook for compiling transactional data
			}
			tickets.put(vo.getTicketId(), vo);
		}

		//transpose OEMs.  Replace value in vo.getOemId()
		bindOEMs(tickets.values());

		//tie the OEM's 800# to each ticket
		bind800Numbers(tickets.values());

		//create product serials, presume all are approved.  replace value in vo.getProductSerialId()
		bindSerialNos(tickets.values());

		//create warranties.  This populates vo.productWarrantyId() with the OEM's warrantyId
		bindWarranties(tickets.values());

		//transpose warranties into product warranties.  replaces value in vo.productWarrantyId()
		bindProductWarranties(tickets.values());

		//transpose product categories
		bindProductCategories(tickets.values());

		//create user profiles in the WC core & wsla_user table
		createUserProfiles(tickets);

		//save the tickets
		writeToDB(new ArrayList<>(tickets.values()));

		//create ticket attributes
		saveTicketAttributes(tickets.values());
	}


	/**
	 * Write certain columns to the DB as ticket_data
	 * @param values
	 */
	private void saveTicketAttributes(Collection<ExtTicketVO> tickets) {
		List<TicketDataVO> attrs = new ArrayList<>(tickets.size()*3);

		for (ExtTicketVO tkt : tickets)
			attrs.addAll(tkt.getTicketData());

		try {
			writeToDB(attrs);
		} catch (Exception e) {
			log.error("could not save ticket data", e);
		}
	}


	/**
	 * @param values
	 */
	private void bindProductCategories(Collection<ExtTicketVO> tickets) {
		Map<String, String> cats = new HashMap<>();
		String sql = StringUtil.join("select product_category_id as key, category_cd as value from ", schema, "wsla_product_category");
		DBProcessor dbp = new DBProcessor(dbConn, schema);
		MapUtil.asMap(cats, dbp.executeSelect(sql, null, new GenericVO()));

		ticketLoop:
			for (ExtTicketVO tkt : tickets) {
				if (cats.containsKey(tkt.getProductCategoryId())) //the key is already set correctly, leave it.
					continue;

				//look for it in the value column
				for (Map.Entry<String, String> entry : cats.entrySet()) {
					if (entry.getValue().equalsIgnoreCase(tkt.getProductCategoryId())) {
						tkt.setProductCategoryId(entry.getKey());
						continue ticketLoop;
					}
				}

				//still missing, add it to the DB as well as our Map
				ProductCategoryVO cat = new ProductCategoryVO();
				cat.setActiveFlag(0);
				cat.setCategoryCode(tkt.getProductCategoryId());
				cat.setProductCategoryId(tkt.getProductCategoryId());
				try {
					dbp.executeBatch(Arrays.asList(cat), true);
					cats.put(cat.getProductCategoryId(), cat.getCategoryCode());
					tkt.setProductCategoryId(cat.getProductCategoryId());
					log.debug("added category " + cat.getCategoryCode());
				} catch (Exception e ) {
					log.error("could not save category", e);
				}
			}
	}


	/**
	 * @param collection 
	 * 
	 */
	private void bindOEMs(Collection<ExtTicketVO> tickets) {
		String sql = StringUtil.join("select provider_id as key, provider_nm as value from ", 
				schema, "wsla_provider where provider_type_id='OEM'");
		DBProcessor db = new DBProcessor(dbConn, schema);
		List<GenericVO> oems = db.executeSelect(sql, null, new GenericVO());
		//add the database lookups to the hard-coded 'special' mappings already in this file
		for (GenericVO vo : oems) {
			//don't stomp ones we've predefined, they're more important
			if (!oemMap.containsKey(vo.getKey().toString()))
				oemMap.put(vo.getKey().toString(), vo.getValue().toString());
		}

		boolean isFound;
		Set<String> missing = new HashSet<>();
		for (ExtTicketVO vo : tickets) {
			if (oemMap.containsKey(vo.getOemId())) {
				//the ID is already the correct here, do nothing
				continue;
			}

			//find the key tied to this matching value
			isFound = false;
			for (Map.Entry<String, String> entry : oemMap.entrySet()) {
				if (vo.getOemId().equalsIgnoreCase(entry.getValue())) {
					vo.setOemId(entry.getKey());
					isFound = true;
					break;
				}
			}
			if (!isFound) 
				missing.add(vo.getOemId());
		}

		//if we're missing OEMs, the import script must fail
		if (!missing.isEmpty()) {
			DBProcessorStub dbp = new DBProcessorStub(dbConn, schema);
			log.fatal("MISSING OEMS:\n");
			for (String s : missing) {
				log.fatal(s);
				ProviderVO vo = new ProviderVO();
				vo.setProviderId(s);
				vo.setProviderName(s);
				vo.setProviderType(ProviderType.OEM);
				vo.setReviewFlag(1);
				try {
					//NOTE this will not insert records, only print SQL to be run manually
					dbp.insert(vo);
				} catch (Exception e) { /*don't need this exception */ }
			}
			throw new RuntimeException("missing OEMs, run these SQL statements please, perhaps slightly "+
					"mutated, but do not change the primary keys unless you're adding a mapping to this file (code)");
		}
		log.debug("all OEMs exist");
	}


	/**
	 * Attach an 800# to each ticket - take the 1st listed for the OEM
	 * @param tickets
	 */
	private void bind800Numbers(Collection<ExtTicketVO> tickets) {
		Map<String, String> oemPhones = new HashMap<>(50);
		String sql = StringUtil.join("select provider_id as key, phone_number_txt as value from ", schema, 
				"wsla_provider_phone where country_cd='MX' and active_flg=1");
		DBProcessor db = new DBProcessor(dbConn, schema);
		MapUtil.asMap(oemPhones, db.executeSelect(sql, null, new GenericVO()));

		//add the database lookups to the hard-coded 'special' mappings already in this file
		for (ExtTicketVO tkt : tickets)
			tkt.setPhoneNumber(oemPhones.get(tkt.getOemId()));

		log.info("phone numbers set");
	}


	/**
	 * Split from above for complexity reasons - creates and saves new products 
	 * and serial#s determined to be missing from the DB.  Calls back to bind afterwards (circular) to ensure all are accounted for.
	 * @param missing
	 * @param tickets
	 */
	private void createNewProducts(List<ExtTicketVO> missing, Collection<ExtTicketVO> tickets) {
		//the keys on these maps ensure we aren't inserting the same SKUs twice.  The key is the vendor's unique ID, not our GUIDs
		UUIDGenerator uuid = new UUIDGenerator();
		Map<String,ProductSerialNumberVO> newSerials = new HashMap<>(missing.size());
		Map<String,ProductVO> newProducts = new HashMap<>(missing.size());
		for (ExtTicketVO vo : missing) {
			ProductVO product = newProducts.get(vo.getOemId() + vo.getCustProductId());
			if (StringUtil.isEmpty(vo.getProductId()) && product != null) {
				//share the one we've already slated for saving with this ticket too
				vo.setProductId(product.getProductId());

			} else if (StringUtil.isEmpty(vo.getProductId())) {
				//log.debug("no productId for " + vo.getCustProductId())
				ProductVO prod = new ProductVO();
				//insert the product with a predefined pkId
				prod.setProductId(uuid.getUUID());
				prod.setProviderId(vo.getOemId());
				prod.setSetFlag(1);
				prod.setActiveFlag(1);
				prod.setCustomerProductId(vo.getCustProductId());
				prod.setProductName(vo.getCustProductId());
				prod.setDescription("CREATED BY LEGACY DATA IMPORT");
				prod.setValidatedFlag(0);
				newProducts.put(vo.getOemId() + prod.getCustomerProductId(), prod); //key here needs to be oem+product
				//give the pkId back to the ticket, so it can be used for serial# inserts
				vo.setProductId(prod.getProductId());
			}

			ProductSerialNumberVO prodser = newSerials.get(vo.getProductId() + vo.getSerialNoText());
			if (StringUtil.isEmpty(vo.getProductSerialId()) && prodser != null) {
				//share the one we've already slated for saving with this ticket too
				vo.setProductSerialId(prodser.getProductSerialId());

			} else if (StringUtil.isEmpty(vo.getProductSerialId())) {
				//log.debug("no serial# for " + vo.getSerialNoText() + " adding to product " + vo.getProductId())
				//insert the productSerial with a predefined pkId
				ProductSerialNumberVO ser = new ProductSerialNumberVO();
				ser.setProductSerialId(uuid.getUUID());
				ser.setSerialNumber(vo.getSerialNoText());
				ser.setProductId(vo.getProductId());
				ser.setValidatedFlag(0);
				newSerials.put(ser.getProductId() + ser.getSerialNumber(), ser); //key here needs to be product+sku
				//give the pkId back to the ticket, so it can be used for serial# inserts
				vo.setProductSerialId(ser.getProductSerialId());
			}
		}

		//save the new records.  Since we've given them pkIds we need to force insert here
		DBProcessor dbp = new DBProcessor(dbConn, schema);
		dbp.setGenerateExecutedSQL(log.isDebugEnabled());
		try {
			dbp.executeBatch(new ArrayList<>(newProducts.values()), true);
			log.info("added " + newProducts.size() + " new products");
			dbp.executeBatch(new ArrayList<>(newSerials.values()), true);
			log.info("added " + newSerials.size() + " new product serial#s");
		} catch (Exception e) {
			log.error("could not add new products or SKUs", e);
		}

		//recusively call this method.  Now that we've added data we should be able to marry more records.
		sleepThread(1000);

		// make a recursive callback to ensure all records exist and are accounted for
		// NOTE: endless looping will occur here if something goes wrong, watch for it.
		bindSerialNos(tickets);
	}


	/**
	 * @param values
	 */
	private void bindSerialNos(Collection<ExtTicketVO> tickets) {
		String sql = StringUtil.join("select coalesce(s.product_serial_id,'blank'||newid()) as product_serial_id, ",
				"s.serial_no_txt, p.product_id, p.provider_id, p.cust_product_id from ",
				schema, "wsla_product_master p  left join ", schema, "wsla_product_serial s ",
				"on s.product_id=p.product_id where p.set_flg=1");
		DBProcessor db = new DBProcessor(dbConn, schema);
		db.setGenerateExecutedSQL(log.isDebugEnabled());
		List<ProductSerialNumberVO> serials = db.executeSelect(sql, null, new ProductSerialNumberVO());

		//update our tickets and give them the proper serial# pkId.  If they don't exist, add them to a list to add to the DB
		boolean isFound;
		String tktProdNm;
		List<ExtTicketVO> missing = new ArrayList<>(1000);
		for (ExtTicketVO tkt : tickets) {
			isFound = false;
			tktProdNm = tkt.getCustProductId(); //this is raw EquipmentId in the file, not yet converted to a GUID
			if (!isValidSKU(tkt)) continue;

			for (ProductSerialNumberVO serial : serials) {
				//ensure the product matches
				if (!tktProdNm.equalsIgnoreCase(serial.getCustomerProductId())) continue;

				//if the products match, push the actual productId (GUID) through to the ticket - so we have it if we need to create the SKU record
				tkt.setProductId(serial.getProductId());

				//if we can align serial#s, we're done.  If not continue searching.
				if (StringUtil.checkVal(tkt.getSerialNoText()).equalsIgnoreCase(serial.getSerialNumber())) {
					tkt.setProductSerialId(serial.getProductSerialId());
					isFound = true;
					break;
				}
			}
			//no serial# found, put it to the list to add.  Note we are capturing the ticket here.
			if (!isFound)
				missing.add(tkt);
		}
		log.info("missing " + missing.size() + " skus in the database");
		if (missing.isEmpty()) return;  //done here, everyone has a productId & productSerialId

		createNewProducts(missing, tickets);
		log.info("serial #s finished");
	}


	/**
	 * Purge the SKU and return false if the SKU is empty or all zeros, or matches one of our know falsifications
	 * @param tkt
	 * @return
	 */
	private boolean isValidSKU(ExtTicketVO tkt) {
		String sn = tkt.getSerialNoText();
		if (StringUtil.isEmpty(sn) || sn.length() < 3 || sn.matches("0+") || fakeSKUs.contains(sn.toUpperCase())) {
			tkt.setProductSerialId(null);
			//if we don't have a product we can't have a productWarranty
			tkt.setProductWarrantyId(null);
			return false;
		} else {
			return true;
		}
	}


	/**
	 * Add an exisitng warranty to every ticket
	 * @param values
	 */
	private void bindWarranties(Collection<ExtTicketVO> tickets) {
		Map<String, String> warrMap = new HashMap<>(100);
		String sql = StringUtil.join("select provider_id as key, warranty_id as value from ", 
				schema, "wsla_warranty where warranty_type_cd='MANUFACTURER' ",
				"order by warranty_service_type_cd, provider_id");
		DBProcessor dbp = new DBProcessor(dbConn, schema);
		dbp.setGenerateExecutedSQL(log.isDebugEnabled());
		MapUtil.asMap(warrMap, dbp.executeSelect(sql, null, new GenericVO()));

		Set<String> newWarrs = new HashSet<>(100);
		for (ExtTicketVO tkt : tickets) {
			//warranty is based on product serial#.  no product, skip it!
			if (StringUtil.isEmpty(tkt.getProductSerialId())) continue;

			tkt.setProductWarrantyId(warrMap.get(tkt.getOemId()));
			if (StringUtil.isEmpty(tkt.getProductWarrantyId()))
				newWarrs.add(tkt.getOemId());
		}

		// Go around again, this will populate the blank tickets from the 1st round.
		// This loop should never go around more than 2x.
		if (!newWarrs.isEmpty()) {
			createWarranties(newWarrs);
			sleepThread(2000);
			bindWarranties(tickets);
		}
		log.info("warranties set");
	}


	/**
	 * Create stock warranties for the OEMs that don't have one
	 * @param newWarrs
	 */
	private void createWarranties(Set<String> newWarrs) {
		List<WarrantyVO> lst = new ArrayList<>(newWarrs.size());
		for (String oemId : newWarrs) {
			WarrantyVO vo = new WarrantyVO();
			vo.setProviderId(oemId);
			vo.setWarrantyLength(365);
			vo.setWarrantyType("MANUFACTURER");
			vo.setServiceTypeCode(WarrantyAction.ServiceTypeCode.ALL);
			vo.setDescription("Legacy-Data - created during migration");
			lst.add(vo);
		}
		log.info(String.format("Creating %d new OEM warranties", lst.size()));

		try {
			new DBProcessor(dbConn, schema).executeBatch(lst);
		} catch (Exception e) {
			log.error("could not create warranties", e);
			throw new RuntimeException(e.getMessage());
		}
	}


	/**
	 * Add a product warranty to every ticket
	 * @param values
	 */
	private void bindProductWarranties(Collection<ExtTicketVO> tickets) {
		Map<String, String> prodWarrMap = new HashMap<>(1000);
		String sql = StringUtil.join("select product_serial_id as key, product_warranty_id as value from ", 
				schema, "wsla_product_warranty");
		DBProcessor dbp = new DBProcessor(dbConn, schema);
		dbp.setGenerateExecutedSQL(log.isDebugEnabled());
		MapUtil.asMap(prodWarrMap, dbp.executeSelect(sql, null, new GenericVO()));

		Map<String, String> newProdWarrs = new HashMap<>(100);
		for (ExtTicketVO tkt : tickets) {
			//can't set a productWarranty without a product.  Skip empties
			if (StringUtil.isEmpty(tkt.getProductSerialId())) continue;

			// Replace the warrantyId with a productWarrantyId
			// If the record does exist add it (serial# + warrantyId = productWarrantyId) 
			String warrantyId = tkt.getProductWarrantyId();
			tkt.setProductWarrantyId(prodWarrMap.get(tkt.getProductSerialId()));
			if (StringUtil.isEmpty(tkt.getProductWarrantyId()))
				newProdWarrs.put(tkt.getProductSerialId(), warrantyId);
		}

		// Go around again, this will populate the blank tickets from the 1st round.
		// This loop should never go around more than 2x.
		if (!newProdWarrs.isEmpty()) {
			createProductWarranties(newProdWarrs);
			sleepThread(2000);
			bindProductWarranties(tickets);
		}
		log.info("product warranties set");
	}


	/**
	 * Create product warranties as needed based on serial#s
	 * @param newWarrs
	 */
	private void createProductWarranties(Map<String, String> newProdWarrs) {
		List<ProductWarrantyVO> lst = new ArrayList<>(newProdWarrs.size());
		for (Map.Entry<String, String> entry : newProdWarrs.entrySet()) {
			ProductWarrantyVO vo = new ProductWarrantyVO();
			vo.setProductSerialId(entry.getKey());
			vo.setWarrantyId(entry.getValue());
			lst.add(vo);
		}
		log.info(String.format("Creating %d new product warranties", lst.size()));

		try {
			new DBProcessor(dbConn, schema).executeBatch(lst);
		} catch (Exception e) {
			log.error("could not create product warranties", e);
			throw new RuntimeException(e.getMessage());
		}
	}


	/**
	 * loop the tickets and create a WC user profile for each user.
	 * Note: this will invoke geocoding
	 * @param tickets
	 */
	private void createUserProfiles(Map<String, ExtTicketVO> tickets) {
		//get a list of emails->userIds first, so we can ignore existing users
		String sql = StringUtil.join("select email_address_txt as key, user_id as value from ", 
				schema, "wsla_user order by create_dt desc"); //if dups, the earliest/inital record will be used
		Map<String, String> existingUsers = new HashMap<>(tickets.size());
		DBProcessor dbp = new DBProcessor(dbConn, schema);
		MapUtil.asMap(existingUsers, dbp.executeSelect(sql, null, new GenericVO()));

		ProfileManager pm = ProfileManagerFactory.getInstance(getAttributes());
		Map<String, UserVO> users = new HashMap<>(tickets.size());
		//build a unique set to save to the DB
		String pkId = null;
		for (ExtTicketVO vo : tickets.values()) {
			UserVO user = vo.getOriginator();
			pkId = user.getEmail();
			if (StringUtil.isEmpty(pkId)) { //ensure users with no email don't collide - we'll let ProfileManager de-duplicate these.
				pkId = vo.getTicketIdText();
			} else {
				String userId = existingUsers.get(user.getEmail());
				if (userId != null) {
					vo.setUserId(userId);
					log.debug("existing user, skipping");
					continue;
				}
			}
			//these two will tell us how to link up the originator after the user is saved or created
			user.setRoleName(pkId);
			vo.setUniqueUserId(pkId);
			users.put(pkId, user);
		}
		log.info(String.format("identified %d unique user profiles to add or further audit", users.size()));

		geocodeUserAddress(users.values());

		for (Map.Entry<String, UserVO> entry : users.entrySet()) {
			try {
				pm.updateProfile(entry.getValue().getProfile(), dbConn);
				entry.getValue().setProfileId(entry.getValue().getProfile().getProfileId());
			} catch (DatabaseException e) {
				log.error("could not save WC user profile", e);
			}
		}
		//cascade down and create the wsla_user entries from here, using the uniqueness map to prevent duplicate users
		Map<String, UserVO>  accts = createWSLAUserProfiles(users.values());

		//marry the users back the tickets.  This can't be done by reference because we de-duplicates the users list before creating profiles/users.
		for (ExtTicketVO tkt : tickets.values()) {
			tkt.setOriginator(accts.get(tkt.getUniqueUserId()));
			//make sure each ticket has a user
			if (StringUtil.isEmpty(tkt.getUserId())) {
				log.warn("ticket has no user " + tkt.getTicketIdText());
			} else {				
				//add the assigned-to Owner
				TicketAssignmentVO assg = new TicketAssignmentVO();
				assg.setTicketId(tkt.getTicketId());
				assg.setUserId(tkt.getUserId());
				assg.setTypeCode(TypeCode.CALLER);
				assg.setOwnerFlag(1);
				tkt.addAssignment(assg);
			}
		}
	}


	/**
	 * Geocode ONLY the users zip code, which should equate to a local DB lookup
	 * Setting the cass flag on the user's location will prevent ProfileManager from 
	 * re-geocoding the address (which would do the full address, not just zip). 
	 * @param values
	 */
	private void geocodeUserAddress(Collection<UserVO> users) {
		String geocodeUrl = props.getProperty(Constants.GEOCODE_URL);
		String geocodeClass = props.getProperty(Constants.GEOCODE_CLASS);

		AbstractGeocoder ag = GeocodeFactory.getInstance(geocodeClass);
		ag.addAttribute(AbstractGeocoder.CONNECT_URL, geocodeUrl);
		ag.addAttribute(AbstractGeocoder.CASS_VALIDATE_FLG, false);
		ag.addAttribute(AbstractGeocoder.BOT_REQUEST, true);

		for (UserVO user : users) {
			GeocodeLocation userLoc = user.getProfile().getLocation();
			userLoc.setCassValidated(Boolean.TRUE); //set note in method comment

			GeocodeLocation tmpLoc = new GeocodeLocation();
			tmpLoc.setCountry(countryMap.get(userLoc.getCountry()));
			tmpLoc.setZipCode(userLoc.getZipCode());
			if (StringUtil.isEmpty(tmpLoc.getCountry()) || tmpLoc.getCountry().length() > 2)
				tmpLoc.setCountry("MX");

			List<GeocodeLocation> geos = ag.geocodeLocation(tmpLoc);
			GeocodeLocation geoLoc = (geos != null && !geos.isEmpty()) ? geos.get(0) : null;
			if (geoLoc == null) continue;

			//transpose the lat/long over to the user's location - discard the rest
			userLoc.setLatitude(geoLoc.getLatitude());
			userLoc.setLongitude(geoLoc.getLongitude());
			log.debug(String.format("country %s is now %s", userLoc.getCountry(), geoLoc.getCountry()));
			userLoc.setCountry(geoLoc.getCountry()); //replace the 3-char from Southware with an ISO code
			userLoc.setMatchCode(geoLoc.getMatchCode());
		}
	}


	/**
	 * Save the users to the wsla_member table, then save that pkId as the ticket's originator_user_id
	 * @param users
	 */
	private Map<String, UserVO> createWSLAUserProfiles(Collection<UserVO> users) {
		DBProcessor dbp = new DBProcessor(dbConn, schema);
		dbp.setGenerateExecutedSQL(log.isDebugEnabled());
		//we need to first check if these users exist in the table, use profile_id for this
		String sql = StringUtil.join("select profile_id as key, user_id as value from ", schema, "wsla_user order by create_dt desc"); //if dups, the earliest/inital record will be used
		Map<String, String> userMap = new HashMap<>(users.size());
		MapUtil.asMap(userMap, dbp.executeSelect(sql, null, new GenericVO()));

		Map<String, UserVO> userVoMap = new HashMap<>(users.size());
		for (UserVO user : users) {
			try {
				user.setUserId(userMap.get(user.getProfileId())); //setting here updates exising records, rather than creating dups
				dbp.save(user);
				userVoMap.put(user.getRoleName(), user);
			} catch (Exception e) {
				log.error("could not save WSLA user profile", e);
			}
		}
		return userVoMap;
	}


	/**
	 * Transpose and enhance the data we get from the import file into what the new schema needs
	 * @param dataVo
	 * @param ticketVO
	 * @return
	 */
	private ExtTicketVO transposeTicketData(SOHeaderFileVO dataVo, ExtTicketVO vo) {
		vo.setTicketId(dataVo.getSoNumber());
		vo.setTicketIdText(dataVo.getSoNumber());
		vo.setCreateDate(dataVo.getReceivedDate());
		vo.setUpdateDate(dataVo.getAltKeyDate());
		//vo.setStandingCode(Standing.GOOD);  not needed, this is the default
		vo.setUnitLocation(UnitLocation.CALLER);
		vo.setOemId(dataVo.getManufacturer());
		vo.setCustProductId(dataVo.getEquipmentId());
		vo.setSerialNoText(dataVo.getSerialNumber());
		vo.setStatusCode(dataVo.getStatusCode()); //there's a switch nested in here doing transposition
		vo.setOriginator(createOriginator(dataVo));
		vo.setPhoneNumber(dataVo.getCustPhone());
		vo.setWarrantyValidFlag("CNG".equalsIgnoreCase(dataVo.getCoverageCode()) ? 1 : 0); //this is the only code that results in warranty coverage
		vo.setProductCategoryId(dataVo.getProductCategory());

		//add attribute for Received Method
		TicketDataVO attr;
		if (!StringUtil.isEmpty(dataVo.getReceivedMethod())) {
			attr = new TicketDataVO();
			attr.setTicketId(vo.getTicketId());
			attr.setAttributeCode("attr_order_origin");
			attr.setValue(dataVo.getReceivedMethod()); //there's a switch nested in here doing transposition
			vo.addTicketData(attr);
		}

		//problem/symptoms of issue
		if (!StringUtil.isEmpty(dataVo.getProblemCode())) {
			attr = new TicketDataVO();
			attr.setTicketId(vo.getTicketId());
			attr.setAttributeCode("attr_symptomsComments");
			attr.setValue(dataVo.getProblemCode());
			vo.addTicketData(attr);
		}

		//add attribute for Customer PO
		if (!StringUtil.isEmpty(dataVo.getCustPO())) {
			attr = new TicketDataVO();
			attr.setTicketId(vo.getTicketId());
			attr.setAttributeCode("attr_customer_po");
			attr.setValue(dataVo.getCustPO());
			vo.addTicketData(attr);
		}

		//add attribute for Coverage Code
		if (!StringUtil.isEmpty(dataVo.getCoverageCode())) {
			attr = new TicketDataVO();
			attr.setTicketId(vo.getTicketId());
			attr.setAttributeCode("attr_dispositionCode");
			attr.setValue(dataVo.getCoverageCode());
			vo.addTicketData(attr);
		}

		//add attribute for Action Code 
		//TODO Steve to add to HDR file first
		//		if (!StringUtil.isEmpty(dataVo.getActionCode())) {
		//			attr = new TicketDataVO();
		//			attr.setTicketId(vo.getTicketId());
		//			attr.setAttributeCode("attr_issueResolved");
		//			attr.setValue("defect-" + dataVo.getActionCode());
		//			vo.addTicketData(attr);
		//		}

		//add the assigned-to CAS
		if (!StringUtil.isEmpty(dataVo.getServiceTech())) {
			TicketAssignmentVO assg = new TicketAssignmentVO();
			assg.setTicketId(vo.getTicketId());
			assg.setLocationId(dataVo.getServiceTech());
			assg.setTypeCode(TypeCode.CAS);
			vo.addAssignment(assg);
		}

		//This is a temporary flag to mark they records we're adding to the DB.
		//We'll correct it when we set the historical bit at the end of the import.
		vo.setLockedBy("MIGRATION");

		return vo;
	}


	/**
	 * Create the owner  
	 * We'll need to add them (as a User), then save THAT guid with the ticket as originator_id
	 * @param dataVo
	 * @return
	 */
	private UserVO createOriginator(SOHeaderFileVO dataVo) {
		UserVO wslaUser = new UserVO();
		UserDataVO user = new UserDataVO();
		String fullName = StringUtil.checkVal(dataVo.getCustName());
		String ctctName = StringUtil.checkVal(dataVo.getCustContact());
		if (fullName.length() > ctctName.length() && !fullName.matches("(?i:.*BODEGA AURRERA.*)")) {
			//log.debug(String.format("Name %s is longer than Contact %s.  Using name column.", fullName, ctctName))
			ctctName = fullName;
		}

		String[] nm = ctctName.split(" ");
		String firstNm = "";
		String lastNm = "";

		if (nm.length > 3) {
			for (int x=0; x < nm.length; x++) {
				if (x < 2) firstNm += " " + nm[x]; //first two words go in first name
				else  lastNm = " " + nm[x]; //remaining words go in last name
			}
		} else if (nm.length == 3) {
			firstNm = nm[0];
			lastNm = nm[1] + " " + nm[2];
		} else if (nm.length == 2) {
			firstNm = nm[0];
			lastNm = nm[1];
		} else if (nm.length == 1) {
			firstNm = nm[0];
			lastNm = nm[0];
		}
		user.setEmailAddress(dataVo.getEmailAddress());
		user.setFirstName(firstNm.trim());
		user.setLastName(lastNm.trim());
		user.setMainPhone(dataVo.getCustPhone());
		user.setAddress(dataVo.getCustAddress1());
		user.setAddress2(dataVo.getCustAddress2());
		user.setCity(dataVo.getCustCity());
		user.setState(dataVo.getCustState());
		user.setZipCode(dataVo.getCustZip());
		user.setCountryCode(dataVo.getCustCountry());

		wslaUser.setFirstName(user.getFirstName());
		wslaUser.setLastName(user.getLastName());
		wslaUser.setEmail(user.getEmailAddress());
		wslaUser.setMainPhone(user.getMainPhone());
		wslaUser.setActiveFlag(1);

		wslaUser.setProfile(user);
		return wslaUser;
	}


	/**
	 * Put the thread to sleep.  We do this between writes and reads, to give 
	 * the DB time to commit.
	 * @param i
	 */
	private void sleepThread(int durationMillis) {
		try {
			Thread.sleep(durationMillis);
		} catch (Exception e ) {
			log.fatal("could not sleep thread", e);
		}
	}
}
