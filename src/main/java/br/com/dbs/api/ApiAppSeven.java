/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package br.com.dbs.api;

import org.json.simple.JSONObject;

/**
 *
 * @author dassa
 */
public class ApiAppSeven extends Api {

    public ApiAppSeven(String appId, String servidor) {
        super(
                servidor + "/gconciliador/",
                "automacaoseven",
                "7Seven_senha@",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.95 Safari/537.36",
                appId);
    }

    public JSONObject downloadPdf() throws Exception {
        String PARAMETROS = "";
        return get("fronteira", PARAMETROS);
    }

    public JSONObject sendRazaoTecnoGera(String json) throws Exception {
        return post("tecnogeraRazaoJson", json);
    }

}
