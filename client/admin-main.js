const LOC_URL = "http://api.ipstack.com/check?access_key=a1d5abe0fd6709ed6ee80744cc29def2";
var USER_LOC = null;
$(document).ready(function() {
  console.log("test");
  setListeners();
  getUsersLocation();
});

function setListeners(){
  $("#countryInput").on("change paste keyup", function() {
    console.log($( "#countryInput" ).val());
    $("#testField div:nth-child(1)").empty();
    $("#testField div:nth-child(1)").append($("#countryInput").val());
    sendToServer($("#countryInput").val());
  });
}

function getUsersLocation(){
  var jqxhr = $.get(LOC_URL, function(data) {
})
  .done(function(data) {
    console.log(data);
    convertToUser(data);
    $("#testField div:nth-child(2)").append(data);
  })
  .fail(function(data) {
  })
  .always(function(data) {
  });
}

function convertToUser(data){
  var USER_LOC = data;
}

function sendToServer(fieldVal){
  $.ajax({
             url: "localhost:8080/countries",
             type: "POST",
             crossDomain: true,
             data: JSON.stringify(fieldVal),
             dataType: "json",
             success: function (response) {
                 var resp = JSON.parse(response)
                 alert(resp.status);
             },
             error: function (xhr, status) {
                 alert("error");
             }
         });
}
