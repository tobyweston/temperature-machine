function getCurrentTemperatures() {
  $.ajax({
      method: "GET",
      url: "temperatures",
      dataType: "json"
    })
    .done(function (temperatures) {
      var measurements = temperatures.measurements;
      var celsius = Math.round(measurements[0].sensors[0].celsius * 10) / 10;
      $('.temperature').text(celsius + " °C");
      $('.source').text(measurements[0].host);
      $('.updated').text(moment.unix(measurements[0].seconds).format('ddd HH:mm a'));
    })
    .fail(function (jqXHR, textStatus) {
      console.log("Failed to get /temperature " + textStatus);
    })
}
