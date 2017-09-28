package com.ecmdapps.distro.providerserver;


import org.json.JSONObject;

interface Responder {
    void respond(String result, JSONObject query_data, String requestID);
    void respond(String[] result, JSONObject query_data, String requestID);
    void respond(String result_type, JSONObject error, JSONObject query_data, String requestID);
    void respond(String result_type, String error, JSONObject query_data, String requestID);
    void start();
}
