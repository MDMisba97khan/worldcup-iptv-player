cmake_minimum_required(VERSION 3.28.0)

project("iptvplayer-native")

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find required NDK components
find_library(log-lib log)
find_library(android-lib android)

add_library(
    playlist-parser
    SHARED
    playlist-parser.cpp
)

target_include_directories(playlist-parser PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})

# Link native logging library
target_link_libraries(playlist-parser ${log-lib} ${android-lib})
