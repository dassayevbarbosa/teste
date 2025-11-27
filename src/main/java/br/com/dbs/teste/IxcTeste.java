package br.com.dbs.teste;

import java.util.Map;

public class IxcTeste {

  public static void main(String[] args) throws Exception {
    Map<String, String> filtros = Map.of(
        "qtype", "fornecedor.cpf_cnpj",
        "query", "21.602.295/0001-46",
        "oper", "=",
        "page", "1",
        "rp", "20",
        "sortname", "fornecedor.id",
        "sortorder", "asc");

    String resposta = IxcWebserviceClient.listarFornecedores(
        "alpha1telecom.com",
        "45:4f5dae1ec99006a18c11f26199ee1fe138e81c9f9a1dcbaab24f87957d6493d1",
        filtros,
        true);

    Functions.writeFile(resposta);

    System.out.println(resposta);
  }

}
