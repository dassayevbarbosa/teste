/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Project/Maven2/JavaApp/src/main/java/${packagePath}/${mainClassName}.java to edit this template
 */

package br.com.dbs.teste;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.google.gson.Gson;

import br.com.dbs.api.ApiAppSeven;

/**
 *
 * @author dassa
 */
public class Teste {

    public static String separador = System.getProperty("line.separator");
    private static final String CONFIG_FILE = "config.properties";

    public static String getDadosXML(File xml) throws Exception {
        String chaveNfe = "";
        HashMap<String, String> dadosNota = new HashMap<String, String>();
        HashMap<String, String> emitente = new HashMap<String, String>();
        HashMap<String, String> destinatario = new HashMap<String, String>();
        ArrayList<HashMap<String, String>> produtos = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xml);

            // IDE
            NodeList ide = doc.getElementsByTagName("ide");
            if (ide.getLength() > 0) {
                Node no = ide.item(0);
                Element e = (Element) no;

                for (int j = 0; j < e.getChildNodes().getLength(); j++) {
                    String nodeName = e.getChildNodes().item(j).getNodeName();
                    String nodeValue = (e.getChildNodes().item(j).getTextContent());
                    dadosNota.put(nodeName, nodeValue);
                }
            }

            // Emitente
            NodeList emit = doc.getElementsByTagName("emit");
            if (emit.getLength() > 0) {
                Node no = emit.item(0);
                Element e = (Element) no;

                for (int j = 0; j < e.getChildNodes().getLength(); j++) {
                    String nodeName = e.getChildNodes().item(j).getNodeName();
                    String nodeValue = (e.getChildNodes().item(j).getTextContent());
                    if (nodeName.toLowerCase().equals("enderEmit".toLowerCase())) {
                        for (int i = 0; i < e.getChildNodes().item(j).getChildNodes().getLength(); i++) {
                            nodeName = e.getChildNodes().item(j).getChildNodes().item(i).getNodeName();
                            nodeValue = (e.getChildNodes().item(j).getChildNodes().item(i).getTextContent());
                            emitente.put(nodeName, nodeValue);
                        }
                    } else {
                        emitente.put(nodeName, nodeValue);
                    }
                }
            }

            // Destinatário
            NodeList dest = doc.getElementsByTagName("dest");
            if (dest.getLength() > 0) {
                Node no = dest.item(0);
                Element e = (Element) no;

                for (int j = 0; j < e.getChildNodes().getLength(); j++) {
                    String nodeName = e.getChildNodes().item(j).getNodeName();
                    String nodeValue = (e.getChildNodes().item(j).getTextContent());
                    if (nodeName.toLowerCase().equals("enderDest".toLowerCase())) {
                        for (int i = 0; i < e.getChildNodes().item(j).getChildNodes().getLength(); i++) {
                            nodeName = e.getChildNodes().item(j).getChildNodes().item(i).getNodeName();
                            nodeValue = (e.getChildNodes().item(j).getChildNodes().item(i).getTextContent());
                            destinatario.put(nodeName, nodeValue);
                        }
                    } else {
                        destinatario.put(nodeName, nodeValue);
                    }
                }
            }

            // Produtos
            NodeList det = doc.getElementsByTagName("det");
            if (det.getLength() > 0) {
                for (int n = 0; n < det.getLength(); n++) {
                    Node no = det.item(n);
                    Element e = (Element) no;

                    HashMap<String, String> produto = new HashMap<String, String>();
                    analisarNoXml(e.getChildNodes().item(0), produto, "");
                    analisarNoXml(e.getChildNodes().item(1), produto, "");
                    produtos.add(produto);
                }

            }

            // Chave
            NodeList infProt = doc.getElementsByTagName("infProt");
            if (infProt.getLength() > 0) {
                Node no = infProt.item(0);
                Element e = (Element) no;

                for (int j = 0; j < e.getChildNodes().getLength(); j++) {
                    String nodeName = e.getChildNodes().item(j).getNodeName();
                    if (nodeName.toUpperCase().equals("CHNFE")) {
                        chaveNfe = (e.getChildNodes().item(j).getTextContent());
                        break;
                    }
                }
            } else {
                chaveNfe = "Xml sem autorização";
            }

            String dadosSaida = "";
            String dadosSaidaNota = dadosNota.get("nNF") + ";" + dadosNota.get("mod") + ";" + dadosNota.get("serie")
                    + ";"
                    + chaveNfe + ";" + converterFormatoData(dadosNota.get("dhEmi")) + ";";
            dadosSaidaNota += emitente.get("CNPJ") + ";" + emitente.get("xNome") + ";" + emitente.get("cMun") + ";"
                    + emitente.get("xMun") + ";"
                    + emitente.get("UF") + ";";
            dadosSaidaNota += destinatario.get("CNPJ") + ";" + destinatario.get("xNome") + ";"
                    + destinatario.get("cMun")
                    + ";"
                    + destinatario.get("xMun") + ";"
                    + destinatario.get("UF") + ";";

            int cont = 0;
            for (HashMap<String, String> p : produtos) {
                cont++;
                if (cont > 1) {
                    dadosSaida += separador;
                }
                dadosSaida += dadosSaidaNota + p.get("cProd") + ";" + p.get("xProd") + ";" +
                        p.get("uCom") + ";"
                        + p.get("cEAN")
                        + ";" + p.get("NCM") + ";" + p.get("CFOP") + ";" + converterFormato(p.get("vUnCom")) + ";" +
                        converterFormato(p.get("qCom")) + ";" + converterFormato(p.get("vProd")) + ";" +
                        p.get("ICMS_orig") + p.get("ICMS_CST") + ";" + converterFormato(p.get("ICMS_vBC")) + ";" +
                        converterFormato(p.get("ICMS_pICMS")) +
                        ";" + converterFormato(p.get("ICMS_vICMS")) + ";" + p.get("PIS_CST") + ";"
                        + converterFormato(p.get("PIS_vBC")) +
                        ";"
                        + converterFormato(p.get("PIS_pPIS")) +
                        ";" + converterFormato(p.get("PIS_vPIS")) + ";" + p.get("COFINS_CST") + ";" +
                        converterFormato(p.get("COFINS_vBC")) + ";"
                        + converterFormato(p.get("COFINS_pCOFINS")) +
                        ";" + converterFormato(p.get("COFINS_vCOFINS")) + ";" + p.get("IPI_CST") + ";" +
                        converterFormato(p.get("IPI_vBC")) + ";"
                        + converterFormato(p.get("IPI_pIPI")) +
                        ";" + converterFormato(p.get("IPI_vIPI")) + ";";
                // System.out.println(p);
            }
            return dadosSaida;

        } catch (Exception e) {
            throw new Exception("Falha ao converter o arquivo: " + xml.getAbsolutePath() + "\n" +
                    e.getMessage());
            // System.out.println(xml.getAbsolutePath() + " -> " + "Falha ao converter o
            // arquivo");
        }
    }

    public static void analisarNoXml(Node e, HashMap<String, String> produto, String prefixo) {
        if (e.getChildNodes().getLength() > 1) {
            for (int i = 0; i < e.getChildNodes().getLength(); i++) {
                if (e.getChildNodes().item(i).getNodeName().equals("ICMS")
                        || e.getChildNodes().item(i).getNodeName().equals("IPI")
                        || e.getChildNodes().item(i).getNodeName().equals("PIS")
                        || e.getChildNodes().item(i).getNodeName().equals("COFINS")) {
                    prefixo = e.getChildNodes().item(i).getNodeName() + "_";
                }
                analisarNoXml(e.getChildNodes().item(i), produto, prefixo);
            }
        } else if (e.getChildNodes().getLength() == 1) {
            if (e.getChildNodes().item(0).getChildNodes().getLength() > 1) {
                for (int i = 0; i < e.getChildNodes().item(0).getChildNodes().getLength(); i++) {
                    analisarNoXml(e.getChildNodes().item(0).getChildNodes().item(i), produto, prefixo);
                }
            } else if (e.getChildNodes().item(0).getChildNodes().getLength() == 1) {
                analisarNoXml(e.getChildNodes().item(0), produto, prefixo);
            } else {
                String nodeName = e.getNodeName();
                String nodeValue = (e.getTextContent());
                produto.put(prefixo + nodeName, nodeValue);
            }
        }
    }

    public static String converterFormato(String numeroAmericano) {
        try {
            if (numeroAmericano == null) {
                return "0,0000";
            }

            // Converte a string para double usando o ponto como separador decimal
            double numero = Double.parseDouble(numeroAmericano);

            // Formata o número para o formato brasileiro com 4 casas decimais e vírgula
            // como separador decimal
            String numeroFormatado = String.format("%.4f", numero).replace('.', ',');

            return numeroFormatado;
        } catch (Exception e) {
            System.out.println("Formato inválido. Certifique-se de que o número está no formato americano.");
            return null;
        }
    }

    public static String converterFormatoData(String data) {
        try {
            // Formato da data de entrada
            DateTimeFormatter formatoEntrada = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

            // Converter a string para OffsetDateTime
            OffsetDateTime offsetDateTime = OffsetDateTime.parse(data, formatoEntrada);

            // Formato desejado para a saída
            DateTimeFormatter formatoSaida = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            // Converter para o formato desejado
            String dataFormatada = offsetDateTime.format(formatoSaida);

            return dataFormatada;
        } catch (Exception e) {
            System.out.println("Formato de data inválido.");
            return null;
        }
    }

    public static String getChaveXML(File xml) throws Exception {
        String chaveNfe = "";

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xml);

            NodeList list = doc.getElementsByTagName("infProt");
            if (list.getLength() == 0) {
                throw new IllegalAccessException("Xml sem autorização");
            }

            if (list.getLength() > 0) {
                Node no = list.item(0);
                Element e = (Element) no;

                for (int j = 0; j < e.getChildNodes().getLength(); j++) {
                    String nodeName = e.getChildNodes().item(j).getNodeName();
                    if (nodeName.toUpperCase().equals("CHNFE")) {
                        chaveNfe = (e.getChildNodes().item(j).getTextContent());
                        break;
                    }
                }
            }
        } catch (IllegalAccessException e) {
            // throw new IllegalAccessException(e.getMessage());
            System.out.println(xml.getAbsolutePath() + " -> " + e.getMessage());
        } catch (Exception e) {
            // throw new Exception("Falha ao converter o arquivo: " + sArquivo + "\n" +
            // e.getMessage());
            System.out.println(xml.getAbsolutePath() + " -> " + "Falha ao converter o arquivo");
        }

        return chaveNfe;
    }

    public static void copiarArquivo(File file, String dirDestino) throws IOException {
        File fileDest = new File(dirDestino + file.getName());

        InputStream in = new FileInputStream(file);
        OutputStream out = new FileOutputStream(fileDest); // Transferindo bytes de entrada para saída
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    public static void localizarNotasSped(String diretorioSpeds, String fileChavesNaoLocalizadas, String diretorioSaida)
            throws FileNotFoundException, IOException {
        // ARQUIVOS SPED
        List<File> arquivos = new ArrayList<File>();
        File dir = new File(diretorioSpeds);
        Functions.listDirectoryAppend(dir, arquivos, ".TXT");
        HashMap<String, String> mapNotas = new HashMap<>();

        for (File file : arquivos) {
            System.out.println(file.getAbsolutePath());
            BufferedReader readerSped = new BufferedReader(new FileReader(file));
            String linhaSped;
            int cont = 0;
            while ((linhaSped = readerSped.readLine()) != null) {
                cont++;
                ArrayList<String> listaRegistrosLinha = Functions.quebraLinhaPorSeparador(linhaSped, "|", true, true);
                String reg = "";
                if (linhaSped.length() > 5) {
                    reg = linhaSped.substring(1, 5);
                }
                if (reg.equals("C100")) {
                    mapNotas.put(listaRegistrosLinha.get(8), file.getAbsolutePath());
                }
            }
        }

        // CHAVES NAO ENCONTRADAS
        File chaves = new File(fileChavesNaoLocalizadas);
        String encoding;
        try (FileReader fr = new FileReader(chaves)) {
            encoding = fr.getEncoding();
            fr.close();
        }
        HashMap<String, String> mapChavesLocalizadas;
        String chavesNaoLocalizadasNosArquivos = "";
        try (BufferedReader leitura = new BufferedReader(
                new InputStreamReader(new FileInputStream(chaves), encoding))) {
            String linha;
            mapChavesLocalizadas = new HashMap<>();
            while ((linha = leitura.readLine()) != null) {
                String nota = mapNotas.get(linha);
                if (nota != null) {
                    mapChavesLocalizadas.put(linha, nota);
                } else {
                    chavesNaoLocalizadasNosArquivos += linha + separador;
                }
            }
        }

        if (!chavesNaoLocalizadasNosArquivos.isEmpty()) {
            try (OutputStreamWriter bufferOut = new OutputStreamWriter(
                    new FileOutputStream(diretorioSaida + "chaves_nao_localizadas_speds.txt"), encoding)) {
                bufferOut.write(chavesNaoLocalizadasNosArquivos);
            }
        }

        System.out.println(mapChavesLocalizadas.size());

        for (File file : arquivos) {
            HashMap<String, String> mapProdutosFile = new HashMap<>();
            HashMap<String, String> mapUnidadesFile = new HashMap<>();
            HashMap<String, String> mapCliFornFile = new HashMap<>();
            HashMap<String, String> mapNotasFile = new HashMap<>();
            String primeiraLinha = "";

            try {
                String encod;
                try (FileReader fr = new FileReader(file)) {
                    encod = fr.getEncoding();
                    fr.close();
                }
                try (BufferedReader leituraFile = new BufferedReader(
                        new InputStreamReader(new FileInputStream(file), encod))) {
                    String linhaFile = "";
                    String linhaNotaFile = "", chaveNotaFile = "";
                    while ((linhaFile = leituraFile.readLine()) != null) {
                        if (primeiraLinha.equals("")) {
                            primeiraLinha = linhaFile;
                        }

                        ArrayList<String> listaRegistrosLinha = Functions.quebraLinhaPorSeparador(linhaFile, "|", true,
                                true);
                        String reg = linhaFile.substring(1, 5);

                        if (reg.equals("0150")) {
                            mapCliFornFile.put(listaRegistrosLinha.get(1), linhaFile);
                        } else if (reg.equals("0190")) {
                            mapUnidadesFile.put(listaRegistrosLinha.get(1), linhaFile);
                        } else if (reg.equals("0200")) {
                            mapProdutosFile.put(listaRegistrosLinha.get(1), linhaFile);
                        } else if (reg.equals("C001") || reg.equals("C990")) {
                            continue;
                        } else if (reg.equals("C100")) {
                            if (!linhaNotaFile.equals("")) {
                                mapNotasFile.put(chaveNotaFile, linhaNotaFile);
                            }

                            chaveNotaFile = listaRegistrosLinha.get(8);
                            linhaNotaFile = linhaFile;
                        } else if (reg.substring(0, 2).equals("C1")) {
                            linhaNotaFile += separador + linhaFile;
                        } else if (!reg.substring(0, 1).equals("C")) {
                            if (!linhaNotaFile.equals("")) {
                                mapNotasFile.put(chaveNotaFile, linhaNotaFile);
                            }
                        }

                    }
                }

            } catch (FileNotFoundException ex) {
                Logger.getLogger(Teste.class.getName()).log(Level.SEVERE, null, ex);
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(Teste.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(Teste.class.getName()).log(Level.SEVERE, null, ex);
            }

            AtomicReference<String> escreverNotas = new AtomicReference<>("");
            AtomicReference<String> escreverPodutos = new AtomicReference<>("");
            AtomicReference<String> escreverUnidades = new AtomicReference<>("");
            AtomicReference<String> escreverClientesFornecedores = new AtomicReference<>("");
            mapChavesLocalizadas.entrySet()
                    .stream()
                    .filter(map -> file.getAbsolutePath().equals(map.getValue()))
                    .forEach(map -> {
                        if (escreverNotas.get().equals("")) {
                            escreverNotas.set(mapNotasFile.get(map.getKey()));
                        } else {
                            escreverNotas.set(escreverNotas.get() + separador + mapNotasFile.get(map.getKey()));
                        }
                    });

            if (!escreverNotas.get().equals("")) {
                HashMap<String, String> mapProdutosEscrever = new HashMap<>();
                HashMap<String, String> mapUnidadesEscrever = new HashMap<>();
                HashMap<String, String> mapCliFornEscrever = new HashMap<>();
                ArrayList<String> lista = new ArrayList<>(Arrays.asList(escreverNotas.get().split(separador)));
                for (String string : lista) {
                    ArrayList<String> listaRegistrosLinha = Functions.quebraLinhaPorSeparador(string, "|", true, true);
                    String reg = string.substring(1, 5);
                    if (reg.equals("C100")) {
                        String codigo = listaRegistrosLinha.get(3);
                        String valor = mapCliFornFile.get(codigo);
                        if (valor != null) {
                            mapCliFornEscrever.put(codigo, valor);
                        }
                    } else if (reg.equals("C170")) {
                        String codigo = listaRegistrosLinha.get(2);
                        String valor = mapProdutosFile.get(codigo);
                        if (valor != null) {
                            mapProdutosEscrever.put(codigo, valor);
                        }
                    }
                }

                for (Map.Entry<String, String> entry : mapCliFornEscrever.entrySet()) {
                    if (escreverClientesFornecedores.get().equals("")) {
                        escreverClientesFornecedores.set(entry.getValue());
                    } else {
                        escreverClientesFornecedores
                                .set(escreverClientesFornecedores.get() + separador + entry.getValue());
                    }
                }

                for (Map.Entry<String, String> entry : mapProdutosEscrever.entrySet()) {
                    if (escreverPodutos.get().equals("")) {
                        escreverPodutos.set(entry.getValue());
                    } else {
                        escreverPodutos.set(escreverPodutos.get() + separador + entry.getValue());
                    }

                    ArrayList<String> listProduto = Functions.quebraLinhaPorSeparador(entry.getValue(), "|", true,
                            true);
                    String codigo = listProduto.get(5);
                    String valor = mapUnidadesFile.get(codigo);
                    if (valor != null) {
                        mapUnidadesEscrever.put(codigo, valor);
                    }
                }

                for (Map.Entry<String, String> entry : mapUnidadesEscrever.entrySet()) {
                    if (escreverUnidades.get().equals("")) {
                        escreverUnidades.set(entry.getValue());
                    } else {
                        escreverUnidades.set(escreverUnidades.get() + separador + entry.getValue());
                    }
                }

                LocalDateTime agora = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy_HH-mm-ss-SSS");
                String agoraFormatado = agora.format(formatter);

                try (OutputStreamWriter bufferOut = new OutputStreamWriter(
                        new FileOutputStream(diretorioSaida + agoraFormatado + ".txt"), encoding)) {
                    bufferOut.write(primeiraLinha + separador + escreverClientesFornecedores.get() + separador
                            + escreverUnidades.get() + separador + escreverPodutos.get() + separador
                            + escreverNotas.get() + separador);
                }
            }

        }
    }

    public static void localizarNotasXml(String diretorioXmls, String fileChavesNaoLocalizadas, String diretorioSaida)
            throws FileNotFoundException, IOException, Exception {
        // ARQUIVOS XMLS
        System.out.println("Listando arquivos XMLs");
        List<File> arquivos = new ArrayList<File>();
        File dir = new File(diretorioXmls);
        Functions.listDirectoryAppend(dir, arquivos, ".XML");
        HashMap<String, File> mapNotas = new HashMap<>();
        System.out.println("Total de arquivos XMLs: " + arquivos.size());

        System.out.println("Carregando chaves dos arquivos XMLs");
        int cont = 0;
        for (File file : arquivos) {
            cont++;
            String chaveNfe = getChaveXML(file);
            if (!chaveNfe.isEmpty()) {
                mapNotas.put(chaveNfe, file);
            }
        }

        FileOutputStream fileOutputMap = new FileOutputStream(diretorioSaida + "map.txt");
        try (ObjectOutputStream objectOutput = new ObjectOutputStream(new BufferedOutputStream(fileOutputMap))) {
            objectOutput.writeObject(mapNotas);
        }

        // CHAVES NAO ENCONTRADAS
        System.out.println("Listando chaves não localizadas");
        File chaves = new File(fileChavesNaoLocalizadas);
        String encoding;
        try (FileReader fr = new FileReader(chaves)) {
            encoding = fr.getEncoding();
            fr.close();
        }
        String chavesNaoLocalizadasNosArquivos = "";
        System.out.println("Localizando e copiando arquivos");
        try (BufferedReader leitura = new BufferedReader(
                new InputStreamReader(new FileInputStream(chaves), encoding))) {
            String linha;
            while ((linha = leitura.readLine()) != null) {
                File xml = mapNotas.get(linha);
                if (xml != null) {
                    copiarArquivo(xml, diretorioSaida);
                } else {
                    chavesNaoLocalizadasNosArquivos += linha + separador;
                }
            }
        }

        if (!chavesNaoLocalizadasNosArquivos.isEmpty()) {
            try (OutputStreamWriter bufferOut = new OutputStreamWriter(
                    new FileOutputStream(diretorioSaida + "chaves_nao_localizadas_xmls.txt"), encoding)) {
                bufferOut.write(chavesNaoLocalizadasNosArquivos);
            }

        }
        System.out.println("Finalizado");
    }

    public static void exportatCuponsSped(String diretorioSpeds, String diretorioSaida)
            throws FileNotFoundException, IOException {
        // ARQUIVOS SPED
        List<File> arquivos = new ArrayList<File>();
        File dir = new File(diretorioSpeds);
        Functions.listDirectoryAppend(dir, arquivos, ".TXT");

        for (File file : arquivos) {
            HashMap<String, String> mapProdutosFile = new HashMap<>();
            HashMap<String, String> mapUnidadesFile = new HashMap<>();
            HashMap<String, String> mapNotasFile = new HashMap<>();
            String primeiraLinha = "";

            String encoding;
            try (FileReader fr = new FileReader(file)) {
                encoding = fr.getEncoding();
                fr.close();
            }

            try {
                String encod;
                try (FileReader fr = new FileReader(file)) {
                    encod = fr.getEncoding();
                    fr.close();
                }
                try (BufferedReader leituraFile = new BufferedReader(
                        new InputStreamReader(new FileInputStream(file), encod))) {
                    String linhaFile = "";
                    String linhaNotaFile = "";
                    while ((linhaFile = leituraFile.readLine()) != null) {
                        if (primeiraLinha.equals("")) {
                            primeiraLinha = linhaFile;
                        }

                        ArrayList<String> listaRegistrosLinha = Functions.quebraLinhaPorSeparador(linhaFile, "|", true,
                                true);
                        String reg = linhaFile.substring(1, 5);

                        if (reg.equals("0190")) {
                            mapUnidadesFile.put(listaRegistrosLinha.get(1), linhaFile);
                        } else if (reg.equals("0200")) {
                            mapProdutosFile.put(listaRegistrosLinha.get(1), linhaFile);
                        } else if (reg.equals("C001") || reg.equals("C990")) {
                            continue;
                        } else if (reg.equals("C400")) {
                            if (!linhaNotaFile.equals("")) {
                                mapNotasFile.put(linhaNotaFile, linhaNotaFile);
                            }

                            linhaNotaFile = linhaFile;
                        } else if (reg.substring(0, 2).equals("C4")) {
                            linhaNotaFile += separador + linhaFile;
                        } else if (!reg.substring(0, 1).equals("C")) {
                            if (!linhaNotaFile.equals("")) {
                                mapNotasFile.put(linhaNotaFile, linhaNotaFile);
                            }
                        }

                    }
                }

            } catch (FileNotFoundException ex) {
                Logger.getLogger(Teste.class.getName()).log(Level.SEVERE, null, ex);
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(Teste.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(Teste.class.getName()).log(Level.SEVERE, null, ex);
            }

            AtomicReference<String> escreverNotas = new AtomicReference<>("");
            AtomicReference<String> escreverPodutos = new AtomicReference<>("");
            AtomicReference<String> escreverUnidades = new AtomicReference<>("");

            for (String n : mapNotasFile.values()) {
                if (escreverNotas.get().equals("")) {
                    escreverNotas.set(n);
                } else {
                    escreverNotas.set(escreverNotas.get() + separador + n);
                }
            }

            if (!escreverNotas.get().equals("")) {
                HashMap<String, String> mapProdutosEscrever = new HashMap<>();
                HashMap<String, String> mapUnidadesEscrever = new HashMap<>();
                ArrayList<String> lista = new ArrayList<>(Arrays.asList(escreverNotas.get().split(separador)));
                for (String string : lista) {
                    ArrayList<String> listaRegistrosLinha = Functions.quebraLinhaPorSeparador(string, "|", true, true);
                    String reg = string.substring(1, 5);
                    if (reg.equals("C470")) {
                        String codigo = listaRegistrosLinha.get(1);
                        String valor = mapProdutosFile.get(codigo);
                        if (valor != null) {
                            mapProdutosEscrever.put(codigo, valor);
                        }
                    }
                }

                for (Map.Entry<String, String> entry : mapProdutosEscrever.entrySet()) {
                    if (escreverPodutos.get().equals("")) {
                        escreverPodutos.set(entry.getValue());
                    } else {
                        escreverPodutos.set(escreverPodutos.get() + separador + entry.getValue());
                    }

                    ArrayList<String> listProduto = Functions.quebraLinhaPorSeparador(entry.getValue(), "|", true,
                            true);
                    String codigo = listProduto.get(5);
                    String valor = mapUnidadesFile.get(codigo);
                    if (valor != null) {
                        mapUnidadesEscrever.put(codigo, valor);
                    }
                }

                for (Map.Entry<String, String> entry : mapUnidadesEscrever.entrySet()) {
                    if (escreverUnidades.get().equals("")) {
                        escreverUnidades.set(entry.getValue());
                    } else {
                        escreverUnidades.set(escreverUnidades.get() + separador + entry.getValue());
                    }
                }

                LocalDateTime agora = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy_HH-mm-ss-SSS");
                String agoraFormatado = agora.format(formatter);

                try (OutputStreamWriter bufferOut = new OutputStreamWriter(
                        new FileOutputStream(diretorioSaida + "CF_" + agoraFormatado + ".txt"), encoding)) {
                    bufferOut.write(primeiraLinha + separador + escreverUnidades.get() + separador
                            + escreverPodutos.get() + separador + escreverNotas.get()
                            + separador);
                }
            }

        }
    }

    public static void separarESalvarArquivosXML(String diretorio, String diretorioSaida, int quantidade) {
        File dir = new File(diretorio);

        if (dir.exists() && dir.isDirectory()) {
            processarDiretorio(dir, diretorioSaida, quantidade);
            System.out.println("Arquivos XML separados e salvos em pastas.");
        } else {
            System.out.println("O caminho especificado não é um diretório válido.");
        }
    }

    public static void processarDiretorio(File diretorio, String diretorioSaida, int quantidade) {
        File[] arquivos = diretorio.listFiles();

        if (arquivos != null) {
            int batchSize = quantidade; // Número de arquivos por lote
            int numBatches = (int) Math.ceil((double) arquivos.length / batchSize);

            for (int i = 0; i < numBatches; i++) {
                int startIndex = i * batchSize;
                int endIndex = Math.min(startIndex + batchSize, arquivos.length);
                File[] batch = Arrays.copyOfRange(arquivos, startIndex, endIndex);

                // Crie uma pasta para o lote atual
                File pastaLote = new File(diretorioSaida, "lote_" + i);
                pastaLote.mkdir();

                // Mova os arquivos XML para a pasta do lote
                for (File arquivo : batch) {
                    if (arquivo.getName().toLowerCase().endsWith(".xml")) {
                        Path origem = arquivo.toPath();
                        Path destino = pastaLote.toPath().resolve(arquivo.getName());
                        try {
                            Files.move(origem, destino, StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                System.out.println("Lote " + i + ": " + batch.length + " arquivos XML movidos para "
                        + pastaLote.getAbsolutePath());
            }
        }

        // Recursivamente processar subdiretórios
        File[] subdiretorios = diretorio.listFiles(file -> file.isDirectory());
        if (subdiretorios != null) {
            for (File subdiretorio : subdiretorios) {
                processarDiretorio(subdiretorio, diretorioSaida, quantidade);
            }
        }
    }

    public static void recalcularPisCofinsSpedC500(String diretorioSpeds, String diretorioSaida)
            throws FileNotFoundException, IOException {

        // ARQUIVOS SPED
        List<File> arquivos = new ArrayList<File>();
        File dir = new File(diretorioSpeds);
        Functions.listDirectoryAppend(dir, arquivos, ".TXT");

        for (File file : arquivos) {
            System.out
                    .println(
                            "Iniciando recalculo dos registros C500, C501 e C505 do arquivo " + file.getName() + "...");
            String saida = "";

            String encoding;
            try (FileReader fr = new FileReader(file)) {
                encoding = fr.getEncoding();
                fr.close();
            }

            try {
                String encod;
                try (FileReader fr = new FileReader(file)) {
                    encod = fr.getEncoding();
                    fr.close();
                }
                try (BufferedReader leituraFile = new BufferedReader(
                        new InputStreamReader(new FileInputStream(file), encod))) {
                    String linhaFile = "";
                    while ((linhaFile = leituraFile.readLine()) != null) {
                        ArrayList<String> listaRegistrosLinha = Functions.quebraLinhaPorSeparador(linhaFile, "|", true,
                                true);
                        String reg = linhaFile.substring(1, 5);

                        if (reg.equals("C500")) {
                            String valorBase = listaRegistrosLinha.get(9);
                            double vlBase = Double.parseDouble(valorBase.replace(".", "").replace(",", "."));
                            double vlPis = vlBase * (1.65 / 100);
                            double vlCofins = vlBase * (7.6 / 100);

                            listaRegistrosLinha.set(12, Functions.formatNumber("###0.00", vlPis));
                            listaRegistrosLinha.set(13, Functions.formatNumber("###0.00", vlCofins));

                            String linhaFormatada = Functions.escreveListaEmStringComSeparador(listaRegistrosLinha, "|",
                                    true, true);
                            saida += linhaFormatada + separador;
                        } else if (reg.equals("C501") || reg.equals("C505")) {
                            String valorBase = listaRegistrosLinha.get(2);
                            listaRegistrosLinha.set(4, valorBase);

                            String aliquota = listaRegistrosLinha.get(5);

                            double vlBase = Double.parseDouble(valorBase.replace(".", "").replace(",", "."));
                            double vlAliquota = Double.parseDouble(aliquota.replace(".", "").replace(",", ".")) / 100;

                            double vlImposto = vlBase * vlAliquota;
                            listaRegistrosLinha.set(6, Functions.formatNumber("###0.00", vlImposto));

                            String linhaFormatada = Functions.escreveListaEmStringComSeparador(listaRegistrosLinha, "|",
                                    true, true);
                            saida += linhaFormatada + separador;
                        } else {
                            saida += linhaFile + separador;
                        }

                    }
                }

                try (OutputStreamWriter bufferOut = new OutputStreamWriter(
                        new FileOutputStream(diretorioSaida + "CORRIGIDO_" + file.getName()),
                        encoding)) {
                    bufferOut.write(saida);
                }
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Teste.class.getName()).log(Level.SEVERE, null, ex);
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(Teste.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(Teste.class.getName()).log(Level.SEVERE, null, ex);
            }

            System.out
                    .println("Recalculo dos registros C500, C501 e C505 do arquivo " + file.getName() + " finalizado!");
            System.out.println("_________________________________________________________");
            System.out.println();

        }
    }

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

    public static String sanitizeForApi(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        // Remove quebras de linha (\n e \r)
        input = input.replaceAll("[\\n\\r]", " ");

        // Usa JsonStringEncoder para escapar corretamente caracteres JSON
        JsonStringEncoder encoder = JsonStringEncoder.getInstance();
        String escapedString = new String(encoder.quoteAsString(input));

        return escapedString;
    }

    public static void lerExcel() throws Exception {
        Properties config = new Properties();
        File file = new File(CONFIG_FILE);

        // Carrega configurações anteriores, se existirem
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                config.load(fis);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Obtém valores anteriores ou usa valores padrão
        String servidor = JOptionPane.showInputDialog("Informe o servidor:", config.getProperty("servidor", ""));
        String appId = JOptionPane.showInputDialog("Informe o AppID:", config.getProperty("appid", ""));
        String caminhoArquivo = JOptionPane.showInputDialog("Informe o caminho do arquivo:",
                config.getProperty("caminho_arquivo", ""));

        // Salva as configurações no arquivo
        config.setProperty("servidor", servidor);
        config.setProperty("appid", appId);
        config.setProperty("caminho_arquivo", caminhoArquivo);

        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            config.store(fos, "Configurações do Aplicativo");
        } catch (IOException e) {
            e.printStackTrace();
        }

        long inicio = System.currentTimeMillis();

        Boolean enviarPorLote = true;
        int contJson = 0;

        try (InputStream inp = new FileInputStream(caminhoArquivo);
                XSSFWorkbook workbook = new XSSFWorkbook(inp);) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.rowIterator();
            rowIterator.next(); // Pular cabeçalho

            String conta = "";
            String saldoAnterior = "";
            StringBuilder jsonLinhas = new StringBuilder("");
            ArrayList<StringBuilder> jsonLancamentos = new ArrayList<>();

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                String[] cols = new String[11];
                for (int i = 0; i < 11; i++) {
                    String retCol = returnValueCell(row.getCell(i));
                    if (retCol == null) {
                        cols[i] = "";
                    } else {
                        cols[i] = retCol.trim();
                    }
                }

                if (cols[0].toUpperCase().contains("CONTA -")) {
                    conta = cols[0].split("-", 2)[1].trim();
                }
                if (cols[1].toUpperCase().contains("SALDO ANTERIOR")) {
                    saldoAnterior = cols[1].split(":", 2)[1].trim();
                }

                if (Functions.isDate(cols[0]) && !cols[2].toUpperCase().contains("CONTA SEM MOVIMENTO NO PERIODO")
                        && !cols[2].toUpperCase().contains("APURACAO DE RESULTADO")) {
                    contJson++;
                    String historico = sanitizeForApi(cols[2]);
                    if (!jsonLinhas.isEmpty()) {
                        jsonLinhas.append(",");
                    }
                    jsonLinhas.append(String.format(
                            "{\"conta\": \"%s\", \"saldoAnterior\": \"%s\", \"data\": \"%s\", \"lote\": \"%s\", " +
                                    "\"historico\": \"%s\", \"cPartida\": \"%s\", \"filialOrigem\": \"%s\", \"cCusto\": \"%s\", "
                                    +
                                    "\"itemConta\": \"%s\", \"codClVal\": \"%s\", \"debito\": \"%s\", \"credito\": \"%s\", \"saldoAtual\": \"%s\"}",
                            conta, saldoAnterior, cols[0], cols[1], historico, cols[3], cols[4], cols[5], cols[6],
                            cols[7], cols[8], cols[9], cols[10]));

                    if ((contJson == 10000) && enviarPorLote) {
                        jsonLancamentos.add(jsonLinhas);
                        jsonLinhas = new StringBuilder("");
                        contJson = 0;
                    }
                }
            }

            if (!jsonLinhas.isEmpty()) {
                jsonLancamentos.add(jsonLinhas);
            }

            int contLotes = 0;
            int totalLotes = jsonLancamentos.size();
            for (StringBuilder loteLancamentos : jsonLancamentos) {
                contLotes++;
                StringBuilder json = new StringBuilder("{\"razaoProtheus\": [");
                json.append(loteLancamentos);
                json.append("]}");

                System.out.println("Enviando Lote " + contLotes + " de " + totalLotes + "...");

                try {
                    JSONParser parser = new JSONParser();
                    ApiAppSeven api = new ApiAppSeven(appId, servidor);
                    JSONObject jsonRetornoEnvio = api.sendRazaoTecnoGera(json.toString());
                    Object objRetornoEnvio = jsonRetornoEnvio.get("retorno");
                    JSONObject jsonRetornoEnvioRetorno = (JSONObject) parser.parse(new Gson().toJson(objRetornoEnvio));
                    System.out.println(
                            "Retorno Lote " + contLotes + " de " + totalLotes + ": "
                                    + jsonRetornoEnvioRetorno.get("mensagem").toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // // Escrevendo no arquivo
                // try (BufferedWriter writer = new BufferedWriter(new
                // FileWriter("D:/temp/razaoProtheus.json"))) {
                // writer.write(json.toString());
                // } catch (IOException e) {
                // System.out.println("Ocorreu um erro ao escrever no arquivo: " +
                // e.getMessage());
                // }

                // System.exit(0);

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        long tempoExecucao = System.currentTimeMillis()
                - inicio;
        System.out.printf("Tempo de execução: %d min %d seg %d ms%n", (tempoExecucao / 1000) / 60,
                (tempoExecucao / 1000) % 60, tempoExecucao % 1000);
    }

    public static void main(String[] args) {
        try {
            // String cabeçalho = "Número Nota; Modelo; Série; Chave; Data Emissão; CNPJ
            // Emit; RZ Emit; Cód Cidade Emit; Cidade Emit; UF Emit; CNPJ Dest; RZ Dest; Cód
            // Cidade Dest; Cidade Dest; UF Dest;"
            // + "Cód Prod; Desc Prod; Unidade Prod; EAN Prod; NCM Prod; CFOP; Vl Unit;
            // Qtd;Vl Total; CST ICMS; Base ICMS; Aliq ICMS; Vl ICMS; CST PIS; Base PIS;
            // AliqPIS; Vl PIS; "
            // + "CST COFINS; Base COFINS; Aliq COFINS; Vl COFINS; CST IPI; Base IPI; Aliq
            // IPI; Vl IPI;";
            // List<File> arquivos = new ArrayList<File>();
            // File dir = new File("D:\\temp\\permanente\\02\\XMLS\\lote_13\\");
            // Functions.listDirectoryAppend(dir, arquivos, ".XML");

            // try (BufferedWriter writer = new BufferedWriter(new
            // FileWriter("D:\\temp\\permanente\\saida_02_14.txt"))) {
            // writer.write(cabeçalho + separador);
            // for (File file : arquivos) {
            // writer.write(getDadosXML(file));
            // }

            // } catch (IOException e) {
            // System.out.println("Ocorreu um erro ao escrever no arquivo: " +
            // e.getMessage());
            // }

            // File xml = new File(
            // "C:/Users/dassa/Downloads/27190505230009003551550020000660271001514622.XML");
            // getDadosXML(xml);

            // localizarNotasSped("E:\\CLIENTES\\KIBARATO\\SPEDS\\SPEDS-BX\\",
            // "E:\\CLIENTES\\KIBARATO\\NOVA_MALHA_2\\chaves.txt",
            // "E:\\CLIENTES\\KIBARATO\\NOVA_MALHA_2\\SAIDA\\");

            // exportatCuponsSped("D:\\temp\\teste\\SPEDS",
            // "D:\\temp\\teste\\SAIDA\\");
            // separarESalvarArquivosXML("D:\\temp\\permanente\\02\\XML_999\\XML",
            // "D:\\temp\\permanente\\02\\XMLS", 5000);

            // String diretorioSped = JOptionPane.showInputDialog("Diretório Sped");
            // String diretorioSaida = JOptionPane.showInputDialog("Diretório Saída");
            // if ((diretorioSped != null) && (diretorioSaida != null)) {
            // if ((!diretorioSaida.endsWith("\\")) && (!diretorioSaida.endsWith("/"))) {
            // diretorioSaida += "\\";
            // }

            // recalcularPisCofinsSpedC500(diretorioSped, diretorioSaida);
            // }

            lerExcel();
        } catch (Exception ex) {
            Logger.getLogger(Teste.class.getName()).log(Level.SEVERE, null, ex);
        }

        /*
         * try {
         * localizarNotasXml("D:\\CLIENTES\\KIBARATO\\XMLS",
         * "D:\\CLIENTES\\KIBARATO\\chaves_nao_localizadas_nos_arquivos.txt",
         * "D:\\CLIENTES\\KIBARATO\\XMLS_LOCALIZADOS\\");
         * } catch (IOException ex) {
         * Logger.getLogger(Teste.class.getName()).log(Level.SEVERE, null, ex);
         * } catch (Exception ex) {
         * Logger.getLogger(Teste.class.getName()).log(Level.SEVERE, null, ex);
         * }
         */

        // TesteClass testeClass1 = TesteService.getTesteClass("Teste 1");
        // System.out.println("1- " + testeClass1.getNome());
        // TesteClass testeClass2 = TesteService.getTesteClass("Teste 2");
        // System.out.println("2- " + testeClass2.getNome());
        // System.out.println("1- " + testeClass1.getNome());
    }
}
