function setConsole(verb, logger) {
    var oldVerb = "old"+verb;
    console[oldVerb] = console[verb];
    console[verb] = function () {
        var output = "", arg, i;
        for (i = 0; i < arguments.length; i++) {
            arg = arguments[i];
            output += "<span class=\"log-" + (typeof arg) + "\">";
            if (
                typeof arg === "object" &&
                typeof JSON === "object" &&
                typeof JSON.stringify === "function"
                ) {
                    output += JSON.stringify(arg);
            } else {
                output += arg;
            }
            output += "</span>&nbsp;";
        }
        logger.innerHTML += output + "<br>";
        console[oldVerb].apply(undefined, arguments);
    };
}

var startWeb3Provider = function(successCB, errorCB){
     var startMmt = new Date().toISOString();
     var resp = distro.start(startMmt);
     if (resp === startMmt){
        successCB(resp);
     } else {
        console.log("this should never happen");
        errorCB("could not start");
     }
 };

 var isWeb3ProviderReady = function(successCB, errorCB){
     var ready = distro.ready();
     if (ready){
        successCB(ready);
     } else {
        errorCB("could not check for a reason");
     }
 };

 var changeNode = function(new_url, successCB, errorCB){
     var return_url = distro.change_network(new_url);
     if (return_url){
        successCB(return_url);
     } else {
        errorCB("could not chaeck or change node");
     }
 };

 var getLog = function(lines, successCB){
    var stLines = "" + lines;
    var logs = distro.get_logs(stLines);
    if(logs){
        successCB(logs);
    }
 }

 var statusChanged = function(){
    if (window.status == "yes"){
        $("#starter-div").hide();
        $("#loader-div").show();
        $("#content-div").hide();

        window.web3 = new Web3(new Web3.providers.HttpProvider("http://localhost:8545"));
        window.web3.eth.getCoinbase(function(error, result){
            if (error){
                console.log(error);
            } else {
                window.coinbase = result;
                $(".coinbase").val(window.coinbase);
                $("#coinbase").text(window.coinbase);
                $("#starter-div").hide();
                $("#loader-div").hide();
                $("#content-div").show();
                Materialize.updateTextFields();
                $("#submit_send").on("mousedown touchstart", handleSendEth);
                $("#contract_submit_send").on("mousedown touchstart", handleContractEth);
                $("#go_button").on("mousedown touchstart", handleGo);
                $(".navigate_button").on("mousedown touchstart", handleNavigation);
                $("#change_network").on("mousedown touchstart", handleNetworkChange);
                getBalance();
                poll_for_balance();
            }
        });
    } else if (window.status == "loading"){
        $("#starter-div").hide();
        $("#loader-div").show();
        $("#content-div").hide();
    } else if (window.status == "not started"){
        $("#starter-div").show();
        $("#loader-div").hide();
        $("#content-div").hide();
    }
 }

 var checker = function(){
     isWeb3ProviderReady(
         function(response){
            if (window.status != response){
                window.status = response;
                statusChanged();
            }
         },
         function (error){
            console.log(error);
         }
     )

     changeNode("current",
        function (response){
            var network_string = "Connected on: " +  networkFromResponse(response);
            $("#current_network").text(network_string);
        },
        function(error){
            console.log(error);
        }
     )

     getLog(1000, function(_logs) {
        $("#native_logs").text(_logs);
     });
 };

 var logResponse = function(response){
    console.log(response);
 }

 var handleSendEth = function (e){
    e.preventDefault();
    console.log("submitting send operation");
    var fromAddr = document.getElementById('coinbase_from').value;
    var toAddr = document.getElementById('to_address').value;
    var valueEth = document.getElementById('eth_amount').value;
    var value = parseFloat(valueEth)*1.0e18;
    var gasPriceGWEI = document.getElementById('gas_price').value
    var gasPrice = parseFloat(gasPriceGWEI)*1.0e9;
    var gas = document.getElementById('gas_limit').value;
    var txData = {from: fromAddr, to: toAddr, value: value, gasPrice: gasPrice, gas: gas};
    console.log(txData);
    web3.eth.sendTransaction(txData, function (err, txhash) {
        if (error){
            console.log('error: ' + err);
        } else {
            storeRes("transaction", txHash);
        }
    });
 }

 var handleContractEth = function(e){
    e.preventDefault();
    console.log("submitting contract operation");
    var fromAddr = document.getElementById('contract_sender_coinbase').value;
    var contractAddr = document.getElementById('contract_address').value;
    var abi = JSON.parse(document.getElementById('contract_abi').value);
    var contract = web3.eth.contract(abi).at(contractAddr);
    var functionName = document.getElementById('contract_function').value;
    var args = JSON.parse('[' + document.getElementById('contract_function_args').value + ']');
    var valueEth = document.getElementById('contract_eth_amount').value;
    var value = parseFloat(valueEth)*1.0e18;
    var gasPriceGWEI = document.getElementById('gas_price').value;
    var gasPrice = parseFloat(gasPriceGWEI)*1.0e9;
    var gas = document.getElementById('gas_limit').value;
    args.push({from: fromAddr, value: value, gasPrice: gasPrice, gas: gas})
    var callback = function(err, txhash) {
        if (error){
                console.log('error: ' + err);
            } else {
                storeRes("transaction", txHash);
            }
    }
    args.push(callback)
    contract[functionName].apply(this, args)
 }

 function isURL(str) {
   var pattern = new RegExp('^(https?:\\/\\/)?'+ // protocol
   '((([a-z\\d]([a-z\\d-]*[a-z\\d])*)\\.)+[a-z]{2,}|'+ // domain name and extension
   '((\\d{1,3}\\.){3}\\d{1,3}))'+ // OR ip (v4) address
   '(\\:\\d+)?'+ // port
   '(\\/[-a-z\\d%_.~+&:]*)*'+ // path
   '(\\?[;&a-z\\d%_.,~+&:=-]*)?'+ // query string
   '(\\#[-a-z\\d_]*)?$','i'); // fragment locator
   return pattern.test(str);
 }

 function replaceAll(str, find, replace) {
    return str.replace(new RegExp(escapeRegExp(find), 'g'), replace);
 }

 function poll_for_balance(){
    setInterval(getBalance, 30000);
 }

 function getBalance(){
     web3.eth.getBalance(window.coinbase, function(err, response){
         if (err){
             console.log(err);
         } else {
             $("#balance").text(response);
         }
     });
 }

 var handleGo = function(){
    var where = $("#go_to").val();
    if (isURL(where)){
        if (where.startsWith("http")){
            var goref = where
        } else {
            var goref = "http://" + where;
        }
        window.location.href = goref;
        var toastContent = "Loading " + where + " ! Please wait...";
        Materialize.toast(toastContent, 20000);
    } else {
        var query = where.replace(/\s/g, '+');;
        window.location.href = "https://duckduckgo.com/?q=" + query;
        var toastContent = "Searching for " + where + ". Please wait...";
        Materialize.toast(toastContent, 20000);
    }
 }

 function handleNavigation(e){
        window.location.href = e.currentTarget.href;
        var toastContent = "Loading " + e.currentTarget.title + ". Please wait...";
        Materialize.toast(toastContent, 20000);
 }

 function handleNetworkChange(e){
    var selected_network = $("#network_selector").val();
    changeNode(selected_network, networkChangeSuccess, networkChangeError);
 }

 function networkChangeSuccess(response){
    $("#current_network").text(networkFromResponse(response));
 }

 function networkChangeError(error){
    console.log(error);
 }

 function networkFromResponse(response){
    var node_names = {
        "https://mainnet.infura.io/KQVpBo7jJIBfKQLFg60S":"mainnet on infura",
        "https://ropsten.infura.io/KQVpBo7jJIBfKQLFg60S":"ropsten on infura",
        "https://rinkeby.infura.io/KQVpBo7jJIBfKQLFg60S":"rinkeby on infura",
        "false": "No network connected"
    }
    return node_names[response];
 }

 function storeRes(action, storeAble){
     if (action === "transaction"){
         storeTx(storeAble);
     }
 }

 function storeTx(txHash){
     db = new PouchDB("distro");
     db.put({
         id: txHash,
         type: "transaction",
         done: false
     }).catch(function(err){
         console.log(err);
     });
 }