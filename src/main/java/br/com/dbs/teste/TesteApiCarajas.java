package br.com.dbs.teste;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TesteApiCarajas {

  protected static String postAuthorization() throws Exception {
    try {
      URL url = new URL("https://labs.carajaslabs.com.br:9205/rest/auth");
      HttpURLConnection conexao = (HttpURLConnection) url.openConnection();

      conexao.setDoInput(true);
      conexao.setDoOutput(true);

      conexao.setRequestMethod("POST");
      conexao.setRequestProperty("Content-Type", "application/json");
      conexao.setRequestProperty("Accept", "application/json");

      String json = "{\"user\":\"solutionweb\",\"password\":\"Carajas@2025\"}";

      try (OutputStream os = conexao.getOutputStream()) {
        byte[] input = json.getBytes("utf-8");
        os.write(input, 0, input.length);
      }

      if (conexao.getResponseCode() != 201) {
        throw new RuntimeException(
            "HTTP error code : " + conexao.getResponseCode() + "\n"
                + "HTTP error message : " + conexao.getResponseMessage());
      }

      return conexao.getHeaderField("Authorization");
    } catch (IOException | RuntimeException e) {
      throw new Exception("ERRO: " + e);
    }
  }

  protected static String getXMLs(String token, HashMap<String, String> params) throws Exception {
    try {
      // Monta a query string
      StringBuilder queryString = new StringBuilder();
      for (Map.Entry<String, String> entry : params.entrySet()) {
        if (queryString.length() != 0) {
          queryString.append("&");
        }
        queryString.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
        queryString.append("=");
        queryString.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
      }

      URL url = new URL(
          "https://labs.carajaslabs.com.br:9205/rest/spy?" + queryString);
      HttpURLConnection conexao = (HttpURLConnection) url.openConnection();

      conexao.setDoInput(true);
      conexao.setDoOutput(true);

      conexao.setRequestMethod("GET");
      conexao.setRequestProperty("Content-Type", "application/json");
      conexao.setRequestProperty("Accept", "*/*");
      conexao.setRequestProperty("User-Agent",
          "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.95 Safari/537.36");
      conexao.setRequestProperty("Authorization", token);

      if (conexao.getResponseCode() != 200) {
        throw new RuntimeException(
            "HTTP error code : " + conexao.getResponseCode() + "\n"
                + "HTTP error message : " + conexao.getResponseMessage());
      }

      BufferedReader resposta = new BufferedReader(new InputStreamReader((conexao.getInputStream())));
      String inputLine;
      StringBuilder response = new StringBuilder();

      while ((inputLine = resposta.readLine()) != null) {
        response.append(inputLine).append("\n");
      }
      resposta.close();

      ObjectMapper mapper = new ObjectMapper();
      List<NFe> nfes = mapper.readValue(response.toString(), new TypeReference<List<NFe>>() {
      });

      // Exemplo de acesso ao objeto
      for (NFe nfe : nfes) {
        System.out.println("Chave de acesso: " + nfe.key_nf);
        System.out.println("Nome do emitente: " + nfe.info.nomeEmitente);
      }

      return "";

    } catch (IOException | RuntimeException e) {
      e.printStackTrace();
      throw new Exception("ERRO: " + e);
    }
  }

  public static void main(String[] args) {
    try {
      String token = postAuthorization();
      if (token == null) {
        System.out.println("Token não encontrado.");
      } else {
        System.out.println("Token encontrado: " + token);

        HashMap<String, String> params = new HashMap<>();
        params.put("cnpj_destiny", "03656804000131");
        params.put("inclusion_from", "2025-04-10T00:00:00.000-0300");
        params.put("inclusion_to", "2025-04-10T23:59:59.000-0300");
        params.put("emission_from", "");
        params.put("emission_to", "");
        getXMLs(token, params);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}

@JsonIgnoreProperties(ignoreUnknown = true)
class NFe {
  public String xml;
  public String key_nf;
  public String emission;
  public Info info;
  public String name_destiny;
  public String cnpj_destiny;
  public String name_emitter;
  public String cnpj_emitter;
  public String branch;
  public Integer id;
  public Object id_deleted;
  public Object created_by_id;
  public Object updated_by_id;
  public String created_at;
  public String updated_at;
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Info {
  public String cnpjCpfEmitente;
  public String nomeEmitente;
  public String ieEmitente;
  public String cnpjCpfDestinatario;
  public String nomeDestinatario;
  public String chaveAcesso;
  public Integer numeroNfe;
  public String dataEmissao;
  public String valorTotal;
  public String situacao;
  public String manifestacao;
  public String tipoOperacao;
  public String xml;
}
