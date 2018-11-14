package com.sample

expect val platform: String

class Greeter {
  fun greeting(): String {
    return "Hello from $platform"
  }
}