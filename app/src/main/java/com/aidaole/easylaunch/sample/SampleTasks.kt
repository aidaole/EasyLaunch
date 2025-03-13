package com.aidaole.easylaunch.sample

import android.util.Log
import com.aidaole.easylaunch.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay

/**
 * 示例任务A
 * 没有依赖的任务，将在启动时最先执行
 */
class TaskA : Task {
    override val name: String
        get() = "TaskA"

    override val isAsync: Boolean
        get() = true

    override suspend fun execute() {
        Log.d("SampleTask", "TaskA 开始执行")
//        delay(500) // 模拟耗时操作
        fibonacci(42)
        Log.d("SampleTask", "TaskA 执行完成")
    }
}

/**
 * 示例任务B
 * 依赖TaskA，只有当TaskA执行完成后才会执行
 */
class TaskB : Task {
    override val name: String
        get() = "TaskB"

    override val dependencies: List<Class<out Task>>
        get() = listOf(TaskA::class.java)

    override suspend fun execute() {
        Log.d("SampleTask", "TaskB 开始执行")
//        delay(300) // 模拟耗时操作
        Thread.sleep(300)
        Log.d("SampleTask", "TaskB 执行完成")
    }
}

/**
 * 示例任务C
 * 依赖TaskA，只有当TaskA执行完成后才会执行
 * 与TaskB并行执行
 */
class TaskC : Task {
    override val name: String
        get() = "TaskC"

    override val dependencies: List<Class<out Task>>
        get() = listOf(TaskD::class.java)

    override suspend fun execute() {
        Log.d("SampleTask", "TaskC 开始执行")
        delay(200) // 模拟耗时操作
        Log.d("SampleTask", "TaskC 执行完成")
    }
}

/**
 * 示例任务D
 * 依赖TaskB和TaskC，只有当TaskB和TaskC都执行完成后才会执行
 */
class TaskD : Task {
    override val name: String
        get() = "TaskD"

    override val dependencies: List<Class<out Task>>
        get() = listOf(TaskB::class.java, TaskC::class.java)

    override val isAsync: Boolean
        get() = false // 在主线程执行

    override suspend fun execute() {
        Log.d("SampleTask", "TaskD 开始执行（主线程）")
        delay(100) // 模拟耗时操作
        Log.d("SampleTask", "TaskD 执行完成（主线程）")
    }
}

fun fibonacci(n: Int): Int {
    if (n == 0 || n == 1) {
        return 1
    }
    return fibonacci(n - 1) + fibonacci(n - 2)
}