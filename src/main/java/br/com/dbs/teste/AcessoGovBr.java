package br.com.dbs.teste;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class AcessoGovBr {

  public static void clearDirectory(String caminhoPasta) {
    File pasta = new File(caminhoPasta);

    // Verifica se o caminho corresponde a uma pasta
    if (!pasta.isDirectory()) {
      System.err.println("O caminho especificado não corresponde a uma pasta.");
      return;
    }

    // Obtém a lista de arquivos e pastas dentro da pasta
    File[] arquivos = pasta.listFiles();

    if (arquivos != null) {
      for (File arquivo : arquivos) {
        if (arquivo.isDirectory()) {
          // Se for uma pasta, chama recursivamente a função para apagar seu conteúdo
          clearDirectory(arquivo.getAbsolutePath());
        }
        // Deleta o arquivo ou pasta
        arquivo.delete();
      }
    }
  }

  public static boolean verificarConexao() {
    try {
      InetAddress.getByName("www.google.com").isReachable(5000); // Verifica se o host do Google é alcançável
                                                                 // dentro de 5 segundos
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  private static String waitForFileDownload(String directory, String extension) throws InterruptedException {
    boolean sair = false;
    int cont = 0;
    String arquivo = "";
    while (!sair) {
      cont++;
      File dir = new File(directory);
      File[] files = dir.listFiles((dir1, name) -> name.endsWith(extension));
      if (files != null && files.length > 0) {
        sair = files[0].length() > 0;
        arquivo = files[0].getAbsolutePath();
      }
      Thread.sleep(1000);

      if (cont == 10) {
        if (verificarConexao()) {
          cont = 0;
        } else {
          throw new InterruptedException("Falha na conexão com internet!");
        }
      }
    }
    return arquivo;
  }

  public static void main(String[] args) {
    System.out.println("início " + new Date());

    /*
     * 1 - CPF e SENHA
     * 2 - CPF/CNPJ, CÓDIGO DE ACESSO E SENHA
     */
    int tipoAcessoEcac = 2;

    boolean carregarPendenciasEcac = true;
    boolean carregarFaturamentoEcac = true;
    boolean carregarCertidaoNegativa = true;

    String CNPJ = "45135775000102";
    String SENHA_ECAC = "Dcpq1518";
    String CODIGO_ACESSO_ECAC = "698864674893";

    String dir_download = (new File("")).getAbsolutePath() + "\\temp";
    dir_download = "D:\\temp\\ecac";

    // Configurando as opções do navegador
    FirefoxOptions options = new FirefoxOptions();
    // options.addArguments("--headless");
    options.addPreference("browser.download.folderList", 2);
    options.addPreference("browser.download.dir", dir_download);
    options.addPreference("browser.helperApps.neverAsk.saveToDisk",
        "image/png, text/html, image/tiff, text/csv, application/zip, application/octet-stream, application/pkcs12, application/vnd.mspowerpoint, application/xhtml+xml, application/xml, application/pdf");
    options.addPreference(
        "browser.download.viewableInternally.previousHandler.alwaysAskBeforeHandling.pdf", false);
    options.addPreference(
        "browser.download.viewableInternally.previousHandler.preferredAction.pdf", 0);
    options.addPreference(
        "browser.download.viewableInternally.typeWasRegistered.pdf", true);
    options.addPreference("browser.download.manager.showWhenStarting", false);
    options.setBinary("C:\\Program Files\\Mozilla Firefox\\firefox.exe");

    // Inicializando o serviço do driver do Firefox
    System.setProperty("webdriver.gecko.driver",
        "geckodriver.exe");
    System.setProperty(FirefoxDriver.SystemProperty.BROWSER_LOGFILE, "null");

    clearDirectory(dir_download);

    // Inicializando o driver do Firefox
    WebDriver driver = new FirefoxDriver(options);

    try {
      System.out.println("Acessando ECAC para captura de Dados...");
      System.out.println("");
      System.out.println("");

      // Abrindo a página do ECAC
      driver.get("https://cav.receita.fazenda.gov.br/autenticacao/Login");

      if (tipoAcessoEcac == 1) {
        /******** CPF E SENHA ********/
        // Inserindo usuário
        WebElement cpfInput = waitForClickableElement(driver,
            By.xpath("//*[@id=\"accountId\"]"));
        cpfInput.click();
        cpfInput.clear();
        cpfInput.sendKeys("04234376400");

        WebElement btnContinuarCPF = waitForClickableElement(driver,
            By.xpath("//*[@id=\"login-button-panel\"]/button"));
        btnContinuarCPF.click();

        // Inserindo senha
        WebElement senhaCpfInput = waitForClickableElement(driver,
            By.xpath("//*[@id=\"password\"]"));
        senhaCpfInput.click();
        senhaCpfInput.clear();
        senhaCpfInput.sendKeys("Mc35796#");

        // Acessando sistema
        WebElement btnAcessar = waitForClickableElement(driver,
            By.xpath("//*[@id=\"submit-button\"]"));
        btnAcessar.click();
      } else if (tipoAcessoEcac == 2) {
        /******** CPF/CNPJ, CODIGO DE ACESSO E SENHA ********/
        // Inserindo CPF/CNPJ
        WebElement cpfCnpjInput = waitForClickableElement(driver, By.xpath("//*[@id=\"NI\"]"));
        cpfCnpjInput.click();
        cpfCnpjInput.clear();
        cpfCnpjInput.sendKeys(CNPJ);

        // Inserindo CODIGO DE ACESSO
        WebElement codigoAcessoInput = waitForClickableElement(driver, By.xpath("//*[@id=\"CodigoAcesso\"]"));
        codigoAcessoInput.click();
        codigoAcessoInput.clear();
        codigoAcessoInput.sendKeys(CODIGO_ACESSO_ECAC);

        // Inserindo SENHA
        WebElement senhaInput = waitForClickableElement(driver, By.xpath("//*[@id=\"Senha\"]"));
        senhaInput.click();
        senhaInput.clear();
        senhaInput.sendKeys(SENHA_ECAC);

        // Acessando sistema
        WebElement btnAcessar = waitForClickableElement(driver,
            By.xpath("//*[@id=\"login-dados-usuario\"]/p[4]/input"));
        btnAcessar.click();
      } else if (tipoAcessoEcac == 3) {
        Thread.sleep(60000);
      }

      if (carregarPendenciasEcac) {
        /******** PENDENCIAS ********/
        WebElement btnCertidoesSituacaoFiscal = waitForClickableElement(driver,
            By.xpath("/html/body/div[3]/div[2]/ul/li[2]/a"));
        btnCertidoesSituacaoFiscal.click();

        WebElement btnConsultarPendenciasSituacaoFiscal = waitForClickableElement(driver,
            By.xpath("/html/body/div[3]/div[2]/div[3]/div[2]/div[2]/div/ul/li[2]/a"));
        btnConsultarPendenciasSituacaoFiscal.click();

        WebElement frameRelatorio = waitForClickableElement(driver, By.xpath("// *[@id=\"frmApp\"]"));
        driver.switchTo().frame(frameRelatorio);

        WebElement btnConsultarPendenciasSituacaoFiscalGerarRelatorio = waitForClickableElement(driver,
            By.xpath("/html/body/form/div[2]/table/tbody/tr/td[1]/div/div[4]/a"));
        btnConsultarPendenciasSituacaoFiscalGerarRelatorio.click();

        Thread.sleep(5000);
        frameRelatorio = waitForClickableElement(driver, By.xpath("//*[@id=\"palco\"]"));
        driver.switchTo().frame(frameRelatorio);

        WebElement btnConsultarPendenciasSituacaoFiscalGerarRelatorio2 = waitForClickableElement(driver,
            By.xpath("/html/body/div/table/tbody/tr[8]/td/div/input"));
        btnConsultarPendenciasSituacaoFiscalGerarRelatorio2.click();

        String arquivo = waitForFileDownload(dir_download, ".pdf");
        ArrayList<ArrayList<String>> retorno = lerRelatorioCertidaoEcac(arquivo);
        clearDirectory(dir_download);
        ArrayList<String> socios = retorno.get(0);
        ArrayList<String> pendencias = retorno.get(1);

        if (socios.size() > 0) {
          System.out.println("Sócios");
          for (String str : socios) {
            System.out.println(str);
          }
        } else {
          System.out.println("Sem Sócios");
        }

        System.out.println("");
        if (pendencias.size() > 0) {
          System.out.println("Pendências");
          for (String str : pendencias) {
            System.out.println(str);
          }
        } else {
          System.out.println("Sem Pendências");
        }

        driver.switchTo().defaultContent();

        WebElement btnHome = waitForClickableElement(driver,
            By.xpath("//*[@id=\"linkHome\"]"));
        btnHome.click();
      }

      if (carregarFaturamentoEcac) {
        /******** FATURAMENTO ********/
        WebElement btnSimplesNacional = waitForClickableElement(driver,
            By.xpath("//*[@id=\"btn266\"]/a"));
        btnSimplesNacional.click();

        WebElement btnPgdasDfis = waitForClickableElement(driver,
            By.xpath("//*[@id=\"containerServicos266\"]/div[2]/ul/li[3]/a"));
        btnPgdasDfis.click();
      }

      if (carregarCertidaoNegativa) {
        /* EMISSÃO DE CERTIDÃO */
        // Abrindo a página dE EMISSÃO DE CERTIFICADO
        driver.get("https://contribuinte.sefaz.al.gov.br/certidao/#/emitircertidao");

        // Inserindo TIPO INSCRIÇÃO
        WebElement selectTipoInscricao = waitForClickableElement(driver,
            By.xpath("/html/body/div[3]/div[1]/div/form/div[1]/select"));
        selectTipoInscricao.click();
        selectTipoInscricao.sendKeys("CNPJ");

        // Inserindo CNPJ
        WebElement cnpjInput = waitForClickableElement(driver,
            By.xpath("//*[@id=\"tipo\"]"));
        cnpjInput.click();
        cnpjInput.clear();
        cnpjInput.sendKeys(CNPJ);

        Thread.sleep(30000);

        WebElement btnEmitir = waitForClickableElement(driver,
            By.xpath("/html/body/div[3]/div[1]/div/form/div[4]/button"));
        btnEmitir.click();

        String arquivo = waitForFileDownload(dir_download, ".pdf");
        clearDirectory(dir_download);
        System.out.println(arquivo);
      }

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      // Fechando o driver do Firefox
      // driver.quit();
    }

    System.out.println("fim " + new Date());
  }

  private static WebElement waitForClickableElement(WebDriver driver, By locator) {
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    return wait.until(ExpectedConditions.elementToBeClickable(locator));
  }

  public static ArrayList<ArrayList<String>> lerRelatorioCertidaoEcac(String arq)
      throws InvalidPasswordException, IOException {
    File file = new File(arq);
    ArrayList<String> socios = new ArrayList<String>();
    ArrayList<String> pendencias = new ArrayList<String>();
    ArrayList<ArrayList<String>> retorno = new ArrayList<ArrayList<String>>();
    try (PDDocument document = PDDocument.load(file)) {
      if (!document.isEncrypted()) {
        PDFTextStripperByArea stripper = new PDFTextStripperByArea();
        stripper.setSortByPosition(true);

        PDFTextStripper tStripper = new PDFTextStripper();
        /*
         * tStripper.setStartPage(34);
         * tStripper.setEndPage(34);
         */

        String pdfFileInText = tStripper.getText(document);
        String lines[] = pdfFileInText.split("\\r?\\n");

        Boolean inicioSocios = false;
        Boolean capturarSocios = false;
        int contLineSocios = 0;

        Boolean inicioPendencias = false;
        Boolean capturarPendencias = false;
        int contLinePendencias = 0;
        for (String line : lines) {
          if (line.toLowerCase().contains("Sócios e Administradores".toLowerCase())) {
            inicioSocios = true;
            contLineSocios = 0;
          } else if (inicioSocios) {
            contLineSocios++;
            if (contLineSocios == 1) {
              capturarSocios = true;
              inicioSocios = false;
            }
          } else if (capturarSocios) {
            if (line.toLowerCase().contains("Certidão Emitida".toLowerCase())) {
              capturarSocios = false;
            } else {
              String[] socio = line.split(" ");
              String cpfSocio = socio[0];
              String nomeSocio = socio[1] + " " + socio[2] + " " + socio[3];
              String cotaSocio = socio[socio.length - 1];
              socios.add("CPF: " + cpfSocio + " | Nome: " + nomeSocio + " | Cota: " + cotaSocio);
            }
          } else if (line.toLowerCase().contains("Pendência - Débito".toLowerCase())) {
            inicioPendencias = true;
            contLinePendencias = 0;
          } else if (inicioPendencias) {
            contLinePendencias++;
            if (contLinePendencias == 2) {
              capturarPendencias = true;
              inicioPendencias = false;
            }
          } else if (capturarPendencias) {
            if (line.toLowerCase()
                .contains("Diagnóstico Fiscal na Procuradoria-Geral da Fazenda Nacional".toLowerCase())) {
              capturarPendencias = false;
            } else {
              String[] pendencia = line.split(" ");
              String situacaoPendencia = pendencia[pendencia.length - 1];
              String saldoDevedorPendencia = pendencia[pendencia.length - 2];
              String valorOriginalPendencia = pendencia[pendencia.length - 3];
              String vencimentoPendencia = pendencia[pendencia.length - 4];
              String competenciaPendencia = pendencia[pendencia.length - 5];

              pendencias.add("Competência: " + competenciaPendencia + " | Vencimento: " + vencimentoPendencia
                  + " | Valor Original: " + valorOriginalPendencia +
                  " | Saldo: " + saldoDevedorPendencia + " | Situação: " + situacaoPendencia);
            }
          }
        }
      }
    }

    retorno.add(socios);
    retorno.add(pendencias);
    return retorno;
  }

}
