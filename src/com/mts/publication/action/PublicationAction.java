package com.mts.publication.action;

// JDK 1.8.x
import java.util.List;

// MTS Libs
import com.mts.publication.data.PublicationVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.*;

//WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: PublicationAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Widget to manage MTS Publications
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 8, 2019
 * @updates:
 ****************************************************************************/

public class PublicationAction extends SBActionAdapter {
	
	/**
	 * Ajax Controller key for this action
	 */
	public static final String AJAX_KEY = "pubs";
	
	/**
	 * 
	 */
	public PublicationAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public PublicationAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.info("*****");
		setModuleData(getPublications());
	}
	
	/**
	 * 
	 * @return
	 */
	public List<PublicationVO> getPublications() {
		StringBuilder sql = new StringBuilder(352);
		sql.append("select coalesce(issues_count, 0) as issue_no, a.*, b.* from ");
		sql.append(getCustomSchema()).append("mts_publication a ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("mts_user b ");
		sql.append("on a.editor_id = b.user_id ");
		sql.append("left outer join ( ");
		sql.append("select publication_id, count(*) as issues_count ");
		sql.append("from ").append(getCustomSchema()).append("mts_issue ");
		sql.append("group by publication_id ");
		sql.append(") c on a.publication_id = c.publication_id ");
		sql.append("order by publication_nm ");
		log.debug(sql.length() + "|" + sql);
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), null, new PublicationVO());
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		
		PublicationVO pub = new PublicationVO(req);
		
		try {
			DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
			db.save(pub);
			putModuleData(pub);
		} catch (Exception e) {
			log.error("Unable to save publication info", e);
			putModuleData(pub, 1, false, e.getLocalizedMessage(), true);
		}
	}

}

