package com.ecrs.vusionesl.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Item data received from Catapult system via webhook POST.
 * Contains item-level fields and a list of store-specific pricing data.
 */
public class CatapultItem {
    
    @SerializedName("recordId")
    private String recordId;
    
    @SerializedName("itemId")
    private String itemId;  // UPC code
    
    @SerializedName("itemName")
    private String itemName;
    
    @SerializedName("receiptAlias")
    private String receiptAlias;
    
    @SerializedName("brand")
    private String brand;
    
    @SerializedName("size")
    private String size;
    
    @SerializedName("sizeUnit")
    private String sizeUnit;
    
    @SerializedName("sizeQty")
    private Double sizeQty;
    
    // Department info
    @SerializedName("deptNumber")
    private Integer deptNumber;
    
    @SerializedName("deptName")
    private String deptName;
    
    @SerializedName("subDeptNumber")
    private Integer subDeptNumber;
    
    @SerializedName("subDeptName")
    private String subDeptName;
    
    // Power fields (custom fields)
    @SerializedName("powerField1")
    private String powerField1;
    
    @SerializedName("powerField2")
    private String powerField2;
    
    @SerializedName("powerField3")
    private String powerField3;
    
    @SerializedName("powerField4")
    private String powerField4;
    
    @SerializedName("powerField5")
    private String powerField5;  // WHItem (warehouse item number)
    
    @SerializedName("powerField6")
    private String powerField6;
    
    @SerializedName("powerField7")
    private String powerField7;
    
    @SerializedName("powerField8")
    private String powerField8;
    
    // Store-specific data (pricing, status)
    @SerializedName("stores")
    private List<CatapultStoreData> stores;
    
    // Ordering information
    @SerializedName("ordering")
    private List<Object> ordering;
    
    // Getters
    
    public String getRecordId() {
        return recordId;
    }
    
    public String getItemId() {
        return itemId;
    }
    
    public String getItemName() {
        return itemName;
    }
    
    public String getReceiptAlias() {
        return receiptAlias;
    }
    
    public String getBrand() {
        return brand;
    }
    
    public String getSize() {
        return size;
    }
    
    public String getSizeUnit() {
        return sizeUnit;
    }
    
    public Double getSizeQty() {
        return sizeQty;
    }
    
    public Integer getDeptNumber() {
        return deptNumber;
    }
    
    public String getDeptName() {
        return deptName;
    }
    
    public Integer getSubDeptNumber() {
        return subDeptNumber;
    }
    
    public String getSubDeptName() {
        return subDeptName;
    }
    
    public String getPowerField1() {
        return powerField1;
    }
    
    public String getPowerField2() {
        return powerField2;
    }
    
    public String getPowerField3() {
        return powerField3;
    }
    
    public String getPowerField4() {
        return powerField4;
    }
    
    /**
     * Gets PowerField5 which contains the warehouse item number (WHItem).
     */
    public String getPowerField5() {
        return powerField5;
    }
    
    /**
     * Alias for getPowerField5() - warehouse item number.
     */
    public String getWHItem() {
        return powerField5;
    }
    
    public String getPowerField6() {
        return powerField6;
    }
    
    public String getPowerField7() {
        return powerField7;
    }
    
    public String getPowerField8() {
        return powerField8;
    }
    
    public List<CatapultStoreData> getStores() {
        return stores;
    }
    
    public List<Object> getOrdering() {
        return ordering;
    }
    
    /**
     * Finds store data for a specific store number.
     *
     * @param storeNumber The store number to find (e.g., "RS1")
     * @return The store data, or null if not found
     */
    public CatapultStoreData getStoreData(String storeNumber) {
        if (stores == null || storeNumber == null) {
            return null;
        }
        for (CatapultStoreData store : stores) {
            if (storeNumber.equals(store.getStoreNumber())) {
                return store;
            }
        }
        return null;
    }
    
    @Override
    public String toString() {
        return "CatapultItem{" +
            "itemId='" + itemId + '\'' +
            ", itemName='" + itemName + '\'' +
            ", brand='" + brand + '\'' +
            ", size='" + size + '\'' +
            ", deptName='" + deptName + '\'' +
            ", stores=" + (stores != null ? stores.size() : 0) + " stores" +
            '}';
    }
}
