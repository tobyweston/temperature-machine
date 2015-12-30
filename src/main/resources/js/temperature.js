function getCurrentTemperature() {
  $.ajax({
        method: "GET",
        url: "temperature"
      })
      .done(function (temperature) {
        $('#current-temperature').text(temperature);
      })
      .fail(function (jqXHR, textStatus) {
        console.log("Failed to get /temperature " + textStatus);
      })
}
