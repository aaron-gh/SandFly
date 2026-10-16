# Copyright 2026 Backtalk contributors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

cmake_minimum_required(VERSION 3.22.1)
project(backtalkquickjs LANGUAGES C)

include(FetchContent)
# Renovate proposes new versions of this (see renovate.json). It can't work out the checksum, so
# update URL_HASH by hand with the version, and the license in src/main/assets/quickjs if its
# copyright lines changed.
FetchContent_Declare(
  quickjs
  URL https://github.com/quickjs-ng/quickjs/archive/refs/tags/v0.17.0.tar.gz
  URL_HASH SHA256=559bc4c420475e55c7ab4510adbc562f55d7524d75e8e89d79ce4bb02f5687d9
)
set(QJS_ENABLE_INSTALL OFF CACHE BOOL "" FORCE)
FetchContent_MakeAvailable(quickjs)

add_library(backtalkquickjs SHARED quickjs_jni.c)
target_link_libraries(backtalkquickjs PRIVATE qjs log)

foreach(target qjs backtalkquickjs)
  target_compile_options(${target} PRIVATE -O2)
  target_compile_definitions(${target} PRIVATE NDEBUG QUICKJS_NG_BUILD)
endforeach()
