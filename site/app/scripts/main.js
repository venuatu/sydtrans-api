/** @jsx React.DOM */
'use strict';

function fetch() {
  return httpinvoke.apply(null, arguments).then(function (data) {
    data.body = JSON.parse(data.body);
    return data;
  });
}

function getRandomHex() {
  return (Math.random() * 0x99 + 0x33 | 0).toString(16);
}

function getRandomColour() {
  return '#'+ getRandomHex() + getRandomHex() + getRandomHex();
}

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
      return { route: 'SCO', routes: {} };
  },
  componentWillMount: function () {
      this.map = L.map('themap').setView([-34.40802, 150.87774], 13);
      this.map.addLayer(L.tileLayer('http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
          attribution: 'Map data © <a href="http://openstreetmap.org">OpenStreetMap</a> contributors',
          minZoom: 8
      }));
      this.fetchRoutes(this.state.route);
  },
  drawPolyline: function (name, colour) {
    this.clearMap();
    var bounds;
    var drawnLines = {};
    var f = (line, name) => {
      if (drawnLines[line]) return;
      drawnLines[line] = true;
      var line = L.Polyline.fromEncoded(line, {
        color: getRandomColour(),
        opacity: 1,
        weight: 3,
      }).bindPopup(name);

      if (!bounds) bounds = line.getBounds();
      bounds.extend(line.getBounds());

      this.map.addLayer(line);
    };

    if (!name || name === this.state.routeDrawn) {
      _.each(this.state.routes, f);
      console.log('drew '+ Object.keys(drawnLines).length +' of '+ Object.keys(this.state.routes).length +' routes');
    } else {
      f(this.state.routes[name], name);
    }

    this.map.fitBounds(bounds, {animate: true});
    this.setState({routeDrawn: name});
  },
  fetchRoutes: function (name) {
    fetch(serverAddress +'shapes/?filter='+ encodeURIComponent(name)).then((data) => {
      this.setState({routes: data.body});
      var keys = Object.keys(data.body);
      var picked = keys[Math.random() * keys.length | 0];
      var bounds;
      this.clearMap();
      this.drawPolyline();
    });
  },
  clearMap: function () {
    _.each(this.map._layers, (layer) => {
        if(layer._path != undefined) {
            try {
                this.map.removeLayer(layer);
            } catch(e) {
                console.log("problem with " + e + layer);
            }
        }
    })
  },
  handleSearch: function (e) {
    e.preventDefault();
    this.fetchRoutes(this.refs.searchbox.getDOMNode().value);
  },
  render: function () {
    return (
      <div>
        <form onSubmit={this.handleSearch}>
          <div className='input-group'>
            <input className='form-control' type='search' ref='searchbox' defaultValue={this.state.route} />
            <span className='input-group-btn'>
              <button className='btn btn-primary' type='submit' onClick={this.routeChange}><i className='glyphicon glyphicon-search'></i></button>
            </span>
          </div>
        </form>
        <Sidebar routes={this.state.routes} selected={this.state.routeDrawn} drawPolyline={this.drawPolyline} />
      </div>
    );
  }
});

React.renderComponent(<SydTransMap></SydTransMap>, document.querySelector('#legend'));
