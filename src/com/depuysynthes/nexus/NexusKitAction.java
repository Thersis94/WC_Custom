package com.depuysynthes.nexus;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;

import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.util.MessageSender;
import com.smt.sitebuilder.util.solr.SolrActionUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.action.search.SolrActionIndexVO;
import com.smt.sitebuilder.action.search.SolrActionVO;
import com.smt.sitebuilder.action.search.SolrQueryProcessor;
import com.smt.sitebuilder.action.search.SolrResponseVO;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;

/****************************************************************************
 * <b>Title</b>: NexusKitAction.java<p/>
 * <b>Description: </b> Handles the manipulation of user created kits for
 * the DePuy NeXus site.
 * <p/>
 * <b>Copyright:</b> (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Aug 9, 2015
 ****************************************************************************/

public class NexusKitAction extends SBActionAdapter {


	public static final String SOLR_INDEX = "DEPUY_NEXUS";
	public static final String KIT_SESSION_NM = "depuyNexusKit";
	
	private String successMsgEnd = " Please click outside the modal in order to complete this action.";
	
	// Potential actions for the user to take
	enum KitAction {
		Permissions, Clone, Save, Delete, Edit, Load, Add, Empty, Reorder, ChangeLayer, Copy, NewKit, Print, showShared
	}
	
	// The level of the kit that is being targeted
	enum EditLevel {
		Layer, Product, Kit
	}
	

	// The level of the kit that is being targeted
	enum KitType {
		Custom, Loaner
	}
	
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		if (req.hasParameter("searchData")) {
			String searchData = StringUtil.checkVal(req.getParameter("searchData"));
			req.setParameter("searchData", "*"+searchData+"*", true);
			req.setParameter("minimumMatch", "100%");
			
			// Do the solr search
		    	ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		    	log.debug((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		    	actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		    	SMTActionInterface sai = new SolrAction(actionInit);
		    	sai.setDBConnection(dbConn);
		    	sai.setAttributes(attributes);
			sai.retrieve(req);
		} else if (!req.hasParameter("edit")){
			// If the user is on the edit page they already have all the kit 
			// information that they need stored in their session and this is
			// not needed
			super.putModuleData(loadKits(req, false));
		}
	}
	
	
	/**
	 * Get all users a kit has been shared with
	 * @throws ActionException 
	 */
	private Object getSharedKits(SMTServletRequest req) throws ActionException {
		NexusKitVO kit = new NexusKitVO(SOLR_INDEX);
		StringBuilder sql = new StringBuilder(300);
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT *, u.PROFILE_ID as SHARED_ID FROM ").append(customDb).append("DPY_SYN_NEXUS_SET_INFO s ");
		sql.append("LEFT JOIN ").append(customDb).append("DPY_SYN_NEXUS_SET_SHARE u ");
		sql.append("on s.SET_INFO_ID = u.SET_INFO_ID ");
		sql.append("LEFT JOIN PROFILE p on p.PROFILE_ID = u.PROFILE_ID ");
		sql.append("WHERE s.SET_INFO_ID = ? and u.PROFILE_ID is not null ");
		log.debug(sql+"|"+req.getParameter("kitId"));
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, req.getParameter("kitId"));
			
			ResultSet rs = ps.executeQuery();
			kit.setData(rs);
			StringEncrypter se = new StringEncrypter((String) attributes.get(Constants.ENCRYPT_KEY));
			while (rs.next()) {
				String email = se.decrypt(rs.getString("EMAIL_ADDRESS_TXT"));
				kit.addPermision(rs.getString("SHARED_ID"), email);
			}
		} catch (Exception e) {
			throw new ActionException(e);
		}
		return kit;
	}


	public void build(SMTServletRequest req) throws ActionException {
		KitAction action;
		try {
			action = KitAction.valueOf(req.getParameter("kitAction"));
		} catch (Exception e) {
			throw new ActionException("unknown kit action: " + req.getParameter("kitAction"), e);
		}
		List<NexusKitVO> kits;
		try {
			switch(action) {
				case Permissions:
					modifyPermissions(req);
					super.putModuleData("Kits Successfully Shared.");
					break;
				case Clone:
					kits = loadKits(req, true);
					if (kits.size() > 0) {
						NexusKitVO kit = kits.get(0);
						kit.setKitId("");
						kit.setKitDesc("(Copy)"+kit.getKitDesc());
						for (NexusKitLayerVO layer : kit.getLayers()) {
							layer.setLayerId(new UUIDGenerator().getUUID());
							for (NexusKitLayerVO sublayer : layer.getSublayers()) {
								sublayer.setLayerId(new UUIDGenerator().getUUID());
								sublayer.setParentId(layer.getLayerId());
							}
						}
						kit.setOrgName(KitType.Custom.toString());
						kit.setBranchCode(KitType.Custom.toString());
						UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
						if (user != null) {
							kit.setOwnerId(user.getProfileId());
						}
						req.getSession().setAttribute(KIT_SESSION_NM, kit);
						saveKit(req);
					}
					super.putModuleData("Kit Successfully Cloned." + successMsgEnd);
					break;
				case Load:
					kits = loadKits(req, true);
					if (kits.size() > 0) {
						if (req.hasParameter("moduleStore")){
							super.putModuleData(kits.get(0));
						} else {
							req.getSession().setAttribute(KIT_SESSION_NM, kits.get(0));
						}
					}
					break;
				case Save: 
					saveKit(req);
					super.putModuleData("Kit Successfully Saved." + successMsgEnd);
					break;
				case Delete: 
					deleteKit(req);
					break;
				case Edit: 
					editKit(req);
					break;
				case ChangeLayer:
					changeLayer(req);
					break;
				case Reorder:
					reorderKit(req);
					break;
				case Copy:
					copyItem(req);
					super.putModuleData("Item Successfully Copied."+successMsgEnd);
					break;
				case NewKit:
					NexusKitVO newKit = new NexusKitVO(SOLR_INDEX);
					UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
					newKit.setKitDesc("Empty Kit");
					newKit.setOwnerId(user.getProfileId());
					newKit.setOrgName(KitType.Custom.toString());
					NexusKitLayerVO tray = new NexusKitLayerVO();
					tray.setLayerId(new UUIDGenerator().getUUID());
					tray.setLayerName("Primary Tray");
					newKit.addLayer(tray);
					req.getSession().setAttribute(KIT_SESSION_NM, newKit);
					break;
				case Print:
					buildReport(req);
					break;
				case showShared:
					super.putModuleData(getSharedKits(req));
			default:
				break;
			}
		} catch (Exception e) {
			super.putModuleData("Action Failed to Complete");
			throw e;
		}
	}
	
	
	
	/**
	 * Build the requested report based off of the request servlet and the 
	 * shopping cart
	 * @param cart
	 * @param req
	 * @throws ActionException 
	 */
	private void buildReport(SMTServletRequest req) throws ActionException {
		AbstractSBReportVO report;

		List<NexusKitVO> kits = loadKits(req, true);
		if (kits.size() > 0) {
			report = new NexusKitPDFReport();
			report.setFileName("NeXus_Kit_Report.pdf");
			Map<String, Object> data = new HashMap<>();
			data.put("kit",kits.get(0));
			data.put("baseDomain", req.getHostName());
			data.put("isForm", req.hasParameter("isForm"));
			report.setData(data);
			req.setAttribute(Constants.BINARY_DOCUMENT, report);
			req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, true);
		} else {
			throw new ActionException("Unable to get kit for report");
		}
	}
	
	
	/**
	 * Copy the item supplied by the request object
	 * @param req
	 * @throws ActionException
	 */
	private void copyItem(SMTServletRequest req) throws ActionException {
		EditLevel level;
		try {
			level = EditLevel.valueOf(req.getParameter("editLevel"));
		} catch (Exception e) {
			throw new ActionException("unknown kit action: " + req.getParameter("kitAction"), e);
		}
		NexusKitLayerVO layer;
		NexusKitVO kit = (NexusKitVO) req.getSession().getAttribute(KIT_SESSION_NM);
		try {
			switch(level) {
				case Layer:
					// Check if we are dealing with a sublayer
					if (req.hasParameter("parentId")) {
						NexusKitLayerVO parent = kit.findLayer(req.getParameter("parentId"));
						layer = parent.getSublayers().get(Convert.formatInteger(req.getParameter("index"))).clone();
						layer.setLayerId(new UUIDGenerator().getUUID());
						for (NexusKitLayerVO sublayer : layer.getSublayers()) {
							sublayer.setLayerId(layer.getLayerId());
							sublayer.setLayerId(new UUIDGenerator().getUUID());
						}
						layer.setOrderNo(parent.getSublayers().size()+1);
						parent.addLayer(layer);
					} else {
						layer= kit.getLayers().get(Convert.formatInteger(req.getParameter("index"))).clone();
						layer.setLayerId(new UUIDGenerator().getUUID());
						for (NexusKitLayerVO sublayer : layer.getSublayers()) {
							sublayer.setLayerId(layer.getLayerId());
							sublayer.setLayerId(new UUIDGenerator().getUUID());
						}
						layer.setOrderNo(kit.getLayers().size()+1);
						kit.addLayer(layer);
					}
					break;
				case Product:
					layer = kit.findLayer(req.getParameter("parentId"));
					NexusProductVO p = layer.getProducts().get(Convert.formatInteger(req.getParameter("index"))).clone();
					p.setOrderNo(layer.getProducts().size()+1);
					layer.addProduct(p);
					break;
				default: break;
			}
		} catch(CloneNotSupportedException e) {
			throw new ActionException(e);
		}
	}


	/**
	 * Call out to the level specific function forreassigning an item or
	 * sublayer to a new layer
	 * @param req
	 * @throws ActionException
	 */
	private void changeLayer(SMTServletRequest req) throws ActionException {
		EditLevel level;
		try {
			level = EditLevel.valueOf(req.getParameter("editLevel"));
		} catch (Exception e) {
			throw new ActionException("unknown kit action: " + req.getParameter("kitAction"), e);
		}
		switch(level) {
			case Layer:
				changeSubLayer(req);
				break;
			case Product:
				changeProductLayer(req);
				break;
			default: break;
		}
	}
	
	
	/**
	 * Change which layer the supplied layer is kept under
	 * @param req
	 */
	private void changeSubLayer(SMTServletRequest req) {
		NexusKitVO kit = (NexusKitVO) req.getSession().getAttribute(KIT_SESSION_NM);
		int index = Convert.formatInteger(req.getParameter("index"));
		NexusKitLayerVO layer;
		if (req.hasParameter("parentId")) {
			NexusKitLayerVO parent = kit.findLayer(req.getParameter("parentId"));
			layer = parent.getSublayers().get(index);
			parent.getSublayers().remove(index);
			changeOrderNo(parent.getSublayers(), index);
		} else {
			layer = kit.getLayers().get(index);
			kit.getLayers().remove(index);
			changeOrderNo(kit.getLayers(), index);
		}
		
		if (req.hasParameter("newParentId")) {
			layer.setParentId(req.getParameter("newParentId"));
			NexusKitLayerVO parent = kit.findLayer(layer.getParentId());
			layer.setOrderNo(parent.getSublayers().size()+1);
			parent.addLayer(layer);
		} else {
			//Since this is a top level layer the parentId needs to be cleared
			layer.setParentId("");
			layer.setOrderNo(kit.getLayers().size());
			kit.addLayer(layer);
		}
		
		req.getSession().setAttribute(KIT_SESSION_NM, kit);
	}


	
	/**
	 * Chang which layer the product is kept under
	 * @param req
	 * @throws ActionException
	 */
	private void changeProductLayer(SMTServletRequest req) throws ActionException {
		NexusKitVO kit = (NexusKitVO) req.getSession().getAttribute(KIT_SESSION_NM);
		String newParent = req.getParameter("newParentId");
		String[] indexes = req.getParameterValues("index");
		
		int offset = 0;
		NexusKitLayerVO oldLayer = null;
		NexusKitLayerVO newLayer = kit.findLayer(newParent);
		String currentLayer = "";
		for (String index : indexes) {
			String[] split = index.split("\\|");
			if (split.length != 2) continue;
			if (!currentLayer.equals(split[1])) {
				offset = 0;
				oldLayer = kit.findLayer(split[1]);
				currentLayer = split[1];
			}
			int i = Convert.formatInteger(split[0]);
			NexusProductVO p = oldLayer.getProducts().get(i-offset);
			oldLayer.getProducts().remove(i-offset);
			if (req.hasParameter("newIndex")) {
				newLayer.getProducts().add(Convert.formatInteger(req.getParameter("newIndex")), p);
			} else {
				newLayer.addProduct(p);
			}
			offset++;
		}
		
		req.getSession().setAttribute(KIT_SESSION_NM, kit);
	}
	
	
	/**
	 * Change where in the list the supplied item resides
	 * @param req
	 * @throws ActionException
	 */
	private void reorderKit(SMTServletRequest req) throws ActionException {
		EditLevel level;
		try {
			level = EditLevel.valueOf(req.getParameter("editLevel"));
		} catch (Exception e) {
			throw new ActionException("unknown kit action: " + req.getParameter("kitAction"), e);
		}
		switch(level) {
			case Layer:
				reorderLayer(req);
				break;
			case Product:
				reorderProduct(req);
				break;
			default: break;
		}
	}

	
	/**
	 * Change where in the list the supplied product is
	 * @param req
	 * @throws ActionException
	 */
	private void reorderProduct (SMTServletRequest req) throws ActionException {
		NexusKitVO kit = (NexusKitVO) req.getSession().getAttribute(KIT_SESSION_NM);
		NexusKitLayerVO layer = kit.findLayer(req.getParameter("layerId"));
		int index = Convert.formatInteger(req.getParameter("index"));
		NexusProductVO p = layer.getProducts().get(index);
		layer.getProducts().remove(index);
		layer.getProducts().add(Convert.formatInteger(req.getParameter("newIndex")), p);
		req.getSession().setAttribute(KIT_SESSION_NM, kit);
	}

	
	/**
	 * Change where in the list the supplied layer is
	 * @param req
	 * @throws ActionException
	 */
	private void reorderLayer(SMTServletRequest req) throws ActionException {
		NexusKitVO kit = (NexusKitVO) req.getSession().getAttribute(KIT_SESSION_NM);
		int index = Convert.formatInteger(req.getParameter("index"));
		int newIndex =Convert.formatInteger( req.getParameter("newIndex"));
		
		// Check whether we are dealing with a sublayer or a top level layer
		if (req.hasParameter("layerId")) {
			NexusKitLayerVO parentLayer = kit.findLayer(req.getParameter("layerId"));
			NexusKitLayerVO layer = parentLayer.getSublayers().get(index);
			parentLayer.getSublayers().remove(index);
			
			if (req.hasParameter("newParent")) {
				changeOrderNo(parentLayer.getSublayers(), index);
				NexusKitLayerVO newParent = kit.findLayer(req.getParameter("newParent"));
				layer.setOrderNo(newParent.getSublayers().size());
				newParent.getSublayers().add(newIndex, layer);
				changeOrderNo(newParent.getSublayers(), newIndex);
				
			} else {
				layer.setOrderNo(newIndex);
				parentLayer.getSublayers().add(newIndex, layer);
				changeOrderNo(parentLayer.getSublayers(), index < newIndex? index : newIndex);
			}
		} else {
			NexusKitLayerVO layer = kit.getLayers().get(index);
			kit.getLayers().remove(index);
			layer.setOrderNo(Convert.formatInteger(req.getParameter("newIndex")));
			kit.getLayers().add(Convert.formatInteger(req.getParameter("newIndex")), layer);
			changeOrderNo(kit.getLayers(), index < newIndex? index : newIndex);
		}
		req.getSession().setAttribute(KIT_SESSION_NM, kit);
	}


	/**
	 * Load all kits restricted either by the current user or by the supplied kit id.
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	private List<NexusKitVO> loadKits(SMTServletRequest req, boolean fullLoad) throws ActionException {
		StringBuilder sql = new StringBuilder(1300);
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		String kitId = StringUtil.checkVal(req.getParameter("kitId"));
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		String profileId;
		String nexusFilter = getCookie(req, "nexusFilter");
		String searchTerms = getCookie(req, "searchTerms");
		boolean profileOne = false;
		boolean profileTwo = false;
		String orgId = getCookie(req, "opCo");
		if (user == null) {
			profileId = "";
		} else {
			profileId = user.getProfileId();
		}
		// Get the kit, its top layers, and their products
		sql.append("SELECT s.*, ");
		if (fullLoad) sql.append("sl.*, si.*, ");
		sql.append("p.PROFILE_ID as SHARED_ID FROM ").append(customDb).append("DPY_SYN_NEXUS_SET_INFO s ");
		if (fullLoad) {
			sql.append("LEFT JOIN ").append(customDb).append("DPY_SYN_NEXUS_SET_LAYER sl ");
			sql.append("on sl.SET_INFO_ID = s.SET_INFO_ID ");
			sql.append("LEFT JOIN ").append(customDb).append("DPY_SYN_NEXUS_SET_ITEM si ");
			sql.append("on si.LAYER_ID = sl.LAYER_ID ");
		}
		sql.append("LEFT JOIN ").append(customDb).append("DPY_SYN_NEXUS_SET_SHARE p ");
		sql.append("on p.SET_INFO_ID = s.SET_INFO_ID and p.APPROVED_FLG = '1' ");
		sql.append("WHERE s.DESCRIPTION_TXT is not null ");


		if (kitId.length() > 0) {
			sql.append("and s.SET_INFO_ID = ? ");
		} else  {
			
			if (orgId.length() > 0) {
				sql.append("and s.ORGANIZATION_ID = ? ");
			}
			
			if (searchTerms.length() > 0) {
				sql.append("and (s.SET_SKU_TXT like ? or s.DESCRIPTION_TXT like ? or s.GTIN_TXT like ?) ");
			}
			
			if ("loaner".equals(nexusFilter)) {
				sql.append("and s.PROFILE_ID is null ");
			} else if ("custom".equals(nexusFilter)) {
				sql.append("and s.PROFILE_ID is not null ");
				sql.append("and (p.PROFILE_ID = ? or s.PROFILE_ID = ?) ");
				profileOne = true;
				profileTwo = true;
			} else if ("own".equals(nexusFilter)) {
				sql.append("and s.PROFILE_ID = ? ");
				profileOne = true;
			} else if ("shared".equals(nexusFilter)) {
				sql.append("and p.PROFILE_ID = ? ");
				profileOne = true;
			} else {
				sql.append("and (p.PROFILE_ID = ? or s.PROFILE_ID = ? or s.PROFILE_ID is null) ");
				profileOne = true;
				profileTwo = true;
			}
		}
		
		
		if (fullLoad) {
			sql.append("union ");
			
			// Get the sublayers and their products
			sql.append("SELECT s.*, sl2.*, si.*, p.PROFILE_ID as SHARED_ID FROM ").append(customDb).append("DPY_SYN_NEXUS_SET_INFO s ");
			sql.append("LEFT JOIN ").append(customDb).append("DPY_SYN_NEXUS_SET_LAYER sl ");
			sql.append("on sl.SET_INFO_ID = s.SET_INFO_ID ");
			sql.append("LEFT JOIN ").append(customDb).append("DPY_SYN_NEXUS_SET_LAYER sl2 ");
			sql.append("on sl2.PARENT_ID = sl.LAYER_ID ");
			sql.append("LEFT JOIN ").append(customDb).append("DPY_SYN_NEXUS_SET_ITEM si ");
			sql.append("on si.LAYER_ID = sl2.LAYER_ID ");
			sql.append("LEFT JOIN ").append(customDb).append("DPY_SYN_NEXUS_SET_SHARE p ");
			sql.append("on p.SET_INFO_ID = s.SET_INFO_ID and p.APPROVED_FLG = '1' ");
			sql.append("WHERE s.DESCRIPTION_TXT is not null ");

			if (kitId.length() > 0) {
				sql.append("and s.SET_INFO_ID = ? ");
			} else {
				if (orgId.length() > 0) {
					sql.append("and s.ORGANIZATION_ID = ? ");
				}
				
				if (searchTerms.length() > 0) {
					sql.append("and (s.SET_SKU_TXT like ? or s.DESCRIPTION_TXT like ? or s.GTIN_TXT like ?) ");
				}
				if ("loaner".equals(nexusFilter)) {
					sql.append("and s.PROFILE_ID is null ");
				} else if ("custom".equals(nexusFilter)) {
					sql.append("and s.PROFILE_ID is not null ");
					sql.append("and (p.PROFILE_ID = ? or s.PROFILE_ID = ?) ");
				} else if ("own".equals(nexusFilter)) {
					sql.append("s.PROFILE_ID = ? ");
				} else if ("shared".equals(nexusFilter)) {
					sql.append("and p.PROFILE_ID = ? ");
				} else {
					sql.append("and (p.PROFILE_ID = ? or s.PROFILE_ID = ? or s.PROFILE_ID is null) ");
				}
			}
		}
		
		if (req.hasParameter("gtinOrder")) {
			sql.append("ORDER BY s.GTIN_TXT ");
		} else if (req.hasParameter("orgOrder")) {
			sql.append("ORDER BY s.ORGANIZATION_ID ");
		} else {
			sql.append("ORDER BY s.DESCRIPTION_TXT ");
		}

		
		
		if (req.hasParameter("desc")) {
			sql.append("DESC ");
		} else {
			sql.append("ASC ");
		}
		
		if (fullLoad) sql.append(", sl.PARENT_ID, sl.ORDER_NO, si.ORDER_NO ");
		
		log.debug(sql+"|"+profileId+"|"+kitId+"|"+orgId+"|"+searchTerms);
		List<NexusKitVO> kits = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			int i = 1;
			if (fullLoad) {
				if (kitId.length() > 0) {
					ps.setString(i++, kitId);
				} else {
					if (orgId.length() > 0) ps.setString(i++, orgId);
					if (searchTerms.length() > 0) {
						ps.setString(i++, "%"+searchTerms+"%");
						ps.setString(i++, "%"+searchTerms+"%");
						ps.setString(i++, "%"+searchTerms+"%");
					}
					if (profileOne) ps.setString(i++, profileId);
					if (profileTwo) ps.setString(i++, profileId);
				}
			}

			if (kitId.length() > 0) {
				ps.setString(i++, kitId);
			} else {
				if (orgId.length() > 0) ps.setString(i++, orgId);
				if (searchTerms.length() > 0) {
					ps.setString(i++, "%"+searchTerms+"%");
					ps.setString(i++, "%"+searchTerms+"%");
					ps.setString(i++, "%"+searchTerms+"%");
				}
				if (profileOne) ps.setString(i++, profileId);
				if (profileTwo) ps.setString(i++, profileId);
			}
			
			ResultSet rs = ps.executeQuery();
			String currentKit = "";
			String currentLayer = "";
			NexusKitVO kit = null;
			NexusKitLayerVO layer = null;

			int page = Convert.formatInteger(req.getParameter("page"), 1);
			int rpp = Convert.formatInteger(req.getParameter("rpp"), 10);
			int count = 0;
			int start = (page-1)*rpp;
			int end = page*rpp;
			while(rs.next()) {
				
				if (!currentKit.equals(rs.getString("SET_INFO_ID"))) {
					if (count > start && count <= end) {
						if (layer != null) kit.addLayer(layer);
						if (kit != null) kits.add(kit);
					}
					currentKit = rs.getString("SET_INFO_ID");
					kit = new NexusKitVO(rs, SOLR_INDEX);
					if (StringUtil.checkVal(rs.getString("SHARED_ID")).length() > 0) {
						kit.setShared(true);
					}
					count++;
				}

				if (count < start || count >= end) continue;
				if (fullLoad) {
					if (!currentLayer.equals(rs.getString("LAYER_ID")) 
							&& StringUtil.checkVal(rs.getString("LAYER_ID")).length() > 0) {
						if (layer != null) kit.addLayer(layer);
						currentLayer = rs.getString("LAYER_ID");
						layer = new NexusKitLayerVO(rs);
					}
					if(StringUtil.checkVal(rs.getString("ITEM_ID")).length() > 0) {
						NexusProductVO p = new NexusProductVO(rs);
						SolrQueryProcessor sqp = new SolrQueryProcessor(attributes, "DePuy_NeXus");
						SolrActionVO qData = new SolrActionVO();
						qData.setNumberResponses(1);
						qData.setStartLocation(0);
						qData.setOrganizationId("DPY_SYN_NEXUS");
						qData.setRoleLevel(0);
						qData.addIndexType(new SolrActionIndexVO("", NexusProductVO.solrIndex));
						Map<String, String> filter = new HashMap<>();
						filter.put("documentId", p.getProductId());
						qData.setFilterQueries(filter);
						SolrResponseVO resp = sqp.processQuery(qData);
						if (resp.getResultDocuments().size() == 1) {
							addProductInfo(p, resp.getResultDocuments().get(0));
						}
						layer.addProduct(p);
					}
				}
			}
			req.setAttribute("total", count);
			if ( kits.size() < rpp) {
				if (layer != null) kit.addLayer(layer);
				if (kit != null) kits.add(kit);
			}
		} catch (SQLException e) {
			throw new ActionException(e);
		}
		return kits;
	}
	
	
	/**
	 * Get product information from the supplied solr document
	 * @param p
	 * @param solrDocument
	 */
	private void addProductInfo(NexusProductVO p, SolrDocument solrDocument) {
		p.setPrimaryDeviceId((String)solrDocument.get(NexusProductVO.DEVICE_ID));
		p.addGtin((String) solrDocument.get(NexusProductVO.DEVICE_ID));
		p.setSummary((String) solrDocument.get(SearchDocumentHandler.SUMMARY));
		Collection<Object> gtins = solrDocument.getFieldValues(NexusProductVO.GTIN);
		int pIndex = 0;
		if (gtins != null) {
			for (Object gtin : gtins) {
				if (p.getPrimaryDeviceId().equals(gtin)) break;
				pIndex++;
			}
			if (solrDocument.getFieldValues(NexusProductVO.UOM_LVL) != null)
				p.addUOMLevel((String) (solrDocument.getFieldValues(NexusProductVO.UOM_LVL).toArray()[pIndex]));
		}
		p.setOrgName((String) solrDocument.get(NexusProductVO.ORGANIZATION_NM));
	}


	/**
	 * Edit the kit that is currently being worked on
	 * @param req
	 * @throws ActionException 
	 */
	private void editKit(SMTServletRequest req) throws ActionException {
		EditLevel level;
		try {
			level = EditLevel.valueOf(req.getParameter("editLevel"));
		} catch (Exception e) {
			throw new ActionException("unknown kit action: " + req.getParameter("kitAction"), e);
		}
		
		switch(level) {
			case Layer:
				editLayer(req);
				break;
			case Product:
				editProduct(req);
				break;
			case Kit:
				NexusKitVO kit = (NexusKitVO)req.getSession().getAttribute(KIT_SESSION_NM);
				if (kit == null) kit = new NexusKitVO(SOLR_INDEX);
				kit.setData(req);
				req.getSession().setAttribute(KIT_SESSION_NM, kit);
				break;
		default:
			break;
		}
	}
	
	
	/**
	 * Find the product we are working with and update it
	 * @param req
	 * @throws ActionException
	 */
	private void editProduct(SMTServletRequest req) throws ActionException {
		NexusKitVO kit = (NexusKitVO)req.getSession().getAttribute(KIT_SESSION_NM);
		if(kit == null) kit = new NexusKitVO(SOLR_INDEX);
		NexusKitLayerVO layer = kit.findLayer(req.getParameter("layerId"));
		if (req.hasParameter("products")) {
			int order = layer.getProducts().size() + 1;
			for (String product : req.getParameterValues("products")) {
				String[] values = product.split("\\|", -1);
				NexusProductVO p = new NexusProductVO();
				p.setProductId(values[0]);
				p.setQuantity(1);
				p.setPrimaryDeviceId(values[1]);
				p.setSummary(values[2]);
				p.addUOMLevel(values[3]);
				p.setOrderNo(order++);
				layer.addProduct(p);
			}
		} else if (req.hasParameter("qty")) {
			NexusProductVO p = layer.getProducts().get(Convert.formatInteger(req.getParameter("index")));
			p.setQuantity(Convert.formatInteger(req.getParameter("qty")));
		}
		req.getSession().setAttribute(KIT_SESSION_NM, kit);
	}


	/**
	 * Find the supplied layer and update it
	 * @param req
	 * @throws ActionException
	 */
	private void editLayer(SMTServletRequest req) throws ActionException {
		NexusKitVO kit = (NexusKitVO)req.getSession().getAttribute(KIT_SESSION_NM);
		if(kit == null) kit = new NexusKitVO(SOLR_INDEX);
		NexusKitLayerVO layer = kit.findLayer(req.getParameter("layerId"));
		if (layer == null) {
			layer = new NexusKitLayerVO(req);
			layer.setLayerId(new UUIDGenerator().getUUID());
			if (StringUtil.checkVal(layer.getParentId()).length() > 0) {
				NexusKitLayerVO parent = kit.findLayer(layer.getParentId());
				layer.setOrderNo(parent.getSublayers().size()+1);
				parent.addLayer(layer);
			} else {
				layer.setOrderNo(kit.getLayers().size()+1);
				kit.addLayer(layer);
			}
		} else {
			// Since sublayers are no longer in use most of this code block is no longer needed.
			// However it is being kept here in case sublayers are ever returned to functionality.
/*			if (!StringUtil.checkVal(req.getParameter("parentId")).equals(layer.getParentId())) {
				if (StringUtil.checkVal(layer.getParentId()).equals("")) {
					kit.getLayers().remove(layer);
				} else {
					kit.findLayer(layer.getParentId()).getSublayers().remove(layer);
				}
				layer.setData(req);
				if (StringUtil.checkVal(layer.getParentId()).length() > 0) {
					NexusKitLayerVO parent = kit.findLayer(layer.getParentId());
					layer.setOrderNo(parent.getSublayers().size()+1);
					parent.addLayer(layer);
				} else {
					layer.setOrderNo(kit.getLayers().size()+1);
					kit.addLayer(layer);
				}
			} else {
			*/
				layer.setData(req);
//			}
		}
		req.getSession().setAttribute(KIT_SESSION_NM, kit);
	}
	
	
	/**
	 * Commit the finished kit to the database and create an entry in
	 * the solr server
	 * @param req
	 * @throws ActionException 
	 */
	private void saveKit(SMTServletRequest req) throws ActionException {
		NexusKitVO kit = (NexusKitVO) req.getSession().getAttribute(KIT_SESSION_NM);
		StringBuilder sql = new StringBuilder(300);
		boolean insert = false;
		if (StringUtil.checkVal(kit.getKitId()).length() == 0) {
			sql.append("INSERT INTO ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("DPY_SYN_NEXUS_SET_INFO ");
			sql.append("(SET_SKU_TXT, ORGANIZATION_ID, DESCRIPTION_TXT, GTIN_TXT, BRANCH_PLANT_CD, ");
			sql.append("CREATE_DT, PROFILE_ID, SET_INFO_ID) ");
			sql.append("VALUES(?,?,?,?,?,?,?,?)");
			kit.setKitId(new UUIDGenerator().getUUID());
			insert = true;
		} else {
			sql.append("UPDATE ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("DPY_SYN_NEXUS_SET_INFO ");
			sql.append("SET SET_SKU_TXT = ?, ORGANIZATION_ID = ?, DESCRIPTION_TXT = ?, GTIN_TXT = ?, BRANCH_PLANT_CD = ?, ");
			sql.append("CREATE_DT = ?, PROFILE_ID = ? WHERE SET_INFO_ID = ? ");
		}
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			int i = 1;
			ps.setString(i++, kit.getKitSKU());
			ps.setString(i++, kit.getOrgName());
			ps.setString(i++, kit.getKitDesc());
			ps.setString(i++, kit.getKitGTIN());
			ps.setString(i++, kit.getBranchCode());
			ps.setTimestamp(i++, Convert.getCurrentTimestamp());
			ps.setString(i++, kit.getOwnerId());
			ps.setString(i++, kit.getKitId());
			
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new ActionException(e);
		} 
		if (!insert) {
			clearKit(kit.getKitId());
		}
		saveLayers(kit);
		
		// Set the solr variables
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		if (kit.getOrganization().size() == 0) kit.addOrganization(site.getOrganizationId());
		if (kit.getRoles().size() == 0) kit.addRole("0");
		getShared(kit);
		addToSolr(kit);
	}
	
	
	/**
	 * Get all users that the supplied kit is shared with and add them to the kit
	 * @param kit
	 * @throws ActionException
	 */
	private void getShared(NexusKitVO kit) throws ActionException {
		StringBuilder sql = new StringBuilder();
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT * FROM ").append(customDb).append("DPY_SYN_NEXUS_SET_SHARE s ");
		sql.append("LEFT JOIN PROFILE p on p.PROFILE_ID = s.PROFILE_ID ");
		sql.append("WHERE SET_INFO_ID = ? ");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, kit.getKitId());
			
			ResultSet rs = ps.executeQuery();

			ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
			while(rs.next()) {
				kit.addPermision(rs.getString("PROFILE_ID"), pm.getStringValue("EMAIL_ADDRESS", rs.getString("EMAIL_ADDRESS")));
			}
		} catch (SQLException e) {
			throw new ActionException(e);
		}
	}


	/**
	 * Remove all existing products and layers from this kit so that they can
	 * recreated from the kit that is being saved.
	 * @param kitId
	 * @throws ActionException
	 */
	private void clearKit(String kitId) throws ActionException {
		StringBuilder sql = new StringBuilder(150);
		
		sql.append("DELETE ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("DPY_SYN_NEXUS_SET_LAYER ");
		sql.append("WHERE SET_INFO_ID = ? ");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, kitId);
			
			ps.executeUpdate();
		} catch(SQLException e) {
			throw new ActionException(e);
		}
	}


	/**
	 * Save all layers and sublayers in this kit
	 * @param kit
	 * @throws ActionException
	 */
	private void saveLayers(NexusKitVO kit) throws ActionException {
		StringBuilder sql = new StringBuilder(250);
		
		sql.append("INSERT INTO ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("DPY_SYN_NEXUS_SET_LAYER ");
		sql.append("(LAYER_NM, PARENT_ID, ORDER_NO, CREATE_DT, SET_INFO_ID, LAYER_ID) ");
		sql.append("VALUES(?,?,?,?,?,?)");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())){
			for (NexusKitLayerVO layer : kit.getLayers()) {
				int i = 1;
				ps.setString(i++, layer.getLayerName());
				ps.setString(i++, layer.getParentId());
				ps.setInt(i++, layer.getOrderNo());
				ps.setTimestamp(i++, Convert.getCurrentTimestamp());
				ps.setString(i++, kit.getKitId());
				ps.setString(i++, layer.getLayerId());
				ps.addBatch();
				for (NexusKitLayerVO sublayer : layer.getSublayers()) {
					i = 1;
					ps.setString(i++, sublayer.getLayerName());
					ps.setString(i++, sublayer.getParentId());
					ps.setInt(i++, sublayer.getOrderNo());
					ps.setTimestamp(i++, Convert.getCurrentTimestamp());
					ps.setString(i++, kit.getKitId());
					ps.setString(i++, sublayer.getLayerId());
					ps.addBatch();
				}
			}
			ps.executeBatch();
		} catch (SQLException e) {
			throw new ActionException(e);
		}

		for (NexusKitLayerVO layer : kit.getLayers()) {
			saveProducts(layer);
			for (NexusKitLayerVO sublayer : layer.getSublayers()) {
				saveProducts(sublayer);
			}
		}
	}
	
	
	/**
	 * Save all products in the supplied kit
	 * @param layer
	 * @throws ActionException
	 */
	private void saveProducts(NexusKitLayerVO layer) throws ActionException {
		StringBuilder insert = new StringBuilder(350);
		
		insert.append("INSERT INTO ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("DPY_SYN_NEXUS_SET_ITEM ");
		insert.append("(PRODUCT_SKU_TXT, QUANTITY_NO, UNIT_MEASURE_CD, EFFECTIVE_START_DT, ");
		insert.append("EFFECTIVE_END_DT, ITEM_GTIN_TXT, CREATE_DT, LAYER_ID, ORDER_NO, ITEM_ID) ");
		insert.append("VALUES(?,?,?,?,?,?,?,?,?,?)");

		PreparedStatement insertState = null;
		try {
			insertState = dbConn.prepareStatement(insert.toString());
			int order = 1;
			for (NexusProductVO product : layer.getProducts()) {
				int i = 1;
				insertState.setString(i++, product.getProductId());
				insertState.setInt(i++, product.getQuantity());
				insertState.setString(i++, product.getUomLevel().get(0));
				insertState.setTimestamp(i++, Convert.formatTimestamp(product.getStart()));
				insertState.setTimestamp(i++, Convert.formatTimestamp(product.getEnd()));
				insertState.setString(i++, product.getPrimaryDeviceId());
				insertState.setTimestamp(i++, Convert.getCurrentTimestamp());
				insertState.setString(i++, layer.getLayerId());
				insertState.setInt(i++, order++);
				insertState.setString(i++, new UUIDGenerator().getUUID());
				insertState.addBatch();
			}
			insertState.executeBatch();
		} catch (SQLException e) {
			throw new ActionException(e);
		} finally {
			try {
				insertState.close();
			} catch (Exception e){}
			
		}
	}
	
	
	/**
	 * Deletes the kit from both the database and solr
	 * @param req
	 */
	private void deleteKit(SMTServletRequest req) throws ActionException {
		EditLevel level;
		NexusKitVO kit;
		try {
			level = EditLevel.valueOf(req.getParameter("editLevel"));
		} catch (Exception e) {
			throw new ActionException("unknown kit action: " + req.getParameter("kitAction"), e);
		}
		int index;
		switch(level) {
			case Layer:
				index = Convert.formatInteger(req.getParameter("index"));
				kit = (NexusKitVO) req.getSession().getAttribute(KIT_SESSION_NM);
				if (req.hasParameter("parentId")) {
					NexusKitLayerVO parent = kit.findLayer(req.getParameter("parentId"));
					parent.getSublayers().remove(index);
					changeOrderNo(parent.getSublayers(), index);
				} else {
					kit.getLayers().remove(index);
					changeOrderNo(kit.getLayers(), index);
				}
				req.getSession().setAttribute(KIT_SESSION_NM, kit);
				break;
			case Product:
				kit = (NexusKitVO) req.getSession().getAttribute(KIT_SESSION_NM);
				String[] indexes = req.getParameterValues("index");
				int offset = 0;
				NexusKitLayerVO layer = null;
				String currentLayer = "";
				for (String single : indexes) {
					String[] split = single.split("\\|");
					if (!currentLayer.equals(split[1])) {
						offset = 0;
						layer = kit.findLayer(split[1]);
					}
					int i = Convert.formatInteger(split[0]);
					layer.getProducts().remove(i-offset);
					offset++;
				}
				
				req.getSession().setAttribute(KIT_SESSION_NM, kit);
				break;
			case Kit:
				index = Convert.formatInteger(req.getParameter("index"));
				UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
				StringBuilder sql = new StringBuilder(150);
				
				sql.append("DELETE ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("DPY_SYN_NEXUS_SET_INFO ");
				sql.append("WHERE (1=2) ");
				
				for (int i=0; i < req.getParameterValues("kitId").length; i++) {
					sql.append("OR (SET_INFO_ID = ? and PROFILE_ID = ?) ");
				}

			    	ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
				try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
					int i = 1;
					for (String s: req.getParameterValues("kitId")) {
						ps.setString(i++, s);
						ps.setString(i++, user.getProfileId());
					}
					
					ps.executeUpdate();
				} catch (SQLException e) {
					throw new ActionException(e);
				}
				attributes.put(Constants.SOLR_COLLECTION_NAME, getSolrCollection((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1)));
				SolrActionUtil util = new SolrActionUtil(attributes, false);
				for (String s: req.getParameterValues("kitId")) {
					util.removeDocument(s);
				}
				req.getSession().removeAttribute(KIT_SESSION_NM);
				break;
		default:
			break;
		}
	}
	
	
	/**
	 * Determine how a kits permissions need to be changed
	 * @param req
	 * @throws ActionException 
	 */
	private void modifyPermissions(SMTServletRequest req) throws ActionException {
		UserDataVO user = new UserDataVO(req);
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		if (!req.hasParameter("profileId")) {
			try {
				user.setProfileId(pm.checkProfile(new UserDataVO(req), dbConn));
				log.debug(user.getProfileId());
				// If this email address has no corrosponding profile create it now.
				if (user.getProfileId() == null) {
					pm.updateProfile(user, dbConn);
				}
			} catch (DatabaseException e1) {
				throw new ActionException(e1);
			}
		}
		
		if (req.hasParameter("delete")) {
			String kitId = req.getParameter("kitId");
			removePermission(req.getParameter("profileId"), kitId);
			
			SolrInputDocument sdoc = new SolrInputDocument();
			Map<String,Object> fieldModifier = new HashMap<>(1);
			fieldModifier.put("remove",user.getProfileId());
			sdoc.addField("owner", fieldModifier);
			sdoc.addField("documentId",kitId);
			
			try {
				String baseUrl = StringUtil.checkVal(attributes.get(Constants.SOLR_BASE_URL), null);
				ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
				String collection = getSolrCollection((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
				HttpSolrServer server = new HttpSolrServer(baseUrl + collection);
				server.add(sdoc);
			} catch (Exception e) {
				throw new ActionException(e);
			}
		} else {
			UserDataVO owner = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
			// Determine if the current user is adding someone else to their kit or someone else
			// is requesting access to a kit.  If they are requesting access the share is placed
			// in an awaiting approval state for the owner to accept or deny at a later date.
			int request = 0;
			if (!owner.getProfileId().equals(user.getProfileId())) request = 1;
			List<SolrInputDocument> docUpdates = new ArrayList<>();
			String[] ids = req.getParameterValues("kitId");
			for (String kitId : ids) {
				addPermission(user.getProfileId(), kitId, request);
				if (request == 1) {
					SolrInputDocument sdoc = new SolrInputDocument();
					Map<String,Object> fieldModifier = new HashMap<>(1);
					fieldModifier.put("add",user.getProfileId());
					sdoc.addField("owner", fieldModifier);
					sdoc.addField("documentId",kitId);
					docUpdates.add(sdoc);
				}
			}
			
			try {
				String baseUrl = StringUtil.checkVal(attributes.get(Constants.SOLR_BASE_URL), null);
				ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
				String collection = getSolrCollection((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
				HttpSolrServer server = new HttpSolrServer(baseUrl + collection);
				for (SolrInputDocument doc : docUpdates) {
					server.add( doc );
				}
			} catch (Exception e) {
				throw new ActionException(e);
			}
			sendEmail(user.getEmailAddress(), owner.getFullName(), ids.length);
		}
	}
	
	
	
	/**
	 * Send an email to the user that the kits were shared with informing them 
	 * of the kits they now have access to.
	 */
	private void sendEmail(String emailAddress, String name, int total) {
		EmailMessageVO email = new EmailMessageVO();
		
		try {
			email.addRecipient(emailAddress);
			email.setSubject(name + " has Shared Some of Their Sets With You");
			StringBuilder body = new StringBuilder(500);
			body.append("<p><img alt='' src='http://dpy-syn-nexus-1.depuydev.siliconmtn.com/binary/themes/CUSTOM/DEPUY/DPY_SYN_NEXUS/images/logo.jpg' style='width: 424px; height: 66px;' /></p>");
			body.append("<p>").append(name).append(" has shared ").append(total).append(" sets with you.</p>");
			body.append("<p>Please log in to <a href='http://dpy-syn-nexus-1.depuydev.siliconmtn.com/my_sets'>DePuy Synthes NeXus</a> to view these sets</p>");
			body.append("<p>If you do not have an account with our site please <a href='http://dpy-syn-nexus-1.depuydev.siliconmtn.com/register'>create one here</a> ");
			body.append("in order to view these sets.</p>");
			email.setHtmlBody(body.toString());
			email.setFrom("donotreply@depuyus.jnj.com");

			MessageSender ms = new MessageSender(attributes, dbConn);
			ms.sendMessage(email);
		} catch (InvalidDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	/**
	 * Delete the selected sharing permission
	 * @param req
	 */
	private void removePermission(String profileId, String kitId) throws ActionException {
		StringBuilder sql =  new StringBuilder(150);
		sql.append("DELETE ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("DPY_SYN_NEXUS_SET_SHARE ");
		sql.append("WHERE PROFILE_ID = ? and SET_INFO_ID = ? ");
		log.debug(sql+"|"+profileId+"|"+kitId);
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, profileId);
			ps.setString(2, kitId);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new ActionException(e);
		}
	}
	
	
	/**
	 * Give a selected user access permissions to the provided kit
	 * @param req
	 */
	private void addPermission(String profileId, String kitId, int request) throws ActionException {
		StringBuilder sql =  new StringBuilder(200);
		sql.append("INSERT INTO ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("DPY_SYN_NEXUS_SET_SHARE ");
		sql.append("(PROFILE_SET_ID, PROFILE_ID, SET_INFO_ID, APPROVED_FLG, CREATE_DT) ");
		sql.append("VALUES(?,?,?,?,?)");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, new UUIDGenerator().getUUID());
			ps.setString(2, profileId);
			ps.setString(3, kitId);
			ps.setInt(4, request);
			ps.setTimestamp(5, Convert.getCurrentTimestamp());
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new ActionException(e);
		}
	}


	/**
	 * Since the layers need to know their order number in order
	 * to be saved and manipulated any removals or mid-list insertions
	 * need to be followed by ensuring each layer has the proper order no.
	 * @param layers
	 * @param index
	 */
	private void changeOrderNo(List<NexusKitLayerVO> layers, int index) {
		for (int i = index; i < layers.size(); i++) {
			layers.get(i).setOrderNo(i+1); 
		}
	}
	
	
	/**
	 * Create a solr action util and submit the user's kit
	 */
	private void addToSolr(NexusKitVO kit) throws ActionException {
	    	ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		attributes.put(Constants.SOLR_COLLECTION_NAME, getSolrCollection((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1)));
		new SolrActionUtil(attributes, false).addDocument(kit);
	}
	
	
	/**
	 * Get the solr collection 
	 * @param solrId
	 * @return
	 * @throws ActionException
	 */
	private String getSolrCollection(String solrId) throws ActionException {
		StringBuilder sql = new StringBuilder(200);
		sql.append("SELECT SOLR_COLLECTION_PATH FROM SOLR_ACTION sa ");
		sql.append("inner join SOLR_COLLECTION sc on sa.SOLR_COLLECTION_ID = sc.SOLR_COLLECTION_ID ");
		sql.append("WHERE ACTION_ID = ? ");
		log.debug(sql+"|"+solrId);
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, solrId);
			ResultSet rs = ps.executeQuery();
			
			if (rs.next()) {
				 return rs.getString(1);
			} else {
				throw new ActionException("Got null value for Solr Collection Name when adding kit to Solr", new NullPointerException());
			}
		} catch(SQLException e) {
			throw new ActionException(e);
		}
	}



	/**
	 * Checks if a cookie exists and returns either the cookie's value or an
	 * empty string
	 */
	private String getCookie(SMTServletRequest req, String name) {
		Cookie c = req.getCookie(name);
		if (c == null) return "";
		return StringEncoder.urlDecode(c.getValue());
	}
}
