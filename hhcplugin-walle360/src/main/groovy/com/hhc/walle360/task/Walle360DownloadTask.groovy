package com.hhc.walle360.task

import com.hhc.walle360.Walle360Extensions
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.TaskAction

/**
 * 下载360加固工具与美团渠道打包工具
 */
class Walle360DownloadTask extends DefaultTask {
    //360加固工具下载地址
    private static final String url_360_mac = "http://down.360safe.com/360Jiagu/360jiagubao_mac.zip"
    private static final String url_360_linux = "http://down.360safe.com/360Jiagu/360jiagubao_linux_64.zip"
    private static final String url_360_win = "http://down.360safe.com/360Jiagu/360jiagubao_windows_32.zip"
    //美团渠道打包工具下载地址
    private static final String url_walle_gw = "https://github.com/Meituan-Dianping/walle/releases/download/v1.1.6/walle-cli-all.jar"
    private static final String url_walle_zc = "https://github.com/zengcanxiang/Android-pack-plugin/blob/master/walle_cli.jar"
    //360加固工具实际执行工具jar文件路径
    private static final String m360JarPath = "/jiagu/jiagu.jar"
    //美团渠道打包工具路径
    private static final String mWalleJarPath = "/walle-cli.jar"

    private String down360Url = url_360_mac
    private String downWalleUrl = url_walle_gw

    //插件配置信息
    private Walle360Extensions mConfig
    //360加固工具压缩文件
    private File m360ZipFile
    //360加固工具压缩文件解压根目录
    private File m360JarRootPath

    Walle360DownloadTask() {
        group = Walle360Constant.TASK_GROUP_NAME
        description = "下载360加固工具和美团渠道打包工具"
        mConfig = Walle360Extensions.getConfig(project)
    }

    @TaskAction
    def download() {
        execute360()
        executeWalle()
    }

    private def init360Config() {
        m360ZipFile = new File(mConfig.outRootPath, "360.zip")
        m360JarRootPath = new File(mConfig.outRootPath, Walle360Constant.UNZIP_360_DIR)
        def os = System.getProperty("os.name").toLowerCase()
        if (os.contains("linux")) {
            down360Url = url_360_linux
        } else if (os.contains("mac")) {
            down360Url = url_360_mac
        } else {
            down360Url = url_360_win
        }
    }

    private def un360Zip() {
        println("解压开始：${m360ZipFile.getAbsolutePath()}")
        println("->${m360JarRootPath.getAbsolutePath()}" + m360JarPath)
        project.copy(new Action<CopySpec>() {
            @Override
            void execute(CopySpec copySpec) {
                copySpec.from(project.zipTree(get360ZipFile()))
                        .into(get360JarRootPath())
                println("解压结束！")
            }
        })
    }

    private def execute360() {
        init360Config()
        def jar = new File(m360JarRootPath, m360JarPath)
        if (!jar.exists()) {
            if (m360ZipFile.exists()) {
                println("本地${m360ZipFile.getAbsolutePath()}压缩包已存在！")
            } else {
                println("本地${m360ZipFile.getAbsolutePath()}压缩包不存在！")
                Walle360Utils.downloadFile(down360Url, m360ZipFile)
            }
            un360Zip()
        } else {
            println("检查到360加固工具已存在！")
        }
    }

    private def executeWalle() {
        def walleJarFile = new File(mConfig.outRootPath, mWalleJarPath)
        if (!walleJarFile.exists()) {
            println("检查到walle工具不存在！")
            //Walle360Utils.downloadFile(downWalleUrl, walleJarFile)
            def walleCli = new File(mConfig.outRootPath, "walle-cli.jar")
            Walle360Utils.copyFile("/walle/walle_cli.jar", walleCli)
            println("下载完成：${walleCli.absolutePath}")
        } else {
            println("检查到walle工具已存在！")
        }
    }

    private File get360ZipFile() {
        return m360ZipFile
    }

    private File get360JarRootPath() {
        return m360JarRootPath
    }

    static String get360JarPath() {
        return m360JarPath
    }

    static String getWalleJarPath() {
        return mWalleJarPath
    }
}
