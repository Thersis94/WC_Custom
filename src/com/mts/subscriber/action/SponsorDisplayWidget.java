package com.mts.subscriber.action;

// JDK 1.8.x
import java.util.Arrays;

import com.mts.publication.action.AssetAction;
// MTS Libs
import com.mts.publication.action.MTSDocumentAction;
import com.mts.publication.data.PublicationVO;
import com.mts.publication.data.SponsorVO;

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
 * <b>Title</b>: SponsorDisplayWidget.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Widget to display the Sponsor Information
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jun 25, 2019
 * @updates:
 ****************************************************************************/

public class SponsorDisplayWidget extends SimpleActionAdapter {

	/**
	 * 
	 */
	public SponsorDisplayWidget() {
		super();
	}

	/**
	 * @param arg0
	 */
	public SponsorDisplayWidget(ActionInitVO arg0) {
		super(arg0);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		SponsorVO sponsor = new SponsorVO(req);
		AssetAction aa = new AssetAction(getDBConnection(), getAttributes());
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			// Get the sponsor, pubs and categories
			db.getByPrimaryKey(sponsor);
			assignCategories(sponsor);
			assignPublications(sponsor);
			sponsor.setAssets(aa.getAssets(sponsor.getSponsorId()));
			
			// Get the most recent articles for the sponsor
			MTSDocumentAction mda = new MTSDocumentAction(getDBConnection(), getAttributes());
			sponsor.setArticles(mda.getAuthorArticles(sponsor.getSponsorId(), false));
			
			// Add the data
			setModuleData(sponsor);
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Unable to get sponsor info", e);
			setModuleData(sponsor, 1, e.getLocalizedMessage());
		}
	}
	
	/**
	 * Retrieves a unique list of categories for the articles written by the supplied author
	 * @param user
	 */
	public void assignCategories(SponsorVO sponsor) {
		
		StringBuilder sql = new StringBuilder(256);
		sql.append("select field_nm, c.widget_meta_data_id ");
		sql.append("from ").append(getCustomSchema()).append("mts_document a ");
		sql.append("inner join sb_action b on a.action_group_id = b.action_group_id and b.pending_sync_flg = 0 ");
		sql.append("inner join widget_meta_data_xr c on b.action_id = c.action_id ");
		sql.append("inner join widget_meta_data d on c.widget_meta_data_id = d.widget_meta_data_id ");
		sql.append("where sponsor_id = ? and d.parent_id = 'CHANNELS' ");
		sql.append("group by field_nm, c.widget_meta_data_id ");
		sql.append("order by field_nm ");
		log.debug(sql + "|" + sponsor.getSponsorId());
		
		DBProcessor db = new DBProcessor(getDBConnection());
		sponsor.setCategories(db.executeSelect(sql.toString(), Arrays.asList(sponsor.getSponsorId()), new WidgetMetadataVO(), "widget_meta_data_id"));
	}
	
	/**
	 * Retrieves a unique list of categories for the articles written by the supplied author
	 * @param user
	 */
	public void assignPublications(SponsorVO sponsor) {
		
		StringBuilder sql = new StringBuilder(256);
		sql.append("select publication_nm, p.publication_id ");
		sql.append("from custom.mts_publication p ");
		sql.append("inner join custom.mts_issue i on p.publication_id = i.publication_id ");
		sql.append("inner join custom.mts_document d on i.issue_id = d.issue_id ");
		sql.append("where sponsor_id = ? ");
		sql.append("group by publication_nm, p.publication_id ");
		sql.append("order by publication_nm ");
		log.debug(sql + "|" + sponsor.getSponsorId());
		
		DBProcessor db = new DBProcessor(getDBConnection());
		sponsor.setPublications(db.executeSelect(sql.toString(), Arrays.asList(sponsor.getSponsorId()), new PublicationVO()));
	}

}

