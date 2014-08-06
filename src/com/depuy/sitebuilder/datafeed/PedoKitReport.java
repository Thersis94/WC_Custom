package com.depuy.sitebuilder.datafeed;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;

/****************************************************************************
 * <b>Title</b>: PedoKitReport.java<p/>
 * <b>Description: Returns the customer data tied to PedoKit requests collected via 
 * BRC's and the JR website (an online alternative to the BRC).</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Mar 18, 2013
 ****************************************************************************/
public class PedoKitReport implements Report {
	private SMTDBConnection conn = null;
	protected static Logger log = null;
	private Map<String, Object> attributes = null;

	public PedoKitReport() {
		log = Logger.getLogger(this.getClass());
	}
	
	/* (non-Javadoc)
	 * @see com.depuy.sitebuilder.datafeed.Report#setAttibutes(java.util.Map)
	 */
	@Override
	public void setAttibutes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	/* (non-Javadoc)
	 * @see com.depuy.sitebuilder.datafeed.Report#setDatabaseConnection(com.siliconmtn.db.pool.SMTDBConnection)
	 */
	@Override
	public void setDatabaseConnection(SMTDBConnection conn) {
		this.conn = conn;
	}

	/* (non-Javadoc)
	 * @see com.depuy.sitebuilder.datafeed.Report#retrieveReport(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public Object retrieveReport(SMTServletRequest req) throws DatabaseException, InvalidDataException {
		log.info("Retrieving PedoKit Report");
		
		// Get the Dates - this campaign didn't start until Q1 2013, so don't bother going back farther than that.
		java.sql.Date startDate = Convert.formatSQLDate(Convert.formatStartDate(req.getParameter("startDate"), "01/01/2013"));
		java.sql.Date endDate = Convert.formatSQLDate(Convert.formatEndDate(req.getParameter("endDate")));

		List<PedoKitVO> data = new ArrayList<PedoKitVO>();

		//load the data from the data feed system  (submissions via BRC)
		try {
			this.loadDataFeedData(data, startDate, endDate);

			//now we have to load Profiles for all these users!
			this.loadProfiles(data);
			
			//finally, load the most recent (previous) campaign tied to each profileId; with it's two "QUAL_*" responses.
			this.loadPreviousCampaigns(data, startDate);
			
		} catch (Exception e) {
			log.error("unable to load PedoKit data", e);
			throw new DatabaseException(e);
		}
		
		log.debug("loaded " + data.size() + " records");
		return data;
	}
	
	
	/**
	 * This method queries the DePuy DATA_FEED database to load the submissions
	 * for PedoKit Requests.  All BRC and Website submissions have been previously 
	 * aggregated into this repository.
	 * @param data
	 * @param startDate
	 * @param endDate
	 */
	private void loadDataFeedData(List<PedoKitVO> data, java.sql.Date startDate, java.sql.Date endDate)
			throws SQLException {
		String dfSchema = ReportFacadeAction.DF_SCHEMA;
		
		StringBuilder sql = new StringBuilder(1950);
		sql.append("select a.CUSTOMER_ID, a.CALL_SOURCE_CD, a.PROFILE_ID, a.PRODUCT_CD, "); 
		sql.append("a.ATTEMPT_DT, b.PROCESS_START_DT, b.PROCESS_FAILED_DT, "); 
		sql.append("b.PROCESS_FAIL_TXT, c.QUESTION_MAP_ID, c.RESPONSE_TXT, d.QUESTION_CD, opc.ALLOW_COMM_FLG, "); 
		sql.append("dt.TERRITORY_NO, dt.DISTRIBUTOR_NM, dt.DIRECTORY_NM, dt.REGION_NM, dt.AVP_NM, ");
		sql.append("cr1.RESPONSE_TXT as SURGEON_NM, cr2.RESPONSE_TXT as HOSPITAL_NM ");
		sql.append("from ").append(dfSchema).append("CUSTOMER a "); 
		sql.append("inner join ").append(dfSchema).append("FULFILLMENT b on a.CUSTOMER_ID=b.CUSTOMER_ID "); 
		sql.append("inner join ").append(dfSchema).append("CUSTOMER_RESPONSE c on a.CUSTOMER_ID=c.CUSTOMER_ID "); 
		sql.append("inner join ").append(dfSchema).append("QUESTION_MAP d on c.QUESTION_MAP_ID=d.QUESTION_MAP_ID "); 
		sql.append("inner join ").append(dfSchema).append("QUESTION e on d.QUESTION_ID=e.QUESTION_ID "); 
		sql.append("left join ").append(dfSchema).append("CUSTOMER cs on a.PROFILE_ID=cs.PROFILE_ID ");
		sql.append("and a.CREATE_DT < cs.CREATE_DT and cs.SELECTION_CD='DPYPEDOIDCARD' ");
		sql.append("left join ").append(dfSchema).append("CUSTOMER_RESPONSE cr1 ");
		sql.append("on cs.CUSTOMER_ID=cr1.CUSTOMER_ID and cr1.QUESTION_MAP_ID=565 ");
		sql.append("left join ").append(dfSchema).append("CUSTOMER_RESPONSE cr2 ");
		sql.append("on cs.CUSTOMER_ID=cr2.CUSTOMER_ID and cr2.QUESTION_MAP_ID=563 ");
		sql.append("left join PROFILE p on p.PROFILE_ID = a.PROFILE_ID ");
		sql.append("left join PROFILE_ADDRESS pa on pa.PROFILE_ID = p.PROFILE_ID ");
		sql.append("left join ORG_PROFILE_COMM opc on a.PROFILE_ID=opc.PROFILE_ID ");
		sql.append("left join ").append((String)attributes.get("customDbSchema")).append("DEPUY_TERRITORIES dt on pa.ZIP_CD = dt.ZIP_CD ");
		sql.append("where a.LEAD_TYPE_ID=15 and SKU_CD in ('DPYKNEPSL1','DPYHIPPSL1','DPYSHOPSL1') ");
		sql.append("and a.attempt_dt between ? and ? order by attempt_dt");
		log.debug(sql);
		
		String lastCustomerId = "";
		PedoKitVO vo = null;
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql.toString());
			ps.setDate(1, startDate);
			ps.setDate(2, endDate);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				//new customer records start here
				if (!rs.getString("customer_id").equals(lastCustomerId)) {
					if (vo != null) data.add(vo);
					vo = new PedoKitVO(rs);
					lastCustomerId = rs.getString("customer_id");
				}
				
				vo.addResponse(rs.getString("QUESTION_CD"), rs.getString("response_txt"));
			}
			
			//add that trailing record to the results
			if (vo != null)
				data.add(vo);
			
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
	}
	
	
	/**
	 * calls ProfileManager to load the user's personal information from WC
	 * @param data
	 * @throws Exception
	 */
	private void loadProfiles(List<PedoKitVO> data) throws Exception {
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		List<String> profileIds = new ArrayList<String>();
		
		for (PedoKitVO vo : data) {
			profileIds.add(vo.getProfileId());
		}
		
		Map<String, UserDataVO> profiles = pm.searchProfileMap(conn, profileIds);
		for (PedoKitVO vo : data) {
			if (profiles.containsKey(vo.getProfileId())) {
				vo.setProfile(profiles.get(vo.getProfileId()));
			}
		}
	}
	
	private void loadPreviousCampaigns(List<PedoKitVO> data, java.sql.Date startDate) throws Exception {
		StringBuilder sql = new StringBuilder();
		sql.append("select top 1 * from ").append(ReportFacadeAction.DF_SCHEMA);
		sql.append("DPY_CAMPAIGN_SUBMISSIONS_VIEW  where ");
		sql.append("PROFILE_ID=? and CUSTOMER_ID != ? and product_cd=? and LEAD_TYPE_ID > 1 ");
		sql.append("order by ATTEMPT_DT desc");
		String sqlStr = sql.toString();
		log.debug(sqlStr);
		
		PreparedStatement ps = null;
		
		//loop our results and grab the last (before 'this') campaign for each user.
		//this could result in a lot of queries against the DB (large loop),
		//but such a specific data-capture from such a large table should be quick and an acceptable tradeoff.
		for (PedoKitVO row : data) {
			try {
				ps = conn.prepareStatement(sqlStr);
				ps.setString(1, row.getProfileId());
				ps.setString(2, row.getCustomerId());
				ps.setString(3, row.getProductCd());
				ResultSet rs = ps.executeQuery();
				if (rs.next()) {
					log.debug("found prev campaign " + rs.getString("call_source_cd"));
					row.setLastCampaign(rs.getString("call_source_cd"));
					row.setLastQual01(rs.getString("qual_01"));
					row.setLastQual02(rs.getString("qual_02"));
					row.setLastAttemptDt(rs.getDate("attempt_dt"));
				}
					
			} finally {
				try {
					ps.close();
				} catch(Exception e) {}
			}
		}
		
	}
		
	
	
	/**
	 * **************************************************************************
	 * <b>Title</b>: PedoKitReport.PedoKitVO.java<p/>
	 * <b>Description: The data-holding VO for this report.</b> 
	 * <p/>
	 * <b>Copyright:</b> Copyright (c) 2013<p/>
	 * <b>Company:</b> Silicon Mountain Technologies<p/>
	 * @author James McKain
	 * @version 1.0
	 * @since Mar 18, 2013
	 ***************************************************************************
	 */
	public class PedoKitVO {
		private String customerId = null;
		private String callSourceCd = null;
		private String productCd = null;
		private Date attemptDt = null;
		private String profileId = null;
		private Date kitMailDt = null;
		private Date kitFailDt = null;
		private String kitFailTxt = null;
		private String hospitalNm = null;
		private String surgeonNm = null;
		private Map<String, String> responses = new HashMap<String, String>();
		private UserDataVO profile = null;
		private int allowComm = 0;
		
		private String lastCampaign = null;
		private String lastQual01 = null;
		private String lastQual02 = null;
		private Date lastAttemptDt = null;
		
		private String territory = null;
		private String distributor = null;
		private String ad = null;
		private String region = null;
		private String avp = null;
		
		public PedoKitVO(ResultSet rs) {
			DBUtil util = new DBUtil();
			customerId = util.getStringVal("customer_id", rs);
			callSourceCd = util.getStringVal("call_source_cd", rs);
			productCd = util.getStringVal("product_cd", rs);
			attemptDt = util.getDateVal("attempt_dt", rs);
			profileId = util.getStringVal("profile_id", rs);
			kitMailDt = util.getDateVal("PROCESS_START_DT", rs);
			kitFailDt = util.getDateVal("PROCESS_FAILED_DT", rs);
			kitFailTxt = util.getStringVal("PROCESS_FAIL_TXT", rs);
			territory = util.getStringVal("TERRITORY_NO", rs);
			distributor = util.getStringVal("DISTRIBUTOR_NM", rs);
			ad = util.getStringVal("DIRECTORY_NM", rs);
			region = util.getStringVal("REGION_NM", rs);
			avp = util.getStringVal("AVP_NM", rs);
			allowComm = util.getIntVal("ALLOW_COMM_FLG", rs);
			hospitalNm = util.getStringVal("HOSPITAL_NM", rs);
			surgeonNm = util.getStringVal("SURGEON_NM", rs);
			
		}

		public String getCustomerId() {
			return customerId;
		}

		public void setCustomerId(String customerId) {
			this.customerId = customerId;
		}

		public String getCallSourceCd() {
			return callSourceCd;
		}

		public void setCallSourceCd(String callSourceCd) {
			this.callSourceCd = callSourceCd;
		}

		public String getProductCd() {
			return productCd;
		}

		public void setProductCd(String productCd) {
			this.productCd = productCd;
		}

		public Date getAttemptDt() {
			return attemptDt;
		}

		public void setAttemptDt(Date attemptDt) {
			this.attemptDt = attemptDt;
		}

		public String getProfileId() {
			return profileId;
		}

		public void setProfileId(String profileId) {
			this.profileId = profileId;
		}

		public Date getKitMailDt() {
			return kitMailDt;
		}

		public void setKitMailDt(Date kitMailDt) {
			this.kitMailDt = kitMailDt;
		}

		public Date getKitFailDt() {
			return kitFailDt;
		}

		public void setKitFailDt(Date kitFailDt) {
			this.kitFailDt = kitFailDt;
		}

		public String getKitFailTxt() {
			return kitFailTxt;
		}

		public void setKitFailTxt(String kitFailTxt) {
			this.kitFailTxt = kitFailTxt;
		}

		public Map<String, String> getResponses() {
			return responses;
		}

		public void setResponses(Map<String, String> responses) {
			this.responses = responses;
		}
		
		public void addResponse(String k, String v) {
			responses.put(k, v);
		}

		public UserDataVO getProfile() {
			return profile;
		}

		public void setProfile(UserDataVO profile) {
			this.profile = profile;
		}

		public String getLastCampaign() {
			return lastCampaign;
		}

		public void setLastCampaign(String lastCampaign) {
			this.lastCampaign = lastCampaign;
		}

		public String getLastQual01() {
			return lastQual01;
		}

		public void setLastQual01(String lastQual01) {
			this.lastQual01 = lastQual01;
		}

		public String getLastQual02() {
			return lastQual02;
		}

		public void setLastQual02(String lastQual02) {
			this.lastQual02 = lastQual02;
		}

		public Date getLastAttemptDt() {
			return lastAttemptDt;
		}

		public void setLastAttemptDt(Date lastAttemptDt) {
			this.lastAttemptDt = lastAttemptDt;
		}

		public String getTerritory() {
			return territory;
		}

		public void setTerritory(String territory) {
			this.territory = territory;
		}

		public String getDistributor() {
			return distributor;
		}

		public void setDistributor(String distributor) {
			this.distributor = distributor;
		}

		public String getAd() {
			return ad;
		}

		public void setAd(String ad) {
			this.ad = ad;
		}

		public String getRegion() {
			return region;
		}

		public void setRegion(String region) {
			this.region = region;
		}

		public String getAvp() {
			return avp;
		}

		public void setAvp(String avp) {
			this.avp = avp;
		}

		public int getAllowComm() {
			return allowComm;
		}

		public void setAllowComm(int allowComm) {
			this.allowComm = allowComm;
		}

		public String getHospitalNm() {
			return hospitalNm;
		}

		public void setHospitalNm(String hospitalNm) {
			this.hospitalNm = hospitalNm;
		}

		public String getSurgeonNm() {
			return surgeonNm;
		}

		public void setSurgeonNm(String surgeonNm) {
			this.surgeonNm = surgeonNm;
		}
		
	}
}

