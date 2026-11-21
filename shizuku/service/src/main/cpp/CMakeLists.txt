cmake_minimum_required(VERSION 3.22.1)
project(sandfly_volume_keys C)

add_library(sandfly_volume_keys SHARED volume_keys.c)
target_compile_options(sandfly_volume_keys PRIVATE -Wall -Wextra -Werror)
