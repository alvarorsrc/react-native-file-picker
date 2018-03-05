# react-native-file-picker

A React Native module that allows you to use native UI to select a file from the device library.

>This is a fork of `callstack/react-native-file-picker`

## Install package

In order to install from the current repository:

```bash
$ yarn add https://github.com/alvarorsrc/react-native-file-picker
```

```bash
$ react-native link react-native-file-picker
```

## Configure native projects

### Android

In order to allow your users select files from within your application, you will have to include relevant android permissions inside your `AndroidManifest.xml` file.

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.myApp">
  <uses-permission android:name="android.permission.CAMERA" />
  <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
  <uses-feature android:name="android.hardware.camera" android:required="true"/>
  <uses-feature android:name="android.hardware.camera.autofocus" />
</manifest>
```

## Usage

### Picking a file

In order to pick any file using native UI, you'll have to import FilePicker module and call `pickFile` method on it.

```js
import FilePicker from 'react-native-file-picker';

//Default values for 'fileType' and 'multiSelect' are ['*/*'] and false respectively.
FilePicker.pickFile({fileType : ['application/xml','application/pdf'], multiSelect: true})
   .then(res => {
     if (!res.cancelled) {
       console.log(res.files);
     } 
   })
   .catch(err => {});
```

Returned promise will be resolved after user either selects a file or dismisses the picker. Otherwise, it will be rejected with an error indicating the failure.

**Note**: `response` the promise resolves with has a `cancelled` boolean you can use to indicate whether file was picked or not.
