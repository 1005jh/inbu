package com.inbu.ledger.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.inbu.ledger.BuildConfig
import com.inbu.ledger.auth.KakaoLoginClient
import com.inbu.ledger.auth.KakaoLoginResult
import com.inbu.ledger.ui.auth.LoginScreen
import com.inbu.ledger.ui.home.HomeScreen
import com.inbu.ledger.ui.components.MainSection
import com.inbu.ledger.ui.sites.SitesScreen
import com.inbu.ledger.ui.sites.AddSiteScreen
import com.inbu.ledger.ui.sites.SiteDetailScreen
import com.inbu.ledger.ui.sites.SiteMockData
import com.inbu.ledger.ui.sites.SiteStatus
import com.inbu.ledger.ui.sites.SiteSummary
import com.inbu.ledger.ui.sites.SiteTrashScreen
import com.inbu.ledger.ui.workers.AddWorkerScreen
import com.inbu.ledger.ui.workers.WorkerMockData
import com.inbu.ledger.ui.workers.WorkerSummary
import com.inbu.ledger.ui.workers.WorkerDetailScreen
import com.inbu.ledger.ui.work.AddWorkRecordScreen
import com.inbu.ledger.ui.work.WorkRecordMockData
import com.inbu.ledger.ui.work.WorkRecordSummary
import com.inbu.ledger.ui.work.WorkRecordDetailScreen
import com.inbu.ledger.ui.work.WorkRecordTrashScreen
import com.inbu.ledger.ui.work.WorkRecordsScreen
import com.inbu.ledger.ui.workers.WorkersScreen
import com.inbu.ledger.ui.payment.PaymentAllocation
import com.inbu.ledger.ui.payment.PaymentMockData
import com.inbu.ledger.ui.payment.PaymentsScreen
import com.inbu.ledger.ui.payment.WorkerPayment
import com.inbu.ledger.ui.payment.WorkerPaymentScreen
import com.inbu.ledger.ui.payment.paidFor

private enum class AppScreen {
    Login,
    Home,
    Sites,
    AddSite,
    SiteDetail,
    Workers,
    AddWorker,
    WorkerDetail,
    WorkRecord,
    Records,
    RecordDetail,
    EditRecord,
    RecordTrash,
    Payments,
    WorkerPayment,
    SiteTrash,
}

@Composable
fun InbuLedgerApp() {
    val context = LocalContext.current
    val loginClient = remember { KakaoLoginClient() }
    var appScreen by rememberSaveable {
        mutableStateOf(if (BuildConfig.UI_REVIEW_MODE) AppScreen.Home else AppScreen.Login)
    }
    var isLoggingIn by rememberSaveable { mutableStateOf(false) }
    var loginMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var sites by remember { mutableStateOf(SiteMockData.sites) }
    var deletedSites by remember { mutableStateOf(emptyList<SiteSummary>()) }
    var selectedSiteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var workers by remember { mutableStateOf(WorkerMockData.workers) }
    var selectedWorkerId by rememberSaveable { mutableStateOf<Long?>(null) }
    var workRecords by remember { mutableStateOf(WorkRecordMockData.records) }
    var deletedWorkRecords by remember { mutableStateOf(emptyList<WorkRecordSummary>()) }
    var selectedRecordId by rememberSaveable { mutableStateOf<Long?>(null) }
    var payments by remember { mutableStateOf(PaymentMockData.payments) }
    var selectedPaymentSiteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedPaymentCutoff by rememberSaveable { mutableStateOf<Long?>(null) }
    var paymentBackToRecords by rememberSaveable { mutableStateOf(false) }
    val isKakaoConfigured = BuildConfig.KAKAO_NATIVE_APP_KEY.isNotBlank()
    val queryableSiteIds = sites.mapTo(mutableSetOf()) { it.id }
    val queryableWorkRecords = workRecords.filter { it.siteId in queryableSiteIds }
    val queryablePayments = payments.filter { it.siteId in queryableSiteIds }

    when (appScreen) {
        AppScreen.Login -> LoginScreen(
            isLoading = isLoggingIn,
            message = loginMessage,
            onKakaoLogin = {
                if (!isKakaoConfigured) {
                    loginMessage = "카카오 앱 키를 연결한 뒤 로그인할 수 있어요."
                    return@LoginScreen
                }

                loginMessage = null
                isLoggingIn = true
                loginClient.login(context) { result ->
                    isLoggingIn = false
                    when (result) {
                        is KakaoLoginResult.Success -> {
                            // 서버 구현 단계에서 accessToken을 서버 세션으로 교환합니다.
                            appScreen = AppScreen.Home
                        }
                        KakaoLoginResult.Cancelled -> Unit
                        is KakaoLoginResult.Failure -> {
                            loginMessage = "로그인하지 못했어요. 잠시 후 다시 시도해 주세요."
                        }
                    }
                }
            },
        )
        AppScreen.Home -> HomeScreen(
            onSectionSelected = { section ->
                when (section) {
                    MainSection.Work -> appScreen = AppScreen.WorkRecord
                    MainSection.Search -> appScreen = AppScreen.Records
                    MainSection.Manage -> appScreen = AppScreen.Sites
                    else -> Unit
                }
            },
        )
        AppScreen.Sites -> SitesScreen(
            sites = sites,
            onAddSite = { appScreen = AppScreen.AddSite },
            onOpenSite = { siteId ->
                selectedSiteId = siteId
                appScreen = AppScreen.SiteDetail
            },
            onOpenTrash = { appScreen = AppScreen.SiteTrash },
            onManageWorkers = { appScreen = AppScreen.Workers },
            onSectionSelected = { section ->
                when (section) {
                    MainSection.Home -> appScreen = AppScreen.Home
                    MainSection.Work -> appScreen = AppScreen.WorkRecord
                    MainSection.Search -> appScreen = AppScreen.Records
                    else -> Unit
                }
            },
        )
        AppScreen.AddSite -> AddSiteScreen(
            onBack = { appScreen = AppScreen.Sites },
            onSave = { name, memo ->
                val newSite = SiteSummary(
                    id = (sites.maxOfOrNull { it.id } ?: 0L) + 1L,
                    name = name,
                    memo = memo.ifBlank { "추가 정보 없음" },
                    status = SiteStatus.Active,
                    totalWorkUnits = 0.0,
                    unpaidAmount = 0,
                )
                sites = listOf(newSite) + sites
                appScreen = AppScreen.Sites
            },
        )
        AppScreen.SiteDetail -> {
            val selectedSite = sites.firstOrNull { it.id == selectedSiteId }
            if (selectedSite == null) {
                appScreen = AppScreen.Sites
            } else {
                SiteDetailScreen(
                    site = selectedSite,
                    onBack = { appScreen = AppScreen.Sites },
                    onSave = { updatedSite ->
                        sites = sites.map { site ->
                            if (site.id == updatedSite.id) updatedSite else site
                        }
                    },
                    onMoveToTrash = {
                        deletedSites = listOf(selectedSite) + deletedSites
                        sites = sites.filterNot { it.id == selectedSite.id }
                        selectedSiteId = null
                        appScreen = AppScreen.Sites
                    },
                )
            }
        }
        AppScreen.Workers -> WorkersScreen(
            workers = workers,
            onAddWorker = { appScreen = AppScreen.AddWorker },
            onOpenWorker = { workerId ->
                selectedWorkerId = workerId
                appScreen = AppScreen.WorkerDetail
            },
            onManageSites = { appScreen = AppScreen.Sites },
            onSectionSelected = { section ->
                when (section) {
                    MainSection.Home -> appScreen = AppScreen.Home
                    MainSection.Work -> appScreen = AppScreen.WorkRecord
                    MainSection.Search -> appScreen = AppScreen.Records
                    else -> Unit
                }
            },
        )
        AppScreen.AddWorker -> AddWorkerScreen(
            onBack = { appScreen = AppScreen.Workers },
            onSave = { name, dailyWage, phone, memo ->
                val newWorker = WorkerSummary(
                    id = (workers.maxOfOrNull { it.id } ?: 0L) + 1L,
                    name = name,
                    dailyWage = dailyWage,
                    phone = phone,
                    memo = memo,
                    totalWorkUnits = 0.0,
                    unpaidAmount = 0,
                    isActive = true,
                )
                workers = listOf(newWorker) + workers
                appScreen = AppScreen.Workers
            },
        )
        AppScreen.WorkerDetail -> {
            val selectedWorker = workers.firstOrNull { it.id == selectedWorkerId }
            if (selectedWorker == null) {
                appScreen = AppScreen.Workers
            } else {
                WorkerDetailScreen(
                    worker = selectedWorker,
                    sites = sites,
                    onBack = { appScreen = AppScreen.Workers },
                    onSave = { updatedWorker ->
                        workers = workers.map { worker ->
                            if (worker.id == updatedWorker.id) updatedWorker else worker
                        }
                    },
                    onActiveChange = { isActive ->
                        workers = workers.map { worker ->
                            if (worker.id == selectedWorker.id) worker.copy(isActive = isActive) else worker
                        }
                    },
                )
            }
        }
        AppScreen.WorkRecord -> AddWorkRecordScreen(
            sites = sites,
            workers = workers,
            records = workRecords,
            onSave = { dateEpochDay, siteId, workerAmounts, memo, fuelCost, mealCost ->
                val newRecord = WorkRecordSummary(
                    id = (workRecords.maxOfOrNull { it.id } ?: 0L) + 1L,
                    dateEpochDay = dateEpochDay,
                    siteId = siteId,
                    workers = workerAmounts,
                    memo = memo,
                    fuelCost = fuelCost,
                    mealCost = mealCost,
                )
                workRecords = listOf(newRecord) + workRecords

                workers = workers.applyWorkerRecord(newRecord, direction = 1)
                sites = sites.applySiteRecord(newRecord, direction = 1)
                appScreen = AppScreen.Home
            },
            onSectionSelected = { section ->
                when (section) {
                    MainSection.Home -> appScreen = AppScreen.Home
                    MainSection.Search -> appScreen = AppScreen.Records
                    MainSection.Manage -> appScreen = AppScreen.Sites
                    else -> Unit
                }
            },
        )
        AppScreen.Records -> WorkRecordsScreen(
            records = queryableWorkRecords,
            sites = sites,
            workers = workers,
            payments = queryablePayments,
            onOpenRecord = { recordId ->
                selectedRecordId = recordId
                appScreen = AppScreen.RecordDetail
            },
            onOpenTrash = { appScreen = AppScreen.RecordTrash },
            onOpenPayments = { appScreen = AppScreen.Payments },
            onOpenWorkerHistory = { workerId, _, endEpochDay, siteId ->
                selectedWorkerId = workerId
                selectedPaymentSiteId = siteId
                selectedPaymentCutoff = endEpochDay
                paymentBackToRecords = true
                appScreen = AppScreen.WorkerPayment
            },
            onSectionSelected = { section ->
                when (section) {
                    MainSection.Home -> appScreen = AppScreen.Home
                    MainSection.Work -> appScreen = AppScreen.WorkRecord
                    MainSection.Manage -> appScreen = AppScreen.Sites
                    else -> Unit
                }
            },
        )
        AppScreen.RecordDetail -> {
            val record = workRecords.firstOrNull { it.id == selectedRecordId }
            if (record == null) {
                appScreen = AppScreen.Records
            } else {
                WorkRecordDetailScreen(
                    record = record,
                    sites = sites,
                    workers = workers,
                    payments = payments,
                    isPaymentLinked = payments.any { payment ->
                        payment.allocations.any { it.recordId == record.id }
                    },
                    onBack = { appScreen = AppScreen.Records },
                    onEdit = { appScreen = AppScreen.EditRecord },
                    onMoveToTrash = {
                        workers = workers.applyWorkerRecord(record, direction = -1)
                        sites = sites.applySiteRecord(record, direction = -1)
                        workRecords = workRecords.filterNot { it.id == record.id }
                        deletedWorkRecords = listOf(record) + deletedWorkRecords
                        selectedRecordId = null
                        appScreen = AppScreen.Records
                    },
                )
            }
        }
        AppScreen.EditRecord -> {
            val originalRecord = workRecords.firstOrNull { it.id == selectedRecordId }
            if (originalRecord == null) {
                appScreen = AppScreen.Records
            } else {
                AddWorkRecordScreen(
                    sites = sites,
                    workers = workers,
                    records = workRecords,
                    initialRecord = originalRecord,
                    onBack = { appScreen = AppScreen.RecordDetail },
                    onSave = { dateEpochDay, siteId, workerAmounts, memo, fuelCost, mealCost ->
                        val updatedRecord = originalRecord.copy(
                            dateEpochDay = dateEpochDay,
                            siteId = siteId,
                            workers = workerAmounts,
                            memo = memo,
                            fuelCost = fuelCost,
                            mealCost = mealCost,
                        )
                        workers = workers
                            .applyWorkerRecord(originalRecord, direction = -1)
                            .applyWorkerRecord(updatedRecord, direction = 1)
                        sites = sites
                            .applySiteRecord(originalRecord, direction = -1)
                            .applySiteRecord(updatedRecord, direction = 1)
                        workRecords = workRecords.map { record ->
                            if (record.id == updatedRecord.id) updatedRecord else record
                        }
                        appScreen = AppScreen.RecordDetail
                    },
                )
            }
        }
        AppScreen.RecordTrash -> WorkRecordTrashScreen(
            records = deletedWorkRecords,
            sites = sites,
            protectedRecordIds = payments.flatMap { payment ->
                payment.allocations.map { it.recordId }
            }.toSet(),
            onBack = { appScreen = AppScreen.Records },
            onRestore = { recordId ->
                deletedWorkRecords.firstOrNull { it.id == recordId }?.let { record ->
                    workers = workers.applyWorkerRecord(record, direction = 1)
                    sites = sites.applySiteRecord(record, direction = 1)
                    workRecords = listOf(record) + workRecords
                    deletedWorkRecords = deletedWorkRecords.filterNot { it.id == recordId }
                }
            },
            onDeletePermanently = { recordId ->
                deletedWorkRecords = deletedWorkRecords.filterNot { it.id == recordId }
            },
        )
        AppScreen.Payments -> PaymentsScreen(
            records = queryableWorkRecords,
            workers = workers,
            payments = queryablePayments,
            onOpenWorker = { workerId ->
                selectedWorkerId = workerId
                selectedPaymentSiteId = null
                selectedPaymentCutoff = null
                paymentBackToRecords = false
                appScreen = AppScreen.WorkerPayment
            },
            onSectionSelected = { section ->
                when (section) {
                    MainSection.Home -> appScreen = AppScreen.Home
                    MainSection.Work -> appScreen = AppScreen.WorkRecord
                    MainSection.Search -> appScreen = AppScreen.Records
                    MainSection.Manage -> appScreen = AppScreen.Sites
                }
            },
        )
        AppScreen.WorkerPayment -> {
            val worker = workers.firstOrNull { it.id == selectedWorkerId }
            if (worker == null) {
                appScreen = AppScreen.Payments
            } else {
                WorkerPaymentScreen(
                    worker = worker,
                    records = queryableWorkRecords,
                    sites = sites,
                    payments = queryablePayments,
                    initialSiteId = selectedPaymentSiteId,
                    initialCutoffEpochDay = selectedPaymentCutoff,
                    onBack = {
                        appScreen = if (paymentBackToRecords) AppScreen.Records else AppScreen.Payments
                    },
                    onSettle = { siteId, cutoffEpochDay ->
                        val payment = settlePayment(
                            id = (payments.maxOfOrNull { it.id } ?: 0L) + 1L,
                            workerId = worker.id,
                            siteId = siteId,
                            cutoffEpochDay = cutoffEpochDay,
                            records = queryableWorkRecords,
                            existingPayments = queryablePayments,
                        )
                        if (payment.amount > 0) {
                            payments = listOf(payment) + payments
                            workers = workers.map { item ->
                                if (item.id == worker.id) {
                                    item.copy(unpaidAmount = (item.unpaidAmount - payment.amount).coerceAtLeast(0L))
                                } else {
                                    item
                                }
                            }
                            val paidBySite = payment.allocations.groupBy { allocation ->
                                workRecords.firstOrNull { it.id == allocation.recordId }?.siteId
                            }.mapNotNull { (siteId, allocations) ->
                                siteId?.let { it to allocations.sumOf { allocation -> allocation.amount } }
                            }.toMap()
                            sites = sites.map { site ->
                                val paidAmount = paidBySite[site.id] ?: return@map site
                                site.copy(unpaidAmount = (site.unpaidAmount - paidAmount).coerceAtLeast(0L))
                            }
                        }
                    },
                    onCancelPayment = { paymentId ->
                        payments.firstOrNull { it.id == paymentId }?.let { payment ->
                            payments = payments.filterNot { it.id == paymentId }
                            workers = workers.map { item ->
                                if (item.id == payment.workerId) {
                                    item.copy(unpaidAmount = item.unpaidAmount + payment.amount)
                                } else {
                                    item
                                }
                            }
                            sites = sites.map { site ->
                                if (site.id == payment.siteId) {
                                    site.copy(unpaidAmount = site.unpaidAmount + payment.amount)
                                } else {
                                    site
                                }
                            }
                        }
                    },
                )
            }
        }
        AppScreen.SiteTrash -> SiteTrashScreen(
            sites = deletedSites,
            protectedSiteIds = buildSet {
                addAll(workRecords.map { it.siteId })
                addAll(deletedWorkRecords.map { it.siteId })
                addAll(payments.map { it.siteId })
            },
            onBack = { appScreen = AppScreen.Sites },
            onRestore = { siteId ->
                deletedSites.firstOrNull { it.id == siteId }?.let { site ->
                    sites = listOf(site) + sites
                    deletedSites = deletedSites.filterNot { it.id == siteId }
                }
            },
            onDeletePermanently = { siteId ->
                deletedSites = deletedSites.filterNot { it.id == siteId }
            },
        )
    }
}

private fun settlePayment(
    id: Long,
    workerId: Long,
    siteId: Long,
    cutoffEpochDay: Long,
    records: List<WorkRecordSummary>,
    existingPayments: List<WorkerPayment>,
): WorkerPayment {
    val allocations = mutableListOf<PaymentAllocation>()
    records.sortedWith(compareBy<WorkRecordSummary> { it.dateEpochDay }.thenBy { it.id }).forEach { record ->
        if (record.siteId != siteId || record.dateEpochDay > cutoffEpochDay) return@forEach
        val work = record.workers.firstOrNull { it.workerId == workerId } ?: return@forEach
        val gross = (work.dailyWage * work.workUnits).toLong()
        val alreadyPaid = existingPayments.paidFor(record.id, workerId)
        val unpaid = (gross - alreadyPaid).coerceAtLeast(0L)
        if (unpaid <= 0) return@forEach
        allocations += PaymentAllocation(
            recordId = record.id,
            workerId = workerId,
            amount = unpaid,
        )
    }
    return WorkerPayment(
        id = id,
        workerId = workerId,
        siteId = siteId,
        settledThroughEpochDay = cutoffEpochDay,
        paidDateEpochDay = java.time.LocalDate.now().toEpochDay(),
        amount = allocations.sumOf { it.amount },
        allocations = allocations,
    )
}

private fun List<WorkerSummary>.applyWorkerRecord(
    record: WorkRecordSummary,
    direction: Int,
): List<WorkerSummary> = map { worker ->
    val amount = record.workers.firstOrNull { it.workerId == worker.id } ?: return@map worker
    worker.copy(
        totalWorkUnits = (worker.totalWorkUnits + direction * amount.workUnits).coerceAtLeast(0.0),
        unpaidAmount = (
            worker.unpaidAmount + direction * (amount.dailyWage * amount.workUnits).toLong()
            ).coerceAtLeast(0L),
    )
}

private fun List<SiteSummary>.applySiteRecord(
    record: WorkRecordSummary,
    direction: Int,
): List<SiteSummary> = map { site ->
    if (site.id != record.siteId) return@map site
    val workUnits = record.workers.sumOf { it.workUnits }
    val laborCost = record.workers.sumOf { (it.dailyWage * it.workUnits).toLong() }
    site.copy(
        totalWorkUnits = (site.totalWorkUnits + direction * workUnits).coerceAtLeast(0.0),
        unpaidAmount = (site.unpaidAmount + direction * laborCost).coerceAtLeast(0L),
    )
}
