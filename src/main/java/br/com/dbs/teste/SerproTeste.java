package br.com.dbs.teste;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class SerproTeste {

  static class Socio {
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

  static class DebitoSief {
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

  static class DebitoSuspenso {
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

  static class PendenciaSida {
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

  public static void main(String[] args) {
    try {
      File file = new File("saida.pdf");
      PDDocument document = PDDocument.load(file);

      PDFTextStripper stripper = new PDFTextStripper();
      String text = stripper.getText(document);
      document.close();

      System.out.println(text);

      // --- Sócios ---
      List<Socio> socios = extrairSocios(text);

      // --- Débito SIEF ---
      List<DebitoSief> debitos = extrairDebitoSief(text);

      // --- Débito Suspenso ---
      List<DebitoSuspenso> suspensos = extrairDebitoSuspenso(text);

      // --- Pendência SIDA ---
      List<PendenciaSida> insc = extrairPendenciaSida(text);

      // Debug: imprime resultados
      System.out.println("=== Sócios ===");
      socios.forEach(System.out::println);

      System.out.println("\n=== Débitos (SIEF) ===");
      debitos.forEach(System.out::println);

      System.out.println("\n=== Débitos Suspensos (SIEF) ===");
      suspensos.forEach(System.out::println);

      System.out.println("\n=== Pendência Inscrição (SIDA) ===");
      insc.forEach(System.out::println);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static List<Socio> extrairSocios(String text) {
    List<Socio> lista = new ArrayList<>();
    Pattern p = Pattern.compile("Sócios e Administradores[\\s\\S]*?Pendência", Pattern.MULTILINE);
    Matcher m = p.matcher(text);
    if (m.find()) {
      String bloco = m.group();
      String[] linhas = bloco.split("\n");
      for (String linha : linhas) {
        if (linha.matches(".*\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}.*")) {
          String[] parts = linha.trim().split("\\s{2,}");
          if (parts.length >= 5) {
            Socio s = new Socio();
            s.cpfCnpj = parts[0];
            s.nome = parts[1];
            s.qualificacao = parts[2];
            s.situacao = parts[3];
            s.capital = parts[4];
            lista.add(s);
          }
        }
      }
    }
    return lista;
  }

  private static List<DebitoSief> extrairDebitoSief(String text) {
    List<DebitoSief> lista = new ArrayList<>();
    Pattern p = Pattern.compile("Pendência - Débito \\(SIEF\\)[\\s\\S]*?Débito com Exigibilidade Suspensa",
        Pattern.MULTILINE);
    Matcher m = p.matcher(text);
    if (m.find()) {
      String bloco = m.group();
      String[] linhas = bloco.split("\n");
      for (String linha : linhas) {
        if (linha.matches("^\\d+.*")) { // começa com código Receita
          String[] parts = linha.trim().split("\\s{2,}");
          if (parts.length >= 5) {
            DebitoSief d = new DebitoSief();
            d.receita = parts[0];
            d.pa = parts[1];
            d.vencimento = parts[2];
            d.valor = parts[3];
            d.situacao = parts[parts.length - 1];
            lista.add(d);
          }
        }
      }
    }
    return lista;
  }

  private static List<DebitoSuspenso> extrairDebitoSuspenso(String text) {
    List<DebitoSuspenso> lista = new ArrayList<>();
    Pattern p = Pattern.compile("Débito com Exigibilidade Suspensa \\(SIEF\\)[\\s\\S]*?Pendência - Inscrição",
        Pattern.MULTILINE);
    Matcher m = p.matcher(text);
    if (m.find()) {
      String bloco = m.group();
      String[] linhas = bloco.split("\n");
      for (String linha : linhas) {
        if (linha.matches("^\\d+.*")) {
          String[] parts = linha.trim().split("\\s{2,}");
          if (parts.length >= 5) {
            DebitoSuspenso d = new DebitoSuspenso();
            d.receita = parts[0];
            d.pa = parts[1];
            d.vencimento = parts[2];
            d.valor = parts[3];
            d.situacao = parts[parts.length - 1];
            lista.add(d);
          }
        }
      }
    }
    return lista;
  }

  private static List<PendenciaSida> extrairPendenciaSida(String text) {
    List<PendenciaSida> lista = new ArrayList<>();
    Pattern p = Pattern.compile("Pendência - Inscrição \\(SIDA\\)[\\s\\S]*?Final do Relatório", Pattern.MULTILINE);
    Matcher m = p.matcher(text);
    if (m.find()) {
      String bloco = m.group();
      String[] linhas = bloco.split("\n");
      for (String linha : linhas) {
        if (linha.matches("^\\d{11}.*")) { // inscrição inicia com 11 dígitos
          String[] parts = linha.trim().split("\\s{2,}");
          if (parts.length >= 6) {
            PendenciaSida s = new PendenciaSida();
            s.inscricao = parts[0];
            s.receita = parts[1];
            s.vencimento = parts[2];
            s.processo = parts[3];
            s.devedor = parts[4];
            s.situacao = parts[5];
            lista.add(s);
          }
        }
      }
    }
    return lista;
  }

}
