package br.com.dbs.teste;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author dassa
 */
public class ConfigurationService {

  private Configuration configuration;
  protected Properties prop;
  protected File f = new File("config.properties");

  public ConfigurationService() {
    this.configuration = new Configuration();
    this.prop = new Properties();
    this.carregar();
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

  public void salvar() throws Exception {
    this.prop.setProperty("url", this.configuration.getUrl());
    this.prop.setProperty("username", this.configuration.getUsername());
    this.prop.setProperty("password", this.configuration.getPassword());
    this.prop.setProperty("dir_firefox", this.configuration.getAppFirefox());
    this.prop.setProperty("exibir_navegador", this.configuration.isExibirNavegador().toString());

    this.prop.store(new FileOutputStream(this.f), null);
    carregar();
  }

  private void carregar() {
    try {
      if (!this.f.exists()) {
        new FileOutputStream(this.f, false).close();
      }
      this.prop.load(new FileInputStream(this.f));
    } catch (IOException e) {
    }

    this.configuration.setUrl(this.prop.getProperty("url"));
    this.configuration.setUsername(this.prop.getProperty("username"));
    this.configuration.setPassword(this.prop.getProperty("password"));
    this.configuration.setAppFirefox(this.prop.getProperty("dir_firefox"));
    this.configuration.setExibirNavegador(Boolean.valueOf(this.prop.getProperty("exibir_navegador", "false")));
  }
}
