package com.depuysynthes.scripts.showpad;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.depuysynthes.scripts.MediaBinDeltaVO;
import com.depuysynthes.scripts.MediaBinDeltaVO.State;
import com.siliconmtn.util.CommandLineUtil;

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

	protected ReplicatorDivisionUtil srcDivision;
	protected ReplicatorDivisionUtil destDivision;

	/**
	 * @param args
	 * @throws IOException
	 */
	public AccountReplicator(String[] args) throws IOException {
		super(args);
		loadProperties("scripts/MediaBin.properties");
		loadDBConnection(props);

		ShowpadApiUtil srcApi = ShowpadApiUtil.makeInstance(props, "src-");
		String[] div = props.getProperty("src-showpadDivisions").split("=");
		props.put("showpadApiUrl", props.getProperty("src-showpadApiUrl"));
		srcDivision = new ReplicatorDivisionUtil(props, div[1], div[0], srcApi, dbConn);

		//setup API util for the source account
		ShowpadApiUtil destApi = ShowpadApiUtil.makeInstance(props, "dest-");
		div = props.getProperty("dest-showpadDivisions").split("=");
		props.put("showpadApiUrl", props.getProperty("dest-showpadApiUrl"));
		//give the destDivision a reference to the sourceDivision's API, so it can load tags for each asset as it iterates
		destDivision = new ReplicatorDivisionUtil(props, div[1], div[0], destApi, dbConn);
	}


	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws Exception {
		AccountReplicator dmb = new AccountReplicator(args);
		dmb.run();
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	@Override
	public void run() {
		//get all assets from the source account
		Map<String, MediaBinDeltaVO> srcAssets = srcDivision.getAllAssets();

		//populate the destDivision with it's assets, so we're updating existing assets instead of creating duplicates
		Map<String, MediaBinDeltaVO> destAssets = destDivision.getAllAssets();

		Map<String, String> pointers = new HashMap<>(destAssets.size());
		for (MediaBinDeltaVO vo : destAssets.values())
			pointers.put(vo.getDpySynMediaBinId(), vo.getShowpadId());
		destDivision.setDivisionAssets(pointers);
		log.debug("gave destDivision " + pointers.size() + " existing assets");

		marryRecords(srcAssets, destAssets);

		//loop through the source assets, fire an update or add for each (the divisionUtil will determine which for us)
		for (MediaBinDeltaVO vo : srcAssets.values()) {
			log.debug(vo.getRecordState() + " " + vo.isFileChanged());
			srcDivision.addDesiredTags(vo);
			destDivision.pushAsset(vo);
		}
		log.info("done!");
	}


	/**
	 * possible future state: If an asset is deleted from the source account that is not realized and deleted from the destination account.
	 * @param srcAssets
	 * @param destAssets
	 */
	protected void marryRecords(Map<String, MediaBinDeltaVO> srcAssets, Map<String, MediaBinDeltaVO> destAssets) {
		for (MediaBinDeltaVO vo : srcAssets.values()) {
			MediaBinDeltaVO target = destAssets.get(vo.getShowpadId());
			if (target == null) {
				vo.setRecordState(State.Insert); //does not exist at the dest account (implies fileChanged=true)
				srcDivision.downloadFile(vo);
			} else if (vo.getFileSizeNo() == target.getFileSizeNo()) {
				vo.setRecordState(State.Ignore); //exists, and file sizes are the same meaning no upload needed.  meta-data will still be replicated.
				vo.setFileChanged(false);
			} else {
				vo.setRecordState(State.Update); //file changed.  the file will be uploaded as an update transaction
				srcDivision.downloadFile(vo);
				vo.setFileChanged(true);
			}
		}
	}
}