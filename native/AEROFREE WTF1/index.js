import React from "react";
import {
  AppRegistry,
  StyleSheet,
  Text,
  View,
  NativeModules,
  Button
} from "react-native";
import Ws from "@adonisjs/websocket-client";
var ws;
var stream;

class HelloWorld extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      connected: false,
      devices: [],
      ip: "",
      sms: ""
    };
  }

  componentDidMount() {
    this.getIP();
  }

  showToast = () => {
    NativeModules.reactCallBack.event("This is a Message!");
  };

  confirmRegister = () => {
    NativeModules.reactCallBack.confirmRegister();
  };

  testIntent = () => {
    this.notify(
      "1",
      "SENSOR_1",
      "7.0864614",
      "125.6158246",
      "250",
      "180",
      "110",
      "112",
      "113",
      "118",
      "1"
    );
  };
  notify = ({ id, device_name, lat, lng, ppm_lpg, status, updated }) => {
    console.log({id, device_name, lat, lng, ppm_lpg, status})
    console.log("attempt notify");
    NativeModules.reactCallBack.alert(
      id + "",
      device_name + "",
      lat+ "",
      lng + "",
      ppm_lpg + "",
      status + "",
      updated + ""
    );
  };

  getIP = () => {
    NativeModules.reactCallBack.getSettings(ip => {
      //console.log("Received IP:"+ip)
      this.setState({
        ip: ip
      });
      //NativeModules.reactCallBack.event(ip)
      ws = Ws("ws://" + ip + ":3333");

      ws.connect();
      stream = ws.subscribe("socket");

      //Connection Listeners
      stream.on("open", () => {
        try {
          stream.getSubscription("socket");
        } catch (e) {
          stream.subscribe("socket");
        }
        this.setState({ connected: true });
        NativeModules.reactCallBack.event("Connected to the Websocket Server!");
      });

      stream.on("ready", () => {
        NativeModules.reactCallBack.event("Connected to the Websocket Server!");
        // stream.emit('register', {
        //   sms: sms,
        //   name: name
        // })
      });

      stream.on("error", data => {
        NativeModules.reactCallBack.event(data);
        console.log(data);
      });

      stream.on("registered", () => {
        NativeModules.reactCallBack.event("--- Already Registered. ---");
        this.confirmRegister();
      });

      stream.on("monitor", data => {
        console.log("[MONITOR] Received Data");
        console.log('----------->',data);
        console.log("WTF");
        this.notify({
          id: data.id,
          device_name: data.device_name,
          lat: data.lat,
          lng: data.lng,
          ppm_lpg: data.LPG,
          status: data.status,
          updated: data.updated
        });
      });

      stream.on("close", b => {
        this.setState({ connected: false });
        NativeModules.reactCallBack.event(
          "----Disconnected from the Websocket Server!----"
        );
        console.log(b);
      });
      //Event Listeners
      stream.on("alert", data => {
        this.notify(
          "1",
          "SENSOR_1",
          "7.0864614",
          "125.6158246",
          "250",
          "180",
          "110",
          "112",
          "113",
          "118",
          "1"
        );
      });
    });
  };

  render() {
    console.log("Test");
    return (
      <View style={styles.container}>
        <Text style={styles.hello}>R-Native Status:</Text>
        {this.state.connected == true && (
          <Text style={styles.hello}>Connected</Text>
        )}
        {this.state.connected == false && (
          <Text style={styles.hello}>Disconnected</Text>
        )}
        <Button title="Hit Me" onPress={this.showToast} />
        <Button title="Get IP" onPress={this.getIP} />
        <Button title="Confirm Register" onPress={this.confirmRegister} />
        <Button title="Test Intent Call" onPress={this.testIntent} />
        {/* <Text>{JSON.stringify(NativeModules)}</Text> */}
      </View>
    );
  }
}
var styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: "center"
  },
  hello: {
    fontSize: 20,
    textAlign: "center",
    margin: 10
  }
});

AppRegistry.registerComponent("MyReactNativeApp", () => HelloWorld);
