package com.hhc.walle360.task

import com.android.build.gradle.BaseExtension
import com.hhc.walle360.Walle360Extensions
import groovy.io.FileType
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.TaskAction

class MultiChannelTask extends DefaultTask {
    //APK对齐工具
    File zipAlignFile
    //APK签名工具
    File signFile

    Walle360Extensions config
    BaseExtension extension

    MultiChannelTask() {
        group = Walle360Constant.TASK_GROUP_NAME
        description = "执行注入多渠道处理"
        config = Walle360Extensions.getConfig(project)
    }

    @TaskAction
    def channel() {
        println("渠道处理开始===================================================")

        if (config.channelConfigFile == null || !config.channelConfigFile.exists()) {
            println("多渠道配置文件不存在,任务结束")
            return
        }
        def androidExtensions = project.extensions.getByName("android")
        if (androidExtensions != null instanceof BaseExtension) {
            extension = androidExtensions as BaseExtension
        } else {
            println("android{}未配置,任务结束")
            return
        }

        def apkFile = Walle360Constant.getApkJiaguFile()
        if (apkFile == null) {
            //如果没有加固文件使用原文件
            apkFile = new File(config.baseApkFilePath)
        }
        if (apkFile == null || !apkFile.exists()) {
            throw new GradleException("需要注入多渠道APK不存在,任务结束：${apkFile.absolutePath}")
        }

        def signApkPath = apkSinger(apkFile)
        checkSign(signApkPath)
        checkZipAlign(signApkPath)

        if (config.channelConfigFile != null && config.channelConfigFile.exists()) {
            Map<String, String> nameVariantMap = [
                    'appName'      : project.name,
                    'projectName'  : project.rootProject.name,
                    'applicationId': extension.defaultConfig.applicationId,
                    'versionName'  : extension.defaultConfig.versionName,
                    'versionCode'  : extension.defaultConfig.versionCode.toString()
            ]
            File channelOutputFolderParent = new File(
                    new File(config.outRootPath, Walle360Constant.CHANNEL_DIR),
                    nameVariantMap["applicationId"]
            )
            channelOutputFolderParent.mkdirs()
            File channelOutputFolder = new File(
                    channelOutputFolderParent, new Date().format("yyyy-MM-dd-HHmmss")
            )
            channelOutputFolder.mkdirs()
            channelApk(config.channelConfigFile,
                    signApkPath,
                    channelOutputFolder,
                    nameVariantMap)

            Walle360Constant.channelOutputFolder = channelOutputFolder
            new CopySourceTask(project).copySource()
        }

        println("渠道处理结束===================================================")
    }

    private String apkSinger(File apkFile) {
        config.initSignConfig(project)
        config.checkSignConfig()
        initBuildPath()

        def apkPath = apkFile.absolutePath
        String zip_aligned_apk_path = apkPath.substring(0, apkPath.length() - 4) + "_zaf.apk"
        String signed_apk_path = zip_aligned_apk_path.substring(0, zip_aligned_apk_path.length() - 4) + "_sign.apk"

        def out = new StringBuilder(), err = new StringBuilder()
        //APK对齐命令
        def zipAlignShell = "$zipAlignFile.absolutePath -v 4 $apkPath $zip_aligned_apk_path"
        //APK签名命令
        def signedShell = "$signFile.absolutePath sign " +
                "--ks $config.apkJksPath " +
                "--ks-key-alias $config.apkJksAlias " +
                "--ks-pass pass:$config.apkJksStorePwd " +
                "--key-pass pass:$config.apkJksPwd " +
                "--out $signed_apk_path $zip_aligned_apk_path"

        println("APK对齐命令：$zipAlignShell")
        Walle360Utils.executeShell(zipAlignShell, out, err, 1000 * 60 * 10)
        if (err != null && err.length() > 0) {
            println("err->${err.toString()}")
            throw new GradleException(err.toString())
        }
        println("out->${out.toString()}")
        println("APK对齐结束")
        print("\n")

        println("APK签名命令：$signedShell")
        Walle360Utils.executeShell(signedShell, out, err, 1000 * 60 * 10)
        if (err != null && err.length() > 0) {
            println("err->${err.toString()}")
            throw new GradleException(err.toString())
        }
        println("out->${out.toString()}")
        println("APK签名结束")
        print("\n")

        File signedApkPath = new File(signed_apk_path)
        File baseApkFilePath = new File(config.baseApkFilePath)
        File target_apk = new File(signedApkPath.getParent(), baseApkFilePath.getName())
        copyApk(signedApkPath, target_apk.getParentFile(), target_apk.getName())
        println("target_apk->${target_apk.absolutePath}")
        print("\n")
        return target_apk
    }

    private def initBuildPath() {
        config.initAndroidSdkDir(project)
        config.initBuildToolsVersion(project)

        def buildToolParent = new File(config.androidSdkDir, Walle360Constant.BUILD_TOOLS)
        File apkBuild = new File(buildToolParent, config.buildToolsVersion)
        println("Android-sdk build-tools目录：$apkBuild.absolutePath")

        if (apkBuild.exists()) {
            apkBuild.eachFile { childFile ->
                if (childFile.name.contains("zipalign")) {
                    zipAlignFile = childFile
                }
                if (childFile.name.contains("apksigner")) {
                    signFile = childFile
                }
            }
        }

        if (zipAlignFile == null || signFile == null) {
            buildToolParent.eachFileRecurse(FileType.DIRECTORIES) { dir ->
                dir.eachFile { childFile ->
                    if (childFile.name.contains("zipalign")) {
                        zipAlignFile = childFile
                    }
                    if (childFile.name.contains("apksigner")) {
                        signFile = childFile
                    }
                }
            }
            if (zipAlignFile == null || signFile == null) {
                throw new GradleException("无法找到${Walle360Constant.BUILD_TOOLS}工具，" +
                        "请下载最新${Walle360Constant.BUILD_TOOLS}工具")
            }
        }
    }

    private def channelApk(File channelConfigFile,
                           String signApkPath,
                           File channelOutputFolder,
                           Map<String, String> nameVariantMap) {
        def jarFile = new File(config.outRootPath, "walle-cli.jar")
        if (!jarFile.exists()) {
            println("请下载walle-cli.jar")
            return
        }

        def channelShell
        boolean isJson = channelConfigFile.name.endsWith(".json")
        if (isJson) {
            channelShell = "java -jar $jarFile.absolutePath " +
                    "batch2 -f " +
                    "$channelConfigFile.absolutePath " +
                    "$signApkPath " +
                    "$channelOutputFolder.absolutePath"
        } else {
            channelShell = "java -jar $jarFile.absolutePath " +
                    "batch -f " +
                    "$channelConfigFile.absolutePath " +
                    "$signApkPath " +
                    "$channelOutputFolder.absolutePath"
        }
        def out = new StringBuilder(), err = new StringBuilder()

        println("注入渠道命令：$channelShell")
        Walle360Utils.executeShell(channelShell, out, err, 1000 * 60 * 10)
        if (err != null && err.length() > 0) {
            println("err->${err.toString()}")
            throw new GradleException(err.toString())
        }
        println("out->${out.toString()}")
        println("注入渠道结束")
        print("\n")
    }

    private def checkZipAlign(String apkPath) {
        def out = new StringBuilder(), err = new StringBuilder()
        //APK是否对齐检查命令
        def zipAlignShell = "$zipAlignFile.absolutePath -c -v 4 $apkPath"
        println("APK对齐检查：$zipAlignShell")
        Walle360Utils.executeShell(zipAlignShell, out, err, 1000 * 60 * 10)
        if (err != null && err.length() > 0) {
            println("err->$err.toString()")
            throw new GradleException(err.toString())
        }
        println("out->${out.toString()}")
        println("APK对齐检查结束")
        print("\n")
    }

    private def checkSign(String apkPath) {
        def out = new StringBuilder(), err = new StringBuilder()
        //APK是否对齐检查命令
        //-v, --verbose 显示详情(显示是否使用V1和V2签名)
        //--print-certs 显示签名证书信息
        def shell = "$signFile.absolutePath verify -v --print-certs $apkPath"
        println("APK签名检查：$shell")
        Walle360Utils.executeShell(shell, out, err, 1000 * 60 * 10)
        if (err != null && err.length() > 0) {
            println("err->${err.toString()}")
            throw new GradleException(err.toString())
        }
        println("out->${out.toString()}")
        println("APK签名检查结束")
        print("\n")
    }

    private def copyApk(File source, File out, String name) {
        project.copy(new Action<CopySpec>() {
            @Override
            void execute(CopySpec copySpec) {
                copySpec.from(source)
                        .into(out)
                        .rename { String fileName ->
                            fileName.replace(source.getName(), name)
                        }
            }
        })
    }
}
