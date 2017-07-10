package com.ram.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.ram.action.or.vo.RAMCaseItemVO;
import com.ram.action.or.vo.RAMCaseKitVO;
import com.ram.action.or.vo.RAMCaseVO;
import com.ram.action.or.vo.RAMSignatureVO;
import com.ram.action.or.vo.RAMSignatureVO.SignatureType;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.filter.fileupload.Constants;

/****************************************************************************
 * <b>Title:</b> RAMCaseDBPersist.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> RAM Case Persistance Manager that works with ActionRequest.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Jul 3, 2017
 ****************************************************************************/
public class RAMCaseDBPersist extends AbstractPersist<SMTDBConnection, RAMCaseVO> {
	private static final Logger log = Logger.getLogger(RAMCaseDBPersist.class);
	private DBProcessor dbp;
	private Connection conn;
	private String schema;
	public RAMCaseDBPersist() {
		super();
	}

	/* (non-Javadoc)
	 * @see com.ram.persistance.PersistanceIntfc#load()
	 */
	@Override
	public RAMCaseVO load(String caseId) {
		RAMCaseVO cVo = null;
		List<Object> params = new ArrayList<>();
		params.add(caseId);
		List<Object> cvos = dbp.executeSelect(loadCaseSql(), params, initialize());
		if(cvos != null && ! cvos.isEmpty())
			cVo = (RAMCaseVO)cvos.get(0);

		return cVo;
	}


	/**
	 * Helper method that builds the case load query.  Retrieves all signatures,
	 * items and kits related to a given case_id.
	 * @return
	 */
	private String loadCaseSql() {
		StringBuilder sql = new StringBuilder(525);
		sql.append("select cu.customer_nm, su.first_nm || ' ' || su.last_nm as surgeon_nm, o.or_name ,c.*, i.*, p.*, k.* ");
		sql.append("from ").append(schema).append("ram_case c ");
		sql.append("left outer join ").append(schema).append("ram_case_signature s ");
		sql.append("on c.case_id = s.case_id ");
		sql.append("left outer join ").append(schema).append("ram_case_item i ");
		sql.append("on c.case_id = i.case_id ");
		sql.append("left outer join ").append(schema).append("ram_product p ");
		sql.append("on i.product_id = p.product_id ");
		sql.append("left outer join ").append(schema).append("RAM_CASE_KIT k ");
		sql.append("on c.case_id = k.case_id and k.case_kit_id = i.case_kit_id ");
		sql.append("left outer join ").append(schema).append("ram_customer cu ");
		sql.append("on c.customer_id = cu.customer_id ");
		sql.append("left outer join ").append(schema).append("ram_surgeon su ");
		sql.append("on c.surgeon_id = su.surgeon_id ");
		sql.append("left outer join ").append(schema).append("ram_or_room o ");
		sql.append("on c.or_room_id = o.or_room_id ");
		sql.append("where c.case_id = ? ");
		
		log.info(sql);
		return sql.toString();
	}


	/**
	 * DB Persist Save method saves the Case and Signatures, drops all existing
	 * items and kits then re-inserts them using the same Ids they had from
	 * before.  Saves us having to track what items were added, removed,
	 * updated etc.  Entire method is wrapped in a manual commit transaction
	 * so that any errors can be immediately rolled back.
	 */
	@Override
	public RAMCaseVO save(RAMCaseVO cVo) {
		if(cVo != null) {
			try {
				//Set Autocommit False.
				conn.setAutoCommit(false);
				
				//Save Case.
				if (cVo.isNewCase()) dbp.insert(cVo);
				else dbp.save(cVo);
				
				//Add Signatures.
				saveSignatures(cVo);
				
				//Flush and insert Items/Kits.
				deleteChildren(cVo);
				insertItems(cVo);
				insertKits(cVo);
				
				//Commit transaction.
				conn.commit();
			} catch (InvalidDataException | DatabaseException | SQLException e) {
				log.error("Error Saving Case, rolling back changes.", e);
				try {
					conn.rollback();
				} catch(SQLException sqle) {
					log.error("Problem Rolling back DB.", sqle);
				}

			} finally {
				try {
					conn.setAutoCommit(true);
				} catch(SQLException sqle) {
					log.error("Probleming setting AutoCommit back to true.", sqle);
				}
			}
		}

		return cVo;
	}


	/**
	 * Helper method removes all Items and Kits from the Case.  They will be
	 * re-inserted immediately following this invocation.
	 * @param cVo
	 * @throws SQLException
	 */
	public void deleteChildren(RAMCaseVO cVo) throws SQLException {
		//Delete All Items on the Case.
		try(PreparedStatement ps = conn.prepareStatement(getItemDelSql())) {
			ps.setString(1, cVo.getCaseId());
			ps.executeUpdate();
		}

		//Delete All Kits on the Case.
		try(PreparedStatement ps = conn.prepareStatement(getKitDelSql())) {
			ps.setString(1, cVo.getCaseId());
			ps.executeUpdate();
		}
	}

	/**
	 * Helper method that builds the Kit Deletion Query.
	 * @return
	 */
	private String getKitDelSql() {
		StringBuilder sql = new StringBuilder(150);
		sql.append("delete from ").append(schema).append("RAM_CASE_KIT where case_id = ? ");
		return sql.toString();
	}

	/**
	 * Helper method that builds the Item Deleting Query.
	 * @return
	 */
	private String getItemDelSql() {
		StringBuilder sql = new StringBuilder(150);
		sql.append("delete from ").append(schema).append("RAM_CASE_ITEM where case_id = ? ");
		return sql.toString();
	}

	/**
	 * Helper method iterates over given RAMCaseVO Kits and saves them all.
	 * @param cVo
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 */
	private void insertKits(RAMCaseVO cVo) throws InvalidDataException, DatabaseException {
		for(Entry<String, RAMCaseKitVO> kit : cVo.getKits().entrySet()) {
			RAMCaseKitVO k = kit.getValue();
			// TODO Hard coded this for the kit product. Needs to be changed
			if (k.getLocationItemMasterId() == 0) k.setLocationItemMasterId(7896543);
			
			//Ensure CaseId is set correctly.
			k.setCaseId(cVo.getCaseId());

			dbp.insert(k);
			if(dbp.getGeneratedPKId() != null) {
				k.setCaseKitId(dbp.getGeneratedPKId());
			}
		}
	}

	/**
	 * Helper method iterates over given RAMCaseVO Items and saves them all.
	 * @param cVo
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 */
	private void insertItems(RAMCaseVO cVo) throws InvalidDataException, DatabaseException {
		for(Entry<String, Map<String, RAMCaseItemVO>> items : cVo.getItems().entrySet()) {
			for(Entry<String, RAMCaseItemVO> e : items.getValue().entrySet()) {
				RAMCaseItemVO i = e.getValue();

				//Ensure CaseId is set correctly.
				i.setCaseId(cVo.getCaseId());

				dbp.insert(i);
				if(dbp.getGeneratedPKId() != null) {
					i.setCaseItemId(dbp.getGeneratedPKId());
				}
			}
		}
	}

	/**
	 * Helper method iterates over RAMCaseVO Signatures and saves them all.
	 * @param cVo
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 */
	private void saveSignatures(RAMCaseVO cVo) throws InvalidDataException, DatabaseException {
		for(Entry<SignatureType, Map<String, RAMSignatureVO>> sigs : cVo.getSignatures().entrySet()) {
			for(Entry<String, RAMSignatureVO> e : sigs.getValue().entrySet()) {
				RAMSignatureVO s = e.getValue();
				//Ensure CaseId is set correctly.
				s.setCaseId(cVo.getCaseId());

				dbp.save(s);
				if(dbp.getGeneratedPKId() != null) {
					s.setSignatureId(dbp.getGeneratedPKId());
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.ram.persistance.PersistanceIntfc#flush()
	 */
	@Override
	public void flush(String caseId) {
		//Not necessary for DB.
	}


	/* (non-Javadoc)
	 * @see com.ram.persistance.PersistanceIntfc#initialize()
	 */
	@Override
	public RAMCaseVO initialize() {
		return new RAMCaseVO();
	}

	/* (non-Javadoc)
	 * @see com.ram.persistance.PersistanceIntfc#setPersistanceSource(java.lang.Object)
	 */
	@Override
	public void setPersistanceSource(SMTDBConnection source) {
		this.conn = source;
		this.dbp = new DBProcessor(conn, schema);
	}

	@Override
	public void setAttributes(Map<String, Object> attributes) {
		super.setAttributes(attributes);
		schema = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);
	}
}
