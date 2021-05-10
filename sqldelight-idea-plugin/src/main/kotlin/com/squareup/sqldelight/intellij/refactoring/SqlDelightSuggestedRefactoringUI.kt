package com.squareup.sqldelight.intellij.refactoring

import com.intellij.psi.PsiCodeFragment
import com.intellij.refactoring.suggested.SignaturePresentationBuilder
import com.intellij.refactoring.suggested.SuggestedChangeSignatureData
import com.intellij.refactoring.suggested.SuggestedRefactoringExecution
import com.intellij.refactoring.suggested.SuggestedRefactoringSupport
import com.intellij.refactoring.suggested.SuggestedRefactoringUI

class SqlDelightSuggestedRefactoringUI : SuggestedRefactoringUI() {

  override fun createSignaturePresentationBuilder(
    signature: SuggestedRefactoringSupport.Signature,
    otherSignature: SuggestedRefactoringSupport.Signature,
    isOldSignature: Boolean
  ): SignaturePresentationBuilder {
    return SqlDelightSignaturePresentationBuilder(signature, otherSignature, isOldSignature)
  }

  override fun extractNewParameterData(data: SuggestedChangeSignatureData): List<NewParameterData> {
    return emptyList()
  }

  override fun extractValue(fragment: PsiCodeFragment): SuggestedRefactoringExecution.NewParameterValue.Expression? {
    return null
  }
}
