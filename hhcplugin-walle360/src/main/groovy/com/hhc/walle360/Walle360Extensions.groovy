package com.hhc.walle360

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.internal.dsl.SigningConfig
import com.hhc.walle360.task.Walle360Constant
import org.gradle.api.GradleException
import org.gradle.api.Project

/**
 * 插件配置信息
 */
class Walle360Extensions {
    //输出根路径
    String outRootPath
    //需加固APK原文件路径
    String baseApkFilePath
    //原文件mapping.txt
    String baseMappingPath
    //360加固工具所需账号
    String accountName360
    //360加固工具所需密码
    String accountPwd360
    //美团工具所需渠道文件路径
    File channelConfigFile

    //签名文件路径
    String apkJksPath
    //签名文件密码
    String apkJksStorePwd
    //签名文件别名
    String apkJksAlias
    //签名文件别名密码
    String apkJksPwd

    //Android SDK 目录
    File androidSdkDir
    //Android build-tools版本
    String buildToolsVersion

    /**
     * 获取配置信息
     * @param project
     * @return Walle360Extensions
     */
    static Walle360Extensions getConfig(Project project) {
        //检查是否配置
        def config = project.getExtensions().findByType(Walle360Extensions.class)
        if (config == null) {
            throw new GradleException("Walle360未配置！")
        }
        //如果没有配置，默认/build根目录
        if (config.outRootPath == null || config.outRootPath.length() == 0) {
            config.outRootPath = "${project.buildDir.absolutePath}/" +
                    Walle360Constant.TASK_GROUP_NAME
        }
        File file = new File(config.outRootPath)
        if (!file.exists()) file.mkdirs()
        return config
    }

    /**
     * 检查360加固工具所需参数配置
     */
    def check360Config() {
        if (accountName360 == null || accountPwd360 == null) {
            throw new GradleException("360加固工具账号密码未配置！")
        }
        if (baseApkFilePath == null || baseApkFilePath.length() == 0
                || !new File(baseApkFilePath).exists()) {
            throw new GradleException("需配置加固的apk文件不存在！")
        }
    }

    /**
     * 检查签名文件配置信息
     */
    def checkSignConfig() {
        if (this.apkJksPath == null
                || this.apkJksStorePwd == null
                || this.apkJksAlias == null
                || this.apkJksPwd == null) {
            throw new GradleException("签名配置错误：" +
                    "\napkJksPath = $apkJksPath, " +
                    "\napkJksStorePwd = $apkJksStorePwd, " +
                    "\napkJksAlias = $apkJksAlias" +
                    "\napkJksPwd = $apkJksPwd")
        }
    }

    /**
     * 初始签名文件配置信息
     * @param project project
     */
    def initSignConfig(Project project) {
        if (this.apkJksPath == null
                || this.apkJksStorePwd == null
                || this.apkJksAlias == null
                || this.apkJksPwd == null) {
            BaseExtension extension = project.extensions.getByName("android") as BaseExtension
            Collection<SigningConfig> signingConfigs = extension.getSigningConfigs()
            signingConfigs.forEach { signingConfig ->
                if (signingConfig.name == "release") {
                    this.apkJksPath = signingConfig.storeFile.absolutePath
                    this.apkJksAlias = signingConfig.keyAlias
                    this.apkJksStorePwd = signingConfig.storePassword
                    this.apkJksPwd = signingConfig.keyPassword
                }
            }
        }
    }

    /**
     * 初始Android sdk根目录配置
     * @param project
     */
    def initAndroidSdkDir(Project project) {
        if (androidSdkDir == null || !androidSdkDir.exists()) {
            Properties properties = new Properties()

            //获取local.properties配置文件是否配置"sdk.dir"(android sdk根目录)
            try {
                InputStream inputStream = project
                        .rootProject
                        .file('local.properties')
                        .newDataInputStream()
                properties.load(inputStream)
                def androidSdkDirPath = properties.getProperty('sdk.dir')
                if (androidSdkDirPath != null && androidSdkDirPath.length() > 0) {
                    androidSdkDir = new File(androidSdkDirPath)
                    println("ANDROID_HOME，local.properties配置")
                }
            } catch (Exception e) {
                androidSdkDir = null
                println("error1->${e.getMessage()}")
            }

            //获取系统配置的变量ANDROID_HOME(android sdk根目录)
            try {
                if (androidSdkDir == null || !androidSdkDir.exists()) {
                    //读取操作系统配置的系统环境变量
                    Map<String, String> env = System.getenv()
                    def androidSdkDirPath = env.get("ANDROID_HOME")
                    println("androidSdkDirPath=${androidSdkDirPath}")
                    if (androidSdkDirPath != null && androidSdkDirPath.length() > 0) {
                        androidSdkDir = new File(androidSdkDirPath)
                        println("ANDROID_HOME，系统环境变量配置")
                    }
                }
            } catch (Exception e){
                androidSdkDir = null
                println("error2->${e.getMessage()}")
            }

            if (androidSdkDir == null || !androidSdkDir.exists()) {
                throw new GradleException("获取Android SDK根目录失败,下列配置请选择配置一个：" +
                        "\n1、在项目根路径下添加local.properties配置文件，并添加sdk_dir配置。" +
                        "\n(sdk.dir=D:\\ToolAndroid\\android-sdk-windows)" +
                        "\n2、在windows/mac/linux操作系统中配置ANDROID_HOME系统环境变量。" +
                        "\n(ANDROID_HOME=D:\\ToolAndroid\\android-sdk-windows)"
                )
            }
        }
    }

    /**
     * 初始构建工具版本配置
     */
    def initBuildToolsVersion(Project project) {
        def androidExtensions = project.extensions.getByName("android")
        if (androidExtensions != null instanceof BaseExtension) {
            def extension = androidExtensions as BaseExtension
            buildToolsVersion = extension.buildToolsVersion
        } else {
            println("无法获取buildToolsVersion，请如下配置构建工具版本：\n"
                    + "android {" +
                    "    buildToolsVersion xxx(如：28.0.3)" +
                    "}")
        }
    }
}
