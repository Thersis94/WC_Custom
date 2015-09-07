package com.depuysynthes.nexus;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.search.SolrAction;

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
	
	public static final String KIT_SESSION_NM = "depuy-nexus-kit";
	
	// Potential actions for the user to take
	enum KitAction {
		Permissions, Clone, Save, Delete, Edit, Load, Add, Empty, Reorder, ChangeLayer, Copy
	}
	
	// The level of the kit that is being targeted
	enum EditLevel {
		Layer, Product, Kit
	}
	
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		// In most cases the user will already have eveyrthing they need in the 
		// session.  These are special cases where more is needed from the system
		if (req.hasParameter("dashboard")) {
			super.putModuleData(loadKits(req));
		} else if (req.hasParameter("searchData")) {
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
		}
	}
	
	
	public void build(SMTServletRequest req) throws ActionException {
		KitAction action;
		try {
			action = KitAction.valueOf(req.getParameter("kitAction"));
		} catch (Exception e) {
			throw new ActionException("unknown kit action: " + req.getParameter("kitAction"), e);
		}
		List<NexusKitVO> kits;
		switch(action) {
			case Permissions:
				modifyPermissions(req);
				break;
			case Clone:
				kits = loadKits(req);
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
					req.getSession().setAttribute(KIT_SESSION_NM, kit);
					saveKit(req);
				}
				break;
			case Load:
				kits = loadKits(req);
				if (kits.size() > 0) {
					req.getSession().setAttribute(KIT_SESSION_NM, kits.get(0));
				}
				break;
			case Save: 
				saveKit(req);
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
				break;
		default:
			break;
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
						layer.setOrderNo(parent.getSublayers().size());
						parent.addLayer(layer);
					} else {
						layer= kit.getLayers().get(Convert.formatInteger(req.getParameter("index"))).clone();
						layer.setLayerId(new UUIDGenerator().getUUID());
						for (NexusKitLayerVO sublayer : layer.getSublayers()) {
							sublayer.setLayerId(layer.getLayerId());
							sublayer.setLayerId(new UUIDGenerator().getUUID());
						}
						layer.setOrderNo(kit.getLayers().size());
						kit.addLayer(layer);
					}
					break;
				case Product:
					layer = kit.findLayer(req.getParameter("parentId"));
					NexusProductVO p = layer.getProducts().get(Convert.formatInteger(req.getParameter("index"))).clone();
					p.setOrderNo(layer.getProducts().size());
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
			layer.setOrderNo(kit.findLayer(layer.getParentId()).getSublayers().size());
		} else {
			//Since this is a top level layer the parentId needs to be cleared
			layer.setParentId("");
			layer.setOrderNo(kit.getLayers().size());
		}
		
		kit.addLayer(layer);
		
		req.getSession().setAttribute(KIT_SESSION_NM, kit);
	}


	
	/**
	 * Chang which layer the product is kept under
	 * @param req
	 * @throws ActionException
	 */
	private void changeProductLayer(SMTServletRequest req) throws ActionException {
		NexusKitVO kit = (NexusKitVO) req.getSession().getAttribute(KIT_SESSION_NM);
		String parent = req.getParameter("parentId");
		String newParent = req.getParameter("newParentId");
		int index = Convert.formatInteger(req.getParameter("index"));
		
		NexusKitLayerVO layer = kit.findLayer(parent);
		NexusProductVO p = layer.getProducts().get(index);
		layer.getProducts().remove(index);
		kit.findLayer(newParent).addProduct(p);
		
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
		
		// Check wether we are dealing with a sublayer or a top level layer
		if (req.hasParameter("layerId")) {
			NexusKitLayerVO parentLayer = kit.findLayer(req.getParameter("layerId"));
			NexusKitLayerVO layer = parentLayer.getSublayers().get(index);
			parentLayer.getSublayers().remove(index);
			layer.setOrderNo(newIndex);
			parentLayer.getSublayers().add(Convert.formatInteger(req.getParameter("newIndex")), layer);
			changeOrderNo(parentLayer.getSublayers(), index < newIndex? index : newIndex);
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
	private List<NexusKitVO> loadKits(SMTServletRequest req) throws ActionException {
		StringBuilder sql = new StringBuilder(1300);
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		String kitId = StringUtil.checkVal(req.getParameter("kitId"));
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		String profileId;
		if (user == null) {
			profileId = "";
		} else {
			profileId = user.getProfileId();
		}
		// Get the kit, its top layers, and their products
		sql.append("SELECT s.*, sl.*, si.*, p.PROFILE_ID as SHARED_ID FROM ").append(customDb).append("DEPUY_SET_INFO s ");
		sql.append("LEFT JOIN ").append(customDb).append("DEPUY_SET_LAYER sl ");
		sql.append("on sl.SET_INFO_ID = s.SET_INFO_ID and (sl.PARENT_ID is null or sl.PARENT_ID = '') ");
		sql.append("LEFT JOIN ").append(customDb).append("DEPUY_SET_ITEM si ");
		sql.append("on si.LAYER_ID = sl.LAYER_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("DEPUY_PROFILE_SET_XR p ");
		sql.append("on p.SET_INFO_ID = s.SET_INFO_ID ");
		sql.append("WHERE (p.PROFILE_ID = ? or s.PROFILE_ID = ? or s.PROFILE_ID is null) ");
		if (kitId.length() > 0) sql.append("and s.SET_INFO_ID = ? ");
		
		
		sql.append("union ");
		
		// Get the sublayers and their products
		sql.append("SELECT s.*, sl2.*, si.*, p.PROFILE_ID as SHARED_ID FROM ").append(customDb).append("DEPUY_SET_INFO s ");
		sql.append("LEFT JOIN ").append(customDb).append("DEPUY_SET_LAYER sl ");
		sql.append("on sl.SET_INFO_ID = s.SET_INFO_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("DEPUY_SET_LAYER sl2 ");
		sql.append("on sl2.PARENT_ID = sl.LAYER_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("DEPUY_SET_ITEM si ");
		sql.append("on si.LAYER_ID = sl2.LAYER_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("DEPUY_PROFILE_SET_XR p ");
		sql.append("on p.SET_INFO_ID = s.SET_INFO_ID ");
		sql.append("WHERE (p.PROFILE_ID = ? or s.PROFILE_ID = ? or s.PROFILE_ID is null) and sl2.LAYER_ID is not null ");
		if (kitId.length() > 0) sql.append("and s.SET_INFO_ID = ? ");
		sql.append("ORDER BY s.SET_INFO_ID, sl.PARENT_ID, sl.ORDER_NO, si.ORDER_NO ");
		
		log.debug(sql+"|"+profileId+"|"+kitId);
		List<NexusKitVO> kits = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			int i = 1;
			ps.setString(i++, profileId);
			ps.setString(i++, profileId);
			if (kitId.length() > 0) ps.setString(i++, kitId);

			ps.setString(i++, profileId);
			ps.setString(i++, profileId);
			if (kitId.length() > 0) ps.setString(i++, kitId);
			
			ResultSet rs = ps.executeQuery();
			String currentKit = "";
			String currentLayer = "";
			NexusKitVO kit = null;
			NexusKitLayerVO layer = null;
			while(rs.next()) {
				if (!currentKit.equals(rs.getString("SET_INFO_ID"))) {
					log.debug("New Kit");
					if (layer != null) kit.addLayer(layer);
					if (kit != null) kits.add(kit);
					currentKit = rs.getString("SET_INFO_ID");
					kit = new NexusKitVO(rs);
				}

				if (!currentLayer.equals(rs.getString("LAYER_ID"))) {
					if (layer != null) kit.addLayer(layer);
					currentLayer = rs.getString("LAYER_ID");
					layer = new NexusKitLayerVO(rs);
				}
				
				layer.addProduct(new NexusProductVO(rs));
			}
			if (layer != null) kit.addLayer(layer);
			if (kit != null) kits.add(kit);
		} catch (SQLException e) {
			throw new ActionException(e);
		}
		return kits;
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
				if (kit == null) kit = new NexusKitVO();
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
		if(kit == null) kit = new NexusKitVO();
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
		if(kit == null) kit = new NexusKitVO();
		NexusKitLayerVO layer = kit.findLayer(req.getParameter("layerId"));
		if (layer == null) {
			layer = new NexusKitLayerVO(req);
			layer.setLayerId(new UUIDGenerator().getUUID());
			if (StringUtil.checkVal(layer.getParentId()).length() > 0) {
				kit.findLayer(layer.getParentId()).addLayer(layer);
			} else {
				kit.addLayer(layer);
			}
		} else {
			if (!StringUtil.checkVal(req.getParameter("parentId")).equals(layer.getParentId())) {
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
				layer.setData(req);
			}
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
			sql.append("INSERT INTO ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("DEPUY_SET_INFO ");
			sql.append("(SET_SKU_TXT, ORGANIZATION_ID, DESCRIPTION_TXT, GTIN_TXT, BRANCH_PLANT_CD, ");
			sql.append("CREATE_DT, PROFILE_ID, SET_INFO_ID) ");
			sql.append("VALUES(?,?,?,?,?,?,?,?)");
			kit.setKitId(new UUIDGenerator().getUUID());
			insert = true;
		} else {
			sql.append("UPDATE ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("DEPUY_SET_INFO ");
			sql.append("SET SET_SKU_TXT = ?, ORGANIZATION_ID = ?, DESCRIPTION_TXT = ?, GTIN_TXT = ?, BRANCH_PLANT_CD = ?, ");
			sql.append("CREATE_DT = ?, PROFILE_ID = ? WHERE SET_INFO_ID = ? ");
		}
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			int i = 1;
			ps.setString(i++, kit.getKitSKU());
			ps.setString(i++, kit.getOrgId());
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
		
	}
	
	
	/**
	 * Remove all existing products and layers from this kit so that they can
	 * recreated from the kit that is being saved.
	 * @param kitId
	 * @throws ActionException
	 */
	private void clearKit(String kitId) throws ActionException {
		StringBuilder sql = new StringBuilder(150);
		
		sql.append("DELETE ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("DEPUY_SET_LAYER ");
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
		
		sql.append("INSERT INTO ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("DEPUY_SET_LAYER ");
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
		
		insert.append("INSERT INTO ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("DEPUY_SET_ITEM ");
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
		int index = Convert.formatInteger(req.getParameter("index"));
		switch(level) {
			case Layer:
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
				String parent = req.getParameter("parentId");
				NexusKitLayerVO layer = kit.findLayer(parent);
				layer.getProducts().remove(index);
				req.getSession().setAttribute(KIT_SESSION_NM, kit);
				break;
			case Kit:
				StringBuilder sql = new StringBuilder(150);
				
				sql.append("DELETE ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("DEPUY_SET_INFO ");
				sql.append("WHERE SET_INFO_ID in ( ");
				for (int i=0; i < req.getParameterValues("kitId").length -1; i++) sql.append("?,");
				sql.append("?)");
				
				try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
					int i = 1;
					for (String s: req.getParameterValues("kitId")) ps.setString(i++, s);
					
					ps.executeUpdate();
				} catch (SQLException e) {
					throw new ActionException(e);
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
		String kitId = req.getParameter("kitId");
		String profileId = req.getParameter("profileId");
		if (req.hasParameter("delete")) {
			removePermission(profileId, kitId);
		} else {
			addPermission(profileId, kitId);
		}
	}
	
	
	/**
	 * Delete the selected sharing permission
	 * @param req
	 */
	private void removePermission(String profileId, String kitId) throws ActionException {
		StringBuilder sql =  new StringBuilder(150);
		sql.append("DELETE ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("DEPUY_PROFILE_SET_XR ");
		sql.append("WHERE PROFILE_ID = ? and SET_INFO_ID = ? ");
		
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
	private void addPermission(String profileId, String kitId) throws ActionException {
		StringBuilder sql =  new StringBuilder(200);
		sql.append("INSERT INTO ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("DEPUY_PROFILE_SET_XR ");
		sql.append("(PROFILE_SET_ID, PROFILE_ID, SET_INFO_ID, CREATE_DT) ");
		sql.append("VALUES(?,?,?,?)");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, new UUIDGenerator().getUUID());
			ps.setString(2, profileId);
			ps.setString(3, kitId);
			ps.setTimestamp(4, Convert.getCurrentTimestamp());
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
			layers.get(i).setOrderNo(i); 
		}
	}

}
