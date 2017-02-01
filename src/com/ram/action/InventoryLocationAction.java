package com.ram.action;

// SMT Base Libs
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// RAM WC Libs
import com.ram.action.data.InventoryLocationVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.user.LocationManager;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteBuilderUtil;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: InventoryLocationAction.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Manages the event location data where inventories will 
 * take place.  Spatial information will be captured as well for integration
 * of mapping/geocoding services
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Feb 13, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class InventoryLocationAction extends SBActionAdapter {
	private SiteBuilderUtil util = new SiteBuilderUtil();
	
	/**
	 * 
	 */
	public InventoryLocationAction() {
		
	}

	/**
	 * @param actionInit
	 */
	public InventoryLocationAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(ActionRequest req) throws ActionException {
		String ilId = StringUtil.checkVal(req.getParameter("inventoryLocationId"));
		boolean add = Convert.formatBoolean(req.getParameter("addLocation")); 
		if (add && ilId.length() == 0) return;
		
		String dbs = (String) this.getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder s = new StringBuilder();
		s.append("select * from ").append(dbs).append("ram_inventory_location ");
		if (ilId.length() > 0) s.append("where inventory_location_id = ? ");
		s.append("order by location_nm, zip_cd ");
		log.debug("Retrieving location info SQL: " + s);
		
		PreparedStatement ps = null;
		List<InventoryLocationVO> data = new ArrayList<InventoryLocationVO>();
		try {
			ps = dbConn.prepareStatement(s.toString());
			if (ilId.length() > 0) ps.setString(1, ilId);
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				data.add(new InventoryLocationVO(rs));
			}
			
			// Get the profile data if there is only 1 customer returned.  If only
			// 1 is returned, this means the entry is being edited, not listed
			if (data.size() == 1 && data.get(0).hasContactAssigned()) {
				InventoryLocationVO il = data.get(0);
				ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
				il.setContact(pm.getProfile(il.getProfileId(), dbConn, ProfileManager.PROFILE_ID_LOOKUP, null));
			}
			
			this.putModuleData(data, data.size(), false);
		} catch (Exception e) {
			log.error("unable to retrieve location data", e);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	public void build(ActionRequest req) throws ActionException {
		String dbs = (String) this.getAttribute(Constants.CUSTOM_DB_SCHEMA);
		InventoryLocationVO ilVo = new InventoryLocationVO(req);
		
		// Geocode the address
		LocationManager lm = new LocationManager(ilVo);
		GeocodeLocation gl = lm.geocode(attributes);
		ilVo.setMatchCode(gl.getMatchCode());
		ilVo.setLatitude(gl.getLatitude());
		ilVo.setLongitude(gl.getLongitude());
		
		// Store the profile
		this.storeProfile(ilVo);
		
		// Build the SQL
		StringBuilder s = new StringBuilder();
		if (StringUtil.checkVal(ilVo.getInventoryLocationId()).length() == 0) {
			s.append("insert into ").append(dbs).append("ram_inventory_location ");
			s.append("(location_nm, profile_id,address_txt, address2_txt, city_nm, ");
			s.append("state_cd, zip_cd, country_cd, match_cd, latitude_no, ");
			s.append("longitude_no, create_dt, inventory_location_id) ");
			s.append("values(?,?,?,?,?,?,?,?,?,?,?,?,?) ");
			
			ilVo.setInventoryLocationId(new UUIDGenerator().getUUID());
		} else {
			s.append("update ").append(dbs).append("ram_inventory_location ");
			s.append("set location_nm = ?, profile_id = ?,address_txt = ?, ");
			s.append("address2_txt = ?, city_nm = ?, state_cd = ?, zip_cd = ?,");
			s.append(" country_cd = ?, match_cd = ?, latitude_no = ?, longitude_no = ?, ");
			s.append("update_dt = ? where inventory_location_id = ? ");
		}
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setString(1, ilVo.getLocationName());
			ps.setString(2, ilVo.getProfileId());
			ps.setString(3, ilVo.getAddress());
			ps.setString(4, ilVo.getAddress2());
			ps.setString(5, ilVo.getCity());
			ps.setString(6, ilVo.getState());
			ps.setString(7, ilVo.getZipCode());
			ps.setString(8, ilVo.getCountry());
			ps.setString(9, ilVo.getMatchCode().toString());
			ps.setDouble(10, ilVo.getLatitude());
			ps.setDouble(11, ilVo.getLongitude());
			ps.setTimestamp(12, Convert.getCurrentTimestamp());
			ps.setString(13, ilVo.getInventoryLocationId());
			
			ps.executeUpdate();
		} catch (Exception e) {
			log.error("Unable to store Inventory Locaiton Data", e);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		
		util.manualRedirect(req, ((PageVO)req.getAttribute(Constants.PAGE_DATA)).getFullPath());
	}
	
	/**
	 * Updates/Inserts the Profile info for the location contact
	 * @param ilVo
	 */
	protected void storeProfile(InventoryLocationVO ilVo) {
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		try {
			ilVo.setProfileId(pm.checkProfile(ilVo.getContact(), dbConn));
			
			// Either insert or update the profile based upon the profileId
			if (StringUtil.checkVal(ilVo.getProfileId()).length() > 0) {
				pm.updateProfilePartially(getProfileFields(ilVo), ilVo.getContact(), dbConn);
			} else {
				pm.updateProfile(ilVo.getContact(), dbConn);
				ilVo.setProfileId(ilVo.getContact().getProfileId());
			}
		} catch (DatabaseException e) {
			log.error("Unable to update user profile info", e);
		}
	}
	
	/**
	 * Sets the profile fields to be updated for an existing account
	 * @param vo
	 * @return
	 */
	protected Map<String, Object> getProfileFields(InventoryLocationVO vo) {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("LAST_NM", vo.getContact().getLastName());
		data.put("FIRST_NM", vo.getContact().getFirstName());
		data.put("EMAIL_ADDRESS_TXT", vo.getContact().getEmailAddress());
		
		return data;
	}
}
