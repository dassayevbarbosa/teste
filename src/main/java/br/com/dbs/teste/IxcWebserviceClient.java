package br.com.dbs.teste;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class IxcWebserviceClient {

  // ============================================================
  // MÉTODO PARA INSERIR ENTRADA (equivalente ao Python original)
  // ============================================================
  public static String postEntradaBasic(String host, String token, Map<String, String> dados, boolean selfSigned)
      throws Exception {

    String endpoint;
    if (host.startsWith("http://") || host.startsWith("https://")) {
      endpoint = host;
      if (!endpoint.endsWith("/"))
        endpoint += "/";
      endpoint += "entrada";
    } else {
      endpoint = "https://" + host + "/webservice/v1/entrada";
    }

    String payload = mapToJson(dados);

    if (selfSigned) {
      disableCertificateValidation();
    }

    URL url = new URL(endpoint);
    HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
    con.setRequestMethod("POST");

    String basicAuth = Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    con.setRequestProperty("Authorization", "Basic " + basicAuth);
    con.setRequestProperty("ixcsoft", "");
    con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
    con.setRequestProperty("Accept", "application/json");

    con.setDoOutput(true);

    try (OutputStream os = con.getOutputStream()) {
      os.write(payload.getBytes(StandardCharsets.UTF_8));
    }

    return readResponse(con);
  }

  // ============================================================
  // NOVO MÉTODO: LISTAR FORNECEDORES (equivalente ao Python)
  // ============================================================
  public static String listarFornecedores(String host, String token, Map<String, String> filtros, boolean selfSigned)
      throws Exception {

    String endpoint;
    if (host.startsWith("http://") || host.startsWith("https://")) {
      endpoint = host;
      if (!endpoint.endsWith("/"))
        endpoint += "/";
      endpoint += "fornecedor";
    } else {
      endpoint = "https://" + host + "/webservice/v1/fornecedor";
    }

    String payload = mapToJson(filtros);

    if (selfSigned) {
      disableCertificateValidation();
    }

    URL url = new URL(endpoint);
    HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
    con.setRequestMethod("POST");

    // Authorization Basic
    String basicAuth = Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    con.setRequestProperty("Authorization", "Basic " + basicAuth);

    // Header importantíssimo para listagem no IXC
    con.setRequestProperty("ixcsoft", "listar");

    con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
    con.setRequestProperty("Accept", "application/json");

    con.setDoOutput(true);

    try (OutputStream os = con.getOutputStream()) {
      os.write(payload.getBytes(StandardCharsets.UTF_8));
    }

    return readResponse(con);
  }

  // ============================================================
  // Função utilitária: ler resposta da conexão HTTP
  // ============================================================
  private static String readResponse(HttpsURLConnection con) throws Exception {
    int status = con.getResponseCode();
    InputStream is = (status >= 200 && status < 400) ? con.getInputStream() : con.getErrorStream();

    StringBuilder sb = new StringBuilder();
    try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
      String line;
      while ((line = br.readLine()) != null) {
        sb.append(line).append("\n");
      }
    }
    return sb.toString().trim();
  }

  // ============================================================
  // Conversão Map → JSON (sem bibliotecas externas)
  // ============================================================
  public static String mapToJson(Map<String, String> map) {
    StringBuilder sb = new StringBuilder("{");
    boolean first = true;

    for (Map.Entry<String, String> e : map.entrySet()) {
      if (!first)
        sb.append(",");
      first = false;

      sb.append("\"").append(escapeJson(e.getKey())).append("\":");

      String value = e.getValue();
      if (value == null) {
        sb.append("null");
      } else {
        sb.append("\"").append(escapeJson(value)).append("\"");
      }
    }

    sb.append("}");
    return sb.toString();
  }

  private static String escapeJson(String s) {
    if (s == null)
      return "";
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  // ============================================================
  // Self-signed SSL
  // ============================================================
  private static void disableCertificateValidation() throws Exception {
    TrustManager[] trustAllCerts = new TrustManager[] {
        new X509TrustManager() {
          public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
          }

          public void checkClientTrusted(X509Certificate[] certs, String authType) {
          }

          public void checkServerTrusted(X509Certificate[] certs, String authType) {
          }
        }
    };

    SSLContext sc = SSLContext.getInstance("TLS");
    sc.init(null, trustAllCerts, new SecureRandom());
    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

    HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
  }
}
