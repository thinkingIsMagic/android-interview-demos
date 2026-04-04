cmake_minimum_required(VERSION 3.22.1)

project("native-crash-lib")

# 设置 C++ 标准
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 源文件
add_library(native-crash-lib SHARED
    native_crash.cpp
)

# 链接 log 库（用于 __android_log_print）
find_library(log-lib log)
if(log-lib)
    target_link_libraries(native-crash-lib ${log-lib})
endif()

# Android 日志
target_compile_definitions(native-crash-lib PRIVATE ANDROID)
