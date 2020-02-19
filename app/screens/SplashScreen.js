import * as React from 'react';
import { View, Text } from 'react-native';

function SplashScreen(props) {
  return (
    <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center' }}>
      <Text onPress={()=>{props.navigation.navigate("Home")}}>Click Me</Text>
    </View>
  );
}

export default SplashScreen;