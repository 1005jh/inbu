package com.inbu.ledger.ui.sites

data class SiteSummary(
    val id: Long,
    val name: String,
    val memo: String,
    val status: SiteStatus,
    val totalWorkUnits: Double,
    val unpaidAmount: Long,
)

enum class SiteStatus(val label: String) {
    Active("진행 중"),
    Completed("종료"),
}

object SiteMockData {
    val sites = listOf(
        SiteSummary(
            id = 1,
            name = "마곡 오피스텔",
            memo = "서울 강서구 · 골조 공사",
            status = SiteStatus.Active,
            totalWorkUnits = 42.5,
            unpaidAmount = 1_040_000,
        ),
        SiteSummary(
            id = 2,
            name = "김포 물류센터",
            memo = "김포 고촌읍 · 내부 마감",
            status = SiteStatus.Active,
            totalWorkUnits = 31.0,
            unpaidAmount = 240_000,
        ),
        SiteSummary(
            id = 3,
            name = "파주 단독주택",
            memo = "파주 운정 · 외부 미장",
            status = SiteStatus.Active,
            totalWorkUnits = 18.5,
            unpaidAmount = 720_000,
        ),
        SiteSummary(
            id = 4,
            name = "일산 상가 리모델링",
            memo = "6월 18일 작업 종료",
            status = SiteStatus.Completed,
            totalWorkUnits = 27.0,
            unpaidAmount = 0,
        ),
    )
}
