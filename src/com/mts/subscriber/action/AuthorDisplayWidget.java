package com.mts.subscriber.action;

// JDK 1.8.x
import java.util.Arrays;

import com.mts.publication.action.MTSDocumentAction;
// MTS Libs
import com.mts.publication.data.PublicationVO;
import com.mts.subscriber.data.MTSUserVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;

// WC Libs
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.metadata.WidgetMetadataVO;

/****************************************************************************
 * <b>Title</b>: AuthorDisplayWidget.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Widget to display the Author Biography
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jun 25, 2019
 * @updates:
 ****************************************************************************/

public class AuthorDisplayWidget extends SimpleActionAdapter {

	/**
	 * 
	 */
	public AuthorDisplayWidget() {
		super();
	}

	/**
	 * @param arg0
	 */
	public AuthorDisplayWidget(ActionInitVO arg0) {
		super(arg0);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		MTSUserVO author = new MTSUserVO(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			// Get the author, pubs and categories
			db.getByPrimaryKey(author);
			assignCategories(author);
			assignPublications(author);
			
			// Get the most recent articles for the author
			MTSDocumentAction mda = new MTSDocumentAction(getDBConnection(), getAttributes());
			author.setArticles(mda.getAuthorArticles(author.getUserId()));
			
			// Add the data
			setModuleData(author);
		} catch (InvalidDataException | DatabaseException e) {
			setModuleData(author, 1, e.getLocalizedMessage());
		}
	}
	
	/**
	 * Retirves a unique list of categories for the articles written by the supplied author
	 * @param user
	 */
	public void assignCategories(MTSUserVO author) {
		
		StringBuilder sql = new StringBuilder(256);
		sql.append("select field_nm, c.widget_meta_data_id ");
		sql.append("from ").append(getCustomSchema()).append("mts_document a ");
		sql.append("inner join sb_action b on a.action_group_id = b.action_group_id and b.pending_sync_flg = 0 ");
		sql.append("inner join widget_meta_data_xr c on b.action_id = c.action_id ");
		sql.append("inner join widget_meta_data d on c.widget_meta_data_id = d.widget_meta_data_id ");
		sql.append("where author_id = ? and d.parent_id = 'CHANNELS' ");
		sql.append("group by field_nm, c.widget_meta_data_id ");
		sql.append("order by field_nm ");
		log.debug(sql + "|" + author.getUserId());
		
		DBProcessor db = new DBProcessor(getDBConnection());
		author.setCategories(db.executeSelect(sql.toString(), Arrays.asList(author.getUserId()), new WidgetMetadataVO(), "widget_meta_data_id"));
	}
	
	/**
	 * Retirves a unique list of categories for the articles written by the supplied author
	 * @param user
	 */
	public void assignPublications(MTSUserVO author) {
		
		StringBuilder sql = new StringBuilder(256);
		sql.append("select publication_nm, p.publication_id ");
		sql.append("from custom.mts_publication p ");
		sql.append("inner join custom.mts_issue i on p.publication_id = i.publication_id ");
		sql.append("inner join custom.mts_document d on i.issue_id = d.issue_id ");
		sql.append("where author_id = ? ");
		sql.append("group by publication_nm, p.publication_id ");
		sql.append("order by publication_nm ");
		log.debug(sql + "|" + author.getUserId());
		
		DBProcessor db = new DBProcessor(getDBConnection());
		author.setPublications(db.executeSelect(sql.toString(), Arrays.asList(author.getUserId()), new PublicationVO()));
	}

}

