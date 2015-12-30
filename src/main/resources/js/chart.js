var options = {
  chart: {
    renderTo: 'chart-img',
    type: 'spline'
  },
  title: {
    text: 'Temperatures (last 24h)'
  },
  subtitle: {
    text: ''
  },
  colors: ['#4572A7', '#AA4643', '#89A54E', '#80699B', '#3D96AE', '#DB843D', '#92A8CD', '#A47D7C', '#B5CA92'],
  xAxis: {
    type: 'datetime',
    dateTimeLabelFormats: {
      hour: '%H. %M'
    }
  },
  yAxis: {
    title: {
      text: 'Temperature (°C)'
    }
  },
  tooltip: {
    formatter: function () {
      return '<b>' + this.series.name + '</b><br/>' + Highcharts.dateFormat('%H:%M', this.x) + ' <strong>' + this.y.toFixed(1) + ' °C</strong>';
    }
  },

  plotOptions: {
    series: {
      marker: {
        radius: 2
      }
    }
  },

  lineWidth: 1,

  series: []
};

function generateChart(options) {
  $.ajax({
    type: "GET",
    url: "temperature.xml",
    dataType: "xml",
    success: function (xml) {
      var series = [];

      //define series
      $(xml).find("name").each(function () {
        var seriesOptions = {
          name: $(this).text(),
          data: []
        };
        options.series.push(seriesOptions);
      });

      //populate with data
      $(xml).find("row").each(function () {
        var timestamp = parseInt($(this).find("timestamp").text()) * 1000;

        $(this).find("values").each(function (index) {
          var values = parseFloat($(this).text());
          values = values || null;
          if (values != null) {
            options.series[index].data.push([timestamp, values])
          }
        });
      });

      $.each(series, function (index) {
        options.series.push(series[index]);
      });
      options.chart = new Highcharts.Chart(options);
    }
  })
}