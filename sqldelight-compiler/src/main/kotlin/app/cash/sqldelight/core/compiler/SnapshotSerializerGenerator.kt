package app.cash.sqldelight.core.compiler

import app.cash.sqldelight.core.capitalize
import app.cash.sqldelight.core.lang.psi.ColumnTypeMixin
import app.cash.sqldelight.core.lang.util.columnDefSource
import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.NamedElement
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

internal class SnapshotSerializerGenerator(
  private val dataClassPackageName: String,
  private val dataClass: TypeSpec,
  private val table: LazyQuery,
) {
  fun implementationSpec(): TypeSpec {
    val serializerName = "${table.tableName.name.capitalize()}Serializer"

    val typeSpec = TypeSpec.objectBuilder(serializerName)
      .addSuperinterface(TYPE_KSERIALIZER.parameterizedBy(ClassName(dataClassPackageName, dataClass.name!!)))

    val columns = table.query.columns.map { it.element as NamedElement }

    typeSpec.addProperty(PropertySpec.builder("descriptor", TYPE_DESCRIPTOR, OVERRIDE)
      .initializer(CodeBlock.builder()
        .beginControlFlow("%M(%S)", MEMBER_BUILD_CLASS_SERIAL_DESCRIPTOR, table.tableName.name)
        .apply {
          columns.forEach { column ->
            if (column.typeWithoutAnnotation.isPrimitive()) {
              addStatement("%M<%T>(%S)", MEMBER_DESCRIPTOR_ELEMENT, column.typeWithoutAnnotation, column.name)
            } else {
              // We kind of cheat a little by using a plain string descriptor for objects that probably don't have an available descriptor
              // TODO: Maybe check if a serializer is available before using a dummy descriptor
              addStatement("%M(%S, descriptor = %T(%S, %T))", MEMBER_DESCRIPTOR_ELEMENT, column.name, TYPE_PRIMITIVE_DESCRIPTOR, column.typeWithoutAnnotation, TYPE_PRIMITIVE_KIND_STRING)
            }
          }
        }
        .endControlFlow()
        .build())
      .build())

    val encodeFunction = FunSpec.builder("serialize")
      .addModifiers(OVERRIDE)
      .addParameter("encoder", TYPE_ENCODER)
      .addParameter("value", ClassName(dataClassPackageName, dataClass.name!!))
      .beginControlFlow("encoder.%M(descriptor)", MEMBER_ENCODE_STRUCTURE)
      .apply {
        dataClass.propertySpecs.forEachIndexed { index, propertySpec ->
          val value = if (propertySpec.type.isNullable) "%N" else "value.%N"
          if (propertySpec.type.isNullable) {
            beginControlFlow("value.%1N?.let { %1N -> ", propertySpec)
          }
          when (propertySpec.type.copy(nullable = false)) {
            Boolean::class.asTypeName() -> addStatement("encodeBooleanElement(descriptor, %L, $value)", index, propertySpec)
            Byte::class.asTypeName() -> addStatement("encodeByteElement(descriptor, %L, $value)", index, propertySpec)
            Char::class.asTypeName() -> addStatement("encodeCharElement(descriptor, %L, $value)", index, propertySpec)
            Double::class.asTypeName() -> addStatement("encodeDoubleElement(descriptor, %L, $value)", index, propertySpec)
            Float::class.asTypeName() -> addStatement("encodeFloatElement(descriptor, %L, $value)", index, propertySpec)
            Int::class.asTypeName() -> addStatement("encodeIntElement(descriptor, %L, $value)", index, propertySpec)
            Long::class.asTypeName() -> addStatement("encodeLongElement(descriptor, %L, $value)", index, propertySpec)
            Short::class.asTypeName() -> addStatement("encodeShortElement(descriptor, %L, $value)", index, propertySpec)
            String::class.asTypeName() -> addStatement("encodeStringElement(descriptor, %L, $value)", index, propertySpec)
            else -> {
              beginControlFlow("when (val serializer = serializersModule.getContextual(%T::class))", propertySpec.type.copy(nullable = false))
              addStatement("null -> encodeStringElement(descriptor, %L, value.%N.toString())", index, propertySpec)
              addStatement("else -> encodeSerializableElement(descriptor, %L, serializer, $value)", index, propertySpec)
              endControlFlow()
            }
          }
          if (propertySpec.type.isNullable) {
            endControlFlow()
          }
        }
      }.endControlFlow()
      .build()

    val decodeFunction = FunSpec.builder("deserialize")
      .addModifiers(OVERRIDE)
      .addParameter("decoder", TYPE_DECODER)
      .returns(ClassName(dataClassPackageName, dataClass.name!!))
      .addStatement("throw UnsupportedOperationException()")
      .build()

    return typeSpec.addFunction(encodeFunction)
      .addFunction(decodeFunction)
      .build()
  }

  private val NamedElement.typeWithoutAnnotation: TypeName
    get() {
      val columnDef = columnDefSource()!!
      val columnType = columnDef.columnType as ColumnTypeMixin
      val javaType = columnType.type().javaType
      return javaType.copy(annotations = emptyList())
    }

  private fun TypeName.isPrimitive(): Boolean = this.copy(nullable = false) in PRIMITIVE_TYPES

  companion object {
    private val PRIMITIVE_TYPES = setOf(
      Boolean::class.asTypeName(),
      Byte::class.asTypeName(),
      Char::class.asTypeName(),
      Double::class.asTypeName(),
      Float::class.asTypeName(),
      Int::class.asTypeName(),
      Long::class.asTypeName(),
      Short::class.asTypeName(),
      String::class.asTypeName(),
    )
    private val TYPE_KSERIALIZER = ClassName("kotlinx.serialization", "KSerializer")
    private val TYPE_ENCODER = ClassName("kotlinx.serialization.encoding", "Encoder")
    private val TYPE_DECODER = ClassName("kotlinx.serialization.encoding", "Decoder")
    private val TYPE_DESCRIPTOR = ClassName("kotlinx.serialization.descriptors", "SerialDescriptor")
    private val TYPE_PRIMITIVE_DESCRIPTOR = ClassName("kotlinx.serialization.descriptors", "PrimitiveSerialDescriptor")
    private val TYPE_PRIMITIVE_KIND_STRING = ClassName("kotlinx.serialization.descriptors", "PrimitiveKind").nestedClass("STRING")
    private val MEMBER_BUILD_CLASS_SERIAL_DESCRIPTOR = MemberName("kotlinx.serialization.descriptors", "buildClassSerialDescriptor")
    private val MEMBER_DESCRIPTOR_ELEMENT = MemberName("kotlinx.serialization.descriptors", "element")
    private val MEMBER_ENCODE_STRUCTURE = MemberName("kotlinx.serialization.encoding", "encodeStructure")
  }
}