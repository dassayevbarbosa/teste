package br.com.dbs.teste;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class ExtratorPDFCompleto {

  static List<Documento> docs = new ArrayList<>();

  public static void main(String[] args) throws Exception {
    // File pdf_aux = Paths.get("D:\\temp\\CARAJAS\\FATURAS 2025\\SITRAM\\0102 -
    // SITRAM PAGAMENTOS 2025.xlsx").toFile();
    // extrairSITRAM(pdf_aux);
    // System.exit(0);

    String dirRN = "D:\\temp\\CARAJAS\\FATURAS 2025\\RN";
    String dirPB = "D:\\temp\\CARAJAS\\FATURAS 2025\\PB";
    String dirAL = "D:\\temp\\CARAJAS\\FATURAS 2025\\AL";
    String dirSITRAM = "D:\\temp\\CARAJAS\\FATURAS 2025\\SITRAM";

    List<String> arquivosRN = Functions.getArquivos(dirRN, ".pdf");
    List<String> arquivosPB = Functions.getArquivos(dirPB, ".pdf");
    List<String> arquivosAL = Functions.getArquivos(dirAL, ".xlsx");
    List<String> arquivosSITRAM = Functions.getArquivos(dirSITRAM, ".xlsx");

    for (String file : arquivosPB) {
      File pdf = Paths.get(file).toFile();
      extrairFatura(pdf);
    }

    for (String file : arquivosSITRAM) {
      File excel = Paths.get(file).toFile();
      extrairSITRAM(excel);
    }

    for (String file : arquivosRN) {
      File pdf = Paths.get(file).toFile();
      extrairRecolhimentoRN(pdf);
    }

    for (String file : arquivosAL) {
      File excel = Paths.get(file).toFile();
      extrairSefazAL(excel);
    }

    Documento.exportarParaExcel(docs, "D:\\temp\\CARAJAS\\resultado_carajas.xlsx");
  }

  public static void extrairRecolhimentoRN(File pdfFile) throws IOException {
    String nomeArquivo = pdfFile.getName().replace(".pdf", "");

    PDDocument doc = PDDocument.load(pdfFile);
    PDFTextStripper stripper = new PDFTextStripper();
    stripper.setSortByPosition(true);
    String texto = stripper.getText(doc);
    doc.close();

    // Extrair CNPJ
    Pattern cnpjPattern = Pattern.compile("\\b(\\d{14})\\b");
    Matcher cnpjMatcher = cnpjPattern.matcher(texto);
    String cnpj = cnpjMatcher.find() ? cnpjMatcher.group(1).replaceAll("\\D", "") : nomeArquivo;

    List<Documento> registros = new ArrayList<>();

    String[] linhas = texto.split("\\R+");
    Boolean inicioBloco = false;
    for (String linha : linhas) {
      if (linha.contains("RECEITA DÉBITO VENCIMENTO VALOR JUROS MULTA MORA")) {
        inicioBloco = true;
        continue;
      }
      if (linha.trim().isEmpty()) {
        continue;
      }
      if (linha.contains(
          "Tipo de Nº Documento Vencimento Valor Doc. Data Pgto. Valor Pgto. Valor Nominal Valor Juros Valor Multa Valor Mora Banco/Agência")
          || linha.contains("Usuário:")) {
        inicioBloco = false;
        continue;
      }
      if (inicioBloco) {
        Pattern pattern = Pattern.compile(
            "(\\d{4})\\s+(.+?)\\s+(\\d{2}/\\d{2}/\\d{4})\\s+" +
                "([\\d.,]+)\\s+([\\d.,]+)\\s+([\\d.,]+)\\s+([\\d.,]+)");
        Matcher matcher = pattern.matcher(linha);

        if (matcher.find()) {
          Documento documento = new Documento();
          documento.tipoRegistro = "CONSULTA RECOLHIMENTO RN";
          documento.cnpj_loja = cnpj;
          documento.codReceita = matcher.group(1).trim();
          documento.origemDebito = matcher.group(2).trim();
          documento.dataVencimento = matcher.group(3).trim();
          documento.total = matcher.group(4).trim();
          documento.totalJuros = matcher.group(5).trim();
          documento.totalMulta = matcher.group(6).trim();
          documento.totalMora = matcher.group(7).trim();
          registros.add(documento);
        }
      }
    }

    docs.addAll(registros);
  }

  public static void extrairFatura(File pdfFile) throws Exception {
    List<Documento> itens = new ArrayList<>();
    String nomeArquivo = pdfFile.getName().replace(".pdf", "");

    try (PDDocument doc = PDDocument.load(pdfFile)) {
      PDFTextStripper stripper = new PDFTextStripper();
      String texto = stripper.getText(doc);

      // Pré-processa: junta todas as linhas em um array
      String[] linhas = texto.split("\\R+");
      List<String> buffer = new ArrayList<>();

      for (int i = 0; i < linhas.length; i++) {
        String linha = linhas[i].trim();

        // Tentativa de identificar uma nova nota fiscal
        if (linha.matches("^\\d{1,8}\\s+\\d{2}/\\d{2}/\\d{4}\\s+\\d{2}/\\d{2}/\\d{4}.*")) {
          if (!buffer.isEmpty()) {
            Documento item = processarBloco(buffer, nomeArquivo);
            if (item != null)
              itens.add(item);
            buffer.clear();
          }
        }

        // Acumula a linha
        buffer.add(linha);
      }

      // Processa o último bloco
      if (!buffer.isEmpty()) {
        Documento item = processarBloco(buffer, nomeArquivo);
        if (item != null)
          itens.add(item);
      }
    }

    docs.addAll(itens);
  }

  public static void extrairSITRAM(File excelFile) throws Exception {
    List<Documento> registros = new ArrayList<>();
    String nomeArquivo = excelFile.getName().replace(".xlsx", "").replace(".xls", "");

    if (excelFile.getName().toLowerCase().contains(".xlsx")) {
      try (FileInputStream fis = new FileInputStream(excelFile);
          Workbook workbook = new XSSFWorkbook(fis)) {

        Sheet planilha = workbook.getSheetAt(0);
        int contador = 0;
        for (Row linha : planilha) {
          if (contador++ <= 0) {
            continue;
          }

          if (Functions.getCellValue(linha.getCell(1)).isEmpty())
            continue;

          Documento documento = new Documento();
          documento.tipoRegistro = "SITRAM";
          documento.cnpj_loja = nomeArquivo;
          documento.ctrc = Functions.getCellValue(linha.getCell(1));
          documento.numeroNota = Functions.getCellValue(linha.getCell(2));
          documento.inclusao = Functions.getCellValue(linha.getCell(3));
          documento.fatoGerador = Functions.getCellValue(linha.getCell(4));
          documento.total = Functions.getCellValue(linha.getCell(5));
          documento.destinatarioEmitentes = Functions.getCellValue(linha.getCell(6));
          documento.credito = Functions.getCellValue(linha.getCell(7));
          documento.dataVencimento = Functions.getCellValue(linha.getCell(8));
          documento.codReceita = Functions.getCellValue(linha.getCell(9));
          documento.calculado = Functions.getCellValue(linha.getCell(10));
          documento.pago = Functions.getCellValue(linha.getCell(11));
          documento.dae = Functions.getCellValue(linha.getCell(12));
          documento.retencao = Functions.getCellValue(linha.getCell(13));
          documento.gnre = Functions.getCellValue(linha.getCell(14));
          documento.ressarc = Functions.getCellValue(linha.getCell(15));
          documento.credPresumido = Functions.getCellValue(linha.getCell(16));
          documento.parcelado = Functions.getCellValue(linha.getCell(17));
          documento.autoInfr = Functions.getCellValue(linha.getCell(18));
          documento.numDaeValor = Functions.getCellValue(linha.getCell(19));
          documento.situacao = Functions.getCellValue(linha.getCell(20));
          registros.add(documento);
        }
      }
    } else {

      Document doc = Jsoup.parse(excelFile, "ISO-8859-1");

      org.jsoup.select.Elements linhas = doc.select("tr");
      int contador = 0;
      for (Element linha : linhas) {
        if (contador++ <= 0) {
          continue;
        }

        org.jsoup.select.Elements colunas = linha.select("td");
        Documento documento = new Documento();
        documento.tipoRegistro = "SITRAM";
        documento.cnpj_loja = nomeArquivo;
        documento.ctrc = colunas.get(1).text().trim();
        documento.numeroNota = colunas.get(2).text().trim();
        documento.inclusao = colunas.get(3).text().trim();
        documento.fatoGerador = colunas.get(4).text().trim();
        documento.total = colunas.get(5).text().trim();
        documento.destinatarioEmitentes = colunas.get(6).text().trim();
        documento.credito = colunas.get(7).text().trim();
        documento.dataVencimento = colunas.get(8).text().trim();
        documento.codReceita = colunas.get(9).text().trim();
        documento.calculado = colunas.get(10).text().trim();
        documento.pago = colunas.get(11).text().trim();
        documento.dae = colunas.get(12).text().trim();
        documento.retencao = colunas.get(13).text().trim();
        documento.gnre = colunas.get(14).text().trim();
        documento.ressarc = colunas.get(15).text().trim();
        documento.credPresumido = colunas.get(16).text().trim();
        documento.parcelado = colunas.get(17).text().trim();
        documento.autoInfr = colunas.get(18).text().trim();
        documento.numDaeValor = colunas.get(19).text().trim();
        documento.situacao = colunas.get(20).text().trim();
        registros.add(documento);
      }
    }

    docs.addAll(registros);
  }

  public static void extrairSefazRN(File excelFile) throws Exception {
    List<Documento> registros = new ArrayList<>();
    String nomeArquivo = excelFile.getName().replace(".xlsx", "").replace(".xls", "");

    try (FileInputStream fis = new FileInputStream(excelFile);
        Workbook workbook = new XSSFWorkbook(fis)) {

      Sheet planilha = workbook.getSheetAt(0);
      int contador = 0;
      for (Row linha : planilha) {
        if (contador++ <= 3) {
          continue;
        }

        Documento documento = new Documento();
        documento.tipoRegistro = "SEFAZ RN";
        documento.cnpj_loja = nomeArquivo;
        documento.numeroNota = Functions.getCellValue(linha.getCell(0));
        documento.chave = Functions.getCellValue(linha.getCell(1));
        documento.origemDebito = Functions.getCellValue(linha.getCell(2));
        documento.total = Functions.getCellValue(linha.getCell(3));
        documento.totalICMS = Functions.getCellValue(linha.getCell(4));
        documento.cnpjDestinatarioEmitentes = Functions.getCellValue(linha.getCell(6));
        documento.destinatarioEmitentes = Functions.getCellValue(linha.getCell(7));
        documento.ufDestinatarioEmitentes = Functions.getCellValue(linha.getCell(8));
        documento.tipoNota = Functions.getCellValue(linha.getCell(9));
        documento.codCobranca = Functions.getCellValue(linha.getCell(10));
        documento.codReceita = Functions.getCellValue(linha.getCell(11));

        String[] serieModelo = documento.origemDebito.split("-");
        if (serieModelo.length == 3) {
          documento.serieNota = serieModelo[2].trim();
          documento.modeloNota = serieModelo[0].trim().equals("NFE") ? "55"
              : serieModelo[0].trim().equals("NFCE") ? "65" : serieModelo[0].trim().equals("CTE") ? "57" : "";
        } else {
          documento.serieNota = "";
          documento.modeloNota = "";
        }

        registros.add(documento);
      }

    } catch (IOException e) {
      e.printStackTrace();
    }

    docs.addAll(registros);
  }

  public static void extrairSefazAL(File excelFile) throws Exception {
    List<Documento> registros = new ArrayList<>();
    String nomeArquivo = excelFile.getName().replace(".xlsx", "").replace(".xls", "");

    try (FileInputStream fis = new FileInputStream(excelFile);
        Workbook workbook = new XSSFWorkbook(fis)) {

      Sheet planilha = workbook.getSheetAt(0);
      int contador = 0;
      String cnpjLoja = "";
      for (Row linha : planilha) {
        if (contador++ <= 6) {
          if (contador == 4) {
            String cnpjLojaPlanilha = Functions.getCellValue(linha.getCell(1));
            Pattern pattern = Pattern.compile("\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}");
            Matcher matcher = pattern.matcher(cnpjLojaPlanilha);

            if (matcher.find()) {
              cnpjLoja = matcher.group();
            }
          }

          continue;
        }

        if ((linha.getCell(0) == null) || (Functions.getCellValue(linha.getCell(0)).equals("VALOR TOTAL:"))
            || (Functions.getCellValue(linha.getCell(0)).isEmpty())) {
          break; // pula linhas em branco
        }

        Documento documento = new Documento();
        documento.tipoRegistro = "SEFAZ AL";
        documento.cnpj_loja = cnpjLoja;
        documento.numeroNota = Functions.getCellValue(linha.getCell(0));
        documento.chave = Functions.getCellValue(linha.getCell(12));
        documento.total = Functions.getCellValue(linha.getCell(4));
        documento.situacao = Functions.getCellValue(linha.getCell(6));
        documento.cnpjDestinatarioEmitentes = Functions.getCellValue(linha.getCell(11));
        documento.tipoImposto = Functions.getCellValue(linha.getCell(9));
        documento.dataEmissao = Functions.getCellValue(linha.getCell(2));
        documento.dataVencimento = Functions.getCellValue(linha.getCell(10));
        documento.modeloNota = documento.chave.substring(20, 22);
        documento.serieNota = documento.chave.substring(22, 25);
        registros.add(documento);
      }

    } catch (IOException e) {
      e.printStackTrace();
    }

    docs.addAll(registros);
  }

  private static Documento processarBloco(List<String> blocoParam, String nomeArquivo) {
    // Junta todas as linhas do bloco em uma única linha
    String bloco = String.join(" ", blocoParam).replaceAll("\\s{2,}", " ");

    // Regex robusto para capturar os dados
    Pattern padrao = Pattern.compile(
        "(\\d{1,8})\\s+" + // 1 - Nota Fiscal
            "(\\d{2}/\\d{2}/\\d{4})\\s+" + // 2 - Data Cobrança
            "(\\d{2}/\\d{2}/\\d{4})\\s+" + // 3 - Data Emissão
            "(.+?)\\s+" + // 4 - Nome empresarial (lazy)
            "(\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2})\\s+" + // 5 - CNPJ
            "([A-Z]{2})\\s+" + // 6 - UF
            "([\\d\\.]+,[\\d]{2})\\s+" + // 7 - ICMS
            "([\\d\\.]+,[\\d]{2})\\s+" + // 8 - BC
            "([\\d\\.]+,[\\d]{2})" + // 9 - Frete
            "(?:\\s+(\\d{2}/\\d{2}/\\d{4}))?" // 10 - MDFE/CTE (opcional com espaço)
    );

    Matcher m = padrao.matcher(bloco);
    if (m.find()) {
      Documento item = new Documento();
      item.tipoRegistro = "FATURA";
      item.cnpj_loja = nomeArquivo;
      item.numeroNota = m.group(1);
      item.dataCobranca = m.group(2);
      item.dataEmissao = m.group(3);
      item.destinatarioEmitentes = m.group(4).trim();
      item.cnpjDestinatarioEmitentes = m.group(5);
      item.ufDestinatarioEmitentes = m.group(6);
      item.totalICMS = m.group(7);
      item.totalBC = m.group(8);
      item.totalFrete = m.group(9);
      item.dataMdfe = m.group(10) != null ? m.group(10) : "";
      return item;
    }

    return null;
  }
}

class Documento {
  String tipoRegistro = "";
  String cnpj_loja = "";
  String numeroNota = "";
  String serieNota = "";
  String modeloNota = "";
  String chave = "";
  String tipoNota = "";
  String dataCobranca = "";
  String dataEmissao = "";
  String dataVencimento = "";
  String destinatarioEmitentes = "";
  String cnpjDestinatarioEmitentes = "";
  String ufDestinatarioEmitentes = "";
  String totalICMS = "";
  String totalBC = "";
  String totalFrete = "";
  String total = "";
  String dataMdfe = "";
  String codCobranca = "";
  String codReceita = "";
  String situacao = "";
  String tipoImposto = "";
  String origemDebito = "";
  String calculado = "", pago = "", dae = "";
  String retencao = "", gnre = "", ressarc = "", credPresumido = "", parcelado = "", autoInfr = "";
  String numDaeValor = "", ctrc = "", inclusao = "", fatoGerador = "", credito = "";
  String totalJuros = "", totalMulta = "", totalMora = "";

  public String toCSV() {
    return String.join(" | ", Arrays.asList(
        tipoRegistro, cnpj_loja, numeroNota, serieNota, modeloNota, chave, tipoNota,
        dataCobranca, dataEmissao, dataVencimento,
        destinatarioEmitentes, cnpjDestinatarioEmitentes, ufDestinatarioEmitentes,
        totalICMS, totalBC, totalFrete, total,
        dataMdfe, codCobranca, codReceita, situacao,
        tipoImposto, origemDebito,
        calculado, pago, dae,
        retencao, gnre, ressarc, credPresumido, parcelado, autoInfr,
        numDaeValor, ctrc, inclusao, fatoGerador, credito, totalJuros, totalMulta, totalMora));
  }

  public static void exportarParaExcel(List<Documento> documentos, String caminhoArquivo) throws Exception {
    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet("Documentos");

    // Cabeçalho
    List<String> cabecalhos = Arrays.asList(
        "Tipo Registro", "Loja", "Número Nota", "Série", "Modelo", "Chave", "Tipo Nota",
        "Data Cobrança", "Data Emissão", "Data Vencimento",
        "Destinatário/Emitente", "CNPJ Dest/Emit", "UF Dest/Emit",
        "Total ICMS", "Total BC", "Total Frete", "Total",
        "Data MDF-e", "Cód. Cobrança", "Cód. Receita", "Situação",
        "Tipo Imposto", "Origem Débito",
        "Calculado", "Pago", "DAE",
        "Retenção", "GNRE", "Ressarcimento", "Crédito Presumido", "Parcelado", "Auto de Infração",
        "Num/Valor DAE", "CTRC", "Inclusão", "Fato Gerador", "Crédito", "Total Juros", "Total Multa", "Total Mora");

    // Cria cabeçalho na planilha
    Row headerRow = sheet.createRow(0);
    for (int i = 0; i < cabecalhos.size(); i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(cabecalhos.get(i));
    }

    // Preenche dados
    int rowNum = 1;
    for (Documento doc : documentos) {
      Row row = sheet.createRow(rowNum++);
      String[] valores = doc.toCSV().split(" \\| ");
      for (int i = 0; i < valores.length; i++) {
        row.createCell(i).setCellValue(valores[i]);
      }
    }

    // Autoajuste de colunas
    for (int i = 0; i < cabecalhos.size(); i++) {
      sheet.autoSizeColumn(i);
    }

    // Salva arquivo
    try (FileOutputStream fileOut = new FileOutputStream(caminhoArquivo)) {
      workbook.write(fileOut);
    }

    workbook.close();
  }
}