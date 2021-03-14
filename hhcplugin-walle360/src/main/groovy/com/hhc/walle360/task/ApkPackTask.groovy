package com.hhc.walle360.task

import com.android.build.gradle.api.BaseVariant
import com.hhc.walle360.Walle360Extensions
import groovy.io.FileType
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * APk加固任务
 */
class ApkPackTask extends DefaultTask {
    @Input
    public BaseVariant variant;
    private Walle360Extensions mConfig
    private File m360JarRootPath
    private File m360JarPath

    ApkPackTask() {
        group = Walle360Constant.TASK_GROUP_NAME
        description = "执行加固处理"
        mConfig = Walle360Extensions.getConfig(project)
    }

    @TaskAction
    def pack() {
        //路径检查
        m360JarRootPath = new File(mConfig.outRootPath, Walle360Constant.UNZIP_360_DIR)
        m360JarPath = new File(m360JarRootPath, Walle360DownloadTask.get360JarPath())
        if (!m360JarPath.exists()) {
            println("360加固工具不存在，请检查文件:${m360JarPath.absolutePath}")
            return
        }

        //获取需要加固的文件
        if (mConfig.baseApkFilePath == null) {
            def iterator = variant.outputs.iterator();
            while (iterator.hasNext()) {
                def it = iterator.next()
                def apkFile = it.outputFile
                mConfig.baseApkFilePath = apkFile.absolutePath
            }
        }
        mConfig.check360Config()

        //360登录处理
        if (!login()) return
        nocert360Service()

        //加固开始执行
        println("360加固开始")
        File file = new File(mConfig.outRootPath, Walle360Constant.PACK_360_DIR)
        def resultPath = new File(file, new Date().format("yyyy-MM-dd-HHmmss"))
        resultPath.mkdirs()

        def shell = "java -jar $m360JarPath.absolutePath -jiagu $mConfig.baseApkFilePath " +
                "$resultPath.absolutePath"
        println("需加固的APK文件->${mConfig.baseApkFilePath}")
        println("APK加固命令：$shell")
        def out = new StringBuilder(), err = new StringBuilder()
        Walle360Utils.executeShell(shell, out, err, 1000 * 60 * 10)
        println("error->" + err.toString())
        println("out->" + out.toString())

        if (err.length() > 0) {
            if (err.contains("error=13, Permission denied")) {
                return
            }
        }
        if (out.length() <= 0 || !(out.contains("已加固")
                || out.contains("任务完成"))) {
            println("加固验证成功条件不符合，可能存在失败情况")
        }

        println("360加固完成")
        resultPath.eachFileMatch(FileType.FILES, ~/.*\.apk/) {
            Walle360Constant.apkJiaguFile = it
        }
    }

    private Boolean login() {
        if (!m360JarPath.exists()) {
            def os = System.getProperty("os.name").toLowerCase()
            if (os.contains("linux")) {
                //360加固linux的文件夹里面的摆放和其他的不一样，需要处理
                m360JarRootPath.eachFile { child ->
                    "mv ${new File(child, "jiagu").absolutePath} $child.parent".execute()
                }
            }
        }
        String loginShell = "java -jar $m360JarPath.absolutePath -login $mConfig.accountName360 $mConfig.accountPwd360"
        def out = new StringBuilder(), err = new StringBuilder()
        Walle360Utils.executeShell(loginShell, out, err, 50000)
        if (out.length() <= 0 || !out.contains("login success")) {
            println(out.toString())
            println(err.toString())
            println(loginShell)
            throw new GradleException("加固登录失败")
        }
        return true
    }

    private def nocert360Service() {
        println("360加固工具清除打包额外配置")
        def clearShell = "java -jar $m360JarPath.absolutePath -config -nocert"
        clearShell.execute()
    }

}
