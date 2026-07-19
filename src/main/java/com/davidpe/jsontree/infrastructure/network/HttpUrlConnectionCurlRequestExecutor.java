package com.davidpe.jsontree.infrastructure.network;

import com.davidpe.jsontree.application.model.CurlExecutionRequest;
import com.davidpe.jsontree.application.model.CurlExecutionResult;
import com.davidpe.jsontree.application.port.out.CurlRequestExecutorPort;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.springframework.stereotype.Component;

/**
 * Executes normalized curl requests with scoped insecure HTTPS handling for this workflow only.
 */
@Component
public class HttpUrlConnectionCurlRequestExecutor implements CurlRequestExecutorPort {

  private static final int CONNECT_TIMEOUT_MILLIS = 10_000;
  private static final int READ_TIMEOUT_MILLIS = 20_000;
  private static final int MAX_REDIRECTS = 5;
  private static final HostnameVerifier INSECURE_HOSTNAME_VERIFIER = (host, session) -> true;

  private final SSLSocketFactory insecureSslSocketFactory = buildInsecureSslSocketFactory();

  @Override
  public CurlExecutionResult execute(CurlExecutionRequest request) {
    try {
      return executeInternal(request);
    } catch (IOException exception) {
      return CurlExecutionResult.failure("Curl fetch failed: " + exception.getMessage());
    }
  }

  private CurlExecutionResult executeInternal(CurlExecutionRequest request) throws IOException {
    URI currentUri = request.url();
    String currentMethod = request.method();
    byte[] currentBody = request.body().getBytes(StandardCharsets.UTF_8);

    for (int redirectCount = 0; redirectCount <= MAX_REDIRECTS; redirectCount++) {
      HttpURLConnection connection = openConnection(currentUri.toURL());
      configureConnection(connection, request, currentMethod, currentBody);

      int statusCode = connection.getResponseCode();
      if (shouldFollowRedirect(request, statusCode)) {
        String location = connection.getHeaderField("Location");
        if (location == null || location.isBlank()) {
          return CurlExecutionResult.failure("Redirect response did not include a Location header.");
        }
        currentUri = currentUri.resolve(location);
        if (statusCode == HttpURLConnection.HTTP_SEE_OTHER) {
          currentMethod = "GET";
          currentBody = new byte[0];
        }
        continue;
      }

      byte[] responseBytes = readResponseBytes(connection);
      String contentType = contentTypeWithoutCharset(connection.getContentType());
      String charset = resolveCharset(connection.getContentType());
      return CurlExecutionResult.success(
          statusCode,
          currentUri,
          sanitizeHeaders(connection.getHeaderFields()),
          responseBytes,
          contentType,
          charset);
    }

    return CurlExecutionResult.failure("Curl fetch exceeded the maximum number of redirects.");
  }

  protected HttpURLConnection openConnection(URL url) throws IOException {
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
    connection.setReadTimeout(READ_TIMEOUT_MILLIS);
    connection.setInstanceFollowRedirects(false);
    if (connection instanceof HttpsURLConnection httpsConnection) {
      httpsConnection.setSSLSocketFactory(insecureSslSocketFactory);
      httpsConnection.setHostnameVerifier(INSECURE_HOSTNAME_VERIFIER);
    }
    return connection;
  }

  private void configureConnection(
      HttpURLConnection connection,
      CurlExecutionRequest request,
      String method,
      byte[] body) throws IOException {
    connection.setRequestMethod(method);
    request.headers().forEach(header -> connection.setRequestProperty(header.name(), header.value()));
    if (body.length == 0) {
      return;
    }
    connection.setDoOutput(true);
    if (request.headers().stream()
        .noneMatch(header -> "content-type".equalsIgnoreCase(header.name()))) {
      connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
    }
    connection.getOutputStream().write(body);
  }

  private boolean shouldFollowRedirect(CurlExecutionRequest request, int statusCode) {
    if (!request.followRedirects()) {
      return false;
    }
    return statusCode == HttpURLConnection.HTTP_MOVED_PERM
        || statusCode == HttpURLConnection.HTTP_MOVED_TEMP
        || statusCode == HttpURLConnection.HTTP_SEE_OTHER
        || statusCode == 307
        || statusCode == 308;
  }

  private byte[] readResponseBytes(HttpURLConnection connection) throws IOException {
    InputStream inputStream =
        connection.getErrorStream() != null ? connection.getErrorStream() : connection.getInputStream();
    if (inputStream == null) {
      return new byte[0];
    }
    try (InputStream stream = inputStream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      stream.transferTo(output);
      return output.toByteArray();
    }
  }

  private String contentTypeWithoutCharset(String contentType) {
    if (contentType == null || contentType.isBlank()) {
      return "";
    }
    return contentType.split(";", 2)[0].trim();
  }

  private String resolveCharset(String contentType) {
    if (contentType == null || contentType.isBlank()) {
      return "";
    }
    String[] segments = contentType.split(";");
    for (String segment : segments) {
      String trimmed = segment.trim().toLowerCase(Locale.ROOT);
      if (trimmed.startsWith("charset=")) {
        return trimmed.substring("charset=".length()).trim();
      }
    }
    return "";
  }

  private Map<String, List<String>> sanitizeHeaders(Map<String, List<String>> headers) {
    Map<String, List<String>> sanitized = new HashMap<>();
    headers.forEach(
        (name, values) -> {
          if (name != null && values != null) {
            sanitized.put(name, List.copyOf(values));
          }
        });
    return sanitized;
  }

  private SSLSocketFactory buildInsecureSslSocketFactory() {
    try {
      TrustManager[] trustManagers = {
        new X509TrustManager() {
          @Override
          public void checkClientTrusted(X509Certificate[] chain, String authType) {}

          @Override
          public void checkServerTrusted(X509Certificate[] chain, String authType) {}

          @Override
          public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
          }
        }
      };
      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, trustManagers, new SecureRandom());
      return sslContext.getSocketFactory();
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("Unable to initialize curl HTTPS transport.", exception);
    }
  }
}
