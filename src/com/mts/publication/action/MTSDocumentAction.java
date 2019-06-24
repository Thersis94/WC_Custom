package com.mts.publication.action;

// JDK 1.8.x
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.mts.admin.action.UserAction;
// MTS Libs
import com.mts.publication.data.MTSDocumentVO;
import com.mts.subscriber.data.MTSUserVO;
// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.util.RandomAlphaNumeric;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.content.DocumentAction;
import com.smt.sitebuilder.action.metadata.WidgetMetadataVO;
import com.smt.sitebuilder.approval.ApprovalDecoratorAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: MTSDocumentAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Extends the Document Management Widget to manage the 
 * extra data tracked by MTS
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 23, 2019
 * @updates:
 ****************************************************************************/

public class MTSDocumentAction extends SimpleActionAdapter {

	/**
	 * Ajax Controller key for this action
	 */
	public static final String AJAX_KEY = "article";
	
	/**
	 * 
	 */
	public MTSDocumentAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public MTSDocumentAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		try {
			IssueArticleAction iac = new IssueArticleAction(getDBConnection(), getAttributes());
			MTSDocumentVO doc = iac.getDocument(null, req.getParameter("uniqueCode"));
			doc.setRelatedArticles(iac.getRelatedArticles(doc.getActionGroupId()));
			
			// Get the article assets
						
			UserAction ua = new UserAction(getDBConnection(), getAttributes());
			MTSUserVO user = ua.getUserProfile(doc.getAuthorId());
			// Get author articles
			
			doc.setAuthor(user);
			setModuleData(doc);
		} catch (Exception e) {
			log.error("Unable to retrieve document", e);
			setModuleData(null, 0, e.getLocalizedMessage());
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) attributes.get( Constants.MODULE_DATA);
		mod.setActionId(req.getParameter("moduleTypeId"));
		
		// Call the document management widget using the approval Decorator
		DocumentAction da = new DocumentAction(getDBConnection(), getAttributes());
		ActionInterface ai = new ApprovalDecoratorAction(da);
		ai.update(req);
		
		// If its a new document, add the group id.
		MTSDocumentVO doc = new MTSDocumentVO(req);
		if (StringUtil.isEmpty(doc.getActionGroupId())) {
			doc.setActionGroupId((String)req.getAttribute(SB_ACTION_GROUP_ID));
		}

		try {
			save(doc);
			
			// Save the categories
			String sbActionId = doc.getSbActionId();
			String userId = getAdminUser(req).getProfileId();
			String[] cats = new String[0];
			if (!StringUtil.isEmpty(req.getParameter("categories"))) {
				cats = StringUtil.checkVal(req.getParameter("categories")).split("\\,");
			}
			
			updateMetadata(sbActionId, cats, userId);
			
			//Remove the redirects from the admin actions and return the data
			req.removeAttribute(Constants.REDIRECT_REQUEST);
			req.removeAttribute(Constants.REDIRECT_URL);
			doc.setDocument("");
			
			setModuleData(doc);
		} catch (Exception e) {
			log.error("unable to save document", e);
			setModuleData(doc, 0, e.getLocalizedMessage());
		}
	}
	
	/**
	 * 
	 * @param doc
	 * @throws DatabaseException
	 */
	public void save(MTSDocumentVO doc) throws DatabaseException {
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			if (StringUtil.isEmpty(doc.getDocumentId())) {
				doc.setDocumentId(doc.getActionGroupId());
				doc.setUniqueCode(RandomAlphaNumeric.generateRandom(6, true).toUpperCase());
				db.insert(doc);
			} else {
				db.update(doc, Arrays.asList(
					"document_id", "unique_cd", "publish_dt", "update_dt", "author_id"
				));
			}
		} catch (Exception e) {
			throw new DatabaseException("Unable to save document", e);
		}
	}
	
	/**
	 * Updates the metadata for the given action
	 * @param actionId
	 * @param categories
	 * @throws DatabaseException
	 */
	public void updateMetadata(String actionId, String[] categories, String userId) 
	throws DatabaseException {
		// Delete any existing entries
		deleteCategories(actionId);
		
		try {
			// Loop the cats and create a VO for each
			if (categories == null || categories.length == 0) return;
			List<WidgetMetadataVO> cats = new ArrayList<>();
			for (String cat : categories) {
				WidgetMetadataVO vo = new WidgetMetadataVO();
				vo.setCreateDate(new Date());
				vo.setCreateById(userId);
				vo.setSbActionId(actionId);
				vo.setWidgetMetadataId(cat.replaceAll("\\s", ""));
				cats.add(vo);
			}
			
			// Save the beans
			DBProcessor db = new DBProcessor(getDBConnection());
			db.executeBatch(cats, true);
		} catch (Exception e) {
			log.error("unable to update categories", e);
			throw new DatabaseException(e.getLocalizedMessage(), e);
		}
	}
	
	/**
	 * Deletes the existing categories before adding the new ones
	 * @param actionId
	 * @throws DatabaseException
	 */
	private void deleteCategories(String actionId) throws DatabaseException {
		String s = "delete from widget_meta_data_xr where action_id = ?";
		try (PreparedStatement ps = dbConn.prepareStatement(s)) {
			ps.setString(1, actionId);
			ps.executeUpdate();
		} catch (Exception e) {
			log.error("unable to update categories", e);
			throw new DatabaseException(e.getLocalizedMessage(), e);
		}
	}
}

