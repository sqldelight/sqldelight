package com.example

class KotlinClass {
  init {
    val queryWrapper = QueryWrapper()
    queryWrapper.mainQueries.someQuery()

    queryWrapper.mainQueries.multiQuery()
    queryWrapper.mainQueries.multiQuery(MultiQuery::Impl)

    queryWrapper.mainQueries.generatesType(GeneratesType::Impl)
    queryWrapper.mainQueries.generatesType(::GeneratesTypeImpl)
  }
}