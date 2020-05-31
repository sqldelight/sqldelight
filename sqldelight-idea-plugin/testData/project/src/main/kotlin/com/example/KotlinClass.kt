package com.example

class KotlinClass {
  init {
    val queryWrapper = QueryWrapper()
    queryWrapper.mainQueries.someQuery()

    queryWrapper.mainQueries.multiQuery()
    queryWrapper.mainQueries.multiQuery(::MultiQuery)

    queryWrapper.mainQueries.generatesType(::GeneratesType)
    queryWrapper.mainQueries.generatesType(::GeneratesTypeImpl)
  }

  enum class InnerClass {
    VAL
  }
}
