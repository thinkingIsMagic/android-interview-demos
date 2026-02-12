# Observability ProGuard 规则

# 保持 SDK 类名不被混淆（便于日志解析）
-keep class com.trackapi.observability.** { *; }

# 保持数据类（用于 JSON 序列化）
-keepclassmembers class com.trackapi.observability.** {
    <fields>;
    <init>(...);
}

# 保持枚举
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
