package com.exemplo.app;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import static com.exemplo.app.Config.*;

public class App {

    public static void main(String[] args) {
        System.out.println("Deseja executar via CURL ou via Java?\n" +
                "1 - CURL\n" +
                "2 - Java\n");
        String opcao = System.console().readLine();

        switch (opcao) {
            case "1":
                System.out.println("Executando via CURL...");
                executaCurl();
                break;
            case "2":
                System.out.println("Executando via Java...");
                executaJava();
                break;
            default:
                System.out.println("Opção inválida.");
        }




    }

    private static void executaCurl(){
        // Conecta à máquina de desenvolvimento
        if (CONEXAO_LOCAL) {
            System.out.println("Conectando à máquina de desenvolvimento...");

            /*
            Passo a passo:
            1. Conectar à máquina de desenvolvimento
            2. Através da máquina de desenvolvimento, conectar ao SALTO SERVER
            3. Fazer uma requisicao HTTP POST para o Zabbix
             */

            // Conectar à máquina de desenvolvimento
            String comandoConexaoDev = "ssh " + USUARIO_MAQUINA_DEV + "@" + IP_MAQUINA_DEV;
            String comandoSenhaDev = "echo " + SENHA_MAQUINA_DEV + " | sudo -S -k ls";
            String comandoConexaoSalto = "ssh " + USUARIO_MAQUINA_SALTO + "@" + IP_MAQUINA_SALTO;
            String comandoSenhaSalto = "echo " + SENHA_MAQUINA_SALTO + " | sudo -S -k ls";

            try {
                ProcessBuilder pb = new ProcessBuilder("bash", "-c", comandoConexaoDev);
                Process process = pb.start();
                process.waitFor();

                pb = new ProcessBuilder("bash", "-c", comandoSenhaDev);
                process = pb.start();
                process.waitFor();

                System.out.println("Conectado à máquina de desenvolvimento.");

                pb = new ProcessBuilder("bash", "-c", comandoConexaoSalto);
                process = pb.start();
                process.waitFor();

                pb = new ProcessBuilder("bash", "-c", comandoSenhaSalto);
                process = pb.start();
                process.waitFor();

                System.out.println("Conectado à máquina de salto.");

                //Envia requisição post ao zabbix
            } catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println("Enviando requisição ao Zabbix...");

            String zabbixApiUrl = "http://" + IP_ZABBIX_1 + ":" + PORTA_ZABBIX_1 + "/api_jsonrpc.php";

            // JSON de autenticação
            String json = "{\n" +
                    "    \"jsonrpc\": \"2.0\",\n" +
                    "    \"method\": \"user.login\",\n" +
                    "    \"params\": {\n" +
                    "        \"username\": \"Admin\",\n" +
                    "        \"password\": \"zabbix\"\n" +
                    "    },\n" +
                    "    \"id\": 1\n" +
                    "}";

            String comandoCurl = "curl -X POST -H \"Content-Type: application/json\" -d '" + json + "' " + zabbixApiUrl;
            try {
                ProcessBuilder pb = new ProcessBuilder("bash", "-c", comandoCurl);
                Process process = pb.start();
                process.waitFor();

                // Ler a resposta
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder responseString = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseString.append(line);
                }

                System.out.println("Resposta do Zabbix:");
                System.out.println(responseString.toString());

                System.out.println("Requisição enviada ao Zabbix.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void executaJava(){
        JSch jsch = new JSch();
        jsch.setLogger(new MyLogger());

        Session sessionDev = null;
        Session sessionSalto = null;

        try {
            // Conectar primeiro à máquina de desenvolvimento
            if (CONEXAO_LOCAL) {
                sessionDev = jsch.getSession(USUARIO_MAQUINA_DEV, IP_MAQUINA_DEV, 22);
                sessionDev.setPassword(SENHA_MAQUINA_DEV);
                sessionDev.setConfig("StrictHostKeyChecking", "no");
                sessionDev.connect();
                System.out.println("Conectado à máquina de desenvolvimento.");
            }

            // Conectar ao SALTO SERVER através da máquina de desenvolvimento
            sessionSalto = jsch.getSession(USUARIO_MAQUINA_SALTO, IP_MAQUINA_SALTO, 22);
            sessionSalto.setPassword(SENHA_MAQUINA_SALTO);
            sessionSalto.setConfig("StrictHostKeyChecking", "no");
            sessionSalto.connect();
            System.out.println("Conectado ao SALTO SERVER.");

            // Redirecionar a porta local 8080 para a porta 80 do Zabbix (172.20.0.22)
            sessionSalto.setPortForwardingL(PORTA_ZABBIX_1, IP_ZABBIX_1, 80);
            System.out.println("Túnel SSH criado com sucesso. Enviando requisição ao Zabbix...");

            // Fazer a requisição HTTP POST para o Zabbix
            sendPostRequest(IP_ZABBIX_1, PORTA_ZABBIX_1);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Desconectar as sessões
            if (sessionSalto != null) {
                sessionSalto.disconnect();
                System.out.println("Desconectado do SALTO SERVER.");
            }

            if (sessionDev != null) {
                sessionDev.disconnect();
                System.out.println("Desconectado da máquina de desenvolvimento.");
            }
        }
    }

    public static void sendPostRequest(String zabbixHost, int zabbixPort) {
        String zabbixApiUrl = "http://" + zabbixHost + ":" + zabbixPort + "/api_jsonrpc.php";

        // JSON de autenticação
        String json = "{\n" +
                "    \"jsonrpc\": \"2.0\",\n" +
                "    \"method\": \"user.login\",\n" +
                "    \"params\": {\n" +
                "        \"username\": \"Admin\",\n" +
                "        \"password\": \"zabbix\"\n" +
                "    },\n" +
                "    \"id\": 1\n" +
                "}";

        // Criar cliente HTTP
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Criar requisição HTTP POST
            HttpPost postRequest = new HttpPost(zabbixApiUrl);
            postRequest.setHeader("Content-Type", "application/json");
            postRequest.setEntity(new StringEntity(json));

            // Executar a requisição
            try (CloseableHttpResponse response = httpClient.execute(postRequest)) {
                // Ler a resposta
                BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                StringBuilder responseString = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseString.append(line);
                }

                // Imprimir resposta JSON
                System.out.println("Resposta do Zabbix:");
                System.out.println(responseString.toString());

                // Usar Jackson para processar o JSON de retorno
                ObjectMapper objectMapper = new ObjectMapper();
                ZabbixAuthResponse authResponse = objectMapper.readValue(responseString.toString(), ZabbixAuthResponse.class);
                System.out.println("Token recebido: " + authResponse.getResult());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Implementação do Logger
    static class MyLogger implements com.jcraft.jsch.Logger {
        public boolean isEnabled(int level) {
            return true; // Habilitar todos os níveis de log
        }

        public void log(int level, String message) {
            System.out.println("JSch: " + message);
        }
    }

    // Classe para mapear a resposta da autenticação
    static class ZabbixAuthResponse {
        private String jsonrpc;
        private String result;
        private int id;

        public String getJsonrpc() {
            return jsonrpc;
        }

        public void setJsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }
    }
}
