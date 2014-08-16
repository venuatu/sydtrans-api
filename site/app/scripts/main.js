/** @jsx React.DOM */

function getRandomHex() {
  return (Math.random() * 0x99 + 0x33 | 0).toString(16);
}

function getRandomColour() {
  return '#'+ getRandomHex() + getRandomHex() + getRandomHex();
}

function getNumKeys(obj) {
  var count = 0;
  for (var i in obj) count++;
  return count;
}

function keys(obj) {
  var arr = [];
  for (var i in obj) arr.push(i);
  return arr;
}

//L.Icon.Default.imagePath = 'images';

var serverAddress = (
  'https://myuow.me/sydtrans/api/'
//  'http://localhost:9001/'
);

var Sidebar = React.createClass({
  clicked: function (name) {
    return function (e) {
      e.preventDefault();
      this.props.drawPolyline(name);
    }.bind(this);
  },
  render: function () {
    var names = _.sortBy(Object.keys(this.props.routes), (name) => name);
    return (
      <ul className="routes">
        {names.map((name, i) =>
          <li key={i} className={this.props.selected === name ? 'selected-route' : ''}>
            <a href={''} onClick={this.clicked(name)}>{name}</a>
          </li>)}
      </ul>
    );
  }
});

var SydTransMap = React.createClass({
  getInitialState: function () {
      return { route: 'SCO', routes: {}, lines: [], stops: [] };
  },
  componentWillMount: function () {
    this.lineCache = {};
    this.onMove = _.debounce(this.onMove, 1000, {leading: true});
  },
  componentDidMount: function () {
    this.onMove();
  },
  drawPolyline: function (name, colour) {
    var bounds;
    var drawnLines = {};
    var lines = [];
    var f = (line, name) => {
      if (drawnLines[line]) return;
      drawnLines[line] = true;
      if (!this.lineCache[line]) {
        this.lineCache[line] = L.Polyline.fromEncoded(line, {
          opacity: 1,
          weight: 4,
          color: getRandomColour(),
        }).bindPopup(name);
      }
      var line = this.lineCache[line];

      if (!bounds) bounds = line.getBounds();
      bounds.extend(line.getBounds());

      line.hash = hashPolyline(line);
      lines.push(line);
    };

    if (!name || name === this.state.routeDrawn) {
      _.each(this.state.routes, f);
      console.log('drew '+ getNumKeys(drawnLines) +' of '+ getNumKeys(this.state.routes) +' routes');
    } else {
      f(this.state.routes[name], name);
    }

    console.log((getNumKeys(this.lineCache) - getNumKeys(drawnLines)) +' unused cached routes');
    _.each(this.lineCache, (value, key) => {
      if (!drawnLines[key])
        delete this.lineCache[key];
    });

    window.lines = lines;
    this.setState({routeDrawn: name || '', lines: lines});
    //this.refs.map.setBounds(bounds);
  },
  onMove: function (center, zoom) {
    var bounds = this.refs.map.getMap().getBounds().toBBoxString();

    this.stopReq && this.stopReq.abort();
    this.stopReq = superagent.get(serverAddress +'stops', {bounds: bounds}, (resp) => {
      var cluster = new L.MarkerClusterGroup();
      var markers = _.chain(resp.body).map((point) =>
          L.marker([point.lat, point.lon], {riseOnHover: true}).bindPopup(point._id +': '+ point.name)).value();
      console.log('got '+ markers.length +' of '+ resp.body.length +' markers')
      markers.forEach(cluster.addLayer.bind(cluster));
      this.setState({
        stops: [cluster],
      });
    });
    
    this.shapeReq && this.shapeReq.abort();
    this.shapeReq = superagent.get(serverAddress +'shapes', {bounds: bounds}, (resp) => {
      this.setState({routes: resp.body});
      this.drawPolyline();
    });
    if (center) {
      localStorage.lastPosition = JSON.stringify({lat: center.lat, lng: center.lng, zoom});
    }
  },
  render: function () {
    var {lat, lng, zoom} = JSON.parse(localStorage.lastPosition || '{}');
    return (
      <div>
        <div>
          <Leaflet ref='map' center={L.latLng(lat || -34.41781, lng || 150.87678)} zoom={zoom || 14} onMove={this.onMove}>
            <Layers layers={this.state.stops} />
            <Layers layers={this.state.lines} />
          </Leaflet>
        </div>
        <div id='legend'>
          <Sidebar routes={this.state.routes} selected={this.state.routeDrawn} drawPolyline={this.drawPolyline} />
        </div>
      </div>
    );
  }
});

React.renderComponent(
  <SydTransMap />,
  document.querySelector('#container')
);


