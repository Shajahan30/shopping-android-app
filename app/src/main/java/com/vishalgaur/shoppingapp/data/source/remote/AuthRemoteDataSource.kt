package com.vishalgaur.shoppingapp.data.source.remote

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.vishalgaur.shoppingapp.data.Result
import com.vishalgaur.shoppingapp.data.Result.Error
import com.vishalgaur.shoppingapp.data.Result.Success
import com.vishalgaur.shoppingapp.data.UserData
import com.vishalgaur.shoppingapp.data.source.UserDataSource
import com.vishalgaur.shoppingapp.data.utils.EmailMobileData
import kotlinx.coroutines.tasks.await

class AuthRemoteDataSource : UserDataSource {
	private val firebaseDb: FirebaseFirestore = Firebase.firestore

	private fun usersCollectionRef() = firebaseDb.collection(USERS_COLLECTION)
	private fun allEmailsMobilesRef() =
		firebaseDb.collection(USERS_COLLECTION).document(EMAIL_MOBILE_DOC)


	override suspend fun getUserById(userId: String): Result<UserData?> {
		val resRef = usersCollectionRef().whereEqualTo(USERS_ID_FIELD, userId).get()
		return if (resRef.isSuccessful) {
			Success(resRef.result?.documents?.get(0)?.toObject(UserData::class.java))
		} else {
			Error(Exception(resRef.exception))
		}
	}


	override suspend fun addUser(userData: UserData) {
		usersCollectionRef().add(userData.toHashMap())
			.addOnSuccessListener {
				Log.d(TAG, "Doc added")
			}
			.addOnFailureListener { e ->
				Log.d(TAG, "firestore error occurred: $e")
			}
	}

	override suspend fun getUserByMobile(phoneNumber: String): UserData =
		usersCollectionRef().whereEqualTo(USERS_MOBILE_FIELD, phoneNumber).get().await()
			.toObjects(UserData::class.java)[0]

	override suspend fun getUserByMobileAndPassword(
		mobile: String,
		password: String
	): MutableList<UserData> =
		usersCollectionRef().whereEqualTo(USERS_MOBILE_FIELD, mobile)
			.whereEqualTo(USERS_PWD_FIELD, password).get().await().toObjects(UserData::class.java)

	override fun updateEmailsAndMobiles(email: String, mobile: String) {
		allEmailsMobilesRef().update(EMAIL_MOBILE_EMAIL_FIELD, FieldValue.arrayUnion(email))
		allEmailsMobilesRef().update(EMAIL_MOBILE_MOB_FIELD, FieldValue.arrayUnion(mobile))
	}

	override suspend fun getEmailsAndMobiles() = allEmailsMobilesRef().get().await().toObject(
		EmailMobileData::class.java
	)

	companion object {
		private const val USERS_COLLECTION = "users"
		private const val USERS_ID_FIELD = "userId"
		private const val USERS_MOBILE_FIELD = "mobile"
		private const val USERS_PWD_FIELD = "password"
		private const val EMAIL_MOBILE_DOC = "emailAndMobiles"
		private const val EMAIL_MOBILE_EMAIL_FIELD = "emails"
		private const val EMAIL_MOBILE_MOB_FIELD = "mobiles"
		private const val TAG = "AuthRemoteDataSource"
	}
}