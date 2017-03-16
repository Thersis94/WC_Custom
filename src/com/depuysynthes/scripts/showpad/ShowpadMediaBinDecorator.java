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
	private boolean deduplicate = false; //override via args[1]

	/**
	 * @param args
	 * @throws IOException 
	 */
	public ShowpadMediaBinDecorator(String[] args) throws IOException {
		super(args);

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
		loadShowpadDivisionList();

		//used only for de-duplication, which is more crisis-cleanup than something we do regularly.
		if (deduplicate) {
			Map<String, MediaBinDeltaVO> records = loadManifest();
			Set<String> localShowpadIds = new HashSet<>(records.size());

			for (MediaBinDeltaVO vo : records.values()) {
				if (vo.getShowpadId() != null) localShowpadIds.add(vo.getShowpadId());
			}

			for (ShowpadDivisionUtil util : divisions)
				util.cleanupShowpadDups(localShowpadIds);
			
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
	protected void loadShowpadDivisionList() {
		String[] divs = props.getProperty("showpadDivisions").split(",");
		for (String d : divs) {
			String[] div = d.split("=");
			divisions.add(new ShowpadDivisionUtil(props, div[1], div[0], showpadApi, dbConn));
			log.debug("created division " + div[0] + " with id " + div[1]);
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
		for (Map.Entry<String, Map<String, String>> entry : divisionAssets.entrySet()) {
			for (ShowpadDivisionUtil util : divisions) {
				if (util.getDivisionId().equals(entry.getKey())) {
					util.setDivisionAssets(entry.getValue());
					log.info("gave " + entry.getValue().size() + " assets to division=" + entry.getKey());
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

		//push all changes to Showpad
		for (MediaBinDeltaVO vo : masterRecords.values()) {
			//we need to sort out what gets pushed to Showpad on our own.
			//if it's failed, being deleted, or unchanged and already in Showpad, skip it.
			State s = vo.getRecordState();
			if (s == State.Failed || s == State.Delete)
				continue;

			loopFileThroughDivisions(vo);
			
			log.info("completed: " + vo.getFileNm());
		}

		//process the ticket queue for each division
		for (ShowpadDivisionUtil util : divisions)
			util.processTicketQueue();

		//save the newly created records to our database for each division
		for (ShowpadDivisionUtil util : divisions)
			util.saveDBRecords();
	}


	/**
	 * called for each mediabin asset in the stack - push it out to each of the Divisions we're managing
	 * @param vo
	 * @throws QuotaException
	 */
	protected void loopFileThroughDivisions(MediaBinDeltaVO vo) {
		for (ShowpadDivisionUtil util : divisions) {
			util.pushAsset(vo);	
		}
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

		List<String> deletedIds = deleteFromShowpad(masterRecords);

		//fail-fast if there's nothing to do
		if (deletedIds == null || deletedIds.isEmpty()) return;

		// Build the SQL Statement
		StringBuilder sql = new StringBuilder(350);
		sql.append("delete from ").append(props.getProperty(Constants.CUSTOM_DB_SCHEMA)).append("dpy_syn_showpad ");
		sql.append("where dpy_syn_mediabin_id=?");
		log.debug(sql);
		try (PreparedStatement ps  = dbConn.prepareStatement(sql.toString())) {
			for (String id : deletedIds) {
				ps.setString(1, id);
				ps.addBatch();
			}
			ps.executeBatch();
		} catch (SQLException sqle) {
			failures.add(sqle);
		}
	}


	/**
	 * deletes all the 'deleted' records from Showpad.
	 * Returns a list of IDs that were deleted, so we can delete them from the local database as well.
	 * @param masterRecords
	 * @return
	 * @throws QuotaException
	 */
	protected List<String> deleteFromShowpad(Map<String, MediaBinDeltaVO> masterRecords) {
		List<String> data = new ArrayList<>(100);
		for (MediaBinDeltaVO vo : masterRecords.values()) {
			if (vo.getRecordState() != State.Delete)  continue;

			//push deletions to Showpad
			for (ShowpadDivisionUtil util : divisions)
				util.deleteAsset(vo);

			//if success, delete it from the DB as well
			data.add(vo.getDpySynMediaBinId());
		}
		return data;
	}


	/**
	 * returns a count of the database records; called after we finish our updates to verify total
	 * @param type
	 * @return
	 */
	@Override
	protected void countDBRecords() {
		super.countDBRecords();

		StringBuilder sql = new StringBuilder(150);
		sql.append("select count(*), division_id, case when asset_id='FAILED_PROCESSING' then 1 else 0 end as status from ");
		sql.append(props.get(Constants.CUSTOM_DB_SCHEMA)).append("dpy_syn_showpad ");
		sql.append("group by division_id, case when asset_id='FAILED_PROCESSING' then 1 else 0 end");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				setRecordCounts(rs);
		} catch (SQLException sqle) {
			log.error("could not count records", sqle);
		}
	}


	/**
	 * sets the counter values (to be reported in the email) into each of the division utils. 
	 * @param rs
	 * @throws SQLException
	 */
	protected void setRecordCounts(ResultSet rs) throws SQLException {
		for (ShowpadDivisionUtil util : divisions) {
			if (rs.getString(2).equals(util.getDivisionId()) && rs.getInt("status") == 0) {
				util.setDbCount(rs.getInt(1));
			} else if (rs.getString(2).equals(util.getDivisionId()) && rs.getInt("status") == 1) {
				util.setFailCount(rs.getInt(1));
			}
		}
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
			html.append("Added: ").append(util.getInsertCount()).append("<br/>");
			html.append("Updated: ").append(util.getUpdateCount()).append("<br/>");
			html.append("Deleted: ").append(util.getDeleteCount()).append("<br/>");
			html.append("Total: ").append(util.getDbCount()).append("<br/>");
			html.append("Failed to Ingest: ").append(util.getFailCount()).append("<br/><br/>");

			List<Exception> failures = util.getFailures();
			if (!failures.isEmpty()) {
				html.append("<b>The following issues were reported:</b><br/><br/>");

				// loop the errors and display them
				for (int i=0; i < failures.size(); i++) {
					html.append(failures.get(i).getMessage()).append("<hr/>\r\n");
					log.warn(failures.get(i).getMessage());
				}
			}

			html.append("<hr/>\r\n");
		}
	}
}