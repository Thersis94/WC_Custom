package com.depuysynthes.scripts.showpad;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.depuysynthes.scripts.MediaBinDeltaVO;
import com.depuysynthes.scripts.MediaBinDeltaVO.State;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.util.RandomAlphaNumeric;
import com.siliconmtn.util.StringUtil;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/****************************************************************************
 * <b>Title</b>: ReplicatorDivisionUtil.java<p/>
 * <b>Description: overrides some superclass methods to adjust behavior for the replicator script's use.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Apr 12, 2017
 ****************************************************************************/
public class ReplicatorDivisionUtil extends ShowpadDivisionUtil {

	/**
	 * @param props
	 * @param divisionId
	 * @param divisionNm
	 * @param util
	 * @param conn
	 */
	public ReplicatorDivisionUtil(Properties props, String divisionId, String divisionNm, ShowpadApiUtil util,
			Connection conn) {
		super(props, divisionId, divisionNm, util, conn);
	}


	/*
	 * (non-Javadoc)
	 * @see com.depuysynthes.scripts.showpad.ShowpadDivisionUtil#createTagManager()
	 */
	@Override
	protected void createTagManager() {
		tagMgr = new ReplicatorTagManager(showpadApiUrl, divisionId, divisionUrl, showpadUtil);
	}


	@Override
	protected void loadAssets(int limit, int offset, Map<String, MediaBinDeltaVO> assets) {
		String tagUrl = divisionUrl + "/assets.json?limit=" + limit + "&fields=id,name,shortLivedDownloadLink,description,fileSize,archivedAt&offset=" + offset;
		log.debug(tagUrl);
		try {
			String resp = showpadUtil.executeGet(tagUrl);
			JSONObject json = JSONObject.fromObject(resp);
			log.info(json);
			JSONObject metaResp = json.getJSONObject("meta");
			if (!"200".equals(metaResp.getString("code")))
				throw new IOException(metaResp.getString("message"));

			JSONObject response = json.getJSONObject("response");
			JSONArray items = response.getJSONArray("items");
			for (int x=0; x < items.size(); x++) {
				JSONObject item = items.getJSONObject(x);
				MediaBinDeltaVO vo = new MediaBinDeltaVO(item);
				vo.setLimeLightUrl(item.optString("shortLivedDownloadLink"));
				vo.setFileSizeNo(item.optInt("fileSize"));
				vo.setDpySynMediaBinId(vo.getTitleTxt()); //these can be the same, it just appeases some Maps we use here in code.
				vo.setDownloadTypeTxt(item.optString("description"));
				if ("null".equalsIgnoreCase(vo.getDownloadTypeTxt())) 
					vo.setDownloadTypeTxt(null);

				//if archivedAt is not empty, it means this item is in the trash.  Tag it so we can toss it out.
				if (!StringUtil.isEmpty(item.optString("archivedAt")) && !"null".equalsIgnoreCase(item.optString("archivedAt")))
					vo.setRecordState(State.ShowpadTrash);

				assets.put(vo.getShowpadId(), vo);
			}

		} catch (IOException | NullPointerException ioe) {
			failures.add(ioe);
			log.error("could not load showpad tags", ioe);
		}
	}


	/*
	 * preserve the asset names as they were in the source account
	 * (non-Javadoc)
	 * @see com.depuysynthes.scripts.showpad.ShowpadDivisionUtil#makeShowpadAssetName(com.depuysynthes.scripts.MediaBinDeltaVO)
	 */
	@Override
	protected String makeShowpadAssetName(MediaBinDeltaVO vo) {
		String[] arr = vo.getTitleTxt().split("\\.");
		String name = vo.getTitleTxt();
		//if we have more than 1 part, presume it to be a file extension and remove it.  The script will add it later.
		if (arr.length > 1)
			name = name.substring(0, name.lastIndexOf('.'));

		log.debug("title=" + name);
		return name;
	}


	/**
	 * downloads the file from the source account in Showpad, into a config-defined temp directory
	 * @param vo
	 */
	public void downloadFile(MediaBinDeltaVO vo) {
		vo.setFileName(props.getProperty("syncTmpDir") + vo.getTitleTxt());
		log.info("retrieving " + vo.getLimeLightUrl());
		try {
			//write the file to our repository
			String fullPath = vo.getFileName(); 
			String parentDir = fullPath.substring(0, fullPath.lastIndexOf(File.separator));
			File dir = new File(parentDir);
			if (!dir.exists()) dir.mkdirs();

			File f = new File(fullPath);

			//if the file already exists, and is the same size as the source one, we don't need to download it again
			if (f.exists() && f.length() == vo.getFileSizeNo()) {
				log.warn("skipping existing file");
				return;
			} else if (f.exists()) {
				//colliding files, rename this one using it's ShowpadID
				fullPath = props.getProperty("syncTmpDir") + vo.getShowpadId().substring(0, 5) + "-" + vo.getTitleTxt();
				vo.setFileName(fullPath);
				f = new File(fullPath);
				if (f.exists() && f.length() == vo.getFileSizeNo()) {
					log.warn("skipping existing file w/special name");
					return;
				}
			}

			downloadFile(f, vo);

		} catch (Exception e) {
			log.error("could not download file", e);
			vo.setRecordState(State.Failed);
		}
	}


	/**
	 * code abstract to keep things simple.  This does the actual file downloading for the 
	 * above/parent method.
	 * @param f
	 * @param vo
	 * @throws IOException
	 */
	private void downloadFile(File f, MediaBinDeltaVO vo) throws IOException {
		SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
		InputStream is = conn.retrieveConnectionStream(vo.getLimeLightUrl(), null);				

		if (404 == conn.getResponseCode())
			throw new FileNotFoundException();

		if (200 != conn.getResponseCode())
			throw new IOException();

		try (FileOutputStream fos = new FileOutputStream(f)) {
			int nRead = 0;
			int byteCnt = 0;
			byte[] byteBuffer = new byte[8192];
			while ((nRead = is.read(byteBuffer)) != -1) {
				byteCnt += nRead;
				fos.write(byteBuffer, 0, nRead);
			}
			fos.flush();
			int kbCnt = byteCnt > 0 ? byteCnt/1000 : byteCnt;
			vo.setFileSizeNo(kbCnt);
			log.debug("wrote file " + f.getAbsolutePath() + " kb=" + kbCnt + " bytes=" + byteCnt);
		}
	}


	/**
	 * @param vo
	 */
	public void addDesiredTags(MediaBinDeltaVO vo) {
		Set<String> tags = new HashSet<>();
		String tagUrl = showpadApiUrl + "/assets/" + vo.getShowpadId() + "/tags.json?limit=1000&suppress_response_codes=true&fields=id,name,externalId";
		try {
			String resp = showpadUtil.executeGet(tagUrl);
			JSONObject json = JSONObject.fromObject(resp);
			log.info("tags=" + json);
			JSONObject metaResp = json.getJSONObject("meta");
			if (!"200".equals(metaResp.getString("code")))
				throw new IOException(metaResp.getString("message"));

			JSONObject response = json.getJSONObject("response");
			JSONArray items = response.getJSONArray("items");
			for (int x=0; x < items.size(); x++) {
				ShowpadTagVO tagVo = new ShowpadTagVO(items.getJSONObject(x), divisionId);
				tags.add(tagVo.getName());
			}

		} catch (IOException | NullPointerException ioe) {
			log.error("Asset not found in Showpad source account: " + vo.getShowpadId(), ioe);
		}

		log.info("loaded " + tags.size() + " showpad tags: " + tags);
		vo.setReplicatorDesiredTags(tags);
	}
}