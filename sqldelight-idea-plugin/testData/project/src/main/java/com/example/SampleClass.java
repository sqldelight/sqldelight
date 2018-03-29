package com.example;

class SampleClass {
  public void stuff() {
    QueryWrapper queryWrapper = QueryWrapper();
    queryWrapper.mainQueries.someQuery();

    queryWrapper.mainQueries.generatesType(GeneratesType.Impl::new);
    queryWrapper.mainQueries.generatesType(GeneratesTypeImpl::new);
  }
}