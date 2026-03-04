package com.hitachi.confirmnfc.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.hitachi.confirmnfc.R
import com.hitachi.confirmnfc.adapter.NfcInfoAdapter
import com.hitachi.confirmnfc.databinding.FragmentNfcConfirmBinding
import com.hitachi.confirmnfc.util.NfcUtil
import com.hitachi.confirmnfc.viewmodel.NfcConfirmViewModel
import com.hitachi.confirmnfc.viewmodel.ViewModelFactory

/**
 * NFC読み取り結果を表示する画面Fragment
 */
class NfcConfirmFragment : Fragment() {

    /** Binding */
    private var _binding: FragmentNfcConfirmBinding? = null
    private val binding get() = _binding!!

    /** ViewModel */
    private val viewModel by lazy {
        ViewModelProvider(this, ViewModelFactory(requireActivity()))[NfcConfirmViewModel::class.java]
    }

    /** NFC ReaderModeを扱うユーティリティ */
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
        binding.lifecycleOwner = viewLifecycleOwner
        binding.nfcViewModel = viewModel

        viewModel.init()
        nfcUtil = NfcUtil(requireActivity())

        val adapter = NfcInfoAdapter()
        binding.infoList.layoutManager = LinearLayoutManager(requireContext())
        binding.infoList.adapter = adapter

        viewModel.matchedList.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }

        return binding.root
    }

    /**
     * フォアグラウンド復帰時にNFC読み取りを開始する。
     */
    override fun onResume() {
        super.onResume()

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
