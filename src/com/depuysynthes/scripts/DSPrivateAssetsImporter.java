package com.depuysynthes.scripts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.depuysynthes.scripts.MediaBinDeltaVO.State;
import com.depuysynthes.scripts.showpad.QuotaException;
import com.depuysynthes.scripts.showpad.ShowpadDivisionUtil;
import com.depuysynthes.scripts.showpad.ShowpadMediaBinDecorator;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

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

	/**
	 * @param args
	 * @throws IOException 
	 */
	public DSPrivateAssetsImporter(String[] args) throws IOException {
		super(args);
		type = 3; //private assets.  always.
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
	protected void loadShowpadDivisionList() throws QuotaException {
		String[] divs = props.getProperty("showpadDivisions").split(",");
		for (String d : divs) {
			String[] div = d.split("=");
			divisions.add(new ShowpadDivisionUtil(props, div[1], div[0], showpadApi, dbConn, "internal"));
			log.debug("created division " + div[0] + " with id " + div[1]);
		}
		log.info("loaded " + divisions.size() + " showpad divisions");
	}
	

	/*
	 * (non-Javadoc)
	 * @see com.depuysynthes.scripts.DSMediaBinImporterV2#loadFile(java.lang.String)
	 */
	@Override
	protected List<Map<String, String>> loadFile(String path) throws IOException {
		log.info("starting file parser " + path);
		return parseFile(new BufferedReader(new FileReader(new File(path))));
	}


	/*
	 * Straight business rules for private assets.  If the asset has no distChannel, or the distChannel is "INT Mobile" (exclusively) 
	 * (non-Javadoc)
	 * @see com.depuysynthes.scripts.DSMediaBinImporterV2#isOpcoAuthorized(java.lang.String, java.lang.String[])
	 */
	@Override
	protected boolean isOpcoAuthorized(String distChannel, String[] allowedOpCoNames) {
		return StringUtil.isEmpty(distChannel) || "INT Mobile".equals(distChannel);
	}


	/*
	 * (non-Javadoc)
	 * @see com.depuysynthes.scripts.DSMediaBinImporterV2#downloadFiles(java.util.Map)
	 */
	@Override
	protected void downloadFiles(Map<String, MediaBinDeltaVO> masterRecords) {
		String dropboxFolder = (String) props.get("downloadDirPrivAssets");
		String dtFmt = "EEE, dd MMM yyyy HH:mm:ss z"; //this matches the http headers coming back from LimeLight

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
			Date lastModDt = new Date(f.lastModified());
			String checksum = new StringBuilder(50).append(Convert.formatDate(lastModDt, dtFmt)).append("||").append(f.length()).toString();
			if (!checksum.equals(vo.getChecksum())) {
				vo.setFileChanged(true);
				vo.setChecksum(checksum);
				vo.setModifiedDt(lastModDt);
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
}
