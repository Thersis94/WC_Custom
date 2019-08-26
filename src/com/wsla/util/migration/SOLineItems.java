package com.wsla.util.migration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.MapUtil;
import com.siliconmtn.util.StringUtil;
import com.wsla.data.ticket.CreditMemoVO;
import com.wsla.data.ticket.PartVO;
import com.wsla.data.ticket.TicketCommentVO;
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
	private Map<String, String> productIds = new HashMap<>(10000);

	/* (non-Javadoc)
	 * @see com.wsla.util.migration.AbstractImporter#run()
	 */
	@Override
	void run() throws Exception {
		File[] files = listFilesMatching(props.getProperty("soLineItemsFile"), "(.*)SOLNI(.*)");

		for (File f : files)
			data.addAll(readFile(f, SOLNIFileVO.class, SHEET_1));

		loadTicketIds();
		loadProductIds();

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
		List<PartVO> inventory = new ArrayList<>(data.size());
		List<TicketCommentVO> comments = new ArrayList<>(data.size());
		List<CreditMemoVO> credits = new ArrayList<>(data.size());

		//split the data into the 3 categories
		for (SOLNIFileVO vo : data) {
			if (vo.isInventory()) {
				inventory.add(transposeInventoryData(vo, new PartVO()));
			} else if (vo.isService()) {
				comments.add(transposeCommentData(vo, new TicketCommentVO()));
			} else if (vo.isCreditMemo()) {
				credits.add(transposeCreditData(vo, new CreditMemoVO()));
			}
		}
		log.info("found " + inventory.size() + " inventory line items");
		log.info("found " + comments.size() + " service line items (comments)");
		log.info("found " + credits.size() + " credit memos");

		verifyInventory(inventory);

		//		super.writeToDB(inventory);
		//		super.writeToDB(comments);
		//		super.writeToDB(credits);
	}


	/**
	 * Assert we have product_master records for the inventory items
	 * @param inventory
	 */
	private void verifyInventory(List<PartVO> inventory) {
		//check for missing inventory
		Set<String> missingProducts = new HashSet<>(inventory.size());
		for (PartVO part : inventory) {
			if (StringUtil.isEmpty(part.getProductId())) {
				missingProducts.add(part.getCustomerProductId());
			}
		}
		if (!missingProducts.isEmpty()) {
			for (String s : missingProducts)
				System.err.println(s);
			throw new RuntimeException("missing above products, can't proceed");
		}
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
			id = id.replaceAll("^([A-Z0-9]+):?H$","$1");
			log.debug("trying " + id + " from " + dataVo.getProductId());
			vo.setProductId(productIds.get(id));
		}
		vo.setCustomerProductId(id); //capture the original (w/o harvesting flags) so we can report missing products
		vo.setTicketId(ticketIds.get(dataVo.getSoNumber())); //transposed
		vo.setQuantity(dataVo.getDefectiveQnty());
		vo.setQuantityReceived(dataVo.getQntyCommitted());
		vo.setCreateDate(dataVo.getReceivedDate());
		return vo;
	}


	/**
	 * Comments:
	 * Transpose and enhance the data we get from the import file into what the new schema needs
	 * @param dataVo
	 * @param commentVO
	 * @return
	 **/
	private TicketCommentVO transposeCommentData(SOLNIFileVO dataVo, TicketCommentVO vo) {
		vo.setTicketId(ticketIds.get(dataVo.getSoNumber())); //transposed
		StringBuilder comment = new StringBuilder(500);
		if (!StringUtil.isEmpty(dataVo.getDesc1())) comment.append(dataVo.getDesc1());
		if (!StringUtil.isEmpty(dataVo.getDesc2())) {
			if (comment.length() > 0) comment.append(" / ");
			comment.append(dataVo.getDesc2());
		}
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
		String sql = StringUtil.join("select ticket_no as key, ticket_id as value from ", schema, "wsla_ticket where historical_flg=1");

		MapUtil.asMap(ticketIds, db.executeSelect(sql, null, new GenericVO()));
		log.debug("loaded " + ticketIds.size() + " ticketIds");
	}


	/**
	 * Populate the Map<Legacy/Product#/Alias, CypherProductId> from the 
	 * database to marry the soNumbers in the Excel.
	 * The Order By here prioritizes best matches over desparation matches (desc_txt)
	 */
	private void loadProductIds() {
		String sql = StringUtil.join("select 3, cust_product_id as key, ",
				"product_id as value from ", schema, "wsla_product_master where length(cust_product_id)>0 ", 
				DBUtil.UNION_ALL,"select 2, sec_cust_product_id as key, product_id as value from ", 
				schema, "wsla_product_master where length(sec_cust_product_id)>0 ", 
				DBUtil.UNION_ALL,"select 1, desc_txt as key, product_id as value from ", 
				schema, "wsla_product_master where length(desc_txt)>0 order by 1");

		MapUtil.asMap(productIds, db.executeSelect(sql, null, new GenericVO()));
		log.debug("loaded " + productIds.size() + " productIds");
	}
}
