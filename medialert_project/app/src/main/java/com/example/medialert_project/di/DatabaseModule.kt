package com.example.medialert_project.di

import android.content.Context
import androidx.room.Room
import com.example.medialert_project.data.local.MediAlertDatabase
import com.example.medialert_project.data.local.dao.DoseLogDao
import com.example.medialert_project.data.local.dao.MedicineDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MediAlertDatabase =
        Room.databaseBuilder(
            context,
            MediAlertDatabase::class.java,
            "medialert.db"
        ).build()

    @Provides
    fun provideMedicineDao(database: MediAlertDatabase): MedicineDao = database.medicineDao()

    @Provides
    fun provideDoseLogDao(database: MediAlertDatabase): DoseLogDao = database.doseLogDao()
}
