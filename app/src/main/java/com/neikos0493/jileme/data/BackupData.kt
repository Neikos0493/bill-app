package com.neikos0493.jileme.data

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.Instant

@Serializable
data class BackupData(
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val accounts: List<AccountEntity>,
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val links: List<TransactionLinkEntity>,
    val rules: List<CategoryRuleEntity>,
    val settings: Map<String,String> = emptyMap(),
)
