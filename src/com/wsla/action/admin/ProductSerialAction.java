package com.wsla.action.admin;

// JDK 1.8.x
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.BatchImport;
import com.wsla.action.ticket.BaseTransactionAction;
import com.wsla.action.ticket.TicketEditAction;
import com.wsla.common.WSLAConstants;

// WC Libs
import com.wsla.data.product.ProductSerialNumberVO;
import com.wsla.data.product.ProductWarrantyVO;
import com.wsla.data.product.WarrantyVO;
import com.wsla.data.ticket.LedgerSummary;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.TicketLedgerVO;
import com.wsla.data.ticket.TicketVO;
import com.wsla.data.ticket.UserVO;

import static com.wsla.action.admin.ProductMasterAction.REQ_PRODUCT_ID;

/****************************************************************************
 * <b>Title</b>: ProductSerialAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the administration of the product_serial table
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James McKain
 * @version 1.0
 * @since Oct 2, 2018
 * @updates:
 ****************************************************************************/

public class ProductSerialAction extends BatchImport {

	public ProductSerialAction() {
		super();
	}

	public ProductSerialAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * Overloaded constructor used for calling between actions.
	 * @param attrs
	 * @param conn
	 */
	public ProductSerialAction(Map<String, Object> attrs, SMTDBConnection conn) {
		this();
		setAttributes(attrs);
		setDBConnection(conn);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("pro serial action retreve called");
		//TODO his method seems to be doing more then one method should, please break this method out into other methods in the future
		String productId = req.getParameter(REQ_PRODUCT_ID);

		if (!StringUtil.isEmpty(productId) && req.hasParameter("serialNo")) {
			// Do serial# lookup for the ticket UI
			ProductWarrantyVO pwvo = getProductSerial(productId, req.getParameter("serialNo"));
			
			// Check for another record
			TicketVO ticket = lookupServiceOrder(productId, req.getParameter("serialNo"));
			
			//check the date and warranty max date
			if(!StringUtil.isEmpty(pwvo.getProviderId()) && req.hasParameter("purchaseDate")){
				
				int max = getMaxWarrantyLength(pwvo.getProviderId());
				Date purchaseDate = req.getDateParameter("purchaseDate");

				long diff = new Date().getTime() - purchaseDate.getTime();
				long numDays = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
				
				log.debug("max days allowed " + max + " number of days ago it was purchased " + numDays);
				if(max < numDays) {
					log.debug("quit early no reason to look up anything they purchased it too long ago");
					putModuleData(new GenericVO(pwvo, ticket));
				}
				
			} 
			
			if(req.hasParameter("ticketId") && pwvo.getDisposeFlag() == 1 && ticket.getTicketId() == null ) {
				TicketEditAction tea = new TicketEditAction();
				tea.setActionInit(actionInit);
				tea.setAttributes(getAttributes());
				tea.setDBConnection(getDBConnection());
				
				ticket = tea.getBaseTicket(req.getStringParameter("ticketId"));
			}
			
			//if its not found and the owner is retail trust them generate a new approved product serial vo 
			//  that is already approved
			if(req.getBooleanParameter("isRetailOwner") && pwvo != null && StringUtil.isEmpty(pwvo.getProductWarrantyId())) {
				DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
				db.setGenerateExecutedSQL(log.isDebugEnabled());
				
				ProductSerialNumberVO pvo = new ProductSerialNumberVO();
				pvo.setProductId(productId);
				pvo.setSerialNumber(req.getParameter("serialNo"));
				pvo.setValidatedFlag(1);
				pvo.setDisposeFlag(0);
				try {
					//save the new product serial record
					db.save(pvo);
					
					Calendar expireDate = Calendar.getInstance();
					expireDate.add( Calendar.YEAR, 10 );
					pwvo.setProductSerialId(pvo.getProductSerialId());
					pwvo.setRequireApprovalFlag(0);
					pwvo.setDisposeFlag(0);
					pwvo.setExpirationDate(expireDate.getTime());
					pwvo.setWarrantyId(WSLAConstants.RETAIL_WARRANTY);
					
					//save the new product warranty record.
					db.save(pwvo);
				} catch (Exception e) {
					log.error("could not save new retailer product warranty vo",e);
				}

			}
			
			// Add the elements in a GVO to the response
			putModuleData(new GenericVO(pwvo, ticket));
			
		} else if (req.getBooleanParameter("bulkSerial")) {
			BSTableControlVO bst = new BSTableControlVO(req, ProductSerialNumberVO.class);
			String pId = req.getParameter("providerId");
			int valFlag = req.getIntegerParameter("validationFlag", 0);
			setModuleData(getSerialsByOem(pId, valFlag, bst));
		} else {
			setModuleData(getSet(productId, new BSTableControlVO(req, ProductSerialNumberVO.class)));
		}
	}

	/**
	 * checks the data base for the longest max warranty length and returns it returns -1 if it can not find a max length
	 * @param providerId
	 * @return
	 */
	private int getMaxWarrantyLength(String providerId) {
		StringBuilder sql = new StringBuilder(158);
		sql.append(DBUtil.SELECT_CLAUSE).append("'max' as key, max(warranty_days_no) as value ").append(DBUtil.FROM_CLAUSE);
		sql.append(getCustomSchema()).append("wsla_warranty ").append(DBUtil.WHERE_CLAUSE).append("provider_id = ? ");
		sql.append(DBUtil.GROUP_BY).append("warranty_days_no ");
		
		List<Object> vals = new ArrayList<>();
		vals.add(providerId);				

		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.setGenerateExecutedSQL(log.isDebugEnabled());
		List<GenericVO> data = db.executeSelect(sql.toString(), vals, new GenericVO());
		
		if (data != null && ! data.isEmpty()) {
			return (int) data.get(0).getValue();
		}else {
			return -1;
		}
			
		
	}

	/**
	 * Gets a list of serial numbers for the given provider
	 * @param providerId
	 * @param valFlag
	 * @param bst
	 * @return
	 */
	public GridDataVO<ProductSerialNumberVO> getSerialsByOem(String providerId, int valFlag, BSTableControlVO bst ) {
		StringBuilder sql = new StringBuilder(384);
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema());
		sql.append("wsla_provider p ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("wsla_product_master pm on p.provider_id = pm.provider_id");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("wsla_product_serial a on pm.product_id = a.product_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema());
		sql.append("wsla_product_warranty b ");
		sql.append("on a.product_serial_id = b.product_serial_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema());
		sql.append("wsla_warranty c on ");
		sql.append("b.warranty_id = c.warranty_id ");
		sql.append("where p.provider_id = ? and a.validated_flg = ? ");
		sql.append(bst.getSQLOrderBy("product_nm", "asc"));
		
		List<Object> vals = new ArrayList<>();
		vals.add(providerId);
		vals.add(valFlag);
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		return db.executeSQLWithCount(sql.toString(), vals, new ProductSerialNumberVO(), bst);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		ProductSerialNumberVO vo = new ProductSerialNumberVO(req);
		int val = req.getIntegerParameter("validatedFlag", 0);
		UserVO user = (UserVO) getAdminUser(req).getUserExtendedInfo();
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			if (req.hasParameter("isDelete")) {
				db.delete(vo);
			} else if (req.getBooleanParameter("bulkSerial")) {
				String psId = req.getParameter("productSerialId");
				String wId = req.getParameter("warrantyId");
				val = req.getIntegerParameter("validationFlag", 0);
				updateValidationFlag(psId, val, wId, user.getUserId());
			} else {
				ProductWarrantyVO pwvo = new ProductWarrantyVO(req);
				save(vo, pwvo, val, user.getUserId());
			}
		} catch (InvalidDataException | DatabaseException | SQLException e) {
			log.error("Unable to save product serial", e);
			putModuleData("", 0, false, e.getLocalizedMessage(), true);
		}
	}
	
	/**
	 * Saves the serial info and warranty if passed.  Checks for open tickets waiting on 
	 * approval of the serial number
	 * @param vo
	 * @param pwvo
	 * @param val
	 * @param userId
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public void save(ProductSerialNumberVO vo, ProductWarrantyVO pwvo, int val, String userId) 
	throws InvalidDataException, DatabaseException {
		
		// Save the serial info
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.save(vo);
		
		pwvo.setProductSerialId(vo.getProductSerialId());
		
		// If the warranty info exists, update it
		if (! StringUtil.isEmpty(pwvo.getProductWarrantyId()) || ! StringUtil.isEmpty(vo.getWarrantyId())) {
			db.save(pwvo);
		}
		
		// if updating a serial number and its validated, look for pending tickets
		TicketVO ticket = getTicketForSerial(pwvo.getProductSerialId());
		if (! StringUtil.isEmpty(ticket.getTicketId()) && vo.getValidatedFlag() ==  1) {
			updateTicketAndStatus(vo.getProductSerialId(), val, userId, pwvo);
		}
	}
	
	/**
	 * 
	 * @param psId
	 * @param valFlag
	 * @throws SQLException
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 */
	public void updateValidationFlag(String psId, int valFlag, String warrantyId, String userId) 
	throws SQLException, InvalidDataException, DatabaseException {
		
		// Update the serial value
		StringBuilder sql = new StringBuilder(64);
		sql.append(DBUtil.UPDATE_CLAUSE).append(getCustomSchema());
		sql.append("wsla_product_serial set validated_flg = ? ");
		sql.append("where product_serial_id = ? ");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setInt(1, valFlag);
			ps.setString(2, psId);
			ps.executeUpdate();
		}
		
		// Add the warranty assoc if the serial is valid
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		ProductWarrantyAction pwa = new ProductWarrantyAction(attributes, getDBConnection());
		ProductWarrantyVO vo = new ProductWarrantyVO();
		if (valFlag == 1 && ! pwa.hasProductWarranty(psId, warrantyId)) {
			vo.setWarrantyId(warrantyId);
			vo.setProductSerialId(psId);
			db.save(vo);
		}
		
		updateTicketAndStatus(psId, valFlag, userId, vo);
	}
	
	/**
	 * 
	 * @param psId
	 * @param valFlag
	 * @param userId
	 * @param vo - the ProductWarrantyVO
	 * @throws DatabaseException
	 * @throws InvalidDataException 
	 */
	public void updateTicketAndStatus(String psId, int valFlag, String userId, ProductWarrantyVO pwvo) throws DatabaseException, InvalidDataException {
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		BaseTransactionAction bta = new BaseTransactionAction(getDBConnection(), getAttributes());
		TicketVO ticket = getTicketForSerial(psId);
		if (StringUtil.isEmpty(ticket.getTicketId())) return;
		
		// Update the warranty on the ticket if this is a valid serial
		if (valFlag == 1 && !StringUtil.isEmpty(pwvo.getProductWarrantyId())) {
			ticket.setProductWarrantyId(pwvo.getProductWarrantyId());
			ticket.setUpdateDate(new Date());
			db.save(ticket);
		}

		// Update status & add ledger entry.
		String summary = valFlag == 1 ? LedgerSummary.SERIAL_APPROVED.summary : null;
		TicketLedgerVO ledger = bta.changeStatus(ticket.getTicketId(), userId, valFlag == 1 ? StatusCode.USER_CALL_DATA_INCOMPLETE : StatusCode.DECLINED_SERIAL_NO, summary, null);
		
		// When serial is declined, close the ticket
		if (ledger.getStatusCode() == StatusCode.DECLINED_SERIAL_NO) {
			bta.changeStatus(ticket.getTicketId(), userId, StatusCode.CLOSED, LedgerSummary.TICKET_CLOSED.summary, null);
		}
	}
	
	
	/**
	 * Finds the ticket associated to this serial validation
	 * 
	 * @param psId
	 * @return
	 */
	private TicketVO getTicketForSerial(String psId) {
		StringBuilder sql = new StringBuilder(135);
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema()).append("wsla_ticket ");
		sql.append("where product_serial_id = ? and status_cd = ? ");
		log.debug(sql);
		
		List<Object> params = new ArrayList<>();
		params.add(psId);
		params.add(StatusCode.UNLISTED_SERIAL_NO.name());
		
		DBProcessor dbp = new DBProcessor(dbConn);
		List<TicketVO> ticketList = dbp.executeSelect(sql.toString(), params, new TicketVO());
		
		if (ticketList.isEmpty()) return new TicketVO();
		else return ticketList.get(0);
	}


	/**
	 * Return a list of products included in the requested set.
	 * @param setId
	 * @param bst
	 * @return
	 */
	public GridDataVO<ProductSerialNumberVO> getSet(String productId, BSTableControlVO bst) {
		if (StringUtil.isEmpty(productId)) return null;

		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select s.*, pw.warranty_id, product_warranty_id, w.desc_txt as warranty_nm from ").append(schema).append("wsla_product_serial s ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_product_master p on s.product_id=p.product_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_product_warranty pw on s.product_serial_id=pw.product_serial_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_warranty w on pw.warranty_id=w.warranty_id ");
		sql.append("where 1=1 ");

		// Filter by search criteria
		if (bst.hasSearch()) {
			sql.append("and (lower(p.product_nm) like ? or lower(p.cust_product_id) like ? or lower(p.sec_cust_product_id) like ? or lower(s.serial_no_txt) like ?) ");
			params.add(bst.getLikeSearch().toLowerCase());
			params.add(bst.getLikeSearch().toLowerCase());
			params.add(bst.getLikeSearch().toLowerCase());
			params.add(bst.getLikeSearch().toLowerCase());
		}

		// always filter by productId
		sql.append("and s.product_id=? ");
		params.add(productId);

		sql.append(bst.getSQLOrderBy("s.serial_no_txt",  "asc"));
		log.debug(sql);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSQLWithCount(sql.toString(), params, new ProductSerialNumberVO(), bst.getLimit(), bst.getOffset());
	}


	/**
	 * Lookup a user-provided serial# to validate it.  Called from the UI (ajax).
	 * @param productId
	 * @param serialNo
	 * @return a list of VOs (rows in the table)
	 */
	public ProductWarrantyVO getProductSerial(String productId, String serialNo) {
		if (StringUtil.isEmpty(productId) || StringUtil.isEmpty(serialNo) || WSLAConstants.NO_SERIAL_NUMBER.equalsIgnoreCase(serialNo))
			return new ProductWarrantyVO();

		StringBuilder sql = new StringBuilder(256);
		sql.append("select a.*, c.*, b.product_warranty_id, b.expiration_dt from ").append(getCustomSchema());
		sql.append("wsla_product_serial a ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema());
		sql.append("wsla_product_warranty b ");
		sql.append("on a.product_serial_id = b.product_serial_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema());
		sql.append("wsla_warranty c on ");
		sql.append("b.warranty_id = c.warranty_id ");
		sql.append("where lower(serial_no_txt) = ? and product_id = ? ");		

		List<Object> vals = new ArrayList<>();
		vals.add(serialNo.toLowerCase());
		vals.add(productId);
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.setGenerateExecutedSQL(log.isDebugEnabled());
		List<ProductWarrantyVO> lpwvo = db.executeSelect(sql.toString(), vals, new ProductWarrantyVO());
		
		if (lpwvo != null && !lpwvo.isEmpty() && lpwvo.get(0) != null)
		log.debug("disposed flag " +  lpwvo.get(0).getDisposeFlag());
		
		if (lpwvo.isEmpty()) return new ProductWarrantyVO();
		else return lpwvo.get(0);
	}
	
	/**
	 * Searches for a work order for the same serial number and product.
	 * @param productId
	 * @param serialNumber
	 * @return
	 */
	public TicketVO lookupServiceOrder(String productId, String serialNumber) {
		StringBuilder sql = new StringBuilder(384);
		List<Object> vals = new ArrayList<>();
		vals.add(serialNumber);
		vals.add(productId);
		vals.add(serialNumber);
		vals.add(productId);
		
		sql.append("select b.* ");
		sql.append("from ").append(getCustomSchema()).append("wsla_product_serial a ");
		sql.append("inner join ").append(getCustomSchema()).append("wsla_ticket b ");
		sql.append("on a.product_serial_id = b.product_serial_id ");
		sql.append("where serial_no_txt = ? and product_id = ? ");
		sql.append("and b.create_dt = ( ");
		sql.append("select max(b.create_dt) ");
		sql.append("from ").append(getCustomSchema()).append(" wsla_product_serial a ");
		sql.append("inner join ").append(getCustomSchema()).append("wsla_ticket b ");
		sql.append("on a.product_serial_id = b.product_serial_id ");
		sql.append("where  b.status_cd != 'CLOSED' and serial_no_txt = ? and product_id = ?) ");
		log.debug(sql + "|" + vals);
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		List<TicketVO> data = db.executeSelect(sql.toString(), vals, new TicketVO());
		if (data.isEmpty()) return new TicketVO();
		else return data.get(0);
	}


	/* (non-Javadoc)
	 * @see com.wsla.action.admin.BatchImport#getBatchImportableClass()
	 */
	@Override
	protected Class<?> getBatchImportableClass() {
		return ProductSerialNumberVO.class;
	}


	/**
	 * Remove any entries that are already in the system (compare by serial#)
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.BatchImport#validateBatchImport(com.siliconmtn.action.ActionRequest, java.util.ArrayList)
	 */
	@Override
	protected void validateBatchImport(ActionRequest req,
			ArrayList<? extends Object> entries) throws ActionException {
		String sql = StringUtil.join("select lower(serial_no_txt) as key from ", getCustomSchema(), 
				"wsla_product_serial where product_id=?");

		// load this product's SKUs from the DB and store them as a Set for quick reference.
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		List<GenericVO> serialNos = db.executeSelect(sql, Arrays.asList(req.getParameter(REQ_PRODUCT_ID)), new GenericVO());
		Set<String> skus = new HashSet<>(serialNos.size());
		for (GenericVO vo : serialNos)
			skus.add(StringUtil.checkVal(vo.getKey()));

		//remove items from the batch import which are already in the database
		Iterator<? extends Object> iter = entries.iterator();
		while (iter.hasNext()) {
			ProductSerialNumberVO vo = (ProductSerialNumberVO) iter.next();
			if (StringUtil.isEmpty(vo.getSerialNumber()) || skus.contains(vo.getSerialNumber().toLowerCase())) {
				iter.remove();
				log.debug("omitting pre-existing or empty SKU: " + vo.getSerialNumber());
				//TODO these records need at least a DB update to ensure they'are marked valid.
			}
		}
	}


	/**
	 * give some additional default values to the records about to be inserted
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.BatchImport#transposeBatchImport(com.siliconmtn.action.ActionRequest, java.util.ArrayList)
	 */
	@Override
	protected void transposeBatchImport(ActionRequest req, ArrayList<? extends Object> entries)
			throws ActionException {
		//set the productId for all beans to the one passed on the request
		String productId = req.getParameter(REQ_PRODUCT_ID);
		Date dt = Calendar.getInstance().getTime();
		for (Object obj : entries) {
			ProductSerialNumberVO vo = (ProductSerialNumberVO) obj;
			vo.setProductId(productId);
			vo.setValidatedFlag(1); //vendor-provided file (not customer via ticket request), mark these all as validated
			vo.setRetailerDate(dt); //default to today for retailer issued date.
		}
	}


	/**
	 * Insert the new serial# records, then create warranty records for each
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.BatchImport#saveBatchImport(com.siliconmtn.action.ActionRequest, java.util.ArrayList)
	 */
	@Override
	protected void saveBatchImport(ActionRequest req, ArrayList<? extends Object> entries)
			throws ActionException {
		//save the product_serial table
		super.saveBatchImport(req, entries);

		//save the product_warranty table
		addProductWarranties(req, entries);
	}


	/**
	 * Creates a product_warranty record for each of the serials#s saved
	 * @param req
	 * @param entries
	 * @throws ActionException 
	 */
	private void addProductWarranties(ActionRequest req, ArrayList<? extends Object> entries)
			throws ActionException {
		String warrantyId = req.getParameter("warrantyId");
		if (StringUtil.isEmpty(warrantyId)) return;

		//get the warranty, then calculate expiration date for the product based on the warrantyDays, from today.
		Calendar today = Calendar.getInstance();
		WarrantyVO warranty = new WarrantyAction(getAttributes(), getDBConnection()).getWarranty(warrantyId);
		today.add(Calendar.DATE, warranty.getWarrantyLength()); //e.g. add 90 days to today.
		Date expDate = today.getTime();

		ArrayList<ProductWarrantyVO> data = new ArrayList<>(entries.size());
		for (Object obj : entries) {
			ProductSerialNumberVO vo = (ProductSerialNumberVO) obj;
			data.add(new ProductWarrantyVO(vo.getProductSerialId(), warrantyId, expDate));
		}
		//push these through the same batch-insert logic
		super.saveBatchImport(req, data);
	}
}