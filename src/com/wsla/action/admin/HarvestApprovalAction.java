package com.wsla.action.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;
import com.wsla.action.ticket.TicketEditAction;
import com.wsla.data.ticket.HarvestApprovalVO;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.TicketVO;
import com.wsla.data.ticket.UserVO;

/****************************************************************************
 * <b>Title</b>: InventoryAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Obtains OEM approval to harvest parts based on available tickets/status.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James McKain
 * @version 1.0
 * @since Oct 26, 2018
 * @updates:
 ****************************************************************************/
public class HarvestApprovalAction extends SBActionAdapter {

	public HarvestApprovalAction() {
		super();
	}

	public HarvestApprovalAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * Overloaded constructor used for calling between actions.
	 * @param attrs
	 * @param conn
	 */
	public HarvestApprovalAction(Map<String, Object> attrs, SMTDBConnection conn) {
		this();
		setAttributes(attrs);
		setDBConnection(conn);
	}


	/*
	 * The users viewing this page are OEMs (based on WC Role).  Query for a 
	 * list of ProductSerialVOs tied to tickets at a specific status (traced back to products owned by this OEM)
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		UserDataVO userData = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		UserVO user = userData != null ? (UserVO) userData.getUserExtendedInfo() : null;

		setModuleData(getData(user, new BSTableControlVO(req, HarvestApprovalVO.class)));
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 * TODO Finish this method once underlying components are built - see remarks inline
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		HarvestApprovalVO vo = new HarvestApprovalVO(req);

		if (StatusCode.HARVEST_REPAIR.equals(vo.getTicket().getStatusCode())) {
			//if approved for repair, close the ticket, clone it, and update the new one as a task for the CAS warehouse.
			//make sure there's a historical reference between the newly created ticket and the original.
		} else {
			//update ticket status

			//if approved for harvesting, call the parts action so it can create a BOM of parts the Tech should collect & record.
			if (StatusCode.HARVEST_APPROVED.equals(vo.getTicket().getStatusCode())) {
				createBOM(req.getParameter("productSerialId"), null);
			}
		}
	}
	
	/**
	 * Calls the parts action so it can create a BOM of parts the Tech
	 * should collect & record.
	 * 
	 * @param productSerialId - set to null if unknown
	 * @param ticketId - use ticketId when you don't know the productSerialId
	 */
	public void createBOM(String productSerialId, String ticketId) {
		if (productSerialId == null) {
			TicketEditAction tea = new TicketEditAction(getDBConnection(), getAttributes());
			TicketVO ticket = tea.getBaseTicket(ticketId);
			productSerialId = ticket.getProductSerialId();
		}
		
		HarvestPartsAction hpa = new HarvestPartsAction(getAttributes(), getDBConnection());
		hpa.prepareHarvest(productSerialId);
	}

	/**
	 * Return a list of products tied to tickets that are status=HarvestPendingApproval.
	 * In this view we only care about the product, so the OEM can approve or reject the request.
	 * @param locationId location who's products to load
	 * @param bst vo to populate data into
	 * @return
	 */
	public GridDataVO<HarvestApprovalVO> getData(UserVO user, BSTableControlVO bst) {
		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select p.*, ps.*, t.ticket_id, t.ticket_no, t.status_cd ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("wsla_ticket t ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_product_serial ps on t.product_serial_id=ps.product_serial_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_product_master p on ps.product_id=p.product_id ");
		sql.append("where 1=1 ");

		//fuzzy keyword search
		String term = bst.getLikeSearch().toLowerCase();
		if (!StringUtil.isEmpty(term)) {
			sql.append("and (lower(p.product_nm) like ? or lower(p.cust_product_id) like ? or lower(ps.serial_no_txt) like ?) ");
			params.add(term);
			params.add(term);
			params.add(term);
		}

		//only show my company's products (I'm an OEM user)
		sql.append("and p.provider_id=? ");
		params.add(user.getProviderId());

		//only show pending authorizations
		sql.append("and t.status_cd=? ");
		params.add(StatusCode.HARVEST_REQ.toString());

		sql.append(bst.getSQLOrderBy("p.product_nm, ps.serial_no_txt",  "asc"));
		log.debug(sql);
		log.debug(String.format("userLocationId=%s, userProviderId=%s", user.getLocationId(), user.getProviderId()));

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSQLWithCount(sql.toString(), params, new HarvestApprovalVO(), bst.getLimit(), bst.getOffset());
	}
}