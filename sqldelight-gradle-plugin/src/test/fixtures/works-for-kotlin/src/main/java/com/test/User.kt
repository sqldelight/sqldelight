package com.test

import com.squareup.sqldelight.prerelease.EnumColumnAdapter;

class User(
    private val id: Long,
    private val firstName: String,
    private val middleInitial: String?,
    private val lastName: String,
    private val age: Int,
    private val gender: Gender
) : UserModel {
  override fun id() = id
  override fun first_name() = firstName
  override fun middle_initial() = middleInitial
  override fun last_name() = lastName
  override fun age() = age
  override fun gender() = gender

  companion object {
    val GENDER_ADAPTER = EnumColumnAdapter.create(Gender::class.java);
    val FACTORY: UserModel.Factory<User> = UserModel.Factory(::User, GENDER_ADAPTER)
  }
}
