package com.bzcode.scorenge.data.auth

import com.bzcode.scorenge.data.local.dao.UserDao
import com.bzcode.scorenge.data.local.entity.UserEntity
import javax.inject.Inject

/** Keeps the users row for the signed-in account; other tables reference it. */
class LocalUserStore @Inject constructor(
    private val userDao: UserDao,
) {
    /** Inserts the user, or updates it only when the profile changed, so updatedAt stays meaningful for sync. */
    suspend fun save(user: AuthUser, nowMillis: Long = System.currentTimeMillis()) {
        val existing = userDao.getById(user.uid)
        if (existing == null) {
            userDao.upsert(
                UserEntity(
                    id = user.uid,
                    displayName = user.displayName,
                    email = user.email,
                    photoUrl = user.photoUrl,
                    createdAt = nowMillis,
                    updatedAt = nowMillis,
                ),
            )
        } else if (
            existing.displayName != user.displayName ||
            existing.email != user.email ||
            existing.photoUrl != user.photoUrl
        ) {
            userDao.upsert(
                existing.copy(
                    displayName = user.displayName,
                    email = user.email,
                    photoUrl = user.photoUrl,
                    updatedAt = nowMillis,
                ),
            )
        }
    }
}
