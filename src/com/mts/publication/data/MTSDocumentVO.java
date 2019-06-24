package com.mts.publication.data;

// JDK 1.8.x
import java.sql.ResultSet;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// MTS Libs
import com.mts.publication.data.AssetVO.AssetType;
import com.mts.subscriber.data.MTSUserVO;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.content.DocumentVO;
import com.smt.sitebuilder.action.metadata.WidgetMetadataVO;

/****************************************************************************
 * <b>Title</b>: MTSDocumentVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object for the MTS documents and articles
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 2, 2019
 * @updates:
 ****************************************************************************/
@Table(name="mts_document")
public class MTSDocumentVO extends DocumentVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6901558022608072385L;
	
	// Members
	private String documentId;
	private String issueId;
	private Date publishDate;
	private String uniqueCode;
	private String authorId;
	private String sbActionId;
	private String infoBar;
	
	// Sub-Beans
	private List<AssetVO> assets = new ArrayList<>();
	private List<WidgetMetadataVO> categories = new ArrayList<>();
	private List<RelatedArticleVO> relatedArticles = new ArrayList<>();
	private MTSUserVO author;
	
	// Helpers
	private String issueName;
	private String publicationId;
	private String publicationName;
	private String userInfoId;
	
	/**
	 * 
	 */
	public MTSDocumentVO() {
		super();
	}

	/**
	 * @param req
	 */
	public MTSDocumentVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public MTSDocumentVO(ResultSet rs) {
		super(rs);
	}
	
	/**
	 * Returns the single channel that an article belongs
	 * @return
	 */
	public WidgetMetadataVO getCategory() {
		log.info("*******");
		for (WidgetMetadataVO cat : categories) {
			log.info(cat);
			if ("CHANNELS".equals(cat.getParentId())) return cat;
		}
		
		return null;
	}
	
	/**
	 * Returns the age of the article in days since it was published
	 * @return
	 */
	public long getNumDaysPublished() {
		Date now = new Date();
		if (publishDate == null || publishDate .after(now)) return 0;
		return ChronoUnit.DAYS.between(publishDate.toInstant(), now.toInstant());
	}
	
	/**
	 * Returns the singular image for the teaser on this document
	 * @return
	 */
	public AssetVO getTeaserAsset() {
		Map<String, AssetVO> fAssets = new HashMap<>();
		Collections.shuffle(assets);
		for (AssetVO vo : assets) {
			if (AssetType.TEASER_IMG.equals(vo.getAssetType())) {
				if (vo.getObjectKeyId().equals(documentId)) return vo;
				else if (PublicationTeaserVO.DEFAULT_TEASER_IMG.equals(vo.getObjectKeyId()))
					fAssets.put(PublicationTeaserVO.DEFAULT_TEASER_IMG, vo);
				else fAssets.put(PublicationTeaserVO.CATEGORY_IMG, vo);
			}
		}
		
		return (fAssets.size() == 1) ? fAssets.get(PublicationTeaserVO.DEFAULT_TEASER_IMG) : fAssets.get(PublicationTeaserVO.CATEGORY_IMG);
	}
	
	/**
	 * Returns the singular image for the feature on this document
	 * @return
	 */
	public AssetVO getFeatureAsset() {
		Map<String, AssetVO> fAssets = new HashMap<>();
		Collections.shuffle(assets);
		for (AssetVO vo : assets) {
			if (AssetType.FEATURE_IMG.equals(vo.getAssetType())) {
				if (vo.getObjectKeyId().equals(documentId)) return vo;
				else if (PublicationTeaserVO.DEFAULT_FEATURE_IMG.equals(vo.getObjectKeyId()))
					fAssets.put(PublicationTeaserVO.DEFAULT_FEATURE_IMG, vo);
				else fAssets.put(PublicationTeaserVO.CATEGORY_IMG, vo);
			}
		}
		
		return (fAssets.size() == 1) ? fAssets.get(PublicationTeaserVO.DEFAULT_FEATURE_IMG) : fAssets.get(PublicationTeaserVO.CATEGORY_IMG);

	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBModuleVO#getApprovableFlag()
	 */
	@Override
	@Column(name="approvable_flg", isReadOnly=true)
	public Integer getApprovableFlag() {
		return super.getApprovableFlag();
	}
	
	/**
	 * @return the documentId
	 */
	@Column(name="document_id", isPrimaryKey=true)
	public String getDocumentId() {
		return documentId;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.content.DocumentVO#getActionGroupId()
	 */
	@Override
	@Column(name="action_group_id")
	public String getActionGroupId() {
		return super.getActionGroupId();
	}

	/**
	 * @return the issueId
	 */
	@Column(name="issue_id")
	public String getIssueId() {
		return issueId;
	}

	/**
	 * @return the assets
	 */
	public List<AssetVO> getAssets() {
		return assets;
	}

	/**
	 * @return the categories
	 */
	public List<WidgetMetadataVO> getCategories() {
		return categories;
	}

	/**
	 * Creates a comma separated list form the collection of categories
	 * @return
	 */
	public String getCatList() {
		return getCatList(null);
	}
	
	/**
	 * Creates a comma separated list form the collection of categories
	 * @return
	 */
	public String getCatList(String parentId) {
		if (categories == null || categories.isEmpty()) return "";
		
		StringBuilder val = new StringBuilder(64);
		int i=0;
		for (WidgetMetadataVO cat : categories) {
			if (!StringUtil.isEmpty(parentId) && !parentId.contentEquals(cat.getParentId())) continue;
			if (i++ > 0) val.append(",");
			val.append(cat.getWidgetMetadataId());
		}
		
		return val.toString();
	}
	
	/**
	 * @return the publishDate
	 */
	@Column(name="publish_dt")
	public Date getPublishDate() {
		return publishDate;
	}

	/**
	 * @return the articleCode
	 */
	@Column(name="unique_cd")
	public String getUniqueCode() {
		return uniqueCode;
	}

	/**
	 * @return the authorId
	 */
	@Column(name="author_id")
	public String getAuthorId() {
		return authorId;
	}

	/**
	 * @return the issueName
	 */
	@Column(name="issue_nm", isReadOnly=true)
	public String getIssueName() {
		return issueName;
	}

	/**
	 * @return the publicationId
	 */
	@Column(name="publication_id", isReadOnly=true)
	public String getPublicationId() {
		return publicationId;
	}

	/**
	 * @return the publicationName
	 */
	@Column(name="publication_nm", isReadOnly=true)
	public String getPublicationName() {
		return publicationName;
	}

	/**
	 * @return the sbActionId
	 */
	public String getSbActionId() {
		return sbActionId;
	}

	/**
	 * @return the infoBar
	 */
	@Column(name="info_bar_txt")
	public String getInfoBar() {
		return infoBar;
	}

	/**
	 * @return the userInfoId
	 */
	@Column(name="user_info_id", isReadOnly=true)
	public String getUserInfoId() {
		return userInfoId;
	}

	/**
	 * @param sbActionId the sbActionId to set
	 */
	public void setSbActionId(String sbActionId) {
		this.sbActionId = sbActionId;
	}

	/**
	 * @return the author
	 */
	public MTSUserVO getAuthor() {
		return author;
	}

	/**
	 * @param documentId the documentId to set
	 */
	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}

	/**
	 * @param issueId the issueId to set
	 */
	public void setIssueId(String issueId) {
		this.issueId = issueId;
	}

	/**
	 * @param assets the assets to set
	 */
	public void setAssets(List<AssetVO> assets) {
		this.assets = assets;
	}
	
	/**
	 * @param asset Add an asset to the assets set
	 */
	@BeanSubElement
	public void addAsset(AssetVO asset) {
		if (asset == null || StringUtil.isEmpty(asset.getDocumentAssetId())) return;
		this.assets.add(asset);
	}

	/**
	 * @param categories the categories to set
	 */
	public void setCategories(List<WidgetMetadataVO> categories) {
		this.categories = categories;
	}
	
	/**
	 * @param cat Add an category to the categories set
	 */
	@BeanSubElement
	public void addCategory(WidgetMetadataVO cat) {
		this.categories.add(cat);
	}

	/**
	 * @param publishDate the publishDate to set
	 */
	public void setPublishDate(Date publishDate) {
		this.publishDate = publishDate;
	}

	/**
	 * @param articleCode the articleCode to set
	 */
	public void setUniqueCode(String uniqueCode) {
		this.uniqueCode = uniqueCode;
	}

	/**
	 * @param authorId the authorId to set
	 */
	public void setAuthorId(String authorId) {
		this.authorId = authorId;
	}

	/**
	 * @param author the author to set
	 */
	@BeanSubElement
	public void setAuthor(MTSUserVO author) {
		this.author = author;
	}

	/**
	 * @param issueName the issueName to set
	 */
	public void setIssueName(String issueName) {
		this.issueName = issueName;
	}

	/**
	 * @param publicationId the publicationId to set
	 */
	public void setPublicationId(String publicationId) {
		this.publicationId = publicationId;
	}

	/**
	 * @param publicationName the publicationName to set
	 */
	public void setPublicationName(String publicationName) {
		this.publicationName = publicationName;
	}

	/**
	 * @param infoBar the infoBar to set
	 */
	public void setInfoBar(String infoBar) {
		this.infoBar = infoBar;
	}

	/**
	 * @param userInfoId the userInfoId to set
	 */
	public void setUserInfoId(String userInfoId) {
		this.userInfoId = userInfoId;
	}

	/**
	 * @return the relatedArticles
	 */
	public List<RelatedArticleVO> getRelatedArticles() {
		return relatedArticles;
	}

	/**
	 * @param relatedArticles the relatedArticles to set
	 */
	public void setRelatedArticles(List<RelatedArticleVO> relatedArticles) {
		this.relatedArticles = relatedArticles;
	}
}

