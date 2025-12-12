package com.ecrs.vusionesl.service;

import com.ecrs.vusionesl.model.CatapultItem;
import com.ecrs.vusionesl.model.CatapultStoreData;
import com.ecrs.vusionesl.model.VusionItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Transforms Catapult item data to Vusion item format.
 * Handles price calculations and field mapping according to the specification.
 */
public class ItemTransformer {
    
    private static final Logger logger = LoggerFactory.getLogger(ItemTransformer.class);
    
    // Number formatter for currency display (e.g., "$12.98")
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.US);
    
    /**
     * Transforms a Catapult item with store-specific data into a Vusion item.
     *
     * @param item      The Catapult item
     * @param storeData The store-specific pricing data
     * @return The transformed Vusion item
     */
    public VusionItem transform(CatapultItem item, CatapultStoreData storeData) {
        if (item == null || storeData == null) {
            throw new IllegalArgumentException("Item and store data must not be null");
        }
        
        logger.debug("Transforming item {} for store {}", item.getItemId(), storeData.getStoreNumber());
        
        VusionItem vusionItem = new VusionItem();
        
        // Core fields
        vusionItem.setId(item.getItemId())
                  .setName(item.getItemName())
                  .setBrand(item.getBrand())
                  .setCapacity(item.getSize());
        
        // Calculate unit price (price1 / divider1)
        Double unitPrice = storeData.getUnitPrice();
        vusionItem.setPrice(unitPrice);
        
        // Custom fields - Pricing
        addPricingCustomFields(vusionItem, storeData);
        
        // Custom fields - Item info
        addItemInfoCustomFields(vusionItem, item, storeData);
        
        // Custom fields - Promo dates
        addPromoDateCustomFields(vusionItem, storeData);
        
        // Custom fields - Power fields
        addPowerFieldCustomFields(vusionItem, item);
        
        logger.debug("Transformed item: {}", vusionItem);
        return vusionItem;
    }
    
    /**
     * Adds pricing-related custom fields to the Vusion item.
     */
    private void addPricingCustomFields(VusionItem vusionItem, CatapultStoreData storeData) {
        Double unitPrice = storeData.getUnitPrice();
        Double promoUnitPrice = storeData.getPromoUnitPrice();
        Integer divider1 = storeData.getDivider1();
        Integer promoDivider1 = storeData.getPromoDivider1();
        Double price1 = storeData.getPrice1();
        Double promoPrice1 = storeData.getPromoPrice1();
        
        // Regular price quantity (for "2/$6" display)
        vusionItem.addCustomField("priceQty", String.valueOf(divider1));
        
        // Formatted regular price
        if (unitPrice != null) {
            vusionItem.addCustomField("formattedRegPrice", formatCurrency(unitPrice));
            
            // Full formatted price with quantity (e.g., "2/$6.00" or "$3.00")
            if (divider1 > 1 && price1 != null) {
                vusionItem.addCustomField("formattedPrice", divider1 + "/" + formatCurrency(price1));
            } else {
                vusionItem.addCustomField("formattedPrice", formatCurrency(unitPrice));
            }
        }
        
        // Promo pricing
        if (promoPrice1 != null) {
            vusionItem.addCustomField("promoPrice", promoUnitPrice);
            vusionItem.addCustomField("promoQty", String.valueOf(promoDivider1));
            
            // Formatted promo price (e.g., "2/$9.00" or "$4.50")
            if (promoDivider1 > 1) {
                vusionItem.addCustomField("formattedPromoPrice", promoDivider1 + "/" + formatCurrency(promoPrice1));
            } else if (promoUnitPrice != null) {
                vusionItem.addCustomField("formattedPromoPrice", formatCurrency(promoUnitPrice));
            }
            
            // Calculate savings if both prices are available
            if (unitPrice != null && promoUnitPrice != null && unitPrice > promoUnitPrice) {
                double savings = unitPrice - promoUnitPrice;
                vusionItem.addCustomField("saveAmt", "SAVE " + formatCurrency(savings));
            }
        }
        
        // formattedRetailPrice - promo price when on promo, otherwise regular price
        if (promoPrice1 != null) {
            // On promo - show promo price
            if (promoDivider1 > 1) {
                vusionItem.addCustomField("formattedRetailPrice", promoDivider1 + "/" + formatCurrency(promoPrice1));
            } else {
                vusionItem.addCustomField("formattedRetailPrice", formatCurrency(promoUnitPrice));
            }
        } else if (price1 != null) {
            // No promo - show regular price
            if (divider1 > 1) {
                vusionItem.addCustomField("formattedRetailPrice", divider1 + "/" + formatCurrency(price1));
            } else {
                vusionItem.addCustomField("formattedRetailPrice", formatCurrency(unitPrice));
            }
        }
    }
    
    /**
     * Adds item information custom fields to the Vusion item.
     */
    private void addItemInfoCustomFields(VusionItem vusionItem, CatapultItem item, CatapultStoreData storeData) {
        // Department info
        vusionItem.addCustomField("department", formatDepartment(item.getDeptNumber(), item.getDeptName()));
        
        if (item.getSubDeptName() != null) {
            vusionItem.addCustomField("subDepartment", item.getSubDeptName());
        }
        
        // Receipt alias
        vusionItem.addCustomField("receiptAlias", item.getReceiptAlias());
        
        // Size info
        vusionItem.addCustomField("Itemsize", item.getSize());
        vusionItem.addCustomField("SizeUnit", item.getSizeUnit());
        
        if (item.getSizeQty() != null) {
            vusionItem.addCustomField("sizeQty", item.getSizeQty());
        }
        
        // UPC barcode (duplicate for ESL display)
        vusionItem.addCustomField("barcodeUPC", item.getItemId());
        
        // Item name (for ESL templates that use ItemName)
        vusionItem.addCustomField("ItemName", item.getItemName());
        
        // RealName - same as itemName
        vusionItem.addCustomField("RealName", item.getItemName());
        
        // Store-specific description lines
        if (storeData.getDescLine1() != null && !storeData.getDescLine1().isEmpty()) {
            vusionItem.addCustomField("descLine1", storeData.getDescLine1());
        }
        if (storeData.getDescLine2() != null && !storeData.getDescLine2().isEmpty()) {
            vusionItem.addCustomField("descLine2", storeData.getDescLine2());
        }
        
        // Weight info if applicable
        if (storeData.getWeight() != null) {
            vusionItem.addCustomField("weight", storeData.getWeight());
        }
        if (storeData.getUnitOfMeasure() != null) {
            vusionItem.addCustomField("unitOfMeasure", storeData.getUnitOfMeasure());
        }
    }
    
    /**
     * Adds promo date custom fields to the Vusion item.
     */
    private void addPromoDateCustomFields(VusionItem vusionItem, CatapultStoreData storeData) {
        String promoStart = storeData.getPromoStart();
        String promoEnd = storeData.getPromoEnd();
        
        if (promoStart != null && !promoStart.isEmpty()) {
            // Store raw date for processing
            vusionItem.addCustomField("promoStartDate", formatPromoDate(promoStart));
        }
        
        if (promoEnd != null && !promoEnd.isEmpty()) {
            // Format end date for display (e.g., "thru 12/31/2025")
            vusionItem.addCustomField("promoEndDate", "thru " + formatPromoDateForDisplay(promoEnd));
        }
    }
    
    /**
     * Adds power field custom fields to the Vusion item.
     * Mapped fields:
     *   - WIC: if powerField3 contains "Y" → "WIC"
     *   - DABUX: if powerField4 contains "DA BUX" → "0002"
     *   - IBMCode: if powerField4 contains "HI-5" → "HI-5"
     *   - WHItem ← powerField5
     */
    private void addPowerFieldCustomFields(VusionItem vusionItem, CatapultItem item) {
        // WIC: if powerField3 contains "Y" → "WIC"
        String pf3 = item.getPowerField3();
        if (pf3 != null && pf3.toUpperCase().contains("Y")) {
            vusionItem.addCustomField("WIC", "WIC");
        }
        
        // DABUX and IBMCode from PowerField4 (can have both)
        String pf4 = item.getPowerField4();
        if (pf4 != null) {
            String pf4Upper = pf4.toUpperCase();
            // DABUX: "0002" if contains "DA BUX"
            if (pf4Upper.contains("DA BUX")) {
                vusionItem.addCustomField("DABUX", "0002");
            }
            // IBMCode: "HI-5" if contains "HI-5"
            if (pf4Upper.contains("HI-5")) {
                vusionItem.addCustomField("IBMCode", "HI-5");
            }
        }
        
        // WHItem from PowerField5
        if (item.getPowerField5() != null) {
            vusionItem.addCustomField("WHItem", item.getPowerField5());
        }
        
        // Keep other power fields available if needed
        if (item.getPowerField1() != null) {
            vusionItem.addCustomField("powerField1", item.getPowerField1());
        }
        if (item.getPowerField2() != null) {
            vusionItem.addCustomField("powerField2", item.getPowerField2());
        }
        if (item.getPowerField6() != null) {
            vusionItem.addCustomField("powerField6", item.getPowerField6());
        }
        if (item.getPowerField7() != null) {
            vusionItem.addCustomField("powerField7", item.getPowerField7());
        }
        if (item.getPowerField8() != null) {
            vusionItem.addCustomField("powerField8", item.getPowerField8());
        }
    }
    
    /**
     * Formats a price as currency (e.g., "$12.98").
     */
    private String formatCurrency(double amount) {
        return CURRENCY_FORMAT.format(amount);
    }
    
    /**
     * Formats department as "NN DeptName" (e.g., "01 Grocery").
     */
    private String formatDepartment(Integer deptNumber, String deptName) {
        if (deptNumber == null && deptName == null) {
            return null;
        }
        if (deptNumber == null) {
            return deptName;
        }
        if (deptName == null) {
            return String.format("%02d", deptNumber);
        }
        return String.format("%02d %s", deptNumber, deptName);
    }
    
    /**
     * Formats promo date from Catapult format (ISO datetime) to Vusion format.
     * Input: "2025-12-01T10:35:16"
     * Output: "12/01/2025"
     */
    private String formatPromoDate(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) {
            return null;
        }
        try {
            // Extract date part (YYYY-MM-DD)
            String datePart = isoDate.split("T")[0];
            String[] parts = datePart.split("-");
            if (parts.length == 3) {
                return parts[1] + "/" + parts[2] + "/" + parts[0];  // MM/DD/YYYY
            }
        } catch (Exception e) {
            logger.warn("Failed to parse promo date: {}", isoDate, e);
        }
        return isoDate;  // Return as-is if parsing fails
    }
    
    /**
     * Formats promo date for display (e.g., "12/31/2025").
     */
    private String formatPromoDateForDisplay(String isoDate) {
        return formatPromoDate(isoDate);
    }
}
