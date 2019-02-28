package com.example;

class SampleClass {
  public void stuff() {
    QueryWrapper queryWrapper = QueryWrapper.Companion.INSTANCE.invoke();
    queryWrapper.getMainQueries().someQuery();

    queryWrapper.getMainQueries().generatesType(GeneratesType.Impl::new);
    queryWrapper.getMainQueries().generatesType(GeneratesTypeImpl::new);
  }
}
