cmake_minimum_required(VERSION 3.22)

project(sandfly_whisper_opencl C CXX)

set(CMAKE_CXX_STANDARD 17)
set(WHISPER_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../../whisper/src/main/cpp/whisper.cpp)
set(OPENCL_DIR ${CMAKE_CURRENT_SOURCE_DIR}/opencl)

# A libOpenCL.so to link against, made from the function names alone. It is built here rather than
# as a target, so that it isn't packaged: on the phone, the GPU vendor's libOpenCL.so is used.
set(OPENCL_STUB ${CMAKE_BINARY_DIR}/opencl-stub/libOpenCL.so)
file(MAKE_DIRECTORY ${CMAKE_BINARY_DIR}/opencl-stub)
execute_process(
    COMMAND ${CMAKE_C_COMPILER} --target=${CMAKE_C_COMPILER_TARGET} --sysroot=${CMAKE_SYSROOT}
        -shared -fPIC -nostdlib -Wl,-soname,libOpenCL.so
        -o ${OPENCL_STUB} ${CMAKE_CURRENT_SOURCE_DIR}/opencl_stub.c
    RESULT_VARIABLE OPENCL_STUB_RESULT)
if (NOT OPENCL_STUB_RESULT EQUAL 0)
    message(FATAL_ERROR "Could not build the OpenCL stub")
endif()
set(OpenCL_INCLUDE_DIR ${OPENCL_DIR} CACHE PATH "" FORCE)
set(OpenCL_LIBRARY ${OPENCL_STUB} CACHE FILEPATH "" FORCE)

# ggml with its CPU and OpenCL backends, built into the one library below.
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_BACKEND_DL OFF CACHE BOOL "" FORCE)
set(GGML_LLAMAFILE OFF CACHE BOOL "" FORCE)
set(GGML_OPENCL ON CACHE BOOL "" FORCE)
set(GGML_OPENCL_EMBED_KERNELS ON CACHE BOOL "" FORCE)
set(GGML_OPENCL_USE_ADRENO_KERNELS ON CACHE BOOL "" FORCE)
add_subdirectory(${WHISPER_DIR}/ggml ggml)

add_library(sandfly_whisper_opencl SHARED
    ${WHISPER_DIR}/src/whisper.cpp
    ${WHISPER_DIR}/src/aneforge/whisper-aneforge.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/../../../../whisper/src/main/cpp/whisper_jni.cpp)
target_include_directories(sandfly_whisper_opencl PRIVATE
    ${WHISPER_DIR}/include
    ${WHISPER_DIR}/src
    ${WHISPER_DIR}/ggml/include)
target_compile_definitions(sandfly_whisper_opencl PRIVATE
    WHISPER_VERSION="1.9.5"
    WHISPER_JNI_CLASS=WhisperOpenClNative)
target_compile_options(sandfly_whisper_opencl PRIVATE -O3 -fvisibility=hidden -ffunction-sections -fdata-sections)
target_link_options(sandfly_whisper_opencl PRIVATE -Wl,--gc-sections -Wl,--exclude-libs,ALL)
find_library(LOG_LIB log)
target_link_libraries(sandfly_whisper_opencl PRIVATE ggml ${LOG_LIB})
