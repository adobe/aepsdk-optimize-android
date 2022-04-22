# Adobe Experience Platform Optimize Mobile SDK (Beta)

## Beta version acknowledgment

By using the AEPOptimize SDK (“Beta”), you hereby acknowledge that the Beta is provided “as is” without warranty of any kind. Adobe shall have no obligation to maintain, correct, update, change, modify or otherwise support the Beta. You are advised to use caution and not to rely in any way on the correct functioning or performance of such Beta and/or accompanying materials. 

## About this project
The AEP Mobile Optimize SDK Extension provides APIs to enable real-time personalization workflows in Adobe Experience Platform SDKs using the Edge decisioning services. It depends on the Mobile Core and requires Edge Extension to send personalization query Events to the Experience Edge network.

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

## Documentation
TBD

## Related Projects

| Project                                                      | Description                                                  |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| [AEP SDK Sample App for Android](https://github.com/adobe/aepsdk-sample-app-android) | Contains Android sample app for the AEP SDK. |

## Contributing
Contributions are welcomed! Read the [CONTRIBUTING](.github/CONTRIBUTING.md) for more information.

## Licensing
This project is licensed under the Apache V2 License. See [LICENSE](LICENSE) for more information.