package com.hhc.walle360.task

import com.hhc.walle360.Walle360Extensions
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.CopySpec

class CopySourceTask {
    private Walle360Extensions config
    private Project project

    CopySourceTask(Project project) {
        this.project = project
        config = Walle360Extensions.getConfig(project)
    }

    def copySource() {
        //def extension = project.extensions.getByName("android") as BaseExtension
        //def versionName = extension.defaultConfig.versionName
        //def versionCode = extension.defaultConfig.versionCode
        File out = new File(Walle360Constant.channelOutputFolder, "apk-source")
        if (config.baseApkFilePath != null) {
            File sourceApk = new File(config.baseApkFilePath)
            if (sourceApk.exists()) {
                println("开始复制原APK")
                copy(sourceApk, out)
            }
        }
        if (config.baseMappingPath != null) {
            File sourceMapping = new File(config.baseMappingPath)
            if (sourceMapping.exists()) {
                println("开始复制原mapping")
                copy(sourceMapping, out)
            }
        }
    }

    private def copy(File source, File out) {
        project.copy(new Action<CopySpec>() {
            @Override
            void execute(CopySpec copySpec) {
                copySpec.from(source)
                        .into(out)
            }
        })
    }
}
