package com.inbu.ledger.ui.home

data class HomeUiState(
    val todayLabel: String,
    val records: List<WorkRecordSummary>,
)

data class WorkRecordSummary(
    val id: Long,
    val siteName: String,
    val dateLabel: String,
    val dateShortLabel: String,
    val workerCount: Int,
    val workUnits: Double,
    val totalWage: Long,
    val unpaidAmount: Long,
    val paymentState: PaymentState,
    val isToday: Boolean,
)

enum class PaymentState(val label: String) {
    Unpaid("미지급"),
    PartiallyPaid("일부 지급"),
    Paid("지급 완료"),
}
object HomeMockData {
    val state = HomeUiState(
        todayLabel = "7월 5일 일요일",
        records = listOf(
            WorkRecordSummary(
                id = 1,
                siteName = "마곡 오피스텔",
                dateLabel = "오늘",
                dateShortLabel = "7/5",
                workerCount = 7,
                workUnits = 6.5,
                totalWage = 1_040_000,
                unpaidAmount = 1_040_000,
                paymentState = PaymentState.Unpaid,
                isToday = true,
            ),
            WorkRecordSummary(
                id = 2,
                siteName = "김포 물류센터",
                dateLabel = "오늘",
                dateShortLabel = "7/5",
                workerCount = 4,
                workUnits = 4.0,
                totalWage = 640_000,
                unpaidAmount = 240_000,
                paymentState = PaymentState.PartiallyPaid,
                isToday = true,
            ),
            WorkRecordSummary(
                id = 3,
                siteName = "마곡 오피스텔",
                dateLabel = "어제",
                dateShortLabel = "7/4",
                workerCount = 6,
                workUnits = 6.0,
                totalWage = 960_000,
                unpaidAmount = 0,
                paymentState = PaymentState.Paid,
                isToday = false,
            ),
            WorkRecordSummary(
                id = 4,
                siteName = "파주 단독주택",
                dateLabel = "7월 3일",
                dateShortLabel = "7/3",
                workerCount = 5,
                workUnits = 4.5,
                totalWage = 720_000,
                unpaidAmount = 720_000,
                paymentState = PaymentState.Unpaid,
                isToday = false,
            ),
        ),
    )
}
