'use strict'

/*
|--------------------------------------------------------------------------
| Websocket
|--------------------------------------------------------------------------
|
| This file is used to register websocket channels and start the Ws server.
| Learn more about same in the official documentation.
| https://adonisjs.com/docs/websocket
|
| For middleware, do check `wsKernel.js` file.
|
*/

const accountSid = 'AC62c99237ad66667bed1bb0dc4bf59c2e';
const authToken = 'cb0516708b75936b4012b323172a05e7';
let _ = require('lodash')
const clientTwilio = require('twilio')(accountSid, authToken);

const Ws = use('Ws')
const Server = use('Server')
const Device = use('App/Models/Device')
const Recipient = use('App/Models/Recipient')
const Smser = use('Smser')
// const io = use('socket.io')(Server.getInstance())
var mosca = require('mosca');
var mqtt = require('mqtt');
var client = mqtt.connect('mqtt://192.168.43.217');

var triggered = false;
var triggerList = [];
var sent = 0
var sentSafe = 0
var devListCondition = [];

console.log('wtf')
var settings = {
  port: 1883
}

var server = new mosca.Server(settings);
server.on('ready', () => {
  console.log("[MOSCA] Server ready");
});

client.on('connect', function (data, ) {
  console.log('[MOSCA][DEVICE] A sensor device has connected.')
  client.subscribe('mytopic/test')
})

client.on('message', async function (topic, message) {

  var context = message.toString();
  // console.log('raw:' + context)
  message = JSON.parse(context)
  // console.log(message)

  var devData = await Device.query().where('name', '=', message.id).fetch()
  if (devData.toJSON().length == 0) {
    devData = new Device()
    devData.name = message.id
    if (devData.triggered != null) {
      devData.ppm_lpg = message.LPG
      devData.ppm_co2 = message.CO
      devData.ppm_smoke = message.SMOKE
      devData.raw_lpg = message.LPG_RAW
      devData.raw_co2 = message.CO_RAW
      devData.raw_smoke = message.SMOKE_RAW
      devData.triggered = message.trigger
      devData.lat = message.lat
      devData.lng = message.lng
    }
    await devData.save()
    console.log('Device Added - ' + devData.name)
  } else {
    devData = await Device.find(devData.toJSON()[0].id)
    devData.ppm_lpg = message.LPG
    devData.ppm_co2 = message.CO
    devData.ppm_smoke = message.SMOKE
    devData.raw_lpg = message.LPG_RAW
    devData.raw_co2 = message.CO_RAW
    devData.raw_smoke = message.SMOKE_RAW
    devData.triggered = message.trigger
    devData.lat = message.lat
    devData.lng = message.lng
    await devData.save()
  }
  var broadcastData = devData.toJSON()
  try {
    // console.log(broadcastData)
    // console.log('[WS]Broadcasting')
    if (message.status == 'disconnected') {
      broadcastData.status = 'Disconnected'
    } else {
      broadcastData.status = 'Connected'
    }
    // console.log(broadcastData)

    if (broadcastData.ppm_co2 != undefined) {
      if (broadcastData.triggered == 0) {
        var sensorName = broadcastData.name
        if (sent != 1) {
          var recips = await Recipient.query().where("to_text", '=', true).fetch()
          recips = recips.toJSON()
          recips.map(async (entry, index) => {
            console.log(entry)
            var condition = ""
            if (broadcastData.ppm_lpg > 200 || broadcastData.ppm_co2 > 200 || broadcastData.ppm_smoke > 200) {
              condition = "Dangerous Levels"
            } else {
              condition = "Mild Levels"
            }
            console.log('Sent: '+condition);
            // await clientTwilio.messages
            //   .create({
            //     body: 'Detected '+condition+' of Gas from Sensor:'+sensorName+". Please proceed with caution.",
            //     from: '+12024105812',
            //     to: entry.sms_no
            //   })
            //   .then(message => console.log(message.sid));
          })
          sent = 1;
          devListCondition.push({id:broadcastData.name, sent:false});
        }
      }else{
        let p = _.findIndex(devListCondition, { id: broadcastData.name })
        if(p!=-1){
          if(devListCondition[p].sent == false)
          console.log('Safe Now')
            // await clientTwilio.messages
            //   .create({
            //     body: 'Detected '+condition+' of Gas from Sensor:'+sensorName+". Please proceed with caution.",
            //     from: '+12024105812',
            //     to: entry.sms_no
            //   })
            //   .then(message => console.log(message.sid));
          devListCondition[p].sent=true
        }
      }
      if (broadcastData.ppm_lpg > 800)
      broadcastData.ppm_lpg = 800
      if (broadcastData.ppm_co2 > 800)
      broadcastData.ppm_co2 = 800
      if (broadcastData.ppm_smoke > 800)
      broadcastData.ppm_smoke = 800

      if (broadcastData.raw_lpg > 800)
      broadcastData.raw_lpg = 800
      if (broadcastData.raw_co2 > 800)
      broadcastData.raw_co2 = 800
      if (broadcastData.raw_smoke > 800)
      broadcastData.raw_smoke = 800


      Ws
        .getChannel('socket')
        .topic('socket')
        .broadcast('monitor', broadcastData)

        // console.log(broadcastData)
      // console.log('[WS]Sent')
    } else {
      console.log('Skipping')
    }


  } catch (e) {
    //console.log(e)
    // console.log('[WS] None Listening.')
  }

})

Ws.channel('socket', 'SocketController')

// io.on('connection', function (socket) {
//   console.log('[SOCKETIO] - an user connected:' + socket.id);
//   socket.on('disconnect', function (socket) {
//     console.log('user disconnected:' + socket.id);
//   });
// });
