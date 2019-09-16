package com.wsla.util.migration;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
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
	
	/*
	 * the time zone of Mexico City - which is what we'll presume all incoming dates/times to be.
	 * We'll offset (increment) these to UTC prior to saving/using them for accuracy.
	 */
	static final String DEF_TIME_ZONE = "GMT";

	//define the ordered list of importers to run.  This will vary through development but all will run at once for staging/prod.
	static {

//		importers.add(LocationInventory.class.getName()); //deps: InventoryLocation, Product

		importers.add(SOHeader.class.getName());
		importers.add(SOExtendedData.class.getName());
		importers.add(SOComments.class.getName());
		importers.add(SOLineItems.class.getName());

		importers.add(AssetParser.class.getName());
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
			} catch (RuntimeException re) {
				throw re; //pass these up, something bad happened
			} catch (Exception e) {
				log.error("could not run importer " + className, e);
			}
		}
	}

	/**
	 * @param startDate
	 * @return
	 */
	public static Date toUTCDate(Date dt) {
		if (dt == null) {
			return null;
		} else {
			ZonedDateTime z = ZonedDateTime.ofInstant(dt.toInstant(), ZoneId.of(DEF_TIME_ZONE));		
			return Date.from(z.toLocalDateTime().atZone( ZoneId.systemDefault()).toInstant());
		}
	}
}