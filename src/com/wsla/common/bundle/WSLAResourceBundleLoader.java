package com.wsla.common.bundle;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

// SMT Base Libs
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.resource.ResourceBundleDataVO;
import com.siliconmtn.resource.ResourceBundleKeyVO;
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.UUIDGenerator;

/****************************************************************************
 * <b>Title</b>: WSLAResourceBundleLoader.java
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

public class WSLAResourceBundleLoader extends CommandLineUtil {
	
	public static final String BUNDLE = "com.wsla.common.bundle.messages";
	public static final String JS_BUNDLE = "com.wsla.common.bundle.js_bundle";
	
	private static final String BUNDLE_ID = "WSLA_BUNDLE";
	
	
	// Member Variables
	private Map<String, String> keyIdMapper = new HashMap<>();
	
	/**
	 * List of locales to process for the base WC localizations files
	 */
	private static List<Locale> locales = new ArrayList<>();
	static {
		locales.add(new Locale("en", "US"));
		locales.add(new Locale("es", "MX"));
	}
	
	/**
	 * 
	 * @param args
	 */
	public WSLAResourceBundleLoader(String... args) {
		super(args);
		loadProperties("scripts/rb_config.properties");
		loadDBConnection(props);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		WSLAResourceBundleLoader wcrbl = new WSLAResourceBundleLoader(args);
		log.info("Started WSLA Resource Bundle Loader");
		wcrbl.run();
		log.info("Completed WSLA Resource Bundle Loader");
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	@Override
	public void run() {
		DBProcessor db = new DBProcessor(dbConn);
		
		// Get current default keys
		getCurrentKeys(db);
		
		// Loop the locales
		for (Locale locale : locales) {
			log.info("Starting Locale: " + locale);
			
			// Load Resource Bundle
			ResourceBundle rb = ResourceBundle.getBundle(JS_BUNDLE, locale);
					
			// Loop the bundle
			int ctr = 0;
			for (String key : rb.keySet()) {
				ResourceBundleKeyVO keyVo = new ResourceBundleKeyVO();
				keyVo.setKeyCode(key);
				keyVo.setKeyDesc(key);
				keyVo.setValue(rb.getString(key));
				keyVo.setCountry(locale.getCountry());
				keyVo.setLanguage(locale.getLanguage());
				keyVo.setResourceBundleId(BUNDLE_ID);
				
				// Check for existence of key
				if (keyIdMapper.containsKey(key)) {
					keyVo.setKeyId(keyIdMapper.get(key));
				
				} else {
					try {
						// Add a new key
						keyVo.setKeyId(addKey(db, keyVo));
						
						// load the key into the mapper
						keyIdMapper.put(keyVo.getKeyCode(), keyVo.getKeyId());
					} catch (Exception e) {
						log.error("Unable to add key: " + keyVo, e);
					}
				}
				
				// Add the data
				try {
					addKeyValue(db, keyVo);
				} catch (Exception e) {
					log.error("Unable to add key/value: " + keyVo, e);
				}
				
				ctr++;
			}
			
			log.info("Processed " + ctr + " elements\n\n");
		}
	}
	
	/**
	 * Loads the existing keys form the default bundle.  This will allow us to 
	 * assign the proper key id to the items in the base bundle
	 * @param db
	 */
	public void getCurrentKeys(DBProcessor db) {
		String sql = "select key_cd as key, key_id as value ";
		sql += "from resource_bundle_key where resource_bundle_id in ('DEFAULT_BUNDLE', 'WSLA_BUNDLE')";
		
		List<GenericVO> keys = db.executeSelect(sql, null, new GenericVO());
		for (GenericVO key : keys) {
			keyIdMapper.put((String)key.getKey(), (String)key.getValue());
		}
	}
	
	/**
	 * Inserts the country and value as well as key and value into the db
	 * @param db
	 * @param ele
	 * @param locale
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 * @throws Exception
	 */
	public void addKeyValue(DBProcessor db, ResourceBundleKeyVO key) 
	throws InvalidDataException, DatabaseException {
		
		ResourceBundleDataVO data = new ResourceBundleDataVO();
		data.setCountry(key.getCountry());
		data.setLanguage(key.getLanguage());
		data.setKeyId(key.getKeyId());
		data.setValue(key.getValue());
		data.setResourceBundleId(BUNDLE_ID);
		data.setResourceBundleDataId(new UUIDGenerator().getUUID());
		db.insert(data);
	}
	
	/**
	 * Adds the keys to the resource bundle key table.  The keyId matches the keyCode
	 * for simplicity sake
	 * @param db
	 * @param ele
	 * @throws Exception
	 */
	public String addKey(DBProcessor db, ResourceBundleKeyVO key) 
	throws InvalidDataException, DatabaseException  {
		db.insert(key);
		
		return key.getKeyId();
	}
}

