package br.com.dbs.teste;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class CorrecaoSped {

  public static String returnValueCell(Cell cell) {
    if (cell == null) {
      return "";
    }

    DataFormatter formatter = new DataFormatter();
    FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();

    switch (cell.getCellType()) {
      case STRING:
      case BOOLEAN:
        return formatter.formatCellValue(cell);

      case NUMERIC:
        if (DateUtil.isCellDateFormatted(cell)) { // Verifica se a célula contém uma data
          SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy"); // Formato correto
          Date dateValue = cell.getDateCellValue();
          return dateFormat.format(dateValue);
        }
        return formatter.formatCellValue(cell); // Mantém formatação de números

      case FORMULA:
        CellValue evaluatedValue = evaluator.evaluate(cell);
        switch (evaluatedValue.getCellType()) {
          case STRING:
            return evaluatedValue.getStringValue();
          case NUMERIC:
            if (DateUtil.isCellDateFormatted(cell)) {
              SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
              return dateFormat.format(cell.getDateCellValue());
            }
            return formatter.formatCellValue(cell);
          case BOOLEAN:
            return String.valueOf(evaluatedValue.getBooleanValue());
          default:
            return "";
        }

      default:
        return "";
    }
  }

  public static HashMap<String, String> lerExcelProdutos() {
    HashMap<String, String> mapProdutos = new HashMap<>();

    try (InputStream inp = new FileInputStream(
        "D:\\Seven\\Clientes\\CONSULTE\\G5 COMERCIO DE MADEIRAS LTDA 10948040000113\\DADOS DE COD PRODUTO X DESCRIÇÃO G5.xlsx");
        XSSFWorkbook workbook = new XSSFWorkbook(inp);) {

      DecimalFormat df = new DecimalFormat("000000000000000000");
      Sheet sheet = workbook.getSheetAt(0);
      Iterator<Row> rowIterator = sheet.rowIterator();
      rowIterator.next(); // Pular cabeçalho
      while (rowIterator.hasNext()) {
        Row row = rowIterator.next();
        String codigo = df.format(Integer.parseInt(returnValueCell(row.getCell(0))));
        String descricao = returnValueCell(row.getCell(1)).trim();
        mapProdutos.put(codigo, descricao);
      }

    } catch (Exception e) {
      e.printStackTrace();
    }

    return mapProdutos;
  }

  public static HashMap<String, ArrayList<String>> lerExcelE113() {
    HashMap<String, ArrayList<String>> mapNotas = new HashMap<>();

    try (InputStream inp = new FileInputStream(
        "D:\\Seven\\Clientes\\CONSULTE\\G5 COMERCIO DE MADEIRAS LTDA 10948040000113\\E113.xlsx");
        XSSFWorkbook workbook = new XSSFWorkbook(inp);) {

      Sheet sheet = workbook.getSheetAt(0);
      Iterator<Row> rowIterator = sheet.rowIterator();
      rowIterator.next(); // Pular cabeçalho
      while (rowIterator.hasNext()) {
        Row row = rowIterator.next();
        String competencia = returnValueCell(row.getCell(0)).replaceAll("[^0-9]", "");
        String codigo = returnValueCell(row.getCell(1));
        String valor = returnValueCell(row.getCell(2)).trim();
        String chave = returnValueCell(row.getCell(3)).trim().replaceAll("[^0-9]", "");
        String emissao = returnValueCell(row.getCell(4)).trim().replaceAll("[^0-9]", "");
        String participante = returnValueCell(row.getCell(5)).trim().replaceAll("[^0-9]", "");
        if (mapNotas.containsKey(competencia + "|" + codigo)) {
          ArrayList<String> valorChave = mapNotas.get(competencia + "|" + codigo);
          valorChave.add(valor + "|" + chave + "|" + emissao + "|" + participante);
        } else {
          ArrayList<String> valorChave = new ArrayList<>();
          valorChave.add(valor + "|" + chave + "|" + emissao + "|" + participante);
          mapNotas.put(competencia + "|" + codigo, valorChave);
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
    }

    return mapNotas;
  }

  public static void corrigirProdutos() throws IOException {
    HashMap<String, String> mapProdutos = lerExcelProdutos();

    // ARQUIVOS SPED
    List<File> arquivos = new ArrayList<File>();
    File dir = new File("D:\\Seven\\Clientes\\CONSULTE\\G5 COMERCIO DE MADEIRAS LTDA 10948040000113\\sped 2023 e 2024");
    Functions.listDirectoryAppend(dir, arquivos, ".TXT");

    for (File file : arquivos) {
      String saidaOriginal = "";
      String saidaLog = "";
      String nomeArquivoLog = "";
      System.out.println("Arquivo: " + file.getName());
      BufferedReader readerSped = new BufferedReader(new InputStreamReader(
          new FileInputStream(file), "iso-8859-1"));
      String linhaSped;
      while ((linhaSped = readerSped.readLine()) != null) {
        String linhaEscrever = linhaSped;
        ArrayList<String> listaRegistrosLinha = Functions.quebraLinhaPorSeparador(linhaSped, "|", true, true);
        String reg = "";
        if (listaRegistrosLinha.size() > 0) {
          reg = listaRegistrosLinha.get(0);
        }
        if (reg.equals("0000")) {
          nomeArquivoLog = "LOG_" + listaRegistrosLinha.get(6) + "_" + listaRegistrosLinha.get(3) + "_"
              + listaRegistrosLinha.get(4) + ".csv";

          listaRegistrosLinha.set(2, "1");
          linhaEscrever = Functions.escreveListaEmStringComSeparador(listaRegistrosLinha, "|", true, true);
        } else if (reg.equals("0200")) {
          String codProduto = "";
          String descricaoProduto = "";
          if (listaRegistrosLinha.size() > 1) {
            codProduto = listaRegistrosLinha.get(1);
            descricaoProduto = listaRegistrosLinha.get(2).trim();

            if (mapProdutos.containsKey(codProduto)) {
              if (!descricaoProduto.equals(mapProdutos.get(codProduto))) {
                saidaLog += "Descrição diferente!;" + codProduto + ";" + descricaoProduto + ";"
                    + mapProdutos.get(codProduto) + "\n";

                listaRegistrosLinha.set(2, mapProdutos.get(codProduto));
                linhaEscrever = Functions.escreveListaEmStringComSeparador(listaRegistrosLinha, "|", true, true);
              }
            } else {
              saidaLog += "Produto não localizado na planilha!;" + codProduto + ";" + descricaoProduto + "\n";
            }
          }
        }

        saidaOriginal += linhaEscrever + "\n";
      }

      try (OutputStreamWriter bufferOut = new OutputStreamWriter(
          new FileOutputStream(
              "D:\\Seven\\Clientes\\CONSULTE\\G5 COMERCIO DE MADEIRAS LTDA 10948040000113\\SPEDS_OK\\"
                  + file.getName()),
          "iso-8859-1")) {
        bufferOut.write(saidaOriginal + "\n");
      }

      try (OutputStreamWriter bufferOut = new OutputStreamWriter(
          new FileOutputStream(
              "D:\\Seven\\Clientes\\CONSULTE\\G5 COMERCIO DE MADEIRAS LTDA 10948040000113\\SPEDS_OK\\"
                  + nomeArquivoLog),
          "iso-8859-1")) {
        bufferOut.write("LOG;CODIGO;DESC_SPED;DESC_PLANILHA\n" + saidaLog + "\n");
      }
    }
  }

  public static void corrigirE113() throws IOException {
    HashMap<String, ArrayList<String>> mapNotas = lerExcelE113();

    // ARQUIVOS SPED
    List<File> arquivos = new ArrayList<File>();
    File dir = new File("D:\\Seven\\Clientes\\CONSULTE\\G5 COMERCIO DE MADEIRAS LTDA 10948040000113\\SPEDS_OK");
    Functions.listDirectoryAppend(dir, arquivos, ".TXT");

    for (File file : arquivos) {
      Boolean finalArquivo = false;
      String saidaOriginal = "";
      String registrosE113 = "";
      String competencia = "";
      int contRegistrosE113 = 0;
      Boolean totalizadorE113 = false;
      System.out.println("Arquivo: " + file.getName());
      BufferedReader readerSped = new BufferedReader(new InputStreamReader(
          new FileInputStream(file), "iso-8859-1"));
      String linhaSped;
      while ((linhaSped = readerSped.readLine()) != null) {
        String linhaEscrever = linhaSped;
        ArrayList<String> listaRegistrosLinha = Functions.quebraLinhaPorSeparador(linhaSped, "|", true, true);
        String reg = "";
        int iReg = 0;
        if (listaRegistrosLinha.size() > 0) {
          reg = listaRegistrosLinha.get(0);
          if (!reg.replaceAll("[^0-9]", "").isEmpty() && !finalArquivo) {
            iReg = Integer.parseInt(reg.replaceAll("[^0-9]", ""));
          }
        }
        if (reg.equals("0000")) {
          competencia = listaRegistrosLinha.get(3);
          listaRegistrosLinha.set(2, "1");
          linhaEscrever = Functions.escreveListaEmStringComSeparador(listaRegistrosLinha, "|", true, true);
        } else if (reg.equals("E111")) {
          String codigoE111 = "";
          if (listaRegistrosLinha.size() > 1) {
            if (registrosE113.length() > 0) {
              saidaOriginal += registrosE113;
              registrosE113 = "";
            }

            codigoE111 = listaRegistrosLinha.get(1);

            if (mapNotas.containsKey(competencia + "|" + codigoE111)) {
              ArrayList<String> listaValores = mapNotas.get(competencia + "|" + codigoE111);
              for (String valoresChaves : listaValores) {
                String[] valorChave = valoresChaves.split("\\|");
                String valorE113 = valorChave[0];
                String chaveE113 = valorChave[1];
                String emissaoE113 = valorChave[2];
                String participanteE113 = "";
                if (valorChave.length > 3) {
                  participanteE113 = valorChave[3];
                }
                String modelo = chaveE113.substring(20, 22);
                String serie = chaveE113.substring(22, 25);
                String numero = chaveE113.substring(25, 34);

                contRegistrosE113++;
                registrosE113 += "|E113|" + participanteE113 + "|" + modelo + "|" + serie + "||" + numero + "|"
                    + emissaoE113 + "||" + valorE113
                    + "|" + chaveE113 + "|" + "\n";
              }
            }
          }
        } else if ((iReg > 113) && (!registrosE113.isEmpty())) {
          saidaOriginal += registrosE113;
          registrosE113 = "";
        } else if (reg.equals("E990") || reg.equals("9999")) {
          String qtd = listaRegistrosLinha.get(1);
          int iQtd = Integer.parseInt(qtd.replaceAll("[^0-9]", ""));
          listaRegistrosLinha.set(1, String.valueOf(iQtd + contRegistrosE113));
          linhaEscrever = Functions.escreveListaEmStringComSeparador(listaRegistrosLinha, "|", true, true);
        } else if (reg.equals("9900")) {
          if (listaRegistrosLinha.get(1).equals("E113")) {
            String qtd = listaRegistrosLinha.get(2);
            int iQtd = Integer.parseInt(qtd.replaceAll("[^0-9]", ""));
            listaRegistrosLinha.set(2, String.valueOf(iQtd + contRegistrosE113));
            linhaEscrever = Functions.escreveListaEmStringComSeparador(listaRegistrosLinha, "|", true, true);
            totalizadorE113 = true;
          } else if (listaRegistrosLinha.get(1).equals("9900")) {
            String qtd = listaRegistrosLinha.get(2);
            int iQtd = Integer.parseInt(qtd.replaceAll("[^0-9]", ""));
            listaRegistrosLinha.set(2, String.valueOf(iQtd + 1));
            linhaEscrever = Functions.escreveListaEmStringComSeparador(listaRegistrosLinha, "|", true, true);

            String registroTotalizadorE113 = "|9900|E113|" + String.valueOf(contRegistrosE113) + "|\n";
            linhaEscrever = registroTotalizadorE113 + linhaEscrever;
            contRegistrosE113++;
          }
        } else if (reg.equals("9990")) {
          String qtd = listaRegistrosLinha.get(1);
          int iQtd = Integer.parseInt(qtd.replaceAll("[^0-9]", ""));
          listaRegistrosLinha.set(1, String.valueOf(iQtd + 1));
          linhaEscrever = Functions.escreveListaEmStringComSeparador(listaRegistrosLinha, "|", true, true);
        }

        if (!finalArquivo) {
          saidaOriginal += linhaEscrever + "\n";
        }

        if (reg.equals("9999")) {
          finalArquivo = true;
        }
      }

      try (OutputStreamWriter bufferOut = new OutputStreamWriter(
          new FileOutputStream(
              "D:\\Seven\\Clientes\\CONSULTE\\G5 COMERCIO DE MADEIRAS LTDA 10948040000113\\SPEDS_OK_2\\"
                  + file.getName()),
          "iso-8859-1")) {
        bufferOut.write(saidaOriginal + "\n");
      }
    }

  }

  public static void excluirRegistros() throws IOException {
    String[] cnpjs = {
        // "05230009000617",
        // "05230009000706",
        // "05230009000889",
        // "05230009000960",
        // "05230009001001",
        // "05230009001184",
        // "05230009001265",
        // "05230009001346",
        // "05230009001427",
        // "05230009001508",
        // "05230009002156",
        // "05230009002580",
        // "05230009003802",
        // "05230009003985",
        // "05230009004019",
        // "05230009004523",
        // "05230009006496",
        // "05230009006577",
        // "05230009006810",
        // "05230009007620",
        // "05230009008359",
        // "05230009008863",
        // "05230009008944",
        // "05230009009169",
        // "05230009009240",

        "05230009010256",
        "05230009004876",
        "05230009008600",
        "05230009008782"
    };

    // ARQUIVOS SPED
    List<File> arquivos = new ArrayList<File>();
    File dir = new File("D:\\Seven\\Clientes\\PERMANENTE\\CORREÇÃO_SPED\\SPEDS-DRUGSTORE-2025");
    Functions.listDirectoryAppend(dir, arquivos, ".TXT");

    ARQUIVOS_LOOP: for (File file : arquivos) {
      Boolean finalArquivo = false;
      String saidaOriginal = "";
      int contE116 = 0, contE250 = 0, contE316 = 0;
      System.out.println("Arquivo: " + file.getName());
      BufferedReader readerSped = new BufferedReader(new InputStreamReader(
          new FileInputStream(file), "iso-8859-1"));
      String linhaSped;
      while ((linhaSped = readerSped.readLine()) != null) {
        String linhaEscrever = linhaSped;
        ArrayList<String> listaRegistrosLinha = Functions.quebraLinhaPorSeparador(linhaSped, "|", true, true);
        String reg = "";
        if (listaRegistrosLinha.size() > 0) {
          reg = listaRegistrosLinha.get(0);
        }
        if (reg.equals("0000")) {
          String dataInicial = listaRegistrosLinha.get(3);
          if (!dataInicial.equals("01022025") && !dataInicial.equals("01032025")) {
            continue ARQUIVOS_LOOP; // Pula arquivos com data inicial 01/02/2025 ou 01/03/2025
          }

          String cnpj = listaRegistrosLinha.get(6);
          Set<String> cnpjsSet = new HashSet<>(Arrays.asList(cnpjs));
          boolean existe = cnpjsSet.contains(cnpj);
          if (!existe) {
            continue ARQUIVOS_LOOP;
          }
        }

        if (reg.equals("E116") && !listaRegistrosLinha.get(4).equals("13170")
            && !listaRegistrosLinha.get(4).equals("50059")) {
          contE116++;
          continue;
        } else if (reg.equals("E250")) {
          contE250++;
          continue;
        } else if (reg.equals("E316")) {
          contE316++;
          continue;
        } else if (reg.equals("E990") || reg.equals("9999")) {
          String qtd = listaRegistrosLinha.get(1);
          int iQtd = Integer.parseInt(qtd.replaceAll("[^0-9]", ""));
          listaRegistrosLinha.set(1, String.valueOf(iQtd - contE116 - contE250 - contE316));
          linhaEscrever = Functions.escreveListaEmStringComSeparador(listaRegistrosLinha, "|", true, true);
        } else if (reg.equals("9900")) {
          if (listaRegistrosLinha.get(1).equals("E116")) {
            String qtd = listaRegistrosLinha.get(2);
            int iQtd = Integer.parseInt(qtd.replaceAll("[^0-9]", ""));
            listaRegistrosLinha.set(2, String.valueOf(iQtd - contE116));
            linhaEscrever = Functions.escreveListaEmStringComSeparador(listaRegistrosLinha, "|", true, true);
          } else if (listaRegistrosLinha.get(1).equals("E250")) {
            String qtd = listaRegistrosLinha.get(2);
            int iQtd = Integer.parseInt(qtd.replaceAll("[^0-9]", ""));
            listaRegistrosLinha.set(2, String.valueOf(iQtd - contE250));
            linhaEscrever = Functions.escreveListaEmStringComSeparador(listaRegistrosLinha, "|", true, true);
          } else if (listaRegistrosLinha.get(1).equals("E316")) {
            String qtd = listaRegistrosLinha.get(2);
            int iQtd = Integer.parseInt(qtd.replaceAll("[^0-9]", ""));
            listaRegistrosLinha.set(2, String.valueOf(iQtd - contE316));
            linhaEscrever = Functions.escreveListaEmStringComSeparador(listaRegistrosLinha, "|", true, true);
          }
        }

        if (!finalArquivo) {
          saidaOriginal += linhaEscrever + "\n";
        }

        if (reg.equals("9999")) {
          finalArquivo = true;
        }
      }

      try (OutputStreamWriter bufferOut = new OutputStreamWriter(
          new FileOutputStream(
              "D:\\Seven\\Clientes\\PERMANENTE\\CORREÇÃO_SPED\\SPEDS_OK\\"
                  + file.getName()),
          "iso-8859-1")) {
        bufferOut.write(saidaOriginal + "\n");
      }
    }

  }

  public static void copiar0300() throws IOException {
    String dirSpedsComRegistros0300 = "D:\\Seven\\Clientes\\CONSULTE\\G5 COMERCIO DE MADEIRAS LTDA 10948040000113\\0300\\teste";
    String dirSpedsBX = "D:\\Seven\\Clientes\\CONSULTE\\G5 COMERCIO DE MADEIRAS LTDA 10948040000113\\0300\\teste_bx";
    String dirSpedsOK = "D:\\Seven\\Clientes\\CONSULTE\\G5 COMERCIO DE MADEIRAS LTDA 10948040000113\\0300\\teste_ok";

    // COPIAR REGITROS 0300 E G PARA MAPAS
    System.out.println("Copiando registros 0300 e G110 para mapas...");
    List<File> arquivos = new ArrayList<File>();
    File dir = new File(dirSpedsComRegistros0300);
    Functions.listDirectoryAppend(dir, arquivos, ".SPED");
    HashMap<String, String> map0300 = new HashMap<>();
    HashMap<String, String> map0500 = new HashMap<>();
    HashMap<String, String> map0600 = new HashMap<>();
    HashMap<String, String> mapG = new HashMap<>();

    for (File file : arquivos) {
      String keyMap = "";
      BufferedReader readerSped = new BufferedReader(new InputStreamReader(
          new FileInputStream(file), "iso-8859-1"));
      String linhaSped;
      while ((linhaSped = readerSped.readLine()) != null) {
        ArrayList<String> listaRegistrosLinha = Functions.quebraLinhaPorSeparador(linhaSped, "|", true, true);
        String reg = listaRegistrosLinha.get(0);
        if (reg.equals("0000")) {
          keyMap = listaRegistrosLinha.get(3) + "|" + listaRegistrosLinha.get(4) + "|" + listaRegistrosLinha.get(6);
        } else if (reg.equals("0300") || reg.equals("0305")) {
          if (keyMap.isEmpty()) {
            System.out.println("Chave do mapa não encontrada para o registro " + reg + " no arquivo " + file.getName());
            continue;
          }
          if (map0300.containsKey(keyMap)) {
            String value = map0300.get(keyMap);
            value += linhaSped + "\n";
            map0300.put(keyMap, value);
          } else {
            String value = linhaSped + "\n";
            map0300.put(keyMap, value);
          }
        } else if (reg.equals("0500")) {
          if (keyMap.isEmpty()) {
            System.out.println("Chave do mapa não encontrada para o registro " + reg + " no arquivo " + file.getName());
            continue;
          }
          if (map0500.containsKey(keyMap)) {
            String value = map0500.get(keyMap);
            value += linhaSped + "\n";
            map0500.put(keyMap, value);
          } else {
            String value = linhaSped + "\n";
            map0500.put(keyMap, value);
          }
        } else if (reg.equals("0600")) {
          if (keyMap.isEmpty()) {
            System.out.println("Chave do mapa não encontrada para o registro " + reg + " no arquivo " + file.getName());
            continue;
          }
          if (map0600.containsKey(keyMap)) {
            String value = map0600.get(keyMap);
            value += linhaSped + "\n";
            map0600.put(keyMap, value);
          } else {
            String value = linhaSped + "\n";
            map0600.put(keyMap, value);
          }
        } else if (reg.substring(0, 1).equals("G")) {
          if (keyMap.isEmpty()) {
            System.out.println("Chave do mapa G não encontrada no arquivo " + file.getName());
            continue;
          }
          if (mapG.containsKey(keyMap)) {
            String value = mapG.get(keyMap);
            value += linhaSped + "\n";
            mapG.put(keyMap, value);
          } else {
            String value = linhaSped + "\n";
            mapG.put(keyMap, value);
          }

        }
      }
      readerSped.close();
    }

    // System.out.println("Registros 0300: " + map0300.toString());
    // System.out.println("Registros G110: " + mapG.toString());

    // ESCREVER REGISTROS 0300 E G EM ARQUIVOS
    System.out.println("Escrevendo registros 0300 e G110 em arquivos...");
    ArrayList<File> arquivosBX = new ArrayList<File>();
    File dirBX = new File(dirSpedsBX);
    Functions.listDirectoryAppend(dirBX, arquivosBX, ".TXT");
    System.out.println("Total de arquivos encontrados: " + arquivosBX.size());

    for (File file : arquivosBX) {
      String saidaOriginal = "";
      Boolean escreveu0300 = false, escreveu0500 = false, escreveu0600 = false, escreveuG = false, finalArquivo = false;
      String keyMap = "";
      BufferedReader readerSped = new BufferedReader(new InputStreamReader(
          new FileInputStream(file), "iso-8859-1"));
      String linhaSped;
      while ((linhaSped = readerSped.readLine()) != null) {
        if (finalArquivo) {
          continue;
        }

        ArrayList<String> listaRegistrosLinha = Functions.quebraLinhaPorSeparador(linhaSped, "|", true, true);
        String reg = listaRegistrosLinha.get(0);
        Integer iReg = Integer.parseInt(reg.substring(1, 4));
        if (reg.equals("0000")) {
          keyMap = listaRegistrosLinha.get(3) + "|" + listaRegistrosLinha.get(4) + "|" + listaRegistrosLinha.get(6);
          saidaOriginal += linhaSped + "\n";
        } else if ((iReg >= 400) && !escreveu0300) {
          escreveu0300 = true;
          if (map0300.containsKey(keyMap)) {
            String registros0300 = map0300.get(keyMap);
            saidaOriginal += registros0300 + linhaSped + "\n";
          } else {
            System.out.println("Chave do mapa 0300 não encontrada: " + keyMap);
          }
        } else if (reg.equals("0990")) {
          if (map0500.containsKey(keyMap)) {
            String registros0500 = map0500.get(keyMap);
            saidaOriginal += registros0500 + linhaSped + "\n";
          }

          if (map0600.containsKey(keyMap)) {
            String registros0600 = map0600.get(keyMap);
            saidaOriginal += registros0600 + linhaSped + "\n";
          }

          saidaOriginal += linhaSped + "\n";
        } else if (reg.equals("E990") && !escreveuG) {
          saidaOriginal += linhaSped + "\n";
          escreveuG = true;
          if (mapG.containsKey(keyMap)) {
            String registrosG = mapG.get(keyMap);
            saidaOriginal += registrosG;
          } else {
            System.out.println("Chave do mapa G110 não encontrada: " + keyMap);
          }
        } else {
          if (reg.substring(0, 1).equals("G")) {
            continue;
          }

          if (reg.equals("9999")) {
            finalArquivo = true;
          }
          if (!finalArquivo) {
            saidaOriginal += linhaSped + "\n";
          }
        }
      }
      readerSped.close();

      String arquivoSaida = dirSpedsOK + "\\"
          + file.getName();
      System.out.println("Escrevendo arquivo: " + arquivoSaida);
      try (OutputStreamWriter bufferOut = new OutputStreamWriter(
          new FileOutputStream(arquivoSaida),
          "iso-8859-1")) {
        bufferOut.write(saidaOriginal + "\n");
      }

      corrigirTotalizadores(arquivoSaida);
    }
  }

  public static void corrigirTotalizadores(String caminhoArquivo) throws IOException {
    List<String> linhasOriginais = new ArrayList<>();

    // 1. Leitura do arquivo com charset ISO-8859-1
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(new FileInputStream(caminhoArquivo), "ISO-8859-1"))) {
      String linha;
      while ((linha = reader.readLine()) != null) {
        linhasOriginais.add(linha);
      }
    }

    // 2. Contagem por tipo de registro (exceto bloco 9)
    Map<String, Integer> contagemPorRegistro = new LinkedHashMap<>();
    List<String> linhasSemBloco9 = new ArrayList<>();

    int qtdLinhasBloco0 = 0;
    int indiceUltimaLinhaBloco0 = -1;

    for (int i = 0; i < linhasOriginais.size(); i++) {
      String linha = linhasOriginais.get(i);
      String[] partes = linha.split("\\|");
      if (partes.length > 1) {
        String reg = partes[1];

        // Ignorar bloco 9 (será recriado)
        if (reg.equals("9900") || reg.equals("9990") || reg.equals("9999")) {
          continue;
        }

        // Remover 0990 antiga
        if (reg.equals("0990")) {
          continue;
        }

        contagemPorRegistro.put(reg, contagemPorRegistro.getOrDefault(reg, 0) + 1);
        linhasSemBloco9.add(linha);

        // Contar linhas do bloco 0
        if (reg.startsWith("0")) {
          qtdLinhasBloco0++;
          indiceUltimaLinhaBloco0 = linhasSemBloco9.size() - 1;
        }
      }
    }

    // 3. Inserir nova 0990 após último registro do bloco 0
    qtdLinhasBloco0 += 1; // soma o próprio 0990
    String novo0990 = String.format("|0990|%d|", qtdLinhasBloco0);
    linhasSemBloco9.add(indiceUltimaLinhaBloco0 + 1, novo0990);

    // 4. Criar nova lista para escrita final
    List<String> novasLinhas = new ArrayList<>(linhasSemBloco9);

    // 5. Gerar 9900 para todos os registros
    List<String> registros9900 = new ArrayList<>();
    for (Map.Entry<String, Integer> entry : contagemPorRegistro.entrySet()) {
      registros9900.add(String.format("|9900|%s|%d|", entry.getKey(), entry.getValue()));
    }

    // 6. Adicionar 9900 de si mesmo, 9990 e 9999
    int qtdTipos = registros9900.size();
    registros9900.add(String.format("|9900|0990|1|")); // ← esta é a linha que estava faltando
    registros9900.add(String.format("|9900|9900|%d|", qtdTipos + 4)); // +0990, 9990, 9999
    registros9900.add("|9900|9990|1|");
    registros9900.add("|9900|9999|1|");

    // 7. Adicionar bloco 9
    novasLinhas.addAll(registros9900);

    int totalBloco9 = registros9900.size() + 3; // +9990 +9999
    novasLinhas.add(String.format("|9990|%d|", totalBloco9));

    int totalArquivo = novasLinhas.size() + 1;
    novasLinhas.add(String.format("|9999|%d|", totalArquivo));

    // 8. Escrever arquivo
    try (BufferedWriter writer = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(caminhoArquivo), "ISO-8859-1"))) {
      for (String l : novasLinhas) {
        writer.write(l);
        writer.newLine();
      }
    }

    System.out.println("Totalizadores corrigidos com sucesso.");
  }

  public static void main(String[] args) throws IOException {
    copiar0300();
  }

}
