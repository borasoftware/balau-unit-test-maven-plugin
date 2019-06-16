cmake_minimum_required(VERSION 3.6.3)
project(TestProject VERSION 1.0.0)

############################# CMAKE CONFIGURATION #############################

## Disallow in-source builds
if ("${PROJECT_BINARY_DIR}" STREQUAL "${PROJECT_SOURCE_DIR}")
	message(FATAL_ERROR
		"
No in-source builds please."
		"
  mkdir build; cd build; cmake .."
		"
Please first remove the files created by this failed run with"
		"
  rm -rf CMakeCache.txt CMakeFiles"
)
endif ()

set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib)
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin)

############################## USER CONFIGURATION #############################

#
# Default user prefix path, install path, include and lib directories if not already set.
#
# By default, the project uses the user's $ENV{HOME}/usr directory.
#
#   CMAKE_PREFIX_PATH    : Used by the FIND_XXX() commands (default = $ENV{HOME}/usr)
#   CMAKE_INSTALL_PREFIX : Location to install the library (default = $ENV{HOME}/usr)
#   USER_INCLUDE_DIR     : Additional include directories  (default = $ENV{HOME}/usr/include)
#   USER_LIB_DIR         : Additional library directories  (default = $ENV{HOME}/usr/lib)
#

if (NOT DEFINED CMAKE_PREFIX_PATH)
	set(CMAKE_PREFIX_PATH $ENV{HOME}/usr)
endif ()

if (CMAKE_INSTALL_PREFIX_INITIALIZED_TO_DEFAULT)
	set(CMAKE_INSTALL_PREFIX "$ENV{HOME}/usr" CACHE PATH "..." FORCE)
endif()

if (NOT DEFINED USER_INCLUDE_DIR)
	set(USER_INCLUDE_DIR $ENV{HOME}/usr/include)
endif ()

if (NOT DEFINED USER_LIB_DIR)
	set(USER_LIB_DIR $ENV{HOME}/usr/lib)
endif ()

include_directories(${USER_INCLUDE_DIR})
link_directories(${USER_LIB_DIR})

message(STATUS "CMAKE_PREFIX_PATH:    ${CMAKE_PREFIX_PATH}")
message(STATUS "CMAKE_INSTALL_PREFIX: ${CMAKE_INSTALL_PREFIX}")
message(STATUS "USER_INCLUDE_DIR:     ${USER_INCLUDE_DIR}")
message(STATUS "USER_LIB_DIR:         ${USER_LIB_DIR}")

################################## DEFINITIONS ################################

include_directories(src/main/cpp)
include_directories(${CMAKE_BINARY_DIR})

set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} "${CMAKE_SOURCE_DIR}/cmake")

################################### COMPILER ##################################

# Common compiler switches used in targets.
set(BORA_CXX_FLAGS "-fPIC -Wall -pedantic -Wextra")

if ("${CMAKE_CXX_COMPILER_ID}" STREQUAL "Clang" OR "${CMAKE_CXX_COMPILER_ID}" STREQUAL "GNU")
	set(BORA_CXX_FLAGS "${BORA_CXX_FLAGS} -Wno-deprecated-declarations")
	set(BORA_CXX_FLAGS "${BORA_CXX_FLAGS} -fno-omit-frame-pointer")
	set(BORA_CXX_FLAGS "${BORA_CXX_FLAGS} -Wno-unused-result")
	set(BORA_CXX_FLAGS "${BORA_CXX_FLAGS} -Wno-unknown-pragmas")
	set(BORA_CXX_FLAGS "${BORA_CXX_FLAGS} -Wno-unused-function")
	set(BORA_CXX_FLAGS "${BORA_CXX_FLAGS} -Wno-unused-parameter") # Required for Boost Process headers.
	set(BORA_CXX_FLAGS "${BORA_CXX_FLAGS} -Wno-ignored-qualifiers") # Required for Boost Beast headers.
endif()

if ("${CMAKE_CXX_COMPILER_ID}" STREQUAL "Clang")
	# using Clang
	set(BORA_CXX_FLAGS "${BORA_CXX_FLAGS} -Wno-unused-lambda-capture") # Required for Boost Process headers.
	set(BORA_CXX_FLAGS "${BORA_CXX_FLAGS} -Wno-unused-private-field")
	set(BORA_CXX_FLAGS_DEBUG "${BORA_CXX_FLAGS_DEBUG} -fstandalone-debug")
elseif ("${CMAKE_CXX_COMPILER_ID}" STREQUAL "GNU")
	# using GCC
	set(BORA_CXX_FLAGS "${BORA_CXX_FLAGS} -Wno-unused-but-set-parameter")
	set(BORA_CXX_FLAGS "${BORA_CXX_FLAGS} -Wno-implicit-fallthrough") # Required for Boost Locale library.
elseif ("${CMAKE_CXX_COMPILER_ID}" STREQUAL "Intel")
	# using Intel C++
	message(FATAL_ERROR "Balau has not been tested with Intel C++.
		Please test and provide a pull request with any required changes.")
elseif ("${CMAKE_CXX_COMPILER_ID}" STREQUAL "MSVC")
	# using Visual Studio C++
	message(FATAL_ERROR "Balau has not been ported to the Windows platform.
		Please provide a pull request with any required changes if a Windows port is required.")
endif()

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${BORA_CXX_FLAGS}")
set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} ${BORA_CXX_FLAGS_DEBUG} -DBALAU_DEBUG -DBALAU_STACK_TRACES -O0 -g")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O3")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

################################ BALAU LIBRARY ################################

find_package(Balau 2019.6.1 REQUIRED)
message(STATUS "Balau include dirs: ${Balau_INCLUDE_DIRS}")
message(STATUS "Balau library:      ${Balau_LIBRARY}")
include_directories(BEFORE ${Balau_INCLUDE_DIRS})
set(ALL_LIBS ${ALL_LIBS} ${Balau_LIBRARY})

################################## BACKPORTS ##################################

# TODO work out how to automate this once installed.

include(CheckIncludeFileCXX)

check_include_file_cxx(string_view STRING_VIEW_AVAILABLE)

if (STRING_VIEW_AVAILABLE)
	message(STATUS "std::string_view is available. Setting -DBALAU_STD_STRING_VIEW_AVAILABLE")
	set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DBALAU_STD_STRING_VIEW_AVAILABLE")
else ()
	message(STATUS "std::string_view is not available. Using boost::string_view")
endif ()

############################### BOOST LIBRARIES ###############################

set(Boost_DETAILED_FAILURE_MSG ON)
set(Boost_USE_STATIC_LIBS ON)
set(Boost_USE_MULTITHREADED ON)
set(Boost_USE_STATIC_RUNTIME OFF)

find_package(Boost 1.68.0 COMPONENTS thread chrono date_time filesystem system serialization iostreams stacktrace_backtrace)

if (Boost_STACKTRACE_BACKTRACE_FOUND)
    find_package(Boost 1.68.0 REQUIRED COMPONENTS thread chrono date_time filesystem system serialization iostreams stacktrace_backtrace)
    message(STATUS "DEBUG MODE: adding libbacktrace to link libraries.")
    set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -DBOOST_STACKTRACE_LINK -DBOOST_STACKTRACE_USE_BACKTRACE")
    set(ALL_LIBS ${ALL_LIBS} ${Boost_LIBRARIES} backtrace)
else ()
    find_package(Boost 1.68.0 REQUIRED COMPONENTS thread chrono date_time filesystem system serialization iostreams)
    set(ALL_LIBS ${ALL_LIBS} ${Boost_LIBRARIES})
endif ()

include_directories(${Boost_INCLUDE_DIRS})
message(STATUS "Boost include dirs: ${Boost_INCLUDE_DIRS}")

#
# Undefine this in end code before including any Balau headers in order to
# print relative paths in stack traces instead of just the filename.
#
add_definitions("-DBALAU_STACK_TRACES_STRIP_PATH")

##################################### ICU #####################################

find_package(ICU 60.2 REQUIRED COMPONENTS i18n uc data)
include_directories(${ICU_INCLUDE_DIRS})
set(ALL_LIBS ${ALL_LIBS} ${ICU_LIBRARIES})
message(STATUS "ICU include dirs: ${ICU_INCLUDE_DIRS}")

############################# THREADING LIBRARIES #############################

set(THREADS_PREFER_PTHREAD_FLAG ON)
find_package(Threads REQUIRED)
set(ALL_LIBS ${ALL_LIBS} Threads::Threads)

############################### OTHER LIBRARIES ###############################

find_package(ZLIB REQUIRED)
include_directories(${ZLIB_INCLUDE_DIR})
set(ALL_LIBS ${ALL_LIBS} ${ZLIB_LIBRARIES})
message(STATUS "ZLIB include dir: ${ZLIB_INCLUDE_DIR}")
message(STATUS "ZLIB libraries: ${ZLIB_LIBRARIES}")

find_package(OpenSSL REQUIRED)
include_directories(${OPENSSL_INCLUDE_DIR})
set(ALL_LIBS ${ALL_LIBS} ${OPENSSL_LIBRARIES})
message(STATUS "OpenSSL include dir: ${OPENSSL_INCLUDE_DIR}")
message(STATUS "OpenSSL libraries: ${OPENSSL_LIBRARIES}")

# TODO
#find_package(LIBZIP REQUIRED)
#include_directories(${LIBZIP_INCLUDE_DIR})
#set(ALL_LIBS ${ALL_LIBS} ${LIBZIP_LIBRARIES})
#message(STATUS "LIBZIP include dir: ${LIBZIP_INCLUDE_DIR}")
#message(STATUS "LIBZIP libraries: ${LIBZIP_LIBRARIES}")

set(ALL_LIBS ${ALL_LIBS} rt zip curl)
set(ALL_LIBS ${ALL_LIBS} sctp)
set(ALL_LIBS ${ALL_LIBS} atomic)

set(ALL_LIBS ${ALL_LIBS} ${CMAKE_DL_LIBS})

############################### STATUS PRINTOUT ###############################

message(STATUS "Include directories:")
get_property(ALL_INCLUDE_DIRECTORIES DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR} PROPERTY INCLUDE_DIRECTORIES)
foreach(INCLUDE_DIRECTORY ${ALL_INCLUDE_DIRECTORIES})
	message(STATUS "    ${INCLUDE_DIRECTORY}")
endforeach()

message(STATUS "Libraries:")
foreach(LIB ${ALL_LIBS})
	message(STATUS "    ${LIB}")
endforeach()

message(STATUS "CMAKE_CXX_FLAGS: ${CMAKE_CXX_FLAGS}")

#################################### MAIN #####################################

add_library(Lib STATIC src/main/cpp/Lib.hpp src/main/cpp/Lib.cpp)
target_link_libraries(Lib ${ALL_LIBS})

add_executable(App src/main/cpp/App.cpp)
target_link_libraries(App Lib ${ALL_LIBS})

#################################### TEST #####################################

set(TESTS_SOURCE_FILES
	src/test/cpp/Tests.cpp

	src/test/cpp/LibTest.cpp
	src/test/cpp/LibTest.hpp

	src/test/cpp/LibIT.cpp
	src/test/cpp/LibIT.hpp
)

add_executable(Tests ${TESTS_SOURCE_FILES})
target_link_libraries(Tests Lib ${ALL_LIBS})
