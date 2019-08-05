package com.depuysynthes.scripts.showpad;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.depuysynthes.action.MediaBinAssetVO;
import com.depuysynthes.scripts.DSMediaBinImporterV2;
import com.depuysynthes.scripts.MediaBinDeltaVO;
import com.depuysynthes.scripts.MediaBinDeltaVO.State;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

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

	public static final String BR = "<br/>";

	protected ShowpadApiUtil showpadApi;
	protected List<ShowpadDivisionUtil> divisions = new ArrayList<>();

	/**
	 * @param args
	 * @throws IOException 
	 */
	public ShowpadMediaBinDecorator(String[] args) throws IOException {
		super(args);
		showpadApi = ShowpadApiUtil.makeInstance(props, null);
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
		sql.append("from ").append(schema).append("DPY_SYN_SHOWPAD ");
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
		sql.append("delete from ").append(schema).append("dpy_syn_showpad ");
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
		sql.append(schema).append("dpy_syn_showpad ");
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
			html.append("Added: ").append(util.getInsertCount()).append(BR);
			html.append("Updated: ").append(util.getUpdateCount()).append(BR);
			html.append("Deleted: ").append(util.getDeleteCount()).append(BR);
			html.append("Total: ").append(util.getDbCount()).append(BR);
			html.append("Failed to Ingest: ").append(util.getFailCount()).append(BR).append(BR);

			List<Exception> failures = util.getFailures();
			if (!failures.isEmpty()) {
				html.append("<b>The following issues were reported:</b>").append(BR).append(BR);

				// loop the errors and display them
				for (int i=0; i < failures.size(); i++) {
					html.append(failures.get(i).getMessage()).append("<hr/>\r\n");
					log.warn(failures.get(i).getMessage());
				}
			}

			html.append("<hr/>\r\n");
		}

		addExpiringSoonAssets(html);
	}


	/**
	 * Add a table to the report of assets expiring soon (or expired).
	 * Color orange for expiring <3mos.  Color red for expired.
	 * @param html
	 */
	protected void addExpiringSoonAssets(StringBuilder msg) {
		String sql = StringUtil.join("select tracking_no_txt, title_txt, expiration_dt from ", 
				schema, "dpy_syn_mediabin where expiration_dt <= current_date+90 ",
				"and expiration_dt is not null and import_file_cd=? ", 
				"order by expiration_dt, tracking_no_txt, title_txt");
		log.debug(sql);
		List<MediaBinAssetVO> assets = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setInt(1, type);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				MediaBinAssetVO vo = new MediaBinAssetVO();
				vo.setTrackingNoTxt(rs.getString(1));
				vo.setTitleTxt(rs.getString(2));
				vo.setExpirationDt(rs.getDate(3));
				assets.add(vo);
			}

		} catch (SQLException sqle) {
			log.error("could not load expiring assets", sqle);
		}

		if (assets.isEmpty()) return;

		//add a html table to the email body
		final String expiresSoon = "<font color=\"orange\">%s</font>";
		final String expired = "<font color=\"red\">%s</font>";
		final Date today = Calendar.getInstance().getTime();

		msg.append("<h4>Asset Expiration</h4>");
		msg.append("<table border='1' width='95%' align='center'><thead><tr>");
		msg.append("<th>Expiration</th>");
		msg.append("<th>Tracking Number</th>");
		msg.append("<th>Title</th>");
		msg.append("</tr></thead>\r<tbody>");

		for (MediaBinAssetVO vo : assets) {
			String mask = vo.getExpirationDt().after(today) ? expiresSoon : expired;
			msg.append("<tr><td>").append(String.format(mask, Convert.formatDate(vo.getExpirationDt(), "dd/MM/yyyy"))).append("</td>");
			msg.append("<td nowrap>").append(String.format(mask, vo.getTrackingNoTxt())).append("</td>");
			msg.append("<td>").append(String.format(mask, vo.getTitleTxt())).append("</td></tr>\r");
		}
		msg.append("</tbody></table>\r");
		msg.append("Color Code:").append(String.format(expiresSoon, "Expiring <3mos. ")).append(String.format(expired, " Expired.  Deleted from Showpad."));
	}
}