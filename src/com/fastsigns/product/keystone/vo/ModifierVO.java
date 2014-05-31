package com.fastsigns.product.keystone.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/****************************************************************************
 * <b>Title</b>: ModifierVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Dec 19, 2012
 ****************************************************************************/
public class ModifierVO implements Serializable, Cloneable {
	private static final long serialVersionUID = 2368209900381889160L;
	private String modifier_id = null;
	private String modifier_name = null;
	private String description = null;
	private double price = 0.0;
	private double discount = 0.0;
	private double unit_cost = 0.0;
	private int	quantity = 0;
	private Map<String, AttributeVO> attributes = null;
	private List<String> images = null;
	
	
	public String getModifier_id() {
		return modifier_id;
	}
	public void setModifier_id(String modifier_id) {
		this.modifier_id = modifier_id;
	}
	public String getModifier_name() {
		return modifier_name;
	}
	public void setModifier_name(String modifier_name) {
		this.modifier_name = modifier_name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	public List<String> getImages() {
		return images;
	}
	public void setImages(List<String> images) {
		this.images = images;
	}
	public void addImage(String image) {
		if (this.images == null) 
			images = new ArrayList<String>();
		
		this.images.add(image);
	}
	public Map<String, AttributeVO> getAttributes() {
		return attributes;
	}
	public void setAttributes(Map<String, AttributeVO> attributes) {
		this.attributes = attributes;
	}
	public void addAttribute(AttributeVO vo) {
		if (this.attributes == null) 
			attributes = new LinkedHashMap<String, AttributeVO>();
		
		attributes.put(vo.getModifiers_attribute_id(), vo);
	}

	
	
	public void setPrice(double price) {
		this.price = price;
	}
	public double getPrice() {
		return price;
	}



	public void setDiscount(double discount) {
		this.discount = discount;
	}
	public double getDiscount() {
		return discount;
	}



	public void setUnit_cost(double unit_cost) {
		this.unit_cost = unit_cost;
	}
	public double getUnit_cost() {
		return unit_cost;
	}



	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}
	public int getQuantity() {
		return quantity;
	}

	public ModifierVO clone() throws CloneNotSupportedException {
		ModifierVO mvo = (ModifierVO) super.clone();
		if(attributes != null)
			for(String k : attributes.keySet()) {
				AttributeVO avo = (AttributeVO) attributes.get(k).clone(); 
				mvo.addAttribute(avo);
			}
		return mvo;
		
	}

	public class AttributeVO implements Serializable, Cloneable {
		private static final long serialVersionUID = 5998788902822458889L;
		private String modifiers_attribute_id = null;
		private String attribute_type = null;
		private String attribute_name = null;
		private String value = null;
		private String positioning = null;
		private int attribute_required = 0;
		private Map<String, OptionVO> options = null;
		
		
		public String getModifiers_attribute_id() {
			return modifiers_attribute_id;
		}
		public void setModifiers_attribute_id(String modifiers_attribute_id) {
			this.modifiers_attribute_id = modifiers_attribute_id;
		}
		public String getAttribute_type() {
			return attribute_type;
		}
		public void setAttribute_type(String attribute_type) {
			this.attribute_type = attribute_type;
		}
		public String getAttribute_name() {
			return attribute_name;
		}
		public void setAttribute_name(String attribute_name) {
			this.attribute_name = attribute_name;
		}
		public int getAttribute_required() {
			return attribute_required;
		}
		public void setAttribute_required(int attribute_required) {
			this.attribute_required = attribute_required;
		}
		
		public Map<String, OptionVO> getOptions() {
			return options;
		}
		public void setOptions(Map<String, OptionVO> options) {
			this.options = options;
		}
		public void addOption(OptionVO opt) {
			if (options == null) options = new LinkedHashMap<String, OptionVO>();
			options.put(opt.getModifiers_attributes_options_id(), opt);
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}



		public String getPositioning() {
			return positioning;
		}
		public void setPositioning(String positioning) {
			this.positioning = positioning;
		}

		public AttributeVO clone() throws CloneNotSupportedException {
			AttributeVO avo = (AttributeVO) super.clone();
			if(options != null)
				for(String k : options.keySet()) {
					OptionVO o = options.get(k).clone();
					avo.addOption(o);
				}
			return avo;
			
		}

		public class OptionVO implements Serializable, Cloneable {
			private static final long serialVersionUID = 4447668517756887511L;
			private String modifiers_attributes_options_id = null;
			private String option_name = null;
			private String option_value = null;
			
			
			public String getModifiers_attributes_options_id() {
				return modifiers_attributes_options_id;
			}
			public void setModifiers_attributes_options_id(
					String modifiers_attributes_options_id) {
				this.modifiers_attributes_options_id = modifiers_attributes_options_id;
			}
			public String getOption_name() {
				return option_name;
			}
			public void setOption_name(String option_name) {
				this.option_name = option_name;
			}
			public String getOption_value() {
				return option_value;
			}
			public void setOption_value(String option_value) {
				this.option_value = option_value;
			}
			
			public OptionVO clone() throws CloneNotSupportedException {
				OptionVO ovo = (OptionVO) super.clone();
				return ovo;
			}
		}
	}
}
