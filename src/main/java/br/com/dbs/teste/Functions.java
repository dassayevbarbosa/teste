package br.com.dbs.teste;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.swing.text.MaskFormatter;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Dasssayev Barbosa
 */
public class Functions {

    /**
     * Método que converte um Número em uma String!
     *
     * @param formato Formato do número (0.00)!
     * @param numero  Número a ser convertido em String!
     * @return Retorna uma String com o número formatado!
     */
    public static String formatNumber(String formato, double numero) {
        DecimalFormat df = new DecimalFormat(formato);
        return df.format(numero);
    }// fim do método formatNumber

    /**
     * Método que inseri um caracter em uma String!
     *
     * @param texto String na qual o caracter será inserido!
     * @param c     Caracter a ser inserido!
     * @param qtd   Quantidade de vezes que o caracter será inserido!
     * @param l     Direção na qual o caracter será inserido (D - na direita | E -
     *              na esquerda)!
     * @return Retorna a String com o caracter inserido!
     */
    public static String insertCharacter(String texto, char c, int qtd, char l) {
        String novoTexto = texto;

        if (l == 'E' || l == 'e') {
            while (novoTexto.length() < qtd) {
                novoTexto = c + novoTexto;
            } // fim do while
        } else if (l == 'D' || l == 'd') {
            while (novoTexto.length() < qtd) {
                novoTexto += c;
            } // fim do while
        }
        return novoTexto;
    }// fim do método insertCharacter

    /**
     * Método que verifica se uma String existe em um Array de String!
     *
     * @param valor String a ser pesquisada!
     * @param lista Array de String!
     * @return Retorna true se a String passada por parâmetro existir no Array de
     *         String e false se não existir!
     */
    public static boolean existsInTheList(String valor, String[] lista) {
        for (int i = 0; i < lista.length; i++) {
            try {
                if (lista[i].equals(valor)) {
                    return true;
                } // fim do if
            } catch (Exception ex) {
            } // fim do try/catch
        } // fim do for
        return false;
    }// fim do método existsInTheList

    /**
     * Método que remove um caracter de uma String!
     *
     * @param texto String na qual será retirada o caracter!
     * @param c     Caracter a ser removido!
     * @return Retorna uma String com o caracter removido!
     */
    public static String removeCharacter(String texto, char c) {
        String novoTexto = "";
        for (int i = 0; i < texto.length(); i++) {
            if (texto.charAt(i) != c) {
                novoTexto += texto.charAt(i);
            }
        } // fim do for
        return novoTexto;
    }// fim do método removeCharacter

    /**
     * Método que remove um caracter do início de uma String!
     *
     * @param texto String na qual será retirada o caracter!
     * @param c     Caracter a ser removido!
     * @return Retorna uma String com o caracter removido!
     */
    public static String removeCharacterInicio(String texto, char c) {
        String novoTexto = "";
        boolean isInicio = true;
        for (int i = 0; i < texto.length(); i++) {
            if (isInicio) {
                if (texto.charAt(i) != c) {
                    novoTexto += texto.charAt(i);
                    isInicio = false;
                }
            } else {
                novoTexto += texto.charAt(i);
            }

        } // fim do for
        return novoTexto;
    }// fim do método removeCharacter

    /**
     * Método que troca uma caracter por outro em um texto!
     *
     *
     * @param texto String com o texto!
     * @param c     Caracter a ser substituído!
     * @param d     Novo caracter!
     * @return Retorna uma String com os caracteres passados por parâmetro
     *         substituídos!
     */
    public static String exchangeCharacter(String texto, char c, char d) {
        String novoTexto = "";
        for (int i = 0; i < texto.length(); i++) {
            if (texto.charAt(i) != c) {
                novoTexto += texto.charAt(i);
            } else {
                novoTexto += d;
            }
        } // fim do for
        return novoTexto;
    }// fim do exchangeCharacter

    /**
     * Método que verifica se uma caracter existe em uma String!
     *
     * @param texto String com o texto!
     * @param c     Caracter a ser procurado no texto!
     * @return Retorna true se o caracter passado por parâmetro existir no texto e
     *         false se não existir!
     */
    public static boolean existsInText(String texto, char c) {
        for (int i = 0; i < texto.length(); i++) {
            if (texto.charAt(i) == c) {
                return true;
            }
        } // fim do for
        return false;
    }// fim do método existsInText

    /**
     * Método que remove acentos de uma String!
     *
     * @param txt String com letras acentuadas!
     * @return Retorna uma String sem acentos!
     */
    public static String removeAccents(String txt) {
        String s = "";
        for (int i = 0; i < txt.length(); i++) {
            char c = txt.charAt(i);
            switch (c) {
                case 'Á':
                case 'À':
                case 'Ã':
                    c = 'A';
                    break;
                case 'È':
                case 'É':
                case 'Ê':
                    c = 'E';
                    break;
                case 'Ì':
                case 'Í':
                    c = 'I';
                    break;
                case 'Ó':
                case 'Ò':
                case 'Õ':
                case 'Ô':
                    c = 'O';
                    break;
                case 'Ù':
                case 'Ú':
                    c = 'U';
                    break;
                case 'Ç':
                    c = 'C';

                case 'á':
                case 'à':
                case 'ã':
                    c = 'a';
                    break;
                case 'è':
                case 'é':
                case 'ê':
                    c = 'e';
                    break;
                case 'ì':
                case 'í':
                    c = 'i';
                    break;
                case 'ó':
                case 'õ':
                case 'ô':
                    c = 'o';
                    break;
                case 'ù':
                case 'ú':
                    c = 'u';
                    break;
                case 'ç':
                    c = 'c';
                    break;
            }
            s += c;
        }
        return s;
    }// fim do método removeAccents

    /**
     * Método que recebe uma String e verifica se é uma Data!
     *
     * @param data String com a suposta data!
     * @return Retorna true se o parâmetro for uma dara e false se não for!
     */
    public static boolean isDate(String data) {
        try {
            DateFormat formatterPT = new SimpleDateFormat("dd/MM/yyyy");
            formatterPT.parse(data);
            return true;
        } catch (Exception e) {
            return false;
        }
    }// fim do método isData

    /**
     * Método que recebe uma String e retorna uma String apenas com números!
     *
     * @param valor String com letras e números!
     * @return Retorna uma String apenas com números contidos na String passada por
     *         parâmetro!
     */
    public static String justNumber(String valor) {
        String novoTexto = "";
        for (int i = 0; i < valor.length(); i++) {
            String s = valor.charAt(i) + "";

            try {
                int f = Integer.parseInt(s);
                novoTexto += f;
            } catch (Exception e) {
            } // fim do try/catch
        } // fim do for
        return novoTexto;
    }// fim do método apenasNumer

    /**
     * Método que remove caracteres especiais de uma String!
     *
     * @param valor String que contém caracteres especiais!
     * @return String sem os caracteres especiais!
     */
    public static String removerCaracterEspeciais(String valor) {
        String test = valor;
        String pattern = "[^A-Z^\\s]";
        String strippedString = test.replaceAll(pattern, "");
        return strippedString;
    }// fim do método removeCaracterEspeciais

    /**
     * Método que converte uma Data em String!
     *
     * @param data    Data que será convertida!
     * @param formato Formato da data!
     * @return Retorna uma String com a data formatada!
     */
    public static String formatDate(Date data, String formato) {
        if (data == null) {
            return "";
        }

        DateFormat f = new SimpleDateFormat(formato);
        return f.format(data);
    }// fim do método formataData

    /**
     * Método que recebe uma String e converte em Date!
     *
     * @param data    String com a data!
     * @param formato String com o formato da data (dd/MM/yyyy ; yyyy/MM/dd)
     * @return Retorna uma data no formato passado no parâmetro
     * @throws ParseException
     */
    public static Date stringToDate(String data, String formato) throws ParseException {
        try {
            DateFormat f = new SimpleDateFormat(formato);
            return f.parse(data);
        } // fim do método stringToDate
        catch (ParseException ex) {
        }
        return null;
    }// fim do método stringToDate

    /**
     * Método que formata o nome dos fields de uma classe!
     *
     * @param nomeField String com o nome do field!
     * @return Retorna o nome do field formatado (dataNascimento = Data Nascimento)
     */
    public static String formatNameField(String nomeField) {
        ArrayList<Integer> maiusculo = new ArrayList<Integer>();
        maiusculo.add(65);
        maiusculo.add(66);
        maiusculo.add(67);
        maiusculo.add(68);
        maiusculo.add(69);
        maiusculo.add(70);
        maiusculo.add(71);
        maiusculo.add(72);
        maiusculo.add(73);
        maiusculo.add(74);
        maiusculo.add(75);
        maiusculo.add(76);
        maiusculo.add(77);
        maiusculo.add(78);
        maiusculo.add(79);
        maiusculo.add(80);
        maiusculo.add(81);
        maiusculo.add(82);
        maiusculo.add(83);
        maiusculo.add(84);
        maiusculo.add(85);
        maiusculo.add(86);
        maiusculo.add(87);
        maiusculo.add(88);
        maiusculo.add(89);
        maiusculo.add(90);

        String novoNome = nomeField.substring(0, 1).toUpperCase();

        for (int i = 1; i < nomeField.length(); i++) {
            if (maiusculo.contains(nomeField.codePointAt(i))) {
                novoNome += " " + nomeField.charAt(i);
            } else {
                novoNome += nomeField.charAt(i);
            } // fim do if/else
        } // fim do for

        return novoNome;
    }// fim do método formatNameField

    /**
     * Método que retorna a quantidade de linhas de uma String!
     *
     * @param s String que ser quer obter a quantidade de linhas!
     * @return Retorna a quantidade de linhas da String!
     */
    public static int totalLines(String s) {
        try {
            File arquivoLeitura = new File(s);

            // pega o tamanho
            long tamanhoArquivo = arquivoLeitura.length();
            FileInputStream fs = new FileInputStream(arquivoLeitura);
            DataInputStream in = new DataInputStream(fs);

            LineNumberReader lineRead = new LineNumberReader(new InputStreamReader(in));
            lineRead.skip(tamanhoArquivo);
            // conta o numero de linhas do arquivo, começa com zero, por isso adiciona 1
            int numLinhas = lineRead.getLineNumber() + 1;
            lineRead.close();
            return numLinhas;

        } catch (IOException e) {
            // TODO: Tratar exceção
        }
        return 0;
    }// fim do método totalLines

    /**
     * Método para recuperar o index de uma letra do alfabeto!
     *
     * @param texto Letra que se quer saber o index (A, B, AD, BT)!
     * @return Retorna o index da letra passada por parâmetro!
     */
    public static int indexLettersAlphabet(String texto) {
        String[] alfabeto = { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
                "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z" };

        int tamanhoTexto = texto.length();
        String ultimoCaracter = texto.substring(tamanhoTexto - 1);
        int posicao = 0;

        for (int i = 0; i < alfabeto.length; i++) {
            if (ultimoCaracter.equals(alfabeto[i])) {
                posicao = i + 1;
                break;
            } // fim do if
        } // fim do for

        int index = 26 * (tamanhoTexto - 1) + posicao;
        return index;
    }// fim do método indexLettersAlphabet

    /**
     * Método que recebe um cpnj e formata-o (11.111.111/1111-11)!
     *
     * @param cnpj String contendo o Cnpj!
     * @return String com o CNPJ formatado!
     */
    public static String formatCpnj(String cnpj) {
        if (cnpj.length() == 14) {
            String newCnpj = cnpj.substring(0, 2) + "." + cnpj.substring(2, 5) + "."
                    + cnpj.substring(5, 8) + "/" + cnpj.substring(8, 12) + "-"
                    + cnpj.substring(12);
            return newCnpj;
        }
        return "";
    }

    /**
     * Método que formata uma string de acordo com a máscara passada por paramentro!
     *
     * @param texto   String a ser formata!
     * @param mascara String com a máscara a ser utilizada na formatação!
     * @return String formatada!
     */
    public static String formatString(String texto, String mascara) {
        try {
            MaskFormatter mf = new MaskFormatter(mascara);
            mf.setValueContainsLiteralCharacters(false);
            return mf.valueToString(texto);
        } catch (ParseException e) {
            e.printStackTrace();
            return "";
        } // fim do try/catch
    }// fim do metodo formatString

    /**
     * Método que recebe uma string com valores separados por um caracter e retorna
     * uma lista com os valores separados!
     *
     * @param linha             String a ser quabrada!
     * @param separador         String com o separador dos campos!
     * @param separadorInicia   Valor booleano que indica se o separador inicia a
     *                          linha!
     * @param separadorFinaliza Valor booleano que indica se o separador finaliza a
     *                          linha!
     * @return List, onde cada registro da lista é um campo da string passada!
     */
    public static ArrayList<String> quebraLinhaPorSeparador(String linha, String separador, boolean separadorInicia,
            boolean separadorFinaliza) {
        ArrayList<String> list = new ArrayList<String>();
        int contSeparador = 0;
        String registro = "";

        if (linha == null) {
            linha = "";
        }

        for (int i = 0; i < linha.length(); i++) {
            if (linha.charAt(i) != separador.charAt(0)) {
                registro += linha.charAt(i);
            } else {
                contSeparador++;
                if (contSeparador == 1 && separadorInicia) {
                    continue;
                } else {
                    list.add(registro);
                    registro = "";
                }

            }
        } // fim do for

        if (!separadorFinaliza) {
            list.add(registro);
        } // fim do id

        return list;
    }

    /**
     * Método que recebe uma lista com valores e retorna uma string com os valores
     * separados por um separador!
     *
     * @param lista             Lista a ser quabrada!
     * @param separador         String com o separador dos campos!
     * @param separadorInicia   Valor booleano que indica se o separador inicia a
     *                          linha!
     * @param separadorFinaliza Valor booleano que indica se o separador finaliza a
     *                          linha!
     * @return String com os valores da lista separados pelo separador!
     */
    public static String escreveListaEmStringComSeparador(ArrayList<String> lista, String separador,
            boolean separadorInicia, boolean separadorFinaliza) {
        String str = "";

        if (separadorInicia) {
            str = separador;
        }

        for (String s : lista) {
            str += s + separador;
        } // fim do for

        if (!separadorFinaliza) {
            int t = str.length();
            str = str.substring(0, t - 1);
        } // fim do id

        return str;
    }// fim do método escreveListaEmStringComSeparador

    public static java.util.List<File> listDirectoryAppend(File dir, java.util.List<File> lista, String extensao) {
        if (dir.isDirectory()) {
            String[] filhos = dir.list();
            for (String filho : filhos) {
                File nome = new File(dir, filho);
                if (nome.isFile()) {
                    if (nome.getName().toUpperCase().endsWith(extensao)) {
                        lista.add(nome);
                    }
                } else if (nome.isDirectory()) {
                    listDirectoryAppend(nome, lista, extensao);
                }
            }
        } else {
            lista.add(dir);
        }
        return lista;
    }

    public static String getCellValue(Cell cell) {
        return getCellValue(cell, true);
    }

    public static String getCellValue(Cell cell, boolean ignoreDate) {
        if (cell == null) {
            return "";
        }

        if ((cell.getCellType() == CellType.NUMERIC) && (!ignoreDate)) {
            if (!DateUtil.isCellDateFormatted(cell)) {
                BigDecimal bd = BigDecimal.valueOf(cell.getNumericCellValue());
                return bd.toPlainString(); // evita notação científica
            }
        }

        DataFormatter formatter = new DataFormatter(); // respeita o formato visual
        String resultado = formatter.formatCellValue(cell);
        if (resultado == null || resultado.isEmpty() || resultado.equals("null")) {
            return "";
        }
        return resultado;
    }

    public static List<String> getArquivos(String dir, String extensao) {
        List<String> listaArquivos = new ArrayList();
        if (dir.equals("")) {
            return listaArquivos;
        }

        File file = new File(dir);
        File afile[] = file.listFiles();
        int i = 0;
        for (int j = afile.length; i < j; i++) {
            File arquivos = afile[i];
            if (!arquivos.isDirectory()) {
                if (arquivos.getAbsolutePath().toLowerCase().endsWith(extensao)) {
                    listaArquivos.add(arquivos.getAbsolutePath());
                }
                continue;
            }
            listaArquivos.addAll(getArquivos(arquivos.getAbsolutePath(), extensao));
        }
        return listaArquivos;
    }

    public static void writeFile(String content) {
        try (FileWriter writer = new FileWriter("D:\\temp\\" +
                UUID.randomUUID().toString() + ".txt")) {
            writer.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
