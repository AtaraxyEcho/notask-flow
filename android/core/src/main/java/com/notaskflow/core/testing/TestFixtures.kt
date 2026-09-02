package com.notaskflow.core.testing

import com.notaskflow.core.model.AuthToken

object TestFixtures {
    val authToken = AuthToken(
        userId = 1L,
        tokenName = "Authorization",
        tokenValue = "test-token",
        expireTime = 1_800_000L
    )
}
