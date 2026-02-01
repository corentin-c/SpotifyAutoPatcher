package com.corentinc.patcher

enum class ApplicationSupported(
    var nameToDisplay: String,
    var packageName: String,
    var requireUninstall: Boolean,
    var requireChangePackageNamePatch: Boolean
) {
    SPOTIFY(
        nameToDisplay = "Spotify",
        packageName = "com.spotify.music",
        requireUninstall = true,
        requireChangePackageNamePatch = false
    ),
    YOUTUBE_MUSIC(
        nameToDisplay = "Youtube Music",
        packageName = "com.google.android.apps.youtube.music",
        requireUninstall = false,
        requireChangePackageNamePatch = true
    )
}