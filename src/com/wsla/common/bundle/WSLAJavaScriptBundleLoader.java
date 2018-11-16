package com.wsla.common.bundle;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

// SMT Base Libs
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.CommandLineUtil;

/****************************************************************************
 * <b>Title</b>: WSLAJavaScriptBundleLoader.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Batch process to load the resource bundles into the Database
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Nov 9, 2018
 * @updates:
 ****************************************************************************/
public class WSLAJavaScriptBundleLoader extends CommandLineUtil {

	private static final String BUNDLE = "com.wsla.common.bundle.js_bundle";
	
	/**
	 * List of js bundles to process for the base WC localizations files
	 */
	private static List<Locale> locales = new ArrayList<>();
	static {
		locales.add(new Locale("en", "US"));
	}

	/**
	 * @param args
	 */
	public WSLAJavaScriptBundleLoader(String[] args) {
		super(args);
		loadProperties("scripts/rb_config.properties");
		loadDBConnection(props);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		WSLAJavaScriptBundleLoader jsbl = new WSLAJavaScriptBundleLoader(args);
		log.info("Started WSLA Resource Bundle Loader");
		jsbl.run();
		log.info("Completed WSLA Resource Bundle Loader");
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	@Override
	public void run() {
		// Load the DB Processor
		DBProcessor db = new DBProcessor(dbConn);
		
		for (Locale locale : locales) {
			ResourceBundle rb = ResourceBundle.getBundle(BUNDLE, locale);
			
			for (String key : rb.keySet()) {
				log.info(key);
			}
		}
	}

}
