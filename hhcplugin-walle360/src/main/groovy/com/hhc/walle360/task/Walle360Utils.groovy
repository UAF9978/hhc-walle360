package com.hhc.walle360.task

class Walle360Utils {

    static def downloadFile(String url, File saveFile) {
        println("下载文件:$url")
        def connection = new URL(url).openStream()
        def stream2 = new URL(url).openConnection()
        def total = stream2.getContentLength()
        def len
        def hasRead = 0
        byte[] arr = new byte[1024 * 5]
        def out = new FileOutputStream(saveFile)
        def lastResult = 0
        while ((len = connection.read(arr)) != -1) {
            out.write(arr, 0, len)
            hasRead += len
            def decimal = hasRead / total * 100 + ""

            if (decimal != "100")
                decimal = decimal.substring(0, decimal.indexOf("."))

            if (lastResult == Integer.parseInt(decimal)) {
                lastResult++
                println("下载进度：" + decimal + "%")
            }
        }
        connection.close()
        out.close()
        println("下载完成")
    }

    static def executeShell(String shellStr,
                            StringBuilder out,
                            StringBuilder err,
                            int millis) {
        def p = shellStr.execute()
        p.consumeProcessOutput(out, err)
        p.waitForOrKill(millis)
    }

    static def copyFile(String path, File file) {
        new FileOutputStream(file).withStream {
            def is = this.getResourceAsStream(path)
            it.write(is.bytes)
            is.close()
        }
    }
}
