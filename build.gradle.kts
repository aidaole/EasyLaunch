// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
}

// 添加JitPack发布相关的任务
tasks.register("cleanBuildPublish") {
    description = "清理并构建发布AAR"
    group = "publishing"
    
    dependsOn(":easylaunch:clean")
    dependsOn(":easylaunch:assembleRelease")
    dependsOn(":easylaunch:publishReleasePublicationToMavenLocal")
    
    tasks.findByPath(":easylaunch:clean")?.let { cleanTask ->
        tasks.findByPath(":easylaunch:assembleRelease")?.mustRunAfter(cleanTask)
    }
    
    tasks.findByPath(":easylaunch:assembleRelease")?.let { assembleTask ->
        tasks.findByPath(":easylaunch:publishReleasePublicationToMavenLocal")?.mustRunAfter(assembleTask)
    }
}