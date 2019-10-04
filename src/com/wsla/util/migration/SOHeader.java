package com.wsla.util.migration;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.gis.AbstractGeocoder;
import com.siliconmtn.gis.GeocodeFactory;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.MapUtil;
import com.siliconmtn.util.StringUtil;
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
import com.wsla.data.ticket.DiagnosticRunVO;
import com.wsla.data.ticket.DiagnosticTicketVO;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.TicketAssignmentVO;
import com.wsla.data.ticket.TicketAssignmentVO.TypeCode;
import com.wsla.data.ticket.TicketDataVO;
import com.wsla.data.ticket.TicketLedgerVO;
import com.wsla.data.ticket.TicketScheduleVO;
import com.wsla.data.ticket.TicketVO.Standing;
import com.wsla.data.ticket.TicketVO.UnitLocation;
import com.wsla.data.ticket.UserVO;
import com.wsla.util.migration.vo.ExtTicketVO;
import com.wsla.util.migration.vo.SOHDRFileVO;

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

	public static final String LEGACY_USER_ID = "WSLA_MH-NC"; // Mariana Hernandez at WSLA

	public static final String LEGACY_PARTS_LOCN = "WSLA_020"; //WSLA Bodega External "external warehouse" - default parts location for shipments to CAS

	private List<SOHDRFileVO> data = new ArrayList<>(50000);
	
	private static Map<String, String> ticketMap = new HashMap<>(10000, 1);

	private static List<String> fakeSKUs = new ArrayList<>();

	private static Map<String, String> oemMap = new HashMap<>(100);

	private static Map<String, String> countryMap = new HashMap<>(100);

	private Map<String, String> defectMap;

	private Map<String, String> casLocations = new HashMap<>(2000);

	/**
	 * the cas diags we're going to presume got run (by the cas, on the unit)
	 */
	private static final String[] CAS_DIAGS = { "POWER_ON", "FIRMWARE", "SCREEN_DISPLAY", "INTERNET", "TV_AUDIO", "TV_OPERATION" };

	private static final String LEGACY_SW_SIGNATURE = "data:image/png;base64,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";

	private static final String LEGACY_DIAG_CD = "LEGACY_SW";
	private static final String LEGACY_DIAG_NM = LEGACY_DIAG_CD.replace('_', ' ');


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
			data.addAll(readFile(f, SOHDRFileVO.class, SHEET_1));

		defectMap = loadDefectCodes(db, schema);
		loadTicketIds();
		loadCasLocations();

		save();
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
		for (SOHDRFileVO dataVo : data) {
			if (!isImportable(dataVo.getSoNumber())) {
				continue; //discard refund/replace/harvest and other edge-cases (internal tickets)
			} else if (ticketMap.containsKey(dataVo.getSoNumber())) {
				throw new RuntimeException("ticket " + dataVo.getSoNumber() + " already exists, delete it first");
			}

			ExtTicketVO vo = transposeTicketData(dataVo, new ExtTicketVO());
			if (tickets.containsKey(vo.getTicketId())) {
				log.debug(String.format("duplicate ticket %s", vo.getTicketId()));
				// this will need a hook for compiling transactional data
			} else if (!isClosedTktRun || (isClosedTktRun && vo.getClosedDate() != null)) {
				tickets.put(vo.getTicketId(), vo);
			}
		}

		// comment 'delete' (line 98) if you're not running all of these!

		//transpose OEMs.  Replace value in vo.getOemId()
		bindOEMs(tickets.values());

		//tie the OEM's 800# to each ticket
		bind800Numbers(tickets.values());

		//create product serials, presume all are approved.  replace value in vo.getProductSerialId()
		bindSerialNos(tickets.values(), false);

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

		//create ticket assignments (caller, retailer, cas)
		createTicketAssignments(tickets.values());

		//create schedules for tickets assigned to cas locations
		if (!isOpenTktRun)
			createSchedules(tickets.values());

		//create a binding to diagnostics
		if (!isOpenTktRun)
			createDiagnosticRun(tickets.values());

		//create initial open and closed (conditionally) ledger entries
		createLedgerEntries(tickets.values());

		//update ticket status
		updateTicketStatus(tickets.values());
	}


	/**
	 * update the ticket table with the correct status - this often change between 
	 * the initial insert and final state 
	 * @param values
	 */
	private void updateTicketStatus(Collection<ExtTicketVO> tickets) {
		if (tickets == null || tickets.isEmpty()) return;
		String sql = StringUtil.join(DBUtil.UPDATE_CLAUSE, schema, "wsla_ticket set status_cd=? where ticket_id=?");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			for (ExtTicketVO vo : tickets) {
				ps.setString(1, vo.getStatusCode() != null ? vo.getStatusCode().name() : null);
				ps.setString(2, vo.getTicketId());
				ps.addBatch();
			}
			int[] cnt = ps.executeBatch();
			log.info(String.format("updated %d tickets with new status values", cnt.length));

		} catch (SQLException sqle) {
			log.error("could not update ticket statuses", sqle);
		}
	}


	/**
	 * Create an Opened and Closed (if closed) ledger entries
	 * @param values
	 * @throws Exception 
	 */
	private void createLedgerEntries(Collection<ExtTicketVO> tickets) throws Exception {
		Calendar startCal = Calendar.getInstance();
		TicketLedgerVO vo;
		StatusCode finalTicketStatus;
		List<TicketLedgerVO> entries = new ArrayList<>(tickets.size()*4);
		for (ExtTicketVO tkt : tickets) {
			startCal.setTime(tkt.getCreateDate());

			// ticket opened - midnight (00:00)
			vo = new TicketLedgerVO();
			vo.setLedgerEntryId(uuid.getUUID());
			vo.setTicketId(tkt.getTicketId());
			vo.setStatusCode(StatusCode.OPENED);
			vo.setSummary(StatusCode.OPENED.codeName);
			vo.setCreateDate(startCal.getTime());
			vo.setBillableAmtNo(0);
			vo.setDispositionBy(LEGACY_USER_ID);
			entries.add(vo);
			finalTicketStatus = vo.getStatusCode();

			//scenario 1: missing serial #
			if (StatusCode.MISSING_SERIAL_NO == tkt.getStatusCode()) {
				startCal.add(Calendar.MINUTE, 5); // relatively - 0:05am
				vo = new TicketLedgerVO();
				vo.setLedgerEntryId(uuid.getUUID());
				vo.setTicketId(tkt.getTicketId());
				vo.setStatusCode(StatusCode.MISSING_SERIAL_NO);
				vo.setSummary(StatusCode.MISSING_SERIAL_NO.codeName);
				vo.setCreateDate(startCal.getTime());
				vo.setBillableAmtNo(0);
				vo.setDispositionBy(LEGACY_USER_ID);
				entries.add(vo);
				//finalTicketStatus = vo.getStatusCode()

				//scenario 2: have serial# but no cas
			} else if (StringUtil.isEmpty(tkt.getCasLocationId())) {
				startCal.add(Calendar.MINUTE, 5); // relatively - 0:05am
				vo = new TicketLedgerVO();
				vo.setLedgerEntryId(uuid.getUUID());
				vo.setTicketId(tkt.getTicketId());
				vo.setStatusCode(StatusCode.USER_CALL_DATA_INCOMPLETE);
				vo.setSummary(StatusCode.USER_CALL_DATA_INCOMPLETE.codeName);
				vo.setCreateDate(startCal.getTime());
				vo.setBillableAmtNo(0);
				vo.setDispositionBy(LEGACY_USER_ID);
				entries.add(vo);
				finalTicketStatus = vo.getStatusCode();

				//scenario 3: rely on legacy value from Soutware
			} else {
				startCal.add(Calendar.HOUR, 20); //relatively - 8:00pm
				startCal.add(Calendar.MINUTE, 0);
				vo = new TicketLedgerVO();
				vo.setLedgerEntryId(uuid.getUUID());
				vo.setTicketId(tkt.getTicketId());
				vo.setStatusCode(tkt.getStatusCode());
				vo.setSummary(tkt.getStatusCode().codeName);
				vo.setCreateDate(startCal.getTime());
				vo.setBillableAmtNo(0);
				vo.setDispositionBy(LEGACY_USER_ID);
				entries.add(vo);
				finalTicketStatus = vo.getStatusCode();
			}

			//ticket closed?
			if (tkt.getClosedDate() != null) {
				startCal.setTime(tkt.getClosedDate());
				startCal.set(Calendar.HOUR, 23);
				startCal.set(Calendar.MINUTE, 59);

				vo = new TicketLedgerVO();
				vo.setLedgerEntryId(uuid.getUUID());
				vo.setTicketId(tkt.getTicketId());
				vo.setStatusCode(StatusCode.CLOSED);
				vo.setSummary(StatusCode.CLOSED.codeName);
				vo.setCreateDate(startCal.getTime());
				vo.setBillableAmtNo(0);
				vo.setDispositionBy(LEGACY_USER_ID);
				entries.add(vo);
				finalTicketStatus = vo.getStatusCode();
			}
			//ensure the ticket follows along with the final status on the ledger
			tkt.setStatusCode(finalTicketStatus);
		}
		writeToDB(entries);
	}


	/**
	 * Create diagnostic_run & _xr records - needed by the UI
	 * @param values
	 * @throws Exception 
	 */
	private void createDiagnosticRun(Collection<ExtTicketVO> tickets) throws Exception {
		List<DiagnosticRunVO> runs = new ArrayList<>(tickets.size());
		List<DiagnosticTicketVO> xrs = new ArrayList<>(tickets.size()*7);
		DiagnosticTicketVO xr;
		for (ExtTicketVO tkt : tickets) {
			//create the run bound to the ticket
			DiagnosticRunVO run = new DiagnosticRunVO();
			run.setTicketId(tkt.getTicketId());
			run.setCreateDate(tkt.getCreateDate());
			run.setDiagnosticRunId("DIAG_" + tkt.getTicketId());
			run.setDispositionedBy(LEGACY_USER_ID);
			run.setDiagComments(LEGACY_DIAG_NM);
			runs.add(run);

			//create the run_XR to the diagnostic record
			xr = new DiagnosticTicketVO();
			xr.setDiagnosticRunId(run.getDiagnosticRunId());
			xr.setDiagnosticCode(LEGACY_DIAG_CD);
			xr.setCreateDate(run.getCreateDate());
			xrs.add(xr);

			//if we have a cas assigned, presume the cas diags have also been run
			if (!StringUtil.isEmpty(tkt.getCasLocationId())) {
				for (String diagCd : CAS_DIAGS) {
					xr = new DiagnosticTicketVO();
					xr.setDiagnosticRunId(run.getDiagnosticRunId());
					xr.setDiagnosticCode(diagCd);
					xr.setCreateDate(run.getCreateDate());
					xrs.add(xr);
				}
			}
		}

		log.info("saving diagnostic_run");
		writeToDB(runs);
		log.info("saving diagnostic_run_xr");
		writeToDB(xrs);
	}


	/**
	 * Insert ticket_assigment for the caller/owner, retailer, and assigned-to cas location
	 * @param values
	 * @throws Exception 
	 */
	private void createTicketAssignments(Collection<ExtTicketVO> tickets) throws Exception {
		String locnId;
		TicketAssignmentVO vo;

		Set<String> missingLocns = new HashSet<>();
		List<TicketAssignmentVO> assgs = new ArrayList<>(tickets.size()*2); //owner+cas
		for (ExtTicketVO tkt : tickets) {
			//bring the owner record over
			assgs.addAll(tkt.getAssignments());

			//create an entry for the cas
			if (!StringUtil.isEmpty(tkt.getCasLocationId()) && !tkt.getCasLocationId().matches("0+")) {
				locnId = casLocations.get(tkt.getCasLocationId());
				if (!StringUtil.isEmpty(locnId)) {
					vo = new TicketAssignmentVO();
					vo.setTicketAssignmentId(uuid.getUUID());
					vo.setTypeCode(TypeCode.CAS);
					vo.setTicketId(tkt.getTicketId());
					vo.setLocationId(locnId);
					assgs.add(vo);
					//add the cas assignment to the ticket - used for creating a schedule
					tkt.addAssignment(vo);
					//replace the locationId on the ticket with the actual one we need from the DB
					tkt.setCasLocationId(locnId);
				} else {
					missingLocns.add(tkt.getCasLocationId());
					tkt.setCasLocationId(null);
				}
			}
		}

		for (String s: missingLocns)
			log.error("missing cas location: " + s + ".  Tickets referencing this value will not have a CAS assigned.");

		writeToDB(assgs);
	}


	/**
	 * Create a "back-n-forth' dummy schedule if the unit was assigned to a cas.
	 * (also presume it was returned, if the ticket is closed)
	 * @param values
	 * @throws Exception 
	 */
	private void createSchedules(Collection<ExtTicketVO> tickets) throws Exception {
		List<TicketScheduleVO> schedules = new ArrayList<>(tickets.size()*2);
		List<TicketLedgerVO> ledgers = new ArrayList<>(tickets.size()*2);
		TicketScheduleVO sched;
		String casAssgId = null;
		String ownerAssgId = null;

		for (ExtTicketVO tkt : tickets) {
			//no schedule if not cas assigned
			if (StringUtil.isEmpty(tkt.getCasLocationId())) continue;
			casAssgId = ownerAssgId = null; //flush these from the last ticket
			for (TicketAssignmentVO assg : tkt.getAssignments()) {
				if (TypeCode.CAS == assg.getTypeCode()) {
					casAssgId = assg.getTicketAssignmentId();
				} else if (TypeCode.CALLER == assg.getTypeCode()) {
					ownerAssgId = assg.getTicketAssignmentId();
				}
			}

			//create a schedule entry from the caller to the cas
			sched = new TicketScheduleVO();
			sched.setTicketId(tkt.getTicketId());
			sched.setCasLocationId(casAssgId);
			sched.setOwnerLocationId(ownerAssgId);
			sched.setSignerName(LEGACY_DIAG_CD);
			sched.setSignatureText(LEGACY_SW_SIGNATURE);
			sched.setProductValidatedFlag(1);
			sched.setCreateDate(tkt.getCreateDate());
			sched.setScheduleDate(tkt.getCreateDate());
			sched.setCompleteDate(tkt.getCreateDate());
			sched.setTypeCode(TicketScheduleVO.TypeCode.DROPOFF);
			sched.setRecordTypeCode("preRepair");
			schedules.add(sched);
			ledgers.add(createLedgerFromSchedule(sched, false, false));
			ledgers.add(createLedgerFromSchedule(sched, false, true));

			//create a schedule entry from the cas back to the caller (owner)
			//only return the TV if the ticket is closed (presume repairs are complete)
			if (tkt.getClosedDate() != null) {
				sched = new TicketScheduleVO();
				sched.setTicketId(tkt.getTicketId());
				sched.setCasLocationId(casAssgId);
				sched.setOwnerLocationId(ownerAssgId);
				sched.setSignerName(LEGACY_DIAG_CD);
				sched.setSignatureText(LEGACY_SW_SIGNATURE);
				sched.setProductValidatedFlag(1);
				sched.setCreateDate(tkt.getClosedDate());
				sched.setScheduleDate(tkt.getClosedDate());
				sched.setCompleteDate(tkt.getClosedDate());
				sched.setTypeCode(TicketScheduleVO.TypeCode.PICKUP);
				sched.setRecordTypeCode("postRepair");
				schedules.add(sched);
				ledgers.add(createLedgerFromSchedule(sched, true, false));
				ledgers.add(createLedgerFromSchedule(sched, true, true));
			}
		}

		writeToDB(ledgers);
		writeToDB(schedules);
	}


	/**
	 * Create a ledger entry for the schedule item.
	 * @param sched
	 * @return
	 */
	private TicketLedgerVO createLedgerFromSchedule(TicketScheduleVO sched, boolean isPickup, boolean isComplete) {
		TicketLedgerVO vo = new TicketLedgerVO();
		vo.setTicketId(sched.getTicketId());
		vo.setDispositionBy(LEGACY_USER_ID);
		vo.setLedgerEntryId(uuid.getUUID());

		//create a calendar we can role forward in small/stepped amounts, to ensure timeline chronology
		Calendar cal = Calendar.getInstance();
		cal.setTime(sched.getCreateDate());

		if (isPickup && !isComplete) { //pickup pending
			vo.setStatusCode(StatusCode.PENDING_PICKUP);
			vo.setSummary("Servicio Programado");
			cal.set(Calendar.SECOND, 05);

		} else if (isPickup && isComplete) { //pickup complete
			vo.setStatusCode(StatusCode.PICKUP_COMPLETE);
			vo.setSummary("Equipo Ingresa a Centro de Servicio - ");
			vo.setUnitLocation(UnitLocation.CAS); //unit is picked up and now at cas
			sched.setLedgerEntryId(vo.getLedgerEntryId()); //pass this back to the schedule, so their connected
			cal.set(Calendar.SECOND, 10);

		} else if (!isComplete) { //pending delivery
			vo.setStatusCode(StatusCode.DELIVERY_SCHEDULED);
			vo.setSummary("Servicio Programado");
			cal.set(Calendar.HOUR, 20);
			cal.set(Calendar.SECOND, 05);

		} else if (isComplete) { //delivery complete
			vo.setStatusCode(StatusCode.DELIVERY_COMPLETE);
			vo.setSummary("Equipo Ingresa a Centro de Servicio - ");
			vo.setUnitLocation(UnitLocation.CALLER); //unit is returned to the owner
			sched.setLedgerEntryId(vo.getLedgerEntryId()); //pass this back to the schedule, so their connected
			cal.set(Calendar.HOUR, 20);
			cal.set(Calendar.SECOND, 10);
		}

		vo.setCreateDate(cal.getTime());
		return vo;
	}


	/**
	 * Similar to Map.contains, but returns the key
	 * @param map
	 * @param value
	 * @return
	 */
	private String getKeyFromValue(Map<String, String> map, String value) {
		if (StringUtil.isEmpty(value)) return null;

		for (Map.Entry<String, String> entry : map.entrySet()) {
			if (value.equals(entry.getValue()))
				return entry.getKey();
		}
		return null;
	}


	/**
	 * Write certain columns to the DB as ticket_data
	 * @param values
	 */
	private void saveTicketAttributes(Collection<ExtTicketVO> tickets) {
		List<TicketDataVO> attrs = new ArrayList<>(tickets.size()*4);

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
		MapUtil.asMap(cats, db.executeSelect(sql, null, new GenericVO()));

		for (ExtTicketVO tkt : tickets) {
			if (cats.containsKey(tkt.getProductCategoryId())) //the key is already set correctly, leave it.
				continue;

			//look for it in the value column
			String catId = getKeyFromValue(cats, tkt.getProductCategoryId());
			if (!StringUtil.isEmpty(catId))
				tkt.setProductCategoryId(catId);

			//still missing, add it to the DB as well as our Map
			ProductCategoryVO cat = new ProductCategoryVO();
			cat.setActiveFlag(0);
			cat.setCategoryCode(tkt.getProductCategoryId());
			cat.setProductCategoryId(tkt.getProductCategoryId());
			try {
				db.executeBatch(Arrays.asList(cat), true);
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
		String sql = StringUtil.join("select provider_id as key, coalesce(provider_nm,'') as value from ", 
				schema, "wsla_provider where provider_type_id='OEM'");
		List<GenericVO> oems = db.executeSelect(sql, null, new GenericVO());
		//add the database lookups to the hard-coded 'special' mappings already in this file
		for (GenericVO vo : oems) {
			//don't stomp ones we've predefined, they're more important
			if (!oemMap.containsKey(vo.getKey().toString()))
				oemMap.put(vo.getKey().toString(), vo.getValue().toString());
		}

		Set<String> missing = new HashSet<>();
		for (ExtTicketVO vo : tickets) {
			if (oemMap.containsKey(vo.getOemId())) {
				//the ID is already the correct here, do nothing
				continue;
			}

			//find the key tied to this matching value
			String oemId = getKeyFromValue(oemMap, vo.getOemId());
			if (!StringUtil.isEmpty(oemId)) {
				vo.setOemId(oemId);
			} else if (StringUtil.isEmpty(vo.getOemId())) {
				log.error(String.format("ticket %s has a blank value for OEM", vo.getTicketId()));
			} else {
				missing.add(vo.getOemId());
				//log.debug("ticket missing oem: " + vo)
			}
		}

		//if we're missing OEMs, the import script must fail
		if (!missing.isEmpty()) {
			log.fatal("ADDING THESE MISSING OEMS:\n");
			for (String s : missing) {
				log.fatal(s);
				ProviderVO vo = new ProviderVO();
				vo.setProviderId(s);
				vo.setProviderName(s);
				vo.setProviderType(ProviderType.OEM);
				vo.setReviewFlag(1);
				try {
					db.insert(vo);
				} catch (Exception e) {
					log.error("could not create OEM " + s, e);
				}
			}
			// Pause and go around again
			// There shouldn't be any missing OEMs, and the tickets that were missing OEMs need populated
			sleepThread(1000);
			bindOEMs(tickets);
		}
		log.debug("all OEMs exist");
	}


	/**
	 * Attach an 800# to each ticket - take the 1st listed for the OEM
	 * @param tickets
	 */
	private void bind800Numbers(Collection<ExtTicketVO> tickets) {
		Map<String, String> oemPhones = new HashMap<>(30);
		oemPhones.put("CMK", "8000623555");
		oemPhones.put("PHI", "8001234567");
		oemPhones.put("SAN", "8001726784");
		oemPhones.put("HIT", "8002882600");
		oemPhones.put("SEI", "8008011050");
		oemPhones.put("SKC", "8000623455");
		oemPhones.put("KON", "8000600075");
		oemPhones.put("WMT", "8000623555");
		oemPhones.put("VIZ", "8008010096");
		oemPhones.put("WES", "8009378464");
		oemPhones.put("RCA", "8009277268");
		oemPhones.put("VEL", "8003248328");
		oemPhones.put("WSL", ""); //doesn't exist - ok blank
		oemPhones.put("MIL", ""); //doesn't exist - ok blank
		oemPhones.put("CT", ""); //unknown by WSLA - possibly bad data
		oemPhones.put("SME", ""); //Steve's initials for testing - ignore
		oemPhones.put("INT", ""); //INTernal testing - ignore
		oemPhones.put("MIT", "8888888888");

		String sql = StringUtil.join("select provider_id as key, phone_number_txt as value from ", schema, 
				"wsla_provider_phone where country_cd='MX' and active_flg=1");
		MapUtil.asMap(oemPhones, db.executeSelect(sql, null, new GenericVO()));

		Set<String> missingPhones = new HashSet<>();
		for (ExtTicketVO tkt : tickets) {
			String phone = oemPhones.get(tkt.getPhoneLookup());
			if (phone == null) {
				missingPhones.add(tkt.getPhoneLookup());
				log.error(String.format("missing 800# for oem %s on ticket %s", tkt.getPhoneLookup(), tkt.getTicketId()));
			} else if (!phone.isEmpty()) {
				//create a ticket attribute and add it to the ticket data
				TicketDataVO attr = new TicketDataVO();
				attr.setTicketId(tkt.getTicketId());
				attr.setAttributeCode("attr_phoneNumberText");
				attr.setValue(phone);
				tkt.addTicketData(attr);
			}
		}

		if (!missingPhones.isEmpty()) {
			log.fatal("MISSING 800#s");
			for (String ph : missingPhones)
				System.err.println(ph);
		}

		log.info("phone numbers set");
	}


	/**
	 * @param values
	 */
	private void bindSerialNos(Collection<ExtTicketVO> tickets, boolean ignoreMissing) {
		String sql = StringUtil.join("select coalesce(s.product_serial_id,'blank'||newid()) as product_serial_id, ",
				"s.serial_no_txt, p.product_id, p.provider_id, p.cust_product_id from ",
				schema, "wsla_product_master p  left join ", schema, "wsla_product_serial s ",
				"on s.product_id=p.product_id where p.set_flg=1");
		List<ProductSerialNumberVO> serials = db.executeSelect(sql, null, new ProductSerialNumberVO());

		//update our tickets and give them the proper serial# pkId.  If they don't exist, add them to a list to add to the DB
		boolean isFound;
		String tktProdNm;
		List<ExtTicketVO> missing = new ArrayList<>(1000);
		for (ExtTicketVO tkt : tickets) {
			isFound = false;
			tktProdNm = StringUtil.checkVal(tkt.getCustProductId()); //this is raw EquipmentId in the file, not yet converted to a GUID
			if (!ignoreMissing) auditSKU(tkt);

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
			//ignoreMissing is true after we've saved the blanks once - avoids an infinite loop
			if (!isFound && !ignoreMissing)
				missing.add(tkt);
		}
		log.info("missing " + missing.size() + " skus in the database");
		if (missing.isEmpty()) return;  //done here, everyone has a productId & productSerialId

		createNewProducts(missing, tickets);
		log.info("serial #s finished");
	}


	/**
	 * Purge the SKU if the SKU is empty or all zeros, or matches one of our know falsifications
	 * By flushing bogus values we pave a path to create a placeholder entry - a serial# will have to be populated (into it) later.
	 * @param tkt
	 * @return
	 */
	private void auditSKU(ExtTicketVO tkt) {
		if (!isValidSKU(tkt)) {
			tkt.setProductSerialId(null);
			//if we don't have a product we can't have a productWarranty
			tkt.setProductWarrantyId(null);
			tkt.setStatusCode(StatusCode.MISSING_SERIAL_NO);
			log.warn(String.format("invalid serial# %s on ticket %s", tkt.getSerialNoText(), tkt.getTicketId()));
		}
	}


	/**
	 * return false if the SKU is empty or all zeros, or matches one of our know falsifications
	 * @param tkt
	 * @return
	 */
	private boolean isValidSKU(ExtTicketVO tkt) {
		String sn = tkt.getSerialNoText();
		return !(StringUtil.isEmpty(sn) || sn.length() < 3 || sn.matches("0+") || fakeSKUs.contains(sn.toUpperCase()));
	}


	/**
	 * Split from above for complexity reasons - creates and saves new products 
	 * and serial#s determined to be missing from the DB.  Calls back to bind afterwards (circular) to ensure all are accounted for.
	 * @param missing
	 * @param tickets
	 */
	private void createNewProducts(List<ExtTicketVO> missing, Collection<ExtTicketVO> tickets) {
		//the keys on these maps ensure we aren't inserting the same SKUs twice.  The key is the vendor's unique ID, not our GUIDs
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
				prod.setValidatedFlag(1);
				prod.setCustomerProductId(vo.getCustProductId());
				prod.setProductName(vo.getCustProductId());
				prod.setDescription("CREATED BY LEGACY DATA IMPORT");
				newProducts.put(vo.getOemId() + prod.getCustomerProductId(), prod); //key here needs to be oem+product
				//give the pkId back to the ticket, so it can be used for serial# inserts
				vo.setProductId(prod.getProductId());
			}

			boolean isValidSku = isValidSKU(vo);
			ProductSerialNumberVO prodser = isValidSku ? newSerials.get(vo.getProductId() + vo.getSerialNoText()) : null;
			if (StringUtil.isEmpty(vo.getProductSerialId()) && prodser != null) {
				//share the one we've already slated for saving with this ticket too
				vo.setProductSerialId(prodser.getProductSerialId());

			} else if (StringUtil.isEmpty(vo.getProductSerialId())) {
				//log.debug("no serial# for " + vo.getSerialNoText() + " adding to product " + vo.getProductId())
				//insert the productSerial with a predefined pkId
				ProductSerialNumberVO ser = new ProductSerialNumberVO();
				ser.setProductSerialId(uuid.getUUID());
				if (isValidSku) ser.setSerialNumber(vo.getSerialNoText());
				ser.setProductId(vo.getProductId());
				if (isValidSku) ser.setValidatedFlag(1);
				newSerials.put(ser.getProductId() + (isValidSku ? vo.getSerialNoText() : uuid.getUUID()), ser); //key here needs to be product+sku
				//log.debug("creating serL: " + ser)
				//give the pkId back to the ticket, so it can be used for serial# inserts
				vo.setProductSerialId(ser.getProductSerialId());
				log.debug(String.format("adding serial %s for ticket %s.  prodSerId=%s", ser.getSerialNumber(), vo.getTicketId(), vo.getProductSerialId()));
			}
		}

		//save the new records.  Since we've given them pkIds we need to force insert here
		try {
			db.executeBatch(new ArrayList<>(newProducts.values()), true);
			log.info("added " + newProducts.size() + " new products");
			db.executeBatch(new ArrayList<>(newSerials.values()), true);
			log.info("added " + newSerials.size() + " new product serial#s");
		} catch (Exception e) {
			log.error("could not add new products or SKUs", e);
		}

		//recusively call this method.  Now that we've added data we should be able to marry more records.
		sleepThread(1000);

		// make a recursive callback to ensure all records exist and are accounted for
		// NOTE: endless looping will occur here if something goes wrong, watch for it.
		bindSerialNos(tickets, true);
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
		MapUtil.asMap(warrMap, db.executeSelect(sql, null, new GenericVO()));

		Set<String> newWarrs = new HashSet<>(100);
		for (ExtTicketVO tkt : tickets) {
			//warranty is based on product serial#.  no product or no oem - skip it!
			if (StringUtil.isEmpty(tkt.getProductSerialId()) || StringUtil.isEmpty(tkt.getOemId())) continue;

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
			db.executeBatch(lst);
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
		MapUtil.asMap(prodWarrMap, db.executeSelect(sql, null, new GenericVO()));

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
			db.executeBatch(lst);
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
		String sql = StringUtil.join("select lower(email_address_txt) as key, user_id as value from ", 
				schema, "wsla_user order by create_dt desc"); //if dups, the earliest/inital record will be used
		Map<String, String> existingUsers = new HashMap<>(tickets.size());
		MapUtil.asMap(existingUsers, db.executeSelect(sql, null, new GenericVO()));

		ProfileManager pm = ProfileManagerFactory.getInstance(getAttributes());
		Map<String, UserVO> users = new HashMap<>(tickets.size());
		//build a unique set to save to the DB
		String pkId = null;
		for (ExtTicketVO tkt : tickets.values()) {
			UserVO user = tkt.getOriginator();
			pkId = user.getEmail();
			if (StringUtil.isEmpty(pkId)) { //ensure users with no email don't collide - we'll let ProfileManager de-duplicate these.
				pkId = tkt.getTicketIdText();
			} else {
				String userId = existingUsers.get(pkId.toLowerCase());
				if (userId != null) {
					tkt.setUserId(userId);
					log.debug("existing user, skipping");
					continue;
				}
			}
			//these two will tell us how to link up the originator after the user is saved or created
			user.setRoleName(pkId);
			tkt.setUniqueUserId(pkId);
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

		//marry the users back the tickets.  This can't be done by reference because we de-duplicated the users list before creating profiles/users.
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
				assg.setTicketAssignmentId(uuid.getUUID());
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
		//we need to first check if these users exist in the table, use profile_id for this
		String sql = StringUtil.join("select profile_id as key, user_id as value from ", schema, "wsla_user order by create_dt desc"); //if dups, the earliest/inital record will be used
		Map<String, String> userMap = new HashMap<>(users.size());
		MapUtil.asMap(userMap, db.executeSelect(sql, null, new GenericVO()));

		Map<String, UserVO> userVoMap = new HashMap<>(users.size());
		for (UserVO user : users) {
			try {
				user.setUserId(userMap.get(user.getProfileId())); //setting here updates exising records, rather than creating dups
				db.save(user);
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
	private ExtTicketVO transposeTicketData(SOHDRFileVO dataVo, ExtTicketVO vo) {
		vo.setBatchName(batchNm);
		vo.setTicketId(dataVo.getSoNumber());
		vo.setTicketIdText(dataVo.getSoNumber());
		vo.setCreateDate(dataVo.getReceivedDate());
		vo.setClosedDate(dataVo.getClosedDate());
		vo.setUpdateDate(dataVo.getAltKeyDate());
		vo.setUnitLocation(UnitLocation.CALLER);
		vo.setOemId(dataVo.getManufacturer());
		vo.setCustProductId(dataVo.getEquipmentId());
		vo.setSerialNoText(dataVo.getSerialNumber());
		vo.setOriginator(createOriginator(dataVo));
		vo.setPhoneNumber(dataVo.getCustPhone());
		vo.setWarrantyValidFlag("CNG".equalsIgnoreCase(dataVo.getCoverageCode()) ? 1 : 0); //this is the only code that results in warranty coverage
		vo.setProductCategoryId(dataVo.getProductCategory());
		vo.setCasLocationId(dataVo.getServiceTech());
		vo.setHistoricalFlag(1);
		vo.setOperator(dataVo.getOperator());

		if (vo.getClosedDate() != null) {
			vo.setStatusCode(StatusCode.CLOSED);
		} else if (!isValidSKU(vo)) { //no serial
			vo.setStatusCode(StatusCode.MISSING_SERIAL_NO);
		} else if (StringUtil.isEmpty(vo.getCasLocationId())) { //have serial but no cas
			vo.setStatusCode(StatusCode.USER_CALL_DATA_INCOMPLETE);
		} else { //have serial, have cas...take the mapped status from Southware
			vo.setStatusCode(dataVo.getOpenTicketStatus());
		}

		if ("40".equals(dataVo.getUserArea2()))
			vo.setStandingCode(Standing.CRITICAL);

		attachTicketData(dataVo, vo);
		return vo;
	}


	/**
	 * split from above to reduce complexity.  Transpose ticket_data table/fields
	 * @param dataVo
	 * @param vo
	 */
	private void attachTicketData(SOHDRFileVO dataVo, ExtTicketVO vo) {
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
			attr.setAttributeCode("attr_unitDefect");
			attr.setValue(lookupDefectCode(dataVo.getProblemCode()));
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
		if (!StringUtil.isEmpty(dataVo.getCoverageCode()) && !StringUtil.isEmpty(vo.getCasLocationId())) {
			attr = new TicketDataVO();
			attr.setTicketId(vo.getTicketId());
			attr.setAttributeCode("attr_dispositionCode");
			if (vo.getClosedDate() != null && "CNG".equals(dataVo.getCoverageCode())) {
				attr.setValue("REPAIRED");
			} else if ("CNG".equals(dataVo.getCoverageCode())) {
				attr.setValue("REPAIRABLE");
			} else { //SNG
				attr.setValue("NONREPAIRABLE");
			}
			vo.addTicketData(attr);
		}
	}


	/**
	 * transpose partial defect & repair codes to the full pkId values stored in our DB.
	 * Note: this method is duplicated in SOExtendedData - change both when revising
	 * @param problemCode
	 * @return
	 */
	private String lookupDefectCode(String partialDefectCode) {
		if (StringUtil.isEmpty(partialDefectCode)) return null;
		if (partialDefectCode.matches("0+")) partialDefectCode="0";

		String code = defectMap.get(partialDefectCode);
		if (StringUtil.isEmpty(code)) {
			log.warn("missing defect code " + partialDefectCode);
			return partialDefectCode; //preserve what we have, these can be fixed via query manually
		}
		return code;
	}


	/**
	 * Create the owner  
	 * We'll need to add them (as a User), then save THAT guid with the ticket as originator_id
	 * @param dataVo
	 * @return
	 */
	private UserVO createOriginator(SOHDRFileVO dataVo) {
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
	 * load the defects table from the DB.  This accepts DBProcessor because it's invoked from other importers
	 * @param dbConn
	 * @return
	 */
	public static Map<String, String> loadDefectCodes(DBProcessor dbp, String schema) {
		String sql = StringUtil.join("select split_part(defect_cd,'-',2) as key, defect_cd as value from ", schema, "wsla_defect");
		Map<String, String> defects = new HashMap<>(250);
		MapUtil.asMap(defects, dbp.executeSelect(sql, null, new GenericVO()));
		return defects;
	}


	/**
	 * load the cas locations
	 * @param dbConn
	 * @return
	 */
	public void loadCasLocations() {
		String sql = StringUtil.join("select location_id as key, location_id as value from ", schema, "wsla_provider_location");
		MapUtil.asMap(casLocations, db.executeSelect(sql, null, new GenericVO()));

		//add a couple hard-coded translations - legacy to GUID translations:
		casLocations.put("MEX100","7d6a960cb55b751aac10029055efc669");
		casLocations.put("TIJ100","17e4e465b55dc48aac100290de8056e9");
		casLocations.put("NLE100","cb55f611b55ec39cac100290224759fd");
		casLocations.put("SON100","6dee5855b55cc0cfac10029011cb825e");
		casLocations.put("000001","b89e4d5a3e2c439f879a25aee66bedde");  //WSLA Bodega (Warehouse)
		casLocations.put("CHI100", "21d40467271a76d3ac10021bf21d3820");
		casLocations.put("RDA100", "bfc773e0271cd2ebac10021bf3309f5e");
		casLocations.put("CTM100", "af14a83e271ec15aac10021b54474781");
		log.debug("loaded " + casLocations.size() + " CAS locations");
	}


	/**
	 * load up the ticketIds from the database to cross-reference the soNumbers
	 */
	private void loadTicketIds() {
		String sql = StringUtil.join("select ticket_no as key, ticket_id as value from ", schema, "wsla_ticket");
		MapUtil.asMap(ticketMap, db.executeSelect(sql, null, new GenericVO()));
		log.info("loaded " + ticketMap.size() + " ticketIds from database");
	}
}
