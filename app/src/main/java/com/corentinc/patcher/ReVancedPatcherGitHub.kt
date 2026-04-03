package com.corentinc.patcher

data class GitLabRelease(
    val tag_name: String,
    val assets: GitLabAssets
)

data class GitLabAssets(
    val links: List<GitLabLink>
)

data class GitLabLink(
    val name: String,
    val direct_asset_url: String 
)