package com.sas.action.product;

// JDK 1.6
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

// DOM4J
import org.dom4j.DocumentException;

// SMT BAse Libs
import com.sas.util.SASRoleModule;
import com.sas.util.WebServiceAction;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.commerce.OrderCompleteVO;
import com.siliconmtn.commerce.ShippingInfoVO;
import com.siliconmtn.commerce.ShoppingCartItemVO;
import com.siliconmtn.commerce.ShoppingCartVO;
import com.siliconmtn.commerce.cart.storage.Storage;
import com.siliconmtn.commerce.cart.storage.StorageFactory;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.commerce.payment.PaymentVO;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: ShoppingCartAction.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Aug 2, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class ShoppingCartAction extends SBActionAdapter {
	
	public static final int SESSION_PERSISTENCE_CART = 1;
	public static final int COOKIE_PERSISTENCE_CART = 2;

	/**
	 * 
	 */
	public ShoppingCartAction() {
		
	}

	/**
	 * @param actionInit
	 */
	public ShoppingCartAction(ActionInitVO actionInit) {
		super(actionInit);
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("Building");
		
		// Retrieve the cart
		ShoppingCartVO cart = null;
		try {
			cart = this.manageCart(req);
			
			// Store the cart onto the request object for display
			this.putModuleData(cart, cart.getSize(), false);
		} catch (Exception e) {
			log.error("Unable to manage cart", e);
		}
		
		// Make sure to set the parameter for the checkout process
		req.setParameter("checkout", "true");
	}
	
	/**
	 * 
	 * @param cart
	 * @param req
	 */
	protected void manageShippingInfo(ShoppingCartVO cart, SMTServletRequest req) {
		String shippingType = StringUtil.checkVal(req.getParameter("shippingType"));
		UserDataVO user = new UserDataVO(req);
		
		if ("billing".equalsIgnoreCase(shippingType)) {
			cart.setBillingInfo(user);
			
			// Add the user to the session
			req.getSession().setAttribute(Constants.USER_DATA, user);
			
			if (Convert.formatBoolean(req.getParameter("useBilling"))) {
				cart.setShippingInfo(user);
				req.setParameter("type", "payment");
				
			} else { 
				req.setParameter("type", "Shipping");
			}
		} else {
			// Add the user to the session
			((UserDataVO)req.getSession().getAttribute(Constants.USER_DATA)).setUserExtendedInfo(user);
			
			// Update the shipping info
			cart.setShippingInfo(user);
			req.setParameter("type", "payment");
			
			// retrieve the shipping options and the sales tax
			try {
				this.calcTaxes(cart);
			} catch (Exception e) {
				log.error("Unable to calculate taxes", e);
			}
		}
		
		// If the user changed the shipping/billing info, have them return to the payment screen
		if (Convert.formatBoolean(req.getParameter("edit")))
			req.setParameter("type", "payment");
	}
	
	/**
	 * 
	 * @param cart
	 * @throws IOException
	 * @throws DocumentException
	 */
	public void calcTaxes(ShoppingCartVO cart) throws IOException, DocumentException {
		WebServiceAction wsa = new WebServiceAction(this.actionInit);
		wsa.setAttributes(attributes);
		double tax = wsa.retrieveTaxInfo(cart.getShippingInfo(), cart.getItems().values(), "");
		cart.setTaxAmount(tax);
	}
	
	/**
	 * 
	 * @param cart
	 * @throws IOException
	 * @throws DocumentException
	 */
	public void calcShippingCosts(ShoppingCartVO cart) 
	throws IOException, DocumentException {
		ShippingInfoVO sVo = cart.getShipping(); 
		String methodId = "";
		if (sVo != null) methodId = sVo.getShippingMethodId();
		
		WebServiceAction wsa = new WebServiceAction(this.actionInit);
		wsa.setAttributes(attributes);
		if (cart.getShippingInfo() != null) {

			Map<String, ShippingInfoVO> sh = wsa.retrieveShippingInfo(cart.getShippingInfo().getZipCode(), cart.getProductCountById());
			cart.setShippingOptions(sh);
			if (methodId.length() == 0) methodId = cart.getShipping().getShippingMethodId();
			cart.setShipping(methodId);
		}
	}
	
	/**
	 * 
	 * @param cart
	 * @param pc
	 * @throws IOException
	 * @throws DocumentException
	 */
	public void applyCoupon(ShoppingCartVO cart, String pc) 
	throws IOException, DocumentException {
		WebServiceAction wsa = new WebServiceAction(this.actionInit);
		wsa.setAttributes(attributes);
		double discount = wsa.retrievePromotionDiscount(cart.getItems().values(), pc);
		cart.setPromotionDiscount(discount);
		cart.setPromotionCode(pc);
	}

	/**
	 * 
	 * @param cart
	 * @param pc
	 * @throws IOException
	 * @throws DocumentException
	 */
	public void payForOrder(ShoppingCartVO cart, SMTServletRequest req) 
	throws IOException, DocumentException {
		WebServiceAction wsa = new WebServiceAction(this.actionInit);
		wsa.setAttributes(attributes);
		OrderCompleteVO ocvo = wsa.placeOrder(cart, req.getRemoteAddr());
		cart.setOrderComplete(ocvo);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(SMTServletRequest req) throws ActionException {
		String orgId = ((SiteVO)req.getAttribute(Constants.SITE_DATA)).getOrganizationId();
		
		// Process the cart
		SBModuleVO module;
		try {
			// Get the login data if passed in
			checkAuth(req);
			
			module = this.retrieveModuleData(orgId, actionInit.getActionId());
			req.setAttribute("shoppingCartModule", module);
			
			// Add the data to the moduleData
			ShoppingCartVO cart = this.manageCart(req);
			this.putModuleData(cart, cart.getSize(), false);
		} catch (Exception e) {
			log.error("Unable to retrieve cart", e);
		}
	}
	
	/**
	 * 
	 * @param req
	 */
	public void checkAuth(SMTServletRequest req) {
		if (! Convert.formatBoolean(req.getParameter("login"))) return;
		if (Convert.formatBoolean(req.getParameter("bypass"))) return;
		
		WebServiceAction wsa = new WebServiceAction(this.actionInit);
		wsa.setAttributes(attributes);
		String loginName = req.getParameter("emailAddress");
		String password = StringUtil.checkVal(req.getParameter("password"));
		UserDataVO user;
		try {
			user = wsa.authenticateMember(loginName, password);
			String siteId = ((SiteVO) req.getAttribute(Constants.SITE_DATA)).getSiteId();
			SASRoleModule role = new SASRoleModule();
			req.getSession().setAttribute(Constants.USER_DATA, user);
			req.getSession().setAttribute(Constants.ROLE_DATA, role.getUserRole(user.getProfileId(), siteId));
		} catch (Exception e) {
			req.setParameter("authLoginFailed", "true");
			req.setParameter("login", "false");
		} 
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	public void list(SMTServletRequest req) throws ActionException {
		SBModuleVO vo = null;
		String msg = (String)getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		
		try {
			String orgId = req.getParameter("organizationId");
			String sbActionId = req.getParameter(SB_ACTION_ID);
			vo = this.retrieveModuleData(orgId, sbActionId);

		} catch (SQLException e) {
			log.error("Error listing shopping cart portlet", e);
			msg = (String)getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
		}
		
		this.putModuleData(vo, 1, true, msg);
	}
	
	/**
	 * 
	 * @param req
	 * @return
	 * @throws ActionException
	 * @throws DocumentException 
	 * @throws IOException 
	 */
	public ShoppingCartVO manageCart(SMTServletRequest req) 
	throws ActionException, IOException, DocumentException {
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		String productId = StringUtil.checkVal(req.getParameter("productId"));
		
		Map<String, Object> attrs = new HashMap<String, Object>();
		attrs.put(GlobalConfig.HTTP_REQUEST, req);
		attrs.put(GlobalConfig.HTTP_RESPONSE, attributes.get(GlobalConfig.HTTP_RESPONSE));
		
		// Load the cart from our Storage medium.
		Storage container = null;
		String classPath = StorageFactory.SESSION_STORAGE;
		int type = Convert.formatInteger(mod.getAttribute(SBModuleVO.ATTRIBUTE_1) + "");
		if (type == COOKIE_PERSISTENCE_CART) {
			classPath = StorageFactory.PERSISTENT_STORAGE;
			attrs.put(GlobalConfig.KEY_DB_CONN, dbConn);
		}
		
		try {
			container = StorageFactory.getInstance(classPath, attrs);
		} catch (Exception e) {
			throw new ActionException(e);
		}
		
		
		ShoppingCartVO cart = container.load();
		log.debug("Is cart new: " + container.isNewCart());
		if (productId.length() == 0 && container.isNewCart()) return cart;
		if (cart.isOrderCompleted() && productId.length() == 0) return cart;
		
		// See if there is data on the session to assign from login
		UserDataVO user = (UserDataVO)req.getSession().getAttribute(Constants.USER_DATA);
		if (cart.getBillingInfo() == null && user != null) {
			cart.setBillingInfo(user);
			cart.setShippingInfo((UserDataVO) user.getUserExtendedInfo());
			
			// Recalculate the taxes
			this.calcTaxes(cart);
		}
		
		// Get the product information and add it to the cart.  Note, if the 
		// quantity is being updated, the new "Total" quantity must be passed here
		boolean itemRemove = Convert.formatBoolean(req.getParameter("itemRemove"));
		if (productId.length() > 0 && ! itemRemove) {
			// Create a new cart if the order is complete and the user is trying 
			// to add a product to the cart (we'll assume that they are trying to 
			// Create a new cart.
			if (cart.isOrderCompleted()) {
				container.flush();
				cart = container.load();
			}
			
			SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
			ShoppingCartItemVO item = this.getProductInfo(productId, site.getOrganizationId());
			
			if (item != null) {
				item.setQuantity(Convert.formatInteger(req.getParameter("qty")));
				cart.add(item);
				
				log.debug("cart: " + cart + "|" + cart.getSize());
				
				// Recalculate the taxes
				this.calcTaxes(cart);
				
				// recalculate the shipping options
				calcShippingCosts(cart);
				
				// Apply the coupon again
				this.applyCoupon(cart, cart.getPromotionCode());
			}
			
		} else if (itemRemove) {
			cart.remove(productId);
			
			// Recalculate the taxes
			this.calcTaxes(cart);
			
			// recalculate the shipping options
			calcShippingCosts(cart);
			
			// Apply the coupon again
			this.applyCoupon(cart, cart.getPromotionCode());
			
		} else if (Convert.formatBoolean(req.getParameter("promoSub"))) {
			// Get the promotion code
			this.applyCoupon(cart, req.getParameter("promoCode"));
			
			// Recalculate the taxes
			this.calcTaxes(cart);
		} else if (Convert.formatBoolean(req.getParameter("finalCheckout"))) {
			String encKey = (String)this.getAttribute(Constants.ENCRYPT_KEY);
			PaymentVO payment = new PaymentVO(encKey);
			payment.setExpirationMonth(req.getParameter("expMonth"));
			payment.setExpirationYear(req.getParameter("expYear"));
			payment.setPaymentName(req.getParameter("nameOnCard"));
			payment.setPaymentCode(req.getParameter("securityNumber"));
			payment.setPaymentNumber(req.getParameter("creditCardNumber"));
			
			cart.setPayment(payment);
			
			// Call the checkout process
			this.payForOrder(cart, req);
		} else if (Convert.formatBoolean(req.getParameter("updateShipping"))) {
			String shippingId = req.getParameter("selShipping");
			cart.setShipping(shippingId);
		}
		
		// If the request is for shipping manage the data
		if (StringUtil.checkVal(req.getParameter("shippingType")).length() > 0) {
			this.manageShippingInfo(cart, req);

			// Recalculate the taxes
			this.calcTaxes(cart);
			
			// recalculate the shipping options
			calcShippingCosts(cart);
		}
		
		if (Convert.formatBoolean(req.getParameter("checkout")) && cart.getShippingOptions().size() < 2)
			this.calcShippingCosts(cart);
			
		// Resave the cart for persistence reasons
		container.save(cart);
		
		return cart;
	}
	
	/**
	 * 
	 * @throws SQLException
	 */
	public SBModuleVO retrieveModuleData(String orgId, String sbActionId) throws SQLException {

		String s = "select * from sb_action where organization_id = ? and action_id = ? ";
		PreparedStatement ps = dbConn.prepareStatement(s);
		ps.setString(1, orgId);
		ps.setString(2, sbActionId);
		ResultSet rs = ps.executeQuery();
		
		SBModuleVO module = new SBModuleVO();
		if (rs.next()) {
            module = new SBModuleVO();
            module.setActionId(rs.getString("action_id"));
            module.setModuleTypeId(rs.getString("module_type_id"));
            module.setActionName(rs.getString("action_nm"));
            module.setActionDesc(rs.getString("action_desc"));
            module.setOrganizationId(rs.getString("organization_id"));
            module.setAttribute(SBModuleVO.ATTRIBUTE_1, rs.getString("attrib1_txt"));
            module.setAttribute(SBModuleVO.ATTRIBUTE_2, rs.getString("attrib2_txt"));
            module.setIntroText(rs.getString("intro_txt"));
            module.setPendingSyncFlag(rs.getInt("pending_sync_flg"));
            module.setActionGroupId(rs.getString("action_group_id"));
		}
		
		ps.close();
		return module;
	}
	/**
	 * 
	 * @param productId
	 * @param orgId
	 * @return
	 */
	public ShoppingCartItemVO getProductInfo(String productId, String orgId) {
		ShoppingCartItemVO vo = null;
		String s = "select * from product a inner join product_catalog b ";
		s += "on a.product_catalog_id = b.product_catalog_id and organization_id = ? ";
		s += "where cust_product_no = ? AND a.PRODUCT_GROUP_ID IS NULL ";
		PreparedStatement ps = null;
		
		try {
			ps = dbConn.prepareStatement(s);
			ps.setString(1, orgId);
			ps.setString(2, productId);

			ResultSet rs = ps.executeQuery();
			vo = new ShoppingCartItemVO();
			if (rs.next()) {
				//vo.setProductId(rs.getString("product_id"));
				vo.setProductId(rs.getString("cust_product_no"));
				vo.setBasePrice(rs.getDouble("msrp_cost_no"));
				vo.setDescription(rs.getString("desc_txt"));
				vo.setProductName(rs.getString("product_nm"));
				vo.setProductCategory(getCategory(rs.getString("product_id")));
				ProductVO product = new ProductVO(rs);
				vo.setProduct(product);
			}
		} catch (SQLException sqle) {
			log.error("Unable to retrieve product", sqle);
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {}
		}
		
		return vo;
	}
	
	/**
	 * 
	 * @param productId
	 * @return
	 */
	private String getCategory(String productId) {
		StringBuilder s = new StringBuilder();
		
		s.append("with categories (parent_cd, product_category_cd, category_nm, category_desc, level) as ( ");
		s.append("select parent_cd, a.product_category_cd, a.category_nm, a.category_desc, 0 ");
		s.append("from dbo.product_category a ");
		s.append("inner join product_category_xr b on a.product_category_cd = b.product_category_cd ");
		s.append("where b.product_id = ? ");
		s.append("union all ");
		s.append("select c.parent_cd, c.product_category_cd,c.category_nm, c.category_desc, level + 1 ");
		s.append("from product_category c ");
		s.append("inner join categories pc on pc.parent_cd  = c.product_category_cd ");
		s.append(") ");
		s.append("select category_nm from categories order by level desc; ");
		
		PreparedStatement ps = null;
		String category = "";
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setString(1, productId);
			ResultSet rs = ps.executeQuery();
			for (int i=0; rs.next(); i++) {
				String cat = rs.getString(1);
				
				if (i == 0)
					category += cat;
				else
					category += " > " + cat;
			}
		} catch (SQLException sqle) {
			log.error("Unable to retrieve product", sqle);
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {}
		}
		
		return category;
	}
}
