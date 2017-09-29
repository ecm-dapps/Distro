package com.ecmdapps.distro;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.ecmdapps.distro.providerserver.DHErrorHandler;
import com.ecmdapps.distro.providerserver.DHReqCodes;
import com.ecmdapps.distro.providerserver.Web3Interface;
import com.ecmdapps.distro.providerserver.Web3Resolver;

import org.json.JSONException;

public class MainActivity extends Activity {

    ProgressBar distroProgressBar;
    ProgressBar navigatorProgressBar;
    WebView webView;
    Web3Resolver web3Resolver;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web_view);

        web3Resolver = new Web3Resolver(this);

        distroProgressBar = (ProgressBar) findViewById(R.id.distroProgressBar);
        distroProgressBar.setVisibility(View.GONE);
        navigatorProgressBar = (ProgressBar) findViewById(R.id.navigatorProgressBar);
        navigatorProgressBar.setVisibility(View.GONE);

        webView = (WebView) findViewById(R.id.webview);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient(){
            public void onProgressChanged(WebView view, int progress) {
                navigatorProgressBar.setProgress(progress);
                if (progress == 100) {
                    navigatorProgressBar.setVisibility(View.GONE);
                } else {
                    navigatorProgressBar.setVisibility(View.VISIBLE);
                }
            }
        });
        webView.addJavascriptInterface(new Web3Interface(web3Resolver, this), "distro");
        webView.loadUrl("file:///android_asset/www/index.html");
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DHReqCodes.credentials_req_code()) {
            if (resultCode == Activity.RESULT_OK) {
                web3Resolver.setup(data);
            }
        } else if (requestCode == DHReqCodes.confirmation_req_code()){
            if (resultCode == Activity.RESULT_OK) {
                try {
                    web3Resolver.approval_response(data);
                } catch (JSONException e) {
                    new DHErrorHandler(this, "Web3Interface").handle_error(e);
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
