var React = require('react');
var bs = require('react-bootstrap');
var classNames = require('classnames');

var intl = require('react-intl');
var { injectIntl, FormattedMessage, FormattedRelative } = require('react-intl');

var { Responsive, WidthProvider } = require('react-grid-layout');
var ResponsiveGridLayout = WidthProvider(Responsive);
const { reduxForm } = require('redux-form');

var PureRenderMixin = require('react-addons-pure-render-mixin');

var MainSection = require('../layout/MainSection');

var ChartBox = require('../helpers/ChartBox');

const { IMAGES } = require('../../constants/HomeConstants');


var timeUtil = require('../../utils/time');

function ErrorDisplay (props) {
  return props.errors ? 
    <div style={{ zIndex: 100}}>
      <img src={`${IMAGES}/alert.svg`} /><span className="infobox-error">{`${props.errors}`}</span>
    </div>
    :
     (<div/>);
}


/* Be Polite, greet user */
function SayHello (props) {
  return (
    <div style={{margin: '40px 30px 20px 30px'}}>
      <h3><FormattedMessage id="dashboard.hello" values={{name:props.firstname}} /></h3>
    </div>
  );
}

function InfoBox (props) {
  const { mode, infobox, updateInfobox, removeInfobox, intl } = props;
  const { id, error, period, type, display, linkToHistory, periods, displays, time } = infobox;
  
  const _t = intl.formatMessage;
  return (
    <div className='infobox'>
      <div className='infobox-header'>
        <div className='header-left'>
          <h4>{infobox.title}</h4>
        </div>

        <div className='header-right'>
          <div style={{marginRight:10}}>
            {
              displays.map(t => t.id!==display?(
                <a key={t.id} onClick={() => updateInfobox(id, {display:t.id})} style={{marginLeft:5}}>{t.title}</a>
                ):<span key={t}/>)
            }
          </div>
          
          <div>
            {
              periods.map(p => (
                <a key={p.id} onClick={() => updateInfobox(id, {period:p.id})} style={{marginLeft:5}}>{(p.id===period)?(<u>{_t({id: p.title})}</u>):(_t({id: p.title}))}</a>
                ))
            }
          </div>
          {
            (() => type === 'last' && time ? 
             <FormattedRelative value={time} /> : <span/>
             )()
          }
          {
            //TODO: disable delete infobox until add is created
               <a className='infobox-x' style={{float: 'right', marginLeft: 5, marginRight:5}} onClick={()=>removeInfobox(infobox.id)}><i className="fa fa-times"></i></a>
          }
        </div>
      </div>
      
      <div className='infobox-body'>
         {
           (()=>{
             if (error) {
               return (<ErrorDisplay errors={error} />);
               } 
             else {
               if (display==='stat') {
                 return (
                   <StatBox {...props} /> 
                 );
               } 
               else if (display==='chart') {
                 return (
                   <ChartBox {...props} /> 
                   );
               }
               else if (display==='tip') {
                 return (
                   <TipBox {...props} />
                   );
               }
             }
           })()
         }
       </div>
       <div className='infobox-footer'>
          <a onClick={linkToHistory}>See more</a>
       </div>
    </div>
  );
}

function StatBox (props) {
  const { id, title, type, improved, data, highlight, metric, measurements, period, device, deviceDetails, index, time, better, comparePercentage, mu } = props.infobox;
  let improvedDiv = <div/>;
  if (improved === true) {
    improvedDiv = (<img src={`${IMAGES}/success.svg`}/>);
  }
  else if (improved === false) {
    improvedDiv = (<img src={`${IMAGES}/warning.svg`}/>);
  }
  const duration = data?(Array.isArray(data)?null:data.duration):null;
  const arrowClass = better?"fa-arrow-down green":"fa-arrow-up red";
  const bow = (better==null || comparePercentage == null) ? false : true;
  return (
    <div>
      <div style={{float: 'left', width: '50%'}}>
        <h2>{highlight}<span style={{fontSize:'0.5em', marginLeft:5}}>{mu}</span></h2>
      </div>
      <div style={{float: 'left', width: '50%'}}>
        <div>
          {
            (() => bow ? 
             <span><i className={`fa ${arrowClass}`}/>{better ? `${comparePercentage}% better than last ${period} so far!` : `${comparePercentage}% worse than last ${period} so far`}</span>
             :
               <span>No data</span>
               )()
          }
        </div>
      </div>
    </div>
  );
}

function TipBox (props) {
  const { title, type, highlight } = props.infobox;
  return (
    <div >
      <p>{highlight}</p>
    </div>
  );
}

function InfoPanel (props) {
  const { mode, layout, infoboxes, updateLayout, switchMode,  updateInfobox, removeInfobox, chartFormatter, intl, periods, displays } = props;

  return (
    <div>
      <ResponsiveGridLayout 
        className='layout'
        layouts={{lg:layout}}
        breakpoints={{lg:1370, md: 900, sm: 600, xs: 480, xxs: 200}}
        rowHeight={160}
        cols={{lg:8, md: 6, sm: 4, xs: 2, xxs: 1}}
        draggableHandle='.infobox-header'
        isDraggable={true}
        isResizable={false}
        onLayoutChange={(layout, layouts) => { 
          //updateLayout(layout);  
        }}
        onResizeStop={(layout, oldItem, newItem, placeholder) => { 
          updateLayout(layout);  
        }}
        onDragStop={(layout) => {
          updateLayout(layout); 
        }}
       >
       {
         infoboxes.map(function(infobox) {
           return (
             <div key={infobox.id}>
               <InfoBox {...{mode, periods, displays, chartFormatter, infobox, updateInfobox, removeInfobox, intl}} /> 
           </div>
           );
         })
       }
      </ResponsiveGridLayout>
     </div>
  );
}

function ButtonToolbar (props) {
  const { switchMode, saveToProfile, mode } = props;
  return (
      <bs.ButtonToolbar style={{float: 'right', marginRight: 20}}>
        <bs.Button onClick={()=> switchMode("add")} active={false}>Add</bs.Button>
        {
          (()=> mode==="save"?(
            <bs.Button onClick={()=> saveToProfile()} bsStyle="primary" active={false}>Save</bs.Button>
            ):(
            null
            ))()
        }
      </bs.ButtonToolbar>
  );
}

function AddInfoboxForm (props) {
  const {fields: {title, type, metric, deviceType}, metrics, types, deviceTypes} = props;
  return (
    <form>
      
      <bs.Tabs className="history-time-nav" position='top' tabWidth={3} activeKey={deviceType.value} onSelect={(key) => deviceType.onChange(key)}>
               {
                deviceTypes.map(devType =>
                            <bs.Tab key={devType.id} eventKey={devType.id} title={devType.title} />)
               } 
       </bs.Tabs>

      
      <div className="add-infobox">
        <div className="add-infobox-left">
          
          <div>
            <label>Type</label>
            <select
              {...type}
              value={type.value}>
              {
                types.map((type, idx) =>
                  <option key={idx} value={type.id}>{type.title}</option>
                )
              }
            </select>
          </div>    
        </div>

        <div className="add-infobox-right">
          <div>
            <input type='text' placeholder='Enter title...' {...title}/>
          </div>
          
        </div>
      </div>
       
  </form>
  );
}

const AddInfobox = reduxForm({
    form: 'addInfobox',
    fields: ['title', 'type', 'deviceType'],
    initialValues: {deviceType: 'AMPHIRO', type: 'totalVolume'}
})(AddInfoboxForm);


function AddInfoboxModal (props) {
  const { showModal, switchMode, addInfobox, metrics, types, deviceTypes, initialValues } = props;
  return (
    <bs.Modal animation={false} className='add-infobox-modal' show={showModal} onHide={() => switchMode('normal')} bsSize="large" backdrop='static'>
        <bs.Modal.Header closeButton>
          <bs.Modal.Title>
            <FormattedMessage id="dashboard.add" />
          </bs.Modal.Title>
        </bs.Modal.Header>
        <bs.Modal.Body>
          
          <AddInfobox {...{metrics, types, deviceTypes}}/> 

        </bs.Modal.Body>
        <bs.Modal.Footer>
          <a onClick={() => switchMode('normal')}>Cancel</a>
          <a style={{marginLeft: 20}} onClick={() => { addInfobox(); switchMode('normal');}}>Add</a>
        </bs.Modal.Footer>
      </bs.Modal> 

  );
}

var Dashboard = React.createClass({
  mixins: [ PureRenderMixin ],

  componentWillMount: function() {
    const { fetchAllInfoboxesData, switchMode } = this.props;
    //switchMode("normal");
    fetchAllInfoboxesData();

  },
  /*
  componentWillReceiveProps: function(nextProps) {
    console.log('history receiving props');   
    //console.log(nextProps);
    //console.log(this.props);
    for (let key in nextProps) {
      let prop = nextProps[key];
      if (typeof prop === 'function') { continue; }
      if (this.props[key] === prop) { continue; }
      console.log('new', key, prop);
      console.log('old', key, this.props[key]);
    }

   
  },
  */
  render: function() {
    const { firstname, mode, switchMode, addInfobox, saveToProfile,  amphiros, meters, metrics , types, deviceTypes, initialValues } = this.props;
    return (
      <MainSection id="section.dashboard">
        <SayHello firstname={firstname} />
        <div className='pull-right'>
          <ButtonToolbar {...{switchMode, saveToProfile, mode}}/>
        </div>
        <AddInfoboxModal {...{showModal:mode==='add', switchMode, addInfobox, metrics, types, deviceTypes, initialValues}}/>
        
        <InfoPanel {...this.props} />
        
      </MainSection>
    );
  }
});

//Dashboard = injectIntl(Dashboard);
module.exports = Dashboard;
