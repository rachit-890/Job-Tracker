package com.rachit.jobtrackr.entity.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA converter: float[] ↔ JSON string stored in a TEXT column.
 *
 * Why not pgvector?
 * pgvector requires installing a Postgres extension, which adds operational
 * complexity. For Phase 4 we store as JSON-serialized TEXT — functionally
 * identical for our use case (cosine similarity computed in Java, not SQL).
 * Upgrade path: swap this converter for a pgvector type mapping when
 * vector search in SQL is needed (Phase 7+).
 *
 * Why not REAL[] (PostgreSQL array)?
 * REAL[] requires a custom Hibernate dialect mapping. The JSON approach
 * works with standard JPA and is trivially portable to other databases.
 */
@Converter
public class FloatArrayConverter implements AttributeConverter<float[], String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(float[] attribute) {
        if (attribute == null) return null;
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize float[] to JSON", e);
        }
    }

    @Override
    public float[] convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            return MAPPER.readValue(dbData, float[].class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize float[] from JSON", e);
        }
    }
}