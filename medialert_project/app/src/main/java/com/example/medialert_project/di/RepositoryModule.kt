package com.example.medialert_project.di

import com.example.medialert_project.data.repository.AuthRepositoryImpl
import com.example.medialert_project.data.repository.DoseLogRepositoryImpl
import com.example.medialert_project.data.repository.MedicineRepositoryImpl
import com.example.medialert_project.domain.repository.AuthRepository
import com.example.medialert_project.domain.repository.DoseLogRepository
import com.example.medialert_project.domain.repository.MedicineRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindMedicineRepository(impl: MedicineRepositoryImpl): MedicineRepository

    @Binds
    @Singleton
    abstract fun bindDoseLogRepository(impl: DoseLogRepositoryImpl): DoseLogRepository
}
