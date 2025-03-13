package com.aidaole.easylaunch.sample

import android.app.Application
import android.util.Log
import com.aidaole.easylaunch.EasyLaunch


/**
 * 更复杂的示例用法
 */
object SampleUsageAdvanced {
    
    /**
     * 演示如何在Activity中使用EasyLaunch
     */
    fun initInActivity(application: Application) {
        // 创建任务列表
        val tasks = listOf(
            TaskA(),
            TaskB(),
            TaskC(),
            TaskD()
        )
        
        // 初始化并添加任务
        EasyLaunch.getInstance()
            .init(application)
            .clear() // 清空之前的任务
            .addTasks(tasks) // 添加任务列表
            .start()
    }
    
    /**
     * 演示如何创建自定义任务
     */
    fun createCustomTask() {
        // 创建匿名任务
        val customTask = object : com.aidaole.easylaunch.Task {
            override val name: String
                get() = "CustomTask"
                
            override val dependencies: List<Class<out com.aidaole.easylaunch.Task>>
                get() = listOf(TaskA::class.java)
                
            override val isAsync: Boolean
                get() = true
                
            override suspend fun execute() {
                Log.d("CustomTask", "自定义任务开始执行")
                // 执行自定义逻辑
                Log.d("CustomTask", "自定义任务执行完成")
            }
        }
        
        // 添加自定义任务
        EasyLaunch.getInstance().addTask(customTask)
    }
} 