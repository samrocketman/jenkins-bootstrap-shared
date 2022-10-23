#!/bin/bash
# Created by Sam Gleske
# Sun Oct 23 05:48:03 EDT 2022
# Pop!_OS 18.04 LTS
# Linux 5.4.0-113-generic x86_64
# GNU bash, version 4.4.20(1)-release (x86_64-pc-linux-gnu)
# Gradle 6.2.2
# Build time: 2020-03-04 08:49:31 UTC
# Revision: 7d0bf6dcb46c143bcc3b7a0fa40a8e5ca28e5856
# Kotlin: 1.3.61
# Groovy: 2.5.8
# Ant: Apache Ant(TM) version 1.10.7 compiled on September 1 2019
# JVM: 1.8.0_342 (Private Build 25.342-b07)
# OS: Linux 5.4.0-113-generic amd64

# DESCRIPTION:
#     Reads dependencies.gradle for only plugin artifacts and prints a sorted
#     list of the artifacts found.  This is useful for migrations when
#     occasionally you may need to start from scratch and capture the same
#     plugins but as their latest versions.
#
#     Running this script is likely only required for validating upgrades to
#     ensure anything is missing.  It is rare a Jenkins instance would need to
#     completely start from scratch.

set -exo pipefail

if [ ! -f dependencies.gradle ]; then
  echo 'Missing dependencies.gradle' >&2
  exit 1
fi

grep getplugins dependencies.gradle | cut -d: -f2 | sort -u
