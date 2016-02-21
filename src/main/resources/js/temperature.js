function getCurrentTemperatures() {
  $.ajax({
      method: "GET",
      url: "temperatures",
      dataType: "json"
    })
    .done(function (temperatures) {
      var measurements = temperatures.measurements;
      if (measurements.length == 0)
        $('#temperatures').append('<p>No readings</p>');

      for (i = 0; i < measurements.length; i++) {
        var celsius = Math.round(measurements[i].sensors[0].celsius * 10) / 10;
        var host = measurements[i].host;
        var lastUpdate = moment.unix(measurements[i].seconds).format('ddd HH:mm a');
        var html =
            '<div class="temperature">' +
                '<h1><span class="temperature">' + celsius + ' °C</span></h1>' +
                '<p class="source">' + host + '</p>' +
                '<span class="updated small">updated: ' + lastUpdate + '</span>' +
            '</div>';
        $('#temperatures').append('' + html);
      }

    })
    .fail(function (jqXHR, textStatus) {
      console.log("Failed to get /temperature " + textStatus);
    })
}
