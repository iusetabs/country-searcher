/*
   @Author James Nolan - Ireland
   @Date 21.04.2019
   @Info javascript/jquery client code
*/

$(document).ready(function() {
  setListeners(); //setListeners on the inputfields and any buttons.
});

function setListeners(){
  $("#countryInput").on("change paste keyup", function() { //Activate function if any action on input field
    if($("#countryInput").val()!="") //Make sure nothing is sent to server if input field is empty
      sendToServer($("#countryInput").val());
    else
      $(".dropdown-divider").empty(); //Ensure the dropdown is empty if input field is empty
  });
}

function sendToServer(fieldVal){
  $.ajax({
      url: 'http://127.0.0.1:8080/countries', //REST server running on localhost
      dataType: 'json',
      type: 'post',
      contentType: 'application/json',
      data: fieldVal,
      processData: false,
      timeout: 5000, // sets timeout to 3 seconds
      success: function( data, textStatus, jQxhr ){
          fillDropDown(data);
      },
      error: function( jqXhr, textStatus, errorThrown ){
          console.log( errorThrown );
          if(textStatus=="timeout"){
            $("#info").clear();
            $("#info").append("Server timed out. Please retry");
          }
      }
  });
}

/*
  @INFO Function fills dynmaic drop downs with country names
  @INFO Country names are ordered by the server
*/
function fillDropDown(countryNamesArray){
  $(".dropdown-divider").empty(); //Clear the html from the dropdown field
  countryNamesArray.forEach(function (name){ //Iteratre through server response
    var html = '<a class="dropdown-item">' + name + '</a>';
    $("#dropdown").append(html);
  });
}
