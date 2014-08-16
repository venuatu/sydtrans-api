/** @jsx React.DOM */
"use strict";

if (typeof window !== 'undefined') {
  var React = React || require('react');
  var L = L || require('leaflet');
}

var utils = (function () {

  function getTime() {
    return window.performance && performance.now() || Date.now();
  }

  var times = {};

  return {
    timeStart: function (str) {
      if (!Leaflet.logging) return;
      times[str] = getTime();
    },
    timeEnd: function (str) {
      if (!Leaflet.logging) return;
      var ret = getTime() - times[str];
      delete times[str];
      return ((ret * 1000) | 0) / 1000;
    },
    log: function () {
      if (!Leaflet.logging) return;
      console.log.apply(console, arguments);
    }
  };
})();

var Leaflet = React.createClass({
  statics: {
    logging: true,
  },
  getDefaultProps: function () {
    if (!L) {
      return {};
    } else {
      return {
        // Sydney AU
        center: L.latLng(-34.41781, 150.87678),
        zoom: 14,
        tileLayers: [
        ],
      };
    }
  },

  componentWillReceiveProps: function (next) {
    var map = this.map;
    if (map) {
      utils.timeStart('mapdraw');
      var children = [];
      React.Children.forEach(next.children, function (layer) {
        layer.type.getLayers && children.push.apply(children, layer.type.getLayers(layer.props));
      });

      utils.timeStart('mapadd');
      var inCurrentSet = {}, count = 0;
      children.forEach(function (layer) {
        if (!map.hasLayer(layer)) {
          map.addLayer(layer);
          layer.reactLayer = true;
          count++;
        }
        inCurrentSet[layer._leaflet_id] = true;
      });
      utils.log('added', count, 'layers in', utils.timeEnd('mapadd'), 'ms ', children.length);
      count = 0;

      utils.timeStart('mapremove');
      map.eachLayer(function (layer) {
        if (layer.reactLayer && !inCurrentSet[layer._leaflet_id]) {
          map.removeLayer(layer);
          count++;
        }
      });
      utils.log('removed', count, 'layers in', utils.timeEnd('mapremove'), 'ms');
      utils.log('mapdraw in ', utils.timeEnd('mapdraw'), 'ms');
    }
  },

  componentDidMount: function () {
    var map = this.map = L.map(this.refs.container.getDOMNode());
    var self = this;
    
    map.setView(this.props.center, this.props.zoom)
      .on('moveend', function () {
        self.maybeFire('onMove', map.getCenter(), map.getZoom());
      })
    ;

    (this.props.tileLayers.length ? this.props.tileLayers : [
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: 'Map data © <a href="http://openstreetmap.org">OpenStreetMap</a> contributors'
      })]).forEach(function (layer) {
      map.addLayer(layer);
    });

    this.componentWillReceiveProps(this);

    // Leaflet seems to have a race condition if layers are added too quickly
    setTimeout(function () {
      var center = map.getCenter();
      center.lng += 1e-8;
      map.setView(center);
    }, 50);
  },

  componentWillUnmount: function () {
    this.map.remove();
  },

  shouldComponentUpdate: function (curr, next) {
    return true;
  },

  maybeFire: function (name) {
    if (typeof this.props[name] === 'function')
      this.props[name].apply(null, Array.prototype.slice.call(arguments, 1));
  },

  getMap: function () {
    return this.map;
  },

  setBounds: function (bounds) {
    this.map.fitBounds(bounds, {animate: true});
  },

  render: function () {
    return (
      <div ref="container" className="leaflet-container">
        {this.props.children}
      </div>
    );
  }
});

var Layers = React.createClass({
  propTypes: {
    layers: React.PropTypes.array,
  },
  statics: {
    getLayers: function (props) {
      var layers = props.layers || [];
      React.Children.forEach(props.children, function (layer) {
        layer.type.getLayers && layers.push.apply(layers, layer.type.getLayers(layer.props));
      });
      return layers;
    },
  },
  render: function () {
    return <span />;
  }
});

var Polyline = React.createClass({
  propTypes: {
    line: React.PropTypes.any.isRequired,
  },
  statics: {
    getLayers: function (props) {
      return [props.line];
    },
  },
  render: function () {
    return <span />;
  }
});

var Marker = React.createClass({
  propTypes: {
    marker: React.PropTypes.any.isRequired,
  },
  statics: {
    getLayers: function (props) {
      return [props.marker];
    },
  },
  render: function () {
    return <span />;
  }
});

function hashPolyline(line) {
  var hash = line.length;
  line._latlngs.forEach(function (latlng) {
    hash = (hash * 31 + (latlng.lat * 1e5 | 0) ^ (latlng.lng * 1e5 | 0)) | 0;
  });
  return hash;
}

if (typeof module !== 'undefined') {
  module.exports = {
    L: L,
    Leaflet: Leaflet,
    Layers: Layers,
    Polyline: Polyline,
    Marker: Marker,
  }
}
