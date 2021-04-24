package com.example.db

import kotlin.random.Random

private val charPool: List<Char> = ('a'..'z') + ('0'..'9')

fun genString(size: Int): String {
  return (1..size)
    .map { Random.nextInt(0, charPool.size) }
    .map(charPool::get)
    .joinToString("")
}

fun genLong(): Long = Random.nextLong(100)
