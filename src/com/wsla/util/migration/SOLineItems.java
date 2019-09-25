package com.wsla.util.migration;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.EnumUtil;
import com.siliconmtn.util.MapUtil;
import com.siliconmtn.util.StringUtil;
import com.wsla.data.ticket.CreditMemoVO;
import com.wsla.data.ticket.PartVO;
import com.wsla.data.ticket.ShipmentVO;
import com.wsla.data.ticket.ShipmentVO.CarrierType;
import com.wsla.data.ticket.ShipmentVO.ShipmentStatus;
import com.wsla.data.ticket.ShipmentVO.ShipmentType;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.TicketCommentVO;
import com.wsla.data.ticket.TicketLedgerVO;
import com.wsla.util.migration.vo.SOLNIFileVO;

/****************************************************************************
 * <p><b>Title:</b> SOLineItems.java</p>
 * <p><b>Description:</b> </p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Feb 1, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class SOLineItems extends AbsImporter {

	private List<SOLNIFileVO> data = new ArrayList<>(50000);
	private Map<String, String> ticketIds = new HashMap<>(5000);
	private Map<String, String> activityMap = new HashMap<>(100);
	private Map<String, String> casLocations = new HashMap<>(4000);
	private static Map<String, String> productIds = new HashMap<>(10000);

	static {
		//from Steve
		productIds.put("100118", "2e70c47c8c959cd7ac100290caca15b1");
		productIds.put("100266","52d50221v1eaf571ac1002908d17145a");
		productIds.put("631013M4511","9465754396f33d507f00010183e7d920");
		productIds.put("845CX508T160616","5530d75396f33d497f0001017573c0d8");
		productIds.put("845CX508T17072","699887c7fdac4c47ac1002907ba7a760");
		productIds.put("890CVS3393PB851","124f08119707d13b7f0001018224773f");
		productIds.put("890JRX3393BHB5","9236dfdd96f33d477f0001017796f4b5");
		productIds.put("P752831V60A018L","ffec24beb081483aac1002907ac435e5");
		productIds.put("ST215B011","e7648100b08202f2ac10029083dd7859");
		productIds.put("TPMT55105PB818","841d7ca9b0837d1cac10029072104401");
		productIds.put("V400HJ6PE1","b9c1890ab08566a6ac1002901844e517");
		//wave 2
		productIds.put("100250", "c34d9ff61bda1432ac1002904d17469e");
		productIds.put("100267", "de77ef651bdb12f9ac10029011d56dbf");
		productIds.put("100268", "7be493d11bdc1678ac1002907dbc9200");
		productIds.put("100272", "c7326e0b1bdd222bac100290931f2967");
		productIds.put("10L538138022", "47d245e11bde8dc6ac100290de1ed3f8");
		productIds.put("306011DP7583100", "33d74b93e4985f95ac1002396caabbe7");
		productIds.put("30604008CXWMQ21", "b2a1673196f33d437f000101eaa0f310");
		productIds.put("306050C026YLDXK", "8f2c551d96f33d427f00010187983dee");
		productIds.put("306070KEY40170", "af8b66f296f33d4a7f000101be82f900");
		productIds.put("306080IR03228A", "2dbfeb5996f33d497f0001017e7d2ad2");
		productIds.put("55180Z00002", "54a64c111be2ff59ac100290d8992e59");
		productIds.put("631008M0352002", "2734ea039729e47c7f00010125cff6a5");
		productIds.put("70532Z2L5PCASY0", "a24535a596f33d437f00010143b5fbc3");
		productIds.put("845CX508T160159", "d1e337a896f33d467f000101be008007");
		productIds.put("890CVT3553PC825", "92fb07c796f33d527f0001011a1ee4f1");
		productIds.put("890CVWB58051GB4", "c1619bee1be82485ac100290f69fa197");
		productIds.put("890CVWB58055", "cc88ed4dfdb11a6bac100290db87eb60");
		productIds.put("890CVWBS80550", "cc88ed4dfdb11a6bac100290db87eb60"); //I think the S here was suppose to be a 5 (line above) - someone misread it
		productIds.put("890CVWBS80550H", "cc88ed4dfdb11a6bac100290db87eb60");
		productIds.put("890CVWB5805532", "9e205a9296f33d3f7f000101218dd0f5");
		productIds.put("890CVWB5805542", "5dc0343e1bebdac6ac100290ed0a763e");
		productIds.put("890CVWB580555", "5c2306d196f33d487f0001011a043e71");
		productIds.put("890JRA3393BHU32", "87a192299707d13c7f0001018760a680");
		productIds.put("890JRX3393BHB50", "9236dfdd96f33d477f0001017796f4b5");
		productIds.put("CVS3393PB8551", "5ca00b479729e47a7f000101b29d6af9");
		productIds.put("CX550DLEDM", "fa5959f01bf07808ac10029082f5d038");
		productIds.put("JR3393BH850-3H1", "7b278be51bf3bdffac1002907ffb6176");
		productIds.put("P752831V60", "a04850f596f33d447f00010113b2974a");
		productIds.put("P752831V60A021L", "e0cdc89d1bf5fb64ac100290bbdd6640");
		productIds.put("ST6308RTU-AP132", "557fc3811bf6f712ac100290ec019ef1");
		productIds.put("TT5461B0325C1", "9bd6760f1bf81f32ac100290ccf61644");
		productIds.put("Z00073CHOT", "4b529a751bf95827ac1002906066ef2a");
		productIds.put("Z00088XCN", "b2feebdf1bf9d23cac10029078b45af5");
		productIds.put("Z00107ST4K", "ebb5b0ee1bfa7a84ac10029016cf0398");
		productIds.put("Z00110AUO", "a02ef94d1bfadc82ac10029050b19ec0");
	}

	/* (non-Javadoc)
	 * @see com.wsla.util.migration.AbstractImporter#run()
	 */
	@Override
	void run() throws Exception {
		//use this to cross-ref the DB for missing products (copy SQL inserts from production)
		//for (String s : productIds.values())
		//System.out.println("('" + s + "'),");

		File[] files = listFilesMatching(props.getProperty("soLineItemsFile"), "(.*)SOLNI(.*)");

		for (File f : files)
			data.addAll(readFile(f, SOLNIFileVO.class, SHEET_1));

		loadTicketIds();
		loadCasLocations();
		loadProductIds();
		loadActivities();

		//Note we don't have a delete here; deleting the tickets will cascade into the tables affecting LineItems

		save();
	}


	/**
	 * Save the imported providers to the database.
	 * @param data
	 * @throws Exception 
	 */
	@Override
	protected void save() throws Exception {
		Map<String, List<PartVO>> tktParts = new HashMap<>(data.size());
		List<TicketCommentVO> activities = new ArrayList<>(data.size());
		List<CreditMemoVO> credits = new ArrayList<>(data.size());

		//split the data into the 3 categories
		for (SOLNIFileVO vo : data) {
			if (vo.getSoNumber().matches("(?i)^WSL0(.*)$")) continue;

			if (vo.isInventory()) {
				PartVO part = transposeInventoryData(vo, new PartVO());
				if (!StringUtil.isEmpty(part.getTicketId())) {
					List<PartVO> parts = tktParts.get(part.getTicketId());
					if (parts == null) parts = new ArrayList<>();
					parts.add(part);
					tktParts.put(part.getTicketId(), parts);
				}

			} else if (vo.isService()) {
				TicketCommentVO cmt = new TicketCommentVO();
				transposeCommentData(vo, cmt);
				//don't save comments if we don't have the ticket
				if (!StringUtil.isEmpty(cmt.getTicketId()))
					activities.add(cmt);

			} else if (vo.isCreditMemo()) {
				credits.add(transposeCreditData(vo, new CreditMemoVO()));
			}
		}
		log.info(String.format("found %d tickets with parts", tktParts.size()));
		log.info(String.format("found %d service line items (activities)", activities.size()));
		log.info(String.format("found %d inventory credits (harvested parts)", credits.size()));

		//no parts, no shipments
		if (!isOpenTktRun)
			saveParts(tktParts);

		saveActivities(activities);

		//writeToDB(credits);
	}


	/**
	 * @param tktParts
	 * @throws Exception 
	 */
	private void saveParts(Map<String, List<PartVO>> tktParts) throws Exception {
		//verify the referenced products are all present
		verifyInventory(tktParts.values());

		//create a shipment for each ticket
		Map<String, ShipmentVO> shipments = createShipments(tktParts);
		log.info(String.format("created %d shipments from %d tickets containing parts", shipments.size(), tktParts.size()));

		//save the shipments
		writeToDB(new ArrayList<>(shipments.values()));

		//each shipment gets two ledger entries - order shipped and order received
		createShipmentLedgers(shipments.values());

		//marry the shipment IDs to the parts and create a flattened list of parts to save
		List<PartVO> parts = new ArrayList<>(tktParts.size());
		for (Map.Entry<String, List<PartVO>> entry : tktParts.entrySet()) {
			ShipmentVO shipVo = shipments.get(entry.getKey());
			if (shipVo == null)
				continue;

			//bind the ticket's shipment to each part on the ticket
			for (PartVO part : entry.getValue()) {
				part.setShipmentId(shipVo.getShipmentId());
				parts.add(part);
			}
		}
		log.info(String.format("married parts to shipments.  saving %d parts", parts.size()));
		writeToDB(parts);
	}


	/**
	 * Create two ledger entries for each shipment:
	 * 1) the shipment was generated and left the warehouse
	 * 2) the shipment was received at the cas and processed
	 * @param shipments
	 * @throws Exception 
	 */
	private void createShipmentLedgers(Collection<ShipmentVO> shipments) throws Exception {
		List<TicketLedgerVO> entries = new ArrayList<>(shipments.size()*2);

		TicketLedgerVO vo;
		for (ShipmentVO shipment : shipments) {
			vo = new TicketLedgerVO();
			vo.setTicketId(shipment.getTicketId());
			vo.setDispositionBy(SOHeader.LEGACY_USER_ID);
			vo.setSummary("Envío Programado"); //shipment scheduled
			vo.setStatusCode(StatusCode.PARTS_SHIPPED_CAS);
			vo.setCreateDate(shipment.getCreateDate());
			entries.add(vo);

			vo = new TicketLedgerVO();
			vo.setTicketId(shipment.getTicketId());
			vo.setDispositionBy(SOHeader.LEGACY_USER_ID);
			vo.setSummary("Envío Recibido"); //shipment received
			vo.setStatusCode(StatusCode.PARTS_RCVD_CAS);
			vo.setCreateDate(shipment.getCreateDate());
			entries.add(vo);
		}
		log.info("saving " + entries.size() + " shipment ledger entries");
		writeToDB(entries);
	}


	/**
	 * Create a shipment for each of the tickets using context from the parts.  (there's often a shipping label in there!)
	 * @param tktParts
	 * @return
	 */
	private Map<String, ShipmentVO> createShipments(Map<String, List<PartVO>> tktParts) {
		Map<String, ShipmentVO> shipments = new HashMap<>(tktParts.size());

		for (Map.Entry<String, List<PartVO>> entry : tktParts.entrySet()) {
			//if this ticket has no parts there's no shipment
			if (entry.getValue() == null || entry.getValue().isEmpty()) continue;

			ShipmentVO vo = new ShipmentVO();
			vo.setTicketId(entry.getKey());
			vo.setFromLocationId(SOHeader.LEGACY_PARTS_LOCN);
			vo.setToLocationId(casLocations.get(entry.getKey()));
			vo.setShipmentType(ShipmentType.PARTS_REQUEST);
			vo.setShippedById(SOHeader.LEGACY_USER_ID);
			vo.setStatus(ShipmentStatus.RECEIVED); //implies created & sent

			//if one of the parts is a shipping label use it to enhance the shipment record
			//iterate the parts so we can remove the shipping label after using it - we don't want it saved as an actual part
			//log.debug(String.format("%s has %d inventory LNIs", vo.getTicketId(), entry.getValue().size()))
			Iterator<PartVO> iter = entry.getValue().iterator();
			while (iter.hasNext()) {
				PartVO part = iter.next();
				if (!StringUtil.isEmpty(part.getCustomerProductId()) && isShippingLabel(part)) { //only the last label is getting used
					vo.setShipmentDate(part.getCreateDate());
					vo.setArrivalDate(part.getCreateDate());
					String carrierNm = part.getCustomerProductId().replaceAll(" GUIA", "").toUpperCase();
					vo.setCarrierType(EnumUtil.safeValueOf(CarrierType.class, carrierNm));
					iter.remove();
				} else if (StringUtil.isEmpty(part.getProductId())) {
					iter.remove(); // we can't save products that aren't present in product_master - these are the red list we printed initially that someone should fix
				}
			}
			//log.debug(String.format("%s has %d parts", vo.getTicketId(), entry.getValue().size()))
			shipments.put(vo.getTicketId(), vo);
		}

		return shipments;
	}


	/**
	 * @param activities
	 * @throws Exception 
	 */
	private void saveActivities(List<TicketCommentVO> activities) throws Exception {
		//write the activities to the comments table 
		writeToDB(activities);

		//wait for the writes to flush so we can read them
		log.info("waiting 5s for activities to commit");
		sleepThread(5000);

		StringBuilder sql = new StringBuilder(700);
		sql.append(DBUtil.INSERT_CLAUSE).append(schema).append("wsla_ticket_ledger (ledger_entry_id,disposition_by_id,ticket_id,");
		sql.append("summary_txt,create_dt,billable_amt_no,billable_activity_cd) ");
		sql.append("select replace(newid(),'-',''), '").append(SOHeader.LEGACY_USER_ID).append("',t.ticket_id, tc.comment_txt, tc.create_dt, ");
		sql.append("coalesce(wb.invoice_amount_no, 0), ba.billable_activity_cd ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("wsla_ticket_comment tc ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_ticket t on tc.ticket_id=t.ticket_id ").append("and t.batch_txt=").append(StringUtil.checkVal(batchNm, true));
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_product_warranty pw on t.product_warranty_id=pw.product_warranty_id ");
		//LOJ here because we want to create a timeline entry for all activities - not just those with billable amts attached
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_billable_activity ba on tc.activity_type_cd=ba.billable_activity_cd ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_warranty_billable_xr wb on pw.warranty_id=wb.warranty_id and wb.billable_activity_cd=ba.billable_activity_cd ");
		sql.append("where tc.activity_type_cd != 'COMMENT'");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			int cnt = ps.executeUpdate();
			log.info(String.format("Added %d rows to the timeline for ticket activities", cnt));
		} catch (SQLException sqle) {
			log.error("could not add ledger entries for ticket activies", sqle);
		}
	}


	/**
	 * Assert we have product_master records for the inventory items
	 * @param inventory
	 */
	static void verifyInventory(Collection<List<PartVO>> tktParts) {
		Set<String> missingProducts = new HashSet<>(tktParts.size());
		int removeCnt = 0;

		//check for missing inventory
		for (List<PartVO> inventory : tktParts) {
			Iterator<PartVO> iter = inventory.iterator();
			while (iter.hasNext()) {
				PartVO part = iter.next();
				if (StringUtil.isEmpty(part.getProductId()) && !isShippingLabel(part)) { //ignore shipping labels
					missingProducts.add(part.getCustomerProductId());
					iter.remove();
					++removeCnt;
				}
			}
		}

		//report missing inventory to the console
		if (!missingProducts.isEmpty()) {
			log.info(String.format("removed %d records because of missing products", removeCnt));
			for (String s : missingProducts) {
				System.err.println(s);
			}
			//TODO reinstate this for production runs, once Steve gives us all the missing products
			//throw new RuntimeException("missing above products, can't proceed");
		}
		log.info("all inventory is accounted for!");
	}


	/**
	 * Does this part# match a shipping label pattern.
	 * We use these for the shipment, but they're not imported as inventory
	 * @param part
	 * @return
	 */
	static boolean isShippingLabel(PartVO part) {
		return part.getCustomerProductId().matches("(?i).*\\ GUIA$");
	}


	/**
	 * Parts/Inventory:
	 * Transpose and enhance the data we get from the import file into what the new schema needs
	 * @param dataVo
	 * @param partVO
	 * @return
	 **/
	private PartVO transposeInventoryData(SOLNIFileVO dataVo, PartVO vo) {
		String id = dataVo.getProductId();
		vo.setProductId(productIds.get(id)); //transposed
		if (StringUtil.isEmpty(vo.getProductId())) { //try removing any harvesting suffix
			id = id.replaceAll("(?i)^([A-Z0-9]+):?H{1,}$","$1");
			log.debug("trying " + id + " from " + dataVo.getProductId());
			vo.setProductId(productIds.get(id));
		}
		vo.setCustomerProductId(id); //capture the original (w/o harvesting flags) so we can report missing products
		vo.setTicketId(ticketIds.get(dataVo.getSoNumber())); //transposed
		vo.setQuantity(dataVo.getQntyNeeded());
		vo.setQuantityReceived(dataVo.getQntyCommitted());
		vo.setUsedQuantityNo(vo.getQuantityReceived()); //presume we used all the parts we GOT (not parts REQUESTED)
		vo.setCreateDate(dataVo.getReceivedDate());
		vo.setSubmitApprovalFlag(1);
		return vo;
	}


	/**
	 * activities:
	 * Transpose and enhance the data we get from the import file into what the new schema needs
	 * @param dataVo
	 * @param commentVO
	 * @return
	 **/
	private TicketCommentVO transposeCommentData(SOLNIFileVO dataVo, TicketCommentVO vo) {
		vo.setTicketId(ticketIds.get(dataVo.getSoNumber())); //transposed
		StringBuilder comment = new StringBuilder(500);
		if (!StringUtil.isEmpty(dataVo.getDesc1())) comment.append(dataVo.getDesc1());

		//add a note about quantity if >1
		if (dataVo.getQntyNeeded() > 1)
			comment.append("\rQnty: ").append(dataVo.getQntyNeeded());
		//add a note about unit cost if >0
		if (dataVo.getAmtBilled() > 0)
			comment.append("\rUnit Price: ").append(dataVo.getAmtBilled());
		//add a note about billable cost if >0
		if (dataVo.getAmtBilled() > 0)
			comment.append("\rBilled: ").append(dataVo.getAmtBilled());

		vo.setComment(comment.toString());
		vo.setCreateDate(dataVo.getChronoReceivedDate());
		vo.setUserId(SOHeader.LEGACY_USER_ID);
		vo.setActivityType(activityMap.get(dataVo.getProductId()));
		if (StringUtil.isEmpty(vo.getActivityType())) {
			log.error("activityType not listed " + dataVo.getProductId() + " using OTHER instead");
			vo.setActivityType("OTHER");
		}
		return vo;
	}


	/**
	 * Credits:
	 * Transpose and enhance the data we get from the import file into what the new schema needs
	 * @param dataVo
	 * @param creditVO
	 * @return
	 **/
	private CreditMemoVO transposeCreditData(SOLNIFileVO dataVo, CreditMemoVO vo) {
		//TODO - talk to Camire about how these go in
		vo.setTicketId(ticketIds.get(dataVo.getSoNumber())); //transposed
		vo.setCreateDate(dataVo.getReceivedDate());
		return vo;
	}


	/**
	 * Populate the Map<Ticket#, TicketId> from the database to marry the soNumbers in the Excel
	 */
	private void loadTicketIds() {
		String sql = StringUtil.join("select ticket_no as key, ticket_id as value from ", schema, "wsla_ticket");
		MapUtil.asMap(ticketIds, db.executeSelect(sql, null, new GenericVO()));
		log.debug(String.format("loaded %d ticketIds", ticketIds.size()));
	}


	/**
	 * Populate the Map<Ticket#, casLocation> from the database to marry shipments to cas locations
	 */
	private void loadCasLocations() {
		String sql = StringUtil.join("select ticket_id as key, location_id as value from ", schema, 
				"wsla_ticket_assignment where assg_type_cd='CAS' and location_id is not null");

		MapUtil.asMap(casLocations, db.executeSelect(sql, null, new GenericVO()));
		log.debug(String.format("loaded %d casLocations", casLocations.size()));
	}


	/**
	 * Populate the Map<Legacy/Product#/Alias, CypherProductId> from the 
	 * database to marry the soNumbers in the Excel.
	 * The Order By here prioritizes best matches over desparation matches (desc_txt)
	 * @return 
	 */
	public Map<String, String> loadProductIds() {
		String sql = StringUtil.join("select 3, cust_product_id as key, ",
				"product_id as value from ", schema, "wsla_product_master where length(cust_product_id)>0 ", 
				DBUtil.UNION_ALL,"select 2, sec_cust_product_id as key, product_id as value from ", 
				schema, "wsla_product_master where length(sec_cust_product_id)>0 ", 
				DBUtil.UNION_ALL,"select 1, desc_txt as key, product_id as value from ", 
				schema, "wsla_product_master where length(desc_txt)>0 order by 1");

		MapUtil.asMap(productIds, db.executeSelect(sql, null, new GenericVO()));
		log.debug(String.format("loaded %d productIds", productIds.size()));
		return productIds;
	}


	/**
	 * Populate the Map of activities.
	 * These came from the master listing Excel file - from Steve.
	 * Take what doesn't align in Cypher and throw them into 'other'.
	 */
	private void loadActivities() {
		activityMap.put("AD-CLMADMN","OTHER");
		activityMap.put("AD-HARVEST","OTHER");
		activityMap.put("AD-REFUND","OTHER");
		activityMap.put("ADJ AUTHOR","ADD_ASSETS");
		activityMap.put("ADJ ENTRGA","ADD_ASSETS");
		activityMap.put("ADJ FOTOS","ADD_ASSETS");
		activityMap.put("ADJ MISC","ADD_ASSETS");
		activityMap.put("ADJ PHOTOS","ADD_ASSETS");
		activityMap.put("ADJ POP","ADD_ASSETS");
		activityMap.put("ADJ SERIE","ADD_ASSETS");
		activityMap.put("BOD-ADDCOS","PRODUCT_HANDLING");
		activityMap.put("BOD-ENVDAT","PRODUCT_HANDLING");
		activityMap.put("BOD-ENVPTE","PRODUCT_HANDLING");
		activityMap.put("BOD-ESTAF+","PRODUCT_HANDLING");
		activityMap.put("BOD-PICPAC","PRODUCT_HANDLING");
		activityMap.put("BOD-PROPRT","PRODUCT_HANDLING");
		activityMap.put("BOD-RECDEF","PRODUCT_HANDLING");
		activityMap.put("BOD-RECEPC","PRODUCT_HANDLING");
		activityMap.put("BOD-RETAUT","PRODUCT_HANDLING");
		activityMap.put("CHATENTR01","EMAIL");
		activityMap.put("CHATENTR02","EMAIL");
		activityMap.put("CHATENTR03","EMAIL");
		activityMap.put("CHATENTR04","EMAIL");
		activityMap.put("CHATENTR05","EMAIL");
		activityMap.put("CHATSALD01","EMAIL");
		activityMap.put("CHATSALD02","EMAIL");
		activityMap.put("CHATSALD03","EMAIL");
		activityMap.put("CHATSALD04","EMAIL");
		activityMap.put("CHATSALD05","EMAIL");
		activityMap.put("CONINIENC","EMAIL");
		activityMap.put("CONINISALF","EMAIL");
		activityMap.put("CONINIVIS","OTHER");
		activityMap.put("CONTINICHT","EMAIL");
		activityMap.put("CONTINILAM","PHONE");
		activityMap.put("CONTINIWEB","EMAIL");
		activityMap.put("CONTINIZEN","EMAIL");
		activityMap.put("EMLENTR01","EMAIL");
		activityMap.put("EMLENTR02","EMAIL");
		activityMap.put("EMLENTR03","EMAIL");
		activityMap.put("EMLENTR04","EMAIL");
		activityMap.put("EMLENTR05","EMAIL");
		activityMap.put("EMLSALD01","EMAIL");
		activityMap.put("EMLSALD02","EMAIL");
		activityMap.put("EMLSALD03","EMAIL");
		activityMap.put("EMLSALD04","EMAIL");
		activityMap.put("EMLSALD05","EMAIL");
		activityMap.put("EMLSALD06","EMAIL");
		activityMap.put("ENV-ENTODO","OTHER");
		activityMap.put("ENV-INCAS","OTHER");
		activityMap.put("ENV-INPROD","OTHER");
		activityMap.put("ENV-MOVIRR","OTHER");
		activityMap.put("ENV-OUTCAS","OTHER");
		activityMap.put("ENV-OUTPRD","OTHER");
		activityMap.put("ENV-SATODO","OTHER");
		activityMap.put("ENV-SEGPAN","OTHER");
		activityMap.put("ENV-SOBPES","OTHER");
		activityMap.put("ENV-SOCOMB","OTHER");
		activityMap.put("LLAMENTR01","PHONE");
		activityMap.put("LLAMENTR02","PHONE");
		activityMap.put("LLAMENTR03","PHONE");
		activityMap.put("LLAMENTR04","PHONE");
		activityMap.put("LLAMENTR05","PHONE");
		activityMap.put("LLAMSALD01","PHONE_OUT");
		activityMap.put("LLAMSALD02","PHONE_OUT");
		activityMap.put("LLAMSALD03","PHONE_OUT");
		activityMap.put("LLAMSALD04","PHONE_OUT");
		activityMap.put("LLAMSALD05","PHONE_OUT");
		activityMap.put("LLAMSALD06","PHONE_OUT");
		activityMap.put("Loan","OTHER");
		activityMap.put("MGT-AUT DO","OTHER");
		activityMap.put("MGT-AUTCER","OTHER");
		activityMap.put("MGT-AUTH","OTHER");
		activityMap.put("MGT-EXPORT","OTHER");
		activityMap.put("MGT-NO POP","OTHER");
		activityMap.put("MGT-PROF","OTHER");
		activityMap.put("MGT-PROFC1","OTHER");
		activityMap.put("MGT-PROFC2","OTHER");
		activityMap.put("MGT-PROFC3","OTHER");
		activityMap.put("MGT-PROMUL","OTHER");
		activityMap.put("MGT-PROREV","OTHER");
		activityMap.put("MGT-PROSEG","OTHER");
		activityMap.put("MGT-REEMB","OTHER");
		activityMap.put("MGT-REPORT","OTHER");
		activityMap.put("S-CANIBAL","OTHER");
		activityMap.put("S-DEVOLUCI","OTHER");
		activityMap.put("S-ETIQUETA","OTHER");
		activityMap.put("S-EVENTO","OTHER");
		activityMap.put("SOPT-REMOT","OTHER");
		activityMap.put("S-REEMPART","OTHER");
		activityMap.put("S-REEMPPRD","OTHER");
		activityMap.put("S-REFURB","OTHER");
		activityMap.put("S-REPMENOR","OTHER");
		activityMap.put("S-REPNIVFB","OTHER");
		activityMap.put("S-REPNIVNR","OTHER");
		activityMap.put("S-REPSNG","OTHER");
		activityMap.put("S-REVISION","OTHER");
		activityMap.put("S-VIDOMTDA","OTHER");
		activityMap.put("S-VISIDOMI","OTHER");
		activityMap.put("TDA-REFUF","OTHER");
		activityMap.put("TDA-REMPUF","OTHER");
		activityMap.put("ZENENTR01","EMAIL");
		activityMap.put("ZENENTR02","EMAIL");
		activityMap.put("ZENENTR03","EMAIL");
		activityMap.put("ZENENTR04","EMAIL");
		activityMap.put("ZENENTR05","EMAIL");
		activityMap.put("ZENSALD01","EMAIL");
		activityMap.put("ZENSALD02","EMAIL");
		activityMap.put("ZENSALD03","EMAIL");
		activityMap.put("ZENSALD04","EMAIL");
		activityMap.put("ZENSALD05","EMAIL");
		activityMap.put("ZENSALD06","EMAIL");
		//added from full legacy import, per Steve
		activityMap.put("ENV-IN Z6","OTHER");
		activityMap.put("ENV-OUT Z1","OTHER");
		activityMap.put("ENV-OUT","OTHER");
		activityMap.put("POPADJUNTO", "ADD_ASSETS");
		activityMap.put("SV-BRDLREP","OTHER");
		activityMap.put("SV-CASREVI","OTHER");
		activityMap.put("SV-CORRENV","EMAIL");
		activityMap.put("SV-CORRREC","EMAIL");
		activityMap.put("SV-DPPREPL","OTHER");
		activityMap.put("SV-DPRTREP","OTHER");
		activityMap.put("SV-MINIREP","OTHER");
		activityMap.put("SV-SRVDEPO","OTHER");
		activityMap.put("S-VISIDOM","OTHER");

		log.debug("loaded " + activityMap.size() + " activities");
	}
}
