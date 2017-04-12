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
 * <b>Title</b>: AccountReplicator.java<p/>
 * <b>Description</b>: Replicates assets, tags, and their relationship
 * from one Showpad account/Division to another.  Used to consolidate legacy
 * Showpad accounts into Divisions within the global jnjemea Showpad account.
 * 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Apr 12, 2017
 ****************************************************************************/
public class AccountReplicator extends CommandLineUtil {

	protected ShowpadApiUtil srcApi;
	protected ShowpadApiUtil destApi;

	/**
	 * @param args
	 * @throws IOException 
	 * @throws QuotaException 
	 */
	public AccountReplicator(String[] args) throws IOException {
		super(args);
		loadProperties("scripts/MediaBin.properties");
		loadDBConnection(props);

		//setup oAuth util for the target account
		Map<Config, String> config = new HashMap<>();
		config.put(Config.USER_ID, props.getProperty("showpadAcctName"));
		config.put(Config.API_KEY, props.getProperty("showpadApiKey"));
		config.put(Config.API_SECRET, props.getProperty("showpadApiSecret"));
		config.put(Config.TOKEN_CALLBACK_URL, props.getProperty("showpadCallbackUrl"));
		config.put(Config.TOKEN_SERVER_URL, props.getProperty("showpadTokenUrl"));
		config.put(Config.AUTH_SERVER_URL,  props.getProperty("showpadAuthUrl"));
		config.put(Config.KEYSTORE, "showpad-" + StringUtil.removeNonAlphaNumeric(config.get(Config.USER_ID)));
		List<String> scopes = Arrays.asList(props.getProperty("showpadScopes").split(","));
		destApi = new ShowpadApiUtil(new OAuth2TokenViaCLI(config, scopes));
		
		//setup OAuth token for the source account
		Map<Config, String> config2 = new HashMap<>();
		config2.put(Config.USER_ID, props.getProperty("src-showpadAcctName"));
		config2.put(Config.API_KEY, props.getProperty("src-showpadApiKey"));
		config2.put(Config.API_SECRET, props.getProperty("src-showpadApiSecret"));
		config2.put(Config.TOKEN_CALLBACK_URL, props.getProperty("src-showpadCallbackUrl"));
		config2.put(Config.TOKEN_SERVER_URL, props.getProperty("src-showpadTokenUrl"));
		config2.put(Config.AUTH_SERVER_URL,  props.getProperty("src-showpadAuthUrl"));
		config2.put(Config.KEYSTORE, "showpad-" + StringUtil.removeNonAlphaNumeric(config.get(Config.USER_ID)));
		List<String> scopes2 = Arrays.asList(props.getProperty("showpadScopes").split(","));
		srcApi = new ShowpadApiUtil(new OAuth2TokenViaCLI(config2, scopes2));
	}


	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws Exception {
		//Create an instance of the MedianBinImporter
		AccountReplicator dmb = new AccountReplicator(args);
		dmb.run();
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	@Override
	public void run() {
		//get all the tags from the source account
		Map<String, ShowpadTagVO> showpadTags;
		
		//get all the tags form the dest account
		
		//replicate needed tags from the source account to the dest account
		
		//get all assets from the source account
		
		//get all assets from the dest account
		
		//replicate needed assets from the source account to the dest account
		
	}
}








