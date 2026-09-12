package com.samdori93.yeoksadam.core.database.di

import android.content.Context
import androidx.room.Room
import com.samdori93.yeoksadam.core.database.YeoksadamDatabase
import com.samdori93.yeoksadam.core.database.dao.FigureDao
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
    fun provideDatabase(@ApplicationContext context: Context): YeoksadamDatabase =
        Room.databaseBuilder(context, YeoksadamDatabase::class.java, "yeoksadam.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideFigureDao(db: YeoksadamDatabase): FigureDao = db.figureDao()
}
