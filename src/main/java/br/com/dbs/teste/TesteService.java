package br.com.dbs.teste;

public class TesteService {
  public static TesteClass getTesteClass(String nome) {
    TesteClass tc = new TesteClass();
    tc.setNome(nome);
    return tc;
  }
}
