package com.depuysynthes.scripts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.depuysynthes.scripts.MediaBinDeltaVO.State;
import com.depuysynthes.scripts.showpad.ShowpadDivisionUtil;
import com.depuysynthes.scripts.showpad.ShowpadMediaBinDecorator;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: DSPrivateAssetsImporter.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Mar 14, 2017
 ****************************************************************************/
public class DSPrivateAssetsImporter extends ShowpadMediaBinDecorator {

	protected static final String INTERNAL_TAG = "internal";
	private Set<String> publicAssetIds;
	private Set<String> blockedPublicAssets;

	/**
	 * @param args
	 * @throws IOException 
	 */
	public DSPrivateAssetsImporter(String[] args) throws IOException {
		super(args);
		type = 3; //private assets.  always.
		publicAssetIds = loadPublicAssets();
		blockedPublicAssets = new HashSet<>();
	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws Exception {
		//Create an instance of the MedianBinImporter
		DSPrivateAssetsImporter dmb = new DSPrivateAssetsImporter(args);
		dmb.run();
	}

	/*
	 * passes the "internal" tag to the overloaded default constructor, to replace the "mediabin" tag that would otherwise
	 * get attached to every asset.
	 * (non-Javadoc)
	 * @see com.depuysynthes.scripts.showpad.ShowpadMediaBinDecorator#loadShowpadDivisionList()
	 */
	@Override
	protected void loadShowpadDivisionList() {
		String[] divs = props.getProperty("showpadDivisions").split(",");
		for (String d : divs) {
			String[] div = d.split("=");
			divisions.add(new ShowpadDivisionUtil(props, div[1], div[0], showpadApi, dbConn, INTERNAL_TAG));
			log.debug("created division " + div[0] + " with id " + div[1]);
		}
		log.info("loaded " + divisions.size() + " showpad divisions");
	}


	/**
	 * builds a list of public-facing EMEA assets.  When we check for opCo authorization, we'll also block any
	 * that are already public assets...they can't be both public and private.
	 */
	protected Set<String> loadPublicAssets() {
		Set<String> data = new HashSet<>();
		String sql = "select tracking_no_txt from " + props.get(Constants.CUSTOM_DB_SCHEMA) + "DPY_SYN_MEDIABIN where import_file_cd=2";
		log.debug(sql);
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				data.add(rs.getString(1));
		} catch (SQLException sqle) {
			log.error("could not load public asset tracking#s", sqle);
		}
		log.debug("public assets to block: " + data.size());
		return data;
	}


	/*
	 * (non-Javadoc)
	 * @see com.depuysynthes.scripts.DSMediaBinImporterV2#loadFile(java.lang.String)
	 */
	@Override
	protected List<Map<String, String>> loadFile(String path) throws IOException {
		log.info("starting file parser " + path);
		return parseFile(new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF-16")));
	}


	/*
	 * Straight business rules for private assets.  If the asset has no distChannel, or the distChannel is "INT Mobile" (exclusively)
	 * Also returns false if the asset is already deemed a public asset. 
	 * (non-Javadoc)
	 * @see com.depuysynthes.scripts.DSMediaBinImporterV2#isOpcoAuthorized(java.lang.String, java.lang.String[])
	 */
	@Override
	protected boolean isOpcoAuthorized(String distChannel, String[] allowedOpCoNames, String tn) {
		boolean isAuth =  StringUtil.isEmpty(distChannel) || "INT Mobile".equals(distChannel);

		if (isAuth && publicAssetIds.contains(tn)) {
			log.debug("blocked - public asset " + tn);
			blockedPublicAssets.add(tn);
			return false;
		} else {
			return isAuth;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.depuysynthes.scripts.DSMediaBinImporterV2#isAssetTypeAuthorized(java.lang.String, java.util.List)
	 */
	@Override
	protected boolean isAssetTypeAuthorized(String assetType, List<String> allowedTypes) {
		return true; //allow all file types for private assets
	}


	/*
	 * (non-Javadoc)
	 * @see com.depuysynthes.scripts.DSMediaBinImporterV2#downloadFiles(java.util.Map)
	 */
	@Override
	protected void downloadFiles(Map<String, MediaBinDeltaVO> masterRecords) {
		String dropboxFolder = (String) props.get("downloadDirPrivAssets");

		for (MediaBinDeltaVO vo : masterRecords.values()) {
			//note backslashes were turned into forward slashes when the data was loaded from the EXP file, in parseData()
			//path on disk omits the two leading directories noted in the EXP file.  Remove them.
			String name = StringUtil.replace(vo.getAssetNm(), "Synthes International/Product Support Material/", "");
			vo.setFileName(dropboxFolder + StringUtil.replace(name, "/", File.separator));

			//check for the file on disk.  if it's not there, flag this as an error.
			File f = new File(vo.getFileName());
			if (!f.exists()) {
				vo.setRecordState(State.Failed);
				String msg = makeMessage(vo, "File not found");
				failures.add(new Exception(msg));
				continue;
			}

			//get modification date and file size.  Changes to either need to trigger an update to Showpad
			String checksum = new StringBuilder(15).append("||").append(f.length()).toString();
			if (!checksum.equals(vo.getChecksum())) {
				vo.setFileChanged(true);
				vo.setChecksum(checksum);
				vo.setModifiedDt(new Date(f.lastModified()));
				if (State.Ignore == vo.getRecordState()) {
					vo.setRecordState(State.Update);
					vo.setErrorReason("File on disk was updated");
				}
			}
		}
	}


	/*
	 * (non-Javadoc)
	 * @see com.depuysynthes.scripts.DSMediaBinImporterV2#syncWithSolr(java.util.Map)
	 */
	@Override
	protected void syncWithSolr(Map<String, MediaBinDeltaVO> masterRecords) {
		//this does nothing for internal assets
	}


	/**
	 * @param html
	 */
	@Override
	protected void addSupplementalDetails(StringBuilder msg) {
		super.addSupplementalDetails(msg);
		if (blockedPublicAssets.isEmpty()) return;
		msg.append("\r\n<h4>Public Assets in File - Not Imported</h4>");
		msg.append("<table border='1' width='95%' align='center'><thead><tr>");
		msg.append("<th>Tracking Number</th>");
		msg.append("</tr></thead><tbody>");

		for (String tn : blockedPublicAssets) {
			msg.append("<tr><td nowrap>").append(tn).append("</td></tr>\r\n");
		}
		msg.append("</tbody></table>\r\n");
	}
}
