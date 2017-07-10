package com.ram.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.ram.action.or.RAMCaseManager;
import com.ram.action.or.vo.RAMCaseItemVO;
import com.ram.action.or.vo.RAMCaseKitVO;
import com.ram.action.or.vo.RAMCaseVO;
import com.ram.action.or.vo.RAMCaseVO.RAMCaseStatus;
import com.ram.action.products.ProductCartFacadeAction;
import com.ram.action.provider.VisionAction;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> SPDAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Action for managing SPD System Interactions.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Jul 7, 2017
 ****************************************************************************/
public class SPDAction extends SimpleActionAdapter {

	public SPDAction() {
		super();
	}

	public SPDAction(ActionInitVO init) {
		super(init);
	}

	@Override
	public void build(ActionRequest req) throws ActionException {
		log.debug("Incoming Request Logged");
		if(Convert.formatBoolean(req.getParameter("isComplete"))) {
			completeKit(req);
		} else if(Convert.formatBoolean(req.getParameter("updateItem"))) {
			updateCaseItemRecord(req);
		}
	}

	/**
	 * Helper method that manages completing a kit.  If all kits are completed,
	 * close the case.
	 * @param req
	 * @throws ActionException
	 */
	private void completeKit(ActionRequest req) throws ActionException {
		RAMCaseManager rcm = getCaseManager(req);

		try {
			RAMCaseVO cVo = rcm.retrieveCase(req.getParameter("caseId"));
			RAMCaseKitVO kVo = cVo.getKits().get(req.getParameter("caseKitId"));

			//Update Kit Status
			kVo.setProcessedFlg(1);

			//Save the Case
			//rcm.saveCase(req);

			//Finalize it to the DB.
			rcm.persistCaseDefault(cVo);
			rcm.finalizeCaseInfo();

		} catch (Exception e) {
			throw new ActionException(e);
		}
	}

	/**
	 * Helper method that manages creating/updated a case Item Record.
	 * @param req
	 * @throws ActionException
	 */
	private void updateCaseItemRecord(ActionRequest req) throws ActionException {
		try {
			RAMCaseItemVO civo = getCaseManager(req).updateItem(req);
			putModuleData(civo);
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}

	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if(req.hasParameter("productId")) {
			loadCaseDataByProduct(req);
		} else {
			loadProductCartAction(req);
		}
	}

	/**
	 * @param req
	 * @throws ActionException 
	 */
	private void loadProductCartAction(ActionRequest req) throws ActionException {
		ProductCartFacadeAction pca = new ProductCartFacadeAction(this.actionInit);
		pca.setAttributes(attributes);
		pca.setDBConnection(dbConn);
		pca.retrieve(req);
	}

	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	/**
	 * Helper method performs lookup for Case Kit where productId matches what's
	 * on the request and is in OR_Complete Status.  Will be replaced with a 
	 * direct lookup on the locationItemMaster Table After Demo.
	 * @param req
	 * @throws Exception 
	 */
	private void loadCaseDataByProduct(ActionRequest req) throws ActionException {
		try{
			//String [] res = getRamCaseId(req);
			RAMCaseVO cVo = getCaseManager(req).retrieveCase(req.getParameter("caseId"));
	
			/*
			 * Get list of consumed RAMCaseItemVOs off the RAMCaseVO with the
			 * given CaseKitId.
			 */
			List<RAMCaseItemVO> items = extractConsumedKitData(cVo, req.getParameter("caseKitId"));

			/*
			 * Load Full Kit Data for the given Product Id and pass the list
			 * of consumed items so we can correct the data initially.  The
			 * result will automatically be placed in the attributes ModuleData.
			 */
			loadKitData(req, items);

		} catch(Exception e) {
			log.error("Could not find Case Info", e);
			throw new ActionException("Could not find Case Info.", e);
		}
	}

	/**
	 * Helper method returns an Array of the RAMCaseId and RAMCaseKitId at
	 * indices 0 and 1 resepectively.  If no results are found then we have no
	 * reason to continue so we throw new ActionException.
	 * @param req
	 * @return
	 * @throws ActionException 
	 */
	private String [] getRamCaseId(ActionRequest req) throws ActionException {
		int productId = Convert.formatInteger(req.getParameter("productId"));
		String [] res = null;
		try(PreparedStatement ps = dbConn.prepareStatement(getCaseIdLookupSql())) {
			ps.setInt(1, productId);
			ps.setString(2, RAMCaseStatus.OR_COMPLETE.toString());

			ResultSet rs = ps.executeQuery();

			if(rs.next()) {
				res = new String [2];
				res[0] = rs.getString("case_id");
				res[1] = rs.getString("case_kit_id");
			} else {
				throw new ActionException("Could not find Case Info");
			}
		} catch (SQLException e) {
			log.error("Error Processing Code", e);
		}
		return res;
	}

	/**
	 * Extract List of Consumed RAMCaseItemVOs for the given caseKitId.
	 * @param cVo
	 * @return
	 */
	private List<RAMCaseItemVO> extractConsumedKitData(RAMCaseVO cVo, String caseKitId) {
		List<RAMCaseItemVO> kitItems = new ArrayList<>();

		for(Map<String, RAMCaseItemVO> items : cVo.getItems().values()) {
			for(Entry<String, RAMCaseItemVO> item : items.entrySet()) {
				RAMCaseItemVO i = item.getValue();
				if(caseKitId.equals(i.getCaseKitId())) {
					kitItems.add(i);
				}
			}
		}
		return kitItems;
	}

	/**
	 * Helper method that calls out to VisionAction to load the Kit Vision Data.
	 * @param productId
	 * @return
	 */
	private void loadKitData(ActionRequest req, List<RAMCaseItemVO> items) throws ActionException {
		attributes.put(VisionAction.CONSUMED_ITEMS, items);
		req.setParameter("isSpd", "true");
		VisionAction va = new VisionAction(actionInit);
		va.setDBConnection(dbConn);
		va.setAttributes(attributes);
		va.retrieve(req);
	}

	/**
	 * Helper method that creates a new RAMCaseManager.
	 * @return
	 * @throws ActionException
	 */
	public RAMCaseManager getCaseManager(ActionRequest req) {
		return new RAMCaseManager(attributes, dbConn, req);
	}

	/**
	 * Helper method that generated the Case/CaseKit Id lookup Query.
	 * @return
	 */
	public String getCaseIdLookupSql() {
		StringBuilder sql = new StringBuilder(275);
		String custom = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("select * from ").append(custom).append("ram_case c ");
		sql.append("inner join ").append(custom).append("ram_case_kit k ");
		sql.append("on c.case_id = k.case_id ");
		sql.append("inner join ").append(custom).append("ram_location_item_master i ");
		sql.append("on k.location_item_master_id = i.location_item_master_id ");
		sql.append("where i.product_id = ? and c.case_status_cd = ?");
		return sql.toString();
	}
}