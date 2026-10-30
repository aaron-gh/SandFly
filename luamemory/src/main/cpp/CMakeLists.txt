cmake_minimum_required(VERSION 3.22)

project(sandfly_lua_memory C)

add_library(sandfly_lua_memory SHARED lua_memory.c)
target_compile_options(sandfly_lua_memory PRIVATE -O2 -Wall -fvisibility=hidden)
target_link_libraries(sandfly_lua_memory PRIVATE dl)
