package com.davidpe.jsontree.infrastructure.network;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.application.model.CurlCommandSource;
import com.davidpe.jsontree.application.model.CurlExecutionHeader;
import com.davidpe.jsontree.application.model.CurlExecutionRequest;
import com.davidpe.jsontree.application.model.CurlExecutionResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HttpUrlConnectionCurlRequestExecutorTest {

  @Test
  void executesGetAndCapturesResponse() {
    RecordingHttpURLConnection connection =
        new RecordingHttpURLConnection(
            200,
            "application/json; charset=UTF-8",
            "{\"ok\":true}".getBytes(StandardCharsets.UTF_8),
            Map.of("Content-Type", List.of("application/json; charset=UTF-8")));
    HttpUrlConnectionCurlRequestExecutor executor =
        new FakeExecutor(new ArrayDeque<>(List.of(connection)));
    CurlExecutionRequest request =
        new CurlExecutionRequest(
            "curl http://localhost/test",
            CurlCommandSource.clipboard(),
            URI.create("http://localhost/test"),
            "GET",
            false,
            List.of(new CurlExecutionHeader("X-Test", "demo")),
            "");

    CurlExecutionResult result = executor.execute(request);

    assertTrue(result.successful());
    assertEquals(200, result.statusCode());
    assertEquals("application/json", result.contentType());
    assertEquals("utf-8", result.charsetName());
    assertEquals("demo", connection.requestProperties().get("X-Test"));
    assertArrayEquals("{\"ok\":true}".getBytes(StandardCharsets.UTF_8), result.responseBody());
  }

  @Test
  void executesPostBodyAndFollowsRedirects() {
    RecordingHttpURLConnection redirect =
        new RecordingHttpURLConnection(
            302,
            "",
            new byte[0],
            Map.of("Location", List.of("/final")));
    RecordingHttpURLConnection target =
        new RecordingHttpURLConnection(
            200,
            "text/markdown; charset=UTF-8",
            "# Heading\n\ncontent".getBytes(StandardCharsets.UTF_8),
            Map.of("Content-Type", List.of("text/markdown; charset=UTF-8")));
    HttpUrlConnectionCurlRequestExecutor executor =
        new FakeExecutor(new ArrayDeque<>(List.of(redirect, target)));
    CurlExecutionRequest request =
        new CurlExecutionRequest(
            "curl --location -d '{\"id\":1}' http://localhost/redirect",
            CurlCommandSource.clipboard(),
            URI.create("http://localhost/redirect"),
            "POST",
            true,
            List.of(new CurlExecutionHeader("Content-Type", "application/json")),
            "{\"id\":1}");

    CurlExecutionResult result = executor.execute(request);

    assertTrue(result.successful());
    assertEquals(200, result.statusCode());
    assertEquals("{\"id\":1}", target.writtenBody());
    assertEquals("/final", result.effectiveUri().getPath());
  }

  @Test
  void reportsTransportFailureCleanly() {
    HttpUrlConnectionCurlRequestExecutor executor =
        new HttpUrlConnectionCurlRequestExecutor() {
          @Override
          protected HttpURLConnection openConnection(URL url) throws IOException {
            throw new IOException("simulated transport failure");
          }
        };
    CurlExecutionRequest request =
        new CurlExecutionRequest(
            "curl http://127.0.0.1/fail",
            CurlCommandSource.clipboard(),
            URI.create("http://127.0.0.1/fail"),
            "GET",
            false,
            List.of(),
            "");

    CurlExecutionResult result = executor.execute(request);

    assertFalse(result.successful());
    assertTrue(result.failureMessage().contains("Curl fetch failed"));
  }

  private static final class FakeExecutor extends HttpUrlConnectionCurlRequestExecutor {

    private final Deque<RecordingHttpURLConnection> connections;

    private FakeExecutor(Deque<RecordingHttpURLConnection> connections) {
      this.connections = connections;
    }

    @Override
    protected HttpURLConnection openConnection(URL url) {
      if (connections.isEmpty()) {
        throw new IllegalStateException("No fake connection registered for " + url);
      }
      return connections.removeFirst();
    }
  }

  private static final class RecordingHttpURLConnection extends HttpURLConnection {

    private final int responseCode;
    private final String contentType;
    private final byte[] responseBody;
    private final Map<String, List<String>> headers;
    private final java.io.ByteArrayOutputStream writtenBody = new java.io.ByteArrayOutputStream();
    private final java.util.Map<String, String> requestProperties = new java.util.HashMap<>();

    private RecordingHttpURLConnection(
        int responseCode,
        String contentType,
        byte[] responseBody,
        Map<String, List<String>> headers) {
      super(null);
      this.responseCode = responseCode;
      this.contentType = contentType;
      this.responseBody = responseBody;
      this.headers = headers;
    }

    @Override
    public void disconnect() {}

    @Override
    public boolean usingProxy() {
      return false;
    }

    @Override
    public void connect() {}

    @Override
    public void setRequestMethod(String method) {
      this.method = method;
    }

    @Override
    public void setRequestProperty(String key, String value) {
      requestProperties.put(key, value);
    }

    @Override
    public int getResponseCode() {
      return responseCode;
    }

    @Override
    public String getContentType() {
      return contentType;
    }

    @Override
    public Map<String, List<String>> getHeaderFields() {
      return headers;
    }

    @Override
    public String getHeaderField(String name) {
      List<String> values = headers.get(name);
      return values == null || values.isEmpty() ? null : values.getFirst();
    }

    @Override
    public InputStream getInputStream() {
      return new ByteArrayInputStream(responseBody);
    }

    @Override
    public InputStream getErrorStream() {
      return responseCode >= 400 ? new ByteArrayInputStream(responseBody) : null;
    }

    @Override
    public java.io.OutputStream getOutputStream() {
      return writtenBody;
    }

    private String writtenBody() {
      return writtenBody.toString(StandardCharsets.UTF_8);
    }

    private Map<String, String> requestProperties() {
      return requestProperties;
    }
  }
}
