import React, { Component } from 'react';
import { View, Text, NativeEventEmitter, AppState } from 'react-native';
import RNStepCounterModule from '../native_modules/RNStepCounterModule';

class HomeScreen extends Component {
    constructor(props) {
        super(props)
        this.nativeEventEmitter;
        this.state = {
            stepsCounter: "0 STEPS",
            appState: AppState.currentState,
            pastStepsCount: 0
        }
    }

    componentDidMount() {
        //native calls
        this.nativeEventEmitter = new NativeEventEmitter(RNStepCounterModule);
        this.nativeEventEmitter.addListener('StepReminder', (event) => {
            console.log(event.eventProperty) // "someValue"
            this.setState({
                stepsCounter: event.stepCount
            })
        })

        RNStepCounterModule.initStepper()

        AppState.addEventListener('change', this._handleAppStateChange);

        // this.focusListener = navigation.addListener("focus", () => {
        //     RNStepCounterModule.onStart()
        // });
        // this.blurListener = navigation.addListener("blur", () => {
        //     // console.log("onBlur called")
        //     RNStepCounterModule.onStop()
        // });
        // this.list = [
        //     this.props.navigation.addListener('focus', () => {
        //         alert("focus")
        //     })
        // ];
    }

    componentWillUnmount() {
        // Remove the event listener
        // this.list.forEach( item => item.remove() )
        // const eventEmitter = new NativeEventEmitter(RNStepCounterModule);
        // this.nativeEventEmitter.removeListener('StepReminder', null)
        AppState.removeEventListener('change', this._handleAppStateChange);

        //native calls
        RNStepCounterModule.onStop()
        RNStepCounterModule.onDestroy()
    }

    _handleAppStateChange = (nextAppState) => {
        this.setState({ appState: nextAppState });

        if (nextAppState === 'background') {
            console.log("App is in Background Mode.")
            RNStepCounterModule.onStart()
        }

        if (nextAppState === 'active') {
            console.log("App is in Active Foreground Mode.")
            RNStepCounterModule.onStop()
        }

        if (nextAppState === 'inactive') {
            console.log("App is in inactive Mode.")
        }
    };

    pastSteps = (data) => {
        console.log("SKK", data)
        this.setState({
            pastStepsCount: data
        })
    }

    errorPastSteps = (data) => {
        console.log("SKK", data)
    }

    render() {
        return (
            <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center' }}>
                <Text>{this.state.stepsCounter}</Text>
                <Text onPress={() => {
                    RNStepCounterModule.startStepper()
                }}>Start Stepper</Text>

                <Text onPress={() => {
                    RNStepCounterModule.stopStepper()
                }}>Stop Stepper</Text>

                <Text>{"Past Steps: " + this.state.pastStepsCount}</Text>

                <Text onPress={() => {
                    RNStepCounterModule.getPastTime(4, this.pastSteps, this.errorPastSteps)
                }}>Get Past Date Step Count</Text>
            </View>
        );
    }
}

export default HomeScreen;