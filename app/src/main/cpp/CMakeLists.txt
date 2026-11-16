cmake_minimum_required(VERSION 3.22.1)
project("cactus_agent")

add_library(cactus_agent SHARED cactus_jni.cpp)

find_library(log-lib log)

target_link_libraries(cactus_agent ${log-lib})