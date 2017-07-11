package com.ram.action.or;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
import com.siliconmtn.http.filter.fileupload.Constants;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

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
	public static final String CASE_STORAGE_ITEM = "caseStorageitem";
	public static final String STORAGE_SUFFIX = "CASE_SUFFIX_";
	
	private Logger log;
	private Map<String, Object> attributes;
	private ActionRequest req;
	private Connection conn;
	private Map<PersistenceType, AbstractPersist<?, ?>> pTypes = new LinkedHashMap<>(4);
	private PersistenceType defaultPType;
	private PersistenceType permPType;
	
	private RAMCaseManager() {
		log = Logger.getLogger(getClass());
	}

	public RAMCaseManager(Map<String, Object> attributes, Connection conn, ActionRequest req) {
		this();
		this.attributes = attributes;
		this.req = req;
		this.conn = conn;
		
		//Load the persistence Types
		defaultPType = PersistenceType.valueOf((String)attributes.get(RAM_PERSISTENCE_TYPE));
		permPType = (PersistenceType.DB.equals(defaultPType)) ? PersistenceType.SESSION : PersistenceType.DB;
		try {
			pTypes.put(PersistenceType.SESSION, RAMCasePersistenceFactory.loadPersistenceObject(PersistenceType.SESSION, req, attributes));
			pTypes.put(PersistenceType.DB, RAMCasePersistenceFactory.loadPersistenceObject(PersistenceType.DB, conn, attributes));
		} catch(Exception e) {
			log.error("Unable to load persistence typse:", e);
		}
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
		persistCaseDefault(cVo);
		return cVo;
	}

	/**
	 * Helper method manages saving a RAMCaseVO.
	 * @throws Exception 
	 */
	public RAMCaseVO saveCase(ActionRequest req) throws Exception {
		String caseId = req.getParameter(RAM_CASE_ID);
		RAMCaseVO cVo = null;
		AbstractPersist<?,?> ap = pTypes.get(defaultPType);
		
		if (StringUtil.isEmpty(caseId)) {
			cVo = (RAMCaseVO) ap.initialize();
			req.setParameter(RAM_CASE_ID, new UUIDGenerator().getUUID());
			cVo.setCaseStatus(RAMCaseStatus.OR_IN_PROGRESS);
			cVo.setNewCase(true);
		} else {
			// Load the case form the default location
			cVo = (RAMCaseVO) ap.load(req.getParameter(RAM_CASE_ID));
			
			// Otherwise load from the database
			if (cVo == null) {
				AbstractPersist<?,?> apdb = pTypes.get(permPType);
				cVo = (RAMCaseVO) apdb.load(req.getParameter(RAM_CASE_ID));
				
				if (cVo == null) throw new Exception("Can't find case for caseId: " + caseId);
			}
		}
		
		// Update the data and persists
		cVo.setData(req);
		ap.save(cVo);
		
		return cVo;
	}

	/**
	 * Helper method manages retrieving a RAMCaseVO.
	 * @throws Exception 
	 */
	public RAMCaseVO retrieveCase(String caseId) throws Exception {
		AbstractPersist<?,?> ap = pTypes.get(defaultPType);
		RAMCaseVO cVo = (RAMCaseVO) ap.load(caseId);
		
		if (cVo == null) {
			AbstractPersist<?,?> apdb = pTypes.get(permPType);
			cVo = (RAMCaseVO) apdb.load(req.getParameter(RAM_CASE_ID));
			
			if (cVo == null) throw new Exception("Can't find case for caseId: " + caseId);
		}

		return cVo; 
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
			if(item != null)
				item.setData(req);
		} else {
			item = buildCaseItem(cVo, req);
			cVo.addItem(item);
		}

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
	private RAMCaseKitVO loadLocationKitData(int kitProductId) {
		DBProcessor db = new DBProcessor(conn, (String)attributes.get(Constants.CUSTOM_DB_SCHEMA));
		List<Object> params = new ArrayList<>();
		params.add(kitProductId);
		List<Object> lim = db.executeSelect(getLocationKitDataSql(), params, new LocationItemMasterVO());

		RAMCaseKitVO kit = new RAMCaseKitVO();
		kit.setProductId(kitProductId);
		if(!lim.isEmpty()) {
			kit.setLocationItemMasterId(((LocationItemMasterVO) lim.get(0)).getLocationItemMasterId());
		}
		return kit;
	}

	/**
	 * Helper method returns query used for retrieving locationItemMaster data.
	 * @return
	 */
	private String getLocationKitDataSql() {
		StringBuilder sql = new StringBuilder(150);
		sql.append("select location_item_master_id from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("RAM_LOCATION_ITEM_MASTER where product_id = ? ");
		return sql.toString();
	}

	/**
	 * Helper method that looks up Product Data for the given productId
	 * @param productId
	 * @return
	 */
	private RAMProductVO lookupProduct(int productId) {
		DBProcessor db = new DBProcessor(conn, (String)attributes.get(Constants.CUSTOM_DB_SCHEMA));

		StringBuilder sql = new StringBuilder(100);
		sql.append("select p.*, c.gtin_number_txt || cast(p.gtin_product_id as varchar(64)) as gtin_number_txt ");
		sql.append("from ram_product p left join ram_customer c on p.customer_id = c.customer_id ");
		sql.append("where p.product_id = ? ");
		
		List<Object> params = new ArrayList<>();
		params.add(productId);
		
		RAMProductVO p = (RAMProductVO) db.executeSelect(sql.toString(), params, new RAMProductVO()).get(0);
		
		if (p == null)
			p = new RAMProductVO();
		
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
		int kitProductId = Convert.formatInteger(req.getParameter("kitProductId"));
		RAMCaseKitVO kvo = null;
		RAMProductVO p = lookupProduct(productId);
		RAMProductVO kp = null;
		if(!StringUtil.isEmpty(req.getParameter("kitProductId"))) {
			kp = lookupProduct(kitProductId);
		}
		/*
		 * If we don't have a caseKitId and this product is a kit,
		 * build a case KitVO.
		 */
		if(StringUtil.isEmpty(req.getParameter("caseKitId")) && kp != null && Integer.valueOf(1).equals(kp.getKitFlag())) {
			kvo = cVo.getKit(Integer.valueOf(kitProductId).toString());
			if(kvo == null) {
				kvo = loadLocationKitData(kitProductId);
				kvo.setCaseId(cVo.getCaseId());
				kvo.setCaseKitId(new UUIDGenerator().getUUID());
				cVo.addCaseKit(kvo);
			}

			req.setParameter("caseKitId", kvo.getCaseKitId());			
		}

		//Build RAMCaseItem
		RAMCaseItemVO civo = new RAMCaseItemVO(req);
		civo.setProductNm(p.getProductName());
		civo.setGtinProductId(p.getGtinProductNumber());
		civo.setCaseItemId(new UUIDGenerator().getUUID());
		return civo;
	}

	/**
	 * Helper method manages removing an item from a CaseVO.
	 * @param cmv
	 * @param item
	 * @throws Exception 
	 */
	public String removeCaseItem(ActionRequest req) throws Exception {
		//Get the Case
		RAMCaseVO cVo = retrieveCase(req.getParameter("caseId"));

		//Get the Item off the Case
		RAMCaseItemVO item = getCaseItem(cVo, req);

		//Remove the Item
		cVo.removeItem(item);

		//Persist the Case.
		updateCaseInfo(cVo);
		
		return item.getCaseItemId();
	}

	/**
	 * Helper method manages updating a RAMCaseVO.
	 * @param cVo
	 * @throws Exception 
	 */
	public void updateCaseInfo(RAMCaseVO cVo) throws Exception {
		persistCaseDefault(cVo);
	}

	/**
	 * Persistence method that Persists to the Database.
	 * @return
	 * @throws Exception
	 */
	public RAMCaseVO finalizeCaseInfo() throws Exception {
		String caseId = req.getParameter(RAM_CASE_ID);
		AbstractPersist<?,?> ap = pTypes.get(defaultPType);
		RAMCaseVO cVo = (RAMCaseVO)ap.load(caseId);
		setORFinalStatusCode(cVo);
		persistCasePerm(cVo);

		return cVo;
	}

	/**
	 * Sets the status code upon competing the dase
	 * @param cVo
	 */
	private void setORFinalStatusCode(RAMCaseVO cVo) {
		if (cVo.getKits().isEmpty()) {
			cVo.setCaseStatus(RAMCaseStatus.CLOSED);
		} else if(allKitsProcessed(cVo)) {
			cVo.setCaseStatus(RAMCaseStatus.CLOSED);
		} else {
			cVo.setCaseStatus(RAMCaseStatus.OR_COMPLETE);
		}
	}

	/**
	 * @param cVo
	 * @return
	 */
	private boolean allKitsProcessed(RAMCaseVO cVo) {
		boolean allKitsComplete = true;
		for(RAMCaseKitVO k : cVo.getKits().values()) {
			if(k.getCaseKitId() != null && !k.isProcessed()) {
				allKitsComplete = false;
			}
		}

		return allKitsComplete;
	}

	/**
	 * Persistence method that Persists to configured Persistence Type.
	 * @param cVo
	 * @param caseId
	 * @return
	 * @throws Exception
	 */
	public void persistCasePerm(RAMCaseVO cVo) throws Exception {
		AbstractPersist<?,?> apdb = pTypes.get(permPType);
		apdb.save(cVo);
	}
	
	/**
	 * Persistence method that Persists to configured Persistence Type.
	 * @param cVo
	 * @param caseId
	 * @return
	 * @throws Exception
	 */
	public void persistCaseDefault(RAMCaseVO cVo) throws Exception {
		AbstractPersist<?,?> apdb = pTypes.get(defaultPType);
		apdb.save(cVo);
	}

	/**
	 * Helper method updates the Status for a RAMCaseVO.  If the status is a
	 * final Status, finalize it.  Otherwise Update it.
	 * @param req
	 * @param st
	 * @return
	 * @throws Exception
	 */
	public RAMCaseVO updateStatus(RAMCaseStatus st) throws Exception {
		RAMCaseVO cVo = retrieveCase(req.getParameter(RAM_CASE_ID));
		if(st != null) {
			cVo.setCaseStatus(st);
			if(st.toString().toLowerCase().contains("complete")) {
				cVo = finalizeCaseInfo();
			} else {
				updateCaseInfo(cVo);
			}
		}
		return cVo;
	}
}