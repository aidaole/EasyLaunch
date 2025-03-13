# EasyLaunch

这是一个Android的启动开源框架, 用于在启动过程中将任务并行, 达到优化启动速度的目的.

框架特点:

1. 使用有向无环图配置任务依赖关系, 并支持检查当前任务是否成环
2. 支持main线程, 子线程同步初始化
3. 使用协程实现, 轻量级
4. 支持任务依赖的延迟解析，可以任意顺序添加任务
5. 支持任务完成监听，可以在特定任务完成后执行特定操作

## 使用方法

### 1. 添加依赖

在项目的根目录的build.gradle中添加JitPack仓库：

```groovy
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

在应用模块的build.gradle中添加依赖：

```groovy
dependencies {
    implementation 'com.github.aidaole:EasyLaunch:1.0.0'
}
```

### 2. 创建启动任务

创建启动任务需要实现`Task`接口：

```kotlin
class YourTask : Task {
    // 任务名称
    override val name: String
        get() = "YourTask"
    
    // 任务依赖，表示当前任务依赖的其他任务
    override val dependencies: List<Class<out Task>>
        get() = listOf(OtherTask::class.java)
    
    // 是否是异步任务
    override val isAsync: Boolean
        get() = true
    
    // 任务执行方法
    override suspend fun execute() {
        // 在此处实现任务的具体逻辑
    }
}
```

### 3. 在Application中初始化

在Application的onCreate方法中初始化EasyLaunch：

```kotlin
class YourApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化EasyLaunch
        EasyLaunch.getInstance().init(this)
        
        // 添加任务 - 可以任意顺序添加
        EasyLaunch.getInstance()
            .addTask(TaskD()) // 依赖TaskB和TaskC
            .addTask(TaskB()) // 依赖TaskA
            .addTask(TaskC()) // 依赖TaskA
            .addTask(TaskA()) // 没有依赖
            .start()
    }
}
```

### 4. 等待任务完成

如果需要等待某些任务完成后再执行某些操作，可以使用任务完成监听：

```kotlin
// 监听单个任务完成
EasyLaunch.getInstance()
    .addTaskCompletedListener(TaskB::class.java) {
        // 当TaskB完成时执行
        Log.d("Sample", "TaskB已完成，可以执行特定操作")
    }
    .start()

// 监听多个任务完成
EasyLaunch.getInstance()
    .addTasksCompletedListener(listOf(TaskB::class.java, TaskC::class.java)) {
        // 当TaskB和TaskC都完成时执行
        Log.d("Sample", "TaskB和TaskC都已完成，可以执行特定操作")
    }
    .start()

// 监听所有任务完成
EasyLaunch.getInstance()
    .addAllTasksCompletedListener {
        // 当所有任务完成时执行
        Log.d("Sample", "所有任务已完成，可以跳转到主界面")
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
    .start()
```

更多详细信息请查看 [EasyLaunch模块README](easylaunch/README.md)。

