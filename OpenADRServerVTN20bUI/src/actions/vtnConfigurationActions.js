import * as types from '../constants/actionTypes';

export const loadVtnConfiguration = () => {
  return (dispatch, getState) => {
      dispatch({
        type    : types.LOAD_VTN_CONFIGURATION
        , swagger: function(api) {
           api.apis["oadr-20b-vtn-controller"].viewConfUsingGET( {responseContentType: 'application/json'})
           	.then(data => {
           		var vtn = JSON.parse(data.data);
	            dispatch({
	              type    : types.LOAD_VTN_CONFIGURATION_SUCCESS
	              , payload: vtn
	            });
           	})
           	.catch(err => {
           		dispatch({
	              type    : types.LOAD_VTN_CONFIGURATION_ERROR
	              , payload: err
	            });
           	})
        }
    })
  }
}

export const loadMarketContext = () => {
  return (dispatch, getState) => {
      dispatch({
        type    : types.LOAD_MARKET_CONTEXT
        , swagger: function(api) {
           api.apis["market-context-controller"].listMarketContextUsingGET( {responseContentType: 'application/json'})
            .then(data => {
              var marketContext = JSON.parse(data.data);
              dispatch({
                type    : types.LOAD_MARKET_CONTEXT_SUCCESS
                , payload: marketContext
              });
            })
            .catch(err => {
              dispatch({
                type    : types.LOAD_MARKET_CONTEXT_ERROR
                , payload: err
              });
            })
        }
    })
  }
}

export const createMarketContext = (marketContext) => {
  return (dispatch, getState) => {
      dispatch({
        type    : types.CREATE_MARKET_CONTEXT
        , swagger: function(api) {
           api.apis["market-context-controller"].createMarketContextUsingPOST({dto:marketContext}, {responseContentType: 'application/json'})
            .then(data => {
              var marketContext = JSON.parse(data.data);
              
              dispatch({
                type    : types.CREATE_MARKET_CONTEXT_SUCCESS
                , payload: marketContext
              });
              loadMarketContext()(dispatch, getState);
            })
            .catch(err => {
              dispatch({
                type    : types.CREATE_MARKET_CONTEXT_ERROR
                , payload: err
              });
            })
        }
    })
  }
}