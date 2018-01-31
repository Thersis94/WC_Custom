package com.rezdox.util.migration;

// Java 8
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Log4j
import org.apache.log4j.PropertyConfigurator;

//SMTBaseLibs
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.UUIDGenerator;

import opennlp.tools.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: LegacyDataMigration.java<p/>
 * <b>Description: This class was created to batch-load and normalize legacy
 * RezDox data for the RezDox site into the new data model. This class manages
 * the entirety of the data migration.</b>
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2018<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Jan 19, 2018
 ****************************************************************************/
public class LegacyDataMigration extends CommandLineUtil {

	// import env params
	private static final String SOURCE_FILE_CONFIG="scripts/rezdox/migration_config.properties";
	private static final String SOURCE_FILE_LOG="scripts/rezdox/migration_log4j.properties";
	
	// delimiters used in legacy fields
	private static final String TWO_PIPES = "\\|\\|";
	private static final String FOUR_PIPES = "\\|\\|\\|\\|";
	
	private static final String BUSINESS_ID = "business_id";

	public LegacyDataMigration(String[] args) {
		super(args);
		PropertyConfigurator.configure(SOURCE_FILE_LOG);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		LegacyMemberImport lmi = new LegacyMemberImport(args);
		lmi.run();

		LegacyDataMigration ldm = new LegacyDataMigration(args);
		ldm.run();
	}
	
	public void run() {
		long startTimeInMillis = Calendar.getInstance().getTimeInMillis();
		
		// load props
		loadProperties(SOURCE_FILE_CONFIG);
		
		// get dbconn
		loadDBConnection(props);
		
		// start migration
		log.info("Migration Started");
		
		try {
			migrateResidences();
			migrateResidenceMembers();
			migrateResidenceAttributes();
			migrateRoomTypes();
			migrateBusinessCategories();
			migrateBusinesses();
			migrateBusinessAttributes();
			migrateBusinessReviews();
			migrateBusinessMembers();
			migrateMemberMessages();
			migrateNotifications();
			migrateConnections();
			createMemberships();
			createPromotions();
			createMembershipPromotions();
			createPaymentTypes();
			migrateSubscriptions();
			migrateResidenceRooms();
			migrateProjectCategoryTypes();
			migrateProjectHistory();
			migrateProjectAttributes();
			migrateProjectPhotosDocuments();
			migrateProjectMaterials();
			migrateProjectMaterialAttributes();
			migrateTreasureCategories();
			migrateTreasureItems();
			migrateTreasureItemAttributes();
			migrateTreasurePhotosDocuments();
			createRewardTypes();
			migrateRewards();
			migrateMemberRewards();
		} catch(Exception e) {
			log.error("Failed to migrate data.", e);
		}
		
		// clean up
		closeDBConnection();
		
		// end migration
		log.info("Migration Completed In " + (Calendar.getInstance().getTimeInMillis() - startTimeInMillis)/1000 + " Seconds");
	}
	
	/**
	 * Executes simple queries that don't require more complex mappings/normalization
	 * 
	 * @param sql
	 * @param migrationType
	 */
	private void executeSimpleMapping(StringBuilder sql, String migrationType) {
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString());) {
			ps.executeUpdate();
		} catch (Exception sqle) {
			log.error("Error migrating " + migrationType + " data.", sqle);
		}
	}
	
	/**
	 * Setup general prepared statement for adding photos from various parts of the old data model
	 * 
	 * @param photoPs
	 * @param legacyPhotoSql
	 * @param idName
	 * @throws Exception
	 */
	private void buildPhotoPs(PreparedStatement photoPs, StringBuilder legacyPhotoSql, String idName) throws Exception {
		try (PreparedStatement legacyPhotoPs = dbConn.prepareStatement(legacyPhotoSql.toString())) {
			ResultSet rs = legacyPhotoPs.executeQuery();
			while (rs.next()) {
				String[] photos = rs.getString("photos").split(FOUR_PIPES);
				String id = rs.getString(idName);
				
				processPhotos(id, photos, photoPs);
			}
		} catch (Exception e) {
			throw new Exception("Error retrieving legacy photo data.", e);
		}
	}
	
	/**
	 * Helper to take the array of photos that was split from one field in the legacy data
	 * and add individual records to the new data model for each.
	 * 
	 * @param id - the owner of the photos
	 * @param photos - the photos to be added
	 * @param photoPs - the prepared statement we are building
	 * @throws SQLException
	 */
	private void processPhotos(String id, String[] photos, PreparedStatement photoPs) throws SQLException {
		int order = 0;
		
		for (String photo : photos) {
			if ("0".equals(photo)) continue;
			int idx = 0;
			photoPs.setString(++idx, new UUIDGenerator().getUUID());
			photoPs.setString(++idx, id);
			photoPs.setString(++idx, photo);
			photoPs.setInt(++idx, order++);
			photoPs.setTimestamp(++idx, Convert.getCurrentTimestamp());
			photoPs.addBatch();
		}
	}
	
	/**
	 * Setup general prepared statement for adding documents from various parts of the old data model
	 * 
	 * @param documentPs
	 * @param legacyFileSql
	 * @param idName
	 * @throws Exception
	 */
	private void buildDocumentPs(PreparedStatement documentPs, StringBuilder legacyFileSql, String idName) throws Exception {
		try (PreparedStatement legacyFilePs = dbConn.prepareStatement(legacyFileSql.toString())) {
			ResultSet rs = legacyFilePs.executeQuery();
			while (rs.next()) {
				String[] files = rs.getString("files").split(FOUR_PIPES);
				String id = rs.getString(idName);
				
				for (String file : files) {
					int idx = 0;
					documentPs.setString(++idx, new UUIDGenerator().getUUID());
					documentPs.setString(++idx, id);
					documentPs.setString(++idx, file);
					documentPs.setTimestamp(++idx, Convert.getCurrentTimestamp());
					documentPs.addBatch();
				}
			}
		} catch (Exception e) {
			throw new Exception("Error retrieving legacy file data.", e);
		}
	}
	
	/**
	 * Builds the prepared statement values for inserting general attribute data to the attribute tables
	 * 
	 * @param attrPs
	 * @param legacySql
	 * @param columnsToSplit
	 * @param fieldPrefix
	 * @throws Exception
	 */
	private void buildAttributesPs(PreparedStatement attrPs, StringBuilder legacySql, Map<String, String> columnsToSplit,
			String fieldPrefix) throws Exception {

		try (PreparedStatement ps = dbConn.prepareStatement(legacySql.toString())) {
			ResultSet rs = ps.executeQuery();
			ResultSetMetaData metaData = rs.getMetaData();
			int columnCnt = metaData.getColumnCount();
			
			while (rs.next()) {
				for (int i = 1; i < columnCnt + 1; i++) {
					String columnName = metaData.getColumnName(i);
					
					// Split columns that have delimited data to be added as separate records
					splitAttributeColumn(columnsToSplit, columnName, attrPs, rs, fieldPrefix);
					
					// Skip fields that do not need to be added, or already added to ps
					if ("id".equals(columnName) || columnsToSplit.containsKey(columnName))
						continue;
					
					int idx = 0;
					attrPs.setString(++idx, new UUIDGenerator().getUUID());
					attrPs.setString(++idx, rs.getObject("id").toString());
					attrPs.setString(++idx, fieldPrefix + "_" + columnName.toUpperCase());
					attrPs.setString(++idx, rs.getObject(columnName) != null ? rs.getObject(columnName).toString() : null);
					attrPs.setTimestamp(++idx, Convert.getCurrentTimestamp());
					attrPs.addBatch();
				}
			}
		} catch (Exception e) {
			throw new Exception("Error processing " + fieldPrefix + " attributes.", e);
		}
	}
	
	/**
	 * Takes a delimited list and splits to separate fields to be individually added to an attribute table
	 * 
	 * @param columnsToSplit
	 * @param columnName
	 * @param attrPs
	 * @param sourceRs
	 * @param fieldPrefix
	 * @throws SQLException 
	 */
	private void splitAttributeColumn(Map<String, String> columnsToSplit, String columnName, PreparedStatement attrPs,
			ResultSet sourceRs, String fieldPrefix) throws SQLException {

		if (!columnsToSplit.containsKey(columnName))
			return;
		
		String[] data = sourceRs.getString(columnName).split(columnsToSplit.get(columnName));
		int position = 0;
		
		for (String datum : data) {
			if (StringUtil.isEmpty(datum)) continue;
			int idx = 0;
			attrPs.setString(++idx, new UUIDGenerator().getUUID());
			attrPs.setString(++idx, sourceRs.getObject("id").toString());
			attrPs.setString(++idx, fieldPrefix + "_" + columnName.toUpperCase() + "_" + position++);
			attrPs.setString(++idx, datum);
			attrPs.setTimestamp(++idx, Convert.getCurrentTimestamp());
			attrPs.addBatch();
		}
	}
	
	/**
	 * Migrate residence data
	 * @return
	 * @throws Exception 
	 */
	protected void migrateResidences() throws Exception {
		log.info("Migrating Residence Table");
		
		// move base residence data to new data model
		StringBuilder residenceSql = new StringBuilder(500);
		residenceSql.append("insert into custom.rezdox_residence (residence_id, residence_nm, address_txt, city_nm, ");
		residenceSql.append("state_cd, country_cd, zip_cd, latitude_no, longitude_no, profile_pic_pth, for_sale_dt, privacy_flg, ");
		residenceSql.append("create_dt) select cast(id as varchar), name, address1, city, state, 'US', zip, lat, lng, profilepic, ");
		residenceSql.append("case when forsale != 0 then getdate() else null end, privacy, getdate() from rezdox.residence_tbl ");
		executeSimpleMapping(residenceSql, "base residence");
		
		log.info("Migrating Residence Photos");

		// migrate residence photos
		StringBuilder photosSql = new StringBuilder(150);
		photosSql.append("insert into custom.rezdox_photo (photo_id, residence_id, photo_nm, order_no, ");
		photosSql.append("create_dt) values (?,?,?,?,?)");

		StringBuilder legacyPhotoSql = new StringBuilder(50);
		legacyPhotoSql.append("select cast(id as varchar) as residence_id, photos from rezdox.residence_tbl where photos != '0'");

		try (PreparedStatement photoPs = dbConn.prepareStatement(photosSql.toString());) {
			buildPhotoPs(photoPs, legacyPhotoSql, "residence_id");
			photoPs.executeBatch();
		} catch (Exception sqle) {
			log.error("Error migrating residence photos. ", sqle);
		}
	}
	
	/**
	 * Migrate residence members
	 * @return
	 * @throws Exception 
	 */
	protected void migrateResidenceMembers() {
		log.info("Migrating Residence Members");
		
		// move residence owner data to new data model
		StringBuilder sql = new StringBuilder(400);
		sql.append("insert into custom.rezdox_residence_member_xr (residence_member_xr_id, member_id, residence_id, ");
		sql.append("status_flg, create_dt) select replace(newid(),'-',''), cast(mem_id as varchar), cast(res_id as varchar), ");
		sql.append("permission, getdate() from rezdox.mem_res_tbl mr inner join rezdox.residence_tbl r on mr.res_id = r.id ");
		sql.append("inner join rezdox.member_tbl m on mr.mem_id = m.id ");
		executeSimpleMapping(sql, "residence member");
	}
	
	/**
	 * Migrate residence attribute data
	 * DB Columns in old model become form fields in the new model
	 * 
	 * @return
	 * @throws Exception 
	 */
	protected void migrateResidenceAttributes() throws Exception {
		log.info("Migrating Residence Attributes");
		
		// Migrate residence attributes
		StringBuilder attrSql = new StringBuilder(150);
		attrSql.append("insert into custom.rezdox_residence_attribute (attribute_id, residence_id, ");
		attrSql.append("form_field_id, value_txt, create_dt) values (?,?,?,?,?) ");
		
		StringBuilder legacySql = new StringBuilder(100);
		legacySql.append("select ri.* from rezdox.residence_info_tbl ri inner join rezdox.residence_tbl r on ri.id = r.id");
		
		Map<String, String> columnsToSplit = new HashMap<>();

		try (PreparedStatement attrPs = dbConn.prepareStatement(attrSql.toString());) {
			buildAttributesPs(attrPs, legacySql, columnsToSplit, "RESIDENCE");
			attrPs.executeBatch();
		} catch (Exception sqle) {
			log.error("Error migrating residence attributes. ", sqle);
		}
	}
	
	/**
	 * Migrate room category/type data
	 */
	protected void migrateRoomTypes() {
		log.info("Migrating Room Categories");
		StringBuilder sqlCat = new StringBuilder(175);
		sqlCat.append("insert into custom.rezdox_room_category (room_category_cd, category_nm, create_dt) ");
		sqlCat.append("select distinct(upper(type)), type, getdate() from rezdox.roomlist_tbl ");
		executeSimpleMapping(sqlCat, "room category");
		
		log.info("Migrating Room Types");
		StringBuilder sqlType = new StringBuilder(200);
		sqlType.append("insert into custom.rezdox_room_type (room_type_cd, room_category_cd, type_nm, create_dt) ");
		sqlType.append("select cast(id as varchar), upper(type), name, getdate() from rezdox.roomlist_tbl ");
		executeSimpleMapping(sqlType, "room type");
	}
	
	/**
	 * Migrates rooms from project history & treasures
	 */
	protected void migrateResidenceRooms() {
		log.info("Migrating Residence Rooms");
		StringBuilder sql = new StringBuilder(750);
		sql.append("insert into custom.rezdox_room (room_id, residence_id, room_type_cd, create_dt) ");
		sql.append("select replace(newid(),'-',''), residence_id, room_type_cd, getdate() from ( ");
		sql.append("select cast(r.id as varchar) as residence_id, cast(rl.id as varchar) as room_type_cd ");
		sql.append("from rezdox.history_tbl h inner join rezdox.res_his_tbl rh on h.id = rh.his_id ");
		sql.append("inner join rezdox.residence_tbl r on rh.res_id = r.id ");
		sql.append("inner join rezdox.roomlist_tbl rl on h.room = rl.name ");
		sql.append("group by r.id, rl.id ");
		sql.append("union ");
		sql.append("select cast(r.res_id as varchar) as residence_id, cast(rl.id as varchar) as room_type_cd ");
		sql.append("from rezdox.treasure_tbl t inner join rezdox.mem_tre_tbl mt on t.id = mt.tre_id ");
		sql.append("inner join (select mr.mem_id, mr.res_id, res.name from rezdox.residence_tbl res inner join rezdox.mem_res_tbl mr on res.id = mr.res_id) r on t.residence = r.name and mt.mem_id = r.mem_id ");
		sql.append("inner join rezdox.roomlist_tbl rl on t.room = rl.name ");
		sql.append("group by r.res_id, rl.id ");
		sql.append(") as rooms ");
		executeSimpleMapping(sql, "residence room");
	}
	
	/**
	 * Migrate business category data
	 */
	protected void migrateBusinessCategories() {
		log.info("Migrating Business Categories");
		StringBuilder catSql = new StringBuilder(225);
		catSql.append("insert into custom.rezdox_business_category (business_category_cd, category_nm, order_no, create_dt) ");
		catSql.append("select concat('CAT_', cast(id as varchar)), disp_name, sort, getdate() from rezdox.business_cat_tbl ");
		executeSimpleMapping(catSql, "business category");
		
		log.info("Migrating Business Subcategories as Categories");
		StringBuilder subCatSql = new StringBuilder(300);
		subCatSql.append("insert into custom.rezdox_business_category (business_category_cd, parent_cd, category_nm, create_dt) ");
		subCatSql.append("select concat('SUB_', cast(id as varchar)), concat('CAT_', cast(bus_cat_id as varchar)), name, ");
		subCatSql.append("getdate() from rezdox.business_subcat_tbl ");
		executeSimpleMapping(subCatSql, "business subcategory");
	}
		
	/**
	 * Migrate business data
	 */
	protected void migrateBusinesses() {
		log.info("Migrating Businesses");
		StringBuilder businessSql = new StringBuilder(550);
		businessSql.append("insert into custom.rezdox_business (business_id, business_nm, address_txt, address2_txt, city_nm, state_cd, ");
		businessSql.append("zip_cd, country_cd, latitude_no, longitude_no, main_phone_txt, alt_phone_txt, email_address_txt, website_url, ");
		businessSql.append("photo_url, ad_file_url, privacy_flg, create_dt) ");
		businessSql.append("select cast(id as varchar), name, address1, address2, city, state, zip, 'US', lat, lng, phone, phone2, ");
		businessSql.append("email, website, profilepic, ad, privacy, getdate() from rezdox.business_tbl ");
		executeSimpleMapping(businessSql, "business");
		
		log.info("Migrating Category XR");
		
		// Single business categories in old data model become possiblity for many in new model
		StringBuilder businessCatSql = new StringBuilder(250);
		businessCatSql.append("insert into custom.rezdox_business_category_xr (business_category_xr_id, business_id, business_category_cd, create_dt) ");
		businessCatSql.append("select replace(newid(),'-',''), cast(id as varchar), concat('SUB_', cast(subtype as varchar)), getdate() from rezdox.business_tbl ");
		executeSimpleMapping(businessCatSql, "business category xr");
		
		log.info("Migrating Business Photos");

		// Migrate pipe delimeted photos from source business record
		StringBuilder photosSql = new StringBuilder(150);
		photosSql.append("insert into custom.rezdox_photo (photo_id, business_id, photo_nm, order_no, ");
		photosSql.append("create_dt) values (?,?,?,?,?)");

		StringBuilder legacyPhotoSql = new StringBuilder(50);
		legacyPhotoSql.append("select cast(id as varchar) as business_id, photos from rezdox.business_tbl where photos != '0'");

		try (PreparedStatement photoPs = dbConn.prepareStatement(photosSql.toString());) {
			buildPhotoPs(photoPs, legacyPhotoSql, BUSINESS_ID);
			photoPs.executeBatch();
		} catch (Exception sqle) {
			log.error("Error migrating business photos. ", sqle);
		}
	}	
	
	/**
	 * Migrate business attribute data
	 * DB Columns in old model become form fields in the new model.
	 * Some columns are split on pipes to additional separate fields.
	 * 
	 * @return
	 * @throws Exception 
	 */
	protected void migrateBusinessAttributes() throws Exception {
		log.info("Migrating Business Attributes");
		
		// Migrate business attributes
		StringBuilder attrSql = new StringBuilder(150);
		attrSql.append("insert into custom.rezdox_business_attribute (attribute_id, business_id, ");
		attrSql.append("form_field_id, value_txt, create_dt) values (?,?,?,?,?) ");

		StringBuilder legacySql = new StringBuilder(100);
		legacySql.append("select bi.* from rezdox.business_info_tbl bi inner join rezdox.business_tbl b on bi.id = b.id");

		Map<String, String> columnsToSplit = new HashMap<>();
		columnsToSplit.put("trade_areas", TWO_PIPES);
		columnsToSplit.put("hours_ops", FOUR_PIPES);
		columnsToSplit.put("socialmedia", TWO_PIPES);
		columnsToSplit.put("payments", TWO_PIPES);
		columnsToSplit.put("specialties", TWO_PIPES);
		columnsToSplit.put("programs", TWO_PIPES);
		columnsToSplit.put("associations", TWO_PIPES);
		columnsToSplit.put("accolades", TWO_PIPES);
		columnsToSplit.put("style_ops", TWO_PIPES);

		try (PreparedStatement attrPs = dbConn.prepareStatement(attrSql.toString());) {
			buildAttributesPs(attrPs, legacySql, columnsToSplit, "BUSINESS");
			attrPs.executeBatch();
		} catch (Exception sqle) {
			log.error("Error migrating business attributes. ", sqle);
		}
	}
	
	/**
	 * Migrate business reviews from members
	 */
	protected void migrateBusinessReviews() {
		log.info("Migrating Business Reviews");
		StringBuilder sql = new StringBuilder(350);
		sql.append("insert into custom.rezdox_member_business_review (business_review_id, member_id, business_id, rating_no, review_txt, create_dt) ");
		sql.append("select cast(id as varchar), cast(mem_id as varchar), cast(bus_id as varchar), rating, review, datetime ");
		sql.append("from rezdox.review_tbl r inner join rezdox.bus_mem_rev_tbl bmr on r.id = bmr.rev_id ");
		executeSimpleMapping(sql, "business review");
	}
	
	/**
	 * Migrate business member xr (business owners)
	 */
	protected void migrateBusinessMembers() {
		log.info("Migrating Business Members");
		StringBuilder sql = new StringBuilder(400);
		sql.append("insert into custom.rezdox_business_member_xr (business_member_xr_id, member_id, business_id, ");
		sql.append("status_flg, create_dt) select replace(newid(),'-',''), cast(mem_id as varchar), cast(bus_id as varchar), ");
		sql.append("permission, getdate() from rezdox.mem_bus_tbl mb inner join rezdox.business_tbl b on mb.bus_id = b.id ");
		sql.append("inner join rezdox.member_tbl m on mb.mem_id = m.id ");
		executeSimpleMapping(sql, "business member");
	}
	
	/**
	 * Migrates messages from one member to another
	 */
	protected void migrateMemberMessages() {
		log.info("Migrating Member Messages");

		StringBuilder legacySql = new StringBuilder(200);
		legacySql.append("select cast(id as varchar), content, pro1, pro2 from rezdox.messages_tbl m ");
		legacySql.append("inner join rezdox.mes_pro_tbl mp on m.id = mp.mes_id and content like concat(pro1, '||', '%')");
		
		StringBuilder messageSql = new StringBuilder(100);
		messageSql.append("insert into custom.rezdox_message (message_id, message_txt, approval_flg, create_dt) ");
		messageSql.append("values (?,?,?,?) ");
		
		StringBuilder membMessageSql = new StringBuilder(175);
		membMessageSql.append("insert into custom.rezdox_member_message (member_message_id, sndr_member_id, rcpt_member_id, ");
		membMessageSql.append("message_id, create_dt) values(?,?,?,?,?) ");
		
		try (PreparedStatement legacyPs = dbConn.prepareStatement(legacySql.toString());
				PreparedStatement messagePs = dbConn.prepareStatement(messageSql.toString());
				PreparedStatement membMessagePs = dbConn.prepareStatement(membMessageSql.toString());) {
			
			ResultSet legacyRs = legacyPs.executeQuery();
			
			while (legacyRs.next()) {
				String[] data = legacyRs.getString("content").split(FOUR_PIPES);
				String baseId = legacyRs.getString("id");
				String member1 = legacyRs.getString("pro1").substring(1);
				String member2 = legacyRs.getString("pro2").substring(1);
				int msgIndex = 0;
				
				for (String datum : data) {
					String messageId = baseId + "_" + msgIndex++;
					String[] messageData = datum.split(TWO_PIPES);
					
					String fromId = messageData[0].substring(1);
					String toId = fromId.equals(member1) ? member2 : member1;

					int idx = 0;
					messagePs.setString(++idx, messageId);
					messagePs.setString(++idx, messageData[2]);
					messagePs.setInt(++idx, 1);
					messagePs.setTimestamp(++idx, Convert.formatTimestamp(Convert.DATE_TIME_DASH_PATTERN, messageData[1]));
					messagePs.addBatch();
					
					idx = 0;
					membMessagePs.setString(++idx, messageId);
					membMessagePs.setString(++idx, fromId);
					membMessagePs.setString(++idx, toId);
					membMessagePs.setString(++idx, messageId);
					membMessagePs.setTimestamp(++idx, Convert.formatTimestamp(Convert.DATE_TIME_DASH_PATTERN, messageData[1]));
					membMessagePs.addBatch();
				}
			}
			
			messagePs.executeBatch();
			membMessagePs.executeBatch();
		} catch (Exception sqle) {
			log.error("Error migrating member messages. ", sqle);
		}
	}

	/**
	 * Migrates system notifications
	 */
	protected void migrateNotifications() {
		log.info("Migrating System Notifications");

		StringBuilder legacySql = new StringBuilder(150);
		legacySql.append("select cast(id as varchar), pro2, message from rezdox.notification_tbl ");
		legacySql.append("where message != '' and message not like '%||%' ");
		
		StringBuilder messageSql = new StringBuilder(125);
		messageSql.append("insert into custom.rezdox_message (message_id, message_txt, approval_flg, create_dt) ");
		messageSql.append("values (?,?,?,?) ");
		
		StringBuilder notificationSql = new StringBuilder(150);
		notificationSql.append("insert into custom.rezdox_notification (notification_id, member_id, business_id, ");
		notificationSql.append("message_id, create_dt) values(?,?,?,?,?) ");
		
		try (PreparedStatement legacyPs = dbConn.prepareStatement(legacySql.toString());
				PreparedStatement messagePs = dbConn.prepareStatement(messageSql.toString());
				PreparedStatement notificationPs = dbConn.prepareStatement(notificationSql.toString());) {
			
			ResultSet legacyRs = legacyPs.executeQuery();
			
			while (legacyRs.next()) {
				String pro2 = legacyRs.getString("pro2");
				
				int idx = 0;
				messagePs.setString(++idx, "NOT_" + legacyRs.getString("id"));
				messagePs.setString(++idx, legacyRs.getString("message"));
				messagePs.setInt(++idx, 1);
				messagePs.setTimestamp(++idx, Convert.getCurrentTimestamp());
				messagePs.addBatch();
				
				idx = 0;
				notificationPs.setString(++idx, "NOT_" + legacyRs.getString("id"));
				notificationPs.setString(++idx, pro2.startsWith("m") ? pro2.substring(1) : null);
				notificationPs.setString(++idx, pro2.startsWith("b") ? pro2.substring(1) : null);
				notificationPs.setString(++idx, "NOT_" + legacyRs.getString("id"));
				notificationPs.setTimestamp(++idx, Convert.getCurrentTimestamp());
				notificationPs.addBatch();
			}
			
			messagePs.executeBatch();
			notificationPs.executeBatch();
		} catch (Exception sqle) {
			log.error("Error migrating member messages. ", sqle);
		}
	}
	
	/**
	 * Migrates member/member, member/business, & business/business connections
	 */
	protected void migrateConnections() {
		// Get lists for comparison of connections to validate referential integrity
		List<String> memberIds = getMemberList();
		List<String> businessIds = getBusinessList();
		
		log.info("Migrating Connections");

		StringBuilder memberSql = new StringBuilder(70);
		memberSql.append("select cast(id as varchar) as member_id, mem_con, bus_con from rezdox.member_tbl order by id");
		
		StringBuilder businessSql = new StringBuilder(70);
		businessSql.append("select cast(id as varchar) as business_id, bus_con from rezdox.business_tbl order by id");
		
		StringBuilder connectionSql = new StringBuilder(200);
		connectionSql.append("insert into custom.rezdox_connection (connection_id, sndr_member_id, rcpt_member_id, ");
		connectionSql.append("sndr_business_id, rcpt_business_id, approved_flg, create_dt) values (?,?,?,?,?,?,?)");
		
		try (PreparedStatement memberPs = dbConn.prepareStatement(memberSql.toString());
				PreparedStatement businessPs = dbConn.prepareStatement(businessSql.toString());
				PreparedStatement connectionPs = dbConn.prepareStatement(connectionSql.toString());) {
			
			// Migrate Member Connections
			ResultSet memberRs = memberPs.executeQuery();
			while (memberRs.next()) {
				String[] memMemConn = memberRs.getString("mem_con").split(FOUR_PIPES);
				String[] memBusConn = memberRs.getString("bus_con").split(FOUR_PIPES);
				String memberId = memberRs.getString("member_id");
				
				processMemToMemConnections(memberId, memMemConn, memberIds, connectionPs);
				processMemToBusConnections(memberId, memBusConn, businessIds, connectionPs);
			}
			
			// Migrate Business Connections
			ResultSet businessRs = businessPs.executeQuery();
			while (businessRs.next()) {
				String[] busBusConn = businessRs.getString("bus_con").split(FOUR_PIPES);
				String businessId = businessRs.getString(BUSINESS_ID);
				
				processBusToBusConnections(businessId, busBusConn, businessIds, connectionPs);
			}
			
			connectionPs.executeBatch();
		} catch (Exception sqle) {
			log.error("Error migrating connections. ", sqle);
		}
	}
	
	/**
	 * Member to member connections need to be de-duped... connections from member1 to member2, also exist
	 * as connections from member2 to member1 in the legacy data. This is not necessary.
	 * 
	 * @param memberId - the member the connections belong to
	 * @param memMemConn - the connections from this member to other members
	 * @param memberIds - valid members in the system
	 * @param connectionPs - the prepared statement we are adding to
	 * @throws SQLException
	 */
	private void processMemToMemConnections(String memberId, String[] memMemConn, List<String> memberIds, PreparedStatement connectionPs) throws SQLException {
		for (String connectionId : memMemConn) {
			if (!memberIds.contains(connectionId)) continue;
			
			// De-dupe the data
			if (Convert.formatInteger(connectionId) > Convert.formatInteger(memberId)) {
				addConnection(connectionPs, memberId, connectionId, null, null);
			}
		}
	}
	
	/**
	 * To de-dupe member to business connections, we won't migrate business to member connections
	 * found in the business records. So there is no de-duping going on here. Simple.
	 * 
	 * @param memberId - the member the connections belong to
	 * @param memBusConn - the connections from this member to businesses
	 * @param businessIds - valid businesses in the system
	 * @param connectionPs - the prepared statement we are adding to
	 * @throws SQLException
	 */
	private void processMemToBusConnections(String memberId, String[] memBusConn, List<String> businessIds, PreparedStatement connectionPs) throws SQLException {
		for (String connectionId : memBusConn) {
			if (!businessIds.contains(connectionId)) continue;
			addConnection(connectionPs, memberId, null, null, connectionId);
		}
	}
	
	/**
	 * Business to business connections need to be de-duped... connections from business1 to business2, also exist
	 * as connections from business2 to business1 in the legacy data. This is not necessary.
	 * 
	 * @param businessId - the business the connections belong to
	 * @param busBusConn - the connections from this business to other businesses
	 * @param businessIds - valid businesses in the system
	 * @param connectionPs - the prepared statement we are adding to
	 * @throws SQLException
	 */
	private void processBusToBusConnections(String businessId, String[] busBusConn, List<String> businessIds, PreparedStatement connectionPs) throws SQLException {
		for (String connectionId : busBusConn) {
			if (!businessIds.contains(connectionId)) continue;
			
			// De-dupe the data
			if (Convert.formatInteger(connectionId) > Convert.formatInteger(businessId)) {
				addConnection(connectionPs, null, null, businessId, connectionId);
			}
		}
	}
	
	/**
	 * Helper to update a prepared statment to migrate a Connection
	 * 
	 * @param ps
	 * @param fromMember
	 * @param toMember
	 * @param fromBusiness
	 * @param toBusiness
	 * @throws SQLException
	 */
	private void addConnection (PreparedStatement ps, String fromMember, String toMember, String fromBusiness, String toBusiness) throws SQLException {
		int idx = 0;
		ps.setString(++idx, new UUIDGenerator().getUUID());
		ps.setString(++idx, fromMember);
		ps.setString(++idx, toMember);
		ps.setString(++idx, fromBusiness);
		ps.setString(++idx, toBusiness);
		ps.setInt(++idx, 1);
		ps.setTimestamp(++idx, Convert.getCurrentTimestamp());
		ps.addBatch();
	}
	
	/**
	 * Get's list of member ids, to verify referential integrity where it didn't previously exists
	 * 
	 * @return
	 */
	private List<String> getMemberList() {
		log.info("Getting List of Member Ids");
		List<String> memberIds = new ArrayList<>();
		
		StringBuilder sql = new StringBuilder(50);
		sql.append("select member_id from custom.rezdox_member");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				memberIds.add(rs.getString("member_id"));
			}
		} catch (Exception sqle) {
			log.error("Error getting member id list. ", sqle);
		}
		
		return memberIds;
	}
	
	/**
	 * Get's list of business ids, to verify referential integrity where it didn't previously exists
	 * 
	 * @return
	 */
	private List<String> getBusinessList() {
		log.info("Getting List of Business Ids");
		List<String> businessIds = new ArrayList<>();
		
		StringBuilder sql = new StringBuilder(50);
		sql.append("select business_id from custom.rezdox_business");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				businessIds.add(rs.getString(BUSINESS_ID));
			}
		} catch (Exception sqle) {
			log.error("Error getting business id list. ", sqle);
		}
		
		return businessIds;
	}
	
	/**
	 * Creates the default memberships available
	 */
	protected void createMemberships() {
		log.info("Adding Memberships");
		StringBuilder sql = new StringBuilder(600);
		sql.append("insert into custom.rezdox_membership (membership_id, membership_nm, group_cd, status_flg, cost_no, qty_no, new_mbr_dflt_flg, create_dt) ");
		sql.append("values (replace(newid(),'-',''), 'Residence', 'HO', 1, 9.99, 1, 1, getdate()), ");
		sql.append("(replace(newid(),'-',''), 'Business', 'BU', 1, 79.99, 1, 1, getdate()), ");
		sql.append("(replace(newid(),'-',''), '100 Connections', 'CO', 1, 99.99, 100, 1, getdate()), ");
		sql.append("(replace(newid(),'-',''), '200 Connections', 'CO', 1, 179.99, 200, 0, getdate()), ");
		sql.append("(replace(newid(),'-',''), '300 Connections', 'CO', 1, 239.99, 300, 0, getdate()) ");
		executeSimpleMapping(sql, "membership");
	}
	
	/**
	 * Creates the default promotions available
	 */
	protected void createPromotions() {
		log.info("Adding Promotions");
		StringBuilder sql = new StringBuilder(450);
		sql.append("insert into custom.rezdox_promotion (promotion_id, promotion_nm, promotion_cd, description_txt, ");
		sql.append("terms_txt, discount_pct_no, status_flg, start_dt, create_dt) ");
		sql.append("values (replace(newid(),'-',''), 'Welcome To RezDox', 'REZDOXFIRST', 'Use the promo code REZDOXFIRST for your FREE RezDox Account.', ");
		sql.append("'Provides free accounts for all RezDox HomeOwner and Business members.', 1.00, 1, getdate(), getdate()) ");
		executeSimpleMapping(sql, "promotion");
	}
	
	/**
	 * Creates the default associations between promotions and memberships
	 */
	protected void createMembershipPromotions() {
		log.info("Adding Membership Promotions");
		StringBuilder sql = new StringBuilder(400);
		sql.append("insert into custom.rezdox_membership_promotion (membership_promotion_id, promotion_id, membership_id, create_dt) ");
		sql.append("select replace(newid(),'-',''), promotion_id, membership_id, getdate() ");
		sql.append("from custom.rezdox_promotion p inner join custom.rezdox_membership m on 1=1 ");
		sql.append("where group_cd in ('HO', 'BU') or (group_cd = 'CO' and qty_no = 100) ");
		executeSimpleMapping(sql, "membership promotion");
	}
	
	/**
	 * Creates the default payment types
	 */
	protected void createPaymentTypes() {
		log.info("Adding Payment Types");
		StringBuilder sql = new StringBuilder(150);
		sql.append("insert into custom.rezdox_payment_type (payment_type_id, type_nm, create_dt) ");
		sql.append("values (replace(newid(),'-',''), 'PayPal', getdate()) ");
		executeSimpleMapping(sql, "payment type");
	}
	
	/**
	 * Migrate member subscriptions based on the provided rules.
	 * Legacy monthly/yearly memberships are no longer applicable.
	 * We are moving them to the new single purchase memberships.
	 * 
	 * Rules:
	 * If they have a residence, they get one residence purchase.
	 * If they have a business, they get one business purchase.
	 * Everyone gets 100 connections.
	 * 
	 * There are some exceptions where a few select people get more residences/businesses.
	 */
	protected void migrateSubscriptions() {
		String insert = "insert into custom.rezdox_subscription (subscription_id, member_id, membership_id, promotion_id, cost_no, discount_no, qty_no, create_dt) ";
		String select = "select replace(newid(),'-',''), member_id, m.membership_id, p.promotion_id, cost_no, discount_pct_no * cost_no * -1, qty_no, getdate() ";
		String joinMp = "inner join custom.rezdox_membership_promotion mp on m.membership_id = mp.membership_id ";
		String joinP = "inner join custom.rezdox_promotion p on mp.promotion_id = p.promotion_id and p.promotion_cd = 'REZDOXFIRST' ";
		String group = "group by member_id, m.membership_id, p.promotion_id ";
		
		log.info("Adding Member Residence Subscriptions");
		StringBuilder resSql = new StringBuilder(700);
		resSql.append(insert).append(select);
		resSql.append("from custom.rezdox_residence_member_xr rm inner join custom.rezdox_membership m on m.group_cd = 'HO' ");
		resSql.append(joinMp).append(joinP).append(group);
		executeSimpleMapping(resSql, "member residence subscription");
		
		log.info("Adding Member Business Subscriptions");
		StringBuilder busSql = new StringBuilder(700);
		busSql.append(insert).append(select);
		busSql.append("from custom.rezdox_business_member_xr bm inner join custom.rezdox_membership m on m.group_cd = 'BU' ");
		busSql.append(joinMp).append(joinP).append(group);
		executeSimpleMapping(busSql, "member business subscription");
		
		log.info("Adding Member Connection Subscriptions");
		StringBuilder conSql = new StringBuilder(600);
		conSql.append(insert).append(select);
		conSql.append("from custom.rezdox_member inner join custom.rezdox_membership m on m.group_cd = 'CO' and qty_no = 100 ");
		conSql.append(joinMp).append(joinP);
		executeSimpleMapping(conSql, "member connection subscription");
	}
	
	/**
	 * Migrates Project Categories and Types
	 */
	protected void migrateProjectCategoryTypes() {
		log.info("Migrating Project Categories");
		StringBuilder catSql = new StringBuilder(180);
		catSql.append("insert into custom.rezdox_project_category (project_category_cd, category_nm, create_dt) ");
		catSql.append("select upper(replace(hcat, ' ', '_')), hcat, getdate() from rezdox.history_tbl group by hcat");
		executeSimpleMapping(catSql, "project category");
		
		log.info("Migrating Project Types");
		StringBuilder typeSql = new StringBuilder(150);
		typeSql.append("insert into custom.rezdox_project_type (project_type_cd, type_nm, create_dt) ");
		typeSql.append("select cast(id as varchar), htype, getdate() from rezdox.htypelist_tbl");
		executeSimpleMapping(typeSql, "project type");
	}
	
	/**
	 * Migrates Project History
	 */
	protected void migrateProjectHistory() {
		log.info("Migrating Project History");
		StringBuilder sql = new StringBuilder(850);
		sql.append("insert into custom.rezdox_project (project_id, residence_id, room_id, business_id, project_category_cd, project_type_cd, ");
		sql.append("project_nm, labor_no, total_no, residence_view_flg, business_view_flg, create_dt) ");
		sql.append("select cast(h.id as varchar), cast(rh.res_id as varchar), rm.room_id, b.id, upper(replace(hcat, ' ', '_')), cast(ht.id as varchar), ");
		sql.append("summary, labor, total, 1, 1, date ");
		sql.append("from rezdox.history_tbl h inner join rezdox.res_his_tbl rh on h.id = rh.his_id ");
		sql.append("inner join rezdox.htypelist_tbl ht on h.htype = ht.htype ");
		sql.append("inner join rezdox.residence_tbl r on rh.res_id = r.id ");
		sql.append("left join rezdox.roomlist_tbl rl on h.room = rl.name ");
		sql.append("left join custom.rezdox_room rm on cast(rh.res_id as varchar) = rm.residence_id and cast(rl.id as varchar) = rm.room_type_cd ");
		sql.append("left join rezdox.business_tbl b on h.provider_id = b.id ");
		executeSimpleMapping(sql, "project history");
	}
	
	/**
	 * Migrates Project Attribute Data
	 */
	protected void migrateProjectAttributes() {
		log.info("Migrating Project Attributes");
		
		// Migrate project attributes
		StringBuilder attrSql = new StringBuilder(150);
		attrSql.append("insert into custom.rezdox_project_attribute (attribute_id, project_id, ");
		attrSql.append("form_field_id, value_txt, create_dt) values (?,?,?,?,?)");
		
		StringBuilder legacySql = new StringBuilder(100);
		legacySql.append("select id, notes, provider, phone, email, res_owner from rezdox.history_tbl h ");
		legacySql.append("where cast(h.id as varchar) in (select project_id from custom.rezdox_project) ");
		
		Map<String, String> columnsToSplit = new HashMap<>();

		try (PreparedStatement attrPs = dbConn.prepareStatement(attrSql.toString());) {
			buildAttributesPs(attrPs, legacySql, columnsToSplit, "PROJECT");
			attrPs.executeBatch();
		} catch (Exception sqle) {
			log.error("Error migrating project attributes. ", sqle);
		}
	}
	
	/**
	 * Migrates project photos and documents
	 */
	protected void migrateProjectPhotosDocuments() {
		log.info("Migrating Project Photos");

		// Migrate project photos
		StringBuilder photosSql = new StringBuilder(150);
		photosSql.append("insert into custom.rezdox_photo (photo_id, project_id, photo_nm, order_no, create_dt) ");
		photosSql.append("values (?,?,?,?,?)");

		StringBuilder legacyPhotoSql = new StringBuilder(200);
		legacyPhotoSql.append("select cast(id as varchar) as project_id, images as photos from rezdox.history_tbl h ");
		legacyPhotoSql.append("where cast(h.id as varchar) in (select project_id from custom.rezdox_project) and images != '' ");

		try (PreparedStatement photoPs = dbConn.prepareStatement(photosSql.toString());) {
			buildPhotoPs(photoPs, legacyPhotoSql, "project_id");
			photoPs.executeBatch();
		} catch (Exception sqle) {
			log.error("Error migrating project photos. ", sqle);
		}

		log.info("Migrating Project Files");

		// Migrate project files
		StringBuilder documentSql = new StringBuilder(150);
		documentSql.append("insert into custom.rezdox_document (document_id, project_id, document_nm, create_dt) ");
		documentSql.append("values (?,?,?,?)");

		StringBuilder legacyFileSql = new StringBuilder(200);
		legacyFileSql.append("select cast(id as varchar) as project_id, files from rezdox.history_tbl h ");
		legacyFileSql.append("where cast(h.id as varchar) in (select project_id from custom.rezdox_project) and files != '' ");

		try (PreparedStatement documentPs = dbConn.prepareStatement(documentSql.toString());) {
			buildDocumentPs(documentPs, legacyFileSql, "project_id");
			documentPs.executeBatch();
		} catch (Exception sqle) {
			log.error("Error migrating project documents. ", sqle);
		}
	}
	
	/**
	 * Migrates Project Materials/Products
	 */
	protected void migrateProjectMaterials() {
		log.info("Migrating Project Materials");
		StringBuilder sql = new StringBuilder(400);
		sql.append("insert into custom.rezdox_project_material(project_material_id, project_id, material_nm, quantity_no, cost_no, create_dt) ");
		sql.append("select cast(p.id as varchar), cast(hp.his_id as varchar), pdesc, pquantity, pcost, getdate() ");
		sql.append("from rezdox.products_tbl p inner join rezdox.his_prod_tbl hp on p.id = hp.prod_id ");
		sql.append("where cast(hp.his_id as varchar) in (select project_id from custom.rezdox_project) ");
		executeSimpleMapping(sql, "project material");
	}
	
	/**
	 * Migrates Project Material Attributes
	 */
	protected void migrateProjectMaterialAttributes() {
		log.info("Migrating Project Material Attributes");
		
		// Migrate project material attributes
		StringBuilder attrSql = new StringBuilder(150);
		attrSql.append("insert into custom.rezdox_project_material_attribute (attribute_id, project_material_id, form_field_id, ");
		attrSql.append("value_txt, create_dt) values (?,?,?,?,?)");
		
		StringBuilder legacySql = new StringBuilder(250);
		legacySql.append("select id, pretailer, rwebsite, pbrand, pwebsite, pmodel, pserialno, psize, pcolor, pwarranty, pmeasure ");
		legacySql.append("from rezdox.products_tbl p ");
		legacySql.append("where cast(id as varchar) in (select project_material_id from custom.rezdox_project_material) ");
		
		Map<String, String> columnsToSplit = new HashMap<>();

		try (PreparedStatement attrPs = dbConn.prepareStatement(attrSql.toString());) {
			buildAttributesPs(attrPs, legacySql, columnsToSplit, "PROJECT_MATERIAL");
			attrPs.executeBatch();
		} catch (Exception sqle) {
			log.error("Error migrating project material attributes. ", sqle);
		}
	}
	
	/**
	 * Migrates the treasure box item categories
	 */
	protected void migrateTreasureCategories() {
		log.info("Migrating Treasure Categories");
		StringBuilder sql = new StringBuilder(200);
		sql.append("insert into rezdox_treasure_category (treasure_category_cd, category_nm, create_dt) ");
		sql.append("select cast(id as varchar), name, getdate() from rezdox.categorylist_tbl");
		executeSimpleMapping(sql, "treasure category");
	}
	
	/**
	 * Migrates treasure box items
	 */
	protected void migrateTreasureItems() {
		log.info("Migrating Treasure Items");
		StringBuilder sql = new StringBuilder(1000);
		sql.append("insert into custom.rezdox_treasure_item(treasure_item_id, owner_member_id, residence_id, room_id, ");
		sql.append("treasure_category_cd, item_nm, valuation_no, quantity_no, create_dt) ");
		sql.append("select cast(t.id as varchar), cast(mt.mem_id as varchar), cast(r.res_id as varchar), rm.room_id, ");
		sql.append("cast(c.id as varchar), description, cost, quantity, date ");
		sql.append("from rezdox.treasure_tbl t inner join rezdox.mem_tre_tbl mt on t.id = mt.tre_id ");
		sql.append("left join rezdox.categorylist_tbl c on t.category = c.name ");
		sql.append("left join (select mr.mem_id, mr.res_id, (case when res_id = 114 then 'Home114' else res.name end) as name from rezdox.residence_tbl res inner join rezdox.mem_res_tbl mr on res.id = mr.res_id) r on t.residence = r.name and mt.mem_id = r.mem_id ");
		sql.append("left join rezdox.roomlist_tbl rl on t.room = rl.name and r.res_id is not null ");
		sql.append("left join custom.rezdox_room rm on cast(r.res_id as varchar) = rm.residence_id and cast(rl.id as varchar) = rm.room_type_cd ");
		executeSimpleMapping(sql, "treasure item");
	}
	
	/**
	 * Migrates treasure item attributes
	 */
	protected void migrateTreasureItemAttributes() {
		log.info("Migrating Treasure Item Attributes");
		
		// Migrate treasure item attributes
		StringBuilder attrSql = new StringBuilder(150);
		attrSql.append("insert into custom.rezdox_treasure_item_attribute (attribute_id, treasure_item_id, form_field_id, ");
		attrSql.append("value_txt, create_dt) values (?,?,?,?,?)");
		
		StringBuilder legacySql = new StringBuilder(175);
		legacySql.append("select id, brand, model, serialno, website, retailer, rwebsite, size, color, warranty, notes, loglink, beneficiary ");
		legacySql.append("from rezdox.treasure_tbl ");
		
		Map<String, String> columnsToSplit = new HashMap<>();

		try (PreparedStatement attrPs = dbConn.prepareStatement(attrSql.toString());) {
			buildAttributesPs(attrPs, legacySql, columnsToSplit, "TREASURE_ITEM");
			attrPs.executeBatch();
		} catch (Exception sqle) {
			log.error("Error migrating treasure item attributes. ", sqle);
		}
	}
	
	/**
	 * Migrates treasure photos and documents
	 */
	protected void migrateTreasurePhotosDocuments() {
		log.info("Migrating Treasure Item Photos");

		// Migrate treasure item photos
		StringBuilder photosSql = new StringBuilder(150);
		photosSql.append("insert into custom.rezdox_photo (photo_id, treasure_item_id, photo_nm, order_no, create_dt) ");
		photosSql.append("values (?,?,?,?,?)");

		StringBuilder legacyPhotoSql = new StringBuilder(90);
		legacyPhotoSql.append("select cast(id as varchar) as treasure_item_id, images as photos from rezdox.treasure_tbl where images != '' ");

		try (PreparedStatement photoPs = dbConn.prepareStatement(photosSql.toString());) {
			buildPhotoPs(photoPs, legacyPhotoSql, "treasure_item_id");
			photoPs.executeBatch();
		} catch (Exception sqle) {
			log.error("Error migrating treasure item photos. ", sqle);
		}

		log.info("Migrating Treasure Item Files");

		// Migrate treasure item files
		StringBuilder documentSql = new StringBuilder(150);
		documentSql.append("insert into custom.rezdox_document (document_id, treasure_item_id, document_nm, create_dt) ");
		documentSql.append("values (?,?,?,?)");

		StringBuilder legacyFileSql = new StringBuilder(80);
		legacyFileSql.append("select cast(id as varchar) as treasure_item_id, files from rezdox.treasure_tbl where files != '' ");

		try (PreparedStatement documentPs = dbConn.prepareStatement(documentSql.toString());) {
			buildDocumentPs(documentPs, legacyFileSql, "treasure_item_id");
			documentPs.executeBatch();
		} catch (Exception sqle) {
			log.error("Error migrating treasure item documents. ", sqle);
		}
	}
	
	/**
	 * Creates the default reward types
	 */
	protected void createRewardTypes() {
		log.info("Adding Reward Types");
		StringBuilder sql = new StringBuilder(200);
		sql.append("insert into custom.rezdox_reward_type (reward_type_cd, type_nm, create_dt) ");
		sql.append("values ('EARN', 'Points Earnable', getdate()), ");
		sql.append("('REDEEM', 'Points Redeemable', getdate()) ");
		executeSimpleMapping(sql, "membership");
	}
	
	/**
	 * Migrates the points that can be earned and redeemed
	 */
	protected void migrateRewards() {
		log.info("Migrating Points Earnable");
		StringBuilder pointsSql = new StringBuilder(300);
		pointsSql.append("insert into custom.rezdox_reward (reward_id, reward_type_cd, reward_nm, action_slug_txt, ");
		pointsSql.append("point_value_no, order_no, active_flg, create_dt) ");
		pointsSql.append("select name, 'EARN', ways_to_earn, name, points, sort, 1, getdate() from rezdox.rezpoints_tbl ");
		executeSimpleMapping(pointsSql, "rez points");
		
		log.info("Migrating Points Redeemable");
		StringBuilder rewardsSql = new StringBuilder(400);
		rewardsSql.append("insert into custom.rezdox_reward (reward_id, reward_type_cd, reward_nm, action_slug_txt, ");
		rewardsSql.append("point_value_no, order_no, active_flg, image_url, currency_value_no, create_dt) ");
		rewardsSql.append("select cast(id as varchar), 'REDEEM', name, upper(replace(name, ' ', '_')), points * -1, ");
		rewardsSql.append("sort, case when active = true then 1 else 0 end, image, value, getdate() from rezdox.rezrewards_tbl ");
		executeSimpleMapping(rewardsSql, "rez rewards");
	}
	
	/**
	 * Migrates the points that a member has earned or redeemed
	 */
	protected void migrateMemberRewards() {
		log.info("Migrating Points That Have Been Earned");
		StringBuilder earnedSql = new StringBuilder(375);
		earnedSql.append("insert into custom.rezdox_member_reward(member_reward_id, member_id, reward_id, point_value_no, approval_flg, create_dt) ");
		earnedSql.append("select cast(e.id as varchar), e.mem_id, e.name, e.points, 1, e.datetime ");
		earnedSql.append("from rezdox.rezearned_tbl e inner join rezdox.member_tbl m on e.mem_id = m.id ");
		earnedSql.append("where type = 'award' ");
		executeSimpleMapping(earnedSql, "rez rewards earned");
		
		log.info("Migrating Points That Have Been Redeemed");
		StringBuilder redeemedSql = new StringBuilder(500);
		redeemedSql.append("insert into custom.rezdox_member_reward(member_reward_id, member_id, reward_id, point_value_no, currency_value_no, approval_flg, create_dt) ");
		redeemedSql.append("select concat('REDEEM_', cast(red.id as varchar)), mem_id, cast(rew.id as varchar), gc_points * -1, gc_value, 1, datetime ");
		redeemedSql.append("from rezdox.rezredeem_tbl red inner join rezdox.rezrewards_tbl rew on red.gc_name = rew.name ");
		redeemedSql.append("inner join rezdox.member_tbl m on red.mem_id = m.id ");
		executeSimpleMapping(redeemedSql, "rez rewards redeemed");
	}
}
