'use strict'
const Device = use ('App/Models/Device')
const Recipient = use ('App/Models/Recipient')


var socketList
class SocketController {

  constructor({ socket, request }) {
    this.socket = socket
    this.request = request
    console.log('[DEVICE] Connected :' + socket.id)
  }

  onMessage(message) {
    console.log(message)

  }

  async alert(message){
    console.log(message)
  }

  onClose(socket) {
    console.log('[DEVICE] Disconnected :' + socket.id)
  }

  sendSMS() {

  }

  async onRegister(data) {
    console.log('[SYS] Registering User')
    console.log(data)
    try{
      var udata = await Recipient.query().where('sms_no', '=', data.sms).fetch()
      if(udata.toJSON().length==0){
        var nudata = new Recipient()
        nudata.sms_no = data.sms
        nudata.name = data.name
        nudata.save()

        console.log('[SYS] User Saved in the Database.')
        this.socket.emit('registered','',this.socket.id)
      }else{
        console.log('[SYS] User is Already in the Database.')
        this.socket.emit('registered','',this.socket.id)
      }
    } catch (e){
      console.log(e)
    }
  }

  async onRegisterDevice(data){
    console.log('[SYS][SENSOR] Sensor Connected.')
    console.log(data)
    try{
      var ddata = await Device.query().where('name', '=', data.name).fetch()
      if(ddata.toJSON().length==0){
        var nddata = new Device()
        nddata.name = data.name
        nddata.save()

        console.log('[SYS][SENSOR] Sensor Added to the Database')
      }else[
        console.log('[SYS][SENSOR] Device already Exists in the database.')
      ]
    } catch (e){
      console.log(e)
    }
  }

  alertApp(device_id, message, level) {

  }

}

module.exports = SocketController
