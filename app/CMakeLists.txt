cmake_minimum_required(VERSION 3.22.1)
project("pixelrooter")

add_library(
    pixelrooter_bridge
    SHARED
    src/main/cpp/pixelrooter_bridge.cpp
)

find_library(log-lib log)
find_library(dl-lib dl)

target_link_libraries(
    pixelrooter_bridge
    ${log-lib}
    ${dl-lib}
)
