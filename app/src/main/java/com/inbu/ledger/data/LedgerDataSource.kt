package com.inbu.ledger.data

import com.inbu.ledger.ui.sites.SiteSummary
import com.inbu.ledger.ui.work.WorkRecordSummary
import com.inbu.ledger.ui.workers.WorkerSummary

interface LedgerDataSource {
    fun loadLedger(callback: (Result<LedgerSnapshot>) -> Unit)
    fun createSite(name: String, memo: String, callback: (Result<LedgerSnapshot>) -> Unit)
    fun updateSite(site: SiteSummary, callback: (Result<LedgerSnapshot>) -> Unit)
    fun trashSite(id: Long, callback: (Result<LedgerSnapshot>) -> Unit)
    fun restoreSite(id: Long, callback: (Result<LedgerSnapshot>) -> Unit)
    fun deleteSite(id: Long, callback: (Result<LedgerSnapshot>) -> Unit)
    fun createWorker(worker: WorkerSummary, callback: (Result<LedgerSnapshot>) -> Unit)
    fun updateWorker(worker: WorkerSummary, callback: (Result<LedgerSnapshot>) -> Unit)
    fun createRecord(record: WorkRecordSummary, callback: (Result<LedgerSnapshot>) -> Unit)
    fun updateRecord(record: WorkRecordSummary, callback: (Result<LedgerSnapshot>) -> Unit)
    fun trashRecord(id: Long, callback: (Result<LedgerSnapshot>) -> Unit)
    fun restoreRecord(id: Long, callback: (Result<LedgerSnapshot>) -> Unit)
    fun deleteRecord(id: Long, callback: (Result<LedgerSnapshot>) -> Unit)
    fun settlePayment(
        workerId: Long,
        siteId: Long,
        cutoffEpochDay: Long,
        callback: (Result<LedgerSnapshot>) -> Unit,
    )
    fun cancelPayment(id: Long, callback: (Result<LedgerSnapshot>) -> Unit)
}
