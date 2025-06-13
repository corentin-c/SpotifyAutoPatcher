# Spotify Auto Patcher

Android app to patch Spotify combining the powers of AntiSplit-M and ReVanced Manager !

[Main screen](images/mainscreen.png)

## Usage

- Uninstall your patched version of Spotify if you already have one
- Make sure you have a stable internet connection
- Install Spotify FROM THE GOOGLE PLAY STORE
- Launch Spotify Auto Patcher
- Click on "Start"
- Wait for the processing to finish
- Uninstall the Spotify version of the play store
- Go back to the app and click on the "Install" button (down arrow)
- Make sure to disable auto updates from the Google play store
- Enjoy !

Whats it does : 
- Extract an APK from the UNPATCHED Spotify app installed FROM THE GOOGLE PLAY STORE of the phone
- Retrieve and download the latest ReVanced Patches
- Apply all existing patches for Spotify
- Rebuild the APK
- Allow you to install the APK after you uninstalled the store version

But this app :
- Cannot patch anything else than Spotify
- Cannot patch an already patched version of Spotify
- Cannot patch a version of Spotify installed from an APK
- Doesn't allow you to choose which patches are applied
- Doesn't allow you to save the patched APK
- Cannot uninstall Spotify before you install the patched version, you have to do it manually

## Used projects

⭐  forked from [AntiSplit-M](https://github.com/AbdurazaaqMohammed/AntiSplit-M) by AbdurazaaqMohammed to extract the APK
* [ReVanced Patcher](https://github.com/ReVanced/revanced-patcher) to patch the app 
* [ReVanced Patches](https://github.com/ReVanced/revanced-patches) to get the patches for Spotifysure 
* [ReVanced Library](https://github.com/ReVanced/revanced-library) to apply the patches and sign the APK

## Permissions

* QUERY_ALL_PACKAGES - to extract the APK from the installed Spotify app
* REQUEST_INSTALL_PACKAGES - to show an install button allowing prompt to install an app after merging it
* Internet permission - to retrieve the latest revanced patches
