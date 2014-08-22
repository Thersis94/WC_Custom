package com.ram.action.util;

// JDK 1.7.x
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

// Log4J 1.2.15
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

//WebCrescendo Custom
import com.ram.action.data.InventoryEventGroupVO;
import com.ram.action.event.InventoryEventRecurrenceAction;
//RAMDataFeed
import com.ram.datafeed.data.AuditorVO;
import com.ram.datafeed.data.CustomerEventVO;
import com.ram.datafeed.data.InventoryEventAuditorVO;
import com.ram.datafeed.data.InventoryEventReturnVO;
import com.ram.datafeed.data.InventoryEventVO;
import com.ram.datafeed.data.InventoryItemType;
import com.ram.datafeed.data.InventoryItemVO;
import com.ram.datafeed.transaction.TransactionVO;
//SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.UUIDGenerator;
//WebCrescendo
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: EventDataGeneratorUtil.java
 * <p/>
 * <b>Project</b>:
 * <p/>
 * <b>Description: </b> Script used to create valid historical inventory data
 * for the RAMGroup for purposes of generating report data.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Aug 1, 2014
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public class EventDataGeneratorUtil {

	// Member variables
	private String locationsFile = null;
	private String productsFile = null;
	private Properties config = new Properties();
	private static final Logger log = Logger.getLogger("EventDataGenerator");
	private StringBuilder errorLog = new StringBuilder();
	private Connection conn = null;

	// Values defined in the config file
	private int NUM_RECALLS = 0;
	private int TOTAL_WEEKS = 0; // 6 months
	private int INV_COUNT = 0;
	private String schema = null;
	private String encKey = null;
	private int tp = 0;
	// DBProcessor for handling some queries
	DBProcessor db = null;

	// Random Generator for randomizing data set.
	private Random r = null;

	/**
	 * Load in the configs and instantiate helper objects
	 * 
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 * 
	 */
	public EventDataGeneratorUtil() throws Exception {
		// Initialize the logger
		PropertyConfigurator.configure("scripts/ram_importer_log4j.properties");

		// Load the config file
		config.load(new FileInputStream(new File(
				"scripts/ram_importer.properties")));

		// Load the file location
		locationsFile = config.getProperty("testLocationsFile");
		productsFile = config.getProperty("testProductsFile");
		TOTAL_WEEKS = Convert.formatInteger(config.getProperty("totalWeeks"));
		NUM_RECALLS = Convert.formatInteger(config.getProperty("numRecalls"));
		INV_COUNT = Convert.formatInteger(config.getProperty("invCount"));
		schema = config.getProperty("schema");
		encKey = config.getProperty("encKey");
		// Get the DB Connection
		conn = getConnection();

		// Instantiate helper methods.
		r = new Random();
		db = new DBProcessor(conn, schema);
	}

	/**
	 * Main method for running the script. Will store the start time and upon
	 * completion print the total time the script took to run.
	 * 
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		EventDataGeneratorUtil edg = new EventDataGeneratorUtil();

		try {
			log.info("Starting RAM Product Importer");
			long s = System.currentTimeMillis();
			edg.processEvents();
			long e = System.currentTimeMillis();
			long t = e - s;
			int min = (int) (t / 1000) / 60;
			int sec = (int) (t / 1000) % 60;
			log.debug("Completed in " + min + ":" + sec);

		} catch (Exception e) {
			log.debug(edg.errorLog);
			edg.errorLog.append("Unable to complete: ").append(e.getMessage())
					.append("\n<br/>");
			log.error("Error creating product info", e);
		}
	}

	/**
	 * This method is in charge of managing when data is retrieved and created
	 * through the lifecycle of the script. The primary steps are 1. Build event
	 * groups 2. Build initial events for replication. 3. Loop over groups and
	 * generate auditor, customer and return data. 4. Call out to
	 * recurrenceAction which will replicate our event as needed. 5. Loop over
	 * List of events and add Transactional data for each.
	 * 
	 * @throws IOException
	 * @throws SQLException
	 * @throws com.siliconmtn.db.util.DatabaseException
	 * @throws InvalidDataException
	 */
	protected void processEvents() throws Exception {

		// Load the product and location data.
		List<InventoryItemVO> prods = loadProducts(retrieveFileData(productsFile));
		List<String> locs = loadLocations(retrieveFileData(locationsFile));

		// Build Event Groups
		Map<String, InventoryEventGroupVO> groups = generateEventGroups(locs);

		// Build Initial Events
		generateEvents(groups);

		// Instantiate RecurrenceAction
		InventoryEventRecurrenceAction iera = new InventoryEventRecurrenceAction();
		Map<String, Object> attributes = new HashMap<String, Object>();
		attributes.put(Constants.CUSTOM_DB_SCHEMA, schema);

		// Special Attribute used by recurrence action to send new events back
		// to us.
		attributes.put(InventoryEventRecurrenceAction.IS_UTIL_BATCH, true);
		iera.setAttributes(attributes);
		iera.setDBConnection(new SMTDBConnection(conn));

		/*
		 * Loop over groups and build initial Auditor, Customer and Return
		 * information before calling to recurrences.
		 */
		for (InventoryEventGroupVO g : groups.values()) {
			InventoryEventVO v = g.getEvents().get(0);

			// Build Event Customers
			generateCustomers(v);

			// Build Event Auditors
			generateAuditors(v);

			// Build Event Returns
			List<InventoryItemVO> returns = generateReturns(v, prods);

			/*
			 * Build Recurrences. Using the flag above, the recurrence action
			 * will place the newly created events on the events list we pass
			 * in.
			 */
			iera.processRecurrences(g, g.getEvents());

			/*
			 * Build Data for all the Events.
			 */
			for (int i = 0; i < g.getEvents().size(); i++) {
				InventoryEventVO iev = g.getEvents().get(i);

				/*
				 * The first event has its auditors, events and returns already
				 * so no reason to get it again.
				 */
				if (i != 0)
					retrievePeripherals(iev);

				// Build Datafeed Transaction
				generateTransaction(iev);

				// Build Inventory Items
				generateItems(iev, prods, returns);

				// Current event is now set up and fully populated with data
			}
			log.debug("Completed Group: " + g.getInventoryEventGroupId());
			// All Events in the group are now fully populates with data.
		}

		// Close the db connection.
		DBUtil.close(conn);
	}

	/**
	 * This method is responsible for generating transactional inventory Item
	 * records for a given event. We Iterate over the returns list and then
	 * randomly select additional products from the master list and then store
	 * the resulting combination in the db.
	 * 
	 * @param iev
	 * @param prods
	 * @param returns
	 * @throws CloneNotSupportedException
	 */
	private void generateItems(InventoryEventVO iev,
			List<InventoryItemVO> prods, List<InventoryItemVO> returns)
			throws CloneNotSupportedException {
		Map<InventoryItemVO, InventoryItemType> items = new HashMap<InventoryItemVO, InventoryItemType>();
		InventoryItemVO t = null;

		// Gather Returns and set them on the map with Recall type.
		for (InventoryItemVO ret : returns) {
			t = ret;
			items.put(t, InventoryItemType.RECALL);
		}

		/*
		 * Gather enough products to fill the difference betwen INV_COUNT and
		 * RECALLS Ensure we don't grab a product already in recall as the map
		 * wont support that.
		 */
		for (int i = 0; i <= INV_COUNT - NUM_RECALLS; i++) {
			InventoryItemVO v = prods.get(r.nextInt(tp));
			if (!returns.contains(v))
				items.put(v, v.getItemType());
			else
				i--;
		}

		// SQL Statment for inserting inventory items
		StringBuilder sb = new StringBuilder();
		sb.append("insert into ").append(schema)
				.append("RAM_INVENTORY_ITEM ( ");
		sb.append("INVENTORY_ITEM_ID, INVENTORY_EVENT_AUDITOR_XR_ID, PRODUCT_ID, ");
		sb.append("TRANSACTION_ID, INVENTORY_ITEM_TYPE_CD, SERIAL_NUMBER_TXT, ");
		sb.append("EXPIRATION_DT, QUANTITY_NO, LOT_NUMBER_TXT, INVENTORY_DT, CREATE_DT) ");
		sb.append("values(?,?,?,?,?,?,?,?,?,?,?)");

		// random generator for ids.
		UUIDGenerator id = new UUIDGenerator();
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sb.toString());

			/*
			 * Loop over the collection of inventoryItems and add them to a
			 * batch statement.
			 */
			for (InventoryItemVO v : items.keySet()) {
				ps.setString(1, id.getUUID());
				ps.setInt(2, iev.getAuditors().get(0)
						.getInventoryEventAuditorId());
				ps.setInt(3, v.getProductId());
				ps.setString(4, iev.getTransaction().getTransactionId());
				ps.setString(5, items.get(v).name());
				ps.setString(6, v.getSerialNumber());
				ps.setDate(7,
						new java.sql.Date(v.getExpirationDate().getTime()));

				// Set random quantity
				ps.setInt(8, r.nextInt(10) + 1);
				ps.setString(9, v.getLotNumber());
				ps.setTimestamp(10, Convert.getCurrentTimestamp());
				ps.setTimestamp(11, Convert.getCurrentTimestamp());
				ps.addBatch();
			}

			// Batch execute the query.
			ps.executeBatch();
		} catch (SQLException sqle) {
			log.error(sqle);
		} finally {
			DBUtil.close(ps);
		}
	}

	/**
	 * This method handles generating our TransactionVO, storing it in the db
	 * and setting it on the event.
	 * 
	 * @param iev
	 * @throws com.siliconmtn.db.util.DatabaseException
	 * @throws InvalidDataException
	 */
	private void generateTransaction(InventoryEventVO iev)
			throws InvalidDataException,
			com.siliconmtn.db.util.DatabaseException {
		TransactionVO d = getTransactionVO(iev);
		db.insert(d);
		iev.setTransaction(d);
	}

	/**
	 * This method generates the TransactionVO for an Event based on information
	 * contained in the event VO.
	 * 
	 * @param iev
	 * @return
	 */
	private TransactionVO getTransactionVO(InventoryEventVO iev) {
		TransactionVO t = new TransactionVO();
		t.setTransactionId(iev.getInventoryEventId() + "_TRANS");
		t.setInventoryEventAuditorId(iev.getAuditors().get(0)
				.getInventoryEventAuditorId());
		t.setTransactionResponseCode("TRANSACTION_SUCCESS");
		t.setTransactionStart(Convert.getCurrentTimestamp());
		t.setPackageCount(0);
		t.setInventoryCount(INV_COUNT);
		t.setTransactionEnd(Convert.getCurrentTimestamp());
		t.setActiveFlag(1);
		t.setCreateDate(Convert.getCurrentTimestamp());
		return t;
	}

	/**
	 * This method is responsible for populating an event with all related
	 * auditor, event and customer information from the db. This information is
	 * not present on any of the recurring events and must be added.
	 * 
	 * @param iev
	 */
	private void retrievePeripherals(InventoryEventVO iev) {

		// Statements for retrieving necessary information
		PreparedStatement aud = null;
		PreparedStatement ret = null;
		PreparedStatement cust = null;

		StringBuilder aq = new StringBuilder();
		aq.append("select * from ")
				.append(schema)
				.append("RAM_INVENTORY_EVENT_AUDITOR_XR a inner join ")
				.append(schema)
				.append("RAM_AUDITOR b on a.auditor_id = b.auditor_id where a.inventory_event_id = ?");
		StringBuilder rq = new StringBuilder();
		rq.append("select * from ").append(schema)
				.append("RAM_EVENT_RETURN_XR where inventory_event_id = ?");
		StringBuilder cq = new StringBuilder();
		cq.append("select * from ")
				.append(schema)
				.append("RAM_CUSTOMER_EVENT_XR a inner join ")
				.append(schema)
				.append("RAM_CUSTOMER b on a.customer_id = b.customer_id where a.inventory_event_id = ?");

		try {

			// retrieve Auditor information
			aud = conn.prepareStatement(aq.toString());
			aud.setInt(1, iev.getInventoryEventId());
			ResultSet rs = aud.executeQuery();
			while (rs.next()) {
				iev.addAuditor(new InventoryEventAuditorVO(rs, false, encKey));
			}

			// Retrieve Returns Information
			ret = conn.prepareStatement(rq.toString());
			ret.setInt(1, iev.getInventoryEventId());
			rs = ret.executeQuery();
			while (rs.next()) {
				iev.addReturnProduct(new InventoryEventReturnVO(rs, false));
			}

			// Retrieve Customer Information
			cust = conn.prepareStatement(cq.toString());
			cust.setInt(1, iev.getInventoryEventId());
			rs = aud.executeQuery();
			while (rs.next()) {
				iev.addEventCustomer(new CustomerEventVO(rs, false));
			}
		} catch (SQLException sqle) {
			log.error(sqle);
		} finally {
			DBUtil.close(aud);
			DBUtil.close(ret);
			DBUtil.close(cust);
		}
	}

	/**
	 * Method is responsible for building the return items, storing them in the
	 * database, updating the eventVO with the resulting list of values and also
	 * returning a list of the inventoryItems the returns are based on for use
	 * later in inserting the inventoryItem records.
	 * 
	 * @param groups
	 * @param prods
	 * @return
	 * @throws ActionException
	 */
	private List<InventoryItemVO> generateReturns(InventoryEventVO eventVO,
			List<InventoryItemVO> prods) throws ActionException {
		List<InventoryItemVO> returns = new ArrayList<InventoryItemVO>();
		PreparedStatement ps = null;
		int prodSize = prods.size();
		try {
			for (int i = 0; i < NUM_RECALLS; i++) {
				InventoryItemVO ret = prods.get(r.nextInt(prodSize));
				returns.add(ret);
				InventoryEventReturnVO er = getReturn(
						eventVO.getInventoryEventId(), ret);
				ps = conn.prepareStatement(buildEventReturnInsertRecord()
						.toString(), Statement.RETURN_GENERATED_KEYS);
				ps.setInt(1, er.getInventoryEventId());
				ps.setInt(2, er.getProductId());
				ps.setInt(3, er.getQuantity());
				ps.setString(4, er.getLotNumber());
				ps.setInt(5, er.getActiveFlag());
				ps.setTimestamp(6, Convert.getCurrentTimestamp());
				ps.executeUpdate();

				// Get the identity column id on an insert
				ResultSet generatedKeys = ps.getGeneratedKeys();
				if (generatedKeys.next())
					er.setEventReturnId(generatedKeys.getInt(1));

				eventVO.addReturnProduct(er);
			}
		} catch (SQLException sqle) {
			throw new ActionException("", sqle);
		} finally {
			DBUtil.close(ps);
		}
		return returns;
	}

	/**
	 * This method builds a generic eventReturnVO based off the given
	 * inventoryItemvO and eventId.
	 * 
	 * @param inventoryEventId
	 * @param inventoryItemVO
	 * @return
	 */
	private InventoryEventReturnVO getReturn(Integer inventoryEventId,
			InventoryItemVO i) {
		InventoryEventReturnVO er = new InventoryEventReturnVO();
		er.setInventoryEventId(inventoryEventId);
		er.setProductId(i.getProductId());
		er.setQuantity(r.nextInt(5) + 1);
		er.setLotNumber(i.getLotNumber());
		er.setActiveFlag(1);
		return er;
	}

	/**
	 * This method builds and stores the InventoryEventAuditor records to the db
	 * and sets the resulting vo on the event.
	 * 
	 * @param groups
	 * @throws ActionException
	 */
	private void generateAuditors(InventoryEventVO eventVO)
			throws ActionException {
		PreparedStatement ps = null;

		try {
			// Generate basic random auditorVO
			InventoryEventAuditorVO auditor = getAuditor(eventVO
					.getInventoryEventId());

			// Store it to the db.
			ps = conn.prepareStatement(buildAuditorInsertRecord().toString(),
					Statement.RETURN_GENERATED_KEYS);
			ps.setInt(1, auditor.getInventoryEventId());
			ps.setInt(2, auditor.getAuditor().getAuditorId());
			ps.setInt(3, auditor.getEventLeaderFlag());
			ps.setInt(4, auditor.getActiveFlag());
			ps.setTimestamp(5, Convert.getCurrentTimestamp());
			ps.executeUpdate();

			// Get the identity column id on an insert
			ResultSet generatedKeys = ps.getGeneratedKeys();
			if (generatedKeys.next())
				auditor.setInventoryEventAuditorId(generatedKeys.getInt(1));

			// Add vo back to the eventVO.
			eventVO.addAuditor(auditor);
		} catch (SQLException sqle) {
			throw new ActionException("", sqle);
		} finally {
			DBUtil.close(ps);
		}
	}

	/**
	 * This method generates a random base AuditorVO based on eventId.
	 * 
	 * @return
	 */
	private InventoryEventAuditorVO getAuditor(int eventId) {

		// Auditor Ids from db.
		int[] auditorIds = { 2, 8, 9, 13, 14 };

		// Create AuditorVO and assign random auditor from list above.
		AuditorVO a = new AuditorVO();
		a.setAuditorId(auditorIds[r.nextInt(auditorIds.length)]);

		// build event AuditorVO and set auditor along with basic info.
		InventoryEventAuditorVO auditor = new InventoryEventAuditorVO();
		auditor.setInventoryEventId(eventId);
		auditor.setActiveFlag(1);
		auditor.setEventLeaderFlag(1);
		auditor.setAuditor(a);
		return auditor;
	}

	/**
	 * This method adds our customer records to the db.
	 * 
	 * @param eventVO
	 * @param groups
	 * @throws ActionException
	 */
	private void generateCustomers(InventoryEventVO eventVO)
			throws ActionException {
		PreparedStatement ps = null;

		// Generate base customerVO
		CustomerEventVO c = getCustomer(eventVO.getInventoryEventId());

		// Insert record to database
		try {
			ps = conn.prepareStatement(buildCustomerInsertRecord().toString());
			ps.setInt(1, c.getInventoryEventId());
			ps.setInt(2, c.getCustomerId()); // Depuy Orthopaedics
			ps.setInt(3, c.getActiveFlag());
			ps.setTimestamp(4, Convert.getCurrentTimestamp());
			ps.execute();
		} catch (SQLException sqle) {
			throw new ActionException("", sqle);
		} finally {
			DBUtil.close(ps);
		}

		// store customer record on eventVO
		eventVO.addEventCustomer(c);
	}

	/**
	 * This method generates a default customer vo for a given inventoryeventId
	 * 
	 * @param inventoryEventId
	 * @return
	 */
	private CustomerEventVO getCustomer(Integer inventoryEventId) {
		CustomerEventVO c = new CustomerEventVO();
		c.setActiveFlag(1);
		c.setInventoryEventId(inventoryEventId);
		c.setCustomerId(9230);
		return c;
	}

	/**
	 * This method is responsible for generating initial events for each group.
	 * 
	 * @param groups
	 * @return
	 */
	private void generateEvents(Map<String, InventoryEventGroupVO> groups) {

		Calendar c = Calendar.getInstance();
		StringBuilder sql = new StringBuilder();
		sql.append("insert into ").append(schema)
				.append("ram_inventory_event ");
		sql.append("(inventory_event_group_id, customer_location_id, comment_txt, ");
		sql.append("schedule_dt, active_flg, vendor_event_id, create_dt) ");
		sql.append("values (?,?,?,?,?,?,?) ");

		/*
		 * For each Event group, build and event, insert it and then update the
		 * event with the generated key from the db.
		 */
		c.set(2014, 2, 1);
		for (String locId : groups.keySet()) {

			// Build Event
			InventoryEventGroupVO g = groups.get(locId);
			List<InventoryEventVO> events = new ArrayList<InventoryEventVO>();
			InventoryEventVO e = new InventoryEventVO();
			e.setCustomerLocationId(Convert.formatInteger(locId));
			e.setComment(locId + " bulk Insert");
			e.setScheduleDate(c.getTime());
			e.setVendorEventId(locId + "-vendor");
			e.setActiveFlag(1);
			e.setPartialInventoryFlag(1);
			e.setInventoryEventGroupId(g.getInventoryEventGroupId());

			// Insert Event
			PreparedStatement ps = null;
			try {
				ps = conn.prepareStatement(sql.toString(),
						Statement.RETURN_GENERATED_KEYS);
				ps.setString(1, e.getInventoryEventGroupId());
				ps.setInt(2, e.getCustomerLocationId());
				ps.setString(3, e.getComment());
				ps.setTimestamp(4, Convert.formatTimestamp(e.getScheduleDate()));
				ps.setInt(5, e.getActiveFlag());
				ps.setString(6, e.getVendorEventId());
				ps.setTimestamp(7, Convert.getCurrentTimestamp());
				ps.executeUpdate();

				// Get the identity column id on an insert
				ResultSet generatedKeys = ps.getGeneratedKeys();
				if (generatedKeys.next())
					e.setInventoryEventId(generatedKeys.getInt(1));
			} catch (SQLException sqle) {
				log.error(sqle);
			} finally {
				DBUtil.close(ps);
			}

			// Add to list and set list on group.
			events.add(e);
			g.setEvents(events);
		}
	}

	/**
	 * This method reads through the locations file and returns a list of all
	 * customer locations we want to generate data for.
	 * 
	 * @param bs
	 * @return
	 * @throws IOException
	 */
	private List<String> loadLocations(byte[] data) throws IOException {
		List<String> locs = new ArrayList<String>();
		BufferedReader inData = new BufferedReader(new InputStreamReader(
				new ByteArrayInputStream(data)));
		String temp = inData.readLine();
		while (temp != null && temp.length() > 0) {
			locs.add(temp.trim());
			temp = inData.readLine();
		}
		return locs;
	}

	/**
	 * This method reads through the products file, parsing out each row and
	 * retrieving any necessary additional data.
	 * 
	 * @param bs
	 * @return
	 * @throws IOException
	 */
	private List<InventoryItemVO> loadProducts(byte[] data) throws IOException {
		List<InventoryItemVO> prods = new ArrayList<InventoryItemVO>();
		BufferedReader inData = new BufferedReader(new InputStreamReader(
				new ByteArrayInputStream(data)));
		String temp = null;
		int i = 0;

		// For each line, retreive the productId and add to prods list.
		for (i = 0; (temp = inData.readLine()) != null; i++) {
			if (1 > 0 && i % 100 == 0)
				log.debug("Parsed " + i + " products");
			String[] row = temp.split(",");
			InventoryItemVO v = getInvItemVO(row);
			prods.add(getProductId(v));
		}
		log.debug(i + " products read.");

		// Set total number of product records.
		tp = i;
		return prods;
	}

	/**
	 * This method builds an inventory Item vo with the basic information and
	 * determines the default item type based on expiration date.
	 * 
	 * @param row
	 * @return
	 */
	private InventoryItemVO getInvItemVO(String[] row) {
		// Build the product VO and add to the collection
		InventoryItemVO c = new InventoryItemVO();
		c.setCustomerProductId(row[0]);
		c.setLotNumber(row[1]);
		Date d = Convert.parseDateUnknownPattern(row[2]);
		c.setExpirationDate(d);
		c.setQuantity(Convert.formatInteger(row[3]));
		c.setItemType(InventoryItemType.SHELF);
		if (c.getExpirationDate().before(Convert.getCurrentTimestamp()))
			c.setItemType(InventoryItemType.EXPIREE_RETURN);

		return c;
	}

	/**
	 * Retrieve the productId from the database. The spreadsheet we received has
	 * customer Product ids so this retrieves the system ProductId for each and
	 * updates the vo.
	 * 
	 * @param custProducts
	 * @return
	 */
	private InventoryItemVO getProductId(InventoryItemVO v) {
		StringBuilder sb = new StringBuilder();
		sb.append("select PRODUCT_ID, CUST_PRODUCT_ID from ").append(schema);
		sb.append("RAM_PRODUCT where cust_product_id = ?");

		PreparedStatement s = null;
		try {
			s = conn.prepareStatement(sb.toString());
			s.setString(1, v.getCustomerProductId());
			ResultSet rs = s.executeQuery();
			if (rs.next()) {
				v.setProductId(rs.getInt(1));
			}
		} catch (SQLException sqle) {
			log.error(sqle);
		} finally {
			DBUtil.close(s);
		}
		return v;
	}

	/**
	 * Generate the Event groups for the list of Customer Locations provided.
	 * 
	 * @return
	 * @throws com.siliconmtn.db.util.DatabaseException
	 * @throws InvalidDataException
	 */
	private Map<String, InventoryEventGroupVO> generateEventGroups(
			List<String> locs) throws InvalidDataException,
			com.siliconmtn.db.util.DatabaseException {

		// Map to store our completed groups.
		Map<String, InventoryEventGroupVO> groups = new HashMap<String, InventoryEventGroupVO>();

		// predefined recurrence schedules
		int[] mwf = { 1, 0, 1, 0, 1 };
		int[] tr = { 0, 1, 0, 1, 0 };
		int[] temp;

		// Start Date of February 1, 2014
		Calendar c = Calendar.getInstance();
		c.set(2014, 2, 1);
		String[] dTime = getTime();

		/*
		 * For each location, randomly choose a schedule and default time and
		 * add to db and our groups map.
		 */
		for (String s : locs) {
			temp = mwf;
			if (r.nextBoolean())
				temp = tr;
			InventoryEventGroupVO v = new InventoryEventGroupVO();
			v.setCreateDate(Convert.getCurrentTimestamp());
			v.setDefaultTime(dTime[r.nextInt(dTime.length)]);
			v.setMondayFlag(temp[0]);
			v.setTuesdayFlag(temp[1]);
			v.setWednesdayFlag(temp[2]);
			v.setThursdayFlag(temp[3]);
			v.setFridayFlag(temp[4]);
			v.setTotalWeek(TOTAL_WEEKS);
			v.setInventoryEventGroupId("BULK_EVENT_LOC_" + s);

			db.insert(v);
			groups.put(s, v);
		}
		return groups;
	}

	/**
	 * Retrieves the file data from the file system or a web server for download
	 * and processing
	 * 
	 * @return
	 * @throws IOException
	 */
	public byte[] retrieveFileData(String loc) throws IOException {
		byte[] b = null;
		BufferedReader data = new BufferedReader(new FileReader(loc));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int c;
		while ((c = data.read()) > -1) {
			baos.write(c);
		}

		b = baos.toByteArray();
		data.close();
		baos.flush();
		baos.close();

		log.info("File Size: " + b.length);
		return b;
	}

	/**
	 * Get the DBConnection
	 * 
	 * @return
	 * @throws DatabaseException
	 * @throws InvalidDataException
	 */
	public Connection getConnection() {
		DatabaseConnection dbc = new DatabaseConnection();
		dbc.setDriverClass(config.getProperty("dbDriver"));
		dbc.setUrl(config.getProperty("dbUrl"));
		dbc.setUserName(config.getProperty("dbUser"));
		dbc.setPassword(config.getProperty("dbPassword"));
		try {
			return dbc.getConnection();
		} catch (Exception e) {
			log.error("Unable to get a DB Connection", e);
			System.exit(-1);
		}

		return null;
	}

	/**
	 * @return the conn
	 */
	public Connection getDatabaseConnection() {
		return conn;
	}

	/**
	 * @param conn
	 *            the conn to set
	 */
	public void setDatabaseConnection(Connection conn) {
		this.conn = conn;
	}

	/**
	 * Builds the Auditor insert statement
	 * 
	 * @return
	 */
	protected StringBuilder buildAuditorInsertRecord() {
		StringBuilder sql = new StringBuilder();
		sql.append("insert into ").append(schema)
				.append("ram_inventory_event_auditor_xr ");
		sql.append("(inventory_event_id, auditor_id, event_leader_flg, ");
		sql.append("active_flg, create_dt) values(?,?,?,?,?)");

		return sql;
	}

	/**
	 * Builds the Customer insert statement
	 * 
	 * @return
	 */
	protected StringBuilder buildCustomerInsertRecord() {
		StringBuilder sql = new StringBuilder();
		sql.append("insert into ").append(schema)
				.append("ram_customer_event_xr ");
		sql.append("(inventory_event_id, customer_id, active_flg, create_dt) ");
		sql.append("values(?,?,?,?)");

		return sql;
	}

	/**
	 * Builds the Event Return insert statement
	 * 
	 * @return
	 */
	protected StringBuilder buildEventReturnInsertRecord() {
		StringBuilder sql = new StringBuilder();
		sql.append("insert into ").append(schema)
				.append("ram_event_return_xr ");
		sql.append("(inventory_event_id, product_id, quantity_no, lot_number_txt, ");
		sql.append("active_flg, create_dt) values(?,?,?,?,?,?)");

		return sql;
	}

	/**
	 * Easy way to retieve valid dates that are on the front end. Used for
	 * recurrence.
	 * 
	 * @return
	 */
	private String[] getTime() {
		return new String[] { "8:00 AM", "8:15 AM", "8:30 AM", "8:45 AM",
				"9:00 AM", "9:15 AM", "9:30 AM", "9:45 AM", "10:00 AM",
				"10:15 AM", "10:30 AM", "10:45 AM", "11:00 AM", "11:15 AM",
				"11:30 AM", "11:45 AM", "1:00 PM", "1:15 PM", "1:30 PM",
				"1:45 PM", "2:00 PM", "2:15 PM", "2:30 PM", "2:45 PM",
				"3:00 PM", "3:15 PM", "3:30 PM", "3:45 PM", "4:00 PM",
				"4:15 PM", "4:30 PM", "4:45 PM", "5:00 PM", "5:15 PM",
				"5:30 PM", "5:45 PM", "6:00 PM" };
	}

}
