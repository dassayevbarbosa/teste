package br.com.dbs.teste;

public class Configuration {

  private String url;
  private String username;
  private String password;
  private String appFirefox;
  private Boolean exibirNavegador;

  public Configuration() {
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getAppFirefox() {
    return appFirefox;
  }

  public void setAppFirefox(String appFirefox) {
    this.appFirefox = appFirefox;
  }

  public Boolean isExibirNavegador() {
    return exibirNavegador;
  }

  public void setExibirNavegador(Boolean exibirNavegador) {
    this.exibirNavegador = exibirNavegador;
  }
}
