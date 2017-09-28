package com.ecmdapps.distro.providerserver;

import android.app.Activity;
import android.content.Intent;

import java.io.PrintWriter;
import java.io.StringWriter;

import static com.ecmdapps.distro.providerserver.DHIntentStrings.ERROR_MESSAGE_LABEL;

public class DHErrorHandler {

    private String from;
    private Activity ownerActivity;

    public DHErrorHandler(Activity ownerActivity, String from){
        this.ownerActivity = ownerActivity;
        this.from = from;
    }

    public void handle_error(Exception e){
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        showMessage(sw.toString());
    }

    void showMessage(String s) {
        Intent intent = new Intent(this.ownerActivity, DHDisplayActivity.class);
        s = from + ": " + s;
        intent.putExtra(ERROR_MESSAGE_LABEL, s);
        this.ownerActivity.startActivity(intent);
    }
}
