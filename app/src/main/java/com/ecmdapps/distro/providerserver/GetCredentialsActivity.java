package com.ecmdapps.distro.providerserver;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import com.ecmdapps.distro.R;

import static com.ecmdapps.distro.providerserver.DHIntentStrings.CREDENTIALS_LABEL;

public class GetCredentialsActivity extends Activity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.credentials_dialog);
        setFinishOnTouchOutside(false);

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
