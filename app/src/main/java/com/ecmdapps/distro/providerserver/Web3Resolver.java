package com.ecmdapps.distro.providerserver;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Sign;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.RawTransaction;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.infura.InfuraHttpService;
import org.web3j.utils.Numeric;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.ecmdapps.distro.providerserver.DHIntentStrings.APPROVED_MESSAGE_LABEL;
import static com.ecmdapps.distro.providerserver.DHIntentStrings.APPROVED_PARAMS_LABEL;
import static com.ecmdapps.distro.providerserver.DHIntentStrings.CANCELLED_LABEL;
import static com.ecmdapps.distro.providerserver.DHIntentStrings.CREDENTIALS_LABEL;
import static com.ecmdapps.distro.providerserver.DHIntentStrings.ORIGINAL_DATA_LABEL;
import static com.ecmdapps.distro.providerserver.DHIntentStrings.PAYLOAD_PARAMS_LABEL;
import static com.ecmdapps.distro.providerserver.DHIntentStrings.REQUEST_ID_LABEL;
import static com.ecmdapps.distro.providerserver.DHIntentStrings.RPC_METHOD_LABEL;
import static com.ecmdapps.distro.providerserver.DHIntentStrings.SIGNONLY_LABEL;

public class Web3Resolver {
    private Future<Credentials> credentialsFuture;
    private Future<Web3j> web3Future;

    private Web3j web3;
    private Credentials credentials;
    private DHErrorHandler dhe;
    private Responder r;
    private Activity ownerActivity;

    private Handler handler;
    private Runnable web3Checker;
    private Runnable credentialsChecker;

    private Web3Resolver self;

    private static final Object lock = new Object();
    private static final String PREFS = DHNameStrings.PREFERENCES_NAME;

    private boolean web3Loaded = false;
    private boolean web3Loading = false;
    private boolean credentialsLoaded = false;
    private boolean credentialsLoading = false;

    public Web3Resolver(Activity activity)  {
        self = this;
        self.handler = new Handler();
        self.ownerActivity = activity;
        self.r = new ProviderServer(ownerActivity, self);
        self.dhe = new DHErrorHandler(ownerActivity, "Web3Resolver");
    }

    void ask_for_credentials(){
        Log.d("providerservice", "in web3 waiting for ACTIVITY");

        Intent i = new Intent(self.ownerActivity, GetCredentialsActivity.class);
        self.ownerActivity.startActivityForResult(i, DHReqCodes.credentials_req_code());
    }

    public void setup(Intent data) {
        String p = data.getStringExtra(CREDENTIALS_LABEL);
        setup(p);
    }

    private void reload_web3(){
        ExecutorService web3Executor = Executors.newFixedThreadPool(1);

        web3Future = web3Executor.submit(new Callable<Web3j>(){
            @Override
            public Web3j call (){
                return  Web3jFactory.build(new InfuraHttpService(current_node()));
            }
        });

        web3Checker = new Runnable() {
            @Override
            public void run() {
                if(web3Ready()){
                    try {
                        setWeb3();
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                        dhe.handle_error(e);
                    }
                } else {
                    self.web3Loading = true;
                    handler.postDelayed(web3Checker, 500);
                }
            }
        };

        web3Checker.run();
    }

    boolean loading() {
        return credentialsLoading || web3Loading;
    }
    boolean ready(){
        return credentialsLoaded && web3Loaded;
    }

    private void setup(final String p) {
        web3Checker = new Runnable() {
            @Override
            public void run() {
                if(web3Ready()){
                    try {
                        setWeb3();
                        load_credentials(p);
                        credentialsChecker.run();
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                        dhe.handle_error(e);
                    }
                } else {
                    self.web3Loading = true;
                    handler.postDelayed(web3Checker, 500);
                }
            }
        };

        credentialsChecker = new Runnable() {
            @Override
            public void run() {
                if(credentialsReady()){
                    try {
                        setCredentials();
                        self.r.start();
                    } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | NoSuchProviderException | ExecutionException | InterruptedException | IOException e) {
                        e.printStackTrace();
                        dhe.handle_error(e);
                    } catch (CipherException e) {
                        e.printStackTrace();
                        dhe.showMessage("Invalid Password Provided");
                    }
                } else {
                    self.credentialsLoading = true;
                    handler.postDelayed(credentialsChecker, 1000);
                }
            }
        };

        load_web3();
        web3Checker.run();
    }

    private void load_web3(){
        ExecutorService web3Executor = Executors.newFixedThreadPool(1);
        self.web3Future = web3Executor.submit(new Callable<Web3j>(){
            @Override
            public Web3j call (){
                return  Web3jFactory.build(new InfuraHttpService(current_node()));
            }
        });
    }

    private boolean web3Ready(){
        return web3Future.isDone();
    }

    private void setWeb3() throws ExecutionException, InterruptedException {
        self.web3 = web3Future.get();
        self.web3Loaded = true;
        self.web3Loading = false;
    }


    private void load_credentials(final String p){
        ExecutorService credentialsExecutor = Executors.newFixedThreadPool(1);
        self.credentialsFuture = credentialsExecutor.submit(new Callable<Credentials>() {
            @Override
            public Credentials call() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, CipherException, IOException {
                return _load_credentials(p);
            }
        });
    }

    private boolean credentialsReady(){
        return credentialsFuture.isDone();
    }

    private void setCredentials() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, CipherException, IOException, ExecutionException, InterruptedException {
        self.credentials = credentialsFuture.get();
        self.credentialsLoaded = true;
        self.credentialsLoading = false;
    }

    String current_node(){
        Context ca = self.ownerActivity.getApplicationContext();
        SharedPreferences settings = ca.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return settings.getString(DHNameStrings.CHOSEN_NETWORK_NODE_LABEL, "https://rinkeby.infura.io/KQVpBo7jJIBfKQLFg60S");
    }

    @SuppressLint("ApplySharedPref")
    private String set_node(String node_url){
        Context ca = self.ownerActivity.getApplicationContext();
        SharedPreferences settings = ca.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (node_url.equals("current")){
            return settings.getString(DHNameStrings.CHOSEN_NETWORK_NODE_LABEL, "https://rinkeby.infura.io/KQVpBo7jJIBfKQLFg60S");
        } else if(node_url.contains("infura.io")){
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(DHNameStrings.CHOSEN_NETWORK_NODE_LABEL, node_url);
            editor.commit();
            return settings.getString(DHNameStrings.CHOSEN_NETWORK_NODE_LABEL, "https://rinkeby.infura.io/KQVpBo7jJIBfKQLFg60S");
        } else {
            dhe.showMessage(("Currently we only support infura nodes, cannot use : " + node_url + " as node :{"));
            return "false";
        }
    }

    String change_node(String node_url){
        String r = set_node(node_url);
        if (r.equals(node_url)){
            reload_web3();
        }
        return current_node();
    }

    @SuppressLint("ApplySharedPref")
    private Credentials _load_credentials(String pass) throws IOException, CipherException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        Context ca = self.ownerActivity.getApplicationContext();
        String wallet_dir = ca.getApplicationInfo().dataDir;
        SharedPreferences settings = ca.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String wallet_path = settings.getString(DHNameStrings.PATH_TO_WALLET_LABEL, "none");

        if (wallet_path.equals("none")){
            SharedPreferences.Editor editor = settings.edit();
            String wallet_file_name = WalletUtils.generateNewWalletFile(pass, new File(wallet_dir), false);

            wallet_path = wallet_dir + File.separator + wallet_file_name;
            editor.putString("path_to_wallet", wallet_path);
            editor.commit();

            return _load_credentials(pass);
        } else {
            return WalletUtils.loadCredentials(pass, wallet_path);
        }
    }

    void get_accounts(JSONObject data, String res){
        String address = credentials.getAddress();
        r.respond(new String[]{address}, data, res);
    }

    void get_coinbase(JSONObject data, String res) {
        String address = credentials.getAddress();
        r.respond(address, data, res);
    }

    void sign_transaction(JSONArray rpc_params, JSONObject data, String res) {
        if (rpc_params.length() > 0){
            JSONObject payload_params = rpc_params.optJSONObject(0);
            String rpc_method = data.optString("method", "eth_signTransaction");
            validate_transaction(payload_params, rpc_method, data, res);
        } else {
            r.respond("error",  "no transaction to be signed", data, res);
        }
    }

    void send_transaction(JSONArray rpc_params, String rpc_method, JSONObject data, String res){
        if (rpc_params.length() > 0){
            JSONObject payload_params = rpc_params.optJSONObject(0);
            validate_transaction(payload_params, rpc_method, data, res);
        } else {
            r.respond("error",  "no transaction to be sent", data, res);
        }
    }

    void sign(JSONArray rpc_params, JSONObject data, String res){
        String address = rpc_params.optString(0, "0x0");
        if(rpc_params.length() > 0 && !address.equals("0x0")){
            String message = rpc_params.optString(1, "");
            JSONObject extraParams = rpc_params.optJSONObject(2);

            try {
                JSONObject msgParams = new JSONObject(extraParams.toString());
                msgParams.put("from", address);
                msgParams.put("data", message);
                validate_message(msgParams, data, res);

            } catch (JSONException e) {
                r.respond("error",  ("error at Web3resolver during sign: " + e.getMessage()), data, res);
                dhe.handle_error(e);
            }

        } else {
            String err_message ="problem with parameters: "
                    + "address is [" +address + "]"
                    + "parameters length is [" + rpc_params.length() + "]";

            r.respond("error", err_message , data, res);
        }
    }

    private void on_approve_tx(JSONObject txParams, JSONObject query_data, String res){
        synchronized (lock) {
            try {
                finalize_and_submit_tx(txParams, query_data, res);
            } catch (JSONException | ExecutionException | InterruptedException e) {
                dhe.handle_error(e);
            }
        }
    }

    private void on_approve_tx(JSONObject txParams, JSONObject query_data, String res, Boolean signOnly){
        if (!signOnly){
            on_approve_tx(txParams, query_data, res);
        } else {
            synchronized (lock) {
                try {
                    finalize_tx(txParams, query_data, res);
                } catch (JSONException | InterruptedException | ExecutionException e) {
                    dhe.handle_error(e);
                }
            }
        }
    }

    private void on_approve_message(String message, JSONObject query_data, String res){
        Sign.SignatureData signed_message = Sign.signMessage(message.getBytes(), credentials.getEcKeyPair());
        String result = signed_message.toString();
        r.respond(result, query_data, res);
    }

    private void on_cancelled(String res, JSONObject query_data){
        r.respond("error", "Tx cancelled by User", query_data, res);
    }

    private String[] get_accounts(){
        String address = credentials.getAddress();
        return new String[]{address};
    }

    private void validate_message(JSONObject msgParams, JSONObject data, String res) {
        String from = msgParams.optString("from", "0x0");
        if (!from.equals("0x0")) {
            String method = data.optString("method", "eth_sign");
            validate_sender(msgParams, method, data, res);
        } else {
            r.respond("error", "from address not given", data, res);
        }
    }

    private void validate_transaction(JSONObject payload_params, String method, JSONObject data, String res){
        if (payload_params == null){
            r.respond("error", "no parameters", data, res);
        } else {
            String from = payload_params.optString("from", "0x0");
            if (!from.equals("0x0")) {
                validate_sender(payload_params, method, data, res);
            } else {
                r.respond("error", "address not given", data, res);
            }
        }
    }

    private void validate_sender(JSONObject payload_params, String method, JSONObject data, String res){
        String from = payload_params.optString("from", "0x0");
        String[] available = self.get_accounts();
        Boolean isMember = Arrays.asList(available).contains(from);
        if(isMember){
            process(payload_params, method, data, res);
        } else {
            r.respond("error", "sender is unknown could not validate transaction", data, res);
        }
    }

    private void process(JSONObject payload_params, String method, JSONObject data, String requestID) {
        if (method.equals("eth_sendTransaction") || method.equals("eth_signTransaction") || method.equals("eth_sign")) {
            Intent i = new Intent(self.ownerActivity, ApprovalActivity.class);
            i.putExtra(PAYLOAD_PARAMS_LABEL, payload_params.toString());
            i.putExtra(ORIGINAL_DATA_LABEL, data.toString());
            i.putExtra(REQUEST_ID_LABEL, requestID);
            self.ownerActivity.startActivityForResult(i, DHReqCodes.confirmation_req_code());
        }
    }

    public void approval_response(Intent data) throws JSONException {
        String original_data_string = data.getStringExtra(ORIGINAL_DATA_LABEL);
        String request_id = data.getStringExtra(REQUEST_ID_LABEL);
        Boolean cancelled = data.getBooleanExtra(CANCELLED_LABEL, false);

        if (!cancelled) {
            String process_method = data.getStringExtra(RPC_METHOD_LABEL);
            switch (process_method) {
                case "eth_sendTransaction":
                case "eth_signTransaction":
                    String approved_params_string = data.getStringExtra(APPROVED_PARAMS_LABEL);
                    Boolean sign_only = data.getBooleanExtra(SIGNONLY_LABEL, false);
                    on_approve_tx(new JSONObject(approved_params_string), new JSONObject(original_data_string), request_id, sign_only);
                    break;
                case "eth_sign":
                    String approved_message = data.getStringExtra(APPROVED_MESSAGE_LABEL);
                    on_approve_message(approved_message, new JSONObject(original_data_string), request_id);

                    break;
                default:
                    dhe.showMessage("could not recognise the rpc_method");
                    break;
            }
        } else {
            on_cancelled(request_id, new JSONObject(original_data_string));
        }
    }

    private void finalize_tx(JSONObject txParams, JSONObject query_data, String res) throws JSONException, ExecutionException, InterruptedException {
        JSONObject complete_tx_params = fill_in_tx_values(txParams);
        String signed_transaction = sign_transaction(complete_tx_params);
        r.respond(signed_transaction, query_data, res);
    }

    private void finalize_and_submit_tx(JSONObject txParams, JSONObject query_data, String res) throws JSONException, ExecutionException, InterruptedException {
        JSONObject complete_tx_params = fill_in_tx_values(txParams);
        String signed_transaction = sign_transaction(complete_tx_params);
        EthSendTransaction ethSendTransaction = web3.ethSendRawTransaction(signed_transaction).sendAsync().get();
        String txHash = ethSendTransaction.getTransactionHash();
        r.respond(txHash, query_data, res);
    }

    private String sign_transaction(JSONObject txParams) throws JSONException {
        RawTransaction raw_transaction;

        BigInteger nonce = new BigInteger(txParams.get("nonce").toString(), 16);
        BigInteger gas_price = new BigInteger(txParams.get("gasPrice").toString(), 16);
        BigInteger gas_limit = new BigInteger(txParams.get("gas").toString(), 16);
        String to = txParams.get("to").toString();
        BigInteger value = new BigInteger(txParams.get("value").toString(), 16);
        String data = txParams.get("nonce").toString();

        if(txParams.has("value")) {
            raw_transaction = RawTransaction.createTransaction(nonce, gas_price, gas_limit, to, value, data);
        } else {
            raw_transaction = RawTransaction.createTransaction(nonce, gas_price, gas_limit, to, data);
        }

        byte[] signed = TransactionEncoder.signMessage(raw_transaction, credentials);
        return Numeric.toHexString(signed);
    }

    private  JSONObject fill_in_tx_values(JSONObject txParams) throws JSONException, ExecutionException, InterruptedException {
        String gas_price = txParams.optString("gasPrice", "0x0");
        String nonce = txParams.optString("nonce", "0x0");
        String gas = txParams.optString("gas", "0x0");

        HashMap<String, Future> futures = new HashMap<>();

        if (gas_price.equals("0x0")){
            Future received_gas_price = web3.ethGasPrice().sendAsync();
            futures.put("gas_price", received_gas_price);
        }

        if (nonce.equals("0x0")){
            Future transactionCount = web3.ethGetTransactionCount(credentials.getAddress(), DefaultBlockParameterName.PENDING).sendAsync();
            nonce = ((EthGetTransactionCount) transactionCount.get()).getTransactionCount().toString(16);
        }

        if (gas.equals("0x0")){
            JSONObject cloned = clone_tx_params(txParams);
            Transaction transaction = new Transaction(
                    cloned.optString("from"),
                    new BigInteger(cloned.optString("nonce", "0x0")),
                    new BigInteger(cloned.optString("gasPrice")),
                    new BigInteger(cloned.optString("gas")),
                    cloned.optString("to"),
                    new BigInteger(cloned.optString("value")),
                    cloned.optString("data")
            );

            Future estimated_gas = web3.ethEstimateGas(transaction).sendAsync();
            futures.put("gas", estimated_gas);
        }

        if (futures.containsKey("gas")){
            gas = ((EthEstimateGas) futures.get("gas").get()).getAmountUsed().toString();
        }

        if (futures.containsKey("gas_price")){
            gas_price = ((EthGasPrice) futures.get("nonce").get()).getGasPrice().toString(16);
        }

        return txParams
                .put("nonce", nonce)
                .put("gas", gas)
                .put("gas_price", gas_price);
    }

    private JSONObject clone_tx_params(JSONObject txParams) throws JSONException {
        String from = txParams.optString("from", "0x0");
        String to = txParams.optString("to", "0x0");
        String value = txParams.optString("value", "0x0");
        String data = txParams.optString("data", "0x0");
        String gas_price = txParams.optString("gasPrice", "0x0");
        String nonce = txParams.optString("nonce", "0x0");
        String gas = txParams.optString("gas", "0x0");

        JSONObject cloned = new JSONObject();
        cloned.put("from", from);
        cloned.put("to", to);
        cloned.put("value", value);
        cloned.put("data", data);
        cloned.put("gasPrice", gas_price);
        cloned.put("gas", gas);
        cloned.put("nonce", nonce);

        return cloned;
    }
}