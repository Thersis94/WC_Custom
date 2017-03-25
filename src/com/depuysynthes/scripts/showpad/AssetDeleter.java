package com.depuysynthes.scripts.showpad;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.security.OAuth2TokenViaCLI;
import com.siliconmtn.security.OAuth2TokenViaCLI.Config;
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
		Map<Config, String> config = new HashMap<>();
		config.put(Config.USER_ID, props.getProperty("showpadAcctName"));
		config.put(Config.API_KEY, props.getProperty("showpadApiKey"));
		config.put(Config.API_SECRET, props.getProperty("showpadApiSecret"));
		config.put(Config.TOKEN_CALLBACK_URL, props.getProperty("showpadCallbackUrl"));
		config.put(Config.TOKEN_SERVER_URL, props.getProperty("showpadTokenUrl"));
		config.put(Config.AUTH_SERVER_URL,  props.getProperty("showpadAuthUrl"));
		config.put(Config.KEYSTORE, "showpad");
		List<String> scopes = Arrays.asList(props.getProperty("showpadScopes").split(","));
		showpadApi = new ShowpadApiUtil(new OAuth2TokenViaCLI(config, scopes));
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
				log.debug("deleted asset " + assetIds);
			} catch (IOException e) {
				log.error("could not delete asset with id=" + assetId, e);
			}
		}
	}
}