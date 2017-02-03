function getCurrentTemperatures() {
  $.ajax({
      method: "GET",
      url: "temperatures/average",
      dataType: "json"
    })
    .done(function (temperatures) {
      var measurements = temperatures.measurements;
      if (measurements.length == 0)
        $('#temperatures').append('<p>No readings</p>');

        for (i = 0; i < measurements.length; i++) {
          var measurement = measurements[i];
          var sensors = measurement.sensors;
          for (j = 0; j < sensors.length; j++) {
            var sensor = sensors[j];
            var celsius = Math.round(sensor.temperature.celsius * 10) / 10;
            var lastUpdate = moment.unix(measurement.seconds).format('ddd HH:mm a');
            var sensorDescription = ''
            if (sensors.length > 1) {
              sensorDescription = '<div class="updated small">sensor : ' + sensor.name.toLowerCase() + '</div>'
            }

            var html =
                '<div class="temperature">' +
                '<h1><span class="temperature">' + celsius + ' °C</span></h1>' +
                '<p class="source">' + measurement.host + '</p>' +
                 sensorDescription +
                '<div class="updated small">updated : ' + lastUpdate + '</div>' +
                '</div>';
            $('#temperatures').append('' + html);
          }
        }
      })
      .fail(function (jqXHR, textStatus) {
        console.log("Failed to get /temperature/average " + textStatus);
      })
}
