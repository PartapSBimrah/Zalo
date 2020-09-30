package com.mgt.zalo.ui.edit_media

import androidx.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import com.mgt.zalo.di.ViewModelKey

@Module
interface EditMediaModule {
    @Binds
    @IntoMap
    @ViewModelKey(EditMediaViewModel::class)
    fun bindViewModel(viewModel: EditMediaViewModel): ViewModel
}