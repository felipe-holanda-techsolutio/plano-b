package com.exemplo.app;

public class Config {

    static boolean CONEXAO_LOCAL = true;
    static String IP_MAQUINA_DEV = "172.19.227.10";
    static String USUARIO_MAQUINA_DEV = "4io.admin";
    static String SENHA_MAQUINA_DEV = "@4io123";

    static String IP_MAQUINA_SALTO = "172.20.27.251";
    static String USUARIO_MAQUINA_SALTO = "algar_sat";
    static String SENHA_MAQUINA_SALTO = "3ntVWCtY@";

    // ----------------------------
    // IPs do Zabbix
    // ----------------------------
    static String IP_ZABBIX_1 = "172.20.0.22";
    static int PORTA_ZABBIX_1 = 8080;
    static String IP_ZABBIX_2 = "172.20.0.31";
    static int PORTA_ZABBIX_2 = 8080;

}
