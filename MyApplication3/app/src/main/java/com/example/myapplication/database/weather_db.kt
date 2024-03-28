package com.example.myapplication.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(entities = [weather::class], version = 1, exportSchema = false)
abstract class weather_db: RoomDatabase(){
    abstract fun weather_dao(): weather_dao
    companion object{
        @Volatile
        private var INSTANCE: weather_db? = null

        fun getDatabase(context: Context): weather_db{
            val tempInstance = INSTANCE
            if(tempInstance != null){
                return tempInstance
            }
            synchronized(this){
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    weather_db::class.java,
                    "weather_database"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}