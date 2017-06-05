package com.depuysynthes.scripts.showpad;

import java.io.IOException;

import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: AssetDeleter.java<p/>
 * <b>Description</b>: Used to delete assets from the remote Showpad account by ID, which 
 * means we don't need to worry about division.  Target assetIds come from the config file.
 * 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Mar 25, 2017
 ****************************************************************************/
public class AssetDeleter extends CommandLineUtil {

	protected ShowpadApiUtil showpadApi;

	/**
	 * @param args
	 * @throws IOException 
	 * @throws QuotaException 
	 */
	public AssetDeleter(String[] args) throws IOException {
		super(args);
		loadProperties("scripts/MediaBin.properties");
		loadDBConnection(props);

		//setup the oAuth util now that the config file has been loaded
		showpadApi = ShowpadApiUtil.makeInstance(props, null);
	}


	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws Exception {
		//Create an instance of the AssetDeleter
		AssetDeleter dmb = new AssetDeleter(args);
		dmb.run();
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	@Override
	public void run() {
		String baseUrl = props.getProperty("showpadApiUrl") + "/assets/";
		// get the list of tags from the config file.  This was better than hard-coding and easier than fetching from a 3rd source
		String[] assetIds = StringUtil.checkVal(props.get("assetIds")).split(",");
		for (String assetId : assetIds) {
			String url = baseUrl + assetId.trim() + ".json";
			try {
				showpadApi.executeDelete(url);
				log.debug("deleted asset " + assetId);
			} catch (IOException e) {
				log.error("could not delete asset with id=" + assetId, e);
			}
		}
	}
}