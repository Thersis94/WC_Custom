package com.depuysynthes.scripts.showpad;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.depuysynthes.scripts.MediaBinDeltaVO;
import com.depuysynthes.scripts.MediaBinDeltaVO.State;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.io.mail.EmailMessageVO;
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
		int cnt = srcAssets.size();
		for (MediaBinDeltaVO vo : srcAssets.values()) {
			log.debug(vo.getRecordState() + " " + vo.isFileChanged() + " " + vo.getTitleTxt());
			srcDivision.addDesiredTags(vo);
			destDivision.pushAsset(vo);
			--cnt;
			log.info(cnt + " assets remaining");
		}
		//process the ticket queue.  This will force the script to wait/block until all assets are processed, which makes our report more accurate
		destDivision.processTicketQueue();

		log.info("done!");
		sendEmail();
	}


	/**
	 * possible future state: If an asset is deleted from the source account that is not realized and deleted from the destination account.
	 * @param srcAssets
	 * @param destAssets
	 */
	protected void marryRecords(Map<String, MediaBinDeltaVO> srcAssets, Map<String, MediaBinDeltaVO> destAssets) {
		int cnt = srcAssets.size();
		Map<String, MediaBinDeltaVO> titleMap = makeTitleMap(destAssets.values());
		for (MediaBinDeltaVO vo : srcAssets.values()) {
			MediaBinDeltaVO target = titleMap.get(vo.getTitleTxt());
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
			--cnt;
			log.info(cnt + " assets remaining in marry");
		}
	}



	/**
	 * sends an email to the admins containing a reconsile report
	 */
	protected void sendEmail() {
		//get all the assets from both accounts
		Map<String, MediaBinDeltaVO> srcAssets = makeTitleMap(srcDivision.getAllAssets().values());
		Map<String, MediaBinDeltaVO> destAssets = makeTitleMap(destDivision.getAllAssets().values());
		Set<String> iter = new HashSet<>(destAssets.keySet()); //copy the keyset - make one that is not backed by the Map.
		//prune the above lists by comparing the two.  We only want records that don't exist on both sides.
		for (String destTitle: iter) {
			srcAssets.remove(destTitle);
			destAssets.remove(destTitle);
		}
		Map<String, GenericVO> data = new HashMap<>();
		data.put("Account Replicator", new GenericVO(new ArrayList<MediaBinDeltaVO>(srcAssets.values()), new ArrayList<MediaBinDeltaVO>(destAssets.values())));
		ReconcileExcelReport rpt = new ReconcileExcelReport(data);
		EmailMessageVO eml = new EmailMessageVO();
		try {
			eml.setFrom("appsupport@siliconmtn.com");
			eml.addRecipients(props.getProperty("replicatorEmailRcpt"));
			eml.setSubject(props.getProperty("replicatorEmailSubj"));
			byte[] rptBytes = rpt.generateReport();
			if (rptBytes.length > 0)
				eml.addAttachment(rpt.getFileName(), rptBytes);

			eml.setHtmlBody(rpt.getEmailSummary() + "\n\n\"Mediabin\" represents the source account, \"Showpad\" represents the destination account");
			super.sendEmail(eml);
		} catch (Exception e) {
			log.error("could not send report email", e);
		}
	}


	/**
	 * @param values
	 * @return
	 */
	private Map<String, MediaBinDeltaVO> makeTitleMap(Collection<MediaBinDeltaVO> values) {
		Map<String, MediaBinDeltaVO> data = new HashMap<>(values.size());
		for (MediaBinDeltaVO vo : values)
			data.put(vo.getTitleTxt(), vo);
		return data;
	}
}