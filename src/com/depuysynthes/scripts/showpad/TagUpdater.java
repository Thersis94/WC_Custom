package com.depuysynthes.scripts.showpad;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: DSPrivateAssetsImporter.java<p/>
 * <b>Description</b>: Updates existing Showpad tags to add externalId where needed.  One-time use script.
 * EMEA gives us a list of tags in an Excel file to change.  They generally don't consider the many divisions we manage.
 * In leu of that, we load all the divisions, and all the tags for each, and then iterate and update the tags accordingly
 * by using their NAMES as the common thread (across the divisions).
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Nov 30, 2017
 ****************************************************************************/
public class TagUpdater extends CommandLineUtil {

	protected ShowpadApiUtil showpadApi;
	private String externalId;

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
		externalId = props.getProperty("tagExternalId", ShowpadTagManager.SMT_MEDIABIN_EXTERNALID);
		log.info("externalId set as " + externalId + " from config");
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


	/* 
	 * (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	@Override
	public void run() {
		// get the list of tags from the config file.  This was better than hard-coding and easier than fetching from a 3rd source
		String[] tagNames = StringUtil.checkVal(props.get("tagNames")).split(",");
		log.debug("loaded " + tagNames.length + " tags from config");

		List<ShowpadDivisionUtil> divisions = loadDivisions();
		for (ShowpadDivisionUtil util : divisions) {
			Map<String, ShowpadTagVO> tags = util.getTagManager().getShowpadTags();
			log.debug("found " + tags.size() + " tags for division " + util.getDivisionNm());
			for (String name : tagNames) {
				ShowpadTagVO tag = tags.get(name);
				if (tag == null || externalId.equals(tag.getExternalId())) {
					log.debug("tag not present or already set correctly");
					continue;
				}

				String url = props.getProperty("showpadApiUrl") + "/tags/" + tag.getId() + ".json";
				Map<String, String> params = new HashMap<>();
				params.put("externalId", externalId);
				try {
					showpadApi.executePost(url, params);
					log.debug("updated tag " + tag.getId());
				} catch (IOException e) {
					log.error("could not update tag with id=" + tag.getId(), e);
				}
			}

			log.info("finished division " + util.getDivisionNm());
		}
	}


	/**
	 * load a list of divisions we need to audit tags for.  Note the constructors here-in will 
	 * load a full set of tags into each Division for us.
	 * @return
	 */
	private List<ShowpadDivisionUtil> loadDivisions() {
		List<ShowpadDivisionUtil> divisions = new ArrayList<>();
		String[] divs = props.getProperty("showpadDivisions").split(",");
		for (String d : divs) {
			String[] div = d.split("=");
			divisions.add(new ShowpadDivisionUtil(props, div[1], div[0], showpadApi, dbConn));
			log.debug("created division " + div[0] + " with id " + div[1]);
		}
		log.info("loaded " + divisions.size() + " showpad divisions from config");
		return divisions;
	}
}