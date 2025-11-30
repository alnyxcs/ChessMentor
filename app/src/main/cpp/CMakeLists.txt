cmake_minimum_required(VERSION 3.18.1)

project("chess_engine")

add_library(
        stockfish-lib
        SHARED
        stockfish_jni.cpp)

find_library(
        log-lib
        log)

target_link_libraries(
        stockfish-lib
        ${log-lib})