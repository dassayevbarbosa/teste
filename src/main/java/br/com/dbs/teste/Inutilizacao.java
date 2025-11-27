package br.com.dbs.teste;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

public class Inutilizacao {

  private static WebElement waitForClickableElement(WebDriver driver, By locator) {
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    return wait.until(ExpectedConditions.elementToBeClickable(locator));
  }

  public static String formatarCnpj(String cnpj) {
    return String.format("%s.%s.%s/%s-%s", cnpj.substring(0, 2), cnpj.substring(2, 5), cnpj.substring(5, 8),
        cnpj.substring(8, 12), cnpj.substring(12));
  }

  public static String ufParaCodigo(String uf) {
    switch (uf.toUpperCase()) {
      case "AC":
        return "12";
      case "AL":
        return "27";
      case "AP":
        return "16";
      case "AM":
        return "13";
      case "BA":
        return "29";
      case "CE":
        return "23";
      case "DF":
        return "53";
      case "ES":
        return "32";
      case "GO":
        return "52";
      case "MA":
        return "21";
      case "MT":
        return "51";
      case "MS":
        return "50";
      case "MG":
        return "31";
      case "PA":
        return "15";
      case "PB":
        return "25";
      case "PR":
        return "41";
      case "PE":
        return "26";
      case "PI":
        return "22";
      case "RJ":
        return "33";
      case "RN":
        return "24";
      case "RS":
        return "43";
      case "RO":
        return "11";
      case "RR":
        return "14";
      case "SC":
        return "42";
      case "SP":
        return "35";
      case "SE":
        return "28";
      case "TO":
        return "17";
      default:
        return "UF inválida";
    }
  }

  public static void main(String[] args) throws InterruptedException, IOException {
    TimeUnit timeUnit = TimeUnit.SECONDS;

    // CARREGANDO CONFIGURAÇÕES
    File fileConfig = new File("inut.properties");
    Properties prop = new Properties();
    prop.load(new FileInputStream(fileConfig));
    String dirFirefox = prop.getProperty("dir_firefox");

    String UF = JOptionPane.showInputDialog("UF");
    String UF_FORMATADO = ufParaCodigo(UF);
    if (UF_FORMATADO.equals("UF inválida")) {
      JOptionPane.showMessageDialog(null, UF_FORMATADO);
      System.out.println();
      System.exit(1);
    }

    String ANO = JOptionPane.showInputDialog("ANO");

    String CNPJ = JOptionPane.showInputDialog("CNPJ");
    String CNPJ_FORMATADO = "";
    if (CNPJ.length() == 14) {
      CNPJ_FORMATADO = formatarCnpj(CNPJ);
    } else {
      JOptionPane.showMessageDialog(null, "CNPJ inválido!");
      System.exit(1);
    }

    // Configurando as opções do navegador
    FirefoxOptions options = new FirefoxOptions();
    // options.addArguments("--headless");
    options.setBinary(dirFirefox);

    // Inicializando o serviço do driver do Firefox
    System.setProperty("webdriver.gecko.driver", "geckodriver.exe");
    // System.setProperty(FirefoxDriver.SystemProperty.BROWSER_LOGFILE, "null");

    // Inicializando o driver do Firefox
    WebDriver driver = new FirefoxDriver(options);
    driver.get("https://dfe-portal.svrs.rs.gov.br/Nfce/Inutilizacao");
    JavascriptExecutor js = (JavascriptExecutor) driver;

    WebElement weSelectUF = waitForClickableElement(driver, By.id("CodUf"));
    Select selectUF = new Select(weSelectUF);
    selectUF.selectByValue(UF_FORMATADO);

    WebElement inputAno = waitForClickableElement(driver, By.id("Ano"));
    inputAno.click();
    inputAno.clear();
    inputAno.sendKeys(ANO);

    WebElement inputCNPJ = waitForClickableElement(driver, By.id("CnpjFormatado"));
    inputCNPJ.click();
    inputCNPJ.clear();
    inputCNPJ.sendKeys(CNPJ_FORMATADO);

    int reply = JOptionPane.showConfirmDialog(null, "o captcha foi resolvido?", "Resolva o captch",
        JOptionPane.YES_NO_OPTION);
    if (reply != JOptionPane.YES_OPTION) {
      System.exit(0);
    }

    try {
      WebElement mensagem = driver.findElement(By.xpath("//*[@id=\"bodyPricipal\"]/div[1]/div/div/div[1]/p[2]"));
      String mensagemText = mensagem.getText();
      driver.quit();
      JOptionPane.showMessageDialog(null, mensagemText);
      System.exit(0);
    } catch (Exception e) {
      // Caso não haja mensagem de erro, continua o fluxo normal
    }

    XSSFWorkbook workbook = new XSSFWorkbook();

    XSSFSheet sheet = workbook.createSheet("Notas inutilizadas");
    Row row = sheet.createRow(0);
    row.createCell(0).setCellValue("MODELO");
    row.createCell(1).setCellValue("SÉRIE");
    row.createCell(2).setCellValue("NÚMERO INICIAL");
    row.createCell(3).setCellValue("NÚMERO FINAL");
    row.createCell(4).setCellValue("PROTOCOLO");
    row.createCell(5).setCellValue("DATA/HORA");

    try {
      Boolean CONSULTAR = true;
      int num_pagina = 0;
      int cont_linhas_geral = 1;
      while (CONSULTAR) {
        num_pagina++;
        js.executeScript("chamaPagina(" + num_pagina + ")");

        timeUnit.sleep(3);

        List<WebElement> rows = driver
            .findElements(By.xpath("/html/body/div[1]/div/div/div[1]/div/div/div/article/div/div/div/table/tbody/tr"));
        int rowsCount = rows.size();

        if (rowsCount == 0) {
          CONSULTAR = false;
        } else {
          for (int numberLine = 1; numberLine <= rowsCount; numberLine++) {
            cont_linhas_geral++;
            String modelo = driver
                .findElement(By.xpath("/html/body/div[1]/div/div/div[1]/div/div/div/article/div/div/div/table/tbody/tr["
                    + numberLine + "]/td[1]"))
                .getText();
            String serie = driver
                .findElement(By.xpath("/html/body/div[1]/div/div/div[1]/div/div/div/article/div/div/div/table/tbody/tr["
                    + numberLine + "]/td[2]"))
                .getText();
            String num_inicial = driver
                .findElement(By.xpath("/html/body/div[1]/div/div/div[1]/div/div/div/article/div/div/div/table/tbody/tr["
                    + numberLine + "]/td[3]"))
                .getText();
            String num_final = driver
                .findElement(By.xpath("/html/body/div[1]/div/div/div[1]/div/div/div/article/div/div/div/table/tbody/tr["
                    + numberLine + "]/td[4]"))
                .getText();
            String protocolo = driver
                .findElement(By.xpath("/html/body/div[1]/div/div/div[1]/div/div/div/article/div/div/div/table/tbody/tr["
                    + numberLine + "]/td[5]"))
                .getText();
            String data_hora = driver
                .findElement(By.xpath("/html/body/div[1]/div/div/div[1]/div/div/div/article/div/div/div/table/tbody/tr["
                    + numberLine + "]/td[6]"))
                .getText();

            System.out.println(
                modelo + " - " + serie + " - " + num_inicial + " - " + num_final + " - " + protocolo + " - "
                    + data_hora);

            row = sheet.createRow(sheet.getLastRowNum() + 1);
            row.createCell(0).setCellValue(modelo);
            row.createCell(1).setCellValue(serie);
            row.createCell(2).setCellValue(num_inicial);
            row.createCell(3).setCellValue(num_final);
            row.createCell(4).setCellValue(protocolo);
            row.createCell(5).setCellValue(data_hora);
          }
        }
      }
    } finally {
      driver.quit();

      String pathNameExcelDestino = (new File("")).getAbsolutePath() + "\\" + CNPJ + "_" + UF + "_ " + ANO + ".xlsx";
      FileOutputStream out = new FileOutputStream(new File(pathNameExcelDestino));
      workbook.write(out);
      workbook.close();
      out.close();
    }
  }
}
