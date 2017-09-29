package com.ecmdapps.distro.providerserver;


import android.app.Activity;
import android.content.Context;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.ecmdapps.distro.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;

import static com.ecmdapps.distro.providerserver.DHNameStrings.HOME_URL;

public class Web3Interface {
    private Web3Resolver web3Resolver;
    private Context context;

    public Web3Interface(Web3Resolver resolver, Context c){
        web3Resolver = resolver;
        context = c;
    }

    @JavascriptInterface
    public String start(String msg){
        if(isHomePage()) {
            if (!web3Resolver.ready() && !web3Resolver.loading()) {
                web3Resolver.ask_for_credentials();
            }
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
        if(isHomePage()){
            return web3Resolver.change_node(param);
        } else {
            return web3Resolver.change_node("current");
        }
    }

    @JavascriptInterface
    public String get_logs(String lines){
        int nbLines = Integer.parseInt(lines);
        return getLogsFromLogCat(nbLines);
    }

    private  String getLogsFromLogCat(int _nbLines) {

        LinkedList<String> logs = new LinkedList<>();

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

    private String currentPage(){
        WebView webView = (WebView) ((Activity) context).findViewById(R.id.webview);
        return webView.getUrl();
    }

    private Boolean isHomePage(){
        return  currentPage().equals(HOME_URL);
    }
}
