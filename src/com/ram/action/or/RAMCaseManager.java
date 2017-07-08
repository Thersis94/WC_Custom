package com.ram.action.or;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.ram.action.or.vo.RAMCaseItemVO;
import com.ram.action.or.vo.RAMCaseKitVO;
import com.ram.action.or.vo.RAMCaseVO;
import com.ram.action.or.vo.RAMCaseVO.RAMCaseStatus;
import com.ram.action.or.vo.RAMSignatureVO;
import com.ram.datafeed.data.RAMProductVO;
import com.ram.persistence.AbstractPersist;
import com.ram.persistence.RAMCasePersistenceFactory;
import com.ram.persistence.RAMCasePersistenceFactory.PersistenceType;
import com.ram.workflow.data.vo.LocationItemMasterVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.filter.fileupload.Constants;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.UUIDGenerator;

import opennlp.tools.util.StringUtil;

/****************************************************************************
 * <b>Title:</b> RAMCaseManager.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Action manages Case Interactions.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Jun 28, 2017
 ****************************************************************************/
public class RAMCaseManager {

	public static final String RAM_CASE_ID = "caseId";
	public static final String RAM_CASE_VO = "ramCaseVO";
	public static final String SALES_SIGNATURE = "sales";
	public static final String SALES_SIGNATURE_DT = "salesDt";
	public static final String ADMIN_SIGNATURE = "admin";
	public static final String ADMIN_SIGNATURE_DT = "adminDt";
	public static final String SIGN_DATE_PATTERN = "MM/dd/yyyy hh:mm";
	public static final String RAM_PERSISTENCE_TYPE = "ramPersistenceType";

	private Logger log;
	private Map<String, Object> attributes;

	private Connection conn;
	public RAMCaseManager() {
		log = Logger.getLogger(getClass());
	}

	public RAMCaseManager(Map<String, Object> attributes, Connection conn) throws InvalidDataException {
		this();
		this.attributes = attributes;
		if(conn != null) {
			this.conn = conn;
		} else {
			throw new InvalidDataException("Passed Connection was null.");
		}
	}


	/**
	 * Helper method manages creating a new RAMCaseVO.
	 * @return
	 * @throws Exception 
	 */
	public RAMCaseVO createCase() throws Exception {
		return (RAMCaseVO) buildPI(null, null, PersistenceType.DB).initialize();
	}

	/**
	 * Helper method that manages reading Signature data off the request and
	 * adding it to the RAMCaseVO.
	 * @param req
	 * @param cVo
	 * @return
	 * @throws Exception 
	 */
	public RAMCaseVO addSignature(ActionRequest req) throws Exception {
		RAMCaseVO cVo = retrieveCase(req.getParameter(RAM_CASE_ID));
		RAMSignatureVO s = new RAMSignatureVO(req);
		cVo.addSignature(s);
		persistCase(cVo, null);
		return cVo;
	}

	/**
	 * Helper method manages saving a RAMCaseVO.
	 * @throws Exception 
	 */
	public RAMCaseVO saveCase(ActionRequest req) throws Exception {
		RAMCaseVO cVo = retrieveCase(req.getParameter(RAM_CASE_ID));
		cVo.setData(req);

		updateCaseInfo(cVo);
		return cVo;
	}

	/**
	 * Helper method mangages retrieving a RAMCaseVO.
	 * @throws Exception 
	 */
	public RAMCaseVO retrieveCase(String ramCaseId) throws Exception {
		return (RAMCaseVO) buildPI(null, ramCaseId).load();
	}

	/**
	 * Helper method used to add an Item to the RAMCaseVO built using data on
	 * the ActionRequest.
	 * @param req
	 * @throws Exception
	 */
	public RAMCaseItemVO updateItem(ActionRequest req) throws Exception {
		RAMCaseItemVO item;
		RAMCaseVO cVo = retrieveCase(req.getParameter(RAM_CASE_ID));

		if(req.hasParameter("caseItemId")) {
			item = getCaseItem(cVo, req);
		} else {
			item = buildCaseItem(cVo, req);
		}

		cVo.addItem(item);

		//Save Case.
		this.updateCaseInfo(cVo);
		return item;
	}

	/**
	 * Helper method that returns a RAMCaseKitVO based on given ProductInfo and
	 * serialId.
	 * @param p
	 * @param serialId
	 * @return
	 */
	private RAMCaseKitVO loadLocationKitData(String serialId) {
		DBProcessor db = new DBProcessor(conn, (String)attributes.get(Constants.CUSTOM_DB_SCHEMA));
		List<Object> params = new ArrayList<>();
		params.add(serialId);
		LocationItemMasterVO lim = (LocationItemMasterVO) db.executeSelect(getLocationKitDataSql(), params, new LocationItemMasterVO());
		RAMCaseKitVO kit = new RAMCaseKitVO();
		kit.setLocationItemMasterId(lim.getLocationItemMasterId());
		return kit;
	}

	/**
	 * Helper method returns query used for retrieving locationItemMaster data.
	 * @return
	 */
	private String getLocationKitDataSql() {
		StringBuilder sql = new StringBuilder(150);
		sql.append("select location_item_master_id from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("RAM_LOCATION_ITEM_MASTER where serial_no_txt = ? ");
		return sql.toString();
	}

	/**
	 * Helper method that looks up Product Data for the given productId
	 * @param productId
	 * @return
	 */
	private RAMProductVO lookupProduct(int productId) {
		RAMProductVO p = new RAMProductVO();
		p.setProductId(productId);
		try {
			new DBProcessor(conn).getByPrimaryKey(p);
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Error Processing Code", e);
		}
		return p;
	}

	/**
	 * Helper method that retrieves a CaseItem off the given RAMCaseVO
	 * @param cVo
	 * @param req
	 * @return
	 */
	private RAMCaseItemVO getCaseItem(RAMCaseVO cVo, ActionRequest req) {
		RAMCaseItemVO item = null;
		if(req.hasParameter("caseTypeCd") && req.hasParameter("caseItemId")) {
			item = cVo.getItems().get(req.getParameter("caseTypeCd")).get(req.getParameter("caseItemId"));
		}
		return item;
	}

	/**
	 * Helper method manages adding an item to a case
	 * @throws Exception 
	 */
	private RAMCaseItemVO buildCaseItem(RAMCaseVO cVo, ActionRequest req) throws Exception {
		int productId = Convert.formatInteger(req.getParameter("productId"));
		String serialId = req.getParameter("serialId");
		RAMCaseKitVO kvo = null;
		RAMProductVO p = lookupProduct(productId);

		/*
		 * If we don't have a caseKitId and this product is a kit,
		 * build a case KitVO.
		 */
		if(StringUtil.isEmpty(req.getParameter("caseKitId")) && p != null && Integer.valueOf(1).equals(p.getKitFlag())) {
			kvo = loadLocationKitData(serialId);
			kvo.setCaseId(cVo.getCaseId());
			kvo.setCaseKitId(new UUIDGenerator().getUUID());
			req.setParameter("caseKitId", kvo.getCaseKitId());
			cVo.addCaseKit(kvo);
		}

		//Build RAMCaseItem
		return new RAMCaseItemVO(req);
	}

	/**
	 * Helper method manages removing an item from a CaseVO.
	 * @param cmv
	 * @param item
	 * @throws Exception 
	 */
	public void removeCaseItem(ActionRequest req) throws Exception {
		//Get the Case
		RAMCaseVO cVo = retrieveCase(req.getParameter("caseId"));

		//Get the Item off the Case
		RAMCaseItemVO item = getCaseItem(cVo, req);

		//Remove the Item
		cVo.removeItem(item);

		//Persist the Case.
		updateCaseInfo(cVo);
	}

	/**
	 * Helper method manages updating a RAMCaseVO.
	 * @param cVo
	 * @throws Exception 
	 */
	public void updateCaseInfo(RAMCaseVO cVo) throws Exception {
		persistCase(cVo, null);
	}

	/**
	 * Persistence method that Persists to the Database.
	 * @param cVo
	 * @return
	 * @throws Exception
	 */
	public RAMCaseVO finalizeCaseInfo(RAMCaseVO cVo) throws Exception {
		return (RAMCaseVO) buildPI(cVo, null, PersistenceType.DB).save();
	}

	/**
	 * Persistence method that Persists to configured Persistence Type.
	 * @param cVo
	 * @param caseId
	 * @return
	 * @throws Exception
	 */
	public RAMCaseVO persistCase(RAMCaseVO cVo, String caseId) throws Exception {
		return (RAMCaseVO) buildPI(cVo, caseId).save();
	}

	/**
	 * Helper method updates the Status for a RAMCaseVO.  If the status is a
	 * final Status, finalize it.  Otherwise Update it.
	 * @param req
	 * @param st
	 * @return
	 * @throws Exception
	 */
	public RAMCaseVO updateStatus(ActionRequest req, RAMCaseStatus st) throws Exception {
		RAMCaseVO cVo = retrieveCase(req.getParameter(RAM_CASE_ID));
		if(st != null) {
			cVo.setCaseStatus(st);
			if(st.toString().toLowerCase().contains("complete")) {
				cVo = finalizeCaseInfo(cVo);
			} else {
				updateCaseInfo(cVo);
			}
		}
		return cVo;
	}

	/**
	 * Helper method that retrieves a Persistence Instance based on config value. 
	 * @param cVo
	 * @param caseId
	 * @return
	 * @throws Exception
	 */
	private AbstractPersist<?, ?> buildPI(RAMCaseVO cVo, String caseId) throws Exception {
		PersistenceType pt = PersistenceType.valueOf((String)attributes.get(RAM_PERSISTENCE_TYPE));
		return buildPI(cVo, caseId, pt);
	}
	/**
	 * Helper method retrieves the AbstractPersistManager for persisting RAMCase
	 * Data.
	 * @return
	 * @throws Exception
	 */
	private AbstractPersist<?, ?> buildPI(RAMCaseVO cVo, String caseId, PersistenceType pt) throws Exception {
		if(cVo != null) {
			attributes.put(RAM_CASE_VO, cVo);
		} else if(!StringUtil.isEmpty(caseId)) {
			attributes.put(RAM_CASE_ID, caseId);
		}
		AbstractPersist<?,?> pi = RAMCasePersistenceFactory.loadPersistenceObject(pt, conn, attributes);
		pi.setAttributes(attributes);

		return pi;
	}
}