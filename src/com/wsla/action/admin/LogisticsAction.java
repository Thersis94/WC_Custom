package com.wsla.action.admin;

// JDK 1.8.x
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

// iText PDF imports
import com.lowagie.text.DocumentException;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.data.report.PDFGenerator;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.EnumUtil;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.report.vo.DownloadReportVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.resource.WCResourceBundle;
import com.smt.sitebuilder.security.SBUserRole;
import com.wsla.action.ticket.BaseTransactionAction;
import com.wsla.action.ticket.TicketEditAction;
import com.wsla.action.ticket.transaction.TicketPartsTransaction;
import com.wsla.common.UserSqlFilter;
import com.wsla.common.WSLAConstants;
import com.wsla.data.provider.ProviderLocationVO;
import com.wsla.data.ticket.LedgerSummary;
import com.wsla.data.ticket.PartVO;
import com.wsla.data.ticket.ShipmentVO;
import com.wsla.data.ticket.ShipmentVO.ShipmentStatus;
import com.wsla.data.ticket.ShipmentVO.ShipmentType;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.TicketVO;
import com.wsla.data.ticket.TicketAssignmentVO.TypeCode;
import com.wsla.data.ticket.UserVO;

// Freemarker Imports
import freemarker.template.TemplateException;

/****************************************************************************
 * <b>Title</b>: LogisticsAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Oversees Shipment creation (by WSLA or OEM) and ingest (by CAS recipient).
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James McKain
 * @version 1.0
 * @since Nov 6, 2018
 * @updates:
 ****************************************************************************/
public class LogisticsAction extends SBActionAdapter {

	public static final String REQ_TICKET_ID = "ticketId";

	public LogisticsAction() {
		super();
	}

	public LogisticsAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * Overloaded constructor used for calling between actions.
	 * @param attrs
	 * @param conn
	 */
	public LogisticsAction(Map<String, Object> attrs, SMTDBConnection conn) {
		this();
		setAttributes(attrs);
		setDBConnection(conn);
	}


	/*
	 * The users viewing this page are Warehouse, OEM, or CAS (Role).
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String toLocnId = req.getParameter("toLocationId");
		ShipmentStatus sts = EnumUtil.safeValueOf(ShipmentStatus.class, req.getParameter("status"));
		
		//lookup the destination locationId for this shipment, based on ticketId
		if (req.hasParameter("isDestLookup")) {
			putModuleData(findDestLocnId(req.getParameter(REQ_TICKET_ID)));
		} else if (req.getBooleanParameter("parts")) {
			LogisticsPartsAction lpa = new LogisticsPartsAction(attributes, dbConn);
			setModuleData(lpa.listParts(req.getParameter("shipmentId"), new BSTableControlVO(req, PartVO.class)));

		} else if (req.getBooleanParameter("packingList")) {
			BSTableControlVO bst = new BSTableControlVO(req);
			SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
			ResourceBundle rb = WCResourceBundle.getBundle(site, getAdminUser(req));
			try {
				String sId = req.getParameter("shipmentId");
				byte[] pdf = buildPackingList(rb, sId, bst, req.getRealPath());
				getReportObj(pdf, req);
			} catch (Exception e) {
				log.error("unable to build packing list pdf", e);
			}

		} else {
			UserVO user = (UserVO) getAdminUser(req).getUserExtendedInfo();
			String roleId = ((SBUserRole)req.getSession().getAttribute(Constants.ROLE_DATA)).getRoleId();
			UserSqlFilter userFilter = new UserSqlFilter(user, roleId, getCustomSchema());

			setModuleData(getData(toLocnId, sts, userFilter, new BSTableControlVO(req, ShipmentVO.class)));
		}


	}

	/**
	 * Creates and streams a packing list
	 * @param user
	 * @param shipId
	 * @param bst
	 * @param rp
	 * @return
	 * @throws InvalidDataException
	 * @throws IOException
	 * @throws TemplateException
	 * @throws DocumentException
	 * @throws SQLException 
	 * @throws DatabaseException 
	 */
	public byte[] buildPackingList(ResourceBundle rb, String shipId, BSTableControlVO bst, String rp) 
	throws Exception {
		// Get the parts
		bst.setSourceBean(PartVO.class);
		LogisticsPartsAction lpa = new LogisticsPartsAction(attributes, dbConn);

		// Get the shipment
		bst.setSourceBean(ShipmentVO.class);
		ShipmentVO shmpt = getShipmentDetail(shipId);
		shmpt.setParts(lpa.listParts(shipId, bst).getRowData());
		
		// Get the ticket
		TicketEditAction tea = new TicketEditAction(getDBConnection(), getAttributes());
		TicketVO ticket = tea.getCompleteTicket(shmpt.getTicketId());
		
		String templateDir = rp + attributes.get(Constants.INCLUDE_DIRECTORY) + "templates/";
		String path = templateDir + "packing_list.ftl";
		
		// Generate the pdf
		PDFGenerator pdf = new PDFGenerator(path, shmpt, rb);
		pdf.addDataObject("ticket", ticket);
		
		return pdf.generate();
	}
	
	/**
	 * Gets the shipment detail
	 * @param shipmentId
	 * @return
	 * @throws InvalidDataException
	 * @throws com.siliconmtn.db.util.DatabaseException
	 */
	public ShipmentVO getShipmentDetail(String shipmentId) 
	throws InvalidDataException, com.siliconmtn.db.util.DatabaseException {
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		
		// Get the shipment
		ShipmentVO shipment = new ShipmentVO();
		shipment.setShipmentId(shipmentId);
		db.getByPrimaryKey(shipment);
		
		// Get the source info
		ProviderLocationVO fromLoc = new ProviderLocationVO();
		fromLoc.setLocationId(shipment.getFromLocationId());
		db.getByPrimaryKey(fromLoc);
		
		// Get the destination info
		ProviderLocationVO toLoc = new ProviderLocationVO();
		toLoc.setLocationId(shipment.getToLocationId());
		db.getByPrimaryKey(toLoc);
		
		// Add the source and dest to the shipment
		shipment.setFromLocation(fromLoc);
		shipment.setToLocation(toLoc);
		
		// Get user who performed shipping
		UserVO shippedBy = new UserVO();
		shippedBy.setUserId(shipment.getShippedById());
		db.getByPrimaryKey(shippedBy);
		shipment.setShippedByUser(shippedBy);
		
		return shipment;
	}
	/**
	 * Builds the Packing List Object to be streamed
	 * @param ticket
	 * @param pdf
	 * @param req
	 * @return
	 */
	public void getReportObj(byte[] pdf, ActionRequest req) {
		
		AbstractSBReportVO report = new DownloadReportVO();
		report.setFileName("packing_list.pdf");
		report.setData(pdf);
		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
		req.setAttribute(Constants.BINARY_DOCUMENT, report);
	}

	/**
	 * Load the locationId of the CAS for the given ticket
	 * @param parameter
	 * @return
	 */
	private GenericVO findDestLocnId(String ticketId) {
		String schema = getCustomSchema();
		String sql = StringUtil.join("select location_id as key from ", schema, 
				"wsla_ticket_assignment where ticket_id=? and assg_type_cd=?");
		log.debug(sql);
		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		List<GenericVO> data = db.executeSelect(sql, Arrays.asList(ticketId, TypeCode.CAS), new GenericVO());
		return !data.isEmpty() ? data.get(0) : new GenericVO();
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		ShipmentVO vo = new ShipmentVO(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			if (req.hasParameter("isDelete")) {
				db.delete(vo);

			} else {
				boolean isInsert = StringUtil.isEmpty(vo.getShipmentId());
				//make sure shipmentDt gets set if status is shipped
				if (ShipmentStatus.SHIPPED.equals(vo.getStatus()) && vo.getShipmentDate() == null)
					vo.setShipmentDate(Calendar.getInstance().getTime());

				db.save(vo);

				//if this is a new shipment getting created, automatically put all the parts from the ticket into it
				//the admin can remove or add on the next screen, but this is a significant convenience for them.
				if (isInsert && req.hasParameter(REQ_TICKET_ID)) {
					addTicketPartsToShipment(vo.getShipmentId(), req.getParameter(REQ_TICKET_ID));
					vo.setShipmentType(ShipmentType.PARTS_REQUEST);
				} else if (isInsert) {
					vo.setShipmentType(ShipmentType.INVENTORY);
				}
				
				// Change the service order status when shipping the service order parts or unit
				if (req.hasParameter(REQ_TICKET_ID) && ShipmentStatus.SHIPPED.equals(vo.getStatus())) {
					UserVO user = (UserVO) getAdminUser(req).getUserExtendedInfo();
					BaseTransactionAction bta = new BaseTransactionAction(getDBConnection(), getAttributes());
					StatusCode status = getShippedStatusChange(vo.getShipmentType());
					bta.changeStatus(req.getParameter(REQ_TICKET_ID), user.getUserId(), status, LedgerSummary.SHIPMENT_CREATED.summary, null);
				}
			}

		} catch (Exception e) {
			log.error("could not save shipment", e);
		}
	}

	/**
	 * Returns an appropriate status change for when a shipment is shipped.
	 * 
	 * @param type
	 * @return
	 */
	public StatusCode getShippedStatusChange(ShipmentType type) {
		switch (type) {
			case UNIT_MOVEMENT:
				return StatusCode.DEFECTIVE_SHIPPED;
			case REPLACEMENT_UNIT:
				return StatusCode.RPLC_DELIVERY_SCHED;
			case PARTS_REQUEST:
			default:
				return StatusCode.PARTS_SHIPPED_CAS;
		}
	}

	/**
	 * Add the ticket's parts to the shipment if they're not already allocated elsewhere
	 * @param partIds
	 * @param schema
	 */
	public void addTicketPartsToShipment(String shipmentId, String ticketId) {
		StringBuilder sql = new StringBuilder(200);
		sql.append(DBUtil.UPDATE_CLAUSE).append(getCustomSchema()).append("wsla_part ");
		sql.append("set shipment_id=?, update_dt=? where ticket_id=? and shipment_id is null");
		log.debug(sql + "|" + shipmentId +"|"+Convert.getCurrentTimestamp() +"|"+  ticketId);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, shipmentId);
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			ps.setString(3, ticketId);
			int cnt = ps.executeUpdate();
			log.debug(String.format("added %d parts to shipment %s", cnt, shipmentId));

		} catch(SQLException sqle) {
			log.error("could not add parts to shipment", sqle);
		}
	}


	/**
	 * Return a list of products tied to tickets that are status=HarvestPendingApproval.
	 * In this view we only care about the product, so the OEM can approve or reject the request.
	 * @param locationId location who's products to load
	 * @param bst vo to populate data into
	 * @return
	 */
	public GridDataVO<ShipmentVO> getData(String toLocationId, ShipmentStatus status, UserSqlFilter userFilter, BSTableControlVO bst) {
		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select s.*, t.ticket_no, srclcn.*, destlcn.*, ");
		sql.append("srclcn.location_nm as from_location_nm, destlcn.location_nm as to_location_nm ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("wsla_shipment s ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_provider_location srclcn on s.from_location_id=srclcn.location_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_provider_location destlcn on s.to_location_id=destlcn.location_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_ticket t on s.ticket_id=t.ticket_id ");
		sql.append(userFilter.getTicketFilter("t", params));
		sql.append("where (s.status_cd != ? or (s.status_cd=? and coalesce(s.shipment_dt, s.update_dt, s.create_dt) > CURRENT_DATE-31)) "); //only show ingested items for 30 days past receipt
		params.add(ShipmentStatus.RECEIVED.toString());
		params.add(ShipmentStatus.RECEIVED.toString());

		//fuzzy keyword search
		String term = bst.getLikeSearch().toLowerCase();
		if (!StringUtil.isEmpty(term)) {
			sql.append("and (lower(t.ticket_no) like ? or lower(s.status_cd) like ? ");
			sql.append("or lower(t.ticket_no) like ? or lower(s.carrier_tracking_no) like ? ");
			sql.append("or lower(destlcn.location_nm) like ? or lower(srclcn.location_nm) like ?) ");
			params.add(term);
			params.add(term);
			params.add(term);
			params.add(term);
			params.add(term);
			params.add(term);
		}

		if (!StringUtil.isEmpty(toLocationId)) {
			sql.append("and s.to_location_id=? ");
			params.add(toLocationId);
		}

		if (status != null) {
			sql.append("and s.status_cd=? ");
			params.add(status);
		}
		
		sql.append(bst.getSQLOrderBy("s.create_dt", "desc"));
		log.debug(sql);

		//after query, adjust the count to be # of unique shipments, not total SQL rows
		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSQLWithCount(sql.toString(), params, new ShipmentVO(), "shipment_id", bst);
	}
	
	/**
	 * Looks up shipments tied to a ticket. Optionally, filtering by one or more statuses.
	 * 
	 * @param ticketId
	 * @param statuses
	 * @return
	 */
	public List<ShipmentVO> getTicketShipments(String ticketId, List<ShipmentStatus> statuses) {
		StringBuilder sql = new StringBuilder(200);
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema()).append("wsla_shipment ");
		sql.append("where ticket_id = ? and status_cd in (").append(DBUtil.preparedStatmentQuestion(statuses.size())).append(") ");
		log.debug(sql);
		
		List<Object> params = new ArrayList<>();
		params.add(ticketId);
		for (ShipmentStatus status : statuses) {
			params.add(status.name());
		}
		
		DBProcessor dbp = new DBProcessor(getDBConnection(), getCustomSchema());
		return dbp.executeSelect(sql.toString(), params, new ShipmentVO());
	}
	
	/**
	 * used when setting up the return of a finished product.  will build a partVO and then save it
	 * @param ticketId
	 * @return
	 * @throws InvalidDataException 
	 * @throws com.siliconmtn.db.util.DatabaseException 
	 */
	public void saveProductAsPart(String ticketId, String shipmentId) throws Exception{
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		List<Object> params = new ArrayList<>();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select ps.product_id, t.ticket_id, 1 as quantity_no, 0 as harvested_flg, 1 as submit_approval_flg, ps.serial_no_txt from ").append(getCustomSchema()).append("wsla_ticket t ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_product_serial ps on t.product_serial_id = ps.product_serial_id  ");
		sql.append(DBUtil.WHERE_CLAUSE).append("ticket_id = ? ");
		PartVO pvo = new PartVO();
		params.add(ticketId);
		List<PartVO> data = db.executeSelect(sql.toString(), params, pvo);
		
		if (data != null && !data.isEmpty()) {
			pvo = data.get(0);
		}else {
			throw new InvalidDataException("no finish product found ");
		}
		log.debug("sql " + sql.toString());
		
		pvo.setShipmentId(shipmentId);
		db.save(pvo);
	}
	
	/**
	 * Cancels pending/open shipments for a specified ticket
	 * 
	 * @param ticketId
	 * @throws com.siliconmtn.db.util.DatabaseException 
	 * @throws InvalidDataException 
	 */
	public void cancelPendingShipments(String ticketId) throws InvalidDataException, com.siliconmtn.db.util.DatabaseException {
		DBProcessor dbp = new DBProcessor(getDBConnection(), getCustomSchema());
		
		List<ShipmentVO> shipments = getTicketShipments(ticketId, Arrays.asList(ShipmentStatus.CREATED, ShipmentStatus.BACKORDERED));
		for (ShipmentVO shipment : shipments) {
			shipment.setStatus(ShipmentStatus.CANCELED);
			shipment.setUpdateDate(new Date());
			dbp.save(shipment);
		}
	}
	
	/**
	 * Saves a shipment for movement of a unit. This will typically be a
	 * replacement unit for the end user.
	 * 
	 * @param ticketId
	 * @param product
	 * @param isPending
	 * @throws SQLException
	 */
	public void saveUnitShipment(String ticketId, PartVO product, boolean isPending, boolean isReplacement) throws SQLException {
		TicketPartsTransaction tpt = new TicketPartsTransaction(getDBConnection(), getAttributes());
		
		// Create the shipment
		ShipmentVO shipment = new ShipmentVO();
		shipment.setFromLocationId(WSLAConstants.DEFAULT_SHIPPING_SRC);
		shipment.setToLocationId(tpt.getCasLocationId(ticketId));
		shipment.setTicketId(ticketId);
		shipment.setStatus(isPending ? ShipmentStatus.PENDING : ShipmentStatus.CREATED);
		shipment.setShipmentType(isReplacement ? ShipmentType.REPLACEMENT_UNIT : ShipmentType.UNIT_MOVEMENT);
		
		// Save the shipment
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			db.save(shipment);
			product.setShipmentId(shipment.getShipmentId());
			db.save(product);
		} catch (InvalidDataException | com.siliconmtn.db.util.DatabaseException e) {
			throw new SQLException(e);
		}
	}
}