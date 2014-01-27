package com.fastsigns.product.keystone.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: ProductVO.java<p/>
 * <b>Description: 
 * 		Extends the ProductVO in SMTBaseLibs - used for commerce api</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Oct 23, 2012
 ****************************************************************************/
public class KeystoneProductVO extends ProductVO implements Serializable, Cloneable {
	private static final long serialVersionUID = 2308239428232094613L;
	//private String product_id = null;
	private String franchise_id = null;
	private String lookup_name = null;
	private String display_name = null;
	//private int active = 0;
	private int taxable = 0;
	//private int ecommerce = 0;
	//private int pos = 0;
	//private int double_sided = 0;
	//private String revenue_location_id = null;
	//private String pricing_method = null;
	private String description = null;
    private String web_description = null;
	//private String corporate_id = null;
	private int dsolable = 0;
    //private String sku = null;
    //private String upc = null;
    private int default_unit = 0;
	//private String catalog_type = null;
	private int tax_code_id = 0;
	private String usageId = null;
	private String name = null;
    private String image_id = null;
    private String imageThumbUrl = null;
    private String imageUrl = null;
    private double weight = 0;
    private double discount = 0.0;
    private List<SizeVO> sizes = null;
    private List<ImageVO> images = null;
    private Map<String, ModifierVO> modifiers = null;
    
    
    public KeystoneProductVO() {
    }
    
    /**
     * Used to Set DSOL Information off request.
     * @param req
     */
    public KeystoneProductVO(SMTServletRequest req) {
    	
		/*
		 * basic data
		 */
		setDescription(req.getParameter("dsolDescText"));
		setCatalogId(req.getParameter("catalog"));
		addProdAttribute("category", req.getParameter("category"));
		setProductName(req.getParameter("dsolProdName"));
		setProduct_id(req.getParameter("dsolItemId"));
		addProdAttribute("dsolItemId", req.getParameter("dsolItemId"));
		setProductId(req.getParameter("dsolProductId"));
		setUsageId(req.getParameter("usageId"));
		
		/*
		 * Special Data
		 */
		if(req.hasParameter("highResData"))
			addProdAttribute("highResData", req.getParameter("highResData").replace("data:image/png;base64,", ""));
		if(req.hasParameter("thumbnailData"))
			addProdAttribute("thumbnailData", req.getParameter("thumbnailData").replace("data:image/png;base64,", ""));
		if(req.hasParameter("jsonData"))
			addProdAttribute("jsonData", req.getParameter("jsonData"));
		if(req.hasParameter("svgData"))
			addProdAttribute("svgData", req.getParameter("svgData"));
		
		/*
		 * KeystoneProductVO Fields
		 */
		setImageThumbUrl(req.getParameter("thumbnailData"));
		setImageUrl(req.getParameter("thumbnailData"));
		setWeight(Convert.formatInteger(req.getParameter("weight")));
		/*
		 * Size Data
		 */
		SizeVO s = new SizeVO();
		s.setEcommerce_size_id(req.getParameter("ecommerce_size_id"));
		s.setSelected(1);
		s.setDimensions(Convert.formatInteger(req.getParameter("heightInches")) + "x" + Convert.formatInteger(req.getParameter("widthInches")));
		
		String[] st = req.getParameter("dimensions").split(" x ");
		s.setWidth_pixels(Integer.parseInt(st[0]) * 72);
		s.setHeight_pixels(Integer.parseInt(st[1]) * 72);
		
		List<SizeVO> sizes = new ArrayList<SizeVO>();
		sizes.add(s);
		setSizes(sizes);
	}

    /*
     * wrapper for JSON translation
     */
	public String getProduct_id() {
		return super.getProductId();
	}

	/*
     * wrapper for JSON translation
     */
	public void setProduct_id(String product_id) {
		super.setProductId(product_id);
	}

	public String getFranchise_id() {
		return franchise_id;
	}

	public void setFranchise_id(String franchise_id) {
		this.franchise_id = franchise_id;
	}

	public Integer getDsolable() {
		return dsolable;
	}

	public void setDsolable(Integer dsolable) {
		this.dsolable = dsolable;
	}

	public String getDisplay_name() {
		return display_name;
	}

	public void setDisplay_name(String display_name) {
		this.display_name = display_name;
	}

	public String getWeb_description() {
		return web_description;
	}

	public void setWeb_description(String web_description) {
		this.web_description = web_description;
	}

	public String getImage_id() {
		return image_id;
	}

	public void setImage_id(String image_id) {
		this.image_id = image_id;
	}

	public String getImageThumbUrl() {
		return imageThumbUrl;
	}

	public void setImageThumbUrl(String imageThumbUrl) {
		this.imageThumbUrl = imageThumbUrl;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public List<SizeVO> getSizes() {
		return sizes;
	}

	public void setSizes(List<SizeVO> sizes) {
		this.sizes = sizes;
	}

	public Map<String, ModifierVO> getModifiers() {
		return modifiers;
	}

	public void setModifiers(Map<String, ModifierVO> modifiers) {
		this.modifiers = modifiers;
	}

	public void addModifier(ModifierVO vo) {
		if (this.modifiers == null) modifiers = new LinkedHashMap<String, ModifierVO>();
		modifiers.put(vo.getModifier_id(), vo);
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getLookup_name() {
		return lookup_name;
	}

	public void setLookup_name(String lookup_name) {
		this.lookup_name = lookup_name;
	}

	public List<ImageVO> getImages() {
		return images;
	}

	public void setImages(List<ImageVO> images) {
		this.images = images;
	}
	
	/*
     * needed wrapper for JSON translation
     */
	public void setDsol_item_id(String dsol_item_id) {
		super.setProductId(dsol_item_id);
	}
	
	public void setDsol_template_id(String dsol_template_id) {
		addProdAttribute("dsol_template_id", dsol_template_id);
	}
	
	public void setTemplate_data(String template_data) {
		addProdAttribute("jsonData", template_data);
	}

	public String getUsageId() {
		return usageId;
	}

	public void setUsageId(String usageId) {
		this.usageId = usageId;
	}

	/**
	 * @param template_name the template_name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the template_name
	 */
	public String getName() {
		return name;
	}

	public int getTax_code_id() {
		return tax_code_id;
	}

	public void setTax_code_id(int tax_code_id) {
		this.tax_code_id = tax_code_id;
	}

	public int getTaxable() {
		return taxable;
	}

	public void setTaxable(int taxable) {
		this.taxable = taxable;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	public double getWeight() {
		return weight;
	}

	public void setDiscount(double discount) {
		this.discount = discount;
	}

	public double getDiscount() {
		return discount;
	}

	public int getDefault_unit() {
		return default_unit;
	}

	public void setDefault_unit(int default_unit) {
		this.default_unit = default_unit;
	}
	
	public KeystoneProductVO clone() throws CloneNotSupportedException {
		KeystoneProductVO kpv = (KeystoneProductVO) super.clone();
		
		if(sizes != null) {
			List<SizeVO> sL = new ArrayList<SizeVO>();
			for(SizeVO s : sizes)
				sL.add(s.clone());
			kpv.setSizes(sL);
		}
		if(images != null) {
			List<ImageVO> iL = new ArrayList<ImageVO>();
			for(ImageVO i : images)
				iL.add(i.clone());
			kpv.setImages(iL);
		}
		if(modifiers != null)
			for(String k : modifiers.keySet())
				kpv.addModifier(modifiers.get(k).clone());
		
		return kpv;
	}
}
