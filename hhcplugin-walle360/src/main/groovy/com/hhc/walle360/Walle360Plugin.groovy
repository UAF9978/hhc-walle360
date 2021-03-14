package com.hhc.walle360

import com.android.build.gradle.api.BaseVariant
import com.hhc.walle360.task.ApkPackTask
import com.hhc.walle360.task.MultiChannelTask
import com.hhc.walle360.task.Walle360DownloadTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class Walle360Plugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.extensions.create("apkPackConfig", Walle360Extensions)

        def downloadTask = project.tasks.create("downloadTask", Walle360DownloadTask)
        def apkPackTask = project.tasks.create("apkPackTask", ApkPackTask)
        def apkChannelTask = project.tasks.create("apkChannelTask", MultiChannelTask)

        project.afterEvaluate {
            project.android.applicationVariants.all { BaseVariant variant ->
                def variantName = variant.name.capitalize()
                apkPackTask.variant = variant
                if (!"Release".equals(variantName)) {
                    return
                }
                String taskName = "assemble${variantName}"
                println("downloadTask dependsOn ->${taskName}")
                def assembleRelease = project.tasks.getByName(taskName)
                if (assembleRelease != null) {
                    downloadTask.dependsOn assembleRelease
                } else {
                    if (variant.hasProperty('assembleProvider')) {
                        downloadTask.dependsOn variant.assembleProvider.get()
                    } else {
                        downloadTask.dependsOn variant.assemble
                    }
                }
            }
        }
        apkPackTask.dependsOn(downloadTask)
        apkChannelTask.dependsOn(apkPackTask)
    }

}
