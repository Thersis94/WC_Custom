package com.fastsigns.product.keystone.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;

public class DSOLVO extends ProductDetailVO implements Serializable{
	
	private static final long serialVersionUID = -308115322150643672L;
	
	private String templateId = null;
	private String templateNm = null;
	private String productTemplateId = null;
	private int widthPixels = 0;
	private int heightPixels = 0;

	public DSOLVO() {
		
	}
	
	public DSOLVO(SMTServletRequest req) {
		/*
		 * probably not needed.
		 */
		templateId = req.getParameter("templateId");
		templateNm = req.getParameter("templateNm");
		productTemplateId = req.getParameter("productTemplateId");
		
		/*
		 * basic data
		 */
		setDescText(req.getParameter("description"));
		setCatalogId(req.getParameter("catalog"));
		setProduct_id(req.getParameter("dsolItemId"));
		
		/*
		 * Special Data
		 */
		addProdAttribute("highResData", req.getParameter("highResData").replace("data:image/png;base64,", ""));
		addProdAttribute("thumbnailData", req.getParameter("thumbnailData").replace("data:image/png;base64,", ""));
		addProdAttribute("jsonData", req.getParameter("jsonData"));
		addProdAttribute("svgData", req.getParameter("svgData"));
		
		/*
		 * KeystoneProductVO Fields
		 */
		setImageThumbUrl(req.getParameter("thumbnailData"));
		setImageUrl(req.getParameter("thumbnailData"));
		
		/*
		 * Size Data
		 */
		SizeVO s = new SizeVO();
		s.setEcommerce_size_id("ecommerce_size_id");
		s.setSelected(1);
		s.setDimensions(Convert.formatInteger(req.getParameter("heightInches")) + "x" + Convert.formatInteger(req.getParameter("widthInches")));
		List<SizeVO> sizes = new ArrayList<SizeVO>();
		sizes.add(s);
		super.setSizes(sizes);
	}

	/**
	 * @return the templateId
	 */
	public String getTemplateId() {
		return templateId;
	}

	/**
	 * @param templateId the templateId to set
	 */
	public void setTemplateId(String templateId) {
		this.templateId = templateId;
	}

	/**
	 * @return the templateNm
	 */
	public String getTemplateNm() {
		return templateNm;
	}

	/**
	 * @param templateNm the templateNm to set
	 */
	public void setTemplateNm(String templateNm) {
		this.templateNm = templateNm;
	}

	/**
	 * @return the productTemplateId
	 */
	public String getProductTemplateId() {
		return productTemplateId;
	}

	/**
	 * @param productTemplateId the productTemplateId to set
	 */
	public void setProductTemplateId(String productTemplateId) {
		this.productTemplateId = productTemplateId;
	}

	/**
	 * @return the widthPixels
	 */
	public int getWidthPixels() {
		return widthPixels;
	}

	/**
	 * @param widthPixels the widthPixels to set
	 */
	public void setWidthPixels(int widthPixels) {
		this.widthPixels = widthPixels;
	}

	/**
	 * @return the heightPixels
	 */
	public int getHeightPixels() {
		return heightPixels;
	}

	/**
	 * @param heightPixels the heightPixels to set
	 */
	public void setHeightPixels(int heightPixels) {
		this.heightPixels = heightPixels;
	}
	
	public void setTemplate_data(String template_data) {
		addProdAttribute("jsonData", template_data);
	}
}
