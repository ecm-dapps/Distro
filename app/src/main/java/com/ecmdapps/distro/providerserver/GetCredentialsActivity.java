package com.ecmdapps.distro.providerserver;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.ecmdapps.distro.R;

import static com.ecmdapps.distro.providerserver.DHIntentStrings.CREDENTIALS_LABEL;

public class GetCredentialsActivity extends Activity{
    private static final String PREFS = DHNameStrings.PREFERENCES_NAME;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.credentials_dialog);
        setFinishOnTouchOutside(false);

        Context ca = getApplicationContext();
        SharedPreferences settings = ca.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String wallet_path = settings.getString(DHNameStrings.PATH_TO_WALLET_LABEL, "none");
        TextView title = (TextView) findViewById(R.id.credentials_title);

        if (wallet_path.equals("none")){
            title.setText(R.string.new_wallet_credentials_title);
        } else {
            title.setText(R.string.old_wallet_credentials_title);
        }

        final EditText editText = (EditText) findViewById(R.id.password_input);
        Button done = (Button) findViewById(R.id.done);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String crepass = editText.getText().toString();
                Intent i = new Intent();
                i.putExtra(CREDENTIALS_LABEL, crepass);
                setResult(RESULT_OK, i);
                finish();
            }
        });
    }
}
