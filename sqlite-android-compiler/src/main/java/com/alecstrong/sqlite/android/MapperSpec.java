package com.alecstrong.sqlite.android;

import com.alecstrong.sqlite.android.model.Table;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;

public class MapperSpec {
  public TypeSpec build(Table table) {
    TypeSpec.Builder mapper = TypeSpec.classBuilder(table.interfaceName() + "Mapper")
        .addModifiers(Modifier.PROTECTED, Modifier.FINAL);



    return mapper.build();
  }
}
