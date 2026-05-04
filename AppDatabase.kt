package com.nutripulse.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nutripulse.app.data.dao.*
import com.nutripulse.app.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Feed::class,
        AnimalProfile::class,
        Ration::class,
        RationItem::class,
        StockItem::class,
        StockTransaction::class,
        FarmProfile::class
    ],
    version = 11,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun feedDao(): FeedDao
    abstract fun animalProfileDao(): AnimalProfileDao
    abstract fun rationDao(): RationDao
    abstract fun rationItemDao(): RationItemDao
    abstract fun stockDao(): StockDao
    abstract fun farmProfileDao(): FarmProfileDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nutripulse_db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        INSTANCE?.let { database ->
                            CoroutineScope(Dispatchers.IO).launch {
                                DatabaseInitializer.populateFeeds(database.feedDao())
                            }
                        }
                    }
                })
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
