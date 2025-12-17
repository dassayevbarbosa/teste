package br.com.dbs.teste;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class BController {

  // Padrão de data de saída no banco (TEXT)
  private static final SimpleDateFormat OUT_DATE = new SimpleDateFormat("yyyy-MM-dd");

  // Padrões para extrair número da nota após "DANFE-D"
  private static final Pattern DANFE_PATTERN = Pattern.compile("DANFE-D\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

  /**
   * Importa a planilha para o SQLite.
   *
   * @param excelPath  caminho do arquivo Excel (ex: "/mnt/data/Razão
   *                   01-2021.xls")
   * @param sqlitePath caminho do arquivo SQLite (ex: "/home/user/razao.db")
   * @throws Exception em caso de erro de IO/SQL
   */
  public static void importRazaoFromExcel(Connection conn, String excelPath) throws Exception {
    FileInputStream fis = null;
    Workbook wb = null;
    PreparedStatement insertStmt = null;

    DataFormatter formatter = new DataFormatter(); // para ler valores como string quando necessário

    try {
      // 1) Abre workbook (suporta .xls e .xlsx)
      fis = new FileInputStream(new File(excelPath));
      wb = WorkbookFactory.create(fis);

      try (Statement stmt = conn.createStatement()) {
        String createSql = "CREATE TABLE IF NOT EXISTS razao (" +
            "data TEXT," +
            "lote TEXT," +
            "numero_nota TEXT," +
            "valor REAL," +
            "status TEXT DEFAULT 'A'" +
            ");";
        stmt.execute(createSql);
      }

      // 3) Prepara insert
      String insertSql = "INSERT INTO razao(data, lote, numero_nota, valor, status) VALUES (?, ?, ?, ?, ?);";
      insertStmt = conn.prepareStatement(insertSql);

      // 4) Itera linhas da primeira sheet
      if (wb.getNumberOfSheets() == 0) {
        throw new Exception("O arquivo Excel não contém nenhuma planilha.");
      }

      Iterator<Row> rowIt = wb.getSheetAt(0).iterator();
      int inserted = 0;

      while (rowIt.hasNext()) {
        Row row = rowIt.next();

        // colunas (índices base 0): A=0, E=4, K=10, X=23
        Cell cellData = row.getCell(0);
        Cell cellLote = row.getCell(4);
        Cell cellTextoNota = row.getCell(10);
        Cell cellValor = row.getCell(23);

        // Ignorar linhas sem valor preenchido (coluna X)
        if (cellValor == null || isCellBlank(cellValor)) {
          continue;
        }

        // --- Ler valor (converter strings com vírgula e símbolos caso necessário)
        Double valor = parseNumericCell(cellValor, formatter);
        if (valor == null) {
          // se não conseguiu parsear valor, ignora a linha
          continue;
        }

        // --- Ler data (tenta interpretar como data; se for texto, tenta vários
        // formatos)
        String dataStr = null;
        if (cellData != null && !isCellBlank(cellData)) {
          dataStr = parseDateCellToIso(cellData, formatter);
        }

        // --- Ler lote
        String lote = (cellLote == null || isCellBlank(cellLote)) ? null : formatter.formatCellValue(cellLote).trim();

        // --- Extrair número da nota do texto (coluna K)
        String numeroNota = null;
        if (cellTextoNota != null && !isCellBlank(cellTextoNota)) {
          String texto = formatter.formatCellValue(cellTextoNota);
          numeroNota = extractNumeroNotaFromText(texto);
        }

        // --- status padrão 'A'
        String status = "A";

        // Insere (data como texto YYYY-MM-DD ou null)
        insertStmt.setString(1, dataStr);
        insertStmt.setString(2, lote);
        insertStmt.setString(3, numeroNota);
        insertStmt.setDouble(4, valor);
        insertStmt.setString(5, status);

        insertStmt.addBatch();
        inserted++;

        // opcional: comitar a cada N registros para grandes planilhas
        if (inserted % 500 == 0) {
          insertStmt.executeBatch();
          conn.commit();
        }
      }

      // executa e commita o restante
      insertStmt.executeBatch();
      conn.commit();

      System.out.println("Import concluída. Registros inseridos: " + inserted);

    } finally {
      // fechar recursos
      if (insertStmt != null)
        try {
          insertStmt.close();
        } catch (SQLException ex) {
        }
      if (wb != null)
        try {
          wb.close();
        } catch (Exception ex) {
        }
      if (fis != null)
        try {
          fis.close();
        } catch (Exception ex) {
        }
    }
  }

  // ---------- Helpers ----------

  /**
   * Arredonda um valor double para 2 casas decimais e trata valores muito
   * pequenos como zero
   * (resolve problemas de precisão de ponto flutuante)
   */
  private static double arredondarValor(double valor) {
    // Se o valor absoluto for muito pequeno (erro de precisão), considera como zero
    if (Math.abs(valor) < 0.0001) {
      return 0.0;
    }
    // Arredonda para 2 casas decimais
    return Math.round(valor * 100.0) / 100.0;
  }

  private static boolean isCellBlank(Cell c) {
    if (c == null)
      return true;
    if (c.getCellType() == CellType.BLANK)
      return true;
    if (c.getCellType() == CellType.STRING && c.getStringCellValue().trim().isEmpty())
      return true;
    return false;
  }

  private static Double parseNumericCell(Cell c, DataFormatter formatter) {
    try {
      if (c.getCellType() == CellType.NUMERIC) {
        return c.getNumericCellValue();
      } else {
        // pode vir como texto "1.234,56" ou "R$ 1.234,56" etc.
        String s = formatter.formatCellValue(c).trim();
        if (s.isEmpty())
          return null;

        // remover símbolos e normalizar: remover tudo exceto dígitos, '.' e ','
        // primeiro remove moeda e letras
        s = s.replaceAll("[^0-9,\\.\\-]", "");

        // se contém '.' e ',', assumir formato brasileiro (milhares '.' e decimal ',')
        if (s.indexOf(',') >= 0 && s.indexOf('.') >= 0) {
          // remover pontos (milhares), trocar vírgula por ponto
          s = s.replace(".", "").replace(",", ".");
        } else if (s.indexOf(',') >= 0 && s.indexOf('.') == -1) {
          // apenas vírgula -> trocar por ponto
          s = s.replace(",", ".");
        } // caso já seja "1234.56" ok

        if (s.isEmpty())
          return null;
        return Double.parseDouble(s);
      }
    } catch (Exception ex) {
      return null;
    }
  }

  private static String parseDateCellToIso(Cell c, DataFormatter formatter) {
    try {
      if (c.getCellType() == CellType.NUMERIC) {
        if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(c)) {
          Date d = c.getDateCellValue();
          return OUT_DATE.format(d);
        } else {
          // pode ser número que representa data; tentar como date
          Date d = org.apache.poi.ss.usermodel.DateUtil.getJavaDate(c.getNumericCellValue());
          return OUT_DATE.format(d);
        }
      } else {
        String s = formatter.formatCellValue(c).trim();
        if (s.isEmpty())
          return null;
        // tentar vários formatos comuns
        String[] patterns = { "dd/MM/yyyy", "d/M/yyyy", "yyyy-MM-dd", "MM/dd/yyyy" };
        for (String p : patterns) {
          try {
            Date d = new SimpleDateFormat(p).parse(s);
            return OUT_DATE.format(d);
          } catch (ParseException e) {
            // tenta próximo
          }
        }
        // tentativa fallback: retornar string original (ou null)
        // aqui retornaremos null se não conseguimos parsear para padrão ISO
        return null;
      }
    } catch (Exception ex) {
      return null;
    }
  }

  private static String extractNumeroNotaFromText(String texto) {
    if (texto == null)
      return null;
    Matcher m = DANFE_PATTERN.matcher(texto);
    if (m.find()) {
      return m.group(1).trim();
    }
    // se não encontrado com "DANFE-D", tentar encontrar primeiro grupo de dígitos
    // com probabilidade
    Matcher m2 = Pattern.compile("\\b(\\d{5,})\\b").matcher(texto);
    if (m2.find()) {
      return m2.group(1);
    }
    return null;
  }

  /*
   * ANÁLISE DE VALORE
   */

  public static class RazaoRecord {
    public final long rowid;
    public final String data;
    public final String lote;
    public final String numeroNota;
    public final double valor;
    public final String status;

    public RazaoRecord(long rowid, String data, String lote, String numeroNota, double valor, String status) {
      this.rowid = rowid;
      this.data = data;
      this.lote = lote;
      this.numeroNota = numeroNota;
      this.valor = valor;
      this.status = status;
    }
  }

  public static class Result {
    public final List<RazaoRecord> selected;
    public final double sum;

    public Result(List<RazaoRecord> selected, double sum) {
      this.selected = selected;
      this.sum = sum;
    }
  }

  public static class RegistroExportacao {
    public final String dataDebito;
    public final String data;
    public final String numeroNota;
    public final double valor;
    public final double totalDebito;

    public RegistroExportacao(String dataDebito, String data, String numeroNota, double valor, double totalDebito) {
      this.dataDebito = dataDebito;
      this.data = data;
      this.numeroNota = numeroNota;
      this.valor = valor;
      this.totalDebito = totalDebito;
    }
  }

  /**
   * Encontra subconjunto com soma <= targetValue e o mais próximo possível do
   * target.
   *
   * @param conn        conexão JDBC (ativa)
   * @param targetValue valor alvo (em Reais, double)
   * @param dateIso     data no formato compatível com a coluna 'data' (ex:
   *                    "yyyy-MM-dd")
   * @return Result contendo lista de registros selecionados e soma obtida (sempre
   *         <= targetValue)
   * @throws SQLException em caso de erro SQL
   */
  public static Result findBestMatch(Connection conn, double targetValue, String dateIso) throws SQLException {

    long targetCents = Math.round(targetValue * 100.0);

    List<RazaoRecord> selectedAll = new ArrayList<>();
    long sumCentsTotal = 0L;

    // 1) tenta fechar com registros do próprio dia
    List<RazaoRecord> items = loadRazaoItemsByDate(conn, dateIso);
    if (!items.isEmpty()) {
      long[] values = toCentsArray(items);
      Result r1 = subsetSumMaxLEQ(items, values, targetCents);
      if (!r1.selected.isEmpty()) {
        selectedAll.addAll(r1.selected);
        sumCentsTotal += Math.round(r1.sum * 100.0);
      }
    }

    // 2) se ainda faltar, considera o PRIMEIRO dia anterior disponível (maior data
    // < dateIso)
    long remainingCents = targetCents - sumCentsTotal;
    if (remainingCents > 0) {
      String prevDate = findPreviousRazaoDateWithAvailableItems(conn, dateIso);
      if (prevDate != null && !prevDate.isBlank()) {
        List<RazaoRecord> prevItems = loadRazaoItemsByDate(conn, prevDate);
        if (!prevItems.isEmpty()) {
          long[] prevValues = toCentsArray(prevItems);
          Result r2 = subsetSumMaxLEQ(prevItems, prevValues, remainingCents);
          if (!r2.selected.isEmpty()) {
            selectedAll.addAll(r2.selected);
            sumCentsTotal += Math.round(r2.sum * 100.0);
          }
        }
      }
    }

    if (selectedAll.isEmpty()) {
      return new Result(Collections.emptyList(), 0.0);
    }

    // Marcar como utilizados (U) os registros efetivamente usados na soma
    // (incluindo dia anterior, se usado)
    markRazaoRecordsAsUsed(conn, selectedAll);

    // refletir o novo status na lista retornada
    List<RazaoRecord> updatedSelected = new ArrayList<>(selectedAll.size());
    for (RazaoRecord r : selectedAll) {
      updatedSelected.add(new RazaoRecord(r.rowid, r.data, r.lote, r.numeroNota, r.valor, "U"));
    }
    return new Result(updatedSelected, sumCentsTotal / 100.0);
  }

  private static long[] toCentsArray(List<RazaoRecord> items) {
    int n = items.size();
    long[] values = new long[n];
    for (int i = 0; i < n; i++) {
      values[i] = Math.round(items.get(i).valor * 100.0);
    }
    return values;
  }

  private static List<RazaoRecord> loadRazaoItemsByDate(Connection conn, String dateIso) throws SQLException {
    String sql = """
            SELECT rowid, data, lote, numero_nota, valor, status
            FROM razao
            WHERE data = ?
              AND valor IS NOT NULL
              AND numero_nota IS NOT NULL
              AND status = 'A'
              AND valor > 0
        """;

    List<RazaoRecord> items = new ArrayList<>();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, dateIso);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          items.add(new RazaoRecord(
              rs.getLong("rowid"),
              rs.getString("data"),
              rs.getString("lote"),
              rs.getString("numero_nota"),
              rs.getDouble("valor"),
              rs.getString("status")));
        }
      }
    }
    return items;
  }

  private static String findPreviousRazaoDateWithAvailableItems(Connection conn, String dateIso) throws SQLException {
    // data está em ISO (yyyy-MM-dd) como TEXT, então MAX/ORDER funciona
    // corretamente
    String sql = """
            SELECT MAX(data) AS prev_date
            FROM razao
            WHERE data < ?
              AND valor IS NOT NULL
              AND numero_nota IS NOT NULL
              AND status = 'A'
              AND valor > 0
        """;

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, dateIso);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getString("prev_date");
        }
      }
    }
    return null;
  }

  private static void markRazaoRecordsAsUsed(Connection conn, List<RazaoRecord> selected) throws SQLException {
    if (selected == null || selected.isEmpty()) {
      return;
    }

    String updateSql = "UPDATE razao SET status = 'U' WHERE rowid = ? AND status = 'A';";
    try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
      for (RazaoRecord r : selected) {
        ps.setLong(1, r.rowid);
        ps.addBatch();
      }
      ps.executeBatch();
    }
  }

  private static Result subsetSumMaxLEQ(
      List<RazaoRecord> items,
      long[] values,
      long targetCents) {

    int T = (int) targetCents;

    boolean[] dp = new boolean[T + 1];
    int[] prev = new int[T + 1];
    Arrays.fill(prev, -1);

    dp[0] = true;

    for (int i = 0; i < values.length; i++) {
      int v = (int) values[i];
      if (v <= 0 || v > T)
        continue;

      for (int s = T; s >= v; s--) {
        if (!dp[s] && dp[s - v]) {
          dp[s] = true;
          prev[s] = i;
        }
      }
    }

    // 🔥 prioridade absoluta: soma EXATA
    int bestSum = -1;
    if (dp[T]) {
      bestSum = T;
    } else {
      // caso não exista soma exata, pegar a maior possível < target
      for (int s = T - 1; s >= 0; s--) {
        if (dp[s]) {
          bestSum = s;
          break;
        }
      }
    }

    if (bestSum <= 0) {
      return new Result(Collections.emptyList(), 0.0);
    }

    // reconstrução do subconjunto
    List<RazaoRecord> selected = new ArrayList<>();
    boolean[] used = new boolean[items.size()];
    int cur = bestSum;

    while (cur > 0) {
      int idx = prev[cur];
      if (idx < 0 || used[idx])
        break;

      used[idx] = true;
      selected.add(items.get(idx));
      cur -= values[idx];
    }

    Collections.reverse(selected);
    return new Result(selected, bestSum / 100.0);
  }

  public static void processarDebitos(Connection conn) throws SQLException {

    // Conta o total de registros para mostrar o progresso
    int totalRegistros = 0;
    String countSql = "SELECT COUNT(*) as total FROM debitos";
    try (PreparedStatement countPs = conn.prepareStatement(countSql);
        ResultSet countRs = countPs.executeQuery()) {
      if (countRs.next()) {
        totalRegistros = countRs.getInt("total");
      }
    }

    if (totalRegistros == 0) {
      System.out.println("Nenhum registro encontrado na tabela debitos.");
      return;
    }

    System.out.println("Total de registros a processar: " + totalRegistros);
    System.out.println("Iniciando processamento...\n");

    String sql = """
            SELECT rowid, id, data, valor
            FROM debitos
        """;

    Set<String> datasProcessadas = new HashSet<>();
    int processados = 0;
    long inicioTempo = System.currentTimeMillis();

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String dataDebito = rs.getString("data");
          Double valor = rs.getDouble("valor");

          Result result = findBestMatch(conn, valor, dataDebito);

          // Executa delete apenas na primeira vez que a data aparecer
          if (!datasProcessadas.contains(dataDebito)) {
            String deleteSql = "DELETE FROM result WHERE data = ?;";
            PreparedStatement deleteStmt = conn.prepareStatement(deleteSql);
            deleteStmt.setString(1, dataDebito);
            deleteStmt.execute();
            deleteStmt.close();
            datasProcessadas.add(dataDebito);
          }

          for (RazaoRecord record : result.selected) {
            String insertSql = "INSERT INTO result(data_debito, data, valor, numero_nota) VALUES (?, ?, ?, ?);";
            PreparedStatement insertStmt = conn.prepareStatement(insertSql);
            // Sempre gravar a data do débito analisado (mesmo se o match usar registros do
            // dia anterior)
            insertStmt.setString(1, dataDebito);
            insertStmt.setString(2, record.data);
            insertStmt.setDouble(3, record.valor);
            insertStmt.setString(4, record.numeroNota);
            insertStmt.execute();
            insertStmt.close();
          }

          processados++;

          // Mostra progresso a cada 10 registros ou no último
          if (processados % 10 == 0 || processados == totalRegistros) {
            double percentual = (processados * 100.0) / totalRegistros;
            long tempoDecorrido = System.currentTimeMillis() - inicioTempo;
            long tempoEstimado = processados > 0
                ? (tempoDecorrido * (totalRegistros - processados)) / processados
                : 0;

            conn.commit();

            System.out.printf("Progresso: %d/%d (%.1f%%) | Tempo decorrido: %d ms | Tempo estimado restante: %d ms%n",
                processados, totalRegistros, percentual, tempoDecorrido, tempoEstimado);
          }
        }
      }
    }

    long tempoTotal = System.currentTimeMillis() - inicioTempo;
    System.out.printf("\nProcessamento concluído! Total: %d registros processados em %d ms (%.2f segundos)%n",
        processados, tempoTotal, tempoTotal / 1000.0);
  }

  public static void exportarExcel(Connection conn) throws SQLException, IOException {

    // Conta o total de registros para mostrar o progresso
    int totalRegistros = 0;
    String countSql = "select count(*) as total from result;";
    try (PreparedStatement countPs = conn.prepareStatement(countSql);
        ResultSet countRs = countPs.executeQuery()) {
      if (countRs.next()) {
        totalRegistros = countRs.getInt("total");
      }
    }

    if (totalRegistros == 0) {
      System.out.println("Nenhum registro encontrado na tabela result.");
      return;
    }

    System.out.println("Total de registros a processar: " + totalRegistros);
    System.out.println("Iniciando processamento...\n");

    String sql = """
             select r.*, d.valor as total_debito
             from result r inner join debitos d on r.data_debito = d.data
             order by r.data_debito;
        """;

    // Estrutura: Map<Ano, Map<Mês, List<Registro>>>
    Map<Integer, Map<Integer, List<RegistroExportacao>>> dadosPorAnoMes = new TreeMap<>();

    // Lê todos os dados e agrupa por ano e mês
    try (PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        String dataDebitoStr = rs.getString("data_debito");
        String dataStr = rs.getString("data");
        String numeroNota = rs.getString("numero_nota");
        double valor = rs.getDouble("valor");
        double totalDebito = rs.getDouble("total_debito");

        // Parse da data para extrair ano e mês
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date data = sdf.parse(dataDebitoStr);
        Calendar cal = Calendar.getInstance();
        cal.setTime(data);
        int ano = cal.get(Calendar.YEAR);
        int mes = cal.get(Calendar.MONTH) + 1; // Calendar.MONTH é 0-based

        dadosPorAnoMes.putIfAbsent(ano, new TreeMap<>());
        dadosPorAnoMes.get(ano).putIfAbsent(mes, new ArrayList<>());
        dadosPorAnoMes.get(ano).get(mes).add(
            new RegistroExportacao(dataDebitoStr, dataStr, numeroNota, valor, totalDebito));
      }
    } catch (ParseException e) {
      throw new SQLException("Erro ao processar data: " + e.getMessage(), e);
    }

    // Diretório de destino
    String diretorioDestino = "D:\\Seven\\Clientes\\AM COMERCIAL\\Resultados";
    File dir = new File(diretorioDestino);
    if (!dir.exists()) {
      dir.mkdirs();
    }

    int processados = 0;
    long inicioTempo = System.currentTimeMillis();

    // Processa cada ano
    for (Map.Entry<Integer, Map<Integer, List<RegistroExportacao>>> entradaAno : dadosPorAnoMes.entrySet()) {
      int ano = entradaAno.getKey();
      Map<Integer, List<RegistroExportacao>> dadosPorMes = entradaAno.getValue();

      // Cria um workbook para o ano
      XSSFWorkbook workbook = new XSSFWorkbook();

      // Processa cada mês
      for (Map.Entry<Integer, List<RegistroExportacao>> entradaMes : dadosPorMes.entrySet()) {
        int mes = entradaMes.getKey();
        List<RegistroExportacao> registros = entradaMes.getValue();

        // Nome da planilha (ex: "Janeiro", "Fevereiro", etc.)
        String[] nomesMeses = { "", "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro" };
        String nomePlanilha = nomesMeses[mes];
        XSSFSheet sheet = workbook.createSheet(nomePlanilha);

        // Cria estilos para cabeçalho e totais
        // Estilo do cabeçalho: negrito, letra branca, fundo azul marinho
        XSSFCellStyle headerStyle = workbook.createCellStyle();
        XSSFFont headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(new XSSFColor(new java.awt.Color(255, 255, 255), null)); // Branco
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(0, 32, 96), null)); // Azul marinho
        headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

        // Estilo dos totais: negrito, letra branca, fundo azul mais claro
        XSSFCellStyle totalStyle = workbook.createCellStyle();
        XSSFFont totalFont = workbook.createFont();
        totalFont.setBold(true);
        totalFont.setColor(new XSSFColor(new java.awt.Color(255, 255, 255), null)); // Branco
        totalStyle.setFont(totalFont);
        totalStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(31, 78, 121), null)); // Azul mais claro
        totalStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

        // Cabeçalho
        XSSFRow headerRow = sheet.createRow(0);
        org.apache.poi.xssf.usermodel.XSSFCell cell0 = headerRow.createCell(0);
        cell0.setCellValue("Data Débito ");
        cell0.setCellStyle(headerStyle);
        org.apache.poi.xssf.usermodel.XSSFCell cell1 = headerRow.createCell(1);
        cell1.setCellValue("Data");
        cell1.setCellStyle(headerStyle);
        org.apache.poi.xssf.usermodel.XSSFCell cell2 = headerRow.createCell(2);
        cell2.setCellValue("Numero Nota");
        cell2.setCellStyle(headerStyle);
        org.apache.poi.xssf.usermodel.XSSFCell cell3 = headerRow.createCell(3);
        cell3.setCellValue("Valor");
        cell3.setCellStyle(headerStyle);
        org.apache.poi.xssf.usermodel.XSSFCell cell4 = headerRow.createCell(4);
        cell4.setCellValue("Total Débito");
        cell4.setCellStyle(headerStyle);
        org.apache.poi.xssf.usermodel.XSSFCell cell5 = headerRow.createCell(5);
        cell5.setCellValue("Diferença");
        cell5.setCellStyle(headerStyle);

        int linhaAtual = 1;
        String dataAnterior = null;
        double somaDia = 0.0;
        double totalDebitoDia = 0.0;

        // Processa cada registro
        for (RegistroExportacao registro : registros) {
          // Se mudou o dia, processa o dia anterior
          if (dataAnterior != null && !dataAnterior.equals(registro.dataDebito)) {
            // Adiciona linha de total do dia anterior
            double diferenca = arredondarValor(somaDia - totalDebitoDia);
            XSSFRow totalRow = sheet.createRow(linhaAtual);
            org.apache.poi.xssf.usermodel.XSSFCell totalCell0 = totalRow.createCell(0);
            totalCell0.setCellValue("Total");
            totalCell0.setCellStyle(totalStyle);
            org.apache.poi.xssf.usermodel.XSSFCell totalCell2 = totalRow.createCell(3);
            totalCell2.setCellValue(arredondarValor(somaDia));
            totalCell2.setCellStyle(totalStyle);
            org.apache.poi.xssf.usermodel.XSSFCell totalCell3 = totalRow.createCell(4);
            totalCell3.setCellValue(arredondarValor(totalDebitoDia));
            totalCell3.setCellStyle(totalStyle);
            org.apache.poi.xssf.usermodel.XSSFCell totalCell4 = totalRow.createCell(5);
            totalCell4.setCellValue(diferenca);
            totalCell4.setCellStyle(totalStyle);
            linhaAtual++;

            // Deixa duas linhas em branco
            linhaAtual += 2;

            somaDia = 0.0;
            totalDebitoDia = 0.0;
          }

          // Nova linha de dados
          XSSFRow row = sheet.createRow(linhaAtual);
          row.createCell(0).setCellValue(registro.dataDebito);
          row.createCell(1).setCellValue(registro.data);
          row.createCell(2).setCellValue(registro.numeroNota != null ? registro.numeroNota : "");
          row.createCell(3).setCellValue(arredondarValor(registro.valor));

          somaDia += registro.valor;
          totalDebitoDia = registro.totalDebito; // Mantém o último total_debito do dia

          dataAnterior = registro.dataDebito;
          linhaAtual++;
          processados++;
        }

        // Processa o último dia
        if (dataAnterior != null) {
          double diferenca = arredondarValor(somaDia - totalDebitoDia);
          XSSFRow totalRow = sheet.createRow(linhaAtual);
          org.apache.poi.xssf.usermodel.XSSFCell totalCell0 = totalRow.createCell(0);
          totalCell0.setCellValue("Total");
          totalCell0.setCellStyle(totalStyle);
          org.apache.poi.xssf.usermodel.XSSFCell totalCell2 = totalRow.createCell(2);
          totalCell2.setCellValue(arredondarValor(somaDia));
          totalCell2.setCellStyle(totalStyle);
          org.apache.poi.xssf.usermodel.XSSFCell totalCell3 = totalRow.createCell(3);
          totalCell3.setCellValue(arredondarValor(totalDebitoDia));
          totalCell3.setCellStyle(totalStyle);
          org.apache.poi.xssf.usermodel.XSSFCell totalCell4 = totalRow.createCell(4);
          totalCell4.setCellValue(diferenca);
          totalCell4.setCellStyle(totalStyle);
        }

        // Ajusta largura das colunas
        sheet.setColumnWidth(0, 4000);
        sheet.setColumnWidth(1, 4000);
        sheet.setColumnWidth(2, 5000);
        sheet.setColumnWidth(3, 4000);
        sheet.setColumnWidth(4, 4000);
        sheet.setColumnWidth(5, 4000);
      }

      // Salva o arquivo do ano
      String caminhoArquivo = diretorioDestino + "\\Resultado_" + ano + ".xlsx";
      try (FileOutputStream fileOut = new FileOutputStream(caminhoArquivo)) {
        workbook.write(fileOut);
      }
      workbook.close();

      System.out.println("Arquivo criado: " + caminhoArquivo);
    }

    long tempoTotal = System.currentTimeMillis() - inicioTempo;
    System.out.printf("\nProcessamento concluído! Total: %d registros processados em %d ms (%.2f segundos)%n",
        processados, tempoTotal, tempoTotal / 1000.0);
  }

  public static void main(String[] args) throws Exception {
    String sqlitePath = "D:\\Seven\\Clientes\\AM COMERCIAL\\am_comercial.db";

    Class.forName("org.sqlite.JDBC");
    String jdbcUrl = "jdbc:sqlite:" + sqlitePath;
    Connection conn = DriverManager.getConnection(jdbcUrl);
    conn.setAutoCommit(false);

    try {
      // System.out.println("Importando Razão 2021");
      // String excel = "D:\\Seven\\Clientes\\AM COMERCIAL\\Razão 2021.xlsx";
      // importRazaoFromExcel(conn, excel);

      // System.out.println("Importando Razão 2022");
      // excel = "D:\\Seven\\Clientes\\AM COMERCIAL\\Razão 2022.xlsx";
      // importRazaoFromExcel(conn, excel);

      // System.out.println("Importando Razão 2023");
      // excel = "D:\\Seven\\Clientes\\AM COMERCIAL\\Razão 2023.xlsx";
      // importRazaoFromExcel(conn, excel);

      // System.out.println("Importando Razão 2024");
      // excel = "D:\\Seven\\Clientes\\AM COMERCIAL\\Razão 2024.xlsx";
      // importRazaoFromExcel(conn, excel);

      // Result result = findBestMatch(conn, 72547.82, "2021-01-07");
      // System.out.println(result.sum);
      // for (RazaoRecord record : result.selected) {
      // System.out.println(record.data + " - " + record.numeroNota + " - " +
      // record.valor);
      // }

      // processarDebitos(conn);
      exportarExcel(conn);
    } catch (Exception e) {
      e.printStackTrace();
      if (conn != null) {
        conn.close();
      }
    }
  }

}