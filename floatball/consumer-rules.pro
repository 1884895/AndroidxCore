
# 保留库的公开类（消费者需要引用的自定义View）
-keep public class com.xcore.floatball.CircleFanMenuViewGroup {
    public <init>(...);  # 保留所有public构造方法（XML解析/代码实例化）
    public *;            # 保留所有public方法（消费者可能调用的接口）
}

# 保留Kotlin相关依赖（避免消费者混淆Lambda/协程等特性）
-keepattributes Signature, InnerClasses, EnclosingMethod
-keep class kotlin.jvm.functions.Function1 { *; }  # 保留Lambda接口
# 保留库的自定义属性（若消费者在XML中使用）
-keep class com.xcore.floatball.R$styleable { *; }

