package com.ecmdapps.distro.providerserver;


import android.webkit.JavascriptInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;

public class Web3Interface {
    private Web3Resolver web3Resolver;

    public Web3Interface(Web3Resolver resolver){
        web3Resolver = resolver;
    }

    @JavascriptInterface
    public String start(String msg){
        if (!web3Resolver.ready() && !web3Resolver.loading()) {
            web3Resolver.ask_for_credentials();
        }
        return msg;
    }

    @JavascriptInterface
    public String ready(){
        if (web3Resolver.ready() && !web3Resolver.loading()) {
            return "yes";
        } else if (!web3Resolver.ready() && web3Resolver.loading()) {
            return "loading";
        } else {
            return "not started";
        }
    }

    @JavascriptInterface
    public String change_network(String param){
        return web3Resolver.change_node(param);
    }

    @JavascriptInterface
    public String get_logs(String lines){
        int nbLines = Integer.parseInt(lines);
        return getLogsFromLogCat(nbLines);
    }

    private  String getLogsFromLogCat(int _nbLines) {

        LinkedList<String> logs = new LinkedList<String>();

        try {
            Process process = Runtime.getRuntime().exec("logcat -d");
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            String line ;
            while (( line = bufferedReader.readLine()) != null) {
                logs.add(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        String log = "";

        int nb = 0;
        while( (nb < _nbLines) && (logs.size() > 0) ) {
            log += logs.getLast();
            log += "\n";
            logs.removeLast();
            nb++;
        }
        return log;
    }
}
