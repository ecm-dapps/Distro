package com.ecmdapps.distro.providerserver;


import android.app.Activity;

import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpPost;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.koushikdutta.async.http.body.JSONObjectBody;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

class ProviderServer implements Responder {
    private AsyncHttpServer server;
    private AsyncServer asyncServer;

    private DHErrorHandler dhe;
    private Web3Resolver resolver;

    private HashMap<String, AsyncHttpServerResponse> responses;

    ProviderServer(Activity ownerActivity, Web3Resolver resolver) {
        this.dhe = new DHErrorHandler(ownerActivity, "ProviderServer");
        this.server = new AsyncHttpServer();
        this.asyncServer = new AsyncServer();
        this.resolver = resolver;
        this.responses = new HashMap<>();
    }

    public void start(){
        server.addAction("OPTIONS", ".*", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                response.getHeaders().set("Access-Control-Allow-Methods", "POST, GET, DELETE, PUT, OPTIONS");
                response.getHeaders().set("Access-Control-Allow-Origin", "*");
                response.getHeaders().set("Access-Control-Allow-Headers", "Origin, Content-Type, X-Auth-Token");
                response.send("application/json", "");
            }
        });

        server.get("/", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                response.getHeaders().set("Access-Control-Allow-Methods", "POST, GET, DELETE, PUT, OPTIONS");
                response.getHeaders().set("Access-Control-Allow-Origin", "*");
                response.getHeaders().set("Access-Control-Allow-Headers", "Origin, Content-Type, X-Auth-Token");
                response.send("Dapphall server");
            }
        });

        server.post("/", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                response.getHeaders().set("Access-Control-Allow-Methods", "POST, GET, DELETE, PUT, OPTIONS");
                response.getHeaders().set("Access-Control-Allow-Origin", "*");
                response.getHeaders().set("Access-Control-Allow-Headers", "Origin, Content-Type, X-Auth-Token");
                String requestUUID = UUID.randomUUID().toString();
                responses.put(requestUUID, response);
                if (request.getBody() instanceof JSONObjectBody) {
                    JSONObject json_data = ((JSONObjectBody)request.getBody()).get();
                    try {
                        handle_request(json_data, requestUUID);
                    } catch (Exception e){
                        dhe.handle_error(e);
                    }
                } else {
                    respond("error", "DistroHall could not parse your request, It must be JSON", new JSONObject(), requestUUID);
                }
            }
        });

        server.listen(asyncServer, 8545);
    }

    private void handle_request(JSONObject data, String requestID){
        JSONObject error = new JSONObject();
        try {
            String rpc_method  = data.getString("method");
            JSONArray rpc_params = data.getJSONArray("params");

            if (rpc_method.equals("eth_coinbase")) {
                resolver.get_coinbase(data, requestID);

            } else if (rpc_method.equals("dapphall_ping")) {
                respond("dapphall_pong", data, requestID);

            } else if (rpc_method.equals("eth_accounts")) {
                resolver.get_accounts(data, requestID);

            } else if (rpc_method.equals("eth_sendTransaction")) {
                resolver.send_transaction(rpc_params, rpc_method, data, requestID);

            } else if (rpc_method.equals("eth_signTransaction")) {
                resolver.sign_transaction(rpc_params, data, requestID);

            } else if (rpc_method.equals("eth_sign")) {
                resolver.sign(rpc_params, data, requestID);

            } else if (rpc_method.equals("personal_listAccounts")) {
                resolver.get_accounts(data, requestID);

            } else if (rpc_method.equals("eth_newFilter")) {
                error.put("code", -32601);
                error.put("message", "DistroHall's provider platform does not support this method");
                respond("error", error, data, requestID);

            } else if (rpc_method.equals("eth_newBlockFilter")) {
                error.put("code", -32601);
                error.put("message", "DistroHall's provider platform does not support this method");
                respond("error", error, data, requestID);

            } else if (rpc_method.equals("eth_newPendingTransactionFilter")) {
                error.put("code", -32601);
                error.put("message", "DistroHall's provider platform does not support this method");
                respond("error", error, data, requestID);

            } else if (rpc_method.equals("eth_uninstallFilter")) {
                error.put("code", -32601);
                error.put("message", "DistroHall's provider platform does not support this method");
                respond("error", error, data, requestID);

            } else if (rpc_method.equals("eth_getFilterChanges")) {
                error.put("code", -32601);
                error.put("message", "DistroHall's provider platform does not support this method");
                respond("error", error, data, requestID);

            } else if (rpc_method.equals("eth_newFilter")) {
                error.put("code", -32601);
                error.put("message", "DistroHall's provider platform does not support this method");
                respond("error", error, data, requestID);

            } else if (rpc_method.equals("personal_sign")) {
                error.put("code", -32601);
                error.put("message", "DistroHall's provider platform does not support personal_sign");
                respond("error", error, data, requestID);

            } else if (rpc_method.equals("personal_ecRecover")) {
                error.put("code", -32601);
                error.put("message", "DistroHall's provider platform does not support personal_ecRecover");
                respond("error", error, data, requestID);

            } else if ( !rpc_method.startsWith("eth_") && !rpc_method.startsWith("net_") && !rpc_method.startsWith("web3_")) {
                respond("", data, requestID);

            } else {
                forward(data, requestID);
            }

        } catch (JSONException e) {
            respond("error", "could not read JSON", data, requestID);
        }
    }

    private void forward(final JSONObject data, final String requestID){
        String connected_node = resolver.current_node();
        AsyncHttpPost post = new AsyncHttpPost(connected_node);
        JSONObjectBody json_body = new JSONObjectBody(data);
        post.setBody(json_body);

        AsyncHttpClient client = new AsyncHttpClient(asyncServer);
        client.executeJSONObject(post, new AsyncHttpClient.JSONObjectCallback() {
            @Override
            public void onCompleted(Exception e, AsyncHttpResponse source, JSONObject result) {
                if (e != null){
                    String message = "from : " + data.toString() + "\n" + "response : " + e.toString();
                    dhe.showMessage(message);
                } else {
                    send_response(result, requestID);
                }
            }
        });
    }

    public void respond(String[] result, JSONObject query_data, String requestID) {
        ArrayList<String> string_list = new ArrayList<>(Arrays.asList(result));
        try {
            JSONArray array_result = new JSONArray();
            array_result.put(string_list);

            JSONObject jsonResponse = new JSONObject();

            jsonResponse.put("id", query_data.getInt("id"));
            jsonResponse.put("jsonrpc",  query_data.getString("jsonrpc"));
            jsonResponse.put("result", array_result);

            send_response(jsonResponse, requestID);

        } catch (JSONException e) {
            dhe.handle_error(e);
        }
    }

    public void respond(String result, JSONObject query_data, String requestID) {
        try {
            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("id", query_data.getInt("id"));
            jsonResponse.put("jsonrpc",  query_data.getString("jsonrpc"));
            jsonResponse.put("result", result);
            send_response(jsonResponse, requestID);

        } catch (JSONException e) {
            dhe.handle_error(e);
        }
    }

    public void respond(String result_type, JSONObject error, JSONObject query_data, String requestID) {
        try {
            JSONObject jsonResponse = new JSONObject();

            jsonResponse.put("id", query_data.optInt("id", -1));
            jsonResponse.put("jsonrpc",  query_data.optString("jsonrpc", "2.0"));
            jsonResponse.put(result_type, error);

            send_response(jsonResponse, requestID);

        } catch (JSONException e) {
            dhe.handle_error(e);
        }
    }

    public void respond(String result_type, String error, JSONObject query_data, String requestID) {
        try {
            JSONObject jsonResponse = new JSONObject();
            JSONObject errorObject = new JSONObject();

            jsonResponse.put("id", query_data.optInt("id", -1));
            jsonResponse.put("jsonrpc",  query_data.optString("jsonrpc", "2.0"));
            errorObject.put("code", -32600);
            errorObject.put("message", error);
            jsonResponse.put(result_type, errorObject);

            send_response(jsonResponse, requestID);

        } catch (JSONException e) {
            dhe.handle_error(e);
        }
    }

    private void send_response(JSONObject jsonResponse, String requestID){
        if (responses.containsKey(requestID)) {
            AsyncHttpServerResponse response = responses.get(requestID);
            response.send(jsonResponse);
            responses.remove(requestID);
        }
    }
}
