package com.aidaole.easylaunch.sample

import android.app.Application
import android.util.Log
import com.aidaole.easylaunch.EasyLaunch

/**
 * 示例用法
 * 展示如何在Application中使用EasyLaunch框架
 */
class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 初始化EasyLaunch
        EasyLaunch.getInstance().init(this)

        // 添加任务
        EasyLaunch.getInstance()
            .addTask(TaskA())
            .addTask(TaskB())
            .addTask(TaskC())
            .addTask(TaskD())
            .start()

        Log.d("SampleApplication", "启动任务已添加并开始执行")

        // 注意：start()方法是异步的，不会阻塞主线程
        // 此时任务可能还在执行中
        Log.d("SampleApplication", "Application.onCreate() 继续执行")
    }
}
