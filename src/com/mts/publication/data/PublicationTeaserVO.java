package com.mts.publication.data;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.metadata.WidgetMetadataVO;

/****************************************************************************
 * <b>Title</b>: PublicationTeaserVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value Object for the display fo the teaser info on the home page
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jun 4, 2019
 * @updates:
 ****************************************************************************/
public class PublicationTeaserVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * 
	 */
	public static final int NUM_TEASER_ARTICLES = 3;
	
	
	public static final String DEFAULT_FEATURE_IMG = "DEFAULT_FEATURE_IMG";
	public static final String DEFAULT_TEASER_IMG = "DEFAULT_TEASER_IMG";
	public static final String CATEGORY_IMG = "DEFAULT_TEASER_IMG";
	
	// Sub-Beans
	private PublicationVO publication = new PublicationVO();
	private List<MTSDocumentVO> documents = new ArrayList<>();
	
	// Members
	private Map<String, List<AssetVO>> assets = new HashMap<>();
	private String categoryCode;
	
	// Helpers
	private String featuredArticleId;
	
	/**
	 * 
	 */
	public PublicationTeaserVO() {
		super();
	}

	/**
	 * @param req
	 */
	public PublicationTeaserVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public PublicationTeaserVO(ResultSet rs) {
		super(rs);
	}
	
	/**
	 * Gets the featured article randomly.  Shuffles the list of documents 
	 * and grabs the first one
	 * @return
	 */
	public MTSDocumentVO getFeaturedArticle() {
		if (documents.isEmpty()) return new MTSDocumentVO();
		
		// If the feature has been assigned already, use that
		if (! StringUtil.isEmpty(featuredArticleId)) {
			for (MTSDocumentVO d : documents) {
				if (featuredArticleId.equals(d.getDocumentId())) {
					assignAsset(d);
					return d;
				}
			}
		}
		
		// Otherwise shuffle the articles and return the item
		Collections.shuffle(documents);
		MTSDocumentVO doc = documents.get(0);
		assignAsset(doc);
		featuredArticleId = doc.getDocumentId();
		return doc;
	}
	
	/**
	 * Gets the teaser articles randomly
	 * @return
	 */
	public List<MTSDocumentVO> getTeaserArticles() {
		// Make sure the featured article is assigned
		if (StringUtil.isEmpty(featuredArticleId)) getFeaturedArticle();
		
		int num = (documents.size() - 1 < NUM_TEASER_ARTICLES) ? documents.size() - 1 : NUM_TEASER_ARTICLES;
		List<MTSDocumentVO> teasers = new ArrayList<>(num);
		Set<String> ids = new HashSet<>(num);
		for (MTSDocumentVO doc : documents) {
			// Do not use the featured article in the teasers
			if (featuredArticleId.equals(doc.getDocumentId())) continue;
			
			// Check the ids in the teasers.  If it is not there, add it
			if (! ids.contains(doc.getDocumentId())) {
				assignAsset(doc);
				teasers.add(doc);
				ids.add(doc.getDocumentId());
				
				// When the number of teasers is assigned, end the loop
				if (ids.size() == num) break;
			}
			
		}
		
		return teasers;
	}
	
	/**
	 * Assigns the assets available to the document
	 * @param doc
	 * @param at
	 */
	public void assignAsset(MTSDocumentVO doc) {
		List<AssetVO> docAssets = assets.get(doc.getDocumentId());
		if (docAssets == null) docAssets = new ArrayList<>();
		for (WidgetMetadataVO cat : doc.getCategories()) {
			if(assets.containsKey(cat.getWidgetMetadataId())) 
				docAssets.addAll(assets.get(cat.getWidgetMetadataId()));
		}
		
		if (! docAssets.isEmpty()) doc.setAssets(docAssets);
		//TODO this is why it was failing null pointers here
		if(assets != null)
		log.debug("##### asset count " + assets.size() );
		for (String as : assets.keySet()) {
			log.debug("#" + as);
			log.debug("## " + assets.get(as) );
			log.debug("###");
		}
		log.debug("##### asset count " + assets.size() );
		
		
		if(assets.get(DEFAULT_FEATURE_IMG) != null)doc.addAsset(assets.get(DEFAULT_FEATURE_IMG).get(0));
		
		if(assets.get(DEFAULT_TEASER_IMG) != null)doc.addAsset(assets.get(DEFAULT_TEASER_IMG).get(0));
	}
	
	/**
	 * Returns a unique list of object ids so the list of assets can be returned
	 * @return
	 */
	public Set<String> getAssetObjectKeys() {
		Set<String> ids = new HashSet<>();
		ids.addAll(Arrays.asList(DEFAULT_TEASER_IMG, DEFAULT_FEATURE_IMG));
		for (MTSDocumentVO doc : documents) {
			if (! StringUtil.isEmpty(doc.getDocumentId())) ids.add(doc.getDocumentId());
			for (WidgetMetadataVO cat : doc.getCategories()) {
				if (! StringUtil.isEmpty(cat.getWidgetMetadataId())) 
					ids.add(cat.getWidgetMetadataId());
			}
		}
		
		return ids;
	}

	/**
	 * Adds an asset to the collection
	 * @param asset
	 */
	public void addAsset(AssetVO asset) {
		if (assets.containsKey(asset.getObjectKeyId()))
			assets.get(asset.getObjectKeyId()).add(asset);
		else {
			List<AssetVO> assetList = new ArrayList<>();
			assetList.add(asset);
			assets.put(asset.getObjectKeyId(), assetList);
		}
	}

	/**
	 * @return the publication
	 */
	public PublicationVO getPublication() {
		return publication;
	}

	/**
	 * @return the documents
	 */
	public List<MTSDocumentVO> getDocuments() {
		return documents;
	}

	/**
	 * @param publication the publication to set
	 */
	@BeanSubElement
	public void setPublication(PublicationVO publication) {
		this.publication = publication;
	}

	/**
	 * @param documents the documents to set
	 */
	public void setDocuments(List<MTSDocumentVO> documents) {
		this.documents = documents;
	}

	/**
	 * 
	 * @param document
	 */
	public void addDocument(MTSDocumentVO document) {
		if (! StringUtil.isEmpty(document.getDocumentId()))
			documents.add(document);
	}

	/**
	 * @return the assets
	 */
	public Map<String, List<AssetVO>> getAssets() {
		return assets;
	}

	/**
	 * @return the categoryCode
	 */
	@Column(name="category_cd")
	public String getCategoryCode() {
		return categoryCode;
	}

	/**
	 * @param categoryCode the categoryCode to set
	 */
	public void setCategoryCode(String categoryCode) {
		this.categoryCode = categoryCode;
	}
}
