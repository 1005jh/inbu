package com.inbu.ledger.ui.payment

import java.time.LocalDate

data class PaymentAllocation(
    val recordId: Long,
    val workerId: Long,
    val amount: Long,
)

data class WorkerPayment(
    val id: Long,
    val workerId: Long,
    val siteId: Long,
    val settledThroughEpochDay: Long,
    val paidDateEpochDay: Long,
    val amount: Long,
    val allocations: List<PaymentAllocation>,
)

enum class PaymentStatus(val label: String) {
    Unpaid("미지급"),
    Partial("일부 지급"),
    Paid("지급 완료"),
}

object PaymentMockData {
    val payments = listOf(
        WorkerPayment(
            id = 1,
            workerId = 2,
            siteId = 1,
            settledThroughEpochDay = LocalDate.now().minusDays(3).toEpochDay(),
            paidDateEpochDay = LocalDate.now().minusDays(1).toEpochDay(),
            amount = 175_000,
            allocations = listOf(
                PaymentAllocation(recordId = 3, workerId = 2, amount = 175_000),
            ),
        ),
    )
}
