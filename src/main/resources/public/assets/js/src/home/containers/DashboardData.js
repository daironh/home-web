var React = require('react');
var bs = require('react-bootstrap');
var { injectIntl } = require('react-intl');
var { bindActionCreators } = require('redux');
var { connect } = require('react-redux');
var { push } = require('react-router-redux');

var HomeConstants = require('../constants/HomeConstants');
var Dashboard = require('../components/sections/Dashboard');

var HistoryActions = require('../actions/HistoryActions');
var DashboardActions = require('../actions/DashboardActions');

var timeUtil = require('../utils/time');

var { getDeviceByKey, getDeviceNameByKey, getDeviceKeysByType, getDeviceTypeByKey, getAvailableDevices, getAvailableDeviceKeys, getAvailableMeters, getDefaultDevice, getLastSession, reduceMetric, reduceSessions, getDataSessions, getDataMeasurements, getShowersCount } = require('../utils/device');

var { getEnergyClass } = require('../utils/general');
var { getChartDataByFilter } = require('../utils/chart');


function mapStateToProps(state, ownProps) {
  return {
    firstname: state.user.profile.firstname,
    devices: state.user.profile.devices,
    layout: state.section.dashboard.layout,
    //mode: state.section.dashboard.mode,
    tempInfoboxData: state.section.dashboard.tempInfoboxData,
    infoboxes: state.section.dashboard.infobox,
  };
}

function mapDispatchToProps(dispatch) {
  return Object.assign({}, 
                       bindActionCreators(DashboardActions, dispatch),
                       {link: options => dispatch(HistoryActions.linkToHistory(options))}
                      ); 

}

function mergeProps(stateProps, dispatchProps, ownProps) {
  const periods = [
      { id: "day", title: "Day", value: timeUtil.today() }, 
      { id: "week", title: "Week", value: timeUtil.thisWeek() }, 
      { id: "month", title: "Month", value: timeUtil.thisMonth() }, 
      { id: "year", title: "Year", value: timeUtil.thisYear() }
    ];
  return assign(ownProps,
               dispatchProps,
               assign(stateProps,
                      {
                        chartFormatter: intl => (x) => intl.formatTime(x, { hour:'numeric', minute:'numeric'}),
                        infoboxData: transformInfoboxData(stateProps.infoboxes, stateProps.devices, dispatchProps.link),
                        periods,
                        //  types,
                        //   subtypes,
                        //   metrics,
                     }));
}

function assign(...objects) {
  return Object.assign({}, ...objects);
}

function transformInfoboxData (infoboxes, devices, link) {

  return infoboxes.map(infobox => {
    const { id, title, type, time, period, index, deviceType, subtype, data, metric, showerId } = infobox;

    let device, chartData, reducedData, linkToHistory, tip;
    
    if (subtype === 'last') {
      device = infobox.device;
      const last = data.find(d=>d.deviceKey===device);
      const lastShowerMeasurements = getDataMeasurements(devices, last, index);
      
      reducedData = lastShowerMeasurements.map(s=>s[metric]).reduce((c, p)=>c+p, 0);
      if (metric === 'volume') reducedData += ' lt';
      else if (metric === 'energy') reducedData += ' Kwh';
      
      chartData = [{
        title: getDeviceNameByKey(devices, device), 
        data: getChartDataByFilter(lastShowerMeasurements, infobox.metric)
      }];
    
      linkToHistory =  () => link({time, showerId, period, deviceType, device:[device], metric, index, data});
    }
    else if (type==='tip') {
      tip = HomeConstants.STATIC_RECOMMENDATIONS[Math.floor(Math.random()*3)].description;
    }
    else {
      device = getDeviceKeysByType(devices, deviceType);
      
      reducedData = reduceSessions(devices, data)
      .map(s=>s.devType==='METER'?s.difference:s[metric])
      .reduce((c, p)=>c+p, 0); 
      
      if (subtype === 'efficiency') { 
        if (metric === 'energy') {
          reducedData = getEnergyClass(reducedData / getShowersCount(devices, data)); 
        }
      }
      else {
        if (metric === 'volume') reducedData += " lt";
        else if (metric === 'energy') reducedData += ' Kwh';
      }

      chartData = data.map(devData => ({ 
        title: getDeviceNameByKey(devices, devData.deviceKey), 
        data: getChartDataByFilter(getDataSessions(devices, devData), infobox.metric, getDeviceTypeByKey(devices, devData.device)) 
      }));
     
     linkToHistory =  () => link({id, time, period, deviceType, device, metric, index, data});
    }

    return Object.assign({}, 
                       infobox,
                       {
                         device,
                         reducedData,
                         chartData,
                         linkToHistory,
                         tip
                       });
     });
}

var DashboardData = connect(mapStateToProps, mapDispatchToProps, mergeProps)(Dashboard);
DashboardData = injectIntl(DashboardData);
module.exports = DashboardData;
