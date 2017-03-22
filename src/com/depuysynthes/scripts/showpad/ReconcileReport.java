package com.depuysynthes.scripts.showpad;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.depuysynthes.scripts.MediaBinDeltaVO;
import com.siliconmtn.security.OAuth2TokenViaCLI;
import com.siliconmtn.security.OAuth2TokenViaCLI.Config;
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/****************************************************************************
 * <b>Title</b>: ReconcileReport.java<p/>
 * <b>Description</b>: Generates CSV output containing assets in Showpad 
 * that shouldn't be there...according to what we have in the local DB.
 * (not in ours = shouldn't be in theirs)
 * 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Mar 21, 2017
 ****************************************************************************/
public class ReconcileReport extends ShowpadMediaBinDecorator {


	/**
	 * @param args
	 * @throws IOException 
	 * @throws QuotaException 
	 */
	public ReconcileReport(String[] args) throws IOException {
		super(args);
	}


	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws Exception {
		//Create an instance of the MedianBinImporter
		ReconcileReport dmb = new ReconcileReport(args);
		dmb.run();
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	@Override
	public void run() {
		//load the divisions
		loadShowpadDivisionList();

		for (ShowpadDivisionUtil util : divisions) {

			// get the list of assets from the database.  loads all assets for all divisions across both importCodes
			Map<String, MediaBinDeltaVO> localAssets = loadDivisionFromDB(util.getDivisionId());
			Map<String, MediaBinDeltaVO> remoteAssets = util.getAllAssets();

			//build two lists - assets only in SMT & assets only in Showpad
			List<MediaBinDeltaVO> smtAssets = new ArrayList<>(8000);
			List<MediaBinDeltaVO> showpadAssets = new ArrayList<>(8000);

			for (Map.Entry<String, MediaBinDeltaVO> entry : localAssets.entrySet()) {
				if (!remoteAssets.containsKey(entry.getKey()))
					smtAssets.add(entry.getValue());
			}
			for (Map.Entry<String, MediaBinDeltaVO> entry : remoteAssets.entrySet()) {
				if (!localAssets.containsKey(entry.getKey()))
					showpadAssets.add(entry.getValue());
			}
		}
	}


	/**
	 * calls to the local DB for all of our assets
	 * @return
	 */
	protected Map<String, MediaBinDeltaVO> loadDivisionFromDB(String divisionId) {
		Map<String, MediaBinDeltaVO> data = new HashMap<>(6000);
		StringBuilder sql = new StringBuilder(300);
		sql.append("select * from ").append(props.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("dpy_syn_mediabin a ");
		sql.append("inner join ").append(props.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("dpy_syn_showpad b on a.dpy_syn_mediabin_id=b.dpy_syn_mediabin_id ");
		sql.append("where b.division_id=?");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, divisionId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				MediaBinDeltaVO vo = new MediaBinDeltaVO(rs);
				vo.setDivisionId(rs.getString("division_id"));
				data.put(vo.getShowpadId(), vo);
			}

		} catch (SQLException sqle) {
			log.error("could not load records", sqle);
		}

		log.debug("loaded " + data.size() + " records from the database");
		return data;
	}






}