package br.com.dbs.teste;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class SeparacaoArquivos {

  public static void copiarXMLsPorChaveConteudo(Path pastaOrigem, Path pastaDestino, List<String> chaves)
      throws IOException {
    if (!Files.exists(pastaDestino)) {
      Files.createDirectories(pastaDestino);
    }

    Set<String> chavesSet = new HashSet<>(chaves);

    try (Stream<Path> arquivos = Files.walk(pastaOrigem)) {
      arquivos
          .parallel()
          .filter(Files::isRegularFile)
          .filter(p -> p.toString().toLowerCase().endsWith(".xml"))
          .forEach(arquivo -> {
            try {
              Optional<String> chaveEncontrada = encontrarChaveNoXML(arquivo, chavesSet);
              if (chaveEncontrada.isPresent()) {
                Path destino = pastaDestino.resolve(arquivo.getFileName());
                Files.copy(arquivo, destino, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Copiado: " + arquivo);
              }
            } catch (Exception e) {
              System.err.println("Erro ao processar " + arquivo + ": " + e.getMessage());
            }
          });
    }
  }

  public static List<String> listarChavesNaoEncontradas(Path caminhoChaves, Path pastaXMLs) throws IOException {
    List<String> chaves = Files.readAllLines(caminhoChaves);
    Set<String> chavesSet = new HashSet<>(chaves);
    Set<String> chavesEncontradas = Collections.synchronizedSet(new HashSet<>());

    try (Stream<Path> arquivos = Files.walk(pastaXMLs)) {
      arquivos
          .parallel()
          .filter(Files::isRegularFile)
          .filter(p -> p.toString().toLowerCase().endsWith(".xml"))
          .forEach(arquivo -> {
            try {
              Optional<String> chave = encontrarChaveNoXML(arquivo, chavesSet);
              chave.ifPresent(chavesEncontradas::add);
            } catch (Exception e) {
              System.err.println("Erro ao processar " + arquivo + ": " + e.getMessage());
            }
          });
    }

    // Retorna as chaves que não foram encontradas
    List<String> chavesNaoEncontradas = new ArrayList<>();
    for (String chave : chavesSet) {
      if (!chavesEncontradas.contains(chave)) {
        chavesNaoEncontradas.add(chave);
      }
    }

    return chavesNaoEncontradas;
  }

  private static Optional<String> encontrarChaveNoXML(Path arquivo, Set<String> chavesSet) throws Exception {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    SAXParser saxParser = factory.newSAXParser();

    ChaveHandler handler = new ChaveHandler(chavesSet);
    try (InputStream is = Files.newInputStream(arquivo)) {
      saxParser.parse(is, handler);
    }

    return handler.getChaveEncontrada();
  }

  public static Set<String> exportarChaves(Path pastaXMLs) throws IOException {
    Map<String, Integer> contadorChaves = new HashMap<>();
    Set<String> chavesEncontradas = Collections.synchronizedSet(new HashSet<>());

    try (Stream<Path> arquivos = Files.walk(pastaXMLs)) {
      arquivos
          .parallel()
          .filter(Files::isRegularFile)
          .filter(p -> p.toString().toLowerCase().endsWith(".xml"))
          .forEach(arquivo -> {
            try {
              Optional<String> chaveOpt = extrairChaveDoXML(arquivo);
              chavesEncontradas.add(chaveOpt.orElse(""));
            } catch (Exception e) {
              System.err.println("Erro ao processar " + arquivo + ": " + e.getMessage());
            }
          });
    }

    return chavesEncontradas;
  }

  public static Map<String, Integer> encontrarChavesDuplicadas(Path pastaXMLs) throws IOException {
    Map<String, Integer> contadorChaves = new HashMap<>();

    try (Stream<Path> arquivos = Files.walk(pastaXMLs)) {
      arquivos
          .parallel()
          .filter(Files::isRegularFile)
          .filter(p -> p.toString().toLowerCase().endsWith(".xml"))
          .forEach(arquivo -> {
            try {
              Optional<String> chaveOpt = extrairChaveDoXML(arquivo);
              chaveOpt.ifPresent(chave -> {
                synchronized (contadorChaves) {
                  contadorChaves.put(chave, contadorChaves.getOrDefault(chave, 0) + 1);
                }
              });
            } catch (Exception e) {
              System.err.println("Erro ao processar " + arquivo + ": " + e.getMessage());
            }
          });
    }

    // Filtra e retorna somente chaves com mais de uma ocorrência
    Map<String, Integer> duplicadas = new HashMap<>();
    for (Map.Entry<String, Integer> entry : contadorChaves.entrySet()) {
      if (entry.getValue() > 1) {
        duplicadas.put(entry.getKey(), entry.getValue());
      }
    }

    return duplicadas;
  }

  private static Optional<String> extrairChaveDoXML(Path arquivo) throws Exception {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    SAXParser saxParser = factory.newSAXParser();

    ExtratorChaveHandler handler = new ExtratorChaveHandler();
    try (InputStream is = Files.newInputStream(arquivo)) {
      saxParser.parse(is, handler);
    }

    return handler.getChave();
  }

  // Handler SAX que procura a chave
  static class ChaveHandler extends DefaultHandler {
    private final Set<String> chavesProcuradas;
    private Optional<String> chaveEncontrada = Optional.empty();

    public ChaveHandler(Set<String> chavesProcuradas) {
      this.chavesProcuradas = chavesProcuradas;
    }

    public Optional<String> getChaveEncontrada() {
      return chaveEncontrada;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
      if ("infNFe".equalsIgnoreCase(qName)) {
        String id = attributes.getValue("Id");

        if (id != null && id.startsWith("NFe")) {
          String chave = id.substring(3); // remove "NFe"
          if (chavesProcuradas.contains(chave)) {
            chaveEncontrada = Optional.of(chave);
          }
        }
      }
    }
  }

  static class ExtratorChaveHandler extends DefaultHandler {
    private Optional<String> chave = Optional.empty();

    public Optional<String> getChave() {
      return chave;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
      if (chave.isPresent())
        return;

      if ("infNFe".equalsIgnoreCase(qName)) {
        String id = attributes.getValue("Id");
        if (id != null && id.startsWith("NFe")) {
          chave = Optional.of(id.substring(3)); // Remove o prefixo "NFe"
        }
      }
    }
  }

  public static void main(String[] args) throws IOException {
    // Path pastaOrigem = Paths
    // .get("\\\\192.168.15.230\\Publica\\CLIENTES\\BOTICARIO\\Arquivos\\ARQUIVOS
    // ANTIGOS\\ANALISE-FISCALIZACAO");
    // Path pastaDestino = Paths.get("D:/temp/XMLsFiltrados");
    // List<String> chaves = Files.readAllLines(Paths.get("D:/temp/chaves.txt"));
    // copiarXMLsPorChaveConteudo(pastaOrigem, pastaDestino, chaves);

    // List<String> chavesNaoEncontradas =
    // listarChavesNaoEncontradas(Paths.get("D:/temp/chaves.txt"),
    // Paths.get("D:\\temp\\XMLsFiltrados"));

    Map<String, Integer> duplicadas = encontrarChavesDuplicadas(Paths.get("D:\\temp\\XMLsFiltrados"));
    // Grava em arquivo
    List<String> linhas = new ArrayList<>();
    duplicadas.forEach((chave, count) -> linhas.add(chave + " - " + count));

    // Set<String> chavesEncontradas =
    // exportarChaves(Paths.get("D:\\temp\\XMLsFiltrados"));

    Files.write(Paths.get("D:/temp/chaves_duplicadas.txt"), linhas);

  }

}
