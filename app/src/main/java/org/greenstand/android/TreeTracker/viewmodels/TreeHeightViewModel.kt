package org.greenstand.android.TreeTracker.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.data.NewTree
import org.greenstand.android.TreeTracker.data.TreeHeightAttributes
import org.greenstand.android.TreeTracker.data.TreeColor
import org.greenstand.android.TreeTracker.managers.TreeManager

class TreeHeightViewModel : CoroutineViewModel() {

    var newTree: NewTree? = null
    var treeColor: TreeColor? = null
        set(value) {
            field = value
            onEnableButtonLiveData.postValue(value != null)
        }

    private val toastMessageLiveData = MutableLiveData<Int>()
    private val onFinishedLiveData = MutableLiveData<Unit>()
    private val onEnableButtonLiveData = MutableLiveData<Boolean>()

    fun saveNewTree() {
        launch {
            if (treeColor == null) {
                toastMessageLiveData.postValue(R.string.tree_height_selection_error)
                return@launch
            }

            newTree
                ?.let {
                    withContext(Dispatchers.IO) {
                        val treeId = TreeManager.addTree(photoPath = it.photoPath,
                                                         minAccuracy = it.minAccuracy,
                                                         timeToNextUpdate = it.timeToNextUpdate,
                                                         content = it.content,
                                                         userId = it.userId,
                                                         planterIdentifierId = it.planterIdentifierId)

                        fun addKeyValueAttribute(key: String, value: String) = TreeManager.addTreeAttribute(treeId, key, value)

                        with(TreeHeightAttributes(treeId = treeId, heightColor = treeColor!!)) {
                            addKeyValueAttribute("height_color", heightColor.value)
                            addKeyValueAttribute("app_build", appBuild)
                            addKeyValueAttribute("app_flavor", appFlavor)
                            addKeyValueAttribute("app_version", appVersion)
                        }
                    }
                }
                ?.also {
                    toastMessageLiveData.postValue(R.string.tree_saved)
                    onFinishedLiveData.postValue(Unit)
                }
                ?: run { toastMessageLiveData.postValue(R.string.tree_height_save_error) }

        }
    }

    fun toastMessagesLiveData(): LiveData<Int> = toastMessageLiveData

    fun onFinishedLiveData(): LiveData<Unit> = onFinishedLiveData

    fun onEnableButtonLiveData(): LiveData<Boolean> = onEnableButtonLiveData
}