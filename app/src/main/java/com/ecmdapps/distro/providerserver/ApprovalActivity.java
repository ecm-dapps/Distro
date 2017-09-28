package com.ecmdapps.distro.providerserver;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.ecmdapps.distro.MainActivity;
import com.ecmdapps.distro.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.Locale;

import static com.ecmdapps.distro.providerserver.DHIntentStrings.APPROVED_MESSAGE_LABEL;
import static com.ecmdapps.distro.providerserver.DHIntentStrings.APPROVED_PARAMS_LABEL;
import static com.ecmdapps.distro.providerserver.DHIntentStrings.CANCELLED_LABEL;
import static com.ecmdapps.distro.providerserver.DHIntentStrings.ERROR_MESSAGE_LABEL;
import static com.ecmdapps.distro.providerserver.DHIntentStrings.ORIGINAL_DATA_LABEL;
import static com.ecmdapps.distro.providerserver.DHIntentStrings.PAYLOAD_PARAMS_LABEL;
import static com.ecmdapps.distro.providerserver.DHIntentStrings.REQUEST_ID_LABEL;
import static com.ecmdapps.distro.providerserver.DHIntentStrings.RPC_METHOD_LABEL;
import static com.ecmdapps.distro.providerserver.DHIntentStrings.SIGNONLY_LABEL;

public class ApprovalActivity extends Activity {

    private View dialog_view;
    private Activity approvalActivity;

    private final BigInteger one_eth = new BigInteger("1").pow(18);
    private final BigInteger one_gwei = new BigInteger("1").pow(9);

    private Bundle b;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = getIntent().getExtras();
        approvalActivity = this;
        try {
            if (b == null) throw new AssertionError("no parameters passed for approval");
        } catch (AssertionError e){
            e.printStackTrace();
            setResult(RESULT_CANCELED, new Intent().putExtra(ERROR_MESSAGE_LABEL, e.toString()));
            finish();
        }
        try {
            JSONObject payload_params = new JSONObject(b != null ? b.getString(PAYLOAD_PARAMS_LABEL) : null);
            JSONObject data = new JSONObject(b != null ? b.getString(ORIGINAL_DATA_LABEL) : null);
            String rpc_method = data.getString("method");
            int xml_layout = get_xml_layout(rpc_method);
            dialog_view = getLayoutInflater().inflate(xml_layout, null);
            populate_view(payload_params, rpc_method);
            setContentView(dialog_view);

            Button ok = (Button) findViewById(R.id.ok);
            Button cancel = (Button) findViewById(R.id.cancel);
            ok.setOnClickListener(new PositiveListener(data));
            cancel.setOnClickListener(new NegativeListener(data));

        } catch (JSONException e) {
            e.printStackTrace();
            setResult(RESULT_CANCELED, new Intent().putExtra(ERROR_MESSAGE_LABEL, e.toString()));
            finish();
        }



    }
    private void populate_view(JSONObject payload_params, String rpc_method) {
        if (rpc_method.equals("eth_sendTransaction") || rpc_method.equals("eth_signTransaction")) {
            populate_view_for_send_transaction(payload_params);

        } else if (rpc_method.equals("eth_sign")) {
            populate_view_for_sign_message(payload_params);
        }
    }

    private void populate_view_for_sign_message(JSONObject payload_params){
        ((EditText) dialog_view.findViewById(R.id.sender))
                .setText(payload_params.optString("from", "0x0"));
        ((EditText) dialog_view.findViewById(R.id.message))
                .setText(payload_params.optString("data", ""));
    }

    private void populate_view_for_send_transaction(JSONObject payload_params) {
        ((EditText) dialog_view.findViewById(R.id.sender))
                .setText(payload_params.optString("from", "0x0"));

        ((EditText) dialog_view.findViewById(R.id.recipient))
                .setText(payload_params.optString("to", "0x0"));

        ((EditText) dialog_view.findViewById(R.id.data))
                .setText(payload_params.optString("data", "0x0"));

        ((EditText) dialog_view.findViewById(R.id.gas))
                .setText(
                        String.format(Locale.getDefault(), "%d",
                                new BigInteger(payload_params.optString("gas", "0x0").substring(2), 16)
                        )
                );

        ((EditText) dialog_view.findViewById(R.id.transaction_value))
                .setText(
                        String.format(Locale.getDefault(), "%d",
                                new BigInteger(payload_params.optString("value", "0x0").substring(2), 16).divide(one_eth)
                        )
                );


        ((EditText) dialog_view.findViewById(R.id.gas_price))
                .setText(
                        String.format(Locale.getDefault(), "%d",
                                new BigInteger(payload_params.optString("gasPrice", "0x0").substring(2), 16).divide(one_gwei)
                        )
                );
    }


    private int get_xml_layout(String rpc_method) {
        if (rpc_method.equals("eth_sendTransaction")) {
            return R.layout.approve_transaction_view;
        } else if (rpc_method.equals("eth_sign")) {
            return R.layout.approve_message_view;
        } else {
            return R.layout.dialog_view;
        }
    }

    private class PositiveListener implements View.OnClickListener {
        private JSONObject data;
        private String rpc_method;

        PositiveListener(JSONObject jsonObject) {
            this.data = jsonObject;
            this.rpc_method = this.data.optString("method", "N/A");
        }

        @Override
        public void onClick(View v) {
            if (rpc_method.equals("eth_sendTransaction") || rpc_method.equals("eth_signTransaction")) {
                try {
                    process_transaction(rpc_method);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else if (rpc_method.equals("eth_sign")) {
                process_message(rpc_method);
            } else {
                setResult(RESULT_CANCELED, new Intent().putExtra(ERROR_MESSAGE_LABEL, "method currently not supported for approval"));
                finish();
            }
        }

        private void process_message(String rpc_method) {
            String message = ((EditText) dialog_view.findViewById(R.id.message)).getText().toString();
            Intent i = new Intent(approvalActivity, MainActivity.class);
            i.putExtra(RPC_METHOD_LABEL, rpc_method);
            i.putExtra(ORIGINAL_DATA_LABEL, data.toString());
            i.putExtra(APPROVED_MESSAGE_LABEL, message);
            i.putExtra(REQUEST_ID_LABEL, b.getString(REQUEST_ID_LABEL));
            setResult(RESULT_OK, i);
            finish();
        }

        private void process_transaction(String rpc_method) throws JSONException {
            String gas_price = ((EditText) dialog_view.findViewById(R.id.gas_price)).getText().toString();
            String hex_gas_price = "0x" + new BigInteger(gas_price).toString(16);
            String gas = ((EditText) dialog_view.findViewById(R.id.gas)).getText().toString();
            String hex_gas = "0x" + new BigInteger(gas).toString(16);

            JSONArray payload = (JSONArray) data.get("params");
            JSONObject payload_params = (JSONObject) payload.get(0);
            payload_params.put("gasPrice", hex_gas_price);
            payload_params.put("gas", hex_gas);

            Intent i = new Intent(approvalActivity, MainActivity.class);
            i.putExtra(REQUEST_ID_LABEL, rpc_method);
            i.putExtra(ORIGINAL_DATA_LABEL, data.toString());
            i.putExtra(REQUEST_ID_LABEL, b.getString("requestId"));
            i.putExtra(APPROVED_PARAMS_LABEL, payload_params.toString());

            if(this.rpc_method.equalsIgnoreCase("eth_signTransaction")){
                i.putExtra(SIGNONLY_LABEL, true);
            } else {
                i.putExtra(SIGNONLY_LABEL, false);
            }
            setResult(RESULT_OK, i);
            finish();
        }
    }

    private class NegativeListener implements View.OnClickListener {
        private JSONObject data;

        NegativeListener(JSONObject data) {
            this.data = data;
        }

        @Override
        public void onClick(View v) {
            Intent i = new Intent(approvalActivity, MainActivity.class);
            i.putExtra(ORIGINAL_DATA_LABEL, data.toString());
            i.putExtra(CANCELLED_LABEL, true);
            i.putExtra(REQUEST_ID_LABEL, REQUEST_ID_LABEL);
            setResult(RESULT_OK, i);
            finish();
        }
    }
}