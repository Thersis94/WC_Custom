package com.depuysynthes.action;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;

import com.depuysynthes.solr.MediaBinSolrIndex;
import com.depuysynthes.solr.MediaBinSolrIndex.MediaBinField;
// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
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
			server.add(doc);
		}
	}
	
	/**
	 * Converts the vo into a solr document
	 * @param vo
	 * @return
	 */
	public SolrInputDocument createSolrDoc(MediaBinAssetVO vo) {
		SolrInputDocument doc = new SolrInputDocument();
		MediaBinSolrIndex idx = new MediaBinSolrIndex();
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		
		// Convert the MediaBinAssetVO into a SolrInputDocument
		doc.setField(SearchDocumentHandler.INDEX_TYPE, MediaBinSolrIndex.INDEX_TYPE);
		doc.setField(SearchDocumentHandler.ORGANIZATION, Arrays.asList("DPY_SYN_EMEA")); //multiValue field
		doc.setField(SearchDocumentHandler.LANGUAGE, StringUtil.checkVal(vo.getLanguageCode(), "en"));
		doc.setField(SearchDocumentHandler.ROLE, SecurityController.PUBLIC_ROLE_LEVEL);
		doc.setField(SearchDocumentHandler.DOCUMENT_URL, vo.getActionUrl());
		doc.setField(SearchDocumentHandler.DOCUMENT_ID, vo.getDpySynMediaBinId());
		doc.setField(SearchDocumentHandler.TITLE, vo.getTitleTxt());
		doc.setField(SearchDocumentHandler.SUMMARY, getSummary(vo));
		doc.setField(SearchDocumentHandler.FILE_NAME, vo.getFileNm());
		doc.setField(SearchDocumentHandler.FILE_SIZE, vo.getFileSizeNo());
		doc.setField(SearchDocumentHandler.DURATION, idx.parseDuration(vo.getDuration()));
		doc.setField(SearchDocumentHandler.SECTION, idx.parseBusinessUnit(vo.getBusinessUnitNm()));
		doc.setField(MediaBinField.DownloadType.getField(), idx.parseDownloadType(vo.getDownloadTypeTxt(), vo.isVideo()));
		doc.setField(SearchDocumentHandler.META_KEYWORDS, vo.getMetaKeywords());
		doc.addField(MediaBinField.SearchType.getField(), vo.isVideo() ? "VIDEO" : "DOWNLOAD");
		doc.setField(SearchDocumentHandler.MODULE_TYPE, "DOCUMENT");
		doc.setField(SearchDocumentHandler.UPDATE_DATE, df.format(vo.getModifiedDt()));
		doc.setField(MediaBinField.TrackingNo.getField(), vo.getTrackingNoTxt()); //DSI uses this to align supporting images and tag favorites
		doc.setField(MediaBinField.AssetType.getField(), idx.getAssetType(vo));
		doc.setField(MediaBinField.AssetDesc.getField(), vo.getAssetDesc());
		doc.setField(MediaBinField.DSOrderNo.getField(), vo.isVideo() ? 25 : 30); //used for moduleType sequencing on DS only
		doc.setField(MediaBinField.ImportFileCd.getField(), vo.getImportFileCd());
		doc.setField(MediaBinField.Checksum.getField(), vo.getChecksum());
		
		return doc;
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
