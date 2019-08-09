package com.mts.publication.data;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// MTS Libs
import com.mts.subscriber.data.MTSUserVO;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.metadata.WidgetMetadataVO;

/****************************************************************************
 * <b>Title</b>: RelatedArticleVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object for the Related Articles data
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jun 7, 2019
 * @updates:
 ****************************************************************************/
@Table(name="mts_related_article")
public class RelatedArticleVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	// Members
	private String relatedArticleId;
	private String documentId;
	private String relatedDocumentId;
	private Date createDate;
	
	// Helpers
	private String name;
	private Date publishDate; 
	
	// Sub-Beans
	private MTSUserVO user;
	private List<WidgetMetadataVO> categories = new ArrayList<>();
	
	/**
	 * 
	 */
	public RelatedArticleVO() {
		super();
	}

	/**
	 * @param req
	 */
	public RelatedArticleVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public RelatedArticleVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the relatedArticleId
	 */
	@Column(name="related_article_id", isPrimaryKey=true)
	public String getRelatedArticleId() {
		return relatedArticleId;
	}

	/**
	 * @return the documentId
	 */
	@Column(name="document_id")
	public String getDocumentId() {
		return documentId;
	}

	/**
	 * @return the relatedDocumentId
	 */
	@Column(name="related_document_id")
	public String getRelatedDocumentId() {
		return relatedDocumentId;
	}

	/**
	 * @return the name
	 */
	@Column(name="action_nm", isReadOnly=true)
	public String getName() {
		return name;
	}

	/**
	 * @return the publishDate
	 */
	@Column(name="publish_dt", isReadOnly=true)
	public Date getPublishDate() {
		return publishDate;
	}

	/**
	 * @return the user
	 */
	public MTSUserVO getUser() {
		return user;
	}

	/**
	 * @return the categories
	 */
	public List<WidgetMetadataVO> getCategories() {
		return categories;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param relatedArticleId the relatedArticleId to set
	 */
	public void setRelatedArticleId(String relatedArticleId) {
		this.relatedArticleId = relatedArticleId;
	}

	/**
	 * @param documentId the documentId to set
	 */
	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}

	/**
	 * @param relatedDocumentId the relatedDocumentId to set
	 */
	public void setRelatedDocumentId(String relatedDocumentId) {
		this.relatedDocumentId = relatedDocumentId;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param publishDate the publishDate to set
	 */
	public void setPublishDate(Date publishDate) {
		this.publishDate = publishDate;
	}

	/**
	 * @param user the user to set
	 */
	@BeanSubElement
	public void setUser(MTSUserVO user) {
		this.user = user;
	}

	/**
	 * @param categories the categories to set
	 */
	public void setCategories(List<WidgetMetadataVO> categories) {
		this.categories = categories;
	}
	
	/**
	 * @param categories the categories to set
	 */
	@BeanSubElement
	public void addCategory(WidgetMetadataVO category) {
		if (StringUtil.isEmpty(category.getWidgetMetadataId())) return;
		this.categories.add(category);
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

}
