cmake_minimum_required(VERSION 3.22)

project(sandfly_whisper C CXX)

set(CMAKE_CXX_STANDARD 17)
set(WHISPER_DIR ${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp)

# ggml with only its CPU backend, built into the one library below.
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_BACKEND_DL OFF CACHE BOOL "" FORCE)
set(GGML_LLAMAFILE OFF CACHE BOOL "" FORCE)
# ggml does all the work. Without this, debug builds of the app, which are the ones published,
# compile it unoptimized, which makes transcription many times slower.
add_compile_options(-O3)
add_subdirectory(${WHISPER_DIR}/ggml ggml)

add_library(sandfly_whisper SHARED
    ${WHISPER_DIR}/src/whisper.cpp
    ${WHISPER_DIR}/src/aneforge/whisper-aneforge.cpp
    whisper_jni.cpp)
target_include_directories(sandfly_whisper PRIVATE
    ${WHISPER_DIR}/include
    ${WHISPER_DIR}/src
    ${WHISPER_DIR}/ggml/include)
target_compile_definitions(sandfly_whisper PRIVATE WHISPER_VERSION="1.9.5")
target_compile_options(sandfly_whisper PRIVATE -O3 -fvisibility=hidden -ffunction-sections -fdata-sections)
target_link_options(sandfly_whisper PRIVATE -Wl,--gc-sections -Wl,--exclude-libs,ALL)
find_library(LOG_LIB log)
target_link_libraries(sandfly_whisper PRIVATE ggml ${LOG_LIB})
