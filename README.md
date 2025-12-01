# Catapult VUSION ESL Driver

A Java application that bridges the ECRS Catapult POS system with the SES Vusion electronic shelf label (ESL) system. It listens for item data updates from Catapult and translates/forwards them to the Vusion API.

## Architecture

```
[Catapult System] --POST--> [Java Listener :port] --POST/DELETE--> [Vusion API]
                            localhost only
```

## Requirements

- Java 8 (Temurin JRE 1.8 or compatible)
- Maven 3.6+

## Quick Start

1. **Build the application:**
   ```bash
   mvn clean package
   ```

2. **Configure the application:**
   Copy `config.properties.example` to `config.properties` and edit:
   ```properties
   # Server port for Catapult to POST to
   server.port=8080
   
   # Vusion API credentials
   vusion.api.baseUrl=https://api-us.vusion.io/vlink-pro/v1
   vusion.api.subscriptionKey=YOUR_SUBSCRIPTION_KEY
   
   # Store mappings (Catapult storeNumber -> Vusion storeId)
   store.RS1=okimoto_corp_us.waianae_store
   store.RS2=okimoto_corp_us.ecrs_test_store
   ```

3. **Run the application:**
   ```bash
   java -jar target/catapult-vusion-esl-driver-1.0.0.jar
   ```

## Configuration

### Server Settings

| Property | Description | Default |
|----------|-------------|---------|
| `server.port` | Port to listen on (localhost only) | `8080` |

### Vusion API Settings

| Property | Description |
|----------|-------------|
| `vusion.api.baseUrl` | Vusion API base URL |
| `vusion.api.subscriptionKey` | Your Ocp-Apim-Subscription-Key |

### Store Mappings

Map Catapult store numbers to Vusion store IDs:
```properties
store.RS1=okimoto_corp_us.waianae_store
store.RS2=okimoto_corp_us.ecrs_test_store
```

**Note:** Stores not mapped (e.g., HQ) will be ignored.

## API Endpoints

### POST /catapult

Receives item updates from Catapult system.

- **Request:** JSON array of Catapult item objects
- **Response:** 
  - `200 OK` - All items processed successfully
  - `500 Internal Server Error` - One or more items failed after retries

## Field Mapping

### Catapult → Vusion

| Vusion Field | Catapult Source | Notes |
|--------------|-----------------|-------|
| `id` | `itemId` | UPC code |
| `name` | `itemName` | Product name |
| `price` | `price1 / divider1` | Unit price |
| `brand` | `brand` | Direct mapping |
| `capacity` | `size` | e.g., "7 OZ" |

### Custom Fields

| Vusion Custom Field | Catapult Source |
|---------------------|-----------------|
| `promoPrice` | `promoPrice1 / promoDivider1` |
| `promoQty` | `promoDivider1` |
| `promoStartDate` | `store.promoStart` |
| `promoEndDate` | `store.promoEnd` |
| `formattedRegPrice` | Computed (e.g., "$12.98") |
| `formattedPromoPrice` | Computed (e.g., "2/$9.00") |
| `department` | `deptName` |
| `receiptAlias` | `receiptAlias` |
| `Itemsize` | `size` |
| `SizeUnit` | `sizeUnit` |
| `WHItem` | `powerField5` |
| `barcodeUPC` | `itemId` |
| `priceQty` | `divider1` |

## Behavior

- **Batching:** Items are batched up to 999 items or 10MB per Vusion API request
- **Retry Logic:** Failed requests are retried up to 3 times with exponential backoff
- **Delete Handling:** Items with `deleted=true` or `discontinued=true` trigger a DELETE to Vusion
- **Store Filtering:** Only stores defined in config are processed; others (e.g., HQ) are ignored

## Logging

All operations are logged with timestamps to stdout. Redirect to a file if needed:
```bash
java -jar target/catapult-vusion-esl-driver-1.0.0.jar > app.log 2>&1
```

## License

Proprietary - ECRS / Okimoto Corp

