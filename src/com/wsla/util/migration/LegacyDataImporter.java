package com.wsla.util.migration;

import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.SMTClassLoader;

/****************************************************************************
 * <p><b>Title:</b> ImportInventoryLedger.java</p>
 * <p><b>Description:</b> Imports the DM-Inventory & Ledger Excel file furnished by Steve.</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Jan 7, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class LegacyDataImporter extends CommandLineUtil {

	private static final List<String> importers = new ArrayList<>();

	//define the ordered list of importers to run.  This will vary through development but all will run at once for staging/prod.
	static {
//		importers.add(OEMProvider.class.getName());
		//TODO likely need to stub-in OEM locations? ("return to manuf" use case)
//		importers.add(Product.class.getName()); //deps: OEMProvider
//		importers.add(Category.class.getName()); //deps: Product
//		importers.add(CASProvider.class.getName());
//		importers.add(CASLocation.class.getName()); //deps: CASProvider
//		importers.add(ProductSerial.class.getName()); //deps: Product
//		importers.add(ProductSet.class.getName()); //deps: ProductSerial
//		importers.add(RetailProvider.class.getName());
		//TODO RetailLocation - all the Wal-Marts, Home Depots, etc.
//		importers.add(WSLAInventoryLocation.class.getName()); //deps: RetailProvider, CASLocation
//TODO		importers.add(ClosedInventory.class.getName()); //deps: ProductSerial, CASLocation
//		importers.add(WSLAStaff.class.getName()); //WSLA Staff, WSLA's default provider location

		importers.add(SOHeader.class.getName());
		importers.add(SOLineItems.class.getName());
		importers.add(SOExtendedData.class.getName());
		/*
		 * TODO:
			Ticket
			TicketComment // N/A
			TicketAssignment // N/A
			TicketSchedule? // N/A
			Diagnostic (what needs to be preloaded - extracts from tickets?)
			TicketDiagnostic
			Attribute (also extracts?)
			TicketAttribute? XDD file
		 */
	}


	/**
	 * @param args
	 */
	public LegacyDataImporter(String[] args) {
		super(args);
		loadProperties("scripts/wsla_migration.properties");
		loadDBConnection(props);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		LegacyDataImporter clazz = new LegacyDataImporter(args);
		clazz.run();
		clazz.closeDBConnection();
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	@Override
	public void run() {
		//loop through the importers we were asked to run
		for (String className : importers) {
			try {
				AbsImporter importer = (AbsImporter) SMTClassLoader.getClassInstance(className);
				importer.setAttributes(dbConn, props, args);
				importer.run();
				log.info("completed " + className + "\r\r\r");
			} catch (Exception e) {
				log.error("could not run importer " + className, e);
			}
		}
	}
}