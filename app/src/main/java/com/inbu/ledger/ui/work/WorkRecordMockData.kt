package com.inbu.ledger.ui.work

import java.time.LocalDate

data class WorkerWorkAmount(
    val workerId: Long,
    val workUnits: Double,
    val dailyWage: Long,
)

data class WorkRecordSummary(
    val id: Long,
    val dateEpochDay: Long,
    val siteId: Long,
    val workers: List<WorkerWorkAmount>,
    val memo: String,
    val fuelCost: Long,
    val mealCost: Long,
)

object WorkRecordMockData {
    val records = listOf(
        WorkRecordSummary(
            id = 1,
            dateEpochDay = LocalDate.now().toEpochDay(),
            siteId = 1,
            workers = listOf(
                WorkerWorkAmount(workerId = 1, workUnits = 1.0, dailyWage = 220_000),
            ),
            memo = "골조 작업",
            fuelCost = 30_000,
            mealCost = 24_000,
        ),
        WorkRecordSummary(
            id = 2,
            dateEpochDay = LocalDate.now().minusDays(1).toEpochDay(),
            siteId = 2,
            workers = listOf(
                WorkerWorkAmount(workerId = 2, workUnits = 1.5, dailyWage = 170_000),
                WorkerWorkAmount(workerId = 3, workUnits = 0.5, dailyWage = 160_000),
            ),
            memo = "내부 마감 및 자재 정리",
            fuelCost = 40_000,
            mealCost = 36_000,
        ),
        WorkRecordSummary(
            id = 3,
            dateEpochDay = LocalDate.now().minusDays(3).toEpochDay(),
            siteId = 1,
            workers = listOf(
                WorkerWorkAmount(workerId = 1, workUnits = 1.5, dailyWage = 220_000),
                WorkerWorkAmount(workerId = 2, workUnits = 1.0, dailyWage = 175_000),
            ),
            memo = "2층 골조 작업",
            fuelCost = 30_000,
            mealCost = 30_000,
        ),
        WorkRecordSummary(
            id = 4,
            dateEpochDay = LocalDate.now().minusDays(7).toEpochDay(),
            siteId = 3,
            workers = listOf(
                WorkerWorkAmount(workerId = 3, workUnits = 1.0, dailyWage = 160_000),
            ),
            memo = "외부 미장 보조",
            fuelCost = 20_000,
            mealCost = 12_000,
        ),
        WorkRecordSummary(
            id = 5,
            dateEpochDay = LocalDate.now().minusDays(2).toEpochDay(),
            siteId = 2,
            workers = listOf(
                WorkerWorkAmount(workerId = 1, workUnits = 1.0, dailyWage = 230_000),
            ),
            memo = "물류센터 골조 보강",
            fuelCost = 25_000,
            mealCost = 12_000,
        ),
    )
}
