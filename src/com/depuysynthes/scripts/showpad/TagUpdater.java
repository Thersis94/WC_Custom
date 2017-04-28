package com.depuysynthes.scripts.showpad;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: DSPrivateAssetsImporter.java<p/>
 * <b>Description</b>: Updates existing Showpad tags to add externalId where needed.  One-time use script.
 * The list of tagIds to update comes from the config file, but first from the Showpad API.  Steps to obtain: 
 * 1) open Showpad's API explorer
 * 2) Search for all Tags, fields "id,name,externalId"
 * 3) copy Json array of tags
 * 4) paste the data here, to convert to CSV: http://www.convertcsv.com/json-to-csv.htm
 * 5) paste CSV to Excel
 * 6) identify the rows that need to be updated.  Typically the customer does this step.
 * 7) paste IDs into text editor, and replace \n with ,
 * 8) paste single-line output into the MediaBin.properties file, propertyName=tagIds
 * 9) run this script from the command line or through Eclipse.
 * 
 * Note: Tags within the ACCOUNT can be updated by ID without a Division scope, so the above steps ensure only tags 
 * 		we want to update are impacted.
 * 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Mar 14, 2017
 ****************************************************************************/
public class TagUpdater extends CommandLineUtil {

	protected ShowpadApiUtil showpadApi;

	/**
	 * @param args
	 * @throws IOException 
	 * @throws QuotaException 
	 */
	public TagUpdater(String[] args) throws IOException {
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
		//Create an instance of the MedianBinImporter
		TagUpdater dmb = new TagUpdater(args);
		dmb.run();
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	@Override
	public void run() {
		// get the list of tags from the config file.  This was better than hard-coding and easier than fetching from a 3rd source
		String[] tagIds = StringUtil.checkVal(props.get("tagIds")).split(",");
		for (String tagId : tagIds) {
			String url = props.getProperty("showpadApiUrl") + "/tags/" + tagId + ".json";
			Map<String, String> params = new HashMap<>();
			params.put("externalId", ShowpadTagManager.SMT_MEDIABIN_EXTERNALID);
			try {
				showpadApi.executePost(url, params);
				log.debug("updated tag " + tagId);
			} catch (IOException e) {
				log.error("could not update tag with id=" + tagId, e);
			}
		}
	}
}