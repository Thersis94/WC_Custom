package com.depuysynthes.scripts.showpad;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.depuysynthes.scripts.DSMediaBinImporterV2;
import com.depuysynthes.scripts.MediaBinDeltaVO;
import com.depuysynthes.scripts.MediaBinDeltaVO.State;
import com.siliconmtn.io.FileType;
import com.siliconmtn.security.OAuth2TokenViaCLI;
import com.siliconmtn.security.OAuth2TokenViaCLI.Config;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: ShowpadMediaBinDecorator.java<p/>
 * <b>Description: Extends the MediaBin importer to record and push assets to Showpad at the same time.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Oct 30, 2015
 ****************************************************************************/
public class ShowpadMediaBinDecorator extends DSMediaBinImporterV2 {

	protected ShowpadApiUtil showpadApi;
	protected List<ShowpadDivisionUtil> divisions = new ArrayList<>();
	private boolean deduplicate = false; //override via args[1];

	/**
	 * @param args
	 * @throws IOException 
	 */
	public ShowpadMediaBinDecorator(String[] args) throws IOException {
		super(args);

		//setup the oAuth util now that the config file has been loaded
		showpadApi = new ShowpadApiUtil(new OAuth2TokenViaCLI(new HashMap<Config, String>(){
			private static final long serialVersionUID = -8625615784451892590L;
			{
				put(Config.USER_ID, props.getProperty("showpadAcctName"));
				put(Config.API_KEY, props.getProperty("showpadApiKey"));
				put(Config.API_SECRET, props.getProperty("showpadApiSecret"));
				put(Config.TOKEN_CALLBACK_URL, props.getProperty("showpadCallbackUrl"));
				put(Config.TOKEN_SERVER_URL, props.getProperty("showpadTokenUrl"));
				put(Config.AUTH_SERVER_URL,  props.getProperty("showpadAuthUrl"));
				put(Config.KEYSTORE, "showpad");
			}}, Arrays.asList(props.getProperty("showpadScopes").split(","))));

		if (args.length > 1)
			deduplicate = Convert.formatBoolean(args[1]); //args[0] passes 'type' to the superclass, so we'll use args[1] here
	}


	public static void main(String[] args) throws Exception {
		//Create an instance of the MedianBinImporter
		ShowpadMediaBinDecorator dmb = new ShowpadMediaBinDecorator(args);
		dmb.run();
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	@Override
	public void run() {
		//load the divisions
		try {
			loadShowpadDivisionList();
		} catch (QuotaException qe) {
			log.error(qe);
		}

		//used only for de-duplication, which is more crisis-cleanup than something we do regularly.
		if (deduplicate) {
			Map<String, MediaBinDeltaVO> records = loadManifest();
			Set<String> assetNames = new HashSet<>(records.size());
			Set<String> localShowpadIds = new HashSet<>(records.size());
			
			for (MediaBinDeltaVO vo : records.values()) {
				assetNames.add(ShowpadDivisionUtil.makeShowpadAssetName(vo, new FileType(vo.getFileNm())));
				if (vo.getShowpadId() != null) localShowpadIds.add(vo.getShowpadId());
			}
			
			try {
				for (ShowpadDivisionUtil util : divisions)
					util.cleanupShowpadDups(assetNames, localShowpadIds);
			} catch (QuotaException qe) {
				log.error(qe);
			}
			log.info("deuplication complete");
			return;
		}
		
		super.run();
	}



	/**
	 * Load a list of tags already at Showpad
	 * If we try to add a tag to an asset without using it's ID, and it already existing in the system, it will fail.
	 * @throws QuotaException 
	 */
	private void loadShowpadDivisionList() throws QuotaException {
		String[] divs = props.getProperty("showpadDivisions").split(",");
		for (String d : divs) {
			String[] div = d.split("=");
			divisions.add(new ShowpadDivisionUtil(props, div[1], div[0], showpadApi, dbConn));
		}
		log.info("loaded " + divisions.size() + " showpad divisions");
	}


	/**
	 * overloaded to include the showpad DB table.
	 * retrieve the showpad data and store it into a Map for each Division
	 * @param type
	 * @return
	 */
	@Override
	protected Map<String,MediaBinDeltaVO> loadManifest() {
		StringBuilder sql = new StringBuilder(250);
		sql.append("select division_id, asset_id, dpy_syn_mediabin_id ");
		sql.append("from ").append(props.get(Constants.CUSTOM_DB_SCHEMA)).append("DPY_SYN_SHOWPAD ");
		log.debug(sql);

		Map<String, Map<String, String>> divisionAssets = new HashMap<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String division = rs.getString(1);
				Map<String, String> assets = divisionAssets.get(division);
				if (assets == null) assets = new HashMap<>();
				assets.put(rs.getString(3), rs.getString(2));
				divisionAssets.put(division, assets);
			}
		} catch (SQLException sqle) {
			log.error("could not load showpad assets from DB", sqle);
		}
		log.info("loaded " + divisionAssets.size() + " divisions from the database");

		//marry the divisionAssets to their respective util object
		for (String divId : divisionAssets.keySet()) {
			for (ShowpadDivisionUtil util : divisions) {
				if (util.getDivisionId().equals(divId)) {
					util.setDivisionAssets(divisionAssets.get(divId));
					log.info("gave " + divisionAssets.get(divId).size() + " assets to division=" + divId);
					break;
				}
			}
		}

		//lean on the superclass to load the roster of assets
		return super.loadManifest();
	}


	/**
	 * override the saveRecords method to push the records to Showpad after 
	 * super.saveRecords() saves them to the database.
	 */
	@Override
	public void saveRecords(Map<String, MediaBinDeltaVO> masterRecords, boolean isInsert) {
		super.saveRecords(masterRecords, isInsert);

		//the below logic will process both inserts & updates at once.  
		//Block here for updates so we don't process the records twice.
		//Insert runs after deletes & updates, so wait for the 'inserts' invocation so 
		//all the mediabin records are already in our database.
		if (!isInsert) return;

		//confirm we have something to add or update
		if (getDataCount("inserted") == 0 && getDataCount("updated") == 0) return;


		//push all changes to Showpad
		outer: for (MediaBinDeltaVO vo : masterRecords.values()) {
			//we need to sort out what gets pushed to Showpad on our own.
			//if it's failed, being deleted, or unchanged and already in Showpad, skip it.
			State s = vo.getRecordState();
			if (s == State.Failed || s == State.Delete)
				continue;

			for (ShowpadDivisionUtil util : divisions) {
				try {
					util.pushAsset(vo);					
				} catch (QuotaException qe) {
					String msg = makeMessage(vo, "Could not push file to showpad: " + qe.getMessage());
					failures.add(new Exception(msg));
					log.error("could not push to showpad, quota reached", qe);
					break outer;
				}
			}
			log.info("completed: " + vo.getFileNm());
		}

		//process the ticket queue for each division
		try {
			for (ShowpadDivisionUtil util : divisions)
				util.processTicketQueue();
		} catch (QuotaException qe) {
			failures.add(qe);
			log.error("could not process showpad queue, quota limit reached", qe);
		}

		//save the newly created records to our database for each division
		for (ShowpadDivisionUtil util : divisions)
			util.saveDBRecords();
	}



	/**
	 * override the deleteRecords methods to push deletions to Showpad after
	 * super.deleteRecords() saves them to the database.
	 */
	@Override
	public void deleteRecords(Map<String, MediaBinDeltaVO> masterRecords) {
		super.deleteRecords(masterRecords);

		//confirm we have something to delete
		if (getDataCount("deleted") == 0) return;

		int cnt = 0;
		// Build the SQL Statement
		StringBuilder sql = new StringBuilder(350);
		sql.append("delete from ").append(props.getProperty(Constants.CUSTOM_DB_SCHEMA)).append("dpy_syn_showpad ");
		sql.append("where dpy_syn_mediabin_id in ('~'");
		for (MediaBinDeltaVO vo : masterRecords.values()) {
			if (vo.getRecordState() == State.Delete) {
				sql.append(",?");
				++cnt;
			}
		}
		sql.append(")");
		log.debug(sql);

		if (cnt > 0) { //don't run the query if we don't need to
			cnt = 1;
			try (PreparedStatement ps  = dbConn.prepareStatement(sql.toString())) {
				for (MediaBinDeltaVO vo : masterRecords.values()) {
					if (vo.getRecordState() == State.Delete)  {
						try {
							//push deletions to Showpad
							for (ShowpadDivisionUtil util : divisions)
								util.deleteAsset(vo);

							//if success, delete it from the DB as well
							ps.setString(cnt++, vo.getDpySynMediaBinId());

						} catch (QuotaException qe) {
							String msg = makeMessage(vo, "Could not delete file from showpad: " + qe.getMessage());
							failures.add(new Exception(msg));
							log.error("could not delete from showpad", qe);
							break;
						}
					}
				}
				cnt = ps.executeUpdate();
			} catch (SQLException sqle) {
				failures.add(sqle);
			}
		}
	}


	/**
	 * returns a count of the database records; called after we finish our updates to verify total
	 * @param type
	 * @return
	 */
	@Override
	protected void countDBRecords() {
		super.countDBRecords();

		int cnt = 0;
		StringBuilder sql = new StringBuilder(100);
		sql.append("select count(*), division_id from ");
		sql.append(props.get(Constants.CUSTOM_DB_SCHEMA)).append("dpy_syn_showpad ");
		sql.append("group by division_id");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				for (ShowpadDivisionUtil util : divisions) {
					if (rs.getString(2).equals(util.getDivisionId()))
						util.setDbCount(rs.getInt(1));
				}
			}

		} catch (SQLException sqle) {
			log.error("could not count records", sqle);
		}

		dataCounts.put("showpad-total", cnt);
		log.info("there are now " + cnt + " records in the showpad database");
	}


	/**
	 * @param html
	 */
	@Override
	protected void addSupplementalDetails(StringBuilder html) {
		//does nothing here, but gets overwritten by the Showpad decorator 
		//to add valueable stats to the admin email
		for (ShowpadDivisionUtil util : divisions) {
			html.append("<h3>Showpad ").append(util.getDivisionNm()).append(" Division</h3>");
			html.append("Showpad Added: ").append(util.getInsertCount()).append("<br/>");
			html.append("Showpad Updated: ").append(util.getUpdateCount()).append("<br/>");
			html.append("Showpad Deleted: ").append(util.getDeleteCount()).append("<br/>");
			html.append("Showpad Total: ").append(util.getDbCount()).append("<br/><br/>");

			if (util.getFailures().size() > 0) {
				html.append("<b>The following issues were reported:</b><br/><br/>");

				// loop the errors and display them
				for (int i=0; i < util.getFailures().size(); i++) {
					html.append(util.getFailures().get(i).getMessage()).append("<hr/>\r\n");
					log.warn(util.getFailures().get(i).getMessage());
				}
			}
			html.append("<hr/>\r\n");
		}
	}
}