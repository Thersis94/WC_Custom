package com.ram.persistence;

// JDK 1.7
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

// RAM Libs
import com.ram.action.or.vo.RAMCaseItemVO;
import com.ram.action.or.vo.RAMCaseKitVO;
import com.ram.action.or.vo.RAMCaseVO;
import com.ram.action.or.vo.RAMSignatureVO;
import com.ram.action.or.vo.RAMSignatureVO.SignatureType;

// SMT Base Libs
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.filter.fileupload.Constants;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.SBProfileManager;

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
		// Retrieve base case.  Return null if not found
		RAMCaseVO cVo = null;
		List<Object> params = new ArrayList<>();
		params.add(caseId);
		List<Object> cvos = dbp.executeSelect(loadCaseSql(), params, initialize());
		if(cvos == null || cvos.isEmpty()) return null;
		
		// Get the case from the collection
		cVo = (RAMCaseVO)cvos.get(0);
		
		// Get the hospital rep info
		cVo.setHospitalRep(getUserByProfileId(cVo.getProfileId()));
		
		// Get the sales rep info
		cVo.setSalesRep(getUserByProfileId(cVo.getSalesRepId()));
		
		// Get case items
		getCaseItems(cVo);
		
		return cVo;
	}
	
	/**
	 * Loads all of the items in the Case.  SInce the items are stored as a map of maps,
	 * it was easier to utilize an the existing case vo and store the items formatted properly
	 * @param caseId
	 */
	public void getCaseItems(RAMCaseVO cvo ) {
		StringBuilder sql = new StringBuilder(256);
		sql.append("select i.*, p.*, c.customer_nm ");
		sql.append(DBUtil.FROM_CLAUSE).append("custom.ram_case_item i ");
		sql.append(DBUtil.INNER_JOIN).append("custom.ram_product p on i.product_id = p.product_id ");
		sql.append(DBUtil.INNER_JOIN).append("custom.ram_customer c on p.customer_id = c.customer_id ");
		sql.append("where i.case_id = ? ");
		
		try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
			ps.setString(1, cvo.getCaseId());
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				cvo.addItem(new RAMCaseItemVO(rs));
			}
			
		} catch(Exception e) {
			log.error("unable to load case items", e);
		}
	}

	/**
	 * Retrieves a user based upon the profile id
	 * @param profileId
	 * @return
	 */
	public UserDataVO getUserByProfileId(String profileId) {
	    ProfileManager pm = new SBProfileManager(this.attributes);
		UserDataVO user = null;
	    try {
			user = pm.getProfile(profileId, conn, ProfileManager.PROFILE_ID_LOOKUP, null);
		} catch (com.siliconmtn.exception.DatabaseException e) {
			log.error("Unable to retrieve user rep", e);
		}
	    
		return user;
	}

	/**
	 * Helper method that builds the case load query.  Retrieves all signatures,
	 * items and kits related to a given case_id.
	 * @return
	 */
	private String loadCaseSql() {
		StringBuilder sql = new StringBuilder(525);
		sql.append("select cu.customer_nm, su.first_nm || ' ' || su.last_nm as surgeon_nm, o.or_name ,c.*, s.*, k.* ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("ram_case c ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("ram_case_signature s ");
		sql.append("on c.case_id = s.case_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("ram_customer_location cl ");
		sql.append("on c.customer_location_id = cl.customer_location_id  ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("ram_customer cu ");
		sql.append("on cl.customer_id = cu.customer_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("ram_surgeon su ");
		sql.append("on c.surgeon_id = su.surgeon_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("ram_or_room o ");
		sql.append("on c.or_room_id = o.or_room_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("ram_case_kit k ");
		sql.append("on c.case_id = k.case_id ");
		sql.append("where c.case_id = ? ");
		
		log.debug(sql);
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
				insertKits(cVo);
				insertItems(cVo);

				//Commit transaction.
				conn.commit();
				cVo.setNewCase(false);
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
				
				// ensure the signature exists
				if (StringUtil.isEmpty(s.getSignatureTxt())) continue;
				
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
