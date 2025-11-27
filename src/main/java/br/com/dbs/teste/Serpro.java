package br.com.dbs.teste;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Serpro {

  // Constantes GET e POST
  private static final String GET = "GET";
  private static final String POST = "POST";
  private static final String CONSULTAR = "Consultar";
  private static final String APOIAR = "Apoiar";
  private static final String EMITIR = "Emitir";
  private static final String NENHUM = "";

  private static final String CHAVE1 = "8LPfzSELq3_tPqf6HFYhXfTedVsa";
  private static final String CHAVE2 = "OkVGAtHc0GdJXdhaYe6Gz7C6VO8a";
  private static final String URL_INTEGRA_CONTADOR = "https://gateway.apiserpro.serpro.gov.br/integra-contador/v1/";
  private static final String URL_CONSULTA_CNPJ = "https://gateway.apiserpro.serpro.gov.br/consulta-cnpj-df/v2/empresa/";
  private static final String URL_CONSULTA_CND = "https://gateway.apiserpro.serpro.gov.br/consulta-cnd/v1/certidao";

  public static JsonNode autenticar() throws Exception {
    // Configurações
    String aut = CHAVE1 + ":" + CHAVE2;
    aut = java.util.Base64.getEncoder().encodeToString(aut.getBytes(StandardCharsets.UTF_8));
    String url = "https://autenticacao.sapi.serpro.gov.br/authenticate";
    String authHeader = "Basic " + aut;
    String p12Path = "D:\\temp\\CERTIFICADOS\\TAXBAM\\TAXBAM TECNOLOGIAS E COMPLIANCE FISCAL LTDA_45135775000102.pfx";
    String p12Password = "261176";

    // Carregar o KeyStore do tipo PKCS12
    KeyStore keyStore = KeyStore.getInstance("PKCS12");
    try (FileInputStream fis = new FileInputStream(p12Path)) {
      keyStore.load(fis, p12Password.toCharArray());
    }

    // Inicializar KeyManagerFactory com o KeyStore
    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(keyStore, p12Password.toCharArray());

    // Criar o SSLContext com o certificado do cliente
    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(kmf.getKeyManagers(), null, null);

    // Criar o HttpClient com SSLContext customizado
    HttpClient client = HttpClient.newBuilder()
        .sslContext(sslContext)
        .build();

    // Corpo da requisição (form-urlencoded)
    String formData = "grant_type=client_credentials";

    // Construir a requisição POST
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Authorization", authHeader)
        .header("Role-Type", "TERCEIROS")
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString(formData, StandardCharsets.UTF_8))
        .build();

    // Enviar a requisição e capturar a resposta
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    ObjectMapper mapper = new ObjectMapper();
    JsonNode rootNode = mapper.readTree(response.body());

    int expiresIn = rootNode.get("expires_in").asInt();
    String scope = rootNode.get("scope").asText();
    String tokenType = rootNode.get("token_type").asText();
    String accessToken = rootNode.get("access_token").asText();
    String jwtToken = rootNode.get("jwt_token").asText();
    JsonNode jwtPucomexNode = rootNode.get("jwt_pucomex");

    // System.out.println("accessToken: " + accessToken);
    // System.out.println("jwtToken: " + jwtToken);

    return rootNode;
  }

  public static HttpResponse<String> consumir(String metodo, String URL, String jsonBody)
      throws Exception {
    // URL de destino
    String url = URL;

    // Autenticação
    JsonNode nodeAutenticacao = autenticar();
    String accessToken = nodeAutenticacao.get("access_token").asText();
    String jwtToken = nodeAutenticacao.get("jwt_token").asText();

    // Criar HttpClient
    HttpClient client = HttpClient.newHttpClient();

    // Criar HttpRequest
    HttpRequest request;
    if ("GET".equalsIgnoreCase(metodo)) {
      request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("Authorization", "Bearer " + accessToken)
          .header("Content-Type", "application/json")
          .header("jwt_token", jwtToken)
          .GET()
          .build();
    } else {
      request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("Authorization", "Bearer " + accessToken)
          .header("Content-Type", "application/json")
          .header("jwt_token", jwtToken)
          .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
          .build();
    }

    // Enviar requisição
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    return response;
  }

  public static String consultarCnpj(String CNPJ) throws Exception {
    // URL de destino
    String url = URL_CONSULTA_CNPJ + CNPJ;

    HttpResponse<String> response = consumir(GET, url, "");

    // Exibir resposta
    System.out.println("Status Code: " + response.statusCode());
    System.out.println("Response Body: " + response.body());

    return response.body();
  }

  public static String consultarCND(String CNPJContribuinte)
      throws Exception {
    String jsonBody = """
        {
          "TipoContribuinte": 1,
              "ContribuinteConsulta": "%s",
              "CodigoIdentificacao": "9001",
              "GerarCertidaoPdf": true
        }
        """.formatted(CNPJContribuinte);
    HttpResponse<String> response = consumir(POST, URL_CONSULTA_CND, jsonBody);

    // Exibir resposta
    System.out.println("Status Code: " + response.statusCode());
    System.out.println("Response Body: " + response.body());

    return response.body();
  }

  public static String consumirIntegraContador(String CNPJContratante, String CNPJAutorPedido, String CNPJContribuinte,
      String idSistema, String idServico, String dados, String TIPO)
      throws Exception {
    String jsonBody = """
        {
          "contratante": {
            "numero": "%s",
            "tipo": 2
          },
          "autorPedidoDados": {
            "numero": "%s",
            "tipo": 2
          },
          "contribuinte": {
            "numero": "%s",
            "tipo": 2
          },
          "pedidoDados": {
            "idSistema": "%s",
            "idServico": "%s",
            "versaoSistema": "2.0",
            "dados": "%s"
          }
        }
        """.formatted(CNPJContratante, CNPJAutorPedido, CNPJContribuinte, idSistema, idServico, dados);
    HttpResponse<String> response = consumir(POST, URL_INTEGRA_CONTADOR + TIPO, jsonBody);

    // Exibir resposta
    System.out.println("Status Code: " + response.statusCode());
    System.out.println("Response Body: " + response.body());

    return response.body();
  }

  public static void extrairDadosPGDAS(String jsonString) {
    try {
      JSONParser parser = new JSONParser();

      // Parse do JSON principal
      JSONObject json = (JSONObject) parser.parse(jsonString);

      // Extrair e parsear o campo 'dados' (que é uma String JSON)
      String dadosString = (String) json.get("dados");
      JSONObject dados = (JSONObject) parser.parse(dadosString);

      Long anoCalendario = (Long) dados.get("anoCalendario");
      System.out.println("Ano Calendário: " + anoCalendario);

      JSONArray periodos = (JSONArray) dados.get("periodos");

      for (Object periodoObj : periodos) {
        JSONObject periodo = (JSONObject) periodoObj;
        Long periodoApuracao = (Long) periodo.get("periodoApuracao");
        System.out.println("\nPeríodo Apuração: " + periodoApuracao);

        JSONArray operacoes = (JSONArray) periodo.get("operacoes");
        for (Object operacaoObj : operacoes) {
          JSONObject operacao = (JSONObject) operacaoObj;
          String tipoOperacao = (String) operacao.get("tipoOperacao");
          System.out.println("  Tipo Operação: " + tipoOperacao);

          JSONObject indiceDeclaracao = (JSONObject) operacao.get("indiceDeclaracao");
          if (indiceDeclaracao != null) {
            String numeroDeclaracao = (String) indiceDeclaracao.get("numeroDeclaracao");
            String dataHoraTransmissao = (String) indiceDeclaracao.get("dataHoraTransmissao");
            String malha = (String) indiceDeclaracao.get("malha");
            System.out.println("    Número Declaração: " + numeroDeclaracao);
            System.out.println("    Data Hora Transmissão: " + dataHoraTransmissao);
            System.out.println("    Malha: " + malha);
          }

          JSONObject indiceDas = (JSONObject) operacao.get("indiceDas");
          if (indiceDas != null) {
            String numeroDas = (String) indiceDas.get("numeroDas");
            String datahoraEmissaoDas = (String) indiceDas.get("datahoraEmissaoDas");
            Boolean dasPago = (Boolean) indiceDas.get("dasPago");
            System.out.println("    Número DAS: " + numeroDas);
            System.out.println("    Data Hora Emissão DAS: " + datahoraEmissaoDas);
            System.out.println("    DAS Pago: " + dasPago);
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void extrairDadosDIVIDAATIVA(String jsonString) {
    try {
      JSONParser parser = new JSONParser();
      JSONObject root = (JSONObject) parser.parse(jsonString);

      // Contratante
      JSONObject contratante = (JSONObject) root.get("contratante");
      String numeroContratante = (String) contratante.get("numero");
      Long tipoContratante = (Long) contratante.get("tipo");

      // Contribuinte
      JSONObject contribuinte = (JSONObject) root.get("contribuinte");
      String numeroContribuinte = (String) contribuinte.get("numero");
      Long tipoContribuinte = (Long) contribuinte.get("tipo");

      // Pedido Dados
      JSONObject pedidoDados = (JSONObject) root.get("pedidoDados");
      String idSistema = (String) pedidoDados.get("idSistema");
      String idServico = (String) pedidoDados.get("idServico");
      String versaoSistema = (String) pedidoDados.get("versaoSistema");
      String dadosPedido = (String) pedidoDados.get("dados");

      // Status
      Long status = (Long) root.get("status");

      // Mensagens
      JSONArray mensagens = (JSONArray) root.get("mensagens");
      for (Object msgObj : mensagens) {
        JSONObject msg = (JSONObject) msgObj;
        String codigo = (String) msg.get("codigo");
        String texto = (String) msg.get("texto");

        System.out.println("Mensagem Código: " + codigo);
        System.out.println("Mensagem Texto: " + texto);
      }

      // Outras informações
      String responseId = (String) root.get("responseId");
      String responseDateTime = (String) root.get("responseDateTime");

      // Exibindo tudo
      System.out.println("Contratante Numero: " + numeroContratante);
      System.out.println("Contratante Tipo: " + tipoContratante);
      System.out.println("Contribuinte Numero: " + numeroContribuinte);
      System.out.println("Contribuinte Tipo: " + tipoContribuinte);
      System.out.println("Pedido ID Sistema: " + idSistema);
      System.out.println("Pedido ID Serviço: " + idServico);
      System.out.println("Pedido Versão: " + versaoSistema);
      System.out.println("Pedido Dados: " + dadosPedido);
      System.out.println("Status: " + status);
      System.out.println("Response ID: " + responseId);
      System.out.println("Response DateTime: " + responseDateTime);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static String extrairDadosProtocoloSituacaoFiscal(String jsonString) {
    String protocoloRelatorio = null;

    try {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode root = mapper.readTree(jsonString);

      // Extrai o JSON que está dentro de "dados"
      if (root.has("dados")) {
        String dadosStr = root.get("dados").asText();
        JsonNode dadosNode = mapper.readTree(dadosStr);

        if (dadosNode.has("protocoloRelatorio")) {
          protocoloRelatorio = dadosNode.get("protocoloRelatorio").asText();
        }

        // System.out.println("tempoEspera: " + dadosNode.get("tempoEspera"));
      }

    } catch (Exception e) {
      e.printStackTrace();
    }

    return protocoloRelatorio;
  }

  public static void teste() {
    try {
      File file = new File("saida.pdf");
      PDDocument document = PDDocument.load(file);

      PDFTextStripper stripper = new PDFTextStripper();
      String text = stripper.getText(document);

      document.close();

      // --- Sócios ---
      System.out.println("=== Sócios e Administradores ===");
      Pattern sociosPattern = Pattern.compile("(Sócios e Administradores[\\s\\S]*?Certidão Emitida)");
      Matcher mSocios = sociosPattern.matcher(text);
      if (mSocios.find()) {
        System.out.println(mSocios.group(1).replace("Certidão Emitida", "").trim());
      }

      // --- Pendência - Débito (SIEF) ---
      System.out.println("\n=== Pendência - Débito (SIEF) ===");
      Pattern debitoPattern = Pattern
          .compile("(Pendência - Débito \\(SIEF\\)[\\s\\S]*?Débito com Exigibilidade Suspensa)");
      Matcher mDebito = debitoPattern.matcher(text);
      if (mDebito.find()) {
        System.out.println(mDebito.group(1).replace("Débito com Exigibilidade Suspensa", "").trim());
      }

      // --- Débito com Exigibilidade Suspensa (SIEF) ---
      System.out.println("\n=== Débito com Exigibilidade Suspensa (SIEF) ===");
      Pattern suspensoPattern = Pattern
          .compile("(Débito com Exigibilidade Suspensa \\(SIEF\\)[\\s\\S]*?Pendência - Inscrição)");
      Matcher mSuspenso = suspensoPattern.matcher(text);
      if (mSuspenso.find()) {
        System.out.println(mSuspenso.group(1).replace("Pendência - Inscrição", "").trim());
      }

      // --- Pendência - Inscrição (SIDA) ---
      System.out.println("\n=== Pendência - Inscrição (SIDA) ===");
      Pattern sidaPattern = Pattern.compile("(Pendência - Inscrição \\(SIDA\\)[\\s\\S]*?Final do Relatório)");
      Matcher mSida = sidaPattern.matcher(text);
      if (mSida.find()) {
        System.out.println(mSida.group(1).replace("Final do Relatório", "").trim());
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static List<List<String>> lerRelatorioCertidaoECAC(String jsonString) throws Exception {

    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = mapper.readTree(jsonString);

    // O campo "dados" vem como string JSON
    String dadosStr = root.get("dados").asText();
    JsonNode dadosNode = mapper.readTree(dadosStr);

    // Extrai o PDF em Base64
    String pdfBase64 = dadosNode.get("pdf").asText();

    // Decodifica para bytes
    byte[] pdfBytes = Base64.getDecoder().decode(pdfBase64);

    // try {
    // Files.write(Paths.get("saida.pdf"), pdfBytes);
    // System.out.println("PDF salvo em: " + Paths.get("saida.pdf"));
    // } catch (Exception e) {
    // e.printStackTrace();
    // }

    List<String> socios = new ArrayList<>();
    List<String> pendencias = new ArrayList<>();
    List<List<String>> retorno = new ArrayList<>();

    try (PDDocument document = PDDocument.load(pdfBytes)) {
      PDFTextStripper pdfStripper = new PDFTextStripper();
      String text = pdfStripper.getText(document);
      String[] lines = text.split("\\r?\\n");

      boolean inicioSocios = false;
      boolean capturarSocios = false;
      boolean inicioPendencias = false;
      boolean capturarPendencias = false;

      int contLineSocios = 0;
      int contLinePendencias = 0;

      for (String line : lines) {
        String lineLower = line.toLowerCase();

        if (lineLower.contains("sócios e administradores")) {
          inicioSocios = true;
          contLineSocios = 0;
        } else if (inicioSocios) {
          contLineSocios++;
          if (contLineSocios == 1) {
            capturarSocios = true;
            inicioSocios = false;
          }
        } else if (capturarSocios) {
          if (lineLower.contains("certidão emitida")) {
            capturarSocios = false;
            inicioSocios = false;
          } else {
            String[] socio = line.split("\\s+");
            if (socio.length >= 4) {
              String cpfSocio = socio[0];
              String nomeSocio = String.join(" ", socio[1], socio[2], socio[3]);
              String cotaSocio = socio[socio.length - 1];
              socios.add(cpfSocio + " | " + nomeSocio + " | " + cotaSocio);
            }
          }
        } else if (lineLower.contains("pendência - débito")) {
          inicioPendencias = true;
          contLinePendencias = 0;
        } else if (inicioPendencias) {
          contLinePendencias++;
          if (contLinePendencias == 2) {
            capturarPendencias = true;
            inicioPendencias = false;
          }
        } else if (capturarPendencias) {
          if (lineLower.contains("diagnóstico fiscal na procuradoria-geral da fazenda nacional") ||
              lineLower.contains("débito com exigibilidade suspensa (sief)")) {
            capturarPendencias = false;
            inicioPendencias = false;
          } else {
            String[] pendencia = line.split("\\s+");
            if (pendencia.length >= 8) {
              String situacaoPendencia = pendencia[pendencia.length - 1];
              String saldoDevedorPendencia = pendencia[pendencia.length - 5];
              String valorOriginalPendencia = pendencia[pendencia.length - 6];
              String vencimentoPendencia = pendencia[pendencia.length - 7];
              String competenciaPendencia = pendencia[pendencia.length - 8];
              String tipo = String.join(" ",
                  java.util.Arrays.copyOfRange(pendencia, 0, pendencia.length - 8));

              pendencias.add(competenciaPendencia + " | " + vencimentoPendencia + " | " +
                  valorOriginalPendencia + " | " + saldoDevedorPendencia + " | " +
                  situacaoPendencia + " | " + tipo);
            }
          }
        }
      }

    } catch (IOException e) {
      e.printStackTrace();
    }

    retorno.add(socios);
    retorno.add(pendencias);
    return retorno;
  }

  public static void main(String[] args) throws Exception {
    teste();
    System.exit(0);

    String CNPJContratante = "45135775000102";
    String CNPJAutorPedido = "45135775000102";
    String CNPJContribuinte = "27408427000180";
    String idSistema = "";
    String idServico = "";
    String dados = "";
    String retorno = "";

    // // DECLARAÇÃO PGDAS
    // idSistema = "PGDASD";
    // idServico = "CONSDECLARACAO13";
    // dados = "{'anoCalendario': '2024'}";
    // retorno = consumirIntegraContador(CNPJContratante, CNPJAutorPedido,
    // CNPJContribuinte,
    // idSistema, idServico, dados, CONSULTAR);

    // DIVIDA ATIVA MEI
    // idSistema = "PGMEI";
    // idServico = "DIVIDAATIVA24";
    // dados = "{'anoCalendario': '2024'}";
    // retorno = consumirIntegraContador(CNPJContratante, CNPJAutorPedido,
    // CNPJContribuinte,
    // idSistema, idServico,
    // dados, CONSULTAR);

    // SITUAÇÃO FISCAL
    // idSistema = "SITFIS";
    // idServico = "SOLICITARPROTOCOLO91";
    // dados = "";
    // retorno = consumirIntegraContador(CNPJContratante, CNPJAutorPedido,
    // CNPJContribuinte,
    // idSistema, idServico,
    // dados, APOIAR);
    // String protocoloSitFis = extrairDadosProtocoloSituacaoFiscal(retorno);
    String protocoloSitFis = "+S7N6c04XNZUVzmxWT7SzpkZA4xeDQC9Rkxld/wvc26pyJur6Jn9PVJhLGFqissGPRjCjQLKeTr8phBVmDDiOAaZu/FC+G1pYOtTMYqokKYmMIe2JF+YwQIfMya3Zu306QyZwZ+ibeSGSe28B5Ev25jDnzpvVJPhOG8mL2N9/CYpczOvwS/WGo94UEElGPyF6F8avRRBJhQe91IgU2GcNjOF0i8m284ByBJaLf8jpjnDIr3OJjSAVA==";

    if (protocoloSitFis == null || protocoloSitFis.isEmpty()) {
      System.out.println("Erro ao obter o protocolo de situação fiscal.");
    } else {
      idSistema = "SITFIS";
      idServico = "RELATORIOSITFIS92";
      dados = "{ \\\"protocoloRelatorio\\\": \\\"" + protocoloSitFis + "\\\" }";
      retorno = consumirIntegraContador(CNPJContratante, CNPJAutorPedido,
          CNPJContribuinte,
          idSistema, idServico,
          dados, EMITIR);

      List<List<String>> relatorio = lerRelatorioCertidaoECAC(retorno);
      List<String> socios = relatorio.get(0);
      List<String> pendencias = relatorio.get(1);
      System.out.println("\nSócios e Administradores:");
      for (String socio : socios) {
        System.out.println(socio);
      }
      System.out.println("\nPendências - Débito:");
      for (String pendencia : pendencias) {
        System.out.println(pendencia);
      }
    }

    // // DECLARAÇÃO DEFIS
    // idSistema = "DEFIS";
    // idServico = "CONSDECLARACAO142";
    // dados = "";
    // retorno = consumirIntegraContador(CNPJContratante, CNPJAutorPedido,
    // CNPJContribuinte,
    // idSistema, idServico,
    // dados, CONSULTAR);

    // // PARCELAMENTO SIMPLES NACIONAL
    // idSistema = "PARCSN";
    // idServico = "PARCELASPARAGERAR162";
    // dados = "";
    // retorno = consumirIntegraContador(CNPJContratante, CNPJAutorPedido,
    // CNPJContribuinte,
    // idSistema, idServico,
    // dados, CONSULTAR);

    // // PARCELAMENTO SIMPLES NACIONAL REGIME ESPECIAL
    // idSistema = "PARCSN-ESP";
    // idServico = "PARCELASPARAGERAR172";
    // dados = "";
    // retorno = consumirIntegraContador(CNPJContratante, CNPJAutorPedido,
    // CNPJContribuinte,
    // idSistema, idServico,
    // dados, CONSULTAR);

    // // PARCELAMENTO MEI
    // idSistema = "PARCMEI";
    // idServico = "PARCELASPARAGERAR202";
    // dados = "";
    // retorno = consumirIntegraContador(CNPJContratante, CNPJAutorPedido,
    // CNPJContribuinte,
    // idSistema, idServico,
    // dados, CONSULTAR);

    // // PARCELAMENTO MEI REGIME ESPECIAL
    // idSistema = "PARCMEI-ESP";
    // idServico = "PARCELASPARAGERAR212";
    // dados = "";
    // retorno = consumirIntegraContador(CNPJContratante, CNPJAutorPedido,
    // CNPJContribuinte,
    // idSistema, idServico,
    // dados, CONSULTAR);

    // CONSULTA DO REGIME DE APURAÇÃO
    // idSistema = "REGIMEAPURACAO";
    // idServico = "CONSULTARANOSCALENDARIOS102";
    // dados = "";
    // retorno = consumirIntegraContador(CNPJContratante, CNPJAutorPedido,
    // CNPJContribuinte,
    // idSistema, idServico,
    // dados, CONSULTAR);

    // CONSULTA CNPJ
    // retorno = consultarCnpj(CNPJContribuinte);

    // CONSULTA CND
    // retorno = consultarCND(CNPJContribuinte);
  }

  class Socio {
    String cpfCnpj;
    String nome;
    String qualificacao;
    String situacao;
    String capital;

    @Override
    public String toString() {
      return cpfCnpj + " | " + nome + " | " + qualificacao + " | " + situacao + " | " + capital;
    }
  }

  class DebitoSief {
    String receita;
    String pa;
    String vencimento;
    String valor;
    String situacao;

    @Override
    public String toString() {
      return receita + " | " + pa + " | " + vencimento + " | " + valor + " | " + situacao;
    }
  }

  class DebitoSuspenso {
    String receita;
    String pa;
    String vencimento;
    String valor;
    String situacao;

    @Override
    public String toString() {
      return receita + " | " + pa + " | " + vencimento + " | " + valor + " | " + situacao;
    }
  }

  class PendenciaSida {
    String inscricao;
    String receita;
    String vencimento;
    String processo;
    String devedor;
    String situacao;

    @Override
    public String toString() {
      return inscricao + " | " + receita + " | " + vencimento + " | " + processo + " | " + devedor + " | " + situacao;
    }
  }

}
