$(document).ready(function(){
    FastClick.attach(document.body);

    $('.collapsible').collapsible();
    $('select').material_select();

    $("#content-div").hide();
    $("#loader-div").hide();

    var logger = document.getElementById("log");
    setConsole("log", logger);
    setConsole("error", logger);
    setConsole("warn", logger);
    setConsole("debug", logger);
    setConsole("info", logger);

    console.log(new Date().toString());

    try {
        $("#starter-btn").on("mousedown touchstart", function(){
            startWeb3Provider(function(response){
                console.log(response);
            }, logResponse);
        });
        checker();
        setInterval(checker, 2500);
        $("#log_toggler").on("mousedown touchend", function(){
            $(".distro_logs").toggle();
        });
    } catch (e){
        console.log(e);
    }
});