package com.ecrs.vusionesl.model;

import com.google.gson.annotations.SerializedName;

/**
 * Store-level pricing and status data from Catapult.
 * Each item can have different pricing per store.
 */
public class CatapultStoreData {
    
    @SerializedName("recordId")
    private String recordId;
    
    @SerializedName("storeName")
    private String storeName;
    
    @SerializedName("storeNumber")
    private String storeNumber;
    
    @SerializedName("deleted")
    private Boolean deleted;
    
    @SerializedName("discontinued")
    private Boolean discontinued;
    
    // Pricing fields
    @SerializedName("price1")
    private Double price1;
    
    @SerializedName("divider1")
    private Integer divider1;
    
    @SerializedName("promoPrice1")
    private Double promoPrice1;
    
    @SerializedName("promoDivider1")
    private Integer promoDivider1;
    
    // Promo date fields
    @SerializedName("promoStart")
    private String promoStart;
    
    @SerializedName("promoEnd")
    private String promoEnd;
    
    // User-assigned fields
    @SerializedName("userAssigned1")
    private String userAssigned1;
    
    @SerializedName("userAssigned2")
    private String userAssigned2;
    
    @SerializedName("userAssigned3")
    private String userAssigned3;
    
    @SerializedName("userAssigned4")
    private String userAssigned4;
    
    @SerializedName("userAssigned5")
    private String userAssigned5;
    
    @SerializedName("userAssigned6")
    private String userAssigned6;
    
    @SerializedName("userAssigned7")
    private String userAssigned7;
    
    // Local power fields (store-specific)
    @SerializedName("localPowerField1")
    private String localPowerField1;
    
    @SerializedName("localPowerField2")
    private String localPowerField2;
    
    @SerializedName("localPowerField3")
    private String localPowerField3;
    
    @SerializedName("localPowerField4")
    private String localPowerField4;
    
    @SerializedName("localPowerField5")
    private String localPowerField5;
    
    @SerializedName("localPowerField6")
    private String localPowerField6;
    
    @SerializedName("localPowerField7")
    private String localPowerField7;
    
    @SerializedName("localPowerField8")
    private String localPowerField8;
    
    // Description fields
    @SerializedName("descLine1")
    private String descLine1;
    
    @SerializedName("descLine2")
    private String descLine2;
    
    // Weight/tare fields
    @SerializedName("weight")
    private Double weight;
    
    @SerializedName("unitOfMeasure")
    private String unitOfMeasure;
    
    @SerializedName("fixedWeightAmt")
    private Double fixedWeightAmt;
    
    @SerializedName("fixedTare")
    private Double fixedTare;
    
    @SerializedName("percentTare")
    private Double percentTare;
    
    @SerializedName("tareType")
    private String tareType;
    
    // Other fields
    @SerializedName("ingredients")
    private String ingredients;
    
    @SerializedName("shelfLife")
    private Integer shelfLife;
    
    // Getters
    
    public String getRecordId() {
        return recordId;
    }
    
    public String getStoreName() {
        return storeName;
    }
    
    public String getStoreNumber() {
        return storeNumber;
    }
    
    public Boolean getDeleted() {
        return deleted;
    }
    
    public boolean isDeleted() {
        return Boolean.TRUE.equals(deleted);
    }
    
    public Boolean getDiscontinued() {
        return discontinued;
    }
    
    public boolean isDiscontinued() {
        return Boolean.TRUE.equals(discontinued);
    }
    
    /**
     * Checks if this item should be deleted from Vusion.
     * @return true if deleted or discontinued
     */
    public boolean shouldDelete() {
        return isDeleted() || isDiscontinued();
    }
    
    public Double getPrice1() {
        return price1;
    }
    
    public Integer getDivider1() {
        return divider1 != null ? divider1 : 1;
    }
    
    public Double getPromoPrice1() {
        return promoPrice1;
    }
    
    public Integer getPromoDivider1() {
        return promoDivider1 != null ? promoDivider1 : 1;
    }
    
    public String getPromoStart() {
        return promoStart;
    }
    
    public String getPromoEnd() {
        return promoEnd;
    }
    
    /**
     * Calculates the unit price (price1 / divider1).
     * @return Unit price, or null if price1 is null
     */
    public Double getUnitPrice() {
        if (price1 == null) {
            return null;
        }
        int div = getDivider1();
        return div > 0 ? price1 / div : price1;
    }
    
    /**
     * Calculates the promo unit price (promoPrice1 / promoDivider1).
     * @return Promo unit price, or null if promoPrice1 is null
     */
    public Double getPromoUnitPrice() {
        if (promoPrice1 == null) {
            return null;
        }
        int div = getPromoDivider1();
        return div > 0 ? promoPrice1 / div : promoPrice1;
    }
    
    public String getUserAssigned1() {
        return userAssigned1;
    }
    
    public String getUserAssigned2() {
        return userAssigned2;
    }
    
    public String getUserAssigned3() {
        return userAssigned3;
    }
    
    public String getUserAssigned4() {
        return userAssigned4;
    }
    
    public String getUserAssigned5() {
        return userAssigned5;
    }
    
    public String getUserAssigned6() {
        return userAssigned6;
    }
    
    public String getUserAssigned7() {
        return userAssigned7;
    }
    
    public String getLocalPowerField1() {
        return localPowerField1;
    }
    
    public String getLocalPowerField2() {
        return localPowerField2;
    }
    
    public String getLocalPowerField3() {
        return localPowerField3;
    }
    
    public String getLocalPowerField4() {
        return localPowerField4;
    }
    
    public String getLocalPowerField5() {
        return localPowerField5;
    }
    
    public String getLocalPowerField6() {
        return localPowerField6;
    }
    
    public String getLocalPowerField7() {
        return localPowerField7;
    }
    
    public String getLocalPowerField8() {
        return localPowerField8;
    }
    
    public String getDescLine1() {
        return descLine1;
    }
    
    public String getDescLine2() {
        return descLine2;
    }
    
    public Double getWeight() {
        return weight;
    }
    
    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }
    
    public Double getFixedWeightAmt() {
        return fixedWeightAmt;
    }
    
    public Double getFixedTare() {
        return fixedTare;
    }
    
    public Double getPercentTare() {
        return percentTare;
    }
    
    public String getTareType() {
        return tareType;
    }
    
    public String getIngredients() {
        return ingredients;
    }
    
    public Integer getShelfLife() {
        return shelfLife;
    }
    
    @Override
    public String toString() {
        return "CatapultStoreData{" +
            "storeNumber='" + storeNumber + '\'' +
            ", storeName='" + storeName + '\'' +
            ", price1=" + price1 +
            ", divider1=" + divider1 +
            ", promoPrice1=" + promoPrice1 +
            ", promoDivider1=" + promoDivider1 +
            ", deleted=" + deleted +
            ", discontinued=" + discontinued +
            '}';
    }
}
