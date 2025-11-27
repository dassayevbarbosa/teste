package br.com.dbs.teste;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XmlProdutosParaExcel {

    private static final int PRINT_INTERVAL_FILES = 10;

    public static void converter(String diretorioXmls, String caminhoExcelSaida, int totalNotasExcel) throws Exception {
        long inicio = System.currentTimeMillis();

        System.out.println("Localizando arquivos XML de: " + diretorioXmls);

        // File[] arquivos = new File(diretorioXmls).listFiles((dir, name) ->
        // name.toLowerCase().endsWith(".xml"));
        File[] arquivos = Files.walk(Paths.get(diretorioXmls))
                .filter(p -> Files.isRegularFile(p) && p.toString().toLowerCase().endsWith(".xml"))
                .map(Path::toFile)
                .toArray(File[]::new);
        if (arquivos == null || arquivos.length == 0) {
            System.out.println("Nenhum XML encontrado.");
            return;
        }

        final int totalFiles = arquivos.length;
        int xmlsProcessados = 0;

        DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        System.out.println("Carregando dados dos " + totalFiles + " arquivos XML");
        // Primeiro passo: descobrir todas as colunas
        Set<String> todasAsColunas = new LinkedHashSet<>();
        for (File xml : arquivos) {
            try {
                Document doc = dBuilder.parse(xml);
                doc.getDocumentElement().normalize();

                Map<String, String> dadosFixos = extrairCamposFixos(doc);
                NodeList listaDet = doc.getElementsByTagName("det");
                for (int i = 0; i < listaDet.getLength(); i++) {
                    Element det = (Element) listaDet.item(i);
                    Map<String, String> linha = new LinkedHashMap<>(dadosFixos);
                    linha.put("det/@nItem", det.getAttribute("nItem"));
                    extrairCamposRecursivo(det, "det", linha);
                    todasAsColunas.addAll(linha.keySet());
                }
            } catch (Exception e) {
                System.err.println("Erro ao ler colunas de " + xml.getName() + ": " + e.getMessage());
            }

            xmlsProcessados++;

            if (xmlsProcessados % PRINT_INTERVAL_FILES == 0 || xmlsProcessados == totalFiles) {
                printProgress(xmlsProcessados, totalFiles, xml.getName(), 0);
            }
        }

        List<String> colunasOrdenadas = new ArrayList<>(todasAsColunas);

        int arquivoContador = 1;
        xmlsProcessados = 0;

        SXSSFWorkbook workbook = null;
        Sheet sheet = null;
        FileOutputStream fos = null;
        int rowIndex = 0;

        int totalRowsWritten = 0;

        try {
            System.out.println("Salvnados os dados na planilha Excel: " + caminhoExcelSaida);
            for (int idx = 0; idx < arquivos.length; idx++) {
                if (xmlsProcessados % totalNotasExcel == 0) {
                    // Fecha o arquivo anterior, se existir
                    if (workbook != null && fos != null) {
                        workbook.write(fos);
                        fos.close();
                        workbook.dispose();
                    }

                    // Cria um novo arquivo
                    String caminhoArquivoAtual = caminhoExcelSaida.replace(".xlsx", "_" + arquivoContador + ".xlsx");
                    workbook = new SXSSFWorkbook(100);
                    workbook.setCompressTempFiles(true);
                    sheet = workbook.createSheet("Produtos");

                    // Cabeçalho
                    Row headerRow = sheet.createRow(0);
                    for (int i = 0; i < colunasOrdenadas.size(); i++) {
                        headerRow.createCell(i).setCellValue(colunasOrdenadas.get(i));
                    }
                    rowIndex = 1; // Reseta linha para o próximo arquivo

                    fos = new FileOutputStream(caminhoArquivoAtual);

                    // System.out.println("Criando novo arquivo: " + caminhoArquivoAtual);
                    arquivoContador++;
                }

                File xml = arquivos[idx];
                try {
                    Document doc = dBuilder.parse(xml);
                    doc.getDocumentElement().normalize();

                    Map<String, String> dadosFixos = extrairCamposFixos(doc);
                    NodeList listaDet = doc.getElementsByTagName("det");
                    for (int i = 0; i < listaDet.getLength(); i++) {
                        Element det = (Element) listaDet.item(i);
                        Map<String, String> linha = new LinkedHashMap<>(dadosFixos);
                        linha.put("det/@nItem", det.getAttribute("nItem"));
                        extrairCamposRecursivo(det, "det", linha);

                        Row row = sheet.createRow(rowIndex++);
                        for (int j = 0; j < colunasOrdenadas.size(); j++) {
                            String valor = linha.getOrDefault(colunasOrdenadas.get(j), "");
                            row.createCell(j).setCellValue(valor.trim());
                        }
                        totalRowsWritten++;
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao processar " + xml.getName() + ": " + e.getMessage());
                }

                xmlsProcessados++;

                if (xmlsProcessados % PRINT_INTERVAL_FILES == 0 || xmlsProcessados == totalFiles) {
                    printProgress(xmlsProcessados, totalFiles, arquivos[idx].getName(), totalRowsWritten);
                }
            }
        } finally {
            // Fecha o último arquivo aberto
            if (workbook != null && fos != null) {
                workbook.write(fos);
                fos.close();
                workbook.dispose();
            }
        }

        long fim = System.currentTimeMillis();
        System.out.println("Processo concluído em " + (fim - inicio) / 1000 + " segundos.");
    }

    private static void printProgress(int processed, int total, String currentFileName, int totalRows) {
        int width = 40; // largura da barra
        double fraction = total == 0 ? 0 : (double) processed / total;
        int filled = (int) Math.round(fraction * width);

        StringBuilder bar = new StringBuilder();
        bar.append('[');
        for (int i = 0; i < filled; i++)
            bar.append('=');
        for (int i = filled; i < width; i++)
            bar.append(' ');
        bar.append(']');

        int percent = (int) Math.round(fraction * 100);

        String msg = String.format(
                "Progresso: %s %3d%% — %d/%d arquivos - atual: %s " + ((totalRows > 0) ? "— linhas: %d" : ""),
                bar.toString(), percent, processed, total, currentFileName, totalRows);

        System.out.print("\r" + msg); // <-- sobrescreve a mesma linha
        System.out.flush(); // <-- garante atualização imediata

    }

    public static void converterOld(String diretorioXmls, String caminhoExcelSaida) throws Exception {
        long inicio = System.currentTimeMillis();

        File[] arquivos = new File(diretorioXmls).listFiles((dir, name) -> name.toLowerCase().endsWith(".xml"));
        if (arquivos == null || arquivos.length == 0) {
            System.out.println("Nenhum XML encontrado.");
            return;
        }

        DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        // Primeiro passo: descobrir todas as colunas
        Set<String> todasAsColunas = new LinkedHashSet<>();
        for (File xml : arquivos) {
            try {
                Document doc = dBuilder.parse(xml);
                doc.getDocumentElement().normalize();

                Map<String, String> dadosFixos = extrairCamposFixos(doc);
                NodeList listaDet = doc.getElementsByTagName("det");
                for (int i = 0; i < listaDet.getLength(); i++) {
                    Element det = (Element) listaDet.item(i);
                    Map<String, String> linha = new LinkedHashMap<>(dadosFixos);
                    linha.put("det/@nItem", det.getAttribute("nItem"));
                    extrairCamposRecursivo(det, "det", linha);
                    todasAsColunas.addAll(linha.keySet());
                }
            } catch (Exception e) {
                System.err.println("Erro ao ler colunas de " + xml.getName() + ": " + e.getMessage());
            }
        }

        List<String> colunasOrdenadas = new ArrayList<>(todasAsColunas);

        // Segundo passo: gerar Excel direto
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
                FileOutputStream fos = new FileOutputStream(caminhoExcelSaida)) {

            workbook.setCompressTempFiles(true);
            Sheet sheet = workbook.createSheet("Produtos");

            // Cabeçalho
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < colunasOrdenadas.size(); i++) {
                headerRow.createCell(i).setCellValue(colunasOrdenadas.get(i));
            }

            int rowIndex = 1;
            for (File xml : arquivos) {
                try {
                    Document doc = dBuilder.parse(xml);
                    doc.getDocumentElement().normalize();

                    Map<String, String> dadosFixos = extrairCamposFixos(doc);
                    NodeList listaDet = doc.getElementsByTagName("det");
                    for (int i = 0; i < listaDet.getLength(); i++) {
                        Element det = (Element) listaDet.item(i);
                        Map<String, String> linha = new LinkedHashMap<>(dadosFixos);
                        linha.put("det/@nItem", det.getAttribute("nItem"));
                        extrairCamposRecursivo(det, "det", linha);

                        Row row = sheet.createRow(rowIndex++);
                        for (int j = 0; j < colunasOrdenadas.size(); j++) {
                            String valor = linha.getOrDefault(colunasOrdenadas.get(j), "");
                            row.createCell(j).setCellValue(valor.trim());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao processar " + xml.getName() + ": " + e.getMessage());
                }
            }

            workbook.write(fos);
        }

        long fim = System.currentTimeMillis();
        System.out.println("Excel gerado com sucesso em " + (fim - inicio) / 1000 + " segundos.");
    }

    private static Map<String, String> extrairCamposFixos(Document doc) {
        Map<String, String> dados = new LinkedHashMap<>();
        Element root = doc.getDocumentElement();
        NodeList filhos = root.getChildNodes();

        for (int i = 0; i < filhos.getLength(); i++) {
            Node node = filhos.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) node;
                if (!el.getTagName().equals("det")) {
                    extrairCamposRecursivoIgnorandoDet(el, el.getTagName(), dados);
                }
            }
        }
        return dados;
    }

    private static void extrairCamposRecursivo(Element element, String prefixo, Map<String, String> map) {
        NodeList filhos = element.getChildNodes();
        boolean temFilhos = false;

        for (int i = 0; i < filhos.getLength(); i++) {
            Node filho = filhos.item(i);
            if (filho.getNodeType() == Node.ELEMENT_NODE) {
                temFilhos = true;
                extrairCamposRecursivo((Element) filho, prefixo + "/" + filho.getNodeName(), map);
            }
        }

        if (!temFilhos) {
            map.put(prefixo, element.getTextContent().trim());
        }
    }

    private static void extrairCamposRecursivoIgnorandoDet(Element element, String prefixo, Map<String, String> map) {
        if ("det".equals(element.getTagName()))
            return;

        NodeList filhos = element.getChildNodes();
        boolean temFilhos = false;

        for (int i = 0; i < filhos.getLength(); i++) {
            Node filho = filhos.item(i);
            if (filho.getNodeType() == Node.ELEMENT_NODE) {
                temFilhos = true;
                extrairCamposRecursivoIgnorandoDet((Element) filho, prefixo + "/" + filho.getNodeName(), map);
            }
        }

        if (!temFilhos) {
            map.put(prefixo, element.getTextContent().trim());
        }
    }

    public static void main(String[] args) throws Exception {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Digite o caminho da pasta com os XMLs: ");
            String pastaXmls = scanner.nextLine();

            System.out.print("Digite o caminho completo para salvar o Excel (.xlsx): ");
            String caminhoExcel = scanner.nextLine();

            System.out.print("Total de notas por planilha: ");
            String strTotalNotaExcel = scanner.nextLine();
            int totalNotasExcel = Integer.parseInt(strTotalNotaExcel);

            converter(pastaXmls, caminhoExcel, totalNotasExcel);
        }
    }
}
