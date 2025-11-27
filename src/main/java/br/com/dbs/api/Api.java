/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package br.com.dbs.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author dassa
 */
public abstract class Api {

    private final String URL;
    private final String USERNAME;
    private final String PASSWORD;
    private final String USER_AGENT;
    private final String APP_ID;

    public Api(String URL, String USERNAME, String PASSWORD, String USER_AGENT, String APP_ID) {
        this.URL = URL;
        this.USERNAME = USERNAME;
        this.PASSWORD = PASSWORD;
        this.USER_AGENT = USER_AGENT;
        this.APP_ID = APP_ID;
    }

    protected JSONObject post(String metodo, String json) throws Exception {
        try {
            URL url = new URL(this.URL + metodo);
            HttpURLConnection conexao = (HttpURLConnection) url.openConnection();

            conexao.setDoInput(true);
            conexao.setDoOutput(true);

            String auth = this.USERNAME + ":" + this.PASSWORD;
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
            String authHeaderValue = "Basic " + new String(encodedAuth);

            conexao.setRequestMethod("POST");
            conexao.setRequestProperty("Content-Type", "application/json");
            conexao.setRequestProperty("Accept", "application/json");
            conexao.setRequestProperty("User-Agent", this.USER_AGENT);
            conexao.setRequestProperty("Appid", this.APP_ID);
            conexao.setRequestProperty("Authorization", authHeaderValue);

            // DataOutputStream wr = new DataOutputStream(conexao.getOutputStream());
            // wr.writeBytes(json);
            // wr.flush();
            // wr.close();

            // ObjectOutputStream oos = new ObjectOutputStream(conexao.getOutputStream());
            // oos.writeObject(json);
            // oos.close();

            try (OutputStream os = conexao.getOutputStream()) {
                byte[] input = json.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            if (conexao.getResponseCode() != 200) {
                throw new RuntimeException(
                        "HTTP error code : " + conexao.getResponseCode() + "\n"
                                + "HTTP error message : " + conexao.getResponseMessage());
            }

            BufferedReader resposta = new BufferedReader(new InputStreamReader((conexao.getInputStream())));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = resposta.readLine()) != null) {
                response.append(inputLine).append("\n");
            }
            resposta.close();

            System.out.println(response.toString());

            JSONParser parser = new JSONParser();
            JSONObject jsonRetorno = (JSONObject) parser.parse(response.toString());

            return jsonRetorno;
        } catch (IOException | RuntimeException e) {
            throw new Exception("ERRO: " + e);
        }
    }

    protected JSONObject get(String metodo, String parametros) throws Exception {
        try {
            URL url = new URL(this.URL + metodo + parametros);
            HttpURLConnection conexao = (HttpURLConnection) url.openConnection();

            conexao.setDoInput(true);
            conexao.setDoOutput(true);

            String auth = this.USERNAME + ":" + this.PASSWORD;
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
            String authHeaderValue = "Basic " + new String(encodedAuth);

            conexao.setRequestMethod("GET");
            conexao.setRequestProperty("Content-Type", "application/json");
            conexao.setRequestProperty("Accept", "application/json");
            conexao.setRequestProperty("User-Agent", this.USER_AGENT);
            conexao.setRequestProperty("Appid", this.APP_ID);
            conexao.setRequestProperty("Authorization", authHeaderValue);

            if (conexao.getResponseCode() != 200) {
                throw new RuntimeException(
                        "HTTP error code : " + conexao.getResponseCode() + "\n"
                                + "HTTP error message : " + conexao.getResponseMessage());
            }

            BufferedReader resposta = new BufferedReader(new InputStreamReader((conexao.getInputStream())));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = resposta.readLine()) != null) {
                response.append(inputLine).append("\n");
            }
            resposta.close();

            JSONParser parser = new JSONParser();
            JSONObject jsonRetorno = (JSONObject) parser.parse(response.toString());

            return jsonRetorno;
        } catch (IOException | RuntimeException e) {
            throw new Exception("ERRO: " + e);
        }
    }

}
