package com.inbu.ledger.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Handler
import android.os.Looper
import com.inbu.ledger.ui.payment.PaymentAllocation
import com.inbu.ledger.ui.payment.WorkerPayment
import com.inbu.ledger.ui.sites.SiteStatus
import com.inbu.ledger.ui.sites.SiteSummary
import com.inbu.ledger.ui.work.WorkRecordSummary
import com.inbu.ledger.ui.work.WorkerWorkAmount
import com.inbu.ledger.ui.workers.WorkerSummary
import java.time.LocalDate
import java.util.concurrent.Executors

class LocalLedgerDataSource(context: Context) : LedgerDataSource {
    private val database = LocalLedgerDatabase(context.applicationContext)
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun loadLedger(callback: (Result<LedgerSnapshot>) -> Unit) = execute(callback) {
        database.readSnapshot()
    }

    override fun createSite(name: String, memo: String, callback: (Result<LedgerSnapshot>) -> Unit) = mutate(callback) { db ->
        requireLocal(name.isNotBlank(), "현장명을 입력해 주세요.")
        db.insertOrThrow(
            "sites",
            null,
            ContentValues().apply {
                put("name", name.trim())
                put("memo", memo.trim())
                put("status", SiteStatus.Active.name)
                put("deleted", 0)
            },
        )
    }

    override fun updateSite(site: SiteSummary, callback: (Result<LedgerSnapshot>) -> Unit) = mutate(callback) { db ->
        requireLocal(site.name.isNotBlank(), "현장명을 입력해 주세요.")
        val changed = db.update(
            "sites",
            ContentValues().apply {
                put("name", site.name.trim())
                put("memo", site.memo.trim())
                put("status", site.status.name)
            },
            "id=? AND deleted=0",
            arrayOf(site.id.toString()),
        )
        requireLocal(changed == 1, "현장을 찾을 수 없어요.")
    }

    override fun trashSite(id: Long, callback: (Result<LedgerSnapshot>) -> Unit) = mutate(callback) { db ->
        val changed = db.update(
            "sites",
            ContentValues().apply { put("deleted", 1) },
            "id=? AND deleted=0",
            arrayOf(id.toString()),
        )
        requireLocal(changed == 1, "현장을 찾을 수 없어요.")
    }

    override fun restoreSite(id: Long, callback: (Result<LedgerSnapshot>) -> Unit) = mutate(callback) { db ->
        val changed = db.update(
            "sites",
            ContentValues().apply { put("deleted", 0) },
            "id=? AND deleted=1",
            arrayOf(id.toString()),
        )
        requireLocal(changed == 1, "복원할 현장을 찾을 수 없어요.")
    }

    override fun deleteSite(id: Long, callback: (Result<LedgerSnapshot>) -> Unit) = mutate(callback) { db ->
        requireLocal(db.count("sites", "id=? AND deleted=1", id) == 1L, "삭제할 현장을 찾을 수 없어요.")
        val references = db.count("work_records", "site_id=?", id) + db.count("payments", "site_id=?", id)
        requireLocal(references == 0L, "작업 기록이나 지급 내역이 있는 현장은 영구 삭제할 수 없어요.")
        db.delete("sites", "id=?", arrayOf(id.toString()))
    }

    override fun createWorker(worker: WorkerSummary, callback: (Result<LedgerSnapshot>) -> Unit) = mutate(callback) { db ->
        validateWorker(worker)
        val workerId = db.insertOrThrow("workers", null, worker.values())
        replaceSiteWages(db, workerId, worker.siteDailyWages)
    }

    override fun updateWorker(worker: WorkerSummary, callback: (Result<LedgerSnapshot>) -> Unit) = mutate(callback) { db ->
        validateWorker(worker)
        val changed = db.update("workers", worker.values(), "id=?", arrayOf(worker.id.toString()))
        requireLocal(changed == 1, "인부를 찾을 수 없어요.")
        replaceSiteWages(db, worker.id, worker.siteDailyWages)
    }

    override fun createRecord(record: WorkRecordSummary, callback: (Result<LedgerSnapshot>) -> Unit) = mutate(callback) { db ->
        validateRecord(db, record, excludingRecordId = null)
        val recordId = db.insertOrThrow("work_records", null, record.values())
        insertRecordWorkers(db, recordId, record.workers)
    }

    override fun updateRecord(record: WorkRecordSummary, callback: (Result<LedgerSnapshot>) -> Unit) = mutate(callback) { db ->
        validateRecord(db, record, excludingRecordId = record.id)
        requireLocal(db.count("payment_allocations", "record_id=?", record.id) == 0L, "지급 내역과 연결된 작업 기록은 수정할 수 없어요.")
        val changed = db.update(
            "work_records",
            record.values(),
            "id=? AND deleted=0",
            arrayOf(record.id.toString()),
        )
        requireLocal(changed == 1, "작업 기록을 찾을 수 없어요.")
        db.delete("record_workers", "record_id=?", arrayOf(record.id.toString()))
        insertRecordWorkers(db, record.id, record.workers)
    }

    override fun trashRecord(id: Long, callback: (Result<LedgerSnapshot>) -> Unit) = mutate(callback) { db ->
        requireLocal(db.count("payment_allocations", "record_id=?", id) == 0L, "지급 내역과 연결된 작업 기록은 휴지통으로 옮길 수 없어요.")
        val changed = db.update(
            "work_records",
            ContentValues().apply { put("deleted", 1) },
            "id=? AND deleted=0",
            arrayOf(id.toString()),
        )
        requireLocal(changed == 1, "작업 기록을 찾을 수 없어요.")
    }

    override fun restoreRecord(id: Long, callback: (Result<LedgerSnapshot>) -> Unit) = mutate(callback) { db ->
        val changed = db.update(
            "work_records",
            ContentValues().apply { put("deleted", 0) },
            "id=? AND deleted=1",
            arrayOf(id.toString()),
        )
        requireLocal(changed == 1, "복원할 작업 기록을 찾을 수 없어요.")
    }

    override fun deleteRecord(id: Long, callback: (Result<LedgerSnapshot>) -> Unit) = mutate(callback) { db ->
        requireLocal(db.count("payment_allocations", "record_id=?", id) == 0L, "지급 내역과 연결된 작업 기록은 영구 삭제할 수 없어요.")
        val changed = db.delete("work_records", "id=? AND deleted=1", arrayOf(id.toString()))
        requireLocal(changed == 1, "삭제할 작업 기록을 찾을 수 없어요.")
    }

    override fun settlePayment(
        workerId: Long,
        siteId: Long,
        cutoffEpochDay: Long,
        callback: (Result<LedgerSnapshot>) -> Unit,
    ) = mutate(callback) { db ->
        requireLocal(db.count("workers", "id=?", workerId) == 1L, "인부를 찾을 수 없어요.")
        requireLocal(db.count("sites", "id=? AND deleted=0", siteId) == 1L, "현장을 찾을 수 없어요.")
        val snapshot = database.readSnapshot(db)
        val allocations = mutableListOf<PaymentAllocation>()
        snapshot.workRecords
            .asSequence()
            .filter { it.siteId == siteId && it.dateEpochDay <= cutoffEpochDay }
            .sortedWith(compareBy<WorkRecordSummary> { it.dateEpochDay }.thenBy { it.id })
            .forEach { record ->
                val work = record.workers.firstOrNull { it.workerId == workerId } ?: return@forEach
                val gross = (work.dailyWage * work.workUnits).toLong()
                val paid = snapshot.payments.sumOf { payment ->
                    payment.allocations.filter { it.recordId == record.id && it.workerId == workerId }.sumOf { it.amount }
                }
                val unpaid = (gross - paid).coerceAtLeast(0L)
                if (unpaid > 0L) allocations += PaymentAllocation(record.id, workerId, unpaid)
            }
        requireLocal(allocations.isNotEmpty(), "선택한 기준일까지 지급할 미지급액이 없어요.")
        val paymentId = db.insertOrThrow(
            "payments",
            null,
            ContentValues().apply {
                put("worker_id", workerId)
                put("site_id", siteId)
                put("settled_through_epoch_day", cutoffEpochDay)
                put("paid_date_epoch_day", LocalDate.now().toEpochDay())
            },
        )
        allocations.forEach { allocation ->
            db.insertOrThrow(
                "payment_allocations",
                null,
                ContentValues().apply {
                    put("payment_id", paymentId)
                    put("record_id", allocation.recordId)
                    put("worker_id", allocation.workerId)
                    put("amount", allocation.amount)
                },
            )
        }
    }

    override fun cancelPayment(id: Long, callback: (Result<LedgerSnapshot>) -> Unit) = mutate(callback) { db ->
        val changed = db.delete("payments", "id=?", arrayOf(id.toString()))
        requireLocal(changed == 1, "지급 내역을 찾을 수 없어요.")
    }

    private fun mutate(
        callback: (Result<LedgerSnapshot>) -> Unit,
        block: (SQLiteDatabase) -> Unit,
    ) = execute(callback) {
        database.transaction { db ->
            block(db)
            database.readSnapshot(db)
        }
    }

    private fun <T> execute(callback: (Result<T>) -> Unit, block: () -> T) {
        executor.execute {
            val result = runCatching(block)
            mainHandler.post { callback(result) }
        }
    }

    private fun validateWorker(worker: WorkerSummary) {
        requireLocal(worker.name.isNotBlank(), "이름을 입력해 주세요.")
        requireLocal(worker.dailyWage >= 0L, "일당은 0원 이상이어야 해요.")
        requireLocal(worker.siteDailyWages.values.none { it < 0L }, "현장별 일당은 0원 이상이어야 해요.")
    }

    private fun validateRecord(db: SQLiteDatabase, record: WorkRecordSummary, excludingRecordId: Long?) {
        requireLocal(record.workers.isNotEmpty(), "인부를 한 명 이상 선택해 주세요.")
        requireLocal(record.workers.map { it.workerId }.distinct().size == record.workers.size, "같은 인부를 중복으로 선택할 수 없어요.")
        requireLocal(record.workers.all { it.workUnits in setOf(0.5, 1.0, 1.5) }, "공수는 0.5, 1, 1.5 중에서 선택해 주세요.")
        requireLocal(record.workers.all { it.dailyWage >= 0L }, "일당은 0원 이상이어야 해요.")
        requireLocal(record.fuelCost >= 0L && record.mealCost >= 0L, "경비는 0원 이상이어야 해요.")
        requireLocal(db.count("sites", "id=? AND deleted=0", record.siteId) == 1L, "사용할 수 없는 현장이에요.")
        record.workers.forEach { work ->
            requireLocal(db.count("workers", "id=?", work.workerId) == 1L, "잘못된 인부가 포함되어 있어요.")
            val args = mutableListOf(
                record.siteId.toString(),
                record.dateEpochDay.toString(),
                work.workerId.toString(),
            )
            val excluding = if (excludingRecordId == null) "" else " AND r.id<>?".also { args += excludingRecordId.toString() }
            val duplicate = db.rawQuery(
                """SELECT COUNT(*) FROM work_records r
                    JOIN record_workers rw ON rw.record_id=r.id
                    WHERE r.site_id=? AND r.date_epoch_day=? AND rw.worker_id=? AND r.deleted=0$excluding""",
                args.toTypedArray(),
            ).use { cursor -> cursor.moveToFirst(); cursor.getLong(0) }
            requireLocal(duplicate == 0L, "같은 날짜와 현장에 이미 등록된 인부가 있어요.")
        }
    }

    private fun replaceSiteWages(db: SQLiteDatabase, workerId: Long, wages: Map<Long, Long>) {
        db.delete("worker_site_wages", "worker_id=?", arrayOf(workerId.toString()))
        wages.forEach { (siteId, wage) ->
            requireLocal(db.count("sites", "id=?", siteId) == 1L, "현장별 일당에 잘못된 현장이 포함되어 있어요.")
            db.insertOrThrow(
                "worker_site_wages",
                null,
                ContentValues().apply {
                    put("worker_id", workerId)
                    put("site_id", siteId)
                    put("daily_wage", wage)
                },
            )
        }
    }

    private fun insertRecordWorkers(db: SQLiteDatabase, recordId: Long, workers: List<WorkerWorkAmount>) {
        workers.forEach { work ->
            db.insertOrThrow(
                "record_workers",
                null,
                ContentValues().apply {
                    put("record_id", recordId)
                    put("worker_id", work.workerId)
                    put("work_units", work.workUnits)
                    put("daily_wage", work.dailyWage)
                },
            )
        }
    }

    private fun WorkerSummary.values() = ContentValues().apply {
        put("name", name.trim())
        put("daily_wage", dailyWage)
        put("phone", phone.trim())
        put("memo", memo.trim())
        put("is_active", if (isActive) 1 else 0)
    }

    private fun WorkRecordSummary.values() = ContentValues().apply {
        put("date_epoch_day", dateEpochDay)
        put("site_id", siteId)
        put("memo", memo.trim())
        put("fuel_cost", fuelCost)
        put("meal_cost", mealCost)
        put("deleted", 0)
    }
}

private class LocalLedgerDatabase(context: Context) : SQLiteOpenHelper(context, "inbu_ledger.db", null, 1) {
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE sites(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, memo TEXT NOT NULL DEFAULT '', status TEXT NOT NULL, deleted INTEGER NOT NULL DEFAULT 0)")
        db.execSQL("CREATE TABLE workers(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, daily_wage INTEGER NOT NULL, phone TEXT NOT NULL DEFAULT '', memo TEXT NOT NULL DEFAULT '', is_active INTEGER NOT NULL DEFAULT 1)")
        db.execSQL("CREATE TABLE worker_site_wages(worker_id INTEGER NOT NULL REFERENCES workers(id) ON DELETE CASCADE, site_id INTEGER NOT NULL REFERENCES sites(id) ON DELETE CASCADE, daily_wage INTEGER NOT NULL, PRIMARY KEY(worker_id,site_id))")
        db.execSQL("CREATE TABLE work_records(id INTEGER PRIMARY KEY AUTOINCREMENT, site_id INTEGER NOT NULL REFERENCES sites(id), date_epoch_day INTEGER NOT NULL, memo TEXT NOT NULL DEFAULT '', fuel_cost INTEGER NOT NULL DEFAULT 0, meal_cost INTEGER NOT NULL DEFAULT 0, deleted INTEGER NOT NULL DEFAULT 0)")
        db.execSQL("CREATE TABLE record_workers(record_id INTEGER NOT NULL REFERENCES work_records(id) ON DELETE CASCADE, worker_id INTEGER NOT NULL REFERENCES workers(id), work_units REAL NOT NULL, daily_wage INTEGER NOT NULL, PRIMARY KEY(record_id,worker_id))")
        db.execSQL("CREATE TABLE payments(id INTEGER PRIMARY KEY AUTOINCREMENT, worker_id INTEGER NOT NULL REFERENCES workers(id), site_id INTEGER NOT NULL REFERENCES sites(id), settled_through_epoch_day INTEGER NOT NULL, paid_date_epoch_day INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE payment_allocations(payment_id INTEGER NOT NULL REFERENCES payments(id) ON DELETE CASCADE, record_id INTEGER NOT NULL REFERENCES work_records(id), worker_id INTEGER NOT NULL REFERENCES workers(id), amount INTEGER NOT NULL, PRIMARY KEY(payment_id,record_id,worker_id))")
        db.execSQL("CREATE INDEX idx_records_site_date ON work_records(site_id,date_epoch_day)")
        db.execSQL("CREATE INDEX idx_allocations_record_worker ON payment_allocations(record_id,worker_id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun <T> transaction(block: (SQLiteDatabase) -> T): T {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            block(db).also { db.setTransactionSuccessful() }
        } finally {
            db.endTransaction()
        }
    }

    fun readSnapshot(): LedgerSnapshot = readSnapshot(readableDatabase)

    fun readSnapshot(db: SQLiteDatabase): LedgerSnapshot {
        val siteRows = mutableListOf<Pair<SiteSummary, Boolean>>()
        db.rawQuery("SELECT id,name,memo,status,deleted FROM sites ORDER BY id DESC", null).use { cursor ->
            while (cursor.moveToNext()) {
                siteRows += SiteSummary(
                    id = cursor.long("id"),
                    name = cursor.string("name"),
                    memo = cursor.string("memo"),
                    status = SiteStatus.valueOf(cursor.string("status")),
                    totalWorkUnits = 0.0,
                    unpaidAmount = 0L,
                ) to (cursor.int("deleted") == 1)
            }
        }

        val wages = mutableMapOf<Long, MutableMap<Long, Long>>()
        db.rawQuery("SELECT worker_id,site_id,daily_wage FROM worker_site_wages", null).use { cursor ->
            while (cursor.moveToNext()) {
                wages.getOrPut(cursor.long("worker_id"), ::mutableMapOf)[cursor.long("site_id")] = cursor.long("daily_wage")
            }
        }
        val workerRows = mutableListOf<WorkerSummary>()
        db.rawQuery("SELECT id,name,daily_wage,phone,memo,is_active FROM workers ORDER BY id DESC", null).use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.long("id")
                workerRows += WorkerSummary(
                    id = id,
                    name = cursor.string("name"),
                    dailyWage = cursor.long("daily_wage"),
                    phone = cursor.string("phone"),
                    memo = cursor.string("memo"),
                    totalWorkUnits = 0.0,
                    unpaidAmount = 0L,
                    isActive = cursor.int("is_active") == 1,
                    siteDailyWages = wages[id].orEmpty(),
                )
            }
        }

        val recordRows = mutableListOf<Pair<WorkRecordSummary, Boolean>>()
        db.rawQuery("SELECT id,date_epoch_day,site_id,memo,fuel_cost,meal_cost,deleted FROM work_records ORDER BY date_epoch_day DESC,id DESC", null).use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.long("id")
                val amounts = mutableListOf<WorkerWorkAmount>()
                db.rawQuery("SELECT worker_id,work_units,daily_wage FROM record_workers WHERE record_id=? ORDER BY worker_id", arrayOf(id.toString())).use { workCursor ->
                    while (workCursor.moveToNext()) {
                        amounts += WorkerWorkAmount(workCursor.long("worker_id"), workCursor.double("work_units"), workCursor.long("daily_wage"))
                    }
                }
                recordRows += WorkRecordSummary(
                    id = id,
                    dateEpochDay = cursor.long("date_epoch_day"),
                    siteId = cursor.long("site_id"),
                    workers = amounts,
                    memo = cursor.string("memo"),
                    fuelCost = cursor.long("fuel_cost"),
                    mealCost = cursor.long("meal_cost"),
                ) to (cursor.int("deleted") == 1)
            }
        }

        val payments = mutableListOf<WorkerPayment>()
        db.rawQuery("SELECT id,worker_id,site_id,settled_through_epoch_day,paid_date_epoch_day FROM payments ORDER BY paid_date_epoch_day DESC,id DESC", null).use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.long("id")
                val allocations = mutableListOf<PaymentAllocation>()
                db.rawQuery("SELECT record_id,worker_id,amount FROM payment_allocations WHERE payment_id=? ORDER BY record_id", arrayOf(id.toString())).use { allocationCursor ->
                    while (allocationCursor.moveToNext()) {
                        allocations += PaymentAllocation(allocationCursor.long("record_id"), allocationCursor.long("worker_id"), allocationCursor.long("amount"))
                    }
                }
                payments += WorkerPayment(
                    id = id,
                    workerId = cursor.long("worker_id"),
                    siteId = cursor.long("site_id"),
                    settledThroughEpochDay = cursor.long("settled_through_epoch_day"),
                    paidDateEpochDay = cursor.long("paid_date_epoch_day"),
                    amount = allocations.sumOf { it.amount },
                    allocations = allocations,
                )
            }
        }

        val activeRecords = recordRows.filterNot { it.second }.map { it.first }
        val paidByWork = payments.flatMap { it.allocations }
            .groupBy { it.recordId to it.workerId }
            .mapValues { (_, values) -> values.sumOf { it.amount } }
        val sites = siteRows.map { (site, deleted) ->
            val records = activeRecords.filter { it.siteId == site.id }
            val gross = records.sumOf { record -> record.workers.sumOf { (it.dailyWage * it.workUnits).toLong() } }
            val paid = records.sumOf { record -> record.workers.sumOf { paidByWork[record.id to it.workerId] ?: 0L } }
            site.copy(
                totalWorkUnits = records.sumOf { it.workers.sumOf { amount -> amount.workUnits } },
                unpaidAmount = (gross - paid).coerceAtLeast(0L),
            ) to deleted
        }
        val workers = workerRows.map { worker ->
            val works = activeRecords.mapNotNull { record ->
                record.workers.firstOrNull { it.workerId == worker.id }?.let { record.id to it }
            }
            val gross = works.sumOf { (_, work) -> (work.dailyWage * work.workUnits).toLong() }
            val paid = works.sumOf { (recordId, _) -> paidByWork[recordId to worker.id] ?: 0L }
            worker.copy(
                totalWorkUnits = works.sumOf { it.second.workUnits },
                unpaidAmount = (gross - paid).coerceAtLeast(0L),
            )
        }
        return LedgerSnapshot(
            sites = sites.filterNot { it.second }.map { it.first },
            deletedSites = sites.filter { it.second }.map { it.first },
            workers = workers,
            workRecords = activeRecords,
            deletedWorkRecords = recordRows.filter { it.second }.map { it.first },
            payments = payments,
        )
    }
}

private fun SQLiteDatabase.count(table: String, selection: String, value: Long): Long =
    rawQuery("SELECT COUNT(*) FROM $table WHERE $selection", arrayOf(value.toString())).use { cursor ->
        cursor.moveToFirst()
        cursor.getLong(0)
    }

private fun Cursor.long(column: String): Long = getLong(getColumnIndexOrThrow(column))
private fun Cursor.int(column: String): Int = getInt(getColumnIndexOrThrow(column))
private fun Cursor.double(column: String): Double = getDouble(getColumnIndexOrThrow(column))
private fun Cursor.string(column: String): String = getString(getColumnIndexOrThrow(column))

private fun requireLocal(condition: Boolean, message: String) {
    if (!condition) throw IllegalStateException(message)
}
