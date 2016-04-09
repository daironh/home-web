var adminAPI = require('../api/admin');
var types = require('../constants/ActionTypes');

var requestedActivity = function() {
  return {
    type : types.ADMIN_REQUESTED_ACTIVITY
  };
};

var receivedActivity = function(success, errors, activity) {
  return {
    type : types.ADMIN_RECEIVED_ACTIVITY,
    success : success,
    errors : errors,
    activity : activity
  };
};

var requestedSessions = function(username) {
  return {
    type : types.ADMIN_REQUESTED_SESSIONS,
    username : username
  };
};

var receivedSessions = function(success, errors, devices) {
  return {
    type : types.ADMIN_RECEIVED_SESSIONS,
    success : success,
    errors : errors,
    devices : devices
  };
};

var requestedMeters = function(username) {
  return {
    type : types.ADMIN_REQUESTED_METERS,
    username : username
  };
};

var receivedMeters = function(success, errors, meters) {
  return {
    type : types.ADMIN_RECEIVED_METERS,
    success : success,
    errors : errors,
    meters : meters
  };
};

var resetUserData = function() {
  return {
    type : types.ADMIN_RESET_USER_DATA
  };
};

var AdminActions = {
  getActivity : function() {
    return function(dispatch, getState) {
      dispatch(requestedActivity());

      return adminAPI.getActivity().then(function(response) {
        dispatch(receivedActivity(response.success, response.errors, response.accounts));
      }, function(error) {
        dispatch(receivedActivity(false, error, null));
      });
    };
  },

  getSessions : function(userKey, username) {
    return function(dispatch, getState) {
      dispatch(requestedSessions(username));

      return adminAPI.getSessions(userKey).then(function(response) {
        dispatch(receivedSessions(response.success, response.errors, response.devices));
      }, function(error) {
        dispatch(receivedSessions(false, error, null));
      });
    };
  },

  getMeters : function(userKey, username) {
    return function(dispatch, getState) {
      dispatch(requestedMeters(username));

      return adminAPI.getMeters(userKey).then(function(response) {
        dispatch(receivedMeters(response.success, response.errors, response.series));
      }, function(error) {
        dispatch(receivedMeters(false, error, null));
      });
    };
  },
  
  resetUserData : function() {
    return resetUserData();
  },

  setFilter : function(filter) {
    return {
      type : types.ADMIN_FILTER_USER,
      filter : filter
    };
  }
};

module.exports = AdminActions;