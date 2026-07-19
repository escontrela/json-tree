package com.davidpe.jsontree.application.model;

/**
 * Simple name/value HTTP header pair for normalized curl requests and responses.
 */
public record CurlExecutionHeader(String name, String value) {}
