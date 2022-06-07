# Adobe Experience Platform Optimize Mobile SDK

## About this project
The AEP Optimize mobile SDK Extension provides APIs to enable real-time personalization workflows in the Adobe Experience Platform SDKs using Adobe Target or Adobe Journey Optimizer Offer Decisioning. It depends on the Mobile Core and requires Edge Extension to send personalization query events to the Experience Edge network.

## Installation
Integrate the Optimize extension into your app by including the following lines in your gradle file's `dependencies`:

```
implementation 'com.adobe.marketing.mobile:optimize:1.+'
implementation 'com.adobe.marketing.mobile:edge:1.+'
implementation 'com.adobe.marketing.mobile:core:1.+'
```

### Development

**Open the project**

To open and run the project, open the `code/build.gradle` file in Android Studio

**Run demo application**
Once you open the project in Android Studio (see above), select the `app` runnable and your favorite emulator and run the program.

## Related Projects

| Project                                                      | Description                                                  |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| [AEP SDK Sample App for Android](https://github.com/adobe/aepsdk-sample-app-android) | Contains Android sample app for the AEP SDK. |

## Contributing
Contributions are welcomed! Read the [CONTRIBUTING](.github/CONTRIBUTING.md) for more information.

## Licensing
This project is licensed under the Apache V2 License. See [LICENSE](LICENSE) for more information.