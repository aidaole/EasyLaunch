# EasyLaunch

这是一个Android的启动开源框架, 用于在启动过程中将任务并行, 达到优化启动速度的目的.

## 框架特点

1. 使用有向无环图配置任务依赖关系, 并支持检查当前任务是否成环
2. 支持main线程, 子线程同步初始化
3. 使用协程实现, 轻量级

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
    // 只有当依赖的任务全部完成后，当前任务才会执行
    override val dependencies: List<Class<out Task>>
        get() = listOf(OtherTask::class.java)
    
    // 任务执行的调度器，默认在IO线程执行
    override val dispatcher: CoroutineDispatcher
        get() = Dispatchers.IO
    
    // 是否是异步任务
    // true: 任务会在后台线程执行，不会阻塞主线程
    // false: 任务会在主线程执行，会阻塞主线程
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
        
        // 添加任务
        EasyLaunch.getInstance()
            .addTask(TaskA())
            .addTask(TaskB())
            .addTask(TaskC())
            .start()
    }
}
```

### 4. 任务依赖关系

EasyLaunch使用有向无环图来管理任务依赖关系，通过`dependencies`属性来指定当前任务依赖的其他任务。

例如，如果TaskC依赖TaskA和TaskB，那么只有当TaskA和TaskB都执行完成后，TaskC才会执行：

```kotlin
class TaskC : Task {
    override val name: String
        get() = "TaskC"
    
    override val dependencies: List<Class<out Task>>
        get() = listOf(TaskA::class.java, TaskB::class.java)
    
    override suspend fun execute() {
        // 执行任务逻辑
    }
}
```

### 5. 主线程和子线程任务

EasyLaunch支持在主线程和子线程执行任务，通过`isAsync`属性来指定：

```kotlin
// 在子线程执行的任务
class AsyncTask : Task {
    override val name: String
        get() = "AsyncTask"
    
    override val isAsync: Boolean
        get() = true
    
    override suspend fun execute() {
        // 在子线程执行的任务逻辑
    }
}

// 在主线程执行的任务
class SyncTask : Task {
    override val name: String
        get() = "SyncTask"
    
    override val isAsync: Boolean
        get() = false
    
    override suspend fun execute() {
        // 在主线程执行的任务逻辑
    }
}
```

## 示例

以下是一个完整的示例，展示如何使用EasyLaunch框架：

```kotlin
// 任务A：没有依赖的任务
class TaskA : Task {
    override val name: String
        get() = "TaskA"
    
    override suspend fun execute() {
        // 执行任务A的逻辑
    }
}

// 任务B：依赖任务A
class TaskB : Task {
    override val name: String
        get() = "TaskB"
    
    override val dependencies: List<Class<out Task>>
        get() = listOf(TaskA::class.java)
    
    override suspend fun execute() {
        // 执行任务B的逻辑
    }
}

// 任务C：依赖任务A
class TaskC : Task {
    override val name: String
        get() = "TaskC"
    
    override val dependencies: List<Class<out Task>>
        get() = listOf(TaskA::class.java)
    
    override suspend fun execute() {
        // 执行任务C的逻辑
    }
}

// 任务D：依赖任务B和任务C
class TaskD : Task {
    override val name: String
        get() = "TaskD"
    
    override val dependencies: List<Class<out Task>>
        get() = listOf(TaskB::class.java, TaskC::class.java)
    
    override val isAsync: Boolean
        get() = false // 在主线程执行
    
    override suspend fun execute() {
        // 执行任务D的逻辑
    }
}

// 在Application中初始化
class MyApplication : Application() {
    
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
    }
}
```

在上面的示例中，任务的执行顺序如下：

1. 首先执行TaskA
2. TaskA执行完成后，并行执行TaskB和TaskC
3. TaskB和TaskC都执行完成后，执行TaskD

## 高级用法

### 1. 添加任务列表

```kotlin
val tasks = listOf(TaskA(), TaskB(), TaskC(), TaskD())
EasyLaunch.getInstance().addTasks(tasks)
```

### 2. 重置和清空任务

```kotlin
// 重置所有任务状态，但不清空任务
EasyLaunch.getInstance().reset()

// 清空所有任务
EasyLaunch.getInstance().clear()
```

## 注意事项

1. 任务依赖关系不能形成环，否则会抛出异常
2. 在添加任务时，确保依赖的任务已经添加到EasyLaunch中
3. start()方法是异步的，不会阻塞调用线程

## 许可证

```
Copyright 2023 aidaole

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
``` 