package com.depuysynthes.action;

// JDK 1.8.x
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

// Solr
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;

// DS Libs
import com.depuysynthes.solr.MediaBinSolrIndex;
import com.depuysynthes.solr.MediaBinSolrIndex.MediaBinField;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.solr.SolrClientBuilder;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.security.SecurityController;

/****************************************************************************
 * <b>Title</b>: DismantlingAdminAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the Media Bin "Dismantling" data
 * <b>Copyright:</b> Copyright (c) 2020
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 10, 2020
 * @updates:
 ****************************************************************************/
public class DismantlingAdminAction extends SBActionAdapter {

	/**
	 * 
	 */
	public DismantlingAdminAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public DismantlingAdminAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		if (! req.hasParameter("json")) return;
		StringBuilder sql = new StringBuilder();
		sql.append("select * from custom.dpy_syn_mediabin dsm "); 
		sql.append("where literature_type_txt = 'Dismantling' ");
		sql.append("order by title_txt");
		
		List<MediaBinAssetVO> data = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			
			while(rs.next()) {
				data.add(new MediaBinAssetVO(rs));
			}
			
			setModuleData(data);
		} catch (Exception e) {
			log.error("Unable to retrieve media bin data", e);
		}
		
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void update(ActionRequest req) throws ActionException {
		MediaBinAssetVO asset = new MediaBinAssetVO(req);
		asset.setAssetNm("Synthes International/Product Support Material/legacy_Synthes_PDF/" + asset.getFileNm());
		asset.setModifiedDt(Convert.parseDateUnknownPattern(req.getParameter("modifiedDt")));
		asset.setExpirationDt(Convert.parseDateUnknownPattern(req.getParameter("expirationDt")));
		log.info(asset.getModifiedDt());
		try {
			DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
			if (req.getBooleanParameter("isInsert")) db.insert(asset);
			else db.update(asset);
			
			// Update solr
			this.updateSolr(asset, false);
			
			// add the data to the response
			setModuleData(asset);
		} catch (Exception e) {
			log.error("Unable to save asset", e);
			setModuleData(asset, 1, e.getLocalizedMessage());
		}
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		MediaBinAssetVO asset = new MediaBinAssetVO(req);
		try {
			DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
			db.delete(asset);
			
			// Update solr
			this.updateSolr(asset, true);
			
			setModuleData("success");
		} catch (Exception e) {
			log.error("Unable to delete asset", e);
			setModuleData(asset, 1, e.getLocalizedMessage());
		}
	}
	
	
	/**
	 * Updates the solr index with the updated content
	 * @param asset
	 * @param isDelete
	 * @throws SolrServerException
	 * @throws IOException
	 */
	public void updateSolr(MediaBinAssetVO asset, boolean isDelete) throws SolrServerException, IOException {
		// initialize the connection to the solr server and connect
		String baseUrl = (String)attributes.get(Constants.SOLR_BASE_URL);
		String collection = (String)attributes.get(Constants.SOLR_COLLECTION_NAME);
		SolrClient server = SolrClientBuilder.build(baseUrl, collection);
		
		// Update solr
		if (isDelete) server.deleteById(asset.getDpySynMediaBinId());
		else {
			SolrInputDocument doc = createSolrDoc(asset);
			log.info(doc);
			server.add(doc);
			server.commit();
		}
	}
	
	/**
	 * Converts the vo into a solr document
	 * @param vo
	 * @return
	 */
	public SolrInputDocument createSolrDoc(MediaBinAssetVO vo) {
		SolrInputDocument doc = new SolrInputDocument();
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		String fileNm = "&name=" + StringEncoder.urlEncode(vo.getFileNm());
		String checksum = new Date() + "||" + ThreadLocalRandom.current().nextInt(1000, 100000);
		
		// Convert the MediaBinAssetVO into a SolrInputDocument
		doc.setField(SearchDocumentHandler.INDEX_TYPE, MediaBinSolrIndex.INDEX_TYPE);
		doc.setField(SearchDocumentHandler.ORGANIZATION, Arrays.asList("DPY_SYN_EMEA")); //multiValue field
		doc.setField(SearchDocumentHandler.LANGUAGE, StringUtil.checkVal(vo.getLanguageCode(), "en"));
		doc.setField(SearchDocumentHandler.ROLE, SecurityController.PUBLIC_ROLE_LEVEL);
		doc.setField(SearchDocumentHandler.DOCUMENT_URL, MediaBinSolrIndex.BASE_REDIR_URL + vo.getDpySynMediaBinId() + fileNm);
		doc.setField(SearchDocumentHandler.DOCUMENT_ID, vo.getDpySynMediaBinId());
		doc.setField(SearchDocumentHandler.TITLE, vo.getTitleTxt());
		doc.setField(SearchDocumentHandler.SUMMARY, getSummary(vo));
		doc.setField(SearchDocumentHandler.FILE_NAME, vo.getFileNm());
		doc.setField(SearchDocumentHandler.FILE_SIZE, vo.getFileSizeNo());
		doc.setField(SearchDocumentHandler.FILE_EXTENSION, vo.getFileNm().substring(vo.getFileNm().lastIndexOf('.')+1));
		doc.setField(SearchDocumentHandler.DURATION, parseDuration(vo.getDuration()));
		doc.setField(SearchDocumentHandler.SECTION, vo.getBusinessUnitNm());
		doc.setField(MediaBinField.DownloadType.getField(), "Dismantling");
		doc.setField(SearchDocumentHandler.META_KEYWORDS, vo.getMetaKeywords());
		doc.addField(MediaBinField.SearchType.getField(), vo.isVideo() ? "VIDEO" : "DOWNLOAD");
		doc.setField(SearchDocumentHandler.MODULE_TYPE, "DOCUMENT");
		doc.setField(SearchDocumentHandler.UPDATE_DATE, df.format(vo.getModifiedDt()));
		doc.setField(MediaBinField.TrackingNo.getField(), vo.getTrackingNoTxt()); //DSI uses this to align supporting images and tag favorites
		doc.setField(MediaBinField.AssetType.getField(), getAssetType(vo));
		doc.setField(MediaBinField.AssetDesc.getField(), StringUtil.isEmpty(vo.getAssetDesc()) ? "Dismantling" : vo.getAssetDesc());
		doc.setField(MediaBinField.DSOrderNo.getField(), vo.isVideo() ? 25 : 30); //used for moduleType sequencing on DS only
		doc.setField(MediaBinField.ImportFileCd.getField(), vo.getImportFileCd() == 0 ? 2 : vo.getImportFileCd());
		doc.setField(MediaBinField.Checksum.getField(), StringUtil.isEmpty(vo.getChecksum()) ? checksum : vo.getChecksum());
		doc.addField(SearchDocumentHandler.HIERARCHY, "");
		
		return doc;
	}
	
	/**
	 * Parses download type and returns the download type text either as an empty String,
	 * a single-value String, or a comma-delimited String.
	 * @param downloadType
	 * @param isVideo
	 * @return
	 */
	public String parseDownloadType(String downloadType, boolean isVideo) {
		String tmp = StringUtil.checkVal(downloadType).replace("~", ",");
		if (isVideo) {
			// this is a video, set the type as 'Video' or add 'Video' to the existing type(s).
			if (tmp.length() == 0) {
				tmp = "Video";
			} else {
				if (! tmp.toLowerCase().contains("video")) tmp += ",Video";
			}
		} else {
			// not a video, set type as 'Other' if no type was supplied.
			if (tmp.length() == 0) {
				tmp = "Other";
			}
		}
		return tmp;
	}
	
	/**
	 * 
	 * @param vo
	 * @return
	 */
	public String getAssetType(MediaBinAssetVO vo) {
		if ("multimedia file".equalsIgnoreCase(vo.getAssetType())) {
			return StringUtil.checkVal(vo.getAssetDesc()).toLowerCase();
		} else {
			return vo.getAssetType();
		}
	}
	
	/**
	 * Parses the duration (seconds) into a String representing
	 * hours and minutes in HH:MM format.
	 * @param duration
	 * @return
	 */
	public String parseDuration(double duration) {
		if (duration == 0) return "";
		StringBuilder dur = new StringBuilder();
		int hours = (int) duration / 3600;
		int minutes = (int) (duration % 3600) / 60;
		int seconds = (int) (duration % 3600) % 60;

		//only include hours if needed
		if (hours > 0) {
			if (hours < 10) dur.append("0");
			dur.append(hours).append(":");
		}

		//append the minutes
		if (minutes < 10) dur.append("0");
		dur.append(minutes).append(":");

		//append the seconds
		if (seconds < 10) dur.append("0");
		dur.append(seconds);

		log.debug("video duration parsed is: " + dur.toString());
		return dur.toString();
	}
	
	/**
	 * Figures out the appropriate summary for the given document
	 * @param vo Document Meta-Data
	 * @return
	 */
	private String getSummary(MediaBinAssetVO vo) {
		String summary = "";
		//DSI work-around.  DS.com should never have been using AssetDesc, but 
		//since it was validated that way we can't change it.
		if (vo.getOpCoNm().indexOf("DSI.com") > -1) {
			summary = StringUtil.checkVal(vo.getDescription());
		}

		if (summary.length() == 0) summary = StringUtil.checkVal(vo.getAssetDesc());
		if (summary.length() == 0) {
			summary = StringUtil.checkVal(vo.getProdFamilyNm());
			if (summary.length() > 0) summary += " | ";
			summary += StringUtil.checkVal(vo.getProdNm());
		}

		return summary;
	}
	
}
