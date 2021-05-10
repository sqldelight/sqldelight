package com.squareup.sqldelight.intellij.refactoring

import com.intellij.refactoring.suggested.SignatureChangePresentationModel.TextFragment.Leaf
import com.intellij.refactoring.suggested.SignaturePresentationBuilder
import com.intellij.refactoring.suggested.SuggestedRefactoringSupport

class SqlDelightSignaturePresentationBuilder(
  signature: SuggestedRefactoringSupport.Signature,
  otherSignature: SuggestedRefactoringSupport.Signature,
  isOldSignature: Boolean
) : SignaturePresentationBuilder(signature, otherSignature, isOldSignature) {
  override fun buildPresentation() {

    fragments += leaf(signature.name, otherSignature.name)

    buildParameterList { fragments, parameter, correspondingParameter ->
      fragments += leaf(parameter.name, correspondingParameter?.name ?: parameter.name)

      fragments += Leaf(" ")

      fragments += leaf(parameter.type, correspondingParameter?.type ?: parameter.type)

      val additionalData = parameter.additionalData
      if (additionalData != null) {
        fragments += Leaf(" ")

        fragments += leaf(additionalData.toString(), correspondingParameter?.additionalData?.toString() ?: additionalData.toString())
      }
    }
  }
}
