package com.hitachi.confirmnfc.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import com.hitachi.confirmnfc.R
import com.hitachi.confirmnfc.databinding.FragmentNfcConfirmBinding
import com.hitachi.confirmnfc.util.NfcUtil
import com.hitachi.confirmnfc.AppData
import com.hitachi.confirmnfc.viewmodel.NfcConfirmViewModel
import com.hitachi.confirmnfc.viewmodel.ViewModelFactory

/**
 * NFC読み取り結果を表示する画面Fragment。
 */
class NfcConfirmFragment : Fragment() {

    companion object {
        /** Fragment生成用のファクトリメソッド。 */
        @JvmStatic
        fun newInstance(): NfcConfirmFragment = NfcConfirmFragment()
    }

    /** ViewBindingの退避領域。 */
    private var _binding: FragmentNfcConfirmBinding? = null

    /** null非許容で使うBinding参照。 */
    private val binding get() = _binding!!

    /** 画面状態を管理するViewModel。 */
    private val viewModel: NfcConfirmViewModel by activityViewModels {
        ViewModelFactory(requireActivity())
    }

    /** NFC ReaderModeを扱うユーティリティ。 */
    private var nfcUtil: NfcUtil? = null

    /**
     * 画面生成時にBindingとNFCユーティリティを初期化する。
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_nfc_confirm, container, false)
        binding.nfcViewModel = viewModel

        viewModel.init()
        nfcUtil = NfcUtil(requireActivity())
        return binding.root
    }

    /**
     * 表示後にライフサイクル所有者を設定し、未ログインならログイン画面へ戻す。
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner

        if (AppData.csvRecords.isEmpty()) {
            parentFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.frameContainer, LoginFragment())
            }
        }
    }

    /**
     * フォアグラウンド復帰時にNFC読み取りを開始する。
     */
    override fun onResume() {
        super.onResume()
        if (AppData.csvRecords.isEmpty()) return

        nfcUtil?.start(
            onRead = { tag ->
                val sn = tag.id.joinToString("") { "%02X".format(it.toInt() and 0xFF) }
                viewModel.onTagRead(sn)
            },
            onFailure = { message ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    /**
     * バックグラウンド遷移時にNFC読み取りを停止する。
     */
    override fun onPause() {
        nfcUtil?.stop()
        super.onPause()
    }

    /**
     * View破棄時にリソースを解放する。
     */
    override fun onDestroyView() {
        super.onDestroyView()
        nfcUtil?.stop()
        nfcUtil = null
        _binding = null
    }
}
