package com.discord.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.discord.views.ToolbarTitleLayout
import com.lytefast.flexinput.managers.FileManager
import rx.functions.Action1
import rx.functions.Action2
import rx.functions.Func0
import rx.subjects.Subject
import java.io.File
import kotlin.jvm.functions.Function0
import kotlin.jvm.functions.Function1

@Suppress("Unused", "Deprecated")
open class AppFragment : Fragment {

    constructor() : super()

    constructor(@LayoutRes layoutId: Int) : super(layoutId)

    open fun setActionBarTitle(string: String) {}

    @CallSuper
    open fun bindToolbar(view: View) {
    }

    open fun getActionBarTitleLayout(): ToolbarTitleLayout? = null
    open fun getAppActivity(): AppActivity? = null
    open fun getAppLogger(): AppLogger? = null
    open fun getFileManager(): FileManager? = null
    open fun getImageFile(): File? = null
    open fun getLoggingConfig(): LoggingConfig? = null
    open fun getMostRecentIntent(): Intent? = null
    open fun getUnsubscribeSignal(): Subject<Void, Void>? = null
    open fun hasMedia(): Boolean = false
    open fun hideKeyboard() {}
    open fun hideKeyboard(view: View) {}
    open fun isRecreated(): Boolean = false

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun onDetach() {
        super.onDetach()
    }

    open fun onImageChosen(uri: Uri, string: String) {}
    open fun onImageCropped(uri: Uri, string: String) {}

    override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
    }

    @CallSuper
    open fun onViewBound(view: View) {
    }

    @CallSuper
    open fun onViewBoundOrOnResume() {
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    open fun openMediaChooser() {}
    open fun requestCameraQRScanner(onGranted: Function0<Unit>) {}
    open fun requestCameraQRScanner(onGranted: Function0<Unit>, onDenied: Function0<Unit>) {}
    open fun requestContacts(onGranted: Function0<Unit>, onDenied: Function0<Unit>) {}
    open fun requestMedia(onGranted: Function0<Unit>) {}
    open fun requestMediaDownload(onGranted: Function0<Unit>) {}
    open fun requestMicrophone(onGranted: Function0<Unit>, onDenied: Function0<Unit>) {}
    open fun requestVideoCallPermissions(onGranted: Function0<Unit>) {}
    open fun requireAppActivity(): AppActivity = AppActivity()

    open fun setActionBarDisplayHomeAsUpEnabled(): Toolbar? = null
    open fun setActionBarDisplayHomeAsUpEnabled(enabled: Boolean): Toolbar? = null
    open fun setActionBarDisplayHomeAsUpEnabled(
        enabled: Boolean,
        @DrawableRes homeIcon: Int?,
        @StringRes contentDescription: Int?
    ): Toolbar? = null

    open fun setActionBarOptionsMenu(
        @MenuRes menuRes: Int,
        onItemSelected: Action2<MenuItem, Context>
    ): Toolbar? = null

    open fun setActionBarOptionsMenu(
        @MenuRes menuRes: Int,
        onItemSelected: Action2<MenuItem, Context>,
        onMenuCreated: Action1<Menu>
    ): Toolbar? = null

    open fun setActionBarSubtitle(@StringRes titleRes: Int) {}
    open fun setActionBarSubtitle(title: CharSequence) {}
    open fun setActionBarTitle(@StringRes titleRes: Int) {}
    open fun setActionBarTitle(title: CharSequence) {}
    open fun setActionBarTitle(title: CharSequence, @DrawableRes icon: Int?) {}
    open fun setActionBarTitle(
        title: CharSequence,
        @DrawableRes icon: Int?,
        @DrawableRes endIcon: Int?
    ) {
    }

    open fun setActionBarTitleAccessibilityViewFocused() {}
    open fun setActionBarTitleClick(listener: View.OnClickListener) {}
    open fun setActionBarTitleColor(@ColorInt color: Int) {}
    open fun setActionBarTitleLayoutExpandedTappableArea() {}
    open fun setActionBarTitleLayoutMinimumTappableArea() {}

    open fun setOnBackPressed(callback: Func0<Boolean>) {}
    open fun setOnBackPressed(callback: Func0<Boolean>, priority: Int) {}
    open fun setOnNewIntentListener(listener: Function1<Intent, Unit>) {}
    open fun showKeyboard(view: View) {}
}