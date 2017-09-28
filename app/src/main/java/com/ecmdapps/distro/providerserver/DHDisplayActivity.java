package com.ecmdapps.distro.providerserver;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.ecmdapps.distro.R;

import static com.ecmdapps.distro.providerserver.DHIntentStrings.ERROR_MESSAGE_LABEL;

public class DHDisplayActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_message);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        String message = intent.getStringExtra(ERROR_MESSAGE_LABEL);

        // Capture the layout's TextView and set the string as its text
        TextView textView = (TextView) findViewById(R.id.textView);
        textView.setText(message);
    }
}
