package com.ecrs.vusionesl.model;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

/**
 * Item data to be sent to Vusion ESL API.
 * Contains core fields and a custom map for additional data.
 */
public class VusionItem {
    
    @SerializedName("id")
    private String id;  // UPC code
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("price")
    private Double price;  // Unit price (price1 / divider1)
    
    @SerializedName("brand")
    private String brand;
    
    @SerializedName("capacity")
    private String capacity;  // Size (e.g., "7 OZ")
    
    @SerializedName("custom")
    private Map<String, String> custom;  // Vusion requires all custom values to be strings
    
    public VusionItem() {
        this.custom = new HashMap<>();
    }
    
    // Builder-style setters for fluent API
    
    public VusionItem setId(String id) {
        this.id = id;
        return this;
    }
    
    public VusionItem setName(String name) {
        this.name = name;
        return this;
    }
    
    public VusionItem setPrice(Double price) {
        this.price = price;
        return this;
    }
    
    public VusionItem setBrand(String brand) {
        this.brand = brand;
        return this;
    }
    
    public VusionItem setCapacity(String capacity) {
        this.capacity = capacity;
        return this;
    }
    
    /**
     * Adds a custom field to the item.
     * All values are converted to strings as required by Vusion API.
     * Numbers ending in .0 are formatted as integers (e.g., 1.0 -> "1").
     *
     * @param key   The custom field name
     * @param value The custom field value (will be converted to string)
     * @return this item for chaining
     */
    public VusionItem addCustomField(String key, Object value) {
        if (value != null) {
            this.custom.put(key, formatValue(value));
        }
        return this;
    }
    
    /**
     * Adds multiple custom fields to the item.
     * All values are converted to strings as required by Vusion API.
     *
     * @param fields Map of custom fields to add
     * @return this item for chaining
     */
    public VusionItem addCustomFields(Map<String, ?> fields) {
        if (fields != null) {
            for (Map.Entry<String, ?> entry : fields.entrySet()) {
                if (entry.getValue() != null) {
                    this.custom.put(entry.getKey(), formatValue(entry.getValue()));
                }
            }
        }
        return this;
    }
    
    /**
     * Formats a value as a clean string.
     * Numbers ending in .0 are formatted as integers (e.g., 1.0 -> "1").
     */
    private String formatValue(Object value) {
        if (value instanceof Double) {
            Double d = (Double) value;
            // If it's a whole number, format without decimal
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                return String.valueOf(d.longValue());
            }
            return String.valueOf(d);
        } else if (value instanceof Float) {
            Float f = (Float) value;
            if (f == Math.floor(f) && !Float.isInfinite(f)) {
                return String.valueOf(f.longValue());
            }
            return String.valueOf(f);
        }
        return String.valueOf(value);
    }
    
    // Getters
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public Double getPrice() {
        return price;
    }
    
    public String getBrand() {
        return brand;
    }
    
    public String getCapacity() {
        return capacity;
    }
    
    public Map<String, String> getCustom() {
        return custom;
    }
    
    /**
     * Gets a custom field value.
     *
     * @param key The custom field name
     * @return The value, or null if not set
     */
    public String getCustomField(String key) {
        return custom.get(key);
    }
    
    @Override
    public String toString() {
        return "VusionItem{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", price=" + price +
            ", brand='" + brand + '\'' +
            ", capacity='" + capacity + '\'' +
            ", customFields=" + custom.size() +
            '}';
    }
}
