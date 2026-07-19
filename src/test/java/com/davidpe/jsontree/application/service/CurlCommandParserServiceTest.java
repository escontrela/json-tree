package com.davidpe.jsontree.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.application.model.CurlCommandParseResult;
import com.davidpe.jsontree.application.model.CurlCommandParseStatus;
import com.davidpe.jsontree.application.model.CurlCommandSource;
import org.junit.jupiter.api.Test;

class CurlCommandParserServiceTest {

  private final CurlCommandParserService service = new CurlCommandParserService();

  @Test
  void parsesGetWithCookieHeader() {
    CurlCommandParseResult result =
        service.detectAndParse(
            "curl --location 'https://example.com/api' --header 'Cookie: token=abc'",
            CurlCommandSource.clipboard());

    assertTrue(result.successful());
    assertEquals("GET", result.request().method());
    assertTrue(result.request().followRedirects());
    assertEquals("https://example.com/api", result.request().url().toString());
    assertEquals(1, result.request().headers().size());
    assertEquals("Cookie", result.request().headers().getFirst().name());
    assertEquals("token=abc", result.request().headers().getFirst().value());
  }

  @Test
  void parsesPostWithJsonBodyAndHeaders() {
    CurlCommandParseResult result =
        service.detectAndParse(
            """
            curl --location https://example.com/items \
              --header 'Content-Type: application/json' \
              --header 'X-Trace: demo' \
              --data '{"name":"json-tree"}'
            """,
            CurlCommandSource.clipboard());

    assertTrue(result.successful());
    assertEquals("POST", result.request().method());
    assertEquals(2, result.request().headers().size());
    assertEquals("{\"name\":\"json-tree\"}", result.request().body());
  }

  @Test
  void rejectsUnsafeShellFeatures() {
    CurlCommandParseResult result =
        service.detectAndParse(
            "curl https://example.com/api | jq .", CurlCommandSource.clipboard());

    assertEquals(CurlCommandParseStatus.INVALID, result.status());
    assertTrue(result.message().contains("Unsupported shell features"));
  }

  @Test
  void returnsNotCurlForPlainJson() {
    CurlCommandParseResult result =
        service.detectAndParse("{\"name\":\"json-tree\"}", CurlCommandSource.clipboard());

    assertFalse(result.successful());
    assertEquals(CurlCommandParseStatus.NOT_CURL, result.status());
  }

  @Test
  void rejectsMissingOptionValuesCleanly() {
    CurlCommandParseResult result =
        service.detectAndParse("curl https://example.com --header", CurlCommandSource.clipboard());

    assertEquals(CurlCommandParseStatus.INVALID, result.status());
    assertTrue(result.message().contains("Missing value"));
  }
}
