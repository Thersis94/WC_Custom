package com.mts.publication.action;

import java.util.List;
// JDK 1.8.x
import java.util.Map;

// MTS Libs
import com.mts.publication.data.SponsorVO;

// SMT Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: SponsorAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the sponsor information for MTS
 * <b>Copyright:</b> Copyright (c) 2020
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 1, 2020
 * @updates:
 ****************************************************************************/
public class SponsorAction extends SBActionAdapter {

	/**
	 * Ajax Controller key for this action
	 */
	public static final String AJAX_KEY = "sponsor";
	
	/**
	 * 
	 */
	public SponsorAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public SponsorAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/**
	 * @param actionInit
	 */
	public SponsorAction(SMTDBConnection dbConn, Map<String, Object> attributes) {
		super();
		this.dbConn = dbConn;
		this.attributes = attributes;
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		setModuleData(getSponsors());
	}
	
	/**
	 * Gets a list of all of the sponsors
	 * @return
	 */
	public List<SponsorVO> getSponsors() {
		StringBuilder sql = new StringBuilder(320);
		sql.append("select a.*, coalesce(b.total_img, 0) as total_images from ");
		sql.append(getCustomSchema()).append("mts_sponsor a ");
		sql.append("left outer join (select cast(count(*) as int) as total_img, object_key_id ");
		sql.append("from ").append(getCustomSchema()).append("mts_document_asset ");
		sql.append("where asset_type_cd = 'SPONSOR_IMG' ");
		sql.append("group by object_key_id) as  b on a.sponsor_id = b.object_key_id ");
		sql.append("order by sponsor_nm ");
		log.debug(sql);
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), null, new SponsorVO());
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		try {
			if (req.hasParameter("update")) {
				setModuleData(updateSponsor(req));
			} else {
				this.deleteSponsor(req);
				putModuleData(null, 0, false, null, false);
			}
			
		} catch (Exception e) {
			putModuleData(null, 0, false, e.getLocalizedMessage(), true);
		}
	}
	
	/**
	 * 
	 * @param req
	 * @return
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 */
	public SponsorVO updateSponsor(ActionRequest req) throws InvalidDataException, DatabaseException {
		SponsorVO svo = new SponsorVO(req);
		log.info("Summary: " + svo.getSummaryDesc());
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.save(svo);
		
		return svo;
	}
	
	/**
	 * 
	 * @param req
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 */
	public void deleteSponsor(ActionRequest req) throws InvalidDataException, DatabaseException  {
		SponsorVO svo = new SponsorVO(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.delete(svo);
	}
	
	/**
	 * Gets a particular sponsor and its assets
	 * @param sponsorId
	 * @return
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public SponsorVO getSponsor(String sponsorId) throws InvalidDataException, DatabaseException {
		SponsorVO svo = new SponsorVO();
		svo.setSponsorId(sponsorId);
		
		// Load the sponsor data
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.getByPrimaryKey(svo);
		
		// Get the assets
		AssetAction aa = new AssetAction(getDBConnection(), getAttributes());
		svo.setAssets(aa.getAssets(svo.getSponsorId()));
		
		return svo;
	}
	
}
