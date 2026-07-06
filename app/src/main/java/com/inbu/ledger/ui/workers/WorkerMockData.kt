package com.inbu.ledger.ui.workers

data class WorkerSummary(
    val id: Long,
    val name: String,
    val dailyWage: Long,
    val phone: String,
    val memo: String,
    val totalWorkUnits: Double,
    val unpaidAmount: Long,
    val isActive: Boolean,
    val siteDailyWages: Map<Long, Long> = emptyMap(),
)

fun WorkerSummary.dailyWageFor(siteId: Long): Long = siteDailyWages[siteId] ?: dailyWage

object WorkerMockData {
    val workers = listOf(
        WorkerSummary(
            id = 1,
            name = "김철수",
            dailyWage = 180_000,
            phone = "010-1234-5678",
            memo = "목수",
            totalWorkUnits = 18.5,
            unpaidAmount = 540_000,
            isActive = true,
            siteDailyWages = mapOf(
                1L to 220_000L,
                2L to 230_000L,
            ),
        ),
        WorkerSummary(
            id = 2,
            name = "박영수",
            dailyWage = 170_000,
            phone = "010-2345-6789",
            memo = "미장",
            totalWorkUnits = 15.0,
            unpaidAmount = 255_000,
            isActive = true,
            siteDailyWages = mapOf(1L to 175_000L),
        ),
        WorkerSummary(
            id = 3,
            name = "이민호",
            dailyWage = 160_000,
            phone = "010-3456-7890",
            memo = "보조",
            totalWorkUnits = 12.5,
            unpaidAmount = 0,
            isActive = true,
        ),
        WorkerSummary(
            id = 4,
            name = "최성호",
            dailyWage = 200_000,
            phone = "",
            memo = "철근",
            totalWorkUnits = 7.0,
            unpaidAmount = 400_000,
            isActive = false,
        ),
    )
}
