package vng.zalo.tdtai.zalo.ui.share

import android.content.Intent
import androidx.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import vng.zalo.tdtai.zalo.base.BaseOnEventListener
import vng.zalo.tdtai.zalo.di.ViewModelKey

@Module(includes = [ShareModule.ProvideModule::class])
interface ShareModule {
    @Binds
    fun eventListener(activity: ShareActivity): BaseOnEventListener

    @Binds
    @IntoMap
    @ViewModelKey(ShareViewModel::class)
    fun bindShareViewModel(viewModel: ShareViewModel): ViewModel

    @Module
    class ProvideModule{
        @Provides
        fun provideIntent(activity: ShareActivity):Intent{
            return activity.intent
        }
    }
}