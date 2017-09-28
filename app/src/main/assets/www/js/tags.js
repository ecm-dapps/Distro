riot.tag2('dhall', '<div class="navbar-fixed"> <nav> <div class="nav-wrapper"> <a href="#!" class="brand-logo center">Distrohall</a> <ul class="right hide-on-med-and-down"> </ul> </div> </nav> </div> <dhallsetup if="{!dhallReady}"></dhallSetup> <dhallpage if="{dhallReady}"></dhallPage>', '', '', function(opts) {
        var self = this;
        self.dhallReady = false;
        self.status = "no";

        self.on("mount", function(){
             startChecker();
        })

        function startChecker(){
            setInterval(daanevChecker, 5000);
        }

        function daanevChecker(){
            isWeb3ProviderReady(returnReady, returnError);
        }

        function returnReady(response){
            if (response == "yes"){
                if (self.status != "yes"){
                    self.status = "yes";
                    self.dhallReady = true;
                    self.update();
                }
            }

        }

        function returnError(error){
            console.log(error);
        }

});

riot.tag2('dhallpage', '<div class="row"> <div class="col s12"> <ul id="tabs-content" class="tabs"> <li class="tab col s3"><a href="#dapps">D-Apps</a></li> <li class="tab col s3"><a href="#wallet">My Wallet</a></li> <li class="tab col s3"><a href="#send">To User</a></li> <li class="tab col s3"><a href="#contract">To Contract</a></li> </ul> <div id="dapps" class="col s12"> <div class="center-align"> <h3>Distributed Apps</h3> <div each="{dapps}" class="col s12"> <h4> {title} </h4> <p> {description}<p> <a class="btn waves-effect waves-light blue extern" href="{link}"> Open {title} </a> </div> </div> </div> <div id="wallet" class="col s12"> <div class="col s3"> <h6 id="coinbase_label">coinbase:</h6> </div> <div class="col s9"> <b id="coinbase" class="coinbase_header"></b> </div> </div> <div id="send" class="col s12"> <div class="row"> <form class="col s12" name="sendEth" onsubmit="{sendEth}"> <div class="row"> <div class="input-field col s12"> <input id="coinbase_from" class="coinbase" type="text" disabled> <label class="active" for="coinbase_from">From</label> </div> </div> <div class="row"> <div class="input-field col s12"> <input id="to_address" type="text"> <label class="active" for="to_address">To</label> </div> </div> <div class="row"> <div class="input-field col s12"> <input id="eth_amount" step="0.0001" type="number"> <label for="eth_amount">Amount in ETH</label> </div> </div> <div class="row"> <div class="input-field col s12"> <input id="gas_limit" value="50000" type="number"> <label for="gas_limit" class="active">Gas Limit</label> </div> </div> <div class="row"> <div class="input-field col s12"> <input id="gas_price" value="10" type="number"> <label for="gas_price" class="active"> Gas Price in GWEI </label> </div> </div> <div class="row"> <div class="input-field col s12"> <input id="submit_send" type="submit"> </div> </div> </form> </div> </div> <div id="contract" class="col s12"> <div class="row"> <form class="col s12" name="contractEth" onsubmit="{contractSubmit}"> <div class="row"> <div class="input-field col s12"> <input id="contract_sender_coinbase" class="coinbase" type="text"> <label class="active" for="contract_sender_coinbase">From</label> </div> </div> <div class="row"> <div class="input-field col s12"> <input id="contract_address" type="text"> <label class="active" for="contract_address">Contract Address</label> </div> </div> <div class="row"> <div class="input-field col s12"> <input id="contract_abi" type="text"> <label class="active" for="contract_abi">Contract ABI</label> </div> </div> <div class="row"> <div class="input-field col s12"> <input id="contract_function" type="text"> <label class="active" for="contract_function">Contract Function</label> </div> </div> <div class="row"> <div class="input-field col s12"> <input id="contract_function_args" type="text"> <label class="active" for="contract_function_args">Contract Function Args</label> </div> </div> <div class="row"> <div class="input-field col s12"> <input id="contract_eth_amount" type="number"> <label for="contract_eth_amount">Value in ETH</label> </div> </div> <div class="row"> <div class="input-field col s12"> <input id="contract_gas_limit" value="3141592" type="number"> <label for="contract_gas_limit" class="active">Gas Limit</label> </div> </div> <div class="row"> <div class="input-field col s12"> <input id="contract_gas_price" value="10" type="number"> <label for="contract_gas_price" class="active"> Gas Price in GWEI </label> </div> </div> <div class="row"> <div class="input-field col s12"> <input id="contract_submit_send" type="submit"> </div> </div> </form> </div> </div> </div> </div>', 'dhallpage .carousel .carousel-item,[data-is="dhallpage"] .carousel .carousel-item{ height: 500px; }', '', function(opts) {
        var self = this;

        self.dapps = [
            {"title": "etheroll", "description": "ethereum gambling application", "link":"http://etheroll.com"},
            {"title": "ethlance", "description": "ethereum personnel market", "link":"http://ethlance.com"},
            {"title": "ethercasts", "description": "ethereum dapps listing", "link":"http://daaps.ethercasts.com"},
        ];

        function setupPage(){
            web3 = new Web3(new Web3.providers.HttpProvider("http://localhost:8545"));
            web3.eth.getCoinbase(function(error, result){
                if (error){
                    console.log(error);
                } else {
                    console.log("result", result);
                    $("#coinbase").text(result);
                    $(".coinbase").val(result);
                    $("#log").hide();

                    $("a.extern").on("mousedown touchstart", function (e){
                        var link = e.currentTarget.href;
                        window.location.href = link;
                        var message = " loading " + link + " please wait ";
                        Materialize.toast(message, 20000);
                    });

                    Materialize.updateTextFields();
                }
                console.log(web3);

            });
        }

        self.on('mount', function(){
            $('ul.tabs').tabs({ swipeable: true });
            Materialize.updateTextFields();
            setupPage();
            FastClick.attach(document.body);
        });

        this.sendEth = function(e){
            e.preventDefault();
                var fromAddr = document.getElementById('coinbase_from').value;
                var toAddr = document.getElementById('to_address').value;
                var valueEth = document.getElementById('eth_amount').value;
                var value = parseFloat(valueEth)*1.0e18;
                var gasPriceGWEI = document.getElementById('gas_price').value
                var gasPrice = parseFloat(gasPriceGWEI)*1.0e9;
                var gas = document.getElementById('gas_limit').value;i
                web3.eth.sendTransaction({from: fromAddr, to: toAddr, value: value, gasPrice: gasPrice, gas: gas}, function (err, txhash) {
                    if (error){
                        console.log('error: ' + err);
                    } else {
                        storeRes("transaction", txHash);
                    }
                });
        }.bind(this)

        this.contractSubmit = function(e){
            e.preventDefault();
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
        }.bind(this)

        function storeRes(action, storeAble){
            if (action === "transaction"){
                storeTx(storeAble);
            }
        }

        function storeTx(txHash){
            db = new PouchDB("distrohall");
            db.put({
                id: txHash,
                type: "transaction",
                done: false,

            }).catch(function(err){
                console.log(err);
            });
        }
});

riot.tag2('dhallsetup', '<h3 class="center">{status}</h3> <p class="center-align"> <b> mainnet on infura </b> </p> <div if="{network_selector_available}" class="row"> <form class="col s12"> <div class="row"> <div class="input-field col s12"> <select onchange="{networkChange}" id="network_selector"> <option value="" disabled selected>Choose a network, (default is Mainnet)</option> <option value="https://mainnet.infura.io/KQVpBo7jJIBfKQLFg60S">mainnet on infura</option> <option value="https://rinkeby.infura.io/KQVpBo7jJIBfKQLFg60S">rinkeby on infura</option> <option value="https://ropsten.infura.io/KQVpBo7jJIBfKQLFg60S">testnet on infura</option> </select> <label>Change Network</label> </div> </div> </form> </div> <p class="center-align"> <a if="{status == \'not started\'}" onclick="{launch}" class="waves-effect waves-light btn-large">Press to Launch Web3</a> </p> <div if="{status == \'loading\'}" class="progress"> <div class="indeterminate"></div> </div>', '', '', function(opts) {
        var self = this;
        self.status = "not started";
        self.current_node = "checking";
        self.network_selector_available = false;

        self.node_names = {
            "checking": "checking",
            "https://mainnet.infura.io/KQVpBo7jJIBfKQLFg60S": "mainnet on infura",
            "https://rinkeby.infura.io/KQVpBo7jJIBfKQLFg60S": "rinkeby on infura",
            "https://ropsten.infura.io/KQVpBo7jJIBfKQLFg60S": "ropsten on infura"
        }

        self.on('mount', function(){
            $('select').material_select();
        });

        this.launch = function(e){
            e.preventDefault();
            startWeb3Provider(
                function(response){

                    self.status = "loading";
                    self.update();
                },
                function (error){console.log(error);}
            );
        }.bind(this)

        this.networkChange = function(e){
            e.preventDefault();
            var selected_network = $("#network_selector").val();
            console.log(selected_network);
            changeNode(selected_network, nodeResponse, returnError);
        }.bind(this)

        function nodeResponse(response){
            if (self.current_node != response){
                self.current_node = response;
                self.update();
            }
        }

        function returnError(error){
            console.log(error);
        }
});
