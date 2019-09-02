package com.wsla.action.admin;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.Arrays;
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
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.data.product.WarrantyVO;

/****************************************************************************
 * <b>Title</b>: WarrantyAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the administration of the warranty table
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James McKain
 * @version 1.0
 * @since Oct 4, 2018
 * @updates:
 ****************************************************************************/
public class WarrantyAction extends SBActionAdapter {

	public static final String REQ_WARRANTY_ID = "warrantyId";


	//holds codes identifying if the warranty will pick up or drop off finished products
	public enum ServiceTypeCode {
		DROP_OFF("Drop Off"),
		PICK_UP("Pick Up"),
		ALL("Drop Off or Pick Up");
		
		private String value;
		ServiceTypeCode(String value) {
			this.value = value;
		}
		
		public String getValue() {
			return value;
		}
	}
	
	public WarrantyAction() {
		super();
	}

	public WarrantyAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * Overloaded constructor used for calling between actions.
	 * @param attrs
	 * @param conn
	 */
	public WarrantyAction(Map<String, Object> attrs, SMTDBConnection conn) {
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
		if (req.hasParameter("getWarrantyById")) {
			String warrantyId = req.getParameter(REQ_WARRANTY_ID);
			setModuleData(getWarranty(warrantyId));
			return;
		}
		
		String providerId = req.getParameter("providerId");
		String typeId = req.getParameter("typeId");
		Integer activeFlag = req.getIntegerParameter("activeFlag");
		Integer flatRateFlag = req.getIntegerParameter("flatRateFlag");
		setModuleData(getData(providerId, typeId, new BSTableControlVO(req, WarrantyVO.class),activeFlag, flatRateFlag));
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		WarrantyVO vo = new WarrantyVO(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			if (req.hasParameter("isDelete")) {
				db.delete(vo);
			} else {
				db.save(vo);
			}
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Unable to save warranty", e);
		}
	}


	/**
	 * Pull a single warranty from the DB.  Used when creating product_warranty records
	 * @param warrantyId
	 * @return
	 */
	public WarrantyVO getWarranty(String warrantyId) {
		String schema = getCustomSchema();
		String sql = StringUtil.join(DBUtil.SELECT_FROM_STAR, schema, "wsla_warranty where warranty_id=?");
		DBProcessor dbp = new DBProcessor(getDBConnection(), schema);
		List<WarrantyVO> data = dbp.executeSelect(sql, Arrays.asList(warrantyId), new WarrantyVO());
		return data != null && !data.isEmpty() ? data.get(0) : new WarrantyVO();
	}

	/**
	 * gets the warranty when sent a ticket id
	 * @param TicketId
	 * @return
	 */
	public WarrantyVO getWarrantyByTicketId(String ticketId) {
		
		StringBuilder sql = new StringBuilder(255);
		sql.append(DBUtil.SELECT_CLAUSE).append(" w.* ").append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("wsla_ticket t ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_product_warranty pw on t.product_warranty_id = pw.product_warranty_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_warranty w on pw.warranty_id = w.warranty_id ");
		sql.append(DBUtil.WHERE_CLAUSE).append("t.ticket_id = ? ");

		DBProcessor dbp = new DBProcessor(getDBConnection(), getCustomSchema());
		List<WarrantyVO> data = dbp.executeSelect(sql.toString(), Arrays.asList(ticketId), new WarrantyVO());
		return data != null && !data.isEmpty() ? data.get(0) : new WarrantyVO();
	}

	/**
	 * Return a list of Warranties included in the requested set.
	 * @param setId
	 * @param bst
	 * @param hasFlatRateFlag 
	 * @param flatRateFlag 
	 * @param hasActiveFlag 
	 * @param activeFlag 
	 * @return
	 */
	public GridDataVO<WarrantyVO> getData(String providerId, String typeId, BSTableControlVO bst, Integer activeFlag, Integer flatRateFlag) {
		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();
		StringBuilder sql = new StringBuilder(304);
		sql.append("select w.*, p.provider_nm, rp.provider_nm as refund_provider_nm from ");
		sql.append(schema).append("wsla_warranty w ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_provider p on w.provider_id=p.provider_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_provider rp on w.refund_provider_id=rp.provider_id ");
		sql.append("where 1=1 ");

		// Filter by search criteria
		if (bst.hasSearch()) {
			sql.append("and (lower(w.desc_txt) like ? or lower(p.provider_nm) like ? or lower(w.warranty_type_cd) like ?) ");
			params.add(bst.getLikeSearch().toLowerCase());
			params.add(bst.getLikeSearch().toLowerCase());
			params.add(bst.getLikeSearch().toLowerCase());
		}

		// filter by providerId
		if (!StringUtil.isEmpty(providerId)) {
			sql.append("and p.provider_id=? ");
			params.add(providerId);
		}

		// filter by type
		if (!StringUtil.isEmpty(typeId)) {
			sql.append("and w.warranty_type_cd=? ");
			params.add(typeId);
		}
		
		if(activeFlag != null) {
			sql.append("and w.active_flg =? ");
			params.add(activeFlag);
		}
		
		if(flatRateFlag != null) {
			sql.append("and w.flat_rate_flg =? ");
			params.add(flatRateFlag);
		}

		sql.append(bst.getSQLOrderBy("p.provider_nm",  "asc"));
		log.debug(sql.length() + "|" + sql);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSQLWithCount(sql.toString(), params, new WarrantyVO(), bst.getLimit(), bst.getOffset());
	}


	/**
	 * Return a id/name pairing of warranties - used for selectpickers via the SelectLookupAction
	 * @return
	 */
	public List<GenericVO> listWarranties(String providerId) {
		List<Object> params = null;
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(150);
		sql.append("select warranty_id as key, desc_txt as value ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("wsla_warranty ");
		sql.append(DBUtil.WHERE_1_CLAUSE).append("and active_flg  = 1 ");
		
		if (!StringUtil.isEmpty(providerId)) {
			sql.append(" and provider_id=? ");
			params = Arrays.asList(providerId);
		}
		
		sql.append("order by desc_txt");
		log.debug(sql + "|" + params);

		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema()); 
		return db.executeSelect(sql.toString(), params, new GenericVO());
	}
}