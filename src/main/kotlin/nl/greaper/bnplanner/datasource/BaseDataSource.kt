package nl.greaper.bnplanner.datasource

import com.mongodb.bulk.BulkWriteResult
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.InsertOneModel
import org.bson.conversions.Bson
import org.litote.kmongo.EMPTY_BSON
import org.litote.kmongo.deleteOneById
import org.litote.kmongo.findOne
import org.litote.kmongo.findOneById

abstract class BaseDataSource<T> {
    protected val collection by lazy { initCollection() }

    abstract fun initCollection(): MongoCollection<T>

    protected fun list(): List<T> = collection.find().toList()
    fun findFirst() = collection.findOne()
    fun findById(id: String): T? = collection.findOneById(id)
    fun deleteById(id: String) = collection.deleteOneById(id)
    fun deleteMany() = collection.deleteMany(EMPTY_BSON)
    fun count(filter: Bson): Int = collection.countDocuments(filter).toInt()
    fun insertOne(value: T) = collection.insertOne(value)
    fun insertMany(values: List<T>): List<BulkWriteResult> = collection.insertManyBatched(values)

    private fun MongoCollection<T>.insertManyBatched(list: List<T>, batchSize: Int = 100): List<BulkWriteResult> {
        val preparedList = list.map { InsertOneModel<T>(it) }

        return preparedList.windowed(batchSize, batchSize, true)
            .map { bulk -> bulkWrite(bulk) }
    }
}
