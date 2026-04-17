package com.mitv.player.di

import android.content.Context
import androidx.room.Room
import com.mitv.player.data.M3uParser
import com.mitv.player.data.MiTVDatabase
import com.mitv.player.data.MiTVRepository
import com.mitv.player.player.MiVideoPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MiTVDatabase =
        Room.databaseBuilder(context, MiTVDatabase::class.java, "mitv_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideM3uParser(): M3uParser = M3uParser()

    @Provides
    @Singleton
    fun provideRepository(
        @ApplicationContext context: Context,
        parser: M3uParser,
        database: MiTVDatabase
    ): MiTVRepository = MiTVRepository(context, parser, database)

    @Provides
    @Singleton
    fun provideMiVideoPlayer(
        @ApplicationContext context: Context
    ): MiVideoPlayer = MiVideoPlayer(context)
}
