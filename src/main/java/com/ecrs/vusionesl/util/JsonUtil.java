package com.ecrs.vusionesl.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

/**
 * JSON utility class using Gson for serialization/deserialization.
 */
public class JsonUtil {
    
    // Gson instance configured for pretty printing (useful for logging)
    private static final Gson GSON_PRETTY = new GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .create();
    
    // Gson instance for compact output (used for API requests)
    private static final Gson GSON = new GsonBuilder()
        .create();
    
    /**
     * Converts an object to JSON string (compact format).
     *
     * @param obj The object to serialize
     * @return JSON string
     */
    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }
    
    /**
     * Converts an object to pretty-printed JSON string.
     *
     * @param obj The object to serialize
     * @return Pretty-printed JSON string
     */
    public static String toPrettyJson(Object obj) {
        return GSON_PRETTY.toJson(obj);
    }
    
    /**
     * Parses JSON string into an object.
     *
     * @param json  The JSON string
     * @param clazz The target class
     * @param <T>   The type of the object
     * @return The parsed object
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }
    
    /**
     * Parses JSON string into an object using a Type reference.
     * Useful for generic types like List&lt;SomeClass&gt;.
     *
     * @param json The JSON string
     * @param type The target type
     * @param <T>  The type of the object
     * @return The parsed object
     */
    public static <T> T fromJson(String json, Type type) {
        return GSON.fromJson(json, type);
    }
    
    /**
     * Parses JSON string into a list of objects.
     *
     * @param json  The JSON string
     * @param clazz The element class
     * @param <T>   The type of list elements
     * @return The parsed list
     */
    public static <T> List<T> fromJsonList(String json, Class<T> clazz) {
        Type listType = TypeToken.getParameterized(List.class, clazz).getType();
        return GSON.fromJson(json, listType);
    }
    
    /**
     * Calculates the approximate size of a JSON string in bytes (UTF-8).
     *
     * @param json The JSON string
     * @return Size in bytes
     */
    public static int getJsonSizeBytes(String json) {
        if (json == null) {
            return 0;
        }
        return json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
    }
    
    /**
     * Gets the Gson instance for custom operations.
     *
     * @return The Gson instance
     */
    public static Gson getGson() {
        return GSON;
    }
}
